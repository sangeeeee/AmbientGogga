package com.sange.ambientgogga.entity.ai;

import com.sange.ambientgogga.entity.Shichieichou;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Continuous flight with a short swept-volume look-ahead and persistent avoidance heading. */
public final class ShichieichouMoveControl extends MoveControl {
    private final Shichieichou butterfly;
    private final ShichieichouFlightMotion motion = new ShichieichouFlightMotion();
    private boolean initialized;
    private int avoidanceTicks;
    private Vec3 avoidance = Vec3.ZERO;

    public ShichieichouMoveControl(Shichieichou butterfly) {
        super(butterfly);
        this.butterfly = butterfly;
    }

    @Override
    public void tick() {
        if (!this.initialized || this.butterfly.isInWaterOrBubble() || this.butterfly.isInLava()
                || this.butterfly.hurtTime > 0) {
            this.motion.reset(this.mob.getYRot(), 0, Math.min(this.mob.getDeltaMovement().length(), ShichieichouFlightMotion.CRUISE_SPEED));
            this.initialized = true;
            if (this.butterfly.isInWaterOrBubble() || this.butterfly.isInLava() || this.butterfly.hurtTime > 0) return;
        }
        this.mob.setNoGravity(true);
        this.mob.setXxa(0); this.mob.setYya(0); this.mob.setZza(0);
        Vec3 desired = new Vec3(this.wantedX, this.wantedY, this.wantedZ).subtract(this.mob.position());
        double speed = this.operation == Operation.MOVE_TO && desired.lengthSqr() > 0.04
                ? ShichieichouFlightMotion.CRUISE_SPEED * this.speedModifier
                    * this.mob.getAttributeValue(Attributes.FLYING_SPEED) / 2.4 : 0;
        this.operation = Operation.WAIT;
        Vec3 forward = direction(this.motion.yaw(), this.motion.pitch());
        if (this.avoidanceTicks > 0) this.avoidanceTicks--;
        boolean blocked = clearance(forward, 1.4) < 1.2;
        if (blocked && (this.avoidanceTicks == 0 || clearance(this.avoidance, 0.8) < 0.6)) {
            this.avoidance = findEscape(forward);
            this.avoidanceTicks = 24;
        }
        boolean avoiding = this.avoidanceTicks > 0;
        if (avoiding) {
            desired = this.avoidance;
            speed = Math.min(speed, 0.035);
        }
        double desiredYaw = desired.horizontalDistanceSqr() > 1.0E-8
                ? Math.toDegrees(Math.atan2(-desired.x, desired.z)) : this.motion.yaw();
        double desiredPitch = Math.toDegrees(Math.atan2(desired.y, Math.max(0.001, desired.horizontalDistance())));
        this.motion.tick(desiredYaw, desiredPitch, speed, avoiding);
        Vec3 velocity = new Vec3(this.motion.x(), this.motion.y(), this.motion.z());
        // Close obstacles may require stopping faster than the ordinary acceleration limit.
        if (!clearAt(velocity.scale(3))) velocity = Vec3.ZERO;
        this.mob.setDeltaMovement(velocity);
        this.mob.setYRot((float) this.motion.yaw());
        this.mob.setXRot(0); // The visual model supplies its characteristic 45-degree posture.
        this.mob.yHeadRot = this.mob.yBodyRot = this.mob.getYRot();
    }

    private Vec3 findEscape(Vec3 forward) {
        Vec3 best = forward;
        double bestScore = -Double.MAX_VALUE;
        int preferredSide = (this.mob.getId() & 1) == 0 ? 1 : -1;
        for (int offset : new int[]{0, 35, -35, 70, -70, 110, -110, 180}) {
            for (int pitch : new int[]{0, 30, -30}) {
                Vec3 candidate = direction(this.motion.yaw() + offset * preferredSide, pitch);
                double distance = clearance(candidate, 2.4);
                double score = distance + candidate.dot(forward) * 0.40 - Math.abs(pitch) * 0.002;
                if (score > bestScore) { bestScore = score; best = candidate; }
            }
        }
        return best;
    }

    private double clearance(Vec3 direction, double limit) {
        for (double distance = 0.2; distance <= limit + 0.001; distance += 0.2) {
            if (!clearAt(direction.scale(distance))) return distance - 0.2;
        }
        return limit;
    }

    private boolean clearAt(Vec3 offset) {
        BlockPos pos = BlockPos.containing(this.mob.position().add(offset));
        if (!this.mob.level().hasChunkAt(pos) || this.mob.level().isOutsideBuildHeight(pos)
                || !this.mob.level().getFluidState(pos).isEmpty()) return false;
        AABB currentBox = this.mob.getBoundingBox();
        // A butterfly already touching a wall must be able to leave the safety
        // margin, rather than remaining frozen inside an inflated collision box.
        double margin = this.mob.level().noCollision(this.mob, currentBox.inflate(0.10)) ? 0.10 : 0;
        AABB box = currentBox.move(offset).inflate(margin);
        return this.mob.level().noCollision(this.mob, box)
                && this.mob.level().getEntitiesOfClass(LivingEntity.class, box,
                    entity -> entity != this.mob && entity.isAlive() && !entity.isSpectator()
                        && !(currentBox.inflate(0.10).intersects(entity.getBoundingBox())
                            && offset.dot(this.mob.position().subtract(entity.position())) >= 0)).isEmpty();
    }

    private static Vec3 direction(double yaw, double pitch) {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new Vec3(-Math.sin(y) * Math.cos(p), Math.sin(p), Math.cos(y) * Math.cos(p));
    }
}
