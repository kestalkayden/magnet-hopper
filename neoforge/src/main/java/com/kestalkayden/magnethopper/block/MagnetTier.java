package com.kestalkayden.magnethopper.block;

/**
 * Tier definitions for the magnet hopper family. Radius is the AABB.inflate() value used
 * to scan for nearby ItemEntities — radius 1 = 3x3x3 cube, radius 2 = 5x5x5, radius 3 = 7x7x7.
 */
public enum MagnetTier {
    BASIC(1),
    ADVANCED(2),
    INDUSTRIAL(3);

    public final int radius;

    MagnetTier(int radius) {
        this.radius = radius;
    }
}
