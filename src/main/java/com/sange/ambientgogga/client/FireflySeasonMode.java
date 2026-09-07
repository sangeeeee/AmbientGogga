package com.sange.ambientgogga.client;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public enum FireflySeasonMode {
    ALL_YEAR, SUMMER_ONLY, REALISTIC;

    public static final Set<String> SUMMER_TERMS = Set.of("beginning_of_summer", "lesser_fullness",
            "grain_in_ear", "summer_solstice", "lesser_heat", "greater_heat");
    public static final Set<String> VALID_TERMS = Set.of("beginning_of_spring", "rain_water", "insects_awakening",
            "spring_equinox", "fresh_green", "grain_rain", "beginning_of_summer", "lesser_fullness",
            "grain_in_ear", "summer_solstice", "lesser_heat", "greater_heat", "beginning_of_autumn",
            "end_of_heat", "white_dew", "autumnal_equinox", "cold_dew", "first_frost", "beginning_of_winter",
            "light_snow", "heavy_snow", "winter_solstice", "lesser_cold", "greater_cold");

    public boolean allows(boolean installed, String term, List<? extends String> realisticTerms) {
        if (!installed || this == ALL_YEAR) return true;
        if (term == null) return false; // No synchronized/valid seasonal calendar yet.
        String name = term.toLowerCase(Locale.ROOT);
        return this == SUMMER_ONLY ? SUMMER_TERMS.contains(name)
                : realisticTerms.stream().anyMatch(value -> name.equals(value.toLowerCase(Locale.ROOT)));
    }
}
