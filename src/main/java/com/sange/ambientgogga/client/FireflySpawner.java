package com.sange.ambientgogga.client;

import com.sange.ambientgogga.config.ClientConfig;

import com.sange.ambientgogga.AmbientGogga;
import com.sange.ambientgogga.compat.EclipticSeasonsCompat;
import com.sange.ambientgogga.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.Tags;

@EventBusSubscriber(modid = AmbientGogga.MODID, value = Dist.CLIENT)
public final class FireflySpawner {
    private static double spawnAccumulator;
    private static ClientLevel previousLevel;

    private FireflySpawner() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level != previousLevel) {
            previousLevel = level;
            spawnAccumulator = 0;
        }

        if (level == null || player == null || minecraft.isPaused()) {
            spawnAccumulator = 0.0;
            return;
        }

        double spawnsPerSecond = getSpawnsPerSecond(level);
        if (!ClientConfig.FIREFLIES.ENABLED.get() || spawnsPerSecond <= 0.0 || !isClearWeather(level)
                || !isAllowedSeason(level)) {
            spawnAccumulator = 0.0;
            return;
        }

        spawnAccumulator += spawnsPerSecond / 20.0;
        if (spawnAccumulator < 1.0) {
            return;
        }
        int count = (int) spawnAccumulator;
        spawnAccumulator -= count;
        RandomSource random = level.getRandom();
        int locationAttempts = ClientConfig.FIREFLIES.LOCATION_ATTEMPTS.get();
        // Bounded candidate sampling also discovers nearby habitats across biome boundaries.
        for (int particle = 0; particle < count; particle++) {
            for (int attempt = 0; attempt < locationAttempts; attempt++) {
                if (trySpawnFirefly(level, player, random)) break;
            }
        }
    }

    private static double getSpawnsPerSecond(ClientLevel level) {
        return ClientConfig.FIREFLIES.SPAWNS_PER_SECOND.get() * FireflyTiming.nightWeight(level.getDayTime(),
                ClientConfig.FIREFLIES.NIGHT_START.get(), ClientConfig.FIREFLIES.RISE_TICKS.get(),
                ClientConfig.FIREFLIES.PLATEAU_TICKS.get(), ClientConfig.FIREFLIES.FALL_TICKS.get());
    }

    private static boolean isAllowedSeason(ClientLevel level) {
        FireflySeasonMode mode = ClientConfig.FIREFLIES.SEASON.get();
        if (mode == FireflySeasonMode.ALL_YEAR || !EclipticSeasonsCompat.isInstalled()) return true;
        return mode.allows(true, EclipticSeasonsCompat.currentTerm(level), ClientConfig.FIREFLIES.REALISTIC_TERMS.get());
    }

    private static boolean isClearWeather(ClientLevel level) {
        return !level.isRaining() && !level.isThundering();
    }

    private static boolean isFireflyHabitat(ClientLevel level, BlockPos spawnPos) {
        Holder<Biome> biome = level.getBiome(spawnPos);
        boolean isForestOrPlains = biome.is(Tags.Biomes.IS_FOREST)
                || biome.is(Tags.Biomes.IS_PLAINS);
        boolean isSnowCovered = biome.is(Tags.Biomes.IS_SNOWY)
                || biome.is(Tags.Biomes.IS_SNOWY_PLAINS);
        return isForestOrPlains && !isSnowCovered;
    }

    private static boolean trySpawnFirefly(ClientLevel level, LocalPlayer player, RandomSource random) {
        double angle = random.nextDouble() * Mth.TWO_PI;
        double minDistance = Math.min(ClientConfig.FIREFLIES.MIN_DISTANCE.get(), ClientConfig.FIREFLIES.MAX_DISTANCE.get());
        double maxDistance = Math.max(ClientConfig.FIREFLIES.MIN_DISTANCE.get(), ClientConfig.FIREFLIES.MAX_DISTANCE.get());
        double distance = minDistance + random.nextDouble() * (maxDistance - minDistance);
        int x = Mth.floor(player.getX() + Mth.cos((float) angle) * distance);
        int z = Mth.floor(player.getZ() + Mth.sin((float) angle) * distance);

        if (!level.hasChunk(x >> 4, z >> 4)) {
            return false;
        }

        int verticalRange = ClientConfig.FIREFLIES.VERTICAL_DISTANCE.get();
        int verticalOffset = random.nextInt(verticalRange + 1) - random.nextInt(verticalRange + 1);
        int y = player.getBlockY() + verticalOffset;
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
            return false;
        }

        BlockPos spawnPos = new BlockPos(x, y, z);
        double spawnX = spawnPos.getX() + 0.15 + random.nextDouble() * 0.70;
        double spawnY = verticalRange == 0 ? player.getY() : spawnPos.getY() + 0.15 + random.nextDouble() * 0.70;
        double spawnZ = spawnPos.getZ() + 0.15 + random.nextDouble() * 0.70;
        double horizontalX = spawnX - player.getX();
        double horizontalZ = spawnZ - player.getZ();
        if (horizontalX * horizontalX + horizontalZ * horizontalZ > maxDistance * maxDistance
                || horizontalX * horizontalX + horizontalZ * horizontalZ < minDistance * minDistance
                || Math.abs(spawnY - player.getY()) > verticalRange) {
            return false;
        }

        // Check the candidate's three-dimensional biome, never the player's biome.
        // Reject unsuitable habitats before collision, sky and ground queries.
        if (!isFireflyHabitat(level, spawnPos)) return false;
        BlockState spawnState = level.getBlockState(spawnPos);
        if (!spawnState.getFluidState().isEmpty()
                || !spawnState.getCollisionShape(level, spawnPos).isEmpty()
                || !level.canSeeSky(spawnPos)
                || !hasSolidGroundNearby(level, spawnPos)) {
            return false;
        }

        level.addParticle(ModParticles.FIREFLY.get(), spawnX, spawnY, spawnZ, 0.0, 0.0, 0.0);
        return true;
    }

    private static boolean hasSolidGroundNearby(ClientLevel level, BlockPos spawnPos) {
        BlockPos.MutableBlockPos groundPos = spawnPos.mutable();
        for (int distance = 1; distance <= ClientConfig.FIREFLIES.GROUND_DISTANCE.get(); distance++) {
            groundPos.setY(spawnPos.getY() - distance);
            BlockState groundState = level.getBlockState(groundPos);
            if (groundState.getFluidState().isEmpty()
                    && !groundState.getCollisionShape(level, groundPos).isEmpty()) {
                return true;
            }
        }

        return false;
    }
}
