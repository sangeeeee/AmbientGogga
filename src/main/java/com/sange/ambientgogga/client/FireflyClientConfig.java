package com.sange.ambientgogga.client;

import java.util.List;
import java.util.Locale;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Local firefly visuals and optional seasonal spawning. All tick durations use 20 ticks/second. */
public final class FireflyClientConfig {
    public static final String FILE_NAME = "ambientgogga-fireflies-client.toml";
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue SPAWNS_PER_SECOND, MIN_DISTANCE, MAX_DISTANCE;
    public static final ModConfigSpec.IntValue LOCATION_ATTEMPTS, VERTICAL_DISTANCE, GROUND_DISTANCE;
    public static final ModConfigSpec.IntValue NIGHT_START, RISE_TICKS, PLATEAU_TICKS, FALL_TICKS;
    public static final ModConfigSpec.DoubleValue MIN_SIZE, MAX_SIZE, MIN_PEAK_ALPHA, MAX_PEAK_ALPHA;
    public static final ModConfigSpec.DoubleValue MIN_BLINK_HZ, MAX_BLINK_HZ, MIN_GLOW;
    public static final ModConfigSpec.IntValue MINIMUM_LIGHT, MIN_LIFETIME, MAX_LIFETIME;
    public static final ModConfigSpec.IntValue MIN_FADE_IN, MAX_FADE_IN, MIN_FADE_OUT, MAX_FADE_OUT;
    public static final ModConfigSpec.DoubleValue MOTION_SPEED, MOTION_FREQUENCY, STEERING_FACTOR, ROAM_DISTANCE;
    public static final ModConfigSpec.IntValue MIN_STEERING_TICKS, MAX_STEERING_TICKS;
    public static final ModConfigSpec.EnumValue<FireflySeasonMode> SEASON;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> REALISTIC_TERMS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Client-side firefly settings. Durations use game ticks (20 ticks/second). Appearance settings affect newly spawned particles.").push("fireflies");
        ENABLED = b.comment("Whether fireflies spawn naturally.").define("enabled", true);
        SPAWNS_PER_SECOND = number(b, "maxSpawnsPerSecond", 10, 0, 120, "Maximum spawn attempts per second at the nightly peak; 0 disables spawning.");
        LOCATION_ATTEMPTS = integer(b, "locationAttempts", 24, 1, 128, "Maximum number of location attempts per particle.");
        MIN_DISTANCE = number(b, "minSpawnDistance", 1.5, 0, 128, "Minimum horizontal distance from the player in blocks; reversed bounds are swapped automatically.");
        MAX_DISTANCE = number(b, "maxSpawnDistance", 56, 0, 128, "Maximum horizontal distance from the player in blocks.");
        VERTICAL_DISTANCE = integer(b, "maxVerticalDistance", 30, 0, 128, "Maximum height difference from the player in blocks.");
        GROUND_DISTANCE = integer(b, "maxHeightAboveGround", 5, 1, 32, "Maximum distance above solid ground for a spawn location in blocks.");

