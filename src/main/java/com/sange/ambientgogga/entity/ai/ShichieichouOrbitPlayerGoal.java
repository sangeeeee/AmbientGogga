package com.sange.ambientgogga.entity.ai;

import com.sange.ambientgogga.entity.Shichieichou;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/** Makes a nearby Shichieichou circle the observing player instead of fleeing. */
public final class ShichieichouOrbitPlayerGoal extends Goal {
    private static final double START_DISTANCE = 7.0D;
    private static final double STOP_DISTANCE_SQR = 12.0D * 12.0D;
    private static final double ORBIT_SPEED = 0.85D;

    private final Shichieichou butterfly;
    @Nullable
    private Player player;
    private double angle;
    private double radius;
    private double heightOffset;
    private double direction;

    public ShichieichouOrbitPlayerGoal(Shichieichou butterfly) {
        this.butterfly = butterfly;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.player = this.butterfly.level().getNearestPlayer(
                this.butterfly,
                START_DISTANCE
        );
        return this.isPlayerValid(this.player);
    }

    @Override
    public boolean canContinueToUse() {
        return this.isPlayerValid(this.player)
                && this.butterfly.distanceToSqr(this.player) <= STOP_DISTANCE_SQR;
    }

    @Override
    public void start() {
        if (this.player == null) {
            return;
        }
        this.butterfly.getNavigation().stop();
        this.angle = Math.atan2(
                this.butterfly.getZ() - this.player.getZ(),
                this.butterfly.getX() - this.player.getX()
        );
        this.radius = 1.8D + this.butterfly.getRandom().nextDouble() * 1.4D;
        this.heightOffset = 0.9D + this.butterfly.getRandom().nextDouble() * 1.0D;
        this.direction = this.butterfly.getRandom().nextBoolean() ? 1.0D : -1.0D;
    }

    @Override
    public void stop() {
        this.player = null;
        this.butterfly.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.player == null) {
            return;
        }

        this.angle += this.direction * 0.055D;
        double bob = Math.sin(this.butterfly.tickCount * 0.11D) * 0.35D;
        double targetX = this.player.getX() + Math.cos(this.angle) * this.radius;
        double targetY = this.player.getY() + this.heightOffset + bob;
        double targetZ = this.player.getZ() + Math.sin(this.angle) * this.radius;
        this.butterfly.getMoveControl().setWantedPosition(targetX, targetY, targetZ, ORBIT_SPEED);
        this.butterfly.getLookControl().setLookAt(this.player, 20.0F, 20.0F);
    }

    private boolean isPlayerValid(@Nullable Player candidate) {
        return candidate != null
                && candidate.isAlive()
                && !candidate.isSpectator();
    }
}
