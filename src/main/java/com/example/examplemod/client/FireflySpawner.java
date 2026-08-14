package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class FireflySpawner {
    private static final int NIGHT_START = 13_000;
    private static final int NIGHT_END = 23_000;
    private static final int ATTEMPT_INTERVAL_TICKS = 2;
    private static final float SPAWN_CHANCE_PER_ATTEMPT = 0.35F;
    private static final double MIN_DISTANCE = 3.0;
    private static final double MAX_DISTANCE = 12.0;
    private static final double MIN_HEIGHT_OFFSET = 0.35;
    private static final double EXTRA_HEIGHT_OFFSET = 2.15;

    private static int ticksUntilAttempt;

    private FireflySpawner() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;

        if (level == null || player == null || minecraft.isPaused()) {
            ticksUntilAttempt = 0;
            return;
        }

        if (++ticksUntilAttempt < ATTEMPT_INTERVAL_TICKS) {
            return;
        }
        ticksUntilAttempt = 0;

        if (!isNight(level)) {
            return;
        }

        RandomSource random = level.getRandom();
        if (random.nextFloat() > SPAWN_CHANCE_PER_ATTEMPT) {
            return;
        }

        spawnFirefly(level, player, random);
    }

    private static boolean isNight(ClientLevel level) {
        long timeOfDay = Math.floorMod(level.getDayTime(), Level.TICKS_PER_DAY);
        return timeOfDay >= NIGHT_START && timeOfDay <= NIGHT_END;
    }

    private static void spawnFirefly(ClientLevel level, LocalPlayer player, RandomSource random) {
        double angle = random.nextDouble() * Mth.TWO_PI;
        double minimumArea = MIN_DISTANCE * MIN_DISTANCE;
        double maximumArea = MAX_DISTANCE * MAX_DISTANCE;
        double distance = Math.sqrt(minimumArea + random.nextDouble() * (maximumArea - minimumArea));
        double spawnX = player.getX() + Mth.cos((float) angle) * distance;
        double spawnY = player.getY() + MIN_HEIGHT_OFFSET + random.nextDouble() * EXTRA_HEIGHT_OFFSET;
        double spawnZ = player.getZ() + Mth.sin((float) angle) * distance;

        level.addParticle(ModParticles.FIREFLY.get(), spawnX, spawnY, spawnZ, 0.0, 0.0, 0.0);
    }
}
