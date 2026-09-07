package com.sange.ambientgogga.client.model;

/** Independent invariants for stroke range, rope constraints, inertia and render scheduling. */
public final class ShichieichouAnimationTest {
    private static final int SIZE = ShichieichouAnimation.ROWS * ShichieichouAnimation.COLUMNS * 3;

    public static void main(String[] args) {
        float[] points = new float[SIZE], next = new float[SIZE], cycle = new float[SIZE];
        float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
        for (int frame = 0; frame < 720; frame++) {
            float phase = (float) (frame * Math.PI / 360);
            min = Math.min(min, ShichieichouAnimation.wingAngle(phase));
            max = Math.max(max, ShichieichouAnimation.wingAngle(phase));
            for (float speed : new float[]{0, 0.5F, 1}) {
                for (float turn : new float[]{-1, 0, 1}) {
                    ShichieichouAnimation.fill(points, phase, speed, turn, 1);
                    ShichieichouAnimation.fill(next, phase + 0.0001F, speed, turn, 1);
                    ShichieichouAnimation.fill(cycle, phase + (float) (Math.PI * 2), speed, turn, 1);
                    checkTail(points);
                    checkSurface(points);
                    for (int i = 0; i < SIZE; i++) {
                        require(Float.isFinite(points[i]), "Non-finite vertex");
                        require(Math.abs(next[i] - points[i]) < 0.002F, "Discontinuous stroke");
                        require(Math.abs(cycle[i] - points[i]) < 0.0001F, "Cycle seam");
                    }
                    float pitch = ShichieichouAnimation.bodyPitch(phase, speed);
                    require(pitch < Math.toRadians(-41) && pitch > Math.toRadians(-48), "Body does not point upward about 45 degrees");
                }
            }
        }
        require(Math.abs(Math.toDegrees(min) + 45) < 0.01, "Downstroke must reach -45 degrees");
        require(Math.abs(Math.toDegrees(max) - 75) < 0.01, "Upstroke must reach +75 degrees");
        require(2 * Math.PI / ShichieichouAnimation.STROKE_SPEED / 20 > 1.75, "Stroke is too fast");
        checkRenderScheduling();
        checkInertia();
        checkStrokeTiming();
        checkTipStability();
        require(ShichieichouAnatomy.MESH.vertices().length / 5 == ShichieichouAnatomy.MESH.colors().length, "Anatomy UV/color count");
        for (float value : ShichieichouAnatomy.MESH.vertices()) require(Float.isFinite(value), "Invalid anatomy vertex");
        System.out.println("Passed 6,480 poses; +75/-45 degree stroke; ~45 degree body; fixed tail lengths, inertia, teleport reset and render-rate independence.");
    }

    private static void checkStrokeTiming() {
        double down = ShichieichouAnimation.DOWNSTROKE_FRACTION;
        double span = ShichieichouAnimation.UPSTROKE - ShichieichouAnimation.DOWNSTROKE;
        double firstTravel = (angleAt(0) - angleAt(down * 0.5)) / span;
        require(firstTravel > 0.20 && firstTravel < 0.27, "Slow departure must cover about the first quarter of downstroke travel");
        double peakDown = -velocityAt(down * 0.8);
        double recoverySpeed = velocityAt(down + (1 - down) * 0.40);
        require(peakDown > recoverySpeed * 2.3, "Downstroke lacks the fast push");
        require(-velocityAt(down * 0.7) > -velocityAt(down * 0.3) * 3, "Downstroke does not accelerate distinctly");
        require(-velocityAt(down * 0.98) < peakDown * 0.05, "Downstroke does not brake at the bottom");
        require(Math.abs(velocityAt(down + (1 - down) * 0.25) - recoverySpeed) < 0.01, "Recovery should be nearly uniform");
        require(velocityAt(down + (1 - down) * 0.9) < recoverySpeed * 0.3, "Recovery does not ease near the top");
        for (double endpoint : new double[]{0, down, 1}) require(Math.abs(velocityAt(endpoint)) < 0.01, "Stroke reversal is not at rest");
        for (int i = 1; i <= 1000; i++) {
            require(angleAt(down * i / 1000) <= angleAt(down * (i - 1) / 1000) + 1E-6, "Downstroke reverses early");
            require(angleAt(down + (1 - down) * i / 1000) >= angleAt(down + (1 - down) * (i - 1) / 1000) - 1E-6, "Recovery reverses early");
        }
    }

