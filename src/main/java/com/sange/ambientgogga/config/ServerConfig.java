package com.sange.ambientgogga.config;

import com.sange.ambientgogga.world.ButterflySeasonMode;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Entity spawning is controlled by the logical server's configuration. */
public final class ServerConfig {
    public static final String FILE_NAME = "ambientgogga-server.toml";
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<ButterflySeasonMode> SEASON;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.comment("Butterfly natural spawning. In multiplayer, the server's configuration controls spawning.").push("butterflies");
        SEASON = builder.comment("ALL_YEAR allows spawning in all seasons. NON_WINTER blocks the six winter solar terms when Ecliptic Seasons is installed.",
                        "Applies only to ordinary butterflies. Shichieichou is unaffected. Ignored without Ecliptic Seasons; an unavailable calendar pauses restricted spawning.",
                        "Existing butterflies, commands, spawn eggs and releases from bottles are unaffected.")
                .translation("ambientgogga.configuration.butterflySeason")
                .defineEnum("spawnSeason", ButterflySeasonMode.ALL_YEAR);
        builder.pop();
        SPEC = builder.build();
    }

    private ServerConfig() { }
}
