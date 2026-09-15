package com.sange.ambientgogga.world;

import com.sange.ambientgogga.config.ServerConfig;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.sange.ambientgogga.client.FireflySeasonMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/** Validate all solar terms, optional-mod fallback and the actual server configuration. */
public final class ButterflySeasonConfigTest {
    public static void main(String[] args) throws Exception {
        Set<String> winter = Set.of("beginning_of_winter", "light_snow", "heavy_snow",
                "winter_solstice", "lesser_cold", "greater_cold");
        int allowed = 0;
        for (String term : FireflySeasonMode.VALID_TERMS) {
            require(ButterflySeasonMode.ALL_YEAR.allows(true, term), "All-year mode rejected " + term);
            require(ButterflySeasonMode.NON_WINTER.allows(false, term), "Missing optional mod restricted " + term);
            boolean result = ButterflySeasonMode.NON_WINTER.allows(true, term);
            require(result == !winter.contains(term), "Incorrect season for " + term);
            require(result == ButterflySeasonMode.NON_WINTER.allows(true, term.toUpperCase(java.util.Locale.ROOT)),
                    "API enum capitalization changed the result");
            if (result) allowed++;
        }
        require(allowed == 18, "Expected eighteen non-winter solar terms");
        for (String unknown : new String[]{null, "NONE", "invalid"}) {
            require(!ButterflySeasonMode.NON_WINTER.allows(true, unknown), "Unknown calendar must wait");
            require(ButterflySeasonMode.NON_WINTER.allows(false, unknown), "No mod must retain ordinary spawning");
            require(ButterflySeasonMode.ALL_YEAR.allows(true, unknown), "All-year mode must not require a calendar");
        }
        var config = CommentedConfig.inMemory();
        ServerConfig.SPEC.correct(config);
        require(config.<Object>get("butterflies.spawnSeason").toString().equals("ALL_YEAR"), "Default must be all year");
        for (ButterflySeasonMode mode : ButterflySeasonMode.values()) {
            config.set("butterflies.spawnSeason", mode.name());
            var roundTrip = new TomlParser().parse(new TomlWriter().writeToString(config));
            require(ServerConfig.SPEC.isCorrect(roundTrip), "TOML rejected " + mode);
        }
        config.set("butterflies.spawnSeason", "INVALID");
        require(!ServerConfig.SPEC.isCorrect(config), "Invalid season accepted");
        ServerConfig.SPEC.correct(config);
        require(config.<Object>get("butterflies.spawnSeason").toString().equals("ALL_YEAR"), "Invalid season not repaired");
        var example = new TomlParser().parse(Files.readString(Path.of(args[0])));
        require(ServerConfig.SPEC.isCorrect(example), "Shipped config example is invalid");
        try {
            Class.forName("com.teamtea.eclipticseasons.api.EclipticSeasonsApi");
            throw new AssertionError("Compile-only seasonal API leaked into runtime");
        } catch (ClassNotFoundException expected) {
            Class.forName("com.sange.ambientgogga.compat.EclipticSeasonsCompat");
            Class.forName("com.sange.ambientgogga.world.ButterflySeasons");
        }
        System.out.println("Passed butterfly seasons: all 24 terms, 18 non-winter terms, missing/unknown calendar, server config round-trip, repair and shipped example.");
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
