# Magnet Hopper

A hopper that pulls dropped items in a radius before depositing into the container below. Solves the "items despawning around my mob farm" pain in one block.

## Requirements

- Minecraft **26.1.x**
- Java **25**
- Fabric Loader **0.18.4+** with **Fabric API**, *or* NeoForge **26.1+**

## Status

Scaffolding only. No items registered yet.

## Building

```bash
./gradlew buildAll
```

Produces:
- `fabric/build/libs/magnethopper-fabric-<version>.jar`
- `neoforge/build/libs/magnethopper-neoforge-<version>.jar`

Individual loaders: `./gradlew :fabric:build` or `./gradlew :neoforge:build`.

Dev clients: `./gradlew :fabric:runClient` or `./gradlew :neoforge:runClient`.

## License

CC0-1.0.
