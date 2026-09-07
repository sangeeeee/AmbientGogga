package com.sange.ambientgogga.entity;

import com.sange.ambientgogga.AmbientGogga;
import com.sange.ambientgogga.entity.ai.ShichieichouWanderGoal;
import com.sange.ambientgogga.entity.ai.ShichieichouMoveControl;
import com.sange.ambientgogga.client.ShichieichouTrailEmitter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** A rare nocturnal butterfly that never lands and briefly visits the overworld. */
public final class Shichieichou extends Butterfly {
    private static final byte NATURAL_DESPAWN_EVENT = 61;
    private static final String REMAINING_LIFETIME_TAG = "RemainingLifetime";
    private static final int MIN_LIFETIME_TICKS = 60 * 20;
    private static final int MAX_LIFETIME_TICKS = 180 * 20;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AmbientGogga.MODID,
            "textures/entity/butterfly/shichieichou.png"
    );

    private int remainingLifetimeTicks;
    private boolean restrictionInitialized;
    private boolean naturalDespawnFadeRequested;
    private float clientFadeProgress;
    private Vec3 clientFlightMovement = Vec3.ZERO;

    public Shichieichou(EntityType<? extends Shichieichou> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new ShichieichouMoveControl(this);
        this.remainingLifetimeTicks = this.randomLifetime();
        this.setSizeModifier(0.9F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ShichieichouWanderGoal(this));
    }

    @Override
    public void travel(Vec3 input) {
        if (this.isNoAi() || this.isInWaterOrBubble() || this.isInLava() || this.isDeadOrDying()) {
            super.travel(input);
            return;
        }
        if (this.isControlledByLocalInstance()) {
            // Use the controller's velocity once: ground friction/forward input
            // from LivingEntity.travel would otherwise distort the smooth arcs.
            this.move(MoverType.SELF, this.getDeltaMovement());
            if (this.hurtTime > 0) this.setDeltaMovement(this.getDeltaMovement().scale(0.94));
            this.fallDistance = 0;
        }
        this.calculateEntityAnimation(false);
    }

    @Override
    protected float tickHeadTurn(float yaw, float animationStep) {
        double dx = this.getX() - this.xo, dz = this.getZ() - this.zo;
        float heading = dx * dx + dz * dz > 1.0E-7
                ? (float) Math.toDegrees(Math.atan2(-dx, dz)) : this.getYRot();
        this.yBodyRot = this.level().isClientSide()
                ? Mth.approachDegrees(this.yBodyRot, heading, 5.0F) : heading;
        this.yHeadRot = this.yBodyRot;
        return animationStep;
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
        Vec3 previousPosition = this.position();
        super.tick();
        if (this.level().isClientSide()) {
            Vec3 step = this.position().subtract(previousPosition);
            this.clientFlightMovement = step.lengthSqr() > 1.0D
                    ? Vec3.ZERO : this.clientFlightMovement.lerp(step, 0.25D);
            ShichieichouTrailEmitter.emit(this, step);
            return;
        }

        if (!this.restrictionInitialized) {
            this.restrictTo(this.blockPosition(), 32);
            this.restrictionInitialized = true;
        }
        if (!this.wasReleasedFromBottle() && --this.remainingLifetimeTicks <= 0) {
            this.level().broadcastEntityEvent(this, NATURAL_DESPAWN_EVENT);
            this.discard();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == NATURAL_DESPAWN_EVENT) {
            this.naturalDespawnFadeRequested = true;
            return;
        }
        super.handleEntityEvent(id);
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
        return 0.5F + 0.5F * Mth.sin(ageInTicks * 0.175F);
    }

    public Vec3 getClientFlightMovement() {
        return this.clientFlightMovement;
    }

    public boolean shouldPlayNaturalDespawnFade() {
        return this.naturalDespawnFadeRequested;
    }

    public void setClientFadeProgress(float progress) {
        this.clientFadeProgress = Mth.clamp(progress, 0.0F, 1.0F);
    }

    public boolean isClientFadeGhost() {
        return this.clientFadeProgress > 0.0F;
    }

    public float getClientFadeAlpha() {
        float progress = this.clientFadeProgress;
        float smoothProgress = progress * progress * (3.0F - 2.0F * progress);
        return 1.0F - smoothProgress;
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

}
