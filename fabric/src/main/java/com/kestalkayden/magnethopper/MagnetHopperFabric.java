package com.kestalkayden.magnethopper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

public class MagnetHopperFabric implements ModInitializer {
    public static final String MOD_ID = "magnethopper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Magnet Hopper (Fabric)");
    }
}
