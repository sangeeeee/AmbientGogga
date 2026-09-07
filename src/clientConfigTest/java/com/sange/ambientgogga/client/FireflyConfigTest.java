package com.sange.ambientgogga.client;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Validate the real config schema, round-trip TOML, and loading without the optional API. */
public final class FireflyConfigTest {
    public static void main(String[] args) throws Exception {
        CommentedConfig config = CommentedConfig.inMemory();
        FireflyClientConfig.SPEC.correct(config);
        require(FireflyClientConfig.SPEC.isCorrect(config), "Generated config is invalid");
        require(config.<Object>get("fireflies.seasons.fireflySeason").toString().equals("ALL_YEAR"), "Default season changed");
        require(config.<Double>get("fireflies.appearance.maxSize") < 0.060, "Maximum size did not shrink");
        require(config.<Double>get("fireflies.appearance.maxPeakAlpha") < 1.0, "Peak glow did not dim");
        String text = new TomlWriter().writeToString(config);
        CommentedConfig parsed = new TomlParser().parse(text);
        require(FireflyClientConfig.SPEC.isCorrect(parsed), "TOML round-trip loses schema values");
        for (FireflySeasonMode mode : FireflySeasonMode.values()) {
            parsed.set("fireflies.seasons.fireflySeason", mode.name());
            require(FireflyClientConfig.SPEC.isCorrect(parsed), "Season choice rejected: " + mode);
        }
        parsed.set("fireflies.appearance.maxSize", -1);
        parsed.set("fireflies.seasons.realisticSolarTerms", List.of("not_a_solar_term"));
        require(!FireflyClientConfig.SPEC.isCorrect(parsed), "Bad settings accepted");
        FireflyClientConfig.SPEC.correct(parsed);
        require(FireflyClientConfig.SPEC.isCorrect(parsed), "Bad settings were not corrected");
        try {
            Class.forName("com.teamtea.eclipticseasons.api.EclipticSeasonsApi");
            throw new AssertionError("Compile-only Ecliptic Seasons leaked into runtime");
        } catch (ClassNotFoundException expected) {
            Class.forName("com.sange.ambientgogga.client.compat.EclipticSeasonsCompat");
        }
        Path output = Path.of(args[0]);
        Files.createDirectories(output.getParent());
        Files.writeString(output, text);
        System.out.println("Passed real firefly config, TOML round-trip, invalid-value repair and optional API absent at runtime.");
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
