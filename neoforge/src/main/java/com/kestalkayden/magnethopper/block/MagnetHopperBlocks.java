package com.kestalkayden.magnethopper.block;

import com.kestalkayden.magnethopper.MagnetHopperNeoForge;

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
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MagnetHopperBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MagnetHopperNeoForge.MOD_ID);
    public static final DeferredRegister.Items  ITEMS  = DeferredRegister.createItems(MagnetHopperNeoForge.MOD_ID);

    public static final DeferredBlock<MagnetHopperBlock> MAGNET_HOPPER            = registerBlock("magnet_hopper",            MagnetTier.BASIC);
    public static final DeferredBlock<MagnetHopperBlock> ADVANCED_MAGNET_HOPPER   = registerBlock("advanced_magnet_hopper",   MagnetTier.ADVANCED);
    public static final DeferredBlock<MagnetHopperBlock> INDUSTRIAL_MAGNET_HOPPER = registerBlock("industrial_magnet_hopper", MagnetTier.INDUSTRIAL);

    public static final DeferredItem<BlockItem> MAGNET_HOPPER_ITEM            = registerBlockItem("magnet_hopper",            MAGNET_HOPPER);
    public static final DeferredItem<BlockItem> ADVANCED_MAGNET_HOPPER_ITEM   = registerBlockItem("advanced_magnet_hopper",   ADVANCED_MAGNET_HOPPER);
    public static final DeferredItem<BlockItem> INDUSTRIAL_MAGNET_HOPPER_ITEM = registerBlockItem("industrial_magnet_hopper", INDUSTRIAL_MAGNET_HOPPER);

    private MagnetHopperBlocks() {}

    private static DeferredBlock<MagnetHopperBlock> registerBlock(String name, MagnetTier tier) {
        // Use the Function<Identifier, …> overload so we can construct the block's ResourceKey
        // and call setId() on the Properties — required by BlockBehaviour as of 26.x to
        // bootstrap the implicit loot-table reference. Without it, effectiveDrops NPEs at construction.
        return BLOCKS.register(name, id -> {
            ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
            ResourceKey<LootTable> lootKey = ResourceKey.create(Registries.LOOT_TABLE,
                Identifier.fromNamespaceAndPath(MagnetHopperNeoForge.MOD_ID, "blocks/" + name));
            return new MagnetHopperBlock(tier,
                BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER)
                    .setId(blockKey)
                    .overrideLootTable(Optional.of(lootKey))
                    .mapColor(MapColor.METAL));
        });
    }

    private static DeferredItem<BlockItem> registerBlockItem(String name, DeferredBlock<MagnetHopperBlock> blockHolder) {
        // Same story for items: Item.Properties also requires setId() for stack-default identification.
        return ITEMS.register(name, id -> {
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
            return new BlockItem(blockHolder.get(),
                new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
        });
    }
}
