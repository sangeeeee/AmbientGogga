package com.example.examplemod.entity.ai;

import com.example.examplemod.entity.Butterfly;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Makes butterflies seek ground cover and leave the world without dying during unsafe conditions. */
public final class ButterflyHideGoal extends MoveToBlockGoal {
    private static final float HIDE_CHANCE = 0.90F;
    private static final double GRASS_CONTACT_MARGIN = 0.15D;

    private final Butterfly butterfly;

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
        return Butterfly.shouldHide(this.butterfly.level())
                && !this.butterfly.isRemoved()
                && super.canContinueToUse();
    }

    @Override
    public double acceptedDistance() {
        return 0.65D;
    }

    @Override
    protected boolean isValidTarget(LevelReader level, BlockPos pos) {
        BlockState ground = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());
        return ground.getBlock() instanceof GrassBlock
                && above.getFluidState().isEmpty()
                && above.getCollisionShape(level, pos.above()).isEmpty();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.butterfly.level().isClientSide()
                && (this.isReachedTarget() || this.isTouchingGrassBlock())) {
            this.butterfly.discard();
        }
    }

    private boolean isTouchingGrassBlock() {
        AABB contactBox = this.butterfly.getBoundingBox().inflate(GRASS_CONTACT_MARGIN);
        BlockPos min = BlockPos.containing(contactBox.minX, contactBox.minY, contactBox.minZ);
        BlockPos max = BlockPos.containing(contactBox.maxX, contactBox.maxY, contactBox.maxZ);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (this.butterfly.level().getBlockState(pos).getBlock() instanceof GrassBlock
                    && contactBox.intersects(new AABB(pos))) {
                return true;
            }
        }
        return false;
    }
}
