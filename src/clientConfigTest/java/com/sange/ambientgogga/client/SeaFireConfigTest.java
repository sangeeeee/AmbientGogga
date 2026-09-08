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
        require(config.<Boolean>get("seaFire.appearance.experimentalVegetationMotion"), "Experimental motion default missing");
        config.set("seaFire.appearance.vegetationMaxLift", -1);
        require(!SeaFireClientConfig.SPEC.isCorrect(config), "Negative visual lift accepted");
        SeaFireClientConfig.SPEC.correct(config);
        config.set("seaFire.seasons.autumnOnly", true);
        require(SeaFireClientConfig.SPEC.isCorrect(config), "Autumn mode must be accepted");
        config.set("seaFire.biomeBlendDistance", -1);
        require(!SeaFireClientConfig.SPEC.isCorrect(config), "Negative blend distance accepted");
        SeaFireClientConfig.SPEC.correct(config);
        require(SeaFireClientConfig.SPEC.isCorrect(config), "Invalid values were not repaired");
        Path output = Path.of(args[0]);
        Files.createDirectories(output.getParent());
        Files.writeString(output, text);
        verifyShaderProtocol(output.getParent());
        System.out.println("Passed Sea Fire config defaults, autumn option, TOML round-trip and invalid-value repair.");
    }

    private static void verifyShaderProtocol(Path directory) throws Exception {
        var root = directory.resolve("sea-fire-protocol-fixture");
        var marker = root.resolve(com.sange.ambientgogga.client.compat.SeaFireShaderCompat.MARKER_PATH);
        Files.createDirectories(marker.getParent());
        Files.writeString(marker, "ambientgogga.seaFireWaveProtocol=2\n");
        require(!com.sange.ambientgogga.client.compat.SeaFireShaderCompat.supports(root), "Unknown wave protocol accepted");
        Files.writeString(marker, com.sange.ambientgogga.client.compat.SeaFireShaderCompat.PROTOCOL + "\n");
        require(com.sange.ambientgogga.client.compat.SeaFireShaderCompat.supports(root), "Compatible directory rejected");
        var zipPath = directory.resolve("sea-fire-protocol-fixture.zip");
        try (var zip = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipPath))) {
            zip.putNextEntry(new java.util.zip.ZipEntry(com.sange.ambientgogga.client.compat.SeaFireShaderCompat.MARKER_PATH));
            zip.write(Files.readAllBytes(marker));
            zip.closeEntry();
        }
        require(com.sange.ambientgogga.client.compat.SeaFireShaderCompat.supports(zipPath), "Compatible zip rejected");
        require(com.sange.ambientgogga.client.compat.SeaFireShaderCompat.markLight(0xB000B0) == 0xB000B0,
                "Shader-disabled rendering must retain original light coordinates");
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