    private static double angleAt(double cycle) {
        return ShichieichouAnimation.wingAngle((float) (Math.PI / 2 + cycle * Math.PI * 2));
    }

    private static double velocityAt(double cycle) {
        double h = 0.0002;
        return (angleAt(cycle + h) - angleAt(cycle - h)) / (2 * h);
    }

    private static void checkTipStability() {
        ShichieichouPose pose = new ShichieichouPose();
        float[][] wings = {new float[SIZE], new float[SIZE]};
        double maxTipBend = 0, maxTipChange = 0;
        double[][] lastTangent = new double[2][3];
        for (int tick = 0; tick < 1200; tick++) {
            double x = Math.sin(tick * 0.035) * 0.6, y = Math.sin(tick * 0.025) * 0.15;
            pose.sample(wings, tick, 17, 0.5F, 0.3F, x, y, 0, tick * 1.2F, 0.9F);
            for (int side = 0; side < 2; side++) {
                checkTail(wings[side]); checkSurface(wings[side]);
                int tip = ShichieichouAnimation.index(ShichieichouAnimation.ROWS - 1, ShichieichouTailSimulation.SPINE_COLUMN);
                int step = ShichieichouAnimation.COLUMNS * 3;
                double[] tangent = tangent(wings[side], tip - step, tip);
                if (tick > 50) {
                    maxTipBend = Math.max(maxTipBend, angleBetween(tangent, tangent(wings[side], tip - 2 * step, tip - step)));
                    maxTipChange = Math.max(maxTipChange, angleBetween(tangent, lastTangent[side]));
                }
                lastTangent[side] = tangent;
            }
        }
        System.out.printf("Fast-stroke tail stress: max terminal bend %.3f degrees, max terminal turn %.3f degrees/tick.%n", maxTipBend, maxTipChange);
        require(maxTipBend < 4, "Terminal links fold independently like a whip");
        require(maxTipChange < 10, "Terminal link snaps between ticks");
    }

    private static double[] tangent(float[] p, int a, int b) {
        double x = p[b] - p[a], y = p[b + 1] - p[a + 1], z = p[b + 2] - p[a + 2];
        double length = Math.sqrt(x*x + y*y + z*z);
        return new double[]{x / length, y / length, z / length};
    }

