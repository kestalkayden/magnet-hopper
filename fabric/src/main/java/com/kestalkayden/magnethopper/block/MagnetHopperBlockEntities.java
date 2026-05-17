package com.kestalkayden.magnethopper.block;

import com.kestalkayden.magnethopper.MagnetHopperFabric;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class MagnetHopperBlockEntities {
    public static BlockEntityType<MagnetHopperBlockEntity> MAGNET_HOPPER_BE;

    private MagnetHopperBlockEntities() {}

    public static void register() {
        MAGNET_HOPPER_BE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(MagnetHopperFabric.MOD_ID, "magnet_hopper"),
            FabricBlockEntityTypeBuilder.create(MagnetHopperBlockEntity::new,
                MagnetHopperBlocks.MAGNET_HOPPER,
                MagnetHopperBlocks.ADVANCED_MAGNET_HOPPER,
                MagnetHopperBlocks.INDUSTRIAL_MAGNET_HOPPER
            ).build());
    }
}
