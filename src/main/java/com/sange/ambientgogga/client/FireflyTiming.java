package com.sange.ambientgogga.client;

/** Pure timing functions shared by particle/spawn code and offline checks. */
public final class FireflyTiming {
    private FireflyTiming() { }

    public static double nightWeight(long time, int start, int riseTicks, int plateauTicks, int fallTicks) {
        int elapsed = (int) Math.floorMod(Math.floorMod(time, 24000) - start, 24000);
        int rise = Math.min(riseTicks, 24000);
        int plateau = Math.min(plateauTicks, 24000 - rise);
        int fall = Math.min(fallTicks, 24000 - rise - plateau);
        if (elapsed < rise) return smooth((double) elapsed / rise);
        elapsed -= rise;
        if (elapsed < plateau) return 1;
        elapsed -= plateau;
        if (elapsed >= fall) return 0;
        double remaining = 1 - (double) elapsed / fall;
        return remaining * remaining;
    }

    public static double glow(int age, int lifetime, int fadeIn, int fadeOut,
                              double peak, double minimum, double phase, double frequencyHz) {
        double wave = (Math.sin(phase + age * frequencyHz * Math.PI * 2 / 20) + 1) * 0.5;
        double fade = smooth((double) age / Math.max(1, fadeIn))
                * smooth((double) (lifetime - age) / Math.max(1, fadeOut));
        return peak * (minimum + (1 - minimum) * smooth(wave)) * fade;
    }

    private static double smooth(double v) {
        v = Math.max(0, Math.min(1, v));
        return v * v * (3 - 2 * v);
    }
}
