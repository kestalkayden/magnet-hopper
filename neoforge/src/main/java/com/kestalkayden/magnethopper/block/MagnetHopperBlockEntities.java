package com.kestalkayden.magnethopper.block;

import com.kestalkayden.magnethopper.MagnetHopperNeoForge;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MagnetHopperBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MagnetHopperNeoForge.MOD_ID);

    public static BlockEntityType<MagnetHopperBlockEntity> MAGNET_HOPPER_BE;

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MagnetHopperBlockEntity>> MAGNET_HOPPER_BE_HOLDER =
        BES.register("magnet_hopper", () -> {
            MAGNET_HOPPER_BE = new BlockEntityType<>(MagnetHopperBlockEntity::new,
                MagnetHopperBlocks.MAGNET_HOPPER.get(),
                MagnetHopperBlocks.ADVANCED_MAGNET_HOPPER.get(),
                MagnetHopperBlocks.INDUSTRIAL_MAGNET_HOPPER.get());
            return MAGNET_HOPPER_BE;
        });

    private MagnetHopperBlockEntities() {}
}
