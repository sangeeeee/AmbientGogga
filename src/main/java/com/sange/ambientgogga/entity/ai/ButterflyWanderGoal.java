package com.sange.ambientgogga.entity.ai;

import com.sange.ambientgogga.entity.Butterfly;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.HoverRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ButterflyWanderGoal extends Goal {
    private final Butterfly butterfly;

    public ButterflyWanderGoal(Butterfly butterfly) {
        this.butterfly = butterfly;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return !this.butterfly.isLanded()
                && this.butterfly.getNavigation().isDone()
                && this.butterfly.getRandom().nextInt(5) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.butterfly.getNavigation().isInProgress();
    }

    @Override
    public void start() {
        Vec3 target = this.getRandomLocation();
        if (target != null) {
            this.butterfly.getNavigation().moveTo(target.x, target.y, target.z, 1.0D);
        }
    }

    @Nullable
    private Vec3 getRandomLocation() {
        BlockPos home = this.butterfly.getRestrictCenter();
        Vec3 direction;
        if (!this.butterfly.isWithinRestriction(home)) {
            direction = Vec3.atCenterOf(home).subtract(this.butterfly.position()).normalize();
        } else {
            direction = this.butterfly.getViewVector(0.0F);
        }

        Vec3 airTarget = HoverRandomPos.getPos(
                this.butterfly,
                8,
                7,
                direction.x,
                direction.z,
                (float) (Math.PI / 2.0D),
                2,
                1
        );
        return airTarget != null
                ? airTarget
                : AirAndWaterRandomPos.getPos(
                        this.butterfly,
                        8,
                        4,
                        -2,
                        direction.x,
                        direction.z,
                        (float) (Math.PI / 2.0D)
                );
    }
}
