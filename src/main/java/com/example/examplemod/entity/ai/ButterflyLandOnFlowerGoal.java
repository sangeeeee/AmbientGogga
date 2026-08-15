package com.example.examplemod.entity.ai;

import com.example.examplemod.entity.Butterfly;
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
    private final Butterfly butterfly;

    public ButterflyLandOnFlowerGoal(Butterfly butterfly, double speed, int searchRadius) {
        super(butterfly, speed, searchRadius);
        this.butterfly = butterfly;
    }

    @Override
    public boolean canUse() {
        return !this.butterfly.isLanded()
                && (this.butterfly.isTired() || this.butterfly.level().isNight())
                && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return !this.butterfly.isLanded()
                && (this.butterfly.isTired() || this.butterfly.level().isNight())
                && super.canContinueToUse();
    }

    @Override
    public void stop() {
        this.butterfly.getNavigation().stop();
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
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
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
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
        super.tick();
        if (!this.isReachedTarget() || !this.isValidTarget(this.butterfly.level(), this.blockPos)) {
            return;
        }

        BlockState state = this.butterfly.level().getBlockState(this.blockPos);
        VoxelShape shape = state.getCollisionShape(this.butterfly.level(), this.blockPos);
        if (shape.isEmpty()) {
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

        this.butterfly.setLanded(true);
        this.butterfly.setPos(x, y, z);
        this.butterfly.setDeltaMovement(Vec3.ZERO);
    }
}
