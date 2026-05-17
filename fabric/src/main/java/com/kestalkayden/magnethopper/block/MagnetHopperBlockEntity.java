package com.kestalkayden.magnethopper.block;

import com.kestalkayden.magnethopper.menu.MagnetHopperMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class MagnetHopperBlockEntity extends RandomizableContainerBlockEntity {

    public static final int CONTAINER_SIZE = 5;
    public static final int FILTER_SIZE = 5;

    /** Ticks between pull-scans. */
    private static final int PULL_COOLDOWN = 8;
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
            changed |= suckFromContainerAbove(level, pos);
            // The magnet add-on (3-block radius item-entity pull) is gated by the UI toggle.
            if (magnetEnabled) {
                changed |= pullNearbyItems(level, pos);
            }
            pullCooldown = PULL_COOLDOWN;
        }

        if (pushCooldown > 0) {
            pushCooldown--;
        } else {
            changed |= pushToContainerBelow(level, pos);
            pushCooldown = PUSH_COOLDOWN;
        }

        if (changed) {
            setChanged();
        }
    }

    /** Vanilla hopper behavior: pull one item from a container directly above, respecting the filter. */
    private boolean suckFromContainerAbove(ServerLevel level, BlockPos pos) {
        Container source = HopperBlockEntity.getContainerAt(level, pos.above());
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

    private boolean pushToContainerBelow(ServerLevel level, BlockPos pos) {
        Container dest = HopperBlockEntity.getContainerAt(level, pos.below());
        if (dest == null) return false;

        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack slotStack = getItem(i);
            if (slotStack.isEmpty()) continue;
            ItemStack one = slotStack.copyWithCount(1);
            ItemStack remainder = HopperBlockEntity.addItem(this, dest, one, Direction.UP);
            if (remainder.isEmpty()) {
                slotStack.shrink(1);
                return true;
            }
        }
        return false;
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
