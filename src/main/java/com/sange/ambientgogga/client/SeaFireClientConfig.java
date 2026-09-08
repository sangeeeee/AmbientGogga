package com.sange.ambientgogga.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Local surface-water particles; no world light sources or server simulation. */
public final class SeaFireClientConfig {
    public static final String FILE_NAME = "ambientgogga-sea-fire-client.toml";
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, AUTUMN_ONLY;
    public static final ModConfigSpec.BooleanValue SHADER_WAVES, VEGETATION_WAVES;
    public static final ModConfigSpec.DoubleValue VEGETATION_STRENGTH, VEGETATION_MAX_HEIGHT;
    public static final ModConfigSpec.DoubleValue SPAWN_RATE, RADIUS, BLEND_DISTANCE, MIN_SIZE, MAX_SIZE;
    public static final ModConfigSpec.DoubleValue MIN_ALPHA, MAX_ALPHA, MIN_BLINK_HZ, MAX_BLINK_HZ, SPEED;
    public static final ModConfigSpec.DoubleValue MIN_GLOW;
    public static final ModConfigSpec.IntValue MAX_PARTICLES, START, RISE, PEAK, FALL, MIN_LIFETIME, MAX_LIFETIME, LIGHT;

    static {
        var b = new ModConfigSpec.Builder();
        b.comment("Client-side Sea Fire particles. Durations use game ticks (20 ticks/second). Appearance changes affect new particles.").push("seaFire");
        ENABLED = b.comment("Enable natural Sea Fire spawning.").define("enabled", true);
        SPAWN_RATE = number(b, "candidateSpawnsPerSecond", 4000, 0, 12000,
                "Uniform surface candidates per second at peak activity. Land, biome blending and particle settings reduce actual spawns.");
        RADIUS = number(b, "spawnRadius", 28, 4, 64, "Horizontal spawn radius around the camera in blocks.");
        BLEND_DISTANCE = number(b, "biomeBlendDistance", 16, 0, 64,
                "Distance in blocks to extend beyond beach water, with a smooth density falloff. Zero strictly limits spawning to beach biomes.");
        MAX_PARTICLES = integer(b, "maxParticles", 12000, 0, 16000, "Maximum tracked live Sea Fire particles per client; 0 disables spawning.");
        b.push("timing");
        START = integer(b, "nightStartTick", 12500, 0, 23999, "Activity window start: 0 sunrise, 12000 sunset, 18000 midnight.");
        RISE = integer(b, "rampUpTicks", 3500, 0, 24000, "Ticks to increase density from zero to its peak.");
        PEAK = integer(b, "peakTicks", 6000, 0, 24000, "Ticks at peak density.");
        FALL = integer(b, "rampDownTicks", 2000, 0, 24000, "Ticks to fade density to zero; the complete window is capped at one day.");
        b.pop().push("appearance");
        MIN_SIZE = number(b, "minSize", 0.006, 0.001, 0.1, "Minimum square half-size in blocks; most particles favor the smaller end of the range.");
        MAX_SIZE = number(b, "maxSize", 0.018, 0.001, 0.1, "Maximum square half-size in blocks; reversed size bounds are swapped.");
        MIN_ALPHA = number(b, "minPeakAlpha", 0.525, 0, 1, "Minimum peak opacity of the blue glow.");
        MAX_ALPHA = number(b, "maxPeakAlpha", 0.975, 0, 1, "Maximum peak opacity of the blue glow.");
        LIGHT = integer(b, "minimumLight", 11, 0, 15, "Minimum rendered block light. Particles never illuminate surrounding blocks.");
        MIN_BLINK_HZ = number(b, "minBlinkFrequencyHz", 0.08, 0.01, 3, "Minimum smooth flashes per second.");
        MAX_BLINK_HZ = number(b, "maxBlinkFrequencyHz", 0.16, 0.01, 3, "Maximum smooth flashes per second.");
        MIN_GLOW = number(b, "minimumGlowFraction", 0.75, 0, 1,
                "Blink trough brightness as a fraction of each particle's peak, excluding spawn/despawn and daylight fading; 1 disables blinking.");
        SHADER_WAVES = b.comment("Enable Iris motion integration. Compatible packs use the exact wave protocol; other packs may use experimental vegetation sampling below.")
                .define("shaderWaterFollowing", true);
        VEGETATION_WAVES = b.comment("For Iris packs without the exact wave protocol, sample vegetation-tag vertex motion on a shared 17x17 grid at up to 10 Hz. Experimental; no shader-pack edits. Unsupported shaders keep the vanilla surface.")
                .define("experimentalVegetationMotion", true);
        VEGETATION_STRENGTH = number(b, "vegetationMotionStrength", 0.5, 0, 2,
                "Scale the magnitude of sampled vertical vegetation motion. This approximates gentle floating, not the shader's actual water surface.");
        VEGETATION_MAX_HEIGHT = number(b, "vegetationMaxLift", 0.12, 0, 0.5,
                "Maximum visual lift above vanilla water in blocks. Downward plant bending is reflected upward to prevent sinking into flat water.");
        b.pop().push("motion");
        SPEED = number(b, "driftSpeed", 0.0015, 0, 0.02, "Surface drift speed in blocks per tick; 0 is stationary. Particles cannot cross onto land or flowing water.");
        MIN_LIFETIME = integer(b, "minLifetimeTicks", 80, 1, 1200, "Minimum lifetime in ticks.");
        MAX_LIFETIME = integer(b, "maxLifetimeTicks", 160, 1, 1200, "Maximum lifetime in ticks.");
        b.pop().push("seasons");
        AUTUMN_ONLY = b.comment("When Ecliptic Seasons is installed, restrict natural spawning to the six autumn solar terms. Ignored without that mod.")
                .define("autumnOnly", false);
        b.pop().pop();
        SPEC = b.build();
    }

    private static ModConfigSpec.DoubleValue number(ModConfigSpec.Builder b, String key, double value, double min, double max, String comment) {
        return b.comment(comment).defineInRange(key, value, min, max);
    }

    private static ModConfigSpec.IntValue integer(ModConfigSpec.Builder b, String key, int value, int min, int max, String comment) {
        return b.comment(comment).defineInRange(key, value, min, max);
    }

    private SeaFireClientConfig() { }
}
