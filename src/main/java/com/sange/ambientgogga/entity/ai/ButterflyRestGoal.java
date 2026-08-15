package com.sange.ambientgogga.entity.ai;

import com.sange.ambientgogga.entity.Butterfly;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class ButterflyRestGoal extends Goal {
    private static final int MIN_REST_TICKS = 3 * 20;
    private static final int MAX_REST_TICKS = 15 * 20;
    private static final int TARGET_VALIDATION_INTERVAL = 5;

    private final Butterfly butterfly;
    @Nullable
    private BlockPos restingPos;
    @Nullable
    private BlockState initialBlockState;
    private int restingTicks;
    private int restDurationTicks;
    private int nextTargetValidationTick;
    private boolean cachedTargetValid;

    public ButterflyRestGoal(Butterfly butterfly) {
        this.butterfly = butterfly;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.butterfly.isLanded() && this.noThreateningPlayersNearby()) {
            this.restingPos = this.butterfly.blockPosition();
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return this.butterfly.isLanded()
                && (this.butterfly.isTired() || this.butterfly.level().isNight())
                && this.isCurrentTargetValid(false);
    }

    @Override
    public void start() {
        if (this.restingPos == null) {
            this.restingPos = this.butterfly.blockPosition();
        }
        this.initialBlockState = this.butterfly.level().getBlockState(this.restingPos);
        this.restingTicks = 0;
        this.restDurationTicks = MIN_REST_TICKS
                + this.butterfly.getRandom().nextInt(MAX_REST_TICKS - MIN_REST_TICKS + 1);
        this.refreshTargetCache();
    }

    @Override
    public void stop() {
        this.butterfly.setNotLanded();
        this.restingPos = null;
        this.initialBlockState = null;
        this.restingTicks = 0;
        this.restDurationTicks = 0;
        this.nextTargetValidationTick = 0;
        this.cachedTargetValid = false;
    }

    @Override
    public void tick() {
        if (this.restingPos == null) {
            this.restingPos = this.butterfly.blockPosition();
        }

        if (!this.isCurrentTargetValid(false)) {
            this.butterfly.setNotLanded();
            return;
        }

        if (!this.butterfly.areWingsFullyFolded()) {
            return;
        }

        this.restingTicks++;
        if (this.restingTicks >= this.restDurationTicks) {
            this.butterfly.setTired(false);
            this.butterfly.setNotLanded();
            return;
        }

        if (this.butterfly.getRandom().nextInt(200) == 0) {
            this.butterfly.setYRot(this.butterfly.getRandom().nextInt(360));
        }
    }

    private boolean noThreateningPlayersNearby() {
        Level level = this.butterfly.level();
        AABB area = this.butterfly.getBoundingBox().inflate(4.0D);
        return level.getEntitiesOfClass(Player.class, area, Butterfly.SHOULD_AVOID::test).isEmpty();
    }

    private boolean isBlockTaken(Level level, BlockPos pos) {
        VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
        if (shape.isEmpty()) {
            return true;
        }

        List<Butterfly> butterflies = level.getEntitiesOfClass(
                Butterfly.class,
                shape.bounds().move(pos).inflate(0.5D)
        );
        return butterflies.stream().anyMatch(other -> {
            if (other == this.butterfly || other.getRestGoal() == null) {
                return false;
            }
            return pos.equals(other.getRestGoal().getRestingPos());
        });
    }

    private void refreshTargetCache() {
        this.cachedTargetValid = this.restingPos != null
                && this.initialBlockState == this.butterfly.level().getBlockState(this.restingPos)
                && !this.isBlockTaken(this.butterfly.level(), this.restingPos)
                && this.noThreateningPlayersNearby();
        this.nextTargetValidationTick = this.butterfly.tickCount + TARGET_VALIDATION_INTERVAL;
    }

    private boolean isCurrentTargetValid(boolean force) {
        if (force || this.butterfly.tickCount >= this.nextTargetValidationTick) {
            this.refreshTargetCache();
        }
        return this.cachedTargetValid;
    }

    @Nullable
    public BlockPos getRestingPos() {
        return this.restingPos;
    }
}
