package com.sange.ambientgogga.client.model;

/** Fixed-tick Verlet ribbons with gravity, drag, and a downward angular constraint. */
public final class ShichieichouTailSimulation {
    public static final int SPINE_COLUMN = 8;
    private static final int SUBSTEPS = 4;
    private static final float DT = 1.0F / SUBSTEPS;
    private static final float DRAG = (float) Math.pow(0.78, DT);
    private static final float LINK_LENGTH = ShichieichouAnimation.TAIL_LENGTH / ShichieichouAnimation.TAIL_SEGMENTS;
    private final float[] current;
    private final float[] previous;
    private final float[] previousFrame;

    public ShichieichouTailSimulation(int size) {
        this.current = new float[size];
        this.previous = new float[size];
        this.previousFrame = new float[size];
    }

    public void reset(float[] pose) {
        System.arraycopy(pose, 0, this.current, 0, pose.length);
        System.arraycopy(pose, 0, this.previous, 0, pose.length);
        System.arraycopy(pose, 0, this.previousFrame, 0, pose.length);
    }

    /** Keep the particles in world space when the butterfly moves or turns. */
    public void rebase(float dx, float dy, float dz, float yawDelta, float scaleRatio) {
        float cos = (float) Math.cos(yawDelta), sin = (float) Math.sin(yawDelta);
        for (float[] points : new float[][]{this.current, this.previous, this.previousFrame}) {
            for (int i = 0; i < points.length; i += 3) {
                float x = points[i] * scaleRatio, z = points[i + 2] * scaleRatio;
                points[i] = x * cos + z * sin - dx;
                points[i + 1] = points[i + 1] * scaleRatio - dy;
                points[i + 2] = -x * sin + z * cos - dz;
            }
        }
    }

    public void tick(float[] target, float phase, float speed, int side) {
        System.arraycopy(this.current, 0, this.previousFrame, 0, this.current.length);
        for (int substep = 1; substep <= SUBSTEPS; substep++) {
            float blend = (float) substep / SUBSTEPS;
            for (int column = SPINE_COLUMN; column <= SPINE_COLUMN; column++) {
                int root = ShichieichouAnimation.index(ShichieichouAnimation.WING_SEGMENTS, column);
                for (int axis = 0; axis < 3; axis++) {
                    this.current[root + axis] = this.previousFrame[root + axis]
                            + (target[root + axis] - this.previousFrame[root + axis]) * blend;
                }
                for (int segment = 1; segment <= ShichieichouAnimation.TAIL_SEGMENTS; segment++) {
                    int i = ShichieichouAnimation.index(ShichieichouAnimation.WING_SEGMENTS + segment, column);
                    float flutter = (float) Math.sin(phase * 0.8F - segment * 0.42F + side * 0.4F);
                    for (int axis = 0; axis < 3; axis++) {
                        float position = this.current[i + axis];
                        float acceleration = axis == 0 ? flutter * 0.025F
                                : axis == 1 ? 0.18F : 0.018F + speed * 0.025F;
                        this.current[i + axis] += (position - this.previous[i + axis]) * DRAG
                                + acceleration * DT * DT;
                        this.previous[i + axis] = position;
                    }
                    constrain(this.current, i, i - ShichieichouAnimation.COLUMNS * 3);
                }
            }
        }
    }

