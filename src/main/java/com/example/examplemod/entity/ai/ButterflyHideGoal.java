package com.example.examplemod.entity.ai;

import com.example.examplemod.entity.Butterfly;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Makes butterflies seek ground cover and leave the world without dying during unsafe conditions. */
public final class ButterflyHideGoal extends MoveToBlockGoal {
    private static final float HIDE_CHANCE = 0.90F;
    private static final double GRASS_CONTACT_MARGIN = 0.15D;
    private static final int SETTLE_TICKS = 2;
    private static final int FOLDED_WAIT_TICKS = 20;
    private static final int TARGET_VALIDATION_INTERVAL = 5;

    private final Butterfly butterfly;
    private boolean targetActive;
    private boolean cachedTargetValid;
    private boolean targetIsGrassPlant;
    private int nextTargetValidationTick;
    private Vec3 cachedHideoutPosition = Vec3.ZERO;
    private boolean waitingAtHideout;
    private int settleTicks;
    private int foldedWaitTicks;

    public ButterflyHideGoal(Butterfly butterfly) {
        super(butterfly, 2.4D, 22, 8);
        this.butterfly = butterfly;
    }

    @Override
    public boolean canUse() {
        return Butterfly.shouldHide(this.butterfly.level())
                && this.butterfly.getRandom().nextFloat() < HIDE_CHANCE
                && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (!Butterfly.shouldHide(this.butterfly.level()) || this.butterfly.isRemoved()) {
            return false;
        }
        return this.waitingAtHideout
                ? this.isCurrentTargetValid(this.butterfly.level(), false)
                : super.canContinueToUse();
    }

    @Override
    public void start() {
        this.waitingAtHideout = false;
        this.settleTicks = 0;
        this.foldedWaitTicks = 0;
        if (this.butterfly.isLanded()) {
            this.butterfly.setNotLanded();
        }
        this.butterfly.setAtHideout(false);
        this.targetActive = true;
        this.refreshTargetCache(this.butterfly.level());
        super.start();
    }

    @Override
    public void stop() {
        this.butterfly.getNavigation().stop();
        if (!this.butterfly.isRemoved()) {
            this.butterfly.setAtHideout(false);
            this.butterfly.setNoGravity(false);
        }
        this.waitingAtHideout = false;
        this.targetActive = false;
        this.cachedTargetValid = false;
        this.settleTicks = 0;
        this.foldedWaitTicks = 0;
        if (Butterfly.shouldHide(this.butterfly.level())) {
            this.nextStartTick = 0;
        }
    }

    @Override
    public double acceptedDistance() {
        return 0.65D;
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        if (this.targetActive && pos.equals(this.blockPos)) {
            return this.isCurrentTargetValid(level, false);
        }
        BlockState state = level.getBlockState(pos);
        return this.isTargetStateValid(level, pos, state);
    }

    private boolean isTargetStateValid(LevelReader level, BlockPos pos, BlockState state) {
        if (isGrassPlant(state)) {
            return true;
        }
        if (!(state.getBlock() instanceof GrassBlock)) {
            return false;
        }

        BlockPos abovePos = pos.above();
        BlockState above = level.getBlockState(abovePos);
        return above.isAir();
    }

    @Override
    protected BlockPos getMoveToTarget() {
        return this.targetIsGrassPlant
                ? this.blockPos
                : this.blockPos.above();
    }

    @Override
    protected void moveMobToBlock() {
        this.butterfly.getNavigation().moveTo(
                this.cachedHideoutPosition.x,
                this.cachedHideoutPosition.y,
                this.cachedHideoutPosition.z,
                this.speedModifier
        );
    }

    @Override
    public void tick() {
        if (this.waitingAtHideout) {
            this.tickAtHideout();
            return;
        }

        super.tick();
        if (this.isReachedTarget() || this.isTouchingTarget()) {
            this.beginWaitingAtHideout();
        }
    }

    private void beginWaitingAtHideout() {
        if (!this.isCurrentTargetValid(this.butterfly.level(), true)) {
            this.butterfly.getNavigation().stop();
            this.nextStartTick = 0;
            return;
        }

        this.waitingAtHideout = true;
        this.settleTicks = 0;
        this.foldedWaitTicks = 0;
        this.butterfly.getNavigation().stop();
        this.butterfly.setDeltaMovement(Vec3.ZERO);
        this.butterfly.setNoGravity(true);
        this.butterfly.setAtHideout(true);
        this.holdAtHideout();
    }

    private void tickAtHideout() {
        if (!this.isCurrentTargetValid(this.butterfly.level(), false)) {
            this.butterfly.setAtHideout(false);
            this.butterfly.setNoGravity(false);
            this.waitingAtHideout = false;
            this.nextStartTick = 0;
            return;
        }

        this.holdAtHideout();
        if (!this.butterfly.areWingsFolded()) {
            this.settleTicks++;
            if (this.settleTicks >= SETTLE_TICKS && this.butterfly.getDeltaMovement().lengthSqr() < 1.0E-7D) {
                this.butterfly.setWingsFolded(true);
            }
            return;
        }

        this.foldedWaitTicks++;
        if (this.foldedWaitTicks >= FOLDED_WAIT_TICKS && !this.butterfly.level().isClientSide()) {
            this.butterfly.discard();
        }
    }

    private void holdAtHideout() {
        this.butterfly.getNavigation().stop();
        this.butterfly.setDeltaMovement(Vec3.ZERO);
        this.butterfly.setNoGravity(true);
        this.butterfly.setPos(
                this.cachedHideoutPosition.x,
                this.cachedHideoutPosition.y,
                this.cachedHideoutPosition.z
        );
    }

    private void refreshTargetCache(LevelReader level) {
        BlockState state = level.getBlockState(this.blockPos);
        this.cachedTargetValid = this.isTargetStateValid(level, this.blockPos, state);
        this.nextTargetValidationTick = this.butterfly.tickCount + TARGET_VALIDATION_INTERVAL;
        if (!this.cachedTargetValid) {
            return;
        }

        this.targetIsGrassPlant = isGrassPlant(state);
        double yOffset;
        if (state.is(Blocks.TALL_GRASS)) {
            yOffset = 0.35D;
        } else if (state.is(Blocks.SHORT_GRASS)) {
            yOffset = 0.15D;
        } else {
            yOffset = 1.01D;
        }
        this.cachedHideoutPosition = new Vec3(
                this.blockPos.getX() + 0.5D,
                this.blockPos.getY() + yOffset,
                this.blockPos.getZ() + 0.5D
        );
    }

    private boolean isCurrentTargetValid(LevelReader level, boolean force) {
        if (force || this.butterfly.tickCount >= this.nextTargetValidationTick) {
            this.refreshTargetCache(level);
        }
        return this.cachedTargetValid;
    }

    private boolean isTouchingTarget() {
        AABB contactBox = this.butterfly.getBoundingBox().inflate(GRASS_CONTACT_MARGIN);
        return contactBox.intersects(new AABB(this.blockPos));
    }

    private static boolean isGrassPlant(BlockState state) {
        if (state.is(Blocks.SHORT_GRASS)) {
            return true;
        }
        return state.is(Blocks.TALL_GRASS)
                && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER;
    }
}
