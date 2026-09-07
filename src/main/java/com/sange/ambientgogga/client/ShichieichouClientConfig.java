package com.sange.ambientgogga.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Local visual controls for different shader packs. */
public final class ShichieichouClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MINIMUM_LIGHT;
    public static final ModConfigSpec.DoubleValue DUST_SIZE;
    public static final ModConfigSpec.IntValue DUST_PER_TICK;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("shichieichou");
        MINIMUM_LIGHT = builder.comment("Minimum local light on the butterfly (0-15). Lower this if your shader pack blooms too strongly.")
                .defineInRange("minimumLight", 8, 0, 15);
        DUST_SIZE = builder.comment("Scale of the fine dust motes.")
                .defineInRange("dustSize", 1.0D, 0.25D, 3.0D);
        DUST_PER_TICK = builder.comment("Maximum dust particles per butterfly per tick; 0 disables the trail.")
                .defineInRange("dustPerTick", 2, 0, 4);
        builder.pop();
        SPEC = builder.build();
    }

    private ShichieichouClientConfig() {
    }
}
