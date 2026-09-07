package com.sange.ambientgogga.entity.ai;

import com.sange.ambientgogga.entity.Shichieichou;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Long, forward-biased legs; the move control rounds changes into continuous arcs. */
public final class ShichieichouWanderGoal extends Goal {
    private final Shichieichou butterfly;
    private Vec3 target;
    private int targetTicks;

    public ShichieichouWanderGoal(Shichieichou butterfly) {
        this.butterfly = butterfly;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() { return !this.butterfly.isInWaterOrBubble() && !this.butterfly.isInLava(); }

    @Override
    public boolean canContinueToUse() { return canUse(); }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void start() { this.butterfly.getNavigation().stop(); this.target = null; }

    @Override
    public void stop() { this.target = null; }

    @Override
    public void tick() {
        if (this.target == null || --this.targetTicks <= 0 || this.butterfly.position().distanceToSqr(this.target) < 6.25) {
            this.target = findTarget();
            this.targetTicks = 100 + this.butterfly.getRandom().nextInt(61);
        }
        if (this.target != null) this.butterfly.getMoveControl().setWantedPosition(this.target.x, this.target.y, this.target.z, 1.0);
    }

    private Vec3 findTarget() {
        Vec3 position = this.butterfly.position();
        Vec3 home = Vec3.atCenterOf(this.butterfly.getRestrictCenter());
        boolean returnHome = this.butterfly.hasRestriction() && position.subtract(home).horizontalDistanceSqr() > 22 * 22;
        double heading = returnHome ? Math.atan2(home.z - position.z, home.x - position.x)
                : Math.toRadians(this.butterfly.getYRot() + 90);
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = heading + (this.butterfly.getRandom().nextDouble() - 0.5) * Math.toRadians(100);
            double length = 6.0 + this.butterfly.getRandom().nextDouble() * 4.0;
            double x = position.x + Math.cos(angle) * length, z = position.z + Math.sin(angle) * length;
            BlockPos column = BlockPos.containing(x, position.y, z);
            if (!this.butterfly.level().hasChunkAt(column)) continue;
            int surface = this.butterfly.level().getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
            double y = Mth.clamp(surface + 3 + this.butterfly.getRandom().nextDouble() * 4, position.y - 2.5, position.y + 2.5);
            y = Mth.clamp(y, this.butterfly.level().getMinBuildHeight() + 1, this.butterfly.level().getMaxBuildHeight() - 2);
            BlockPos targetPos = BlockPos.containing(x, y, z);
            if (!this.butterfly.isWithinRestriction(targetPos) || !this.butterfly.level().isEmptyBlock(targetPos)
                    || !this.butterfly.level().isEmptyBlock(targetPos.above()) || !this.butterfly.level().getFluidState(targetPos).isEmpty()) continue;
            return new Vec3(x, y, z);
        }
        // In a tight space keep a heading for the obstacle controller to resolve.
        return position.add(Math.cos(heading) * 3, 0.5, Math.sin(heading) * 3);
    }
}
