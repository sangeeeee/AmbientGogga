# Ambient Gogga

Ambient Gogga（氛围小虫）is a NeoForge mod for Minecraft 1.21.1 that adds small critters to make the world feel more alive and atmospheric.

**This is not a client-only mod. Install Ambient Gogga on both the server and every player's client.** Butterflies are actual entities, and the mod also adds items that require server-side support. The particle effects render locally, but the complete mod must be installed on both sides.

## Features

- Fireflies appear as lightweight client-side particles at night.
- Bioluminescence brings a blue glow to still surface water in beach biomes at night. Tiny square particles drift slowly and shimmer gently, becoming more abundant as night falls and fading toward dawn. Their density gradually decreases beyond beach boundaries for a smooth transition into neighboring waters.
- Bioluminescence brightness, density, size, timing and shoreline blending are configurable. The glow is visual and does not illuminate nearby blocks. Optional Ecliptic Seasons integration can restrict it to autumn. See [the bioluminescence guide](docs/sea-fire.md).
- Firefly glow, size, blinking, spawning and timing are client-configurable. Optional Ecliptic Seasons integration provides all-year, summer-only and configurable realistic spawning. See [the firefly configuration guide](docs/fireflies.md).
- Butterflies spawn naturally in suitable biomes, fly through the environment, rest on flowers, and react to players and weather.
- Up to three butterflies can be captured in one butterfly bottle and released together while preserving their variants.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1 or newer
- Java 21
- Ambient Gogga installed on **both the server and clients** in multiplayer

## Credits and third-party resources

Ambient Gogga was inspired by [Illuminations](https://www.curseforge.com/minecraft/mc-mods/illuminations) by **doctor4t**, particularly its atmospheric firefly effects.

The butterfly feature was inspired by [Terra Incognita: The Unknown Land](https://www.curseforge.com/minecraft/mc-mods/terraincognita) by **azmalent**. Butterfly texture resources from the older Minecraft 1.18.2 version of Terra Incognita are used in this project. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for details.

## Configuration

Configuration is grouped into `config/ambientgogga-client.toml` for local particles and appearance, and `config/ambientgogga-server.toml` for entity spawning. Legacy files are merged automatically and archived in `config/ambientgogga-legacy/`. See [configuration details](docs/configuration.md).

## Building

Run the Gradle build from the project root:

```shell
./gradlew build
```

The compiled mod is written to `build/libs/`.

## License

Ambient Gogga's original source code and original assets are available under the [MIT License](LICENSE). Third-party resources retain their respective upstream licenses and are not relicensed by the Ambient Gogga MIT license.
