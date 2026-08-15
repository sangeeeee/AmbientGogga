package com.sange.ambientgogga.entity;

import com.sange.ambientgogga.AmbientGogga;
import com.sange.ambientgogga.entity.ai.ShichieichouWanderGoal;
import com.sange.ambientgogga.item.ButterflyBottleItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
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
            return;
        }

        if (!this.restrictionInitialized) {
            this.restrictTo(this.blockPosition(), 32);
            this.restrictionInitialized = true;
        }
        if (--this.remainingLifetimeTicks <= 0) {
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
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (heldStack.is(Items.GLASS_BOTTLE)
                || heldStack.getItem() instanceof ButterflyBottleItem) {
            return InteractionResult.PASS;
        }
        return super.mobInteract(player, hand);
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
}
