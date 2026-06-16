package com.kestalkayden.magnethopper.menu;

import com.kestalkayden.magnethopper.MagnetHopperRefs;
import com.kestalkayden.magnethopper.block.MagnetHopperBlockEntity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MagnetHopperMenu extends AbstractContainerMenu {

    public static final int MAIN_SLOTS   = MagnetHopperBlockEntity.CONTAINER_SIZE;  // 5
    public static final int FILTER_SLOTS = MagnetHopperBlockEntity.FILTER_SIZE;     // 5
    public static final int MAIN_SLOT_START   = 0;
    public static final int MAIN_SLOT_END     = MAIN_SLOTS;                          // 5
    public static final int FILTER_SLOT_START = MAIN_SLOT_END;                       // 5
    public static final int FILTER_SLOT_END   = MAIN_SLOT_END + FILTER_SLOTS;        // 10
    public static final int PLAYER_INV_START  = FILTER_SLOT_END;                     // 10

    public static final int BUTTON_TOGGLE_WHITELIST = 0;
    public static final int BUTTON_TOGGLE_MAGNET    = 1;

    public static final int DATA_WHITELIST = 0;
    public static final int DATA_MAGNET    = 1;
    public static final int DATA_SIZE      = 2;

    // Slot Y positions — referenced by Screen for label alignment
    public static final int ROW_STORAGE_Y = 30;
    public static final int ROW_FILTER_Y  = 62;
    public static final int ROW_X_START   = 26;

    private final Container mainContainer;
    private final Container filterContainer;
    private final ContainerData data;

    public MagnetHopperMenu(int containerId, Inventory playerInv) {
        this(containerId, playerInv,
             new SimpleContainer(MAIN_SLOTS),
             new SimpleContainer(FILTER_SLOTS),
             new SimpleContainerData(DATA_SIZE));
    }

    public MagnetHopperMenu(int containerId, Inventory playerInv, MagnetHopperBlockEntity be) {
        this(containerId, playerInv, be, be.filterContainer, new ContainerData() {
            @Override public int get(int i) {
                return switch (i) {
                    case DATA_WHITELIST -> be.isWhitelist() ? 1 : 0;
                    case DATA_MAGNET    -> be.isMagnetEnabled() ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int i, int v) {
                switch (i) {
                    case DATA_WHITELIST -> be.setWhitelist(v == 1);
                    case DATA_MAGNET    -> be.setMagnetEnabled(v == 1);
                }
            }
            @Override public int getCount() { return DATA_SIZE; }
        });
    }

    private MagnetHopperMenu(int containerId, Inventory playerInv,
                             Container mainContainer, Container filterContainer, ContainerData data) {
        super(MagnetHopperRefs.MAGNET_HOPPER_MENU.get(), containerId);
        this.mainContainer = mainContainer;
        this.filterContainer = filterContainer;
        this.data = data;

        // Storage row (5 slots), indices 0-4
        for (int i = 0; i < MAIN_SLOTS; i++) {
            addSlot(new Slot(mainContainer, i, ROW_X_START + i * 18, ROW_STORAGE_Y));
        }
        // Filter row (5 slots), indices 5-9. Ghost slots — mayPlace/mayPickup false so vanilla
        // input handlers (SWAP, drag, double-click) don't actually transfer items. The custom
        // clicked() override below handles PICKUP/QUICK_MOVE to ghost-set or clear.
        for (int i = 0; i < FILTER_SLOTS; i++) {
            addSlot(new FilterSlot(filterContainer, i, ROW_X_START + i * 18, ROW_FILTER_Y));
        }
        // Player inventory (3 rows × 9), indices 10-36
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 102 + row * 18));
            }
        }
        // Hotbar, indices 37-45
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 160));
        }

        addDataSlots(data);
    }

    public boolean isWhitelist()     { return data.get(DATA_WHITELIST) == 1; }
    public boolean isMagnetEnabled() { return data.get(DATA_MAGNET)    == 1; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(mainContainer instanceof MagnetHopperBlockEntity be)) {
            return super.clickMenuButton(player, id);
        }
        switch (id) {
            case BUTTON_TOGGLE_WHITELIST -> { be.setWhitelist(!be.isWhitelist()); return true; }
            case BUTTON_TOGGLE_MAGNET    -> { be.setMagnetEnabled(!be.isMagnetEnabled()); return true; }
            default -> { return super.clickMenuButton(player, id); }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return mainContainer.stillValid(player);
    }

    @Override
    public void clicked(int slotId, int dragType, ContainerInput input, Player player) {
        if (slotId >= FILTER_SLOT_START && slotId < FILTER_SLOT_END
                && (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE)) {
            Slot slot = slots.get(slotId);
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.set(carried.copyWithCount(1));
            }
            broadcastChanges();
            return;
        }
        super.clicked(slotId, dragType, input, player);
    }

    /** Ghost slot: vanilla input handlers (SWAP, drag, double-click, throw) do nothing. */
    private static class FilterSlot extends Slot {
        FilterSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player)  { return false; }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < MAIN_SLOT_END) {
            // storage → player inv
            if (!moveItemStackTo(stack, PLAYER_INV_START, slots.size(), true)) return ItemStack.EMPTY;
        } else if (index < FILTER_SLOT_END) {
            // filter slot: clear (ghost — never moves items)
            slot.set(ItemStack.EMPTY);
            return ItemStack.EMPTY;
        } else {
            // player inv → storage (never to filter via shift-click)
            if (!moveItemStackTo(stack, MAIN_SLOT_START, MAIN_SLOT_END, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return original;
    }
}
