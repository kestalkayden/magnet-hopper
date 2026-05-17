package com.kestalkayden.magnethopper.block;

import com.kestalkayden.magnethopper.MagnetHopperFabric;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Optional;

public final class MagnetHopperBlocks {
    public static MagnetHopperBlock MAGNET_HOPPER;
    public static BlockItem MAGNET_HOPPER_ITEM;
    public static MagnetHopperBlock ADVANCED_MAGNET_HOPPER;
    public static BlockItem ADVANCED_MAGNET_HOPPER_ITEM;
    public static MagnetHopperBlock INDUSTRIAL_MAGNET_HOPPER;
    public static BlockItem INDUSTRIAL_MAGNET_HOPPER_ITEM;

    private MagnetHopperBlocks() {}

    public static void register() {
        Registered basic = registerTier("magnet_hopper", MagnetTier.BASIC);
        MAGNET_HOPPER = basic.block();
        MAGNET_HOPPER_ITEM = basic.item();

        Registered adv = registerTier("advanced_magnet_hopper", MagnetTier.ADVANCED);
        ADVANCED_MAGNET_HOPPER = adv.block();
        ADVANCED_MAGNET_HOPPER_ITEM = adv.item();

        Registered ind = registerTier("industrial_magnet_hopper", MagnetTier.INDUSTRIAL);
        INDUSTRIAL_MAGNET_HOPPER = ind.block();
        INDUSTRIAL_MAGNET_HOPPER_ITEM = ind.item();
    }

    private static Registered registerTier(String name, MagnetTier tier) {
        Identifier id = Identifier.fromNamespaceAndPath(MagnetHopperFabric.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item>  itemKey  = ResourceKey.create(Registries.ITEM, id);
        ResourceKey<LootTable> lootKey = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(MagnetHopperFabric.MOD_ID, "blocks/" + name));

        MagnetHopperBlock block = Registry.register(BuiltInRegistries.BLOCK, id,
            new MagnetHopperBlock(tier,
                BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER)
                    .setId(blockKey)
                    .overrideLootTable(Optional.of(lootKey))
                    .mapColor(MapColor.METAL)));

        BlockItem item = Registry.register(BuiltInRegistries.ITEM, id,
            new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));

        return new Registered(block, item);
    }

    private record Registered(MagnetHopperBlock block, BlockItem item) {}
}
