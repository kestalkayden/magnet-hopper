package com.kestalkayden.magnethopper.client;

import com.kestalkayden.magnethopper.menu.MagnetHopperMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class MagnetHopperScreen extends AbstractContainerScreen<MagnetHopperMenu> {

    // Taller than vanilla chest to fit two slot rows + their labels + standard player inv.
    private static final int GUI_W = 176;
    private static final int GUI_H = 184;

    // Layout Y positions — labels sit ABOVE their slot rows with a clean 2-px gap.
    private static final int LABEL_STORAGE_Y = MagnetHopperMenu.ROW_STORAGE_Y - 10;  // 20
    private static final int LABEL_FILTER_Y  = MagnetHopperMenu.ROW_FILTER_Y  - 10;  // 52

    // Color palette
    private static final int BG_GRAY      = 0xFFC6C6C6;
    private static final int BG_BORDER    = 0xFF555555;
    private static final int SLOT_BORDER  = 0xFF373737;
    private static final int SLOT_INNER   = 0xFF8B8B8B;
    private static final int FILTER_INNER = 0xFF7B8FB0;
    private static final int LABEL_COLOR  = 0xFF404040;

    // Right-side buttons (stacked) — aligned vertically with their slot rows.
    private static final int BTN_W = 50;
    private static final int BTN_H = 20;
    private static final int BTN_X = 120;
    private static final int BTN_MAGNET_Y    = MagnetHopperMenu.ROW_STORAGE_Y - 1;
    private static final int BTN_WHITELIST_Y = MagnetHopperMenu.ROW_FILTER_Y  - 1;

    private Button magnetButton;
    private Button whitelistButton;

    public MagnetHopperScreen(MagnetHopperMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, GUI_W, GUI_H);
        this.inventoryLabelY = GUI_H - 94 + 2;
    }

    @Override
    protected void init() {
        super.init();

        this.magnetButton = Button.builder(getMagnetLabel(), b -> {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId, MagnetHopperMenu.BUTTON_TOGGLE_MAGNET);
                }
            })
            .bounds(leftPos + BTN_X, topPos + BTN_MAGNET_Y, BTN_W, BTN_H)
            .build();
        addRenderableWidget(magnetButton);

        this.whitelistButton = Button.builder(getWhitelistLabel(), b -> {
                if (minecraft != null && minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(
                        menu.containerId, MagnetHopperMenu.BUTTON_TOGGLE_WHITELIST);
                }
            })
            .bounds(leftPos + BTN_X, topPos + BTN_WHITELIST_Y, BTN_W, BTN_H)
            .build();
        addRenderableWidget(whitelistButton);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;

        g.fill(x, y, x + GUI_W, y + GUI_H, BG_GRAY);
        g.fill(x, y, x + GUI_W, y + 1, BG_BORDER);
        g.fill(x, y + GUI_H - 1, x + GUI_W, y + GUI_H, BG_BORDER);
        g.fill(x, y, x + 1, y + GUI_H, BG_BORDER);
        g.fill(x + GUI_W - 1, y, x + GUI_W, y + GUI_H, BG_BORDER);

        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            int sx = x + slot.x - 1;
            int sy = y + slot.y - 1;
            boolean isFilterSlot = i >= MagnetHopperMenu.FILTER_SLOT_START
                                && i <  MagnetHopperMenu.FILTER_SLOT_END;
            g.fill(sx, sy, sx + 18, sy + 18, SLOT_BORDER);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, isFilterSlot ? FILTER_INNER : SLOT_INNER);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        // Pass shadow=false so the dark labels don't get a darker drop-shadow ghost behind them
        g.text(font, "Storage", 8, LABEL_STORAGE_Y, LABEL_COLOR, false);
        g.text(font, "Filter",  8, LABEL_FILTER_Y,  LABEL_COLOR, false);
        // Static labels above each button — pairs with the short action verb on the button itself
        g.text(font, "Magnet", BTN_X, LABEL_STORAGE_Y, LABEL_COLOR, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (magnetButton != null) magnetButton.setMessage(getMagnetLabel());
        if (whitelistButton != null) whitelistButton.setMessage(getWhitelistLabel());
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }

    private Component getWhitelistLabel() {
        // State on the button (current mode), color reinforces which one is active.
        if (menu.isWhitelist()) {
            return Component.translatable("gui.magnethopper.whitelist").withStyle(ChatFormatting.GREEN);
        } else {
            return Component.translatable("gui.magnethopper.blacklist").withStyle(ChatFormatting.RED);
        }
    }

    private Component getMagnetLabel() {
        // State on the button (the magnet IS enabled/disabled right now). "Magnet" static label above gives context.
        if (menu.isMagnetEnabled()) {
            return Component.translatable("gui.magnethopper.enabled").withStyle(ChatFormatting.GREEN);
        } else {
            return Component.translatable("gui.magnethopper.disabled").withStyle(ChatFormatting.RED);
        }
    }
}