        b.push("timing");
        NIGHT_START = integer(b, "nightStartTick", 13000, 0, 23999, "Spawn window start: 0 sunrise, 6000 noon, 12000 sunset, 18000 midnight.");
        RISE_TICKS = integer(b, "rampUpTicks", 5000, 0, 24000, "Ticks to increase the spawn rate from zero to its peak; 0 reaches the peak immediately.");
        PLATEAU_TICKS = integer(b, "peakTicks", 5000, 0, 24000, "Ticks to maintain the peak spawn rate.");
        FALL_TICKS = integer(b, "rampDownTicks", 1000, 0, 24000, "Ticks to decrease the spawn rate to zero; the window may wrap across midnight and lasts at most one day.");
        b.pop().push("appearance");
        MIN_SIZE = number(b, "minSize", 0.018, 0.001, 0.25, "Minimum particle quad half-size in blocks.");
        MAX_SIZE = number(b, "maxSize", 0.054, 0.001, 0.25, "Maximum particle quad half-size; reduced by about 10% from the previous value of 0.060.");
        MIN_PEAK_ALPHA = number(b, "minPeakAlpha", 0.70, 0, 1, "Lower bound of randomized peak brightness (alpha).");
        MAX_PEAK_ALPHA = number(b, "maxPeakAlpha", 0.88, 0, 1, "Upper bound of randomized peak brightness; the current mean peak alpha is about 13% lower than before.");
        MINIMUM_LIGHT = integer(b, "minimumLight", 14, 0, 15, "Minimum particle block light; preserves environmental light without illuminating nearby blocks. Previously fixed at full brightness.");
        MIN_BLINK_HZ = number(b, "minBlinkFrequencyHz", 0.17, 0.01, 3, "Minimum flashes per second; the current default is slightly faster than before.");
        MAX_BLINK_HZ = number(b, "maxBlinkFrequencyHz", 0.27, 0.01, 3, "Maximum flashes per second.");
        MIN_GLOW = number(b, "minimumGlowFraction", 0.24, 0, 1, "Brightness at the blink trough as a fraction of peak brightness; 0 allows complete darkness, 1 disables blinking.");
        b.pop().push("lifetime");
        MIN_LIFETIME = integer(b, "minLifetimeTicks", 240, 1, 12000, "Minimum particle lifetime in ticks.");
        MAX_LIFETIME = integer(b, "maxLifetimeTicks", 600, 1, 12000, "Maximum particle lifetime in ticks.");
        MIN_FADE_IN = integer(b, "minFadeInTicks", 20, 1, 12000, "Minimum fade-in duration after spawning in ticks.");
        MAX_FADE_IN = integer(b, "maxFadeInTicks", 50, 1, 12000, "Maximum fade-in duration after spawning in ticks.");
        MIN_FADE_OUT = integer(b, "minFadeOutTicks", 40, 1, 12000, "Minimum fade-out duration before disappearing in ticks.");
        MAX_FADE_OUT = integer(b, "maxFadeOutTicks", 80, 1, 12000, "Maximum fade-out duration before disappearing in ticks. Fade durations cannot exceed the particle lifetime.");
        b.pop().push("motion");
        MOTION_SPEED = number(b, "speedMultiplier", 1, 0, 4, "Flight speed multiplier; 0 keeps particles stationary.");
        MOTION_FREQUENCY = number(b, "swayFrequencyMultiplier", 1, 0, 4, "Flight sway frequency multiplier.");
        STEERING_FACTOR = number(b, "steeringFactor", 0.055, 0.001, 1, "Steering smoothing factor; smaller values produce gentler turns.");
        MIN_STEERING_TICKS = integer(b, "minDirectionChangeTicks", 45, 1, 1200, "Minimum interval between choosing new flight directions in ticks.");
        MAX_STEERING_TICKS = integer(b, "maxDirectionChangeTicks", 120, 1, 1200, "Maximum interval between choosing new flight directions in ticks.");
        ROAM_DISTANCE = number(b, "maxRoamDistance", 12, 0.1, 64, "Distance from the spawn position at which particles start returning, in blocks.");
        b.pop().push("seasons");
        SEASON = b.comment("Firefly season: ALL_YEAR = all year, SUMMER_ONLY = summer only, REALISTIC = realistic (Greater Heat phenology preset by default).",
                "Restricts natural spawning only when Ecliptic Seasons is installed; otherwise this setting has no effect.")
                .translation("ambientgogga.configuration.fireflySeason")
                .defineEnum("fireflySeason", FireflySeasonMode.ALL_YEAR);
        REALISTIC_TERMS = b.comment("Allowed solar terms in realistic mode. Greater Heat approximates a 15-day midsummer phenology window, not a universal firefly calendar.",
                "Adjust for the region or species, for example by adding lesser_heat (Lesser Heat) or grain_in_ear (Grain in Ear).")
                .defineListAllowEmpty("realisticSolarTerms", List.of("greater_heat"),
                        value -> value instanceof String name && FireflySeasonMode.VALID_TERMS.contains(name.toLowerCase(Locale.ROOT)));
        b.pop().pop();
        SPEC = b.build();
    }

    private static ModConfigSpec.IntValue integer(ModConfigSpec.Builder b, String key, int value, int min, int max, String comment) {
        return b.comment(comment).defineInRange(key, value, min, max);
    }

    private static ModConfigSpec.DoubleValue number(ModConfigSpec.Builder b, String key, double value, double min, double max, String comment) {
        return b.comment(comment).defineInRange(key, value, min, max);
    }

    private FireflyClientConfig() { }
}
