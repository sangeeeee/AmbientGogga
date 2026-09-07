package com.sange.ambientgogga.client.model;

import java.util.ArrayList;
import java.util.List;

/** Tapered round anatomy and curved antennae. No cuboid body or flat antenna sprite. */
public final class ShichieichouAnatomy {
    public static final Mesh MESH = create();

    public record Mesh(float[] vertices, int[] colors) { }

    private ShichieichouAnatomy() { }

    private static Mesh create() {
        Builder b = new Builder();
        b.spindle(0, -0.02F, -1.04F, 0.26F, 0.22F, 0.34F, 8, 0xFFFFFFFF);
        b.spindle(0, 0, -0.34F, 0.30F, 0.25F, 0.58F, 10, 0xFFFFFFFF);
        // A long, progressively narrower abdomen with distinct segment rings.
        float[] z = {0.10F, 0.28F, 0.49F, 0.72F, 0.96F, 1.20F, 1.44F, 1.67F, 1.87F, 2.03F};
        float[] radius = {0.19F, 0.23F, 0.22F, 0.20F, 0.18F, 0.15F, 0.12F, 0.09F, 0.05F, 0.015F};
        b.rings(0, 0.03F, z, radius, radius, 0xFFF4F6FF);
        for (int side : new int[]{-1, 1}) {
            b.spindle(side * 0.20F, -0.10F, -1.13F, 0.085F, 0.08F, 0.12F, 5, 0xFFB6BDD5);
            float[][] antenna = new float[13][3];
            for (int i = 0; i < antenna.length; i++) {
                float t = (float) i / (antenna.length - 1);
                antenna[i] = new float[]{side * (0.13F + 0.39F * t + 0.11F * t * t),
                        -0.16F - 0.57F * t + 0.14F * t * t, -1.25F - 1.32F * t};
            }
            b.tube(antenna, 0.024F, 0.013F, 0xFFFFFFFF);
            float[] tip = antenna[antenna.length - 1];
            b.spindle(tip[0], tip[1], tip[2], 0.045F, 0.04F, 0.105F, 5, 0xFFFFFFFF);
            for (int leg = 0; leg < 3; leg++) {
                float baseZ = -0.63F + leg * 0.34F;
                float[][] path = {
                        {side * 0.17F, 0.12F, baseZ},
                        {side * 0.38F, 0.30F, baseZ + 0.12F},
                        {side * 0.44F, 0.46F, baseZ + 0.39F},
                        {side * 0.37F, 0.49F, baseZ + 0.55F}};
                b.tube(path, 0.024F, 0.009F, 0xFFF1F5FF);
            }
        }
        return b.build();
    }

    private static final class Builder {
        private static final int SIDES = 10;
        private final List<Float> vertices = new ArrayList<>();
        private final List<Integer> colors = new ArrayList<>();

        private void vertex(float x, float y, float z, float u, float v, int color) {
            this.vertices.add(x); this.vertices.add(y); this.vertices.add(z);
            this.vertices.add(u); this.vertices.add(v); this.colors.add(color);
        }

        private void spindle(float x, float y, float z, float rx, float ry, float rz, int count, int color) {
            float[] zs = new float[count + 1], xs = new float[count + 1], ys = new float[count + 1];
            for (int i = 0; i <= count; i++) {
                float a = (float) Math.PI * i / count;
                zs[i] = z - (float) Math.cos(a) * rz;
                xs[i] = Math.max(0.005F, (float) Math.sin(a) * rx);
                ys[i] = Math.max(0.005F, (float) Math.sin(a) * ry);
            }
            rings(x, y, zs, xs, ys, color);
        }

        private void rings(float x, float y, float[] zs, float[] xs, float[] ys, int color) {
            for (int ring = 0; ring < zs.length - 1; ring++) {
                for (int side = 0; side < SIDES; side++) {
                    for (int corner = 0; corner < 4; corner++) {
                        int r = ring + (corner >= 2 ? 1 : 0);
                        int s = side + (corner == 1 || corner == 2 ? 1 : 0);
                        float angle = (float) (s * Math.PI * 2 / SIDES);
                        vertex(x + xs[r] * (float) Math.cos(angle), y + ys[r] * (float) Math.sin(angle),
                                zs[r], (float) s / SIDES, (float) r / (zs.length - 1), color);
                    }
                }
            }
        }

        private void tube(float[][] path, float startRadius, float endRadius, int color) {
            float[][][] rings = new float[path.length][SIDES][3];
            for (int r = 0; r < path.length; r++) {
                float[] a = path[Math.max(0, r - 1)], b = path[Math.min(path.length - 1, r + 1)];
                float tx = b[0] - a[0], ty = b[1] - a[1], tz = b[2] - a[2];
                float len = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
                tx /= len; ty /= len; tz /= len;
                float nx = -tz, nz = tx;
                len = (float) Math.sqrt(nx * nx + nz * nz);
                nx /= len; nz /= len;
                float bx = ty * nz, by = tz * nx - tx * nz, bz = -ty * nx;
                float radius = startRadius + (endRadius - startRadius) * r / (path.length - 1);
                for (int s = 0; s < SIDES; s++) {
                    float angle = (float) (s * Math.PI * 2 / SIDES);
                    float c = (float) Math.cos(angle), sn = (float) Math.sin(angle);
                    rings[r][s] = new float[]{path[r][0] + radius * (c * nx + sn * bx),
                            path[r][1] + radius * sn * by, path[r][2] + radius * (c * nz + sn * bz)};
                }
            }
            for (int r = 0; r < path.length - 1; r++) {
                for (int s = 0; s < SIDES; s++) {
                    for (int corner = 0; corner < 4; corner++) {
                        int row = r + (corner >= 2 ? 1 : 0), col = s + (corner == 1 || corner == 2 ? 1 : 0);
                        float[] p = rings[row][col % SIDES];
                        vertex(p[0], p[1], p[2], 0.38F + (float) col / SIDES * 0.15F,
                                (float) row / (path.length - 1), color);
                    }
                }
            }
        }

        private Mesh build() {
            float[] points = new float[this.vertices.size()];
            int[] tint = new int[this.colors.size()];
            for (int i = 0; i < points.length; i++) points[i] = this.vertices.get(i);
            for (int i = 0; i < tint.length; i++) tint[i] = this.colors.get(i);
            return new Mesh(points, tint);
        }
    }
}
