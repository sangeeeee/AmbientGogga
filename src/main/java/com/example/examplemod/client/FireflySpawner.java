package com.example.examplemod.client;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.Tags;

@EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class FireflySpawner {
    private static final int NIGHTFALL_START = 13_000;
    private static final int DEEP_NIGHT_START = 18_000;
    private static final int DAWN_START = 23_000;
    private static final double MAX_SPAWNS_PER_SECOND = 10.0;
    private static final int LOCATION_ATTEMPTS = 24;
    private static final double MIN_DISTANCE = 1.5;
    private static final double MAX_DISTANCE = 56.0;
    private static final int MAX_VERTICAL_DISTANCE = 30;
    private static final int MAX_HEIGHT_ABOVE_SOLID = 5;

    private static double spawnAccumulator;

    private FireflySpawner() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;

        if (level == null || player == null || minecraft.isPaused()) {
            spawnAccumulator = 0.0;
            return;
        }

        double spawnsPerSecond = getSpawnsPerSecond(level);
        if (spawnsPerSecond <= 0.0 || !isClearWeather(level) || !isFireflyHabitat(level, player)) {
            spawnAccumulator = 0.0;
            return;
        }

        spawnAccumulator += spawnsPerSecond / 20.0;
        if (spawnAccumulator < 1.0) {
            return;
        }
        spawnAccumulator -= 1.0;

        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < LOCATION_ATTEMPTS; attempt++) {
            if (trySpawnFirefly(level, player, random)) {
                return;
            }
        }
    }

    private static double getSpawnsPerSecond(ClientLevel level) {
        long timeOfDay = Math.floorMod(level.getDayTime(), Level.TICKS_PER_DAY);
        if (timeOfDay < NIGHTFALL_START) {
            return 0.0;
        }

        if (timeOfDay < DEEP_NIGHT_START) {
            double nightProgress = (double) (timeOfDay - NIGHTFALL_START)
                    / (DEEP_NIGHT_START - NIGHTFALL_START);
            return MAX_SPAWNS_PER_SECOND * smoothStep(nightProgress);
        }

        if (timeOfDay < DAWN_START) {
            return MAX_SPAWNS_PER_SECOND;
        }

        double dawnProgress = (double) (timeOfDay - DAWN_START)
                / (Level.TICKS_PER_DAY - DAWN_START);
        double remainingNight = Mth.clamp(1.0 - dawnProgress, 0.0, 1.0);
        return MAX_SPAWNS_PER_SECOND * remainingNight * remainingNight;
    }

    private static double smoothStep(double value) {
        double clamped = Mth.clamp(value, 0.0, 1.0);
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    private static boolean isClearWeather(ClientLevel level) {
        return !level.isRaining() && !level.isThundering();
    }

    private static boolean isFireflyHabitat(ClientLevel level, LocalPlayer player) {
        Holder<Biome> biome = level.getBiome(player.blockPosition());
        boolean isForestOrPlains = biome.is(Tags.Biomes.IS_FOREST)
                || biome.is(Tags.Biomes.IS_PLAINS);
        boolean isSnowCovered = biome.is(Tags.Biomes.IS_SNOWY)
                || biome.is(Tags.Biomes.IS_SNOWY_PLAINS);
        return isForestOrPlains && !isSnowCovered;
    }

    private static boolean trySpawnFirefly(ClientLevel level, LocalPlayer player, RandomSource random) {
        double angle = random.nextDouble() * Mth.TWO_PI;
        double distance = MIN_DISTANCE + random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
        int x = Mth.floor(player.getX() + Mth.cos((float) angle) * distance);
        int z = Mth.floor(player.getZ() + Mth.sin((float) angle) * distance);

        if (!level.hasChunk(x >> 4, z >> 4)) {
            return false;
        }

        int halfVerticalRange = MAX_VERTICAL_DISTANCE / 2;
        int verticalOffset = random.nextInt(MAX_VERTICAL_DISTANCE + 1) - halfVerticalRange
                + random.nextInt(MAX_VERTICAL_DISTANCE + 1) - halfVerticalRange;
        int y = player.getBlockY() + verticalOffset;
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
            return false;
        }

        BlockPos spawnPos = new BlockPos(x, y, z);
        BlockState spawnState = level.getBlockState(spawnPos);
        if (!spawnState.getFluidState().isEmpty()
                || !spawnState.getCollisionShape(level, spawnPos).isEmpty()
                || !level.canSeeSky(spawnPos)
                || !hasSolidGroundNearby(level, spawnPos)) {
            return false;
        }

        double spawnX = spawnPos.getX() + 0.15 + random.nextDouble() * 0.70;
        double spawnY = spawnPos.getY() + 0.15 + random.nextDouble() * 0.70;
        double spawnZ = spawnPos.getZ() + 0.15 + random.nextDouble() * 0.70;
        double horizontalX = spawnX - player.getX();
        double horizontalZ = spawnZ - player.getZ();
        if (horizontalX * horizontalX + horizontalZ * horizontalZ > MAX_DISTANCE * MAX_DISTANCE
                || Math.abs(spawnY - player.getY()) > MAX_VERTICAL_DISTANCE) {
            return false;
        }

        level.addParticle(ModParticles.FIREFLY.get(), spawnX, spawnY, spawnZ, 0.0, 0.0, 0.0);
        return true;
    }

    private static boolean hasSolidGroundNearby(ClientLevel level, BlockPos spawnPos) {
        BlockPos.MutableBlockPos groundPos = spawnPos.mutable();
        for (int distance = 1; distance <= MAX_HEIGHT_ABOVE_SOLID; distance++) {
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
