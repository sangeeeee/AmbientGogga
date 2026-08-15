package com.sange.ambientgogga.advancement;

import com.sange.ambientgogga.AmbientGogga;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ModAdvancements {
    private static final String TRIGGERED_CRITERION = "triggered";
    private static final ResourceLocation BUTTER_FLY = ResourceLocation.fromNamespaceAndPath(
            AmbientGogga.MODID,
            "butter_fly"
    );
    private static final ResourceLocation POCKET_A_MEMORY = ResourceLocation.fromNamespaceAndPath(
            AmbientGogga.MODID,
            "pocket_a_memory"
    );

    private ModAdvancements() {
    }

    public static void awardButterFly(ServerPlayer player) {
        award(player, BUTTER_FLY);
    }

    public static void awardPocketAMemory(ServerPlayer player) {
        award(player, POCKET_A_MEMORY);
    }

    private static void award(ServerPlayer player, ResourceLocation id) {
        AdvancementHolder advancement = player.server.getAdvancements().get(id);
        if (advancement != null) {
            player.getAdvancements().award(advancement, TRIGGERED_CRITERION);
        }
    }
}
