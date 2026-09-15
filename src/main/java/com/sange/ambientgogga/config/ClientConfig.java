package com.sange.ambientgogga.config;

import com.sange.ambientgogga.client.FireflyClientConfig;
import com.sange.ambientgogga.client.SeaFireClientConfig;
import com.sange.ambientgogga.client.ShichieichouClientConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** All local visual settings share one registered spec and one file. */
public final class ClientConfig {
    public static final String FILE_NAME = "ambientgogga-client.toml";
    public static final ModConfigSpec SPEC;
    public static final ShichieichouClientConfig SHICHIEICHOU;
    public static final FireflyClientConfig FIREFLIES;
    public static final SeaFireClientConfig SEA_FIRE;

    static {
        var builder = new ModConfigSpec.Builder();
        SHICHIEICHOU = new ShichieichouClientConfig(builder);
        FIREFLIES = new FireflyClientConfig(builder);
        SEA_FIRE = new SeaFireClientConfig(builder);
        SPEC = builder.build();
    }

    private ClientConfig() { }
}
