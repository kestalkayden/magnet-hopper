package com.kestalkayden.magnethopper;

import java.util.function.Supplier;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.kestalkayden.magnethopper.block.MagnetHopperBlockEntity;
import com.kestalkayden.magnethopper.component.MagnetHopperConfig;
import com.kestalkayden.magnethopper.menu.MagnetHopperMenu;

/**
 * Loader-agnostic accessors for the registered objects the shared {@code common} gameplay code
 * needs — the block-entity type (block-entity ctor), the config data component (component
 * collect/apply), and the menu type (menu ctor).
 *
 * <p>Each loader binds these lazy suppliers from its own registration (Fabric registers directly;
 * NeoForge via {@code DeferredHolder}) in its initializer, and shared code reads them through
 * {@link Supplier#get()}. The suppliers are lazy, so a loader may bind them before its deferred
 * registry has fired; resolution happens at first {@code get()} — always well after registration,
 * since block entities and menus are only constructed at world-interaction time.
 *
 * <p>This single indirection is what lets the block-entity, menu, block, config and tier classes
 * live in {@code common} without importing the per-loader registration classes (whose field types
 * differ between loaders).
 */
public final class MagnetHopperRefs {
    private MagnetHopperRefs() {}

    public static Supplier<BlockEntityType<MagnetHopperBlockEntity>> MAGNET_HOPPER_BE;
    public static Supplier<DataComponentType<MagnetHopperConfig>> CONFIG;
    public static Supplier<MenuType<MagnetHopperMenu>> MAGNET_HOPPER_MENU;
}
