package com.kestalkayden.magnethopper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kestalkayden.magnethopper.block.MagnetHopperBlockEntities;
import com.kestalkayden.magnethopper.block.MagnetHopperBlocks;
import com.kestalkayden.magnethopper.component.MagnetHopperComponents;
import com.kestalkayden.magnethopper.menu.MagnetHopperMenus;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
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
        MagnetHopperComponents.register();

        // Bind loader-agnostic refs so shared :common code reaches the registered objects.
        MagnetHopperRefs.MAGNET_HOPPER_BE = () -> MagnetHopperBlockEntities.MAGNET_HOPPER_BE;
        MagnetHopperRefs.MAGNET_HOPPER_MENU = () -> MagnetHopperMenus.MAGNET_HOPPER_MENU;
        MagnetHopperRefs.CONFIG = MagnetHopperComponents::CONFIG;

        // Expose our inventory to Fabric's item-transfer pipe API. ContainerStorage.of() respects
        // WorldlyContainer (our filter), so pipes can't push/pull blacklisted items in either direction.
        ItemStorage.SIDED.registerForBlockEntity(
            (be, dir) -> ContainerStorage.of(be, dir),
            MagnetHopperBlockEntities.MAGNET_HOPPER_BE);

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(output -> {
            output.insertAfter(Items.HOPPER,
                MagnetHopperBlocks.MAGNET_HOPPER_ITEM.getDefaultInstance(),
                MagnetHopperBlocks.ADVANCED_MAGNET_HOPPER_ITEM.getDefaultInstance(),
                MagnetHopperBlocks.INDUSTRIAL_MAGNET_HOPPER_ITEM.getDefaultInstance());
        });
    }
}