    /** Interpolate at render time; pin to the exact membrane edge to prevent a seam. */
    public void sample(float[] output, float partialTick) {
        for (int column = SPINE_COLUMN; column <= SPINE_COLUMN; column++) {
            int root = ShichieichouAnimation.index(ShichieichouAnimation.WING_SEGMENTS, column);
            float ox = output[root] - lerp(root, partialTick);
            float oy = output[root + 1] - lerp(root + 1, partialTick);
            float oz = output[root + 2] - lerp(root + 2, partialTick);
            for (int row = ShichieichouAnimation.WING_SEGMENTS + 1; row < ShichieichouAnimation.ROWS; row++) {
                int i = ShichieichouAnimation.index(row, column);
                output[i] = lerp(i, partialTick) + ox;
                output[i + 1] = lerp(i + 1, partialTick) + oy;
                output[i + 2] = lerp(i + 2, partialTick) + oz;
                constrain(output, i, i - ShichieichouAnimation.COLUMNS * 3);
            }
        }
        // A single coherent ribbon surrounds the simulated spine. Independent
        // ropes per texture column can shear into triangular/twisted tips.
        int rootRow = ShichieichouAnimation.WING_SEGMENTS;
        int root = ShichieichouAnimation.index(rootRow, SPINE_COLUMN);
        int left = ShichieichouAnimation.index(rootRow, 0);
        int right = ShichieichouAnimation.index(rootRow, ShichieichouAnimation.COLUMNS - 1);
        float rx = output[right] - output[left], ry = output[right + 1] - output[left + 1], rz = output[right + 2] - output[left + 2];
        float width = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        rx /= width; ry /= width; rz /= width;
        float sign = rx < 0 ? -1 : 1;
        for (int row = rootRow + 1; row < ShichieichouAnimation.ROWS; row++) {
            int spine = ShichieichouAnimation.index(row, SPINE_COLUMN);
            int previous = ShichieichouAnimation.index(row - 1, SPINE_COLUMN);
            int next = ShichieichouAnimation.index(Math.min(row + 1, ShichieichouAnimation.ROWS - 1), SPINE_COLUMN);
            float tx = output[next] - output[previous], ty = output[next + 1] - output[previous + 1], tz = output[next + 2] - output[previous + 2];
            float length = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
            tx /= length; ty /= length; tz /= length;
            float progress = (float) (row - rootRow) / ShichieichouAnimation.TAIL_SEGMENTS;
            float nx = rx * (1 - progress) + sign * progress;
            float ny = ry * (1 - progress), nz = rz * (1 - progress);
            float dot = nx * tx + ny * ty + nz * tz;
            nx -= tx * dot; ny -= ty * dot; nz -= tz * dot;
            length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length < 1.0E-5F) {
                nx = ty; ny = -tx; nz = 0;
                length = (float) Math.sqrt(nx * nx + ny * ny);
                if (length < 1.0E-5F) { nx = 1; ny = 0; length = 1; }
            }
            nx /= length; ny /= length; nz /= length;
            for (int column = 0; column < ShichieichouAnimation.COLUMNS; column++) {
                if (column == SPINE_COLUMN) continue;
                int base = ShichieichouAnimation.index(rootRow, column);
                float dx = output[base] - output[root], dy = output[base + 1] - output[root + 1], dz = output[base + 2] - output[root + 2];
                float offset = (float) Math.sqrt(dx * dx + dy * dy + dz * dz)
                        * (column < SPINE_COLUMN ? -1 : 1) * (1 - progress * 0.30F);
                int i = ShichieichouAnimation.index(row, column);
                output[i] = output[spine] + nx * offset;
                output[i + 1] = output[spine + 1] + ny * offset;
                output[i + 2] = output[spine + 2] + nz * offset;
            }
        }
    }

    private float lerp(int i, float t) {
        return this.previousFrame[i] + (this.current[i] - this.previousFrame[i]) * t;
    }

    private static void constrain(float[] points, int i, int parent) {
        float dx = points[i] - points[parent];
        float dy = points[i + 1] - points[parent + 1];
        float dz = points[i + 2] - points[parent + 2];
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-6F) {
            dx = 0; dy = 1; dz = 0;
        } else {
            dx /= length; dy /= length; dz /= length;
        }
        // Even at the bottom of a broad downstroke, the tail remains below its root.
        if (dy < 0.35F) {
            float horizontal = (float) Math.sqrt(dx * dx + dz * dz);
            float factor = (float) Math.sqrt(1 - 0.35F * 0.35F) / Math.max(horizontal, 1.0E-6F);
            dx *= factor; dz *= factor; dy = 0.35F;
            if (horizontal < 1.0E-6F) { dx = 0; dy = 1; dz = 0; }
        }
        points[i] = points[parent] + dx * LINK_LENGTH;
        points[i + 1] = points[parent + 1] + dy * LINK_LENGTH;
        points[i + 2] = points[parent + 2] + dz * LINK_LENGTH;
    }
}
