# Bioluminescence

Tiny blue particles drift and shimmer on exposed, still beach water at night. Density rises after sunset, fades toward dawn, and blends gradually into neighboring waters. The glow is visual and does not illuminate blocks.

**Install Ambient Gogga on both the server and clients.** These effects render locally, but the mod is not client-only.

## Where to find it

Visit a beach at night. Unfrozen snowy beaches and modded biomes tagged as beaches also qualify; stony shores do not qualify by default. Flowing, covered, frozen and submerged water is excluded.

For a quick check, use `/locate biome minecraft:beach` and `/time set 18000`, then set Minecraft's particle setting to **All**.

## Configuration

Edit the `[seaFire]` sections in `config/ambientgogga-client.toml`. The internal `seaFire` name is retained for compatibility. Durations use game ticks (20 per second).

| Setting | Default | Purpose |
| --- | --- | --- |
| `enabled` | `true` | Enable natural spawning |
| `candidateSpawnsPerSecond` | `4000` | Peak candidate rate; actual spawns depend on suitable water |
| `maxParticles` | `12000` | Particle limit |
| `spawnRadius` | `28` | Horizontal range in blocks |
| `biomeBlendDistance` | `16` | Extension into neighboring waters; `0` disables blending |
| `timing.*` | — | Start time and density ramp durations |
| `appearance.*` | — | Size, brightness, blinking and shader integration |
| `motion.*` | — | Drift speed and lifetime |

New particles use updated appearance settings. The configuration file includes comments and allowed ranges.

## Seasons and shaders

To restrict spawning to autumn when **Ecliptic Seasons** is installed:

```toml
[seaFire.seasons]
autumnOnly = true
```

The default is all year. Restrictions use the world's solar terms, not your computer's date, and are ignored without Ecliptic Seasons.

Iris integration supports native emissive rendering where available; brightness and bloom depend on the shader pack. Experimental floating motion approximates surface movement and does not guarantee exact wave matching. Disable `appearance.experimentalVegetationMotion` if it looks wrong, or `appearance.shaderWaterFollowing` to disable all wave following. Unsupported integrations fall back to ordinary surface particles.