    private static double angleBetween(double[] a, double[] b) {
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, a[0]*b[0] + a[1]*b[1] + a[2]*b[2]))));
    }

    private static void checkRenderScheduling() {
        ShichieichouPose sparse = new ShichieichouPose(), frequent = new ShichieichouPose();
        float[][] a = {new float[SIZE], new float[SIZE]}, b = {new float[SIZE], new float[SIZE]};
        for (int tick = 0; tick < 240; tick++) {
            double x = tick * 0.006 + (tick >= 180 ? 10 : 0);
            float yaw = tick * 0.4F;
            sparse.sample(a, tick, 9, 0.3F, 0.1F, x, 0, 0, yaw, 0.9F);
            sparse.sample(a, tick + 0.75F, 9, 0.3F, 0.1F, x, 0, 0, yaw, 0.9F);
            for (int frame = 0; frame < 8; frame++) frequent.sample(b, tick + frame / 8.0F, 9, 0.3F, 0.1F, x, 0, 0, yaw, 0.9F);
            sparse.sample(a, tick + 0.95F, 9, 0.3F, 0.1F, x, 0, 0, yaw, 0.9F);
            frequent.sample(b, tick + 0.95F, 9, 0.3F, 0.1F, x, 0, 0, yaw, 0.9F);
            for (int side = 0; side < 2; side++) {
                checkTail(a[side]); checkSurface(a[side]);
                for (int i = 0; i < SIZE; i++) require(Math.abs(a[side][i] - b[side][i]) < 0.00001F, "Frame rate changes physics");
            }
            frequent.sample(b, tick + 0.95F, 9, 0.3F, 0.1F, x, 0, 0, yaw, 0.9F);
            for (int side = 0; side < 2; side++) for (int i = 0; i < SIZE; i++) require(a[side][i] == b[side][i], "Repeated shadow/render pass advances simulation");
        }
    }

    private static void checkInertia() {
        float[] rest = new float[SIZE], target = new float[SIZE], output = new float[SIZE];
        ShichieichouAnimation.fill(rest, 0, 0, 0, 1);
        ShichieichouTailSimulation simulation = new ShichieichouTailSimulation(SIZE);
        simulation.reset(rest);
        for (int tick = 1; tick <= 8; tick++) {
            System.arraycopy(rest, 0, target, 0, SIZE);
            for (int i = 0; i < SIZE; i += 3) target[i] += tick * 0.10F;
            simulation.tick(target, 0, 0, 1);
        }
        System.arraycopy(target, 0, output, 0, SIZE);
        simulation.sample(output, 1);
        int tip = ShichieichouAnimation.index(ShichieichouAnimation.ROWS - 1, ShichieichouTailSimulation.SPINE_COLUMN);
        float first = output[tip];
        require(first - rest[tip] < 0.65F, "Tail follows the root rigidly");
        for (int tick = 0; tick < 10; tick++) simulation.tick(target, 0, 0, 1);
        System.arraycopy(target, 0, output, 0, SIZE);
        simulation.sample(output, 1);
        require(Math.abs(first - output[tip]) > 0.03F, "Tail has no inertial follow-through");
        checkTail(output);
    }

    private static void checkTail(float[] v) {
        for (int c = ShichieichouTailSimulation.SPINE_COLUMN; c <= ShichieichouTailSimulation.SPINE_COLUMN; c++) {
            for (int r = ShichieichouAnimation.WING_SEGMENTS + 1; r < ShichieichouAnimation.ROWS; r++) {
                int a = ShichieichouAnimation.index(r - 1, c), b = ShichieichouAnimation.index(r, c);
                double x = v[b] - v[a], y = v[b + 1] - v[a + 1], z = v[b + 2] - v[a + 2];
                double length = ShichieichouAnimation.TAIL_LENGTH / ShichieichouAnimation.TAIL_SEGMENTS;
                require(y > length * 0.859, "Tail flips upward in world space");
                require(Math.abs(Math.sqrt(x*x+y*y+z*z) - length) < 0.00002, "Stretched/detached tail link");
            }
        }
    }

    private static void checkSurface(float[] v) {
        for (int r = 0; r < ShichieichouAnimation.ROWS - 1; r++) {
            for (int c = 0; c < ShichieichouAnimation.SPAN_SEGMENTS; c++) {
                int a = ShichieichouAnimation.index(r,c), b = ShichieichouAnimation.index(r,c+1), d = ShichieichouAnimation.index(r+1,c);
                float ux=v[b]-v[a], uy=v[b+1]-v[a+1], uz=v[b+2]-v[a+2];
                float vx=v[d]-v[a], vy=v[d+1]-v[a+1], vz=v[d+2]-v[a+2];
                float nx=uy*vz-uz*vy, ny=uz*vx-ux*vz, nz=ux*vy-uy*vx;
                require(nx*nx+ny*ny+nz*nz > 1.0E-8F, "Degenerate surface normal");
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
