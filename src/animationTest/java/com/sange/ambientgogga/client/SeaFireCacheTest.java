package com.sange.ambientgogga.client;

import java.util.ArrayList;
import java.util.Random;

/** Validate shared-query lifetimes and exact density against a brute-force irregular shoreline. */
public final class SeaFireCacheTest {
    public static void run() {
        var cache = new SeaFireTickCache<Integer>(1);
        require(cache.get(0, 0) == null, "Empty tick-zero cache must miss");
        cache.put(1, 10, 7);
        require(cache.get(1, 10) == 7, "Same-cell query must reuse data");
        cache.put(2, 10, 9);
        require(cache.get(1, 10) == null, "Collisions must not return another cell");
        require(cache.get(2, 11) == null && cache.get(2, 9) == null, "Another tick must refresh data");
        cache.clear();
        require(cache.get(2, 10) == null, "World reset must discard cached values");

        Random random = new Random(771);
        var seeds = new ArrayList<int[]>();
        for (int i = 0; i < 45; i++) seeds.add(new int[]{random.nextInt(80) - 40, random.nextInt(80) - 40});
        SeaFireBeachField.Source source = (x,y,z) -> y == 62 && seeds.stream().anyMatch(s -> s[0] == x && s[1] == z);
        var field = new SeaFireBeachField();
        for (int i = 0; i < 3000; i++) {
            double x = random.nextDouble() * 120 - 60, z = random.nextDouble() * 120 - 60;
            double extent = switch (i % 3) { case 0 -> 1.5; case 1 -> 16; default -> 64; };
            double nearest = extent;
            for (int[] seed : seeds) nearest = Math.min(nearest,
                    SeaFireBeachField.distanceToRectangle(x, z, seed[0], seed[1], 1));
            double expected = SeaFireRules.density(nearest, extent);
            double actual = field.weight(source, false, 62, x, z, extent, 0);
            require(Math.abs(actual - expected) < 1e-12, "Neighborhood pruning changed density: " + i);
        }

        // Exercise the actual primitive cache with the same packed keys used for water blocks.
        long[] particles = new long[12000];
        for (int i = 0; i < particles.length; i++) {
            double angle = random.nextDouble() * Math.PI * 2, radius = Math.sqrt(random.nextDouble()) * 28;
            int x = (int) Math.floor(Math.cos(angle) * radius), z = (int) Math.floor(Math.sin(angle) * radius);
            particles[i] = ((long) x & 0x3ffffffL) << 38 | ((long) z & 0x3ffffffL) << 12 | 62;
        }
        var cells = new SeaFireTickCache<Integer>(32768);
        int loads = 0;
        for (int request = 0; request < 3; request++) {
            for (long cell : particles) {
                if (cells.get(cell, 1) == null) { loads++; cells.put(cell, 1, 1); }
            }
        }
        require(loads < 5000, "Shared water-grid queries unexpectedly degenerated");
        System.out.println("Passed Sea Fire cache: collisions, tick expiry, world reset, 3000 exact density comparisons; 36000 shared requests required " + loads + " cell loads.");
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
