package com.sange.ambientgogga.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import java.nio.file.Files;
import java.nio.file.Path;

/** Exercise migration with actual files, mixed versions, malformed TOML and repeated starts. */
public final class ConfigMigrationTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(Path.of(args[0]), "config-migration-");
        Path good = Files.createDirectory(root.resolve("upgrade"));
        String client = "[shichieichou]\nminimumLight=7\ndustDensity=0.13\n[fireflies]\nmaxSpawnsPerSecond=23.0\n";
        String fireflies = "[fireflies]\nmaxSpawnsPerSecond=19.0\nmaxSpawnDistance=47.0\n[fireflies.seasons]\nfireflySeason='REALISTIC'\nrealisticSolarTerms=['lesser_heat','greater_heat']\n";
        String sea = "[seaFire]\nmaxParticles=7654\n[seaFire.motion]\ndriftSpeed=0.0023\n";
        Files.writeString(good.resolve(ClientConfig.FILE_NAME), client);
        Files.writeString(good.resolve("ambientgogga-fireflies-client.toml"), fireflies);
        Files.writeString(good.resolve("ambientgogga-sea-fire-client.toml"), sea);
        Files.writeString(good.resolve("ambientgogga-butterflies-common.toml"), "[butterflies]\nspawnSeason='NON_WINTER'\n");
        ConfigMigration.server(good);
        ConfigMigration.client(good);
        var result = read(good.resolve(ClientConfig.FILE_NAME));
        require(ClientConfig.SPEC.isCorrect(result), "Combined client file is invalid");
        require(result.<Integer>get("shichieichou.minimumLight") == 7, "Existing butterfly appearance lost");
        require(result.<Double>get("shichieichou.dustDensity") == 0.13, "Existing dust density lost");
        require(result.<Double>get("fireflies.maxSpawnsPerSecond") == 23, "New file must win conflicts");
        require(result.<Double>get("fireflies.maxSpawnDistance") == 47, "Legacy firefly value lost");
        require(result.<java.util.List<?>>get("fireflies.seasons.realisticSolarTerms").size() == 2, "Legacy list lost");
        require(result.<Integer>get("seaFire.maxParticles") == 7654, "Sea Fire count lost");
        require(result.<Double>get("seaFire.motion.driftSpeed") == 0.0023, "Nested Sea Fire value lost");
        require(read(good.resolve(ServerConfig.FILE_NAME)).<String>get("butterflies.spawnSeason").equals("NON_WINTER"), "Server season lost");
        require(Files.readString(good.resolve("ambientgogga-legacy/" + ClientConfig.FILE_NAME)).equals(client), "Client backup changed");
        require(Files.readString(good.resolve("ambientgogga-legacy/ambientgogga-fireflies-client.toml")).equals(fireflies), "Legacy backup changed");
        try (var files = Files.list(good)) { require(files.filter(Files::isRegularFile).count() == 2, "Expected only two active files"); }
        String first = Files.readString(good.resolve(ClientConfig.FILE_NAME));
        ConfigMigration.client(good); ConfigMigration.server(good);
        require(Files.readString(good.resolve(ClientConfig.FILE_NAME)).equals(first), "Repeated migration rewrote config");
        // Existing archives are never overwritten when old files reappear after a downgrade.
        Files.writeString(good.resolve("ambientgogga-fireflies-client.toml"), "[fireflies]\nmaxSpawnsPerSecond=5.0\n");
        ConfigMigration.client(good);
        require(read(good.resolve(ClientConfig.FILE_NAME)).<Double>get("fireflies.maxSpawnsPerSecond") == 23, "Downgrade input overwrote current values");
        require(Files.readString(good.resolve("ambientgogga-legacy/ambientgogga-fireflies-client.toml")).equals(fireflies), "Original archive overwritten");

        Path broken = Files.createDirectory(root.resolve("malformed-source"));
        Files.writeString(broken.resolve("ambientgogga-fireflies-client.toml"), "[broken");
        Files.writeString(broken.resolve("ambientgogga-sea-fire-client.toml"), sea);
        ConfigMigration.client(broken);
        require(Files.readString(broken.resolve("ambientgogga-fireflies-client.toml")).equals("[broken"), "Malformed input discarded");
        require(read(broken.resolve(ClientConfig.FILE_NAME)).<Integer>get("seaFire.maxParticles") == 7654, "Valid independent input not migrated");
        Path destination = Files.createDirectory(root.resolve("malformed-destination"));
        Files.writeString(destination.resolve(ClientConfig.FILE_NAME), "[broken");
        Files.writeString(destination.resolve("ambientgogga-fireflies-client.toml"), fireflies);
        ConfigMigration.client(destination);
        require(Files.readString(destination.resolve(ClientConfig.FILE_NAME)).equals("[broken"), "Malformed destination overwritten");
        require(Files.exists(destination.resolve("ambientgogga-fireflies-client.toml")), "Source removed after failed migration");

        require(ClientConfig.SPEC.isCorrect(read(Path.of(args[1]))), "Shipped client example invalid");
        require(ServerConfig.SPEC.isCorrect(read(Path.of(args[2]))), "Shipped server example invalid");
        System.out.println("Passed unified config migration: custom values, precedence, nested lists, backups, two active files, idempotence, malformed files and shipped examples.");
    }

    private static CommentedConfig read(Path path) throws Exception { return new TomlParser().parse(Files.readString(path)); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
