package com.sange.ambientgogga.client;

import com.sange.ambientgogga.client.compat.SeaFireWaveField;
import java.util.Arrays;
import java.util.Random;

/** Checks curve continuity, brightness fidelity, footprint reuse and shared grid interpolation. */
public final class SeaFireCurvesTest {
    private static volatile double sink;

    public static void main(String[] args) {
        double maxTurn = 0, maxError = 0;
        for (int track = 0; track < SeaFireCurves.TRACKS; track++) {
            for (int step = 0; step < SeaFireCurves.STEPS; step++) {
                double x = SeaFireCurves.x(track, step), z = SeaFireCurves.z(track, step);
                require(Math.abs(x * x + z * z - 1) < 1e-6, "Drift speed must remain normalized");
                double turn = Math.hypot(x - SeaFireCurves.x(track, step + 1), z - SeaFireCurves.z(track, step + 1));
                maxTurn = Math.max(maxTurn, turn);
                require(turn < 0.06, "Abrupt turn, including the loop seam");
            }
        }
        for (int i = 0; i < 100000; i++) {
            double phase = i * 0.11371;
            double wave = (Math.sin(phase * Math.PI * 2 / SeaFireCurves.STEPS) + 1) * 0.5;
            double expected = wave * wave * (3 - 2 * wave);
            maxError = Math.max(maxError, Math.abs(expected - SeaFireCurves.blink(phase)));
        }
        require(maxError < 0.000003, "Blink lookup changes brightness visibly");
        for (int lifetime : new int[]{1, 10, 80, 160, 1200}) {
            for (double hz : new double[]{0.01, 0.08, 0.16, 3}) {
                double initial = 1.7, phase = initial * SeaFireCurves.STEPS / (Math.PI * 2);
                for (int age = 0; age <= lifetime; age++) {
                    double actual = 0.975 * (0.75 + 0.25 * SeaFireCurves.blink(phase)) * SeaFireCurves.fade(age, lifetime);
                    double expected = FireflyTiming.glow(age, lifetime, Math.min(20, lifetime), Math.min(40, lifetime),
                            0.975, 0.75, initial, hz);
                    require(Math.abs(actual - expected) < 1e-6, "Lifetime or configured blink frequency changed");
                    phase += hz * SeaFireCurves.STEPS / 20;
                    if (phase >= SeaFireCurves.STEPS) phase -= SeaFireCurves.STEPS;
                }
            }
        }
        var random = new Random(413);
        int reused = 0;
        for (int i = 0; i < 100000; i++) {
            double x = random.nextDouble() * 40 - 20, z = random.nextDouble() * 40 - 20;
            double nx = x + random.nextDouble() * 0.04 - 0.02, nz = z + random.nextDouble() * 0.04 - 0.02;
            double size = 0.001 + random.nextDouble() * 0.099;
            if (SeaFireCurves.sameFootprint(x, z, nx, nz, size)) {
                reused++;
                require(Arrays.equals(cells(x, z, size), cells(nx, nz, size)), "Reused a different set of water cells");
            }
        }
        require(reused > 90000, "Normal drift should usually reuse its water footprint");
        require(!SeaFireCurves.sameFootprint(-0.02, 0.5, 0.02, 0.5, 0.01), "Negative coordinate boundary missed");
        require(!SeaFireCurves.sameFootprint(0.98, 0.98, 0.995, 0.995, 0.01), "Diagonal shore crossing missed");
        verifyField();
        System.out.printf("Passed Sea Fire curves: 32768 drift steps; max direction delta %.6f; blink error %.9f; %d/100000 footprints reusable; wave interpolation/reset.\n",
                maxTurn, maxError, reused);
        if (args.length > 0 && args[0].equals("--benchmark")) benchmark();
    }

