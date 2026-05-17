package com.kestalkayden.magnethopper.component;

import com.kestalkayden.magnethopper.MagnetHopperFabric;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class MagnetHopperComponents {
    private static DataComponentType<MagnetHopperConfig> config;

    private MagnetHopperComponents() {}

    public static void register() {
        config = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(MagnetHopperFabric.MOD_ID, "config"),
            DataComponentType.<MagnetHopperConfig>builder()
                .persistent(MagnetHopperConfig.CODEC)
                .networkSynchronized(MagnetHopperConfig.STREAM_CODEC)
                .build()
        );
    }

    public static DataComponentType<MagnetHopperConfig> CONFIG() {
        return config;
    }
}
