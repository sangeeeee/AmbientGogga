package com.sange.ambientgogga.client;

import com.sange.ambientgogga.config.ClientConfig;

import com.sange.ambientgogga.AmbientGogga;
import com.sange.ambientgogga.compat.EclipticSeasonsCompat;
import com.sange.ambientgogga.client.compat.SeaFireShaderCompat;
import com.sange.ambientgogga.client.particle.SeaFireParticle;
import com.sange.ambientgogga.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = AmbientGogga.MODID, value = Dist.CLIENT)
public final class SeaFireSpawner {
    private static ClientLevel previousLevel;
    private static SeaFireBeachField field = new SeaFireBeachField();
    private static double accumulator;
    private static ClientLevel activityLevel;
    private static long activityTick = Long.MIN_VALUE;
    private static double cachedActivity;

    public static double activity(ClientLevel level) {
        long tick = level.getGameTime();
        if (activityLevel == level && activityTick == tick) return cachedActivity;
        activityLevel = level;
        activityTick = tick;
        cachedActivity = FireflyTiming.nightWeight(level.getDayTime(), ClientConfig.SEA_FIRE.START.get(),
                ClientConfig.SEA_FIRE.RISE.get(), ClientConfig.SEA_FIRE.PEAK.get(), ClientConfig.SEA_FIRE.FALL.get());
        return cachedActivity;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != previousLevel) {
            previousLevel = level;
            field = new SeaFireBeachField();
            accumulator = 0;
            SeaFireSurface.reset(level);
            activityLevel = null;
            activityTick = Long.MIN_VALUE;
            SeaFireShaderCompat.reset();
        }
        if (level != null) SeaFireShaderCompat.refresh();
        if (level == null || mc.player == null || mc.isPaused() || !ClientConfig.SEA_FIRE.ENABLED.get()) {
            accumulator = 0;
            return;
        }
        double night = activity(level);
        boolean restricted = ClientConfig.SEA_FIRE.AUTUMN_ONLY.get() && EclipticSeasonsCompat.isInstalled();
        if (night <= 0 || restricted && !SeaFireRules.allowsSeason(true, true, EclipticSeasonsCompat.currentTerm(level))) {
            accumulator = 0;
            return;
        }
        accumulator += ClientConfig.SEA_FIRE.SPAWN_RATE.get() * night / 20;
        int candidates = (int) accumulator;
        accumulator -= candidates;
        var camera = mc.gameRenderer.getMainCamera().getPosition();
        var random = level.getRandom();
        double radius = ClientConfig.SEA_FIRE.RADIUS.get();
        SeaFireBeachField.Source beachWater = (x, y, z) -> {
            if (!level.hasChunk(x >> 4, z >> 4)) return false;
            var pos = new BlockPos(x, y, z);
            return SeaFireSurface.isBeach(level, pos) && SeaFireSurface.isSurface(level, pos);
        };
        for (int i = 0; i < candidates; i++) {
            if (!SeaFireParticle.hasCapacity(level)) break;
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(random.nextDouble()) * radius;
            double x = camera.x + Math.cos(angle) * distance;
            double z = camera.z + Math.sin(angle) * distance;
            var pos = SeaFireSurface.find(level, Mth.floor(x), Mth.floor(z));
            if (pos == null || Math.abs(camera.y - pos.getY()) > 24) continue;
            // Rejected density samples are consumed, never retried, preserving the shoreline gradient.
            if (random.nextDouble() >= field.weight(beachWater, SeaFireSurface.isBeach(level, pos),
                    pos.getY(), x, z, ClientConfig.SEA_FIRE.BLEND_DISTANCE.get(), level.getGameTime())) continue;
            level.addParticle(ModParticles.SEA_FIRE.get(), x, SeaFireSurface.height(level, pos), z, 0, 0, 0);
        }
    }

    private SeaFireSpawner() { }
}
