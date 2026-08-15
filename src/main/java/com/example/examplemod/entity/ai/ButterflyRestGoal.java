package com.example.examplemod.entity.ai;

import com.example.examplemod.entity.Butterfly;
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
    private final Butterfly butterfly;
    @Nullable
    private BlockPos restingPos;
    @Nullable
    private BlockState initialBlockState;
    private int ticks;

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
                && this.noThreateningPlayersNearby();
    }

    @Override
    public void start() {
        if (this.restingPos == null) {
            this.restingPos = this.butterfly.blockPosition();
        }
        this.initialBlockState = this.butterfly.level().getBlockState(this.restingPos);
        this.ticks = 0;
    }

    @Override
    public void stop() {
        this.butterfly.setNotLanded();
        this.restingPos = null;
        this.initialBlockState = null;
        this.ticks = 0;
    }

    @Override
    public void tick() {
        if (this.restingPos == null) {
            this.restingPos = this.butterfly.blockPosition();
        }

        this.ticks++;
        if (this.blockStateUpdated() || this.isBlockTaken(this.butterfly.level(), this.restingPos)) {
            this.butterfly.setNotLanded();
            return;
        }

        if (this.butterfly.getRandom().nextInt(200) == 0) {
            if (this.ticks > 300) {
                this.butterfly.setTired(false);
                if (!this.butterfly.level().isNight()) {
                    this.butterfly.setNotLanded();
                }
            } else {
                this.butterfly.setYRot(this.butterfly.getRandom().nextInt(360));
            }
        }
    }

    private boolean noThreateningPlayersNearby() {
        Level level = this.butterfly.level();
        AABB area = this.butterfly.getBoundingBox().inflate(4.0D);
        return level.getEntitiesOfClass(Player.class, area, Butterfly.SHOULD_AVOID::test).isEmpty();
    }

    private boolean isBlockTaken(Level level, BlockPos pos) {
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
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

    private boolean blockStateUpdated() {
        return this.initialBlockState != this.butterfly.level().getBlockState(this.butterfly.blockPosition());
    }

    @Nullable
    public BlockPos getRestingPos() {
        return this.restingPos;
    }
}
