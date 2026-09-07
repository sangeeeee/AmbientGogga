package com.sange.ambientgogga.entity.ai;

/** Tick-based steering in degrees and blocks/tick, independent of rendering and path nodes. */
public final class ShichieichouFlightMotion {
    public static final double CRUISE_SPEED = 0.060;
    public static final double MAX_YAW_RATE = 2.4;
    public static final double MAX_PITCH_RATE = 0.8;
    private double yaw, pitch, speed, yawRate, pitchRate;

    public void reset(double yaw, double pitch, double speed) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.speed = speed;
        this.yawRate = this.pitchRate = 0;
    }

    public void tick(double desiredYaw, double desiredPitch, double desiredSpeed, boolean avoiding) {
        double yawError = wrap(desiredYaw - this.yaw);
        double yawLimit = avoiding ? 9.0 : MAX_YAW_RATE;
        this.yawRate = clamp(approach(this.yawRate, clamp(yawError * 0.12, -yawLimit, yawLimit), avoiding ? 1.8 : 0.20), -yawLimit, yawLimit);
        this.yaw = wrap(this.yaw + this.yawRate);
        double pitchLimit = avoiding ? 2.5 : MAX_PITCH_RATE;
        double pitchError = clamp(desiredPitch, -35, 35) - this.pitch;
        this.pitchRate = clamp(approach(this.pitchRate, clamp(pitchError * 0.12, -pitchLimit, pitchLimit), avoiding ? 0.5 : 0.10), -pitchLimit, pitchLimit);
        this.pitch += this.pitchRate;
        // Slow a little into a bend; never stop and pivot at ordinary waypoints.
        double bendSpeed = desiredSpeed * (1.0 - Math.min(Math.abs(yawError) / 180, 1) * 0.35);
        this.speed = approach(this.speed, clamp(bendSpeed, 0, CRUISE_SPEED * 1.2), avoiding ? 0.008 : 0.0018);
    }

    public double yaw() { return this.yaw; }
    public double pitch() { return this.pitch; }
    public double speed() { return this.speed; }
    public double x() { return -Math.sin(Math.toRadians(this.yaw)) * Math.cos(Math.toRadians(this.pitch)) * this.speed; }
    public double y() { return Math.sin(Math.toRadians(this.pitch)) * this.speed; }
    public double z() { return Math.cos(Math.toRadians(this.yaw)) * Math.cos(Math.toRadians(this.pitch)) * this.speed; }

    public static double wrap(double angle) { return angle - Math.floor((angle + 180) / 360) * 360; }
    private static double clamp(double v, double low, double high) { return Math.max(low, Math.min(high, v)); }
    private static double approach(double from, double to, double step) { return from + clamp(to - from, -step, step); }
}
