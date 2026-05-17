package com.kestalkayden.magnethopper.menu;

import com.kestalkayden.magnethopper.MagnetHopperFabric;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class MagnetHopperMenus {
    public static MenuType<MagnetHopperMenu> MAGNET_HOPPER_MENU;

    private MagnetHopperMenus() {}

    public static void register() {
        MAGNET_HOPPER_MENU = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(MagnetHopperFabric.MOD_ID, "magnet_hopper"),
            new MenuType<>(MagnetHopperMenu::new, FeatureFlags.VANILLA_SET));
    }
}
