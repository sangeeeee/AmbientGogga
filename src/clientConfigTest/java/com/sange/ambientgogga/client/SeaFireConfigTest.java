package com.sange.ambientgogga.client;

import com.sange.ambientgogga.config.ClientConfig;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SeaFireConfigTest {
    public static void main(String[] args) throws Exception {
        var config = CommentedConfig.inMemory();
        ClientConfig.SPEC.correct(config);
        require(ClientConfig.SPEC.isCorrect(config), "Invalid default schema");
        String text = new TomlWriter().writeToString(config);
        require(ClientConfig.SPEC.isCorrect(new TomlParser().parse(text)), "Invalid TOML round-trip");
        require(!config.<Boolean>get("seaFire.seasons.autumnOnly"), "Default must be all year");
        require(config.<Boolean>get("seaFire.appearance.experimentalVegetationMotion"), "Experimental motion default missing");
        require(config.<Boolean>get("seaFire.appearance.shaderEmissive"), "Native emissive default missing");
        require(!com.sange.ambientgogga.client.compat.SeaFireShaderCompat.useEmissive(), "Iris-absent fallback must remain inactive");
        config.set("seaFire.appearance.shaderEmissiveStrength", 2.0);
        require(!ClientConfig.SPEC.isCorrect(config), "Unrepresentable vertex color strength accepted");
        ClientConfig.SPEC.correct(config);
        config.set("seaFire.appearance.vegetationMaxLift", -1);
        require(!ClientConfig.SPEC.isCorrect(config), "Negative visual lift accepted");
        ClientConfig.SPEC.correct(config);
        config.set("seaFire.seasons.autumnOnly", true);
        require(ClientConfig.SPEC.isCorrect(config), "Autumn mode must be accepted");
        config.set("seaFire.biomeBlendDistance", -1);
        require(!ClientConfig.SPEC.isCorrect(config), "Negative blend distance accepted");
        ClientConfig.SPEC.correct(config);
        require(ClientConfig.SPEC.isCorrect(config), "Invalid values were not repaired");
        Path output = Path.of(args[0]);
        Files.createDirectories(output.getParent());
        Files.writeString(output, text);
        verifyShaderProtocol(output.getParent());
        verifySurfaceViews();
        System.out.println("Passed Sea Fire config defaults, autumn option, TOML round-trip and invalid-value repair.");
    }

    private static void verifySurfaceViews() {
        double surface = 62 + 8.0 / 9;
        for (float size : new float[]{0.006F, 0.018F, 0.1F}) {
            for (float lift : new float[]{0, 0.12F, 0.5F}) {
                double above = SeaFireGeometry.bottom(surface + 0.002, surface + 1, size, lift);
                double below = SeaFireGeometry.bottom(surface + 0.002, surface - 1, size, lift);
                require(above > surface, "Above-water view must keep the quad above the depth surface");
                require(below + 2 * size < surface, "Underwater view must keep the whole quad inside water");
                require(Math.abs((above - surface) - (surface - below - 2 * size)) < 1e-6, "Surface offsets must be symmetric");
            }
        }
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
        require(!com.sange.ambientgogga.client.compat.SeaFireShaderCompat.usesDeferredEyes(zipPath), "Unrelated shader archive misidentified");
        var eyes = root.resolve("shaders/world0/gbuffers_spidereyes.fsh");
        var solid = root.resolve("shaders/program/gbuffers_all_solid.fsh");
        Files.createDirectories(eyes.getParent()); Files.createDirectories(solid.getParent());
        Files.writeString(eyes, "#define PROGRAM_GBUFFERS_SPIDEREYES\n#include \"/program/gbuffers_all_solid.fsh\"\n");
        Files.writeString(solid, "Photon Shader by SixthSurge\ngbuffer_data_0 = pack_unorm_2x8();\n");
        require(com.sange.ambientgogga.client.compat.SeaFireShaderCompat.usesDeferredEyes(root), "Deferred eye layout rejected");
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
