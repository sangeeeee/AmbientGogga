package com.sange.ambientgogga.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.Tags;

public final class SeaFireSurface {
    private record Sample(boolean valid, double height) { }
    private static final Sample INVALID = new Sample(false, 0);
    private static final SeaFireTickCache<Sample> SURFACES = new SeaFireTickCache<>(32768);
    private static final SeaFireTickCache<Integer> LIGHTS = new SeaFireTickCache<>(32768);
    private static final SeaFireTickCache<Integer> COLUMNS = new SeaFireTickCache<>(16384);
    private static final SeaFireTickCache<Boolean> BEACHES = new SeaFireTickCache<>(32768);
    private static ClientLevel owner;

    public static void reset(ClientLevel level) {
        if (owner == level) return;
        owner = level;
        SURFACES.clear(); LIGHTS.clear(); COLUMNS.clear(); BEACHES.clear();
    }

    public static boolean isBeach(ClientLevel level, BlockPos pos) {
        reset(level);
        long key = pos.asLong(), tick = level.getGameTime();
        Boolean cached = BEACHES.get(key, tick);
        if (cached != null) return cached;
        var biome = level.getBiome(pos);
        boolean result = biome.is(BiomeTags.IS_BEACH) || biome.is(Tags.Biomes.IS_BEACH);
        BEACHES.put(key, tick, result);
        return result;
    }

    /** Accept only exposed source-water blocks, excluding ice, waterlogging, roofs and submerged layers. */
    public static boolean isSurface(ClientLevel level, BlockPos pos) {
        return sample(level, pos.getX(), pos.getY(), pos.getZ()).valid;
    }

    public static boolean isSurface(ClientLevel level, int x, int y, int z) {
        return sample(level, x, y, z).valid;
    }

    private static Sample sample(ClientLevel level, int x, int y, int z) {
        reset(level);
        long key = BlockPos.asLong(x, y, z), tick = level.getGameTime();
        Sample cached = SURFACES.get(key, tick);
        if (cached != null) return cached;
        Sample result = INVALID;
        if (level.hasChunk(x >> 4, z >> 4)) {
            var pos = new BlockPos(x, y, z);
            var state = level.getBlockState(pos);
            var fluid = state.getFluidState();
            if (state.is(Blocks.WATER) && fluid.isSource()
                    && level.getBlockState(pos.above()).isAir() && level.canSeeSky(pos.above())) {
                result = new Sample(true, y + fluid.getHeight(level, pos) + 0.002);
            }
        }
        SURFACES.put(key, tick, result);
        return result;
    }

    public static BlockPos find(ClientLevel level, int x, int z) {
        reset(level);
        long key = BlockPos.asLong(x, 0, z), tick = level.getGameTime();
        Integer y = COLUMNS.get(key, tick);
        if (y == null) {
            y = level.hasChunk(x >> 4, z >> 4)
                    ? level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1 : Integer.MIN_VALUE;
            COLUMNS.put(key, tick, y);
        }
        if (y < level.getMinBuildHeight()) return null;
        var pos = new BlockPos(x, y, z);
        return isSurface(level, pos) ? pos : null;
    }

    public static double height(ClientLevel level, BlockPos pos) {
        // A tiny offset avoids coplanar flicker with the water mesh while remaining attached to its surface.
        return height(level, pos.getX(), pos.getY(), pos.getZ());
    }

    public static double height(ClientLevel level, int x, int y, int z) {
        return sample(level, x, y, z).height;
    }

    public static int light(ClientLevel level, int x, int y, int z) {
        reset(level);
        long key = BlockPos.asLong(x, y, z), tick = level.getGameTime();
        Integer cached = LIGHTS.get(key, tick);
        if (cached != null) return cached;
        int result = level.hasChunk(x >> 4, z >> 4) ? LevelRenderer.getLightColor(level, new BlockPos(x, y, z)) : 0;
        LIGHTS.put(key, tick, result);
        return result;
    }

    private SeaFireSurface() { }
}
