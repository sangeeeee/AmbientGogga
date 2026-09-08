package com.sange.ambientgogga.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Share exact nearest-source candidates across a four-block tile without quantizing spawn density. */
public final class SeaFireBeachField {
    @FunctionalInterface
    public interface Source { boolean isBeachWater(int x, int y, int z); }
    private record Key(int x, int y, int z) { }
    private record NeighborhoodKey(int x, int y, int z, double extent) { }
    private record Seed(int x, int z) { }
    private record Tile(long expires, Seed[] seeds) { }
    private final Map<Key, Tile> tiles = new HashMap<>();
    private final Map<NeighborhoodKey, Tile> neighborhoods = new HashMap<>();

    public double weight(Source source, boolean inBeach, int y, double x, double z, double extent, long now) {
        if (inBeach) return 1;
        if (extent <= 0) return 0;
        int tx = Math.floorDiv((int) Math.floor(x), 4), tz = Math.floorDiv((int) Math.floor(z), 4);
        var key = new NeighborhoodKey(tx, y, tz, extent);
        Tile neighborhood = neighborhoods.get(key);
        if (neighborhood == null || neighborhood.expires <= now) {
            neighborhood = neighborhood(source, tx, tz, y, extent, now);
            if (neighborhoods.size() >= 2048) neighborhoods.clear();
            neighborhoods.put(key, neighborhood);
        }
        double nearestSquared = extent * extent;
        for (Seed seed : neighborhood.seeds) {
            nearestSquared = Math.min(nearestSquared, distanceSquared(x, z, seed.x, seed.z, 1));
        }
        return SeaFireRules.density(Math.sqrt(nearestSquared), extent);
    }

    private Tile neighborhood(Source source, int tx, int tz, int y, double extent, long now) {
        int left = tx * 4, top = tz * 4;
        int minX = Math.floorDiv((int) Math.floor(left - extent), 4);
        int maxX = Math.floorDiv((int) Math.floor(left + 4 + extent), 4);
        int minZ = Math.floorDiv((int) Math.floor(top - extent), 4);
        int maxZ = Math.floorDiv((int) Math.floor(top + 4 + extent), 4);
        var found = new ArrayList<Seed>();
        double upperSquared = extent * extent;
        long expires = Long.MAX_VALUE;
        for (int sx = minX; sx <= maxX; sx++) {
            for (int sz = minZ; sz <= maxZ; sz++) {
                Tile tile = seeds(source, sx, sz, y, now);
                expires = Math.min(expires, tile.expires);
                for (Seed seed : tile.seeds) {
                    if (rectangleDistanceSquared(left, top, seed) > extent * extent) continue;
                    found.add(seed);
                    // This source bounds the nearest distance everywhere in the query tile.
                    double farthest = Math.max(Math.max(distanceSquared(left, top, seed.x, seed.z, 1),
                                    distanceSquared(left + 4, top, seed.x, seed.z, 1)),
                            Math.max(distanceSquared(left, top + 4, seed.x, seed.z, 1),
                                    distanceSquared(left + 4, top + 4, seed.x, seed.z, 1)));
                    upperSquared = Math.min(upperSquared, farthest);
                }
            }
        }
        double bound = upperSquared;
        // Discard only sources that cannot win anywhere in the tile, preserving exact distances.
        found.removeIf(seed -> rectangleDistanceSquared(left, top, seed) > bound);
        return new Tile(expires, found.toArray(Seed[]::new));
    }

    private static double rectangleDistanceSquared(int left, int top, Seed seed) {
        double dx = Math.max(0, Math.max(left - seed.x - 1, seed.x - left - 4));
        double dz = Math.max(0, Math.max(top - seed.z - 1, seed.z - top - 4));
        return dx * dx + dz * dz;
    }

    private Tile seeds(Source source, int tx, int tz, int y, long now) {
        var key = new Key(tx, y, tz);
        Tile tile = tiles.get(key);
        if (tile != null && tile.expires > now) return tile;
        var found = new ArrayList<Seed>();
        for (int dx = 0; dx < 4; dx++) {
            for (int dz = 0; dz < 4; dz++) {
                int x = tx * 4 + dx, z = tz * 4 + dz;
                if (source.isBeachWater(x, y, z)) found.add(new Seed(x, z));
            }
        }
        if (tiles.size() >= 8192) tiles.clear();
        Tile result = new Tile(now + 80 + Math.floorMod(key.hashCode(), 40), found.toArray(Seed[]::new));
        tiles.put(key, result);
        return result;
    }

    private static double distanceSquared(double x, double z, double left, double top, double size) {
        double dx = Math.max(0, Math.max(left - x, x - left - size));
        double dz = Math.max(0, Math.max(top - z, z - top - size));
        return dx * dx + dz * dz;
    }

    static double distanceToRectangle(double x, double z, double left, double top, double size) {
        return Math.sqrt(distanceSquared(x, z, left, top, size));
    }
}
