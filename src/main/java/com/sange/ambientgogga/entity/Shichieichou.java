package com.sange.ambientgogga.entity;

import com.sange.ambientgogga.AmbientGogga;
import com.sange.ambientgogga.entity.ai.ShichieichouWanderGoal;
import com.sange.ambientgogga.particle.ModParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** A rare nocturnal butterfly that never lands and briefly visits the overworld. */
public final class Shichieichou extends Butterfly {
    private static final String REMAINING_LIFETIME_TAG = "RemainingLifetime";
    private static final int MIN_LIFETIME_TICKS = 60 * 20;
    private static final int MAX_LIFETIME_TICKS = 180 * 20;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AmbientGogga.MODID,
            "textures/entity/butterfly/shichieichou.png"
    );

    private int remainingLifetimeTicks;
    private boolean restrictionInitialized;

    public Shichieichou(EntityType<? extends Shichieichou> entityType, Level level) {
        super(entityType, level);
        this.remainingLifetimeTicks = this.randomLifetime();
        this.setSizeModifier(0.9F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new ShichieichouWanderGoal(this));
        this.goalSelector.addGoal(1, new FloatGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData
    ) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.setSizeModifier(0.85F + this.getRandom().nextFloat() * 0.1F);
        this.remainingLifetimeTicks = this.randomLifetime();
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.spawnTrailParticle();
            return;
        }

        if (!this.restrictionInitialized) {
            this.restrictTo(this.blockPosition(), 32);
            this.restrictionInitialized = true;
        }
        if (!this.wasReleasedFromBottle() && --this.remainingLifetimeTicks <= 0) {
            this.discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt(REMAINING_LIFETIME_TAG, this.remainingLifetimeTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int storedLifetime = tag.getInt(REMAINING_LIFETIME_TAG);
        this.remainingLifetimeTicks = storedLifetime > 0 ? storedLifetime : this.randomLifetime();
    }

    @Override
    public ResourceLocation getTexture() {
        return TEXTURE;
    }

    @Override
    public float getWingRotation(float ageInTicks) {
        return Math.abs(Mth.sin(ageInTicks / 4.8F));
    }

    @Override
    public boolean isTired() {
        return false;
    }

    @Override
    public void setTired(boolean tired) {
        super.setTired(false);
    }

    @Override
    public boolean isLanded() {
        return false;
    }

    @Override
    public void setLanded(boolean landed) {
        super.setLanded(false);
    }

    @Override
    public boolean isAtHideout() {
        return false;
    }

    @Override
    public void setAtHideout(boolean atHideout) {
        super.setAtHideout(false);
    }

    @Override
    public boolean isResting() {
        return false;
    }

    @Override
    public boolean areWingsFolded() {
        return false;
    }

    @Override
    public void setWingsFolded(boolean folded) {
        super.setWingsFolded(false);
    }

    private int randomLifetime() {
        return MIN_LIFETIME_TICKS
                + this.getRandom().nextInt(MAX_LIFETIME_TICKS - MIN_LIFETIME_TICKS + 1);
    }

    private void spawnTrailParticle() {
        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() < 1.0E-4D || this.getRandom().nextInt(5) != 0) {
            return;
        }

        Vec3 trailOffset = movement.normalize().scale(-0.22D);
        double spreadX = (this.getRandom().nextDouble() - 0.5D) * 0.12D;
        double spreadY = (this.getRandom().nextDouble() - 0.5D) * 0.08D;
        double spreadZ = (this.getRandom().nextDouble() - 0.5D) * 0.12D;
        this.level().addParticle(
                ModParticles.SHICHIEICHOU_TRAIL.get(),
                this.getX() + trailOffset.x + spreadX,
                this.getY() + this.getBbHeight() * 0.45D + trailOffset.y + spreadY,
                this.getZ() + trailOffset.z + spreadZ,
                -movement.x * 0.04D,
                -movement.y * 0.04D + 0.002D + this.getRandom().nextDouble() * 0.004D,
                -movement.z * 0.04D
        );
    }
}
