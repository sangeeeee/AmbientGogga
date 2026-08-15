package com.sange.ambientgogga.entity.ai;

import com.sange.ambientgogga.entity.Butterfly;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ButterflyLandOnFlowerGoal extends MoveToBlockGoal {
    private static final float DAYTIME_FLOWER_INTEREST = 0.35F;
    private static final double FINAL_APPROACH_SPEED = 0.18D;
    private static final double FINAL_APPROACH_DISTANCE_SQR = 0.01D;
    private static final int MAX_FINAL_APPROACH_TICKS = 40;
    private static final int TARGET_VALIDATION_INTERVAL = 5;

    private final Butterfly butterfly;
    private boolean mandatorySearchActive;
    private boolean voluntaryVisit;
    private boolean targetActive;
    private boolean cachedTargetValid;
    private int nextTargetValidationTick;
    private Vec3 cachedLandingPosition = Vec3.ZERO;
    private boolean finalApproach;
    private int finalApproachTicks;

    public ButterflyLandOnFlowerGoal(Butterfly butterfly, double speed, int searchRadius) {
        super(butterfly, speed, searchRadius);
        this.butterfly = butterfly;
    }

    @Override
    public boolean canUse() {
        if (this.butterfly.isLanded()) {
            return false;
        }

        boolean mandatorySearch = this.butterfly.isTired() || this.butterfly.level().isNight();
        if (mandatorySearch) {
            if (!this.mandatorySearchActive) {
                this.nextStartTick = 0;
            }
            this.mandatorySearchActive = true;
            this.voluntaryVisit = false;
            return super.canUse();
        }

        this.mandatorySearchActive = false;
        this.voluntaryVisit = false;
        if (this.nextStartTick > 0) {
            this.nextStartTick--;
            return false;
        }

        this.nextStartTick = this.nextStartTick(this.butterfly);
        if (this.butterfly.getRandom().nextFloat() >= DAYTIME_FLOWER_INTEREST) {
            return false;
        }

        this.voluntaryVisit = this.findNearestBlock();
        return this.voluntaryVisit;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.butterfly.isLanded()
                && (this.butterfly.isTired() || this.butterfly.level().isNight() || this.voluntaryVisit)
                && super.canContinueToUse();
    }

    @Override
    public void start() {
        this.finalApproach = false;
        this.finalApproachTicks = 0;
        this.targetActive = true;
        this.refreshTargetCache(this.butterfly.level());
        super.start();
    }

    @Override
    public void stop() {
        boolean targetInvalid = this.targetActive && !this.cachedTargetValid;
        this.butterfly.getNavigation().stop();
        this.voluntaryVisit = false;
        this.targetActive = false;
        this.cachedTargetValid = false;
        this.finalApproach = false;
        this.finalApproachTicks = 0;
        if (targetInvalid) {
            this.nextStartTick = 0;
        }
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        if (this.targetActive && pos.equals(this.blockPos)) {
            return this.isCurrentTargetValid(level, false);
        }
        return this.isTargetStateValid(level, pos);
    }

    private boolean isTargetStateValid(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return this.canLandOnBlock(state) && !this.isBlockTaken(level, pos);
    }

    private boolean canLandOnBlock(BlockState state) {
        if (state.is(BlockTags.TALL_FLOWERS) && state.getBlock() instanceof TallFlowerBlock) {
            return state.getValue(TallFlowerBlock.HALF) == DoubleBlockHalf.UPPER;
        }
        return state.is(BlockTags.FLOWERS);
    }

    private boolean isBlockTaken(LevelReader level, BlockPos pos) {
        VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
        if (shape.isEmpty()) {
            return true;
        }

        AABB area = shape.bounds().move(pos).inflate(0.5D);
        List<Butterfly> butterflies = this.butterfly.level().getEntitiesOfClass(Butterfly.class, area);
        return butterflies.stream().anyMatch(other -> {
            ButterflyRestGoal restGoal = other.getRestGoal();
            return restGoal != null && pos.equals(restGoal.getRestingPos());
        });
    }

    @Override
    public void tick() {
        if (this.finalApproach) {
            this.tickFinalApproach();
            return;
        }

        super.tick();
        if (!this.isReachedTarget()) {
            return;
        }
        this.beginFinalApproach();
    }

    private void beginFinalApproach() {
        if (!this.isCurrentTargetValid(this.butterfly.level(), true)) {
            this.abortCurrentTarget();
            return;
        }

        this.finalApproach = true;
        this.finalApproachTicks = 0;
        this.butterfly.getNavigation().stop();
        this.butterfly.setDeltaMovement(this.butterfly.getDeltaMovement().scale(0.35D));
        this.tickFinalApproach();
    }

    private void tickFinalApproach() {
        if (!this.isCurrentTargetValid(this.butterfly.level(), false)) {
            this.abortCurrentTarget();
            return;
        }
        if (this.butterfly.position().distanceToSqr(this.cachedLandingPosition) <= FINAL_APPROACH_DISTANCE_SQR) {
            this.completeLanding();
            return;
        }
        if (++this.finalApproachTicks > MAX_FINAL_APPROACH_TICKS) {
            this.abortCurrentTarget();
            return;
        }

        this.butterfly.getNavigation().stop();
        this.butterfly.getMoveControl().setWantedPosition(
                this.cachedLandingPosition.x,
                this.cachedLandingPosition.y,
                this.cachedLandingPosition.z,
                FINAL_APPROACH_SPEED
        );
    }

    private void completeLanding() {
        if (!this.isCurrentTargetValid(this.butterfly.level(), true)) {
            this.abortCurrentTarget();
            return;
        }
        if (this.butterfly.position().distanceToSqr(this.cachedLandingPosition) > FINAL_APPROACH_DISTANCE_SQR) {
            return;
        }

        this.finalApproach = false;
        this.butterfly.setLanded(true);
        if (this.voluntaryVisit) {
            this.butterfly.setTired(true);
        }
        this.butterfly.setPos(
                this.cachedLandingPosition.x,
                this.cachedLandingPosition.y,
                this.cachedLandingPosition.z
        );
        this.butterfly.setDeltaMovement(Vec3.ZERO);
    }

    private void refreshTargetCache(LevelReader level) {
        this.cachedTargetValid = this.isTargetStateValid(level, this.blockPos);
        this.nextTargetValidationTick = this.butterfly.tickCount + TARGET_VALIDATION_INTERVAL;
        if (!this.cachedTargetValid) {
            return;
        }

        BlockState state = level.getBlockState(this.blockPos);
        VoxelShape shape = state.getShape(level, this.blockPos);
        if (shape.isEmpty()) {
            this.cachedTargetValid = false;
            return;
        }

        AABB bounds = shape.bounds();
        double x = this.blockPos.getX() + (bounds.minX + bounds.maxX) / 2.0D;
        double y = this.blockPos.getY() + bounds.maxY;
        double z = this.blockPos.getZ() + (bounds.minZ + bounds.maxZ) / 2.0D;
        if (state.getBlock() instanceof WaterlilyBlock) {
            y += 0.1D;
        } else if (state.getBlock() instanceof TallFlowerBlock) {
            y -= 0.25D;
        }
        this.cachedLandingPosition = new Vec3(x, y, z);
    }

    private boolean isCurrentTargetValid(LevelReader level, boolean force) {
        if (force || this.butterfly.tickCount >= this.nextTargetValidationTick) {
            this.refreshTargetCache(level);
        }
        return this.cachedTargetValid;
    }

    private void abortCurrentTarget() {
        this.butterfly.getNavigation().stop();
        this.finalApproach = false;
        this.finalApproachTicks = 0;
        this.cachedTargetValid = false;
        this.nextStartTick = 0;
    }
}
