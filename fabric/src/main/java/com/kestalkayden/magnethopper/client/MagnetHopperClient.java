package com.kestalkayden.magnethopper.client;

import com.kestalkayden.magnethopper.menu.MagnetHopperMenus;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class MagnetHopperClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(MagnetHopperMenus.MAGNET_HOPPER_MENU, MagnetHopperScreen::new);
        MagnetHopperFieldRenderer.register();
    }
}
