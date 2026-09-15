package com.sange.ambientgogga.world;

import java.util.Locale;

/** Calendar restrictions apply only to natural spawning with the optional seasonal mod. */
public enum ButterflySeasonMode {
    ALL_YEAR, NON_WINTER;

    public boolean allows(boolean installed, String term) {
        if (!installed || this == ALL_YEAR) return true;
        if (term == null) return false;
        return switch (term.toLowerCase(Locale.ROOT)) {
            case "beginning_of_spring", "rain_water", "insects_awakening", "spring_equinox", "fresh_green", "grain_rain",
                    "beginning_of_summer", "lesser_fullness", "grain_in_ear", "summer_solstice", "lesser_heat", "greater_heat",
                    "beginning_of_autumn", "end_of_heat", "white_dew", "autumnal_equinox", "cold_dew", "first_frost" -> true;
            // Winter terms and unavailable calendars (including NONE) do not allow restricted spawning.
            default -> false;
        };
    }
}
