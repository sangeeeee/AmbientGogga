package com.sange.ambientgogga.client;

/** Shared periodic drift and blink curves. Only initialization evaluates trigonometric functions. */
public final class SeaFireCurves {
    public static final int TRACKS = 16;
    public static final int STEPS = 2048;
    private static final int MASK = STEPS - 1;
    private static final float[] X = new float[TRACKS * STEPS];
    private static final float[] Z = new float[TRACKS * STEPS];
    private static final float[] BLINK = new float[STEPS];

    static {
        for (int step = 0; step < STEPS; step++) {
            double t = step * (Math.PI * 2 / STEPS);
            BLINK[step] = (float) smooth((Math.sin(t) + 1) * 0.5);
            for (int track = 0; track < TRACKS; track++) {
                double phase = track * 2.399963229728653;
                double heading = 1.05 * Math.sin(t * (7 + track % 4) + phase)
                        + 0.4 * Math.sin(t * (13 + track % 5) - phase * 1.7);
                X[track * STEPS + step] = (float) Math.cos(heading);
                Z[track * STEPS + step] = (float) Math.sin(heading);
            }
        }
    }

    public static float x(int track, int step) { return X[track * STEPS + (step & MASK)]; }
    public static float z(int track, int step) { return Z[track * STEPS + (step & MASK)]; }

    /** Phase is measured in table samples and must be nonnegative. Interpolation also wraps at the seam. */
    public static double blink(double phase) {
        int step = (int) phase;
        float a = BLINK[step & MASK], b = BLINK[(step + 1) & MASK];
        return a + (b - a) * (phase - step);
    }

    public static double fade(int age, int lifetime) {
        return smooth((double) age / Math.min(20, lifetime))
                * smooth((double) (lifetime - age) / Math.min(40, lifetime));
    }

    /** Matching footprints may reuse a validation performed during this same tick. */
    public static boolean sameFootprint(double x, double z, double nextX, double nextZ, double size) {
        return Math.floor(x - size) == Math.floor(nextX - size)
                && Math.floor(x + size) == Math.floor(nextX + size)
                && Math.floor(z - size) == Math.floor(nextZ - size)
                && Math.floor(z + size) == Math.floor(nextZ + size);
    }

    private static double smooth(double v) {
        v = Math.max(0, Math.min(1, v));
        return v * v * (3 - 2 * v);
    }

    private SeaFireCurves() { }
}
