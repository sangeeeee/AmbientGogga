package com.sange.ambientgogga.client.model;

/** Per-butterfly simulation; render passes sample it without advancing the clock. */
public final class ShichieichouPose {
    private final int size = ShichieichouAnimation.ROWS * ShichieichouAnimation.COLUMNS * 3;
    private final ShichieichouTailSimulation[] tails = {
            new ShichieichouTailSimulation(size), new ShichieichouTailSimulation(size)};
    private final float[][] target = {new float[size], new float[size]};
    private int lastTick = Integer.MIN_VALUE;
    private double previousX, previousY, previousZ;
    private float previousYaw, previousScale;

    public void sample(float[][] output, float age, int entityId, float speed, float turn,
                       double x, double y, double z, float yaw, float scale) {
        int tick = (int) Math.floor(age);
        double dx = x - this.previousX, dy = y - this.previousY, dz = z - this.previousZ;
        boolean reset = this.lastTick == Integer.MIN_VALUE || tick < this.lastTick || tick - this.lastTick > 5
                || dx * dx + dy * dy + dz * dz > 4.0D;
        if (reset) {
            for (int side = 0; side < 2; side++) {
                ShichieichouAnimation.fill(this.target[side], ShichieichouAnimation.phase(tick, entityId), speed, turn, side == 0 ? 1 : -1);
                this.tails[side].reset(this.target[side]);
            }
        } else if (tick > this.lastTick) {
            float radians = (float) Math.toRadians(yaw);
            float cos = (float) Math.cos(radians), sin = (float) Math.sin(radians);
            float units = 16.0F / Math.max(0.01F, scale);
            for (int side = 0; side < 2; side++) {
                this.tails[side].rebase((float) (cos * dx + sin * dz) * units, (float) -dy * units,
                        (float) (sin * dx - cos * dz) * units,
                        (float) Math.toRadians(this.previousYaw - yaw), this.previousScale / scale);
                for (int step = this.lastTick + 1; step <= tick; step++) {
                    float phase = ShichieichouAnimation.phase(step, entityId);
                    ShichieichouAnimation.fill(this.target[side], phase, speed, turn, side == 0 ? 1 : -1);
                    this.tails[side].tick(this.target[side], phase, speed, side == 0 ? 1 : -1);
                }
            }
        }
        if (reset || tick != this.lastTick) {
            this.previousX = x; this.previousY = y; this.previousZ = z;
            this.previousYaw = yaw; this.previousScale = scale;
            this.lastTick = tick;
        }
        for (int side = 0; side < 2; side++) {
            // One tick of render interpolation, shared by wings, body and tails.
            ShichieichouAnimation.fill(output[side], ShichieichouAnimation.phase(age - 1, entityId), speed, turn, side == 0 ? 1 : -1);
            this.tails[side].sample(output[side], age - tick);
        }
    }
}