    private static long[] cells(double x, double z, double size) {
        int minX = (int) Math.floor(x - size), maxX = (int) Math.floor(x + size);
        int minZ = (int) Math.floor(z - size), maxZ = (int) Math.floor(z + size);
        long[] result = new long[(maxX - minX + 1) * (maxZ - minZ + 1)];
        int i = 0;
        for (int cx = minX; cx <= maxX; cx++) for (int cz = minZ; cz <= maxZ; cz++)
            result[i++] = ((long) cx << 32) | (cz & 0xffffffffL);
        return result;
    }

    private static void verifyField() {
        var field = new SeaFireWaveField();
        float[] values = new float[SeaFireWaveField.SIDE * SeaFireWaveField.SIDE];
        for (int row = 0; row < 17; row++) for (int col = 0; col < 17; col++) values[row * 17 + col] = (row + col) * 0.01F;
        long now = 2_000_000_000L;
        field.update(-32, -32, 4, values, now);
        require(field.sample(0, 0) == 0, "First sample must fade in");
        field.frame(now + 50_000_000);
        require(Math.abs(field.sample(1, 2) - 0.08375) < 1e-6, "Shared temporal/spatial interpolation changed");
        float before = field.sample(1, 2);
        field.update(-28, -32, 4, values, now + 50_000_000);
        require(Math.abs(field.sample(1, 2) - before) < 1e-6, "Moving the grid caused a discontinuity");
        require(field.sample(-28, 0) == 0 && field.sample(100, 100) == 0, "Grid perimeter must fade to zero");
        field.frame(now + 2_000_000_000L);
        require(field.sample(1, 2) == 0, "Expired grid retained displacement");
        field.update(-32, -32, 4, values, now + 2_000_000_000L);
        require(field.sample(1, 2) == 0, "Restart reused expired displacement");
    }

    private static void benchmark() {
        for (int i = 0; i < 4; i++) { measure(false); measure(true); }
        double[] old = new double[7], current = new double[7];
        for (int i = 0; i < old.length; i++) {
            if ((i & 1) == 0) { old[i] = measure(false); current[i] = measure(true); }
            else { current[i] = measure(true); old[i] = measure(false); }
        }
        Arrays.sort(old); Arrays.sort(current);
        System.out.printf("CPU curve kernel, 12000 particles, warmed median ms/tick: old=%.4f, lookup=%.4f, ratio=%.2fx. Excludes world queries, engine, rendering and GPU.\n",
                old[3], current[3], old[3] / current[3]);
    }

    private static double measure(boolean lookup) {
        int count = 12000, ticks = 400;
        double[] phase = new double[count], heading = new double[count], cos = new double[count], sin = new double[count];
        var random = new Random(42);
        for (int i = 0; i < count; i++) {
            phase[i] = random.nextDouble() * Math.PI * 2;
            heading[i] = random.nextDouble() * Math.PI * 2;
            cos[i] = Math.cos(heading[i]) * 0.0015; sin[i] = Math.sin(heading[i]) * 0.0015;
        }
        double total = 0;
        long start = System.nanoTime();
        for (int tick = 0; tick < ticks; tick++) {
            for (int i = 0; i < count; i++) {
                int age = (tick + i) % 160;
                double glow;
                if (lookup) {
                    float x = SeaFireCurves.x(i & 15, tick + i), z = SeaFireCurves.z(i & 15, tick + i);
                    total += x * cos[i] - z * sin[i] + x * sin[i] + z * cos[i];
                    glow = 0.975 * (0.75 + 0.25 * SeaFireCurves.blink((phase[i] + age * 0.12 * Math.PI * 2 / 20)
                            * SeaFireCurves.STEPS / (Math.PI * 2))) * SeaFireCurves.fade(age, 160);
                } else {
                    heading[i] += Math.sin(phase[i] + age * 0.035) * 0.045;
                    total += (Math.cos(heading[i]) + Math.sin(heading[i])) * 0.0015;
                    glow = FireflyTiming.glow(age, 160, 20, 40, 0.975, 0.75, phase[i], 0.12);
                }
                total += glow;
            }
        }
        long elapsed = System.nanoTime() - start;
        sink = total;
        return elapsed / 1e6 / ticks;
    }

    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
