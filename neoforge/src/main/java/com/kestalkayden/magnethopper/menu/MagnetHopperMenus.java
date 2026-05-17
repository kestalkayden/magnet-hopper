package com.kestalkayden.magnethopper.menu;

import com.kestalkayden.magnethopper.MagnetHopperNeoForge;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MagnetHopperMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, MagnetHopperNeoForge.MOD_ID);

    /** Static field assigned the first time the DeferredHolder is resolved. */
    public static MenuType<MagnetHopperMenu> MAGNET_HOPPER_MENU;

    public static final DeferredHolder<MenuType<?>, MenuType<MagnetHopperMenu>> MAGNET_HOPPER_MENU_HOLDER =
        MENUS.register("magnet_hopper", () -> {
            MAGNET_HOPPER_MENU = new MenuType<>(MagnetHopperMenu::new, FeatureFlags.VANILLA_SET);
            return MAGNET_HOPPER_MENU;
        });

    private MagnetHopperMenus() {}
}
