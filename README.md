# Magnet Hopper

A hopper variant that pulls dropped items in a 3D radius before depositing them into the container below. Solves the universal "items despawn around my mob farm" pain in one dedicated block — no 30-mod ecosystem required.

## Tiers

| Tier | Recipe | Magnet radius | Volume covered |
|---|---|---|---|
| **Magnet Hopper** | 1 hopper + 4 copper ingots (cross pattern) | 1 block | 3×3×3 cube |
| **Advanced Magnet Hopper** | 1 magnet hopper + 4 gold ingots | 2 blocks | 5×5×5 cube |
| **Industrial Magnet Hopper** | 1 advanced magnet hopper + 4 diamonds | 3 blocks | 7×7×7 cube |

Each tier is a strict upgrade of the previous (recipe ladder enforces progression). Tier shows on the lid as copper / gold / diamond, plus a matching colored rim band on the upper portion of the bowl sides.

## Features

- **Magnet pull** — ItemEntities in the tier's 3D radius get vacuumed into the internal 5-slot storage and flow down into the container below.
- **Vanilla hopper behavior always active** — the magnet hopper is a strict superset of a normal hopper: it pulls from any container above and pushes to any container below at the standard 8-tick cadence.
- **Magnet on/off toggle** — UI button to disable just the magnet (block continues to act as a normal hopper).
- **5-slot filter** with whitelist/blacklist mode toggle. Click filter slot with item to ghost-set (item not consumed), empty-hand click to clear.
- **Redstone-disable** — applying a redstone signal pauses everything (matches vanilla hopper convention).
- **Placement preview** — hold a magnet hopper item in either hand and the radius cube draws as a cyan-green wireframe over wherever you're cursor-targeting. A warm-green contour follows the terrain along the footprint edges so the floor coverage is always legible.

## Requirements

- Minecraft **26.1.x**
- Java **25**
- Fabric Loader **0.18.4+** with **Fabric API**, *or* NeoForge **26.1+**

## Building

```bash
./gradlew buildAll
```

Produces:
- `fabric/build/libs/magnethopper-fabric-<version>.jar`
- `neoforge/build/libs/magnethopper-neoforge-<version>.jar`

Individual loaders: `./gradlew :fabric:build` or `./gradlew :neoforge:build`.

Dev clients: `./gradlew :fabric:runClient` or `./gradlew :neoforge:runClient`.

## Known limitations (v0.1)

- **Inventory not preserved on break**: items currently in the internal storage spill into the world when the block is broken by a player (you can pick them up). They don't carry over inside the item NBT for replacement.
- **Filter slots not preserved on break**: filter configuration is lost when the block is broken. Re-set after replacing.
- **GUI uses procedural rendering**: no custom PNG texture for the menu background — looks minimal but functional. Texture polish planned for v0.2.
- **Block facing**: all magnet hoppers face down only; the side-facing variant of vanilla hopper geometry isn't yet supported.

## License

CC0-1.0.
