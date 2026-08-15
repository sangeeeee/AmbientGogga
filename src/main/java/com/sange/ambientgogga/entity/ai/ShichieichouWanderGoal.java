package com.sange.ambientgogga.entity.ai;

import com.sange.ambientgogga.entity.Shichieichou;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Keeps Shichieichou wandering several blocks above the local surface. */
public final class ShichieichouWanderGoal extends Goal {
    private static final int HORIZONTAL_RANGE = 10;
    private static final int MIN_SURFACE_CLEARANCE = 3;
    private static final int MAX_SURFACE_CLEARANCE = 7;
    private static final int TARGET_ATTEMPTS = 8;

    private final Shichieichou butterfly;

    public ShichieichouWanderGoal(Shichieichou butterfly) {
        this.butterfly = butterfly;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.butterfly.getNavigation().isDone()
                && this.butterfly.getRandom().nextInt(5) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return this.butterfly.getNavigation().isInProgress();
    }

    @Override
    public void start() {
        Vec3 target = this.findHighAirTarget();
        if (target != null) {
            this.butterfly.getNavigation().moveTo(target.x, target.y, target.z, 0.9D);
        }
    }

    @Nullable
    private Vec3 findHighAirTarget() {
        for (int attempt = 0; attempt < TARGET_ATTEMPTS; attempt++) {
            int x = this.butterfly.getBlockX()
                    + this.butterfly.getRandom().nextInt(HORIZONTAL_RANGE * 2 + 1)
                    - HORIZONTAL_RANGE;
            int z = this.butterfly.getBlockZ()
                    + this.butterfly.getRandom().nextInt(HORIZONTAL_RANGE * 2 + 1)
                    - HORIZONTAL_RANGE;
            BlockPos chunkCheck = new BlockPos(x, this.butterfly.getBlockY(), z);
            if (!this.butterfly.level().hasChunkAt(chunkCheck)) {
                continue;
            }

            int surfaceY = this.butterfly.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            int clearance = MIN_SURFACE_CLEARANCE + this.butterfly.getRandom().nextInt(
                    MAX_SURFACE_CLEARANCE - MIN_SURFACE_CLEARANCE + 1
            );
            BlockPos targetPos = new BlockPos(x, surfaceY + clearance, z);
            if (!this.butterfly.isWithinRestriction(targetPos)
                    || !this.butterfly.level().isEmptyBlock(targetPos)
                    || !this.butterfly.level().isEmptyBlock(targetPos.above())) {
                continue;
            }
            return Vec3.atCenterOf(targetPos);
        }
        return null;
    }
}
