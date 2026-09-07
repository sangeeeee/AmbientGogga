package com.sange.ambientgogga.client;

import java.util.List;

public final class FireflyTimingTest {
    public static void main(String[] args) {
        require(weight(12000) == 0 && weight(13000) == 0, "Daytime spawning");
        require(Math.abs(weight(15500) - 0.5) < 1E-9, "Dusk ramp");
        require(weight(18000) == 1 && weight(23000) == 1, "Night plateau");
        require(Math.abs(weight(23500) - 0.25) < 1E-9 && weight(24000) == 0, "Dawn fade");
        require(FireflyTiming.nightWeight(500, 23000, 0, 2000, 1000) == 1, "Window must cross midnight");
        require(FireflyTiming.nightWeight(0, 0, 0, 0, 0) == 0, "Empty window");
        for (long time : new long[]{Long.MIN_VALUE, -1, 0, 23999, 24000, Long.MAX_VALUE}) {
            double value = FireflyTiming.nightWeight(time, 13000, 24000, 24000, 24000);
            require(Double.isFinite(value) && value >= 0 && value <= 1, "Invalid extended timing");
        }
        for (int age = 0; age <= 600; age++) {
            double value = FireflyTiming.glow(age, 600, 20, 80, 0.88, 0.24, 0, 0.25);
            require(value >= 0 && value <= 0.88, "Glow exceeds peak or becomes negative");
        }
        require(FireflyTiming.glow(0, 600, 20, 80, 0.88, 0.24, 0, 0.25) == 0, "Birth fade");
        require(FireflyTiming.glow(600, 600, 20, 80, 0.88, 0.24, 0, 0.25) == 0, "Death fade");
        double a = FireflyTiming.glow(160, 600, 20, 80, 0.88, 0.24, 1, 0.25);
        double b = FireflyTiming.glow(240, 600, 20, 80, 0.88, 0.24, 1, 0.25);
        require(Math.abs(a-b) < 1E-9, "Hz must correspond to seconds, not radians per tick");
        List<String> realistic = List.of("greater_heat");
        int summer = 0, real = 0;
        for (String term : FireflySeasonMode.VALID_TERMS) {
            require(FireflySeasonMode.ALL_YEAR.allows(true, term, realistic), "ALL_YEAR rejected a term");
            if (FireflySeasonMode.SUMMER_ONLY.allows(true, term, realistic)) summer++;
            if (FireflySeasonMode.REALISTIC.allows(true, term, realistic)) real++;
        }
        require(summer == 6 && real == 1, "Season term membership");
        for (FireflySeasonMode mode : FireflySeasonMode.values()) require(mode.allows(false, null, realistic), "Optional mod became required");
        require(!FireflySeasonMode.REALISTIC.allows(true, null, realistic), "Unsynchronized calendar spawned fireflies");
        require(!FireflySeasonMode.SUMMER_ONLY.allows(true, "NONE", realistic), "Invalid dimension spawned seasonal fireflies");
        require(FireflySeasonMode.REALISTIC.allows(true, "GRAIN_IN_EAR", List.of("grain_in_ear")), "Custom phenology list ignored");
        System.out.println("Passed firefly timing, glow envelopes, frequency units, all 24 solar terms and optional-mod fallback.");
    }

    private static double weight(long t) { return FireflyTiming.nightWeight(t, 13000, 5000, 5000, 1000); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
