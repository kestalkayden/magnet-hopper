package com.kestalkayden.magnethopper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kestalkayden.magnethopper.block.MagnetHopperBlockEntities;
import com.kestalkayden.magnethopper.block.MagnetHopperBlocks;
import com.kestalkayden.magnethopper.menu.MagnetHopperMenus;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;

public class MagnetHopperFabric implements ModInitializer {
    public static final String MOD_ID = "magnethopper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Magnet Hopper (Fabric)");

        MagnetHopperBlocks.register();
        MagnetHopperBlockEntities.register();
        MagnetHopperMenus.register();

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(output -> {
            output.insertAfter(Items.HOPPER,
                MagnetHopperBlocks.MAGNET_HOPPER_ITEM.getDefaultInstance(),
                MagnetHopperBlocks.ADVANCED_MAGNET_HOPPER_ITEM.getDefaultInstance(),
                MagnetHopperBlocks.INDUSTRIAL_MAGNET_HOPPER_ITEM.getDefaultInstance());
        });
    }
}
