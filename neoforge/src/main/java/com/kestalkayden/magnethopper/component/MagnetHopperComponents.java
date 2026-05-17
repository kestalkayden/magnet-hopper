package com.kestalkayden.magnethopper.component;

import com.kestalkayden.magnethopper.MagnetHopperNeoForge;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MagnetHopperComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MagnetHopperNeoForge.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MagnetHopperConfig>> CONFIG_HOLDER =
        COMPONENTS.registerComponentType("config", builder -> builder
            .persistent(MagnetHopperConfig.CODEC)
            .networkSynchronized(MagnetHopperConfig.STREAM_CODEC));

    private MagnetHopperComponents() {}

    public static DataComponentType<MagnetHopperConfig> CONFIG() {
        return CONFIG_HOLDER.get();
    }
}
