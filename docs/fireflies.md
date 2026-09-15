# Firefly Configuration

Fireflies appear on clear nights in suitable, non-snowy forest and plains biomes. Spawn locations must be open to the sky, near solid ground, and within the configured range. Each candidate's biome is checked, so you can see fireflies in nearby suitable terrain even when standing in another biome.

**Install Ambient Gogga on both the server and clients.** Fireflies render locally, but the mod is not client-only.

## Configuration

Edit the `[fireflies]` sections in `config/ambientgogga-client.toml`. Durations use game ticks (20 per second). New particles use updated appearance and motion settings.

| Setting or section | Default | Purpose |
| --- | --- | --- |
| `enabled` | `true` | Enable natural spawning |
| `maxSpawnsPerSecond` | `10` | Peak spawn opportunities per second |
| `locationAttempts` | `24` | Maximum candidate checks per opportunity |
| `minSpawnDistance` / `maxSpawnDistance` | `1.5` / `56` | Horizontal distance from the player in blocks |
| `maxVerticalDistance` | `30` | Maximum height difference from the player |
| `maxHeightAboveGround` | `5` | Maximum distance above solid ground |
| `timing.*` | — | Start time and spawn-rate ramp durations |
| `appearance.*` | — | Size, brightness and blinking |
| `lifetime.*` | — | Lifetime and fade durations |
| `motion.*` | — | Movement, steering and roaming distance |

Actual numbers depend on suitable locations and Minecraft's particle setting. Small patches of suitable habitat may take several attempts to produce fireflies. Their glow does not illuminate nearby blocks.

## Optional seasons

With **Ecliptic Seasons** installed, choose a spawning season:

```toml
[fireflies.seasons]
fireflySeason = "ALL_YEAR"
realisticSolarTerms = ["greater_heat"]
```

- `ALL_YEAR`: no seasonal restriction (default).
- `SUMMER_ONLY`: allow the six summer solar terms.
- `REALISTIC`: allow only the terms listed in `realisticSolarTerms`. The default, Greater Heat, is a midsummer preset rather than a universal biological calendar. Use `["lesser_heat", "greater_heat"]` for a longer window.

Restrictions use the world's solar terms, not your computer's date. They are ignored without Ecliptic Seasons. If the calendar is unavailable, restricted spawning waits; existing particles fade out normally. Weather, habitat and nighttime requirements still apply in every mode.
