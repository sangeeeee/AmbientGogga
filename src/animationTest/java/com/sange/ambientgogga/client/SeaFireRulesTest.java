package com.sange.ambientgogga.client;

/** Exercise spatial falloff at real block boundaries, cache refresh and all solar terms. */
public final class SeaFireRulesTest {
    public static void main(String[] args) {
        SeaFireCacheTest.run();
        int autumn = 0;
        for (String term : FireflySeasonMode.VALID_TERMS) {
            if (SeaFireRules.allowsSeason(true, true, term)) autumn++;
            require(SeaFireRules.allowsSeason(false, true, term), "Absent optional mod must not restrict spawning");
            require(SeaFireRules.allowsSeason(true, false, term), "All-year mode must allow every term");
        }
        require(autumn == 6, "Expected six autumn terms");
        require(!SeaFireRules.allowsSeason(true, true, null), "Unsynchronized calendar must wait");
        require(!SeaFireRules.allowsSeason(true, true, "NONE"), "Invalid dimension must not spawn in autumn-only mode");
        require(SeaFireRules.allowsSeason(true, true, "WHITE_DEW"), "API enum case must be accepted");

        var field = new SeaFireBeachField();
        SeaFireBeachField.Source shore = (x, y, z) -> x < 0 && y == 62;
        double previous = 1;
        for (int i = 0; i <= 640; i++) {
            double x = i / 32.0;
            double weight = field.weight(shore, false, 62, x, -3.7, 16, 0);
            require(Math.abs(weight - SeaFireRules.density(x, 16)) < 1e-9, "Incorrect distance at a beach boundary");
            require(weight <= previous + 1e-12 && weight >= 0, "Density must decrease smoothly toward zero");
            previous = weight;
        }
        require(field.weight(shore, false, 62, 0.0001, 0.5, 16, 0) > 0.999999, "Visible density seam at biome boundary");
        require(field.weight(shore, false, 61, 1, 0.5, 16, 0) == 0, "Do not blend from water on another level");
        require(field.weight(shore, false, 62, 0.5, 0.5, 0, 0) == 0, "Zero extent must disable extension");
        require(field.weight(shore, true, 62, -0.5, 0.5, 0, 0) == 1, "Beach core must retain full density");
        require(field.weight((x,y,z) -> false, false, 62, 1, 0.5, 16, 200) == 0, "Changed water/unloaded chunks must expire from cache");

        var island = new SeaFireBeachField();
        SeaFireBeachField.Source single = (x,y,z) -> x == -1 && z == -1;
        double diagonal = island.weight(single, false, 62, 3, 4, 16, 0);
        require(Math.abs(diagonal - SeaFireRules.density(5, 16)) < 1e-9, "Use circular rather than square extension bounds");
        require(SeaFireSpawnerTiming.at(12500) == 0 && SeaFireSpawnerTiming.at(18000) == 1
                && SeaFireSpawnerTiming.at(24000) == 0, "Night activity endpoints changed");
        System.out.println("Passed Sea Fire: 641 shoreline samples, diagonal distance, strict boundary, water-height isolation, cache expiry and 24 solar terms.");
    }

    private static final class SeaFireSpawnerTiming {
        static double at(long time) { return FireflyTiming.nightWeight(time, 12500, 3500, 6000, 2000); }
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
