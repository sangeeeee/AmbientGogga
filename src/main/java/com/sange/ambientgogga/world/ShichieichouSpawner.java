package com.sange.ambientgogga.world;

import com.sange.ambientgogga.entity.ModEntities;
import com.sange.ambientgogga.entity.Shichieichou;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Independent low-frequency spawner for the MISC-category Shichieichou. */
public final class ShichieichouSpawner {
    private static final int ATTEMPT_INTERVAL_TICKS = 10 * 20;
    private static final int ATTEMPT_CHANCE = 120;
    private static final int MAX_NEARBY = 2;
    private static final double NEARBY_RADIUS = 64.0D;
    private static final int MIN_SPAWN_DISTANCE = 24;
    private static final int MAX_SPAWN_DISTANCE = 48;

    private ShichieichouSpawner() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ShichieichouSpawner::onLevelTick);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD
                || !level.isNight()
                || level.getGameTime() % ATTEMPT_INTERVAL_TICKS != 0L) {
            return;
        }

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }
            RandomSource random = level.getRandom();
            if (random.nextInt(ATTEMPT_CHANCE) != 0
                    || level.getEntitiesOfClass(
                            Shichieichou.class,
                            player.getBoundingBox().inflate(NEARBY_RADIUS)
                    ).size() >= MAX_NEARBY) {
                continue;
            }
            trySpawn(level, player, random);
        }
    }

    private static void trySpawn(ServerLevel level, ServerPlayer player, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        int distance = MIN_SPAWN_DISTANCE
                + random.nextInt(MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE + 1);
        int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos spawnPos = new BlockPos(x, surfaceY + 1 + random.nextInt(4), z);
        if (!level.hasChunkAt(spawnPos)
                || !level.getWorldBorder().isWithinBounds(spawnPos)
                || !level.isEmptyBlock(spawnPos)
                || !level.isEmptyBlock(spawnPos.above())) {
            return;
        }

        Shichieichou butterfly = ModEntities.SHICHIEICHOU.get().create(level);
        if (butterfly == null) {
            return;
        }
        butterfly.moveTo(
                spawnPos.getX() + 0.5D,
                spawnPos.getY() + random.nextDouble() * 0.5D,
                spawnPos.getZ() + 0.5D,
                random.nextFloat() * 360.0F,
                0.0F
        );
        if (!level.noCollision(butterfly)) {
            butterfly.discard();
            return;
        }

        butterfly.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(spawnPos),
                MobSpawnType.NATURAL,
                null
        );
        level.addFreshEntity(butterfly);
    }
}
