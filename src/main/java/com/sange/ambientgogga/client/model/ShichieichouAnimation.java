package com.sange.ambientgogga.client.model;

/** Body-space wing stroke followed by a world-vertical hanging rest pose. +Y is down. */
public final class ShichieichouAnimation {
    public static final int SPAN_SEGMENTS = 12;
    public static final int WING_SEGMENTS = 12;
    public static final int TAIL_SEGMENTS = 12;
    public static final int ROWS = WING_SEGMENTS + TAIL_SEGMENTS + 1;
    public static final int COLUMNS = SPAN_SEGMENTS + 1;
    public static final float TAIL_ROOT_V = 0.68F;
    public static final float U_MIN = 0.288F;
    public static final float U_MAX = 0.79F;
    public static final float V_MIN = 0.0F;
    public static final float V_MAX = 1.0F;
    public static final float TAIL_LENGTH = 6.2F;
    public static final float STROKE_SPEED = 0.175F;
    public static final float UPSTROKE = (float) Math.toRadians(75);
    public static final float DOWNSTROKE = (float) Math.toRadians(-65);

    private ShichieichouAnimation() { }

    public static float phase(float ageInTicks, int entityId) {
        return ageInTicks * STROKE_SPEED + (entityId & 255) * 2.3999632F;
    }

    public static float wingAngle(float phase) {
        return (UPSTROKE + DOWNSTROKE) * 0.5F
                + (UPSTROKE - DOWNSTROKE) * 0.5F * sin(phase);
    }

    public static float bodyPitch(float phase, float speed) {
        return -(float) Math.PI / 4.0F + 0.025F * sin(phase - 0.7F) + speed * 0.025F;
    }

    public static float bodyRoll(float phase, float turn) {
        return 0.016F * sin(phase) - turn * 0.055F;
    }

    public static float textureV(int row) {
        return row <= WING_SEGMENTS
                ? V_MIN + (TAIL_ROOT_V - V_MIN) * row / WING_SEGMENTS
                : TAIL_ROOT_V + (V_MAX - TAIL_ROOT_V) * (row - WING_SEGMENTS) / TAIL_SEGMENTS;
    }

    public static void fill(float[] positions, float phase, float speed, float turn, int side) {
        speed = Math.max(0, Math.min(1, speed));
        turn = Math.max(-1, Math.min(1, turn));
        float pitch = bodyPitch(phase, speed), roll = bodyRoll(phase, turn);
        for (int column = 0; column < COLUMNS; column++) {
            float span = (float) column / SPAN_SEGMENTS;
            for (int row = 0; row <= WING_SEGMENTS; row++) {
                float v = textureV(row);
                float radius = 0.18F + span * 5.3F;
                float localPhase = phase - span * 0.20F - (v - V_MIN) * 0.32F;
                float angle = wingAngle(localPhase);
                float flex = 0.10F * span * span * sin(localPhase - 0.7F);
                int i = index(row, column);
                positions[i] = side * radius * cos(angle);
                positions[i + 1] = -radius * sin(angle) + flex;
                positions[i + 2] = (v - 0.4375F) * 10.0F + 0.12F * flex;
                rotateBody(positions, i, pitch, roll);
            }
            int root = index(WING_SEGMENTS, column);
            for (int segment = 1; segment <= TAIL_SEGMENTS; segment++) {
                int i = index(WING_SEGMENTS + segment, column);
                float length = TAIL_LENGTH * segment / TAIL_SEGMENTS;
                positions[i] = positions[root];
                positions[i + 1] = positions[root + 1] + length * 0.98F;
                positions[i + 2] = positions[root + 2] + length * (float) Math.sqrt(1 - 0.98F * 0.98F);
            }
        }
    }

    public static void rotateBody(float[] positions, int index, float pitch, float roll) {
        float x = positions[index];
        float y = positions[index + 1] * cos(pitch) - positions[index + 2] * sin(pitch);
        float z = positions[index + 1] * sin(pitch) + positions[index + 2] * cos(pitch);
        positions[index] = x * cos(roll) - y * sin(roll);
        positions[index + 1] = x * sin(roll) + y * cos(roll);
        positions[index + 2] = z;
    }

    public static int index(int row, int column) {
        return (row * COLUMNS + column) * 3;
    }

    private static float sin(float v) { return (float) Math.sin(v); }
    private static float cos(float v) { return (float) Math.cos(v); }
}
