package com.sange.ambientgogga.client;

import java.util.Locale;
import java.util.Set;

/** Pure density and calendar rules, shared with offline verification. */
public final class SeaFireRules {
    private static final Set<String> AUTUMN = Set.of("beginning_of_autumn", "end_of_heat", "white_dew",
            "autumnal_equinox", "cold_dew", "first_frost");

    public static double density(double distance, double extent) {
        if (distance <= 0) return 1;
        if (extent <= 0 || distance >= extent) return 0;
        double t = distance / extent;
        return 1 - t * t * (3 - 2 * t);
    }

    public static boolean allowsSeason(boolean installed, boolean autumnOnly, String term) {
        return !installed || !autumnOnly || term != null && AUTUMN.contains(term.toLowerCase(Locale.ROOT));
    }

    private SeaFireRules() { }
}
