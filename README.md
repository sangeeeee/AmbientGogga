# Ambient Gogga

Ambient Gogga（氛围小虫）is a NeoForge mod for Minecraft 1.21.1 that adds small critters to make the world feel more alive and atmospheric.

## Features

- Fireflies appear as lightweight client-side particles at night.
- Butterflies spawn naturally in suitable biomes, fly through the environment, rest on flowers, and react to players and weather.
- Up to three butterflies can be captured in one butterfly bottle and released together while preserving their variants.
- Shichieichou has compact pixel-art wings, one hanging tail per side, rounded anatomy and curved antennae, a slower broad wing stroke, and fine sparkling dust. See [the visual rework notes](docs/shichieichou-rework.md) for settings and verification.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1 or newer
- Java 21

Shichieichou uses 256-pixel wing and 64-pixel body textures with the standard renderer; Sodium and external animation/modeling software are not required.

## Credits and third-party resources

Ambient Gogga was inspired by [Illuminations](https://www.curseforge.com/minecraft/mc-mods/illuminations) by **doctor4t**, particularly its atmospheric firefly effects.

The butterfly feature was inspired by [Terra Incognita: The Unknown Land](https://www.curseforge.com/minecraft/mc-mods/terraincognita) by **azmalent**. Butterfly texture resources from the older Minecraft 1.18.2 version of Terra Incognita are used in this project. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for details.

## Building

Run the Gradle build from the project root:

```shell
./gradlew build
```

The compiled mod is written to `build/libs/`.

## License

Ambient Gogga's original source code and original assets are available under the [MIT License](LICENSE). Third-party resources retain their respective upstream licenses and are not relicensed by the Ambient Gogga MIT license.
