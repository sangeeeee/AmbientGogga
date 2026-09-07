package com.sange.ambientgogga.entity.ai;

/** Regression checks for abrupt target changes, wraparound, altitude easing and braking. */
public final class ShichieichouFlightTest {
    public static void main(String[] args) {
        ShichieichouFlightMotion motion = new ShichieichouFlightMotion();
        motion.reset(179, 0, 0);
        double previousRate = 0, maximumTurn = 0;
        for (int tick = 0; tick < 600; tick++) {
            double yaw = motion.yaw(), pitch = motion.pitch(), speed = motion.speed();
            double desiredYaw = tick < 100 ? -179 : tick < 350 ? 1 : -100;
            motion.tick(desiredYaw, tick < 170 ? 30 : -30, 0.06, false);
            double rate = ShichieichouFlightMotion.wrap(motion.yaw() - yaw);
            maximumTurn = Math.max(maximumTurn, Math.abs(rate));
            require(Math.abs(rate) <= 2.400001, "Abrupt horizontal turn");
            require(Math.abs(rate - previousRate) <= 0.200001, "Abrupt change in turn rate");
            require(Math.abs(motion.pitch() - pitch) <= 0.800001, "Abrupt climb/descent");
            require(Math.abs(motion.speed() - speed) <= 0.001801, "Abrupt acceleration");
            require(motion.speed() <= 0.060001 && (tick < 45 || motion.speed() > 0.035), "Stop/pivot or overspeed");
            require(Double.isFinite(motion.x() + motion.y() + motion.z()), "Invalid movement");
            if (tick == 90) require(Math.abs(ShichieichouFlightMotion.wrap(motion.yaw() + 179)) < 0.1, "Took long way around yaw seam");
            previousRate = rate;
        }
        require(Math.abs(ShichieichouFlightMotion.wrap(motion.yaw() + 100)) < 0.1, "Heading does not settle");
        require(Math.abs(motion.pitch() + 30) < 0.01, "Altitude does not settle");
        for (int tick = 0; tick < 80; tick++) motion.tick(-100, 0, 0, false);
        require(motion.speed() == 0, "Controller cannot come to rest");
        for (int tick = 0; tick < 6; tick++) motion.tick(50, 30, 0.035, true);
        double beforeYaw = motion.yaw(), beforePitch = motion.pitch();
        motion.tick(50, 30, 0.06, false);
        require(Math.abs(ShichieichouFlightMotion.wrap(motion.yaw() - beforeYaw)) <= 2.400001, "Fast avoidance turn persists after obstacle clears");
        require(Math.abs(motion.pitch() - beforePitch) <= 0.800001, "Fast avoidance climb persists after obstacle clears");
        System.out.println("Passed smooth flight: 600 ticks, max turn " + maximumTurn + " degrees/tick; yaw seam, target reversals, climb/descent and braking.");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
