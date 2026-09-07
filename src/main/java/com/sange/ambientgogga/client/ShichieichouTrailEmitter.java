package com.sange.ambientgogga.client;

import com.sange.ambientgogga.entity.Shichieichou;
import com.sange.ambientgogga.particle.ModParticles;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Emits a small expanding cone behind the observed client movement. */
public final class ShichieichouTrailEmitter {
    private ShichieichouTrailEmitter() {
    }

    public static void emit(Shichieichou butterfly, Vec3 step) {
        int maximum = ShichieichouClientConfig.DUST_PER_TICK.get();
        double distance = step.length();
        if (maximum == 0 || distance > 1.0D || butterfly.isInvisible() || butterfly.isClientFadeGhost()) {
            return;
        }
        RandomSource random = butterfly.getRandom();
        boolean hovering = distance < 0.002D;
        if (hovering && butterfly.tickCount % 2 != 0) {
            return;
        }
        Vec3 forward = hovering ? butterfly.getLookAngle().normalize() : step.scale(1.0D / distance);
        Vec3 lateral = forward.cross(new Vec3(0, 1, 0));
        if (lateral.lengthSqr() < 1.0E-6D) {
            lateral = new Vec3(1, 0, 0);
        } else {
            lateral = lateral.normalize();
        }
        Vec3 vertical = lateral.cross(forward).normalize();
        int count = hovering ? 1 : maximum;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = Math.sqrt(random.nextDouble());
            Vec3 radial = lateral.scale(Math.cos(angle)).add(vertical.scale(Math.sin(angle)));
            Vec3 origin = butterfly.position().subtract(step.scale((i + random.nextDouble()) / count))
                    .add(0.0D, 0.12D, 0.0D)
                    .subtract(forward.scale(0.06D + random.nextDouble() * 0.10D))
                    .add(radial.scale(radius * 0.09D * butterfly.getSizeModifier()));
            Vec3 velocity = forward.scale(-0.008D - random.nextDouble() * 0.018D)
                    .add(radial.scale(0.003D + radius * 0.009D))
                    .add(0.0D, -0.001D, 0.0D);
            butterfly.level().addParticle(ModParticles.SHICHIEICHOU_TRAIL.get(),
                    origin.x, origin.y, origin.z, velocity.x, velocity.y, velocity.z);
        }
    }
}
