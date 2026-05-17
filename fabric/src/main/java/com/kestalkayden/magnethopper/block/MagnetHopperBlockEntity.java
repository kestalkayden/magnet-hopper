package com.kestalkayden.magnethopper.block;

import com.kestalkayden.magnethopper.component.MagnetHopperComponents;
import com.kestalkayden.magnethopper.component.MagnetHopperConfig;
import com.kestalkayden.magnethopper.menu.MagnetHopperMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class MagnetHopperBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {

    public static final int CONTAINER_SIZE = 5;
    public static final int FILTER_SIZE = 5;

    /** All main storage slot indices — used by WorldlyContainer.getSlotsForFace. */
    private static final int[] ALL_MAIN_SLOTS = new int[]{0, 1, 2, 3, 4};

    /** Ticks between pull-scans when actively finding items. */
    private static final int PULL_COOLDOWN = 8;
    /** Max pull cooldown after backoff (6.4s of idle). */
    private static final int MAX_PULL_COOLDOWN = 128;
    /** Ticks between push attempts (matches vanilla hopper cadence). */
    private static final int PUSH_COOLDOWN = 8;

    private NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    /** Filter slots. SimpleContainer so the Menu can bind Slot objects to it directly. */
    public final SimpleContainer filterContainer = new SimpleContainer(FILTER_SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            MagnetHopperBlockEntity.this.setChanged();
        }
    };

    /** True = pull only items matching a non-empty filter slot. False = pull only items NOT matching any filter slot. */
    private boolean whitelist = true;

    /** Toggles the MAGNET pull only. When false, the block still acts as a vanilla hopper (pull from
     *  container above + push down). Redstone power disables everything regardless. */
    private boolean magnetEnabled = true;

    private int pullCooldown = 0;
    private int pushCooldown = 0;
    /** Consecutive empty pull scans, exponentiates the pull cooldown up to MAX_PULL_COOLDOWN. */
    private int emptyPullScans = 0;

    public MagnetHopperBlockEntity(BlockPos pos, BlockState state) {
        super(MagnetHopperBlockEntities.MAGNET_HOPPER_BE, pos, state);
    }

    public boolean isWhitelist() { return whitelist; }

    public void setWhitelist(boolean whitelist) {
        this.whitelist = whitelist;
        setChanged();
    }

    public boolean isMagnetEnabled() { return magnetEnabled; }

    public void setMagnetEnabled(boolean enabled) {
        this.magnetEnabled = enabled;
        setChanged();
    }

    // --- Container (main storage) ---

    @Override
    public int getContainerSize() { return CONTAINER_SIZE; }

    @Override
    protected NonNullList<ItemStack> getItems() { return items; }

    @Override
    protected void setItems(NonNullList<ItemStack> items) { this.items = items; }

    @Override
    protected Component getDefaultName() {
        // Use the block's own translation key so each tier shows its own name in the GUI title.
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    /** Returns the radius to scan, derived from the block's tier. */
    private int getRadius() {
        return (getBlockState().getBlock() instanceof MagnetHopperBlock mhb)
            ? mhb.getTier().radius
            : MagnetTier.BASIC.radius;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new MagnetHopperMenu(containerId, playerInventory, this);
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!trySaveLootTable(output)) {
            ContainerHelper.saveAllItems(output, items);
        }
        // Persist filter container contents
        NonNullList<ItemStack> filterList = NonNullList.withSize(FILTER_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < FILTER_SIZE; i++) filterList.set(i, filterContainer.getItem(i));
        ContainerHelper.saveAllItems(output.child("FilterItems"), filterList);
        output.putBoolean("Whitelist", whitelist);
        output.putBoolean("MagnetEnabled", magnetEnabled);
        output.putInt("PullCooldown", pullCooldown);
        output.putInt("PushCooldown", pushCooldown);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);
        if (!tryLoadLootTable(input)) {
            ContainerHelper.loadAllItems(input, items);
        }
        // Load filter container contents
        NonNullList<ItemStack> filterList = NonNullList.withSize(FILTER_SIZE, ItemStack.EMPTY);
        input.child("FilterItems").ifPresent(fi -> ContainerHelper.loadAllItems(fi, filterList));
        for (int i = 0; i < FILTER_SIZE; i++) filterContainer.setItem(i, filterList.get(i));
        this.whitelist = input.getBooleanOr("Whitelist", true);
        this.magnetEnabled = input.getBooleanOr("MagnetEnabled", true);
        this.pullCooldown = input.getIntOr("PullCooldown", 0);
        this.pushCooldown = input.getIntOr("PushCooldown", 0);
    }

    // --- Tick (server only) ---

    /** Called by MagnetHopperBlock.getTicker. */
    public void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        // Redstone signal disables everything (matches vanilla hopper convention).
        if (!state.getValue(MagnetHopperBlock.ENABLED)) {
            return;
        }

        boolean changed = false;

        if (pullCooldown > 0) {
            pullCooldown--;
        } else {
            // Vanilla hopper "suck" is ALWAYS active — magnet hopper is a strict superset of vanilla hopper.
            boolean pulled = suckFromContainerAbove(level, pos);
            // The magnet add-on (3-block radius item-entity pull) is gated by the UI toggle.
            if (magnetEnabled) {
                pulled |= pullNearbyItems(level, pos);
            }
            changed |= pulled;
            // Adaptive backoff: idle hoppers tick less often. Resets on any successful pull or push.
            if (pulled) emptyPullScans = 0;
            else if (emptyPullScans < 4) emptyPullScans++;
            pullCooldown = Math.min(PULL_COOLDOWN << emptyPullScans, MAX_PULL_COOLDOWN);
        }

        if (pushCooldown > 0) {
            pushCooldown--;
        } else {
            boolean pushed = pushToContainer(level, pos, state);
            if (pushed) {
                changed = true;
                emptyPullScans = 0;  // freed up inventory, wake pull loop back to baseline
            }
            pushCooldown = PUSH_COOLDOWN;
        }

        if (changed) {
            setChanged();
        }
    }

    /** Vanilla hopper behavior: pull one item from a container directly above, respecting the filter. */
    private boolean suckFromContainerAbove(ServerLevel level, BlockPos pos) {
        Container source = findContainerAt(level, pos.above());
        if (source == null) return false;
        for (int i = 0; i < source.getContainerSize(); i++) {
            ItemStack stack = source.getItem(i);
            if (stack.isEmpty()) continue;
            if (!passesFilter(stack)) continue;
            ItemStack one = stack.copyWithCount(1);
            ItemStack remainder = HopperBlockEntity.addItem(source, this, one, Direction.DOWN);
            if (remainder.isEmpty()) {
                stack.shrink(1);
                source.setChanged();
                return true;
            }
        }
        return false;
    }

    private boolean pullNearbyItems(ServerLevel level, BlockPos pos) {
        AABB box = new AABB(pos).inflate(getRadius());
        boolean anyInserted = false;
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, box, ItemEntity::isAlive)) {
            if (!passesFilter(itemEntity.getItem())) continue;
            if (HopperBlockEntity.addItem(this, itemEntity)) {
                anyInserted = true;
            }
        }
        return anyInserted;
    }

    private boolean pushToContainer(ServerLevel level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(MagnetHopperBlock.FACING);
        Container dest = findContainerAt(level, pos.relative(facing));
        if (dest == null) return false;

        // The destination's "input side" is the face the hopper points INTO (opposite of facing).
        Direction insertSide = facing.getOpposite();
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack slotStack = getItem(i);
            if (slotStack.isEmpty()) continue;
            if (!passesFilter(slotStack)) continue;  // filter applies to push-out too
            ItemStack one = slotStack.copyWithCount(1);
            ItemStack remainder = HopperBlockEntity.addItem(this, dest, one, insertSide);
            if (remainder.isEmpty()) {
                slotStack.shrink(1);
                return true;
            }
        }
        return false;
    }

    // --- Data components (carry main inventory across break/place) ---

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.items));

        NonNullList<ItemStack> filterItems = NonNullList.withSize(FILTER_SIZE, ItemStack.EMPTY);
        for (int i = 0; i < FILTER_SIZE; i++) filterItems.set(i, filterContainer.getItem(i));
        components.set(MagnetHopperComponents.CONFIG(),
            new MagnetHopperConfig(ItemContainerContents.fromItems(filterItems), whitelist, magnetEnabled));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter componentGetter) {
        super.applyImplicitComponents(componentGetter);
        componentGetter.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(this.items);

        MagnetHopperConfig config = componentGetter.getOrDefault(MagnetHopperComponents.CONFIG(), MagnetHopperConfig.DEFAULT);
        NonNullList<ItemStack> filterItems = NonNullList.withSize(FILTER_SIZE, ItemStack.EMPTY);
        config.filters().copyInto(filterItems);
        for (int i = 0; i < FILTER_SIZE; i++) filterContainer.setItem(i, filterItems.get(i));
        this.whitelist = config.whitelist();
        this.magnetEnabled = config.magnetEnabled();
    }

    // --- WorldlyContainer (filter applies to automation inserts; UI is unfiltered) ---

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_MAIN_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return passesFilter(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        // External hoppers pulling FROM us respect the filter — same conceptual rule as
        // automation push-in. Filter is symmetric across all automation paths; only UI bypasses.
        return passesFilter(stack);
    }

    /**
     * Returns a container at the given pos — checks block entities first, then falls back to
     * any Container entity at that position (hopper minecart, chest minecart, etc.).
     */
    private static Container findContainerAt(net.minecraft.world.level.Level level, BlockPos pos) {
        Container blockContainer = HopperBlockEntity.getContainerAt(level, pos);
        if (blockContainer != null) return blockContainer;
        AABB box = new AABB(pos);
        java.util.List<Entity> entities = level.getEntities((Entity) null, box,
            e -> e instanceof Container && e.isAlive());
        return entities.isEmpty() ? null : (Container) entities.get(0);
    }

    /**
     * Returns whether the given stack passes the filter.
     * - All filter slots empty: pass-all (true regardless of mode).
     * - Any filter slot set, whitelist mode: pass only on match.
     * - Any filter slot set, blacklist mode: pass only on no-match.
     */
    private boolean passesFilter(ItemStack stack) {
        boolean anyFilterSet = false;
        for (int i = 0; i < FILTER_SIZE; i++) {
            ItemStack filter = filterContainer.getItem(i);
            if (filter.isEmpty()) continue;
            anyFilterSet = true;
            if (ItemStack.isSameItem(filter, stack)) {
                return whitelist;
            }
        }
        if (!anyFilterSet) return true;
        return !whitelist;
    }
}
