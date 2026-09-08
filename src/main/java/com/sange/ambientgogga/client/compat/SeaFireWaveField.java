package com.sange.ambientgogga.client.compat;

/** A small shared displacement grid, interpolated in space and time without particle allocations. */
public final class SeaFireWaveField {
    public static final int SIDE = 17;
    private final float[] previous = new float[SIDE * SIDE];
    private final float[] target = new float[SIDE * SIDE];
    private final float[] resampled = new float[SIDE * SIDE];
    private double originX, originZ, spacing;
    private long changed;
    private boolean valid;
    private float blend;

    public void update(double x, double z, double step, float[] heights, long now) {
        for (int row = 0; row < SIDE; row++) {
            for (int col = 0; col < SIDE; col++) {
                int i = row * SIDE + col;
                resampled[i] = sample(x + col * step, z + row * step);
            }
        }
        System.arraycopy(resampled, 0, previous, 0, previous.length);
        System.arraycopy(heights, 0, target, 0, target.length);
        originX = x; originZ = z; spacing = step; changed = now;
        blend = 0;
        valid = true;
    }

    /** Called once per render frame. Old results expire instead of leaving frozen offsets behind. */
    public void frame(long now) {
        if (valid && now - changed > 1_000_000_000L) clear();
        blend = Math.max(0, Math.min(1, (now - changed) / 100_000_000F));
    }

    public float sample(double x, double z) {
        if (!valid) return 0;
        double gx = (x - originX) / spacing, gz = (z - originZ) / spacing;
        if (gx < 0 || gz < 0 || gx >= SIDE - 1 || gz >= SIDE - 1) return 0;
        int col = (int) gx, row = (int) gz, i = row * SIDE + col;
        float fx = (float) (gx - col), fz = (float) (gz - row);
        float a = value(i), b = value(i + 1), c = value(i + SIDE), d = value(i + SIDE + 1);
        // Fade at the perimeter when the camera crosses into a new grid.
        float edge = (float) Math.min(1, Math.min(Math.min(gx, gz), Math.min(SIDE - 1 - gx, SIDE - 1 - gz)));
        return ((a + (b - a) * fx) * (1 - fz) + (c + (d - c) * fx) * fz) * edge;
    }

    private float value(int i) { return previous[i] + (target[i] - previous[i]) * blend; }
    public void clear() { valid = false; }
}
