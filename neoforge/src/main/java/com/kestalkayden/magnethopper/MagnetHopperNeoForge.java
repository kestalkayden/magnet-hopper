package com.kestalkayden.magnethopper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(MagnetHopperNeoForge.MOD_ID)
public class MagnetHopperNeoForge {
    public static final String MOD_ID = "magnethopper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public MagnetHopperNeoForge(IEventBus modBus) {
        LOGGER.info("Initializing Magnet Hopper (NeoForge)");
    }
}
