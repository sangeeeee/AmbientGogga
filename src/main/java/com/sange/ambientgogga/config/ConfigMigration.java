package com.sange.ambientgogga.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Merge legacy values before FML loads the new spec; archive inputs only after a successful write. */
public final class ConfigMigration {
    public record Legacy(String file, String section) { }

    public static void client(Path directory) {
        migrate(directory, ClientConfig.FILE_NAME, ClientConfig.SPEC,
                new Legacy("ambientgogga-fireflies-client.toml", "fireflies"),
                new Legacy("ambientgogga-sea-fire-client.toml", "seaFire"));
    }

    public static void server(Path directory) {
        migrate(directory, ServerConfig.FILE_NAME, ServerConfig.SPEC,
                new Legacy("ambientgogga-butterflies-common.toml", "butterflies"));
    }

    public static void migrate(Path directory, String file, ModConfigSpec spec, Legacy... sources) {
        Path target = directory.resolve(file);
        List<Path> migrated = new ArrayList<>();
        Path temporary = null;
        try {
            // Leave malformed destinations to FML's normal recovery; never overwrite them here.
            CommentedConfig merged = Files.exists(target) ? read(target) : CommentedConfig.inMemory();
            for (Legacy source : sources) {
                Path input = directory.resolve(source.file);
                if (!Files.exists(input)) continue;
                try {
                    CommentedConfig old = read(input);
                    Object section = old.get(source.section);
                    if (!(section instanceof UnmodifiableConfig values)) continue;
                    mergeMissing(merged, List.of(source.section), values);
                    migrated.add(input);
                } catch (IOException | RuntimeException error) {
                    LogUtils.getLogger().warn("Unable to migrate legacy config {}; retaining the original file.", input, error);
                }
            }
            if (migrated.isEmpty()) return;
            spec.correct(merged);
            Files.createDirectories(directory);
            // Also preserve the old client file, which already contains Shichieichou settings.
            if (Files.exists(target)) Files.copy(target, archivePath(directory, file));
            temporary = Files.createTempFile(directory, file, ".tmp");
            Files.writeString(temporary, new TomlWriter().writeToString(merged));
            try { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            temporary = null;
            for (Path input : migrated) Files.move(input, archivePath(directory, input.getFileName().toString()));
            LogUtils.getLogger().info("Migrated legacy settings into {}; originals are in ambientgogga-legacy.", target);
        } catch (IOException | RuntimeException error) {
            LogUtils.getLogger().warn("Unable to complete config migration for {}; retained unmigrated originals.", target, error);
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

    private static CommentedConfig read(Path path) throws IOException {
        return new TomlParser().parse(Files.readString(path));
    }

    private static void mergeMissing(CommentedConfig target, List<String> prefix, UnmodifiableConfig source) {
        for (var entry : source.entrySet()) {
            List<String> path = new ArrayList<>(prefix);
            path.add(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof UnmodifiableConfig child) {
                if (!target.contains(path) || target.get(path) instanceof UnmodifiableConfig) mergeMissing(target, path, child);
            } else if (!target.contains(path)) {
                target.set(path, value);
            }
        }
    }

    private static Path archivePath(Path directory, String name) throws IOException {
        Path archive = directory.resolve("ambientgogga-legacy");
        Files.createDirectories(archive);
        Path path = archive.resolve(name);
        for (int suffix = 1; Files.exists(path); suffix++) path = archive.resolve(name + "." + suffix);
        return path;
    }

    private ConfigMigration() { }
}
