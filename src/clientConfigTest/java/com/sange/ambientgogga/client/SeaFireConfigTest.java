package com.sange.ambientgogga.client;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SeaFireConfigTest {
    public static void main(String[] args) throws Exception {
        var config = CommentedConfig.inMemory();
        SeaFireClientConfig.SPEC.correct(config);
        require(SeaFireClientConfig.SPEC.isCorrect(config), "Invalid default schema");
        String text = new TomlWriter().writeToString(config);
        require(SeaFireClientConfig.SPEC.isCorrect(new TomlParser().parse(text)), "Invalid TOML round-trip");
        require(!config.<Boolean>get("seaFire.seasons.autumnOnly"), "Default must be all year");
        config.set("seaFire.seasons.autumnOnly", true);
        require(SeaFireClientConfig.SPEC.isCorrect(config), "Autumn mode must be accepted");
        config.set("seaFire.biomeBlendDistance", -1);
        require(!SeaFireClientConfig.SPEC.isCorrect(config), "Negative blend distance accepted");
        SeaFireClientConfig.SPEC.correct(config);
        require(SeaFireClientConfig.SPEC.isCorrect(config), "Invalid values were not repaired");
        Path output = Path.of(args[0]);
        Files.createDirectories(output.getParent());
        Files.writeString(output, text);
        System.out.println("Passed Sea Fire config defaults, autumn option, TOML round-trip and invalid-value repair.");
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
