package com.sange.ambientgogga.entity;

import com.sange.ambientgogga.advancement.ModAdvancements;
import com.sange.ambientgogga.entity.ai.ButterflyLandOnFlowerGoal;
import com.sange.ambientgogga.entity.ai.ButterflyAvoidPlayerGoal;
import com.sange.ambientgogga.entity.ai.ButterflyHideGoal;
import com.sange.ambientgogga.entity.ai.ButterflyRestGoal;
import com.sange.ambientgogga.entity.ai.ButterflyWanderGoal;
import com.sange.ambientgogga.item.ButterflyBottleItem;
import com.sange.ambientgogga.world.ModTags;
import com.sange.ambientgogga.world.ButterflySeasons;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

public class Butterfly extends PathfinderMob implements FlyingAnimal {
    private static final ResourceLocation CROPTOPIA_BUTTER = ResourceLocation.fromNamespaceAndPath(
            "croptopia",
            "butter"
    );
    private static final float BUTTER_DROP_CHANCE = 0.03F;
    private static final float BUTTER_DROP_CHANCE_PER_LOOTING_LEVEL = 0.01F;
    private static final String RELEASED_FROM_BOTTLE_TAG = "ReleasedFromBottle";
    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(Butterfly.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TIRED =
            SynchedEntityData.defineId(Butterfly.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LANDED =
            SynchedEntityData.defineId(Butterfly.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SIZE_MODIFIER =
            SynchedEntityData.defineId(Butterfly.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> AT_HIDEOUT =
            SynchedEntityData.defineId(Butterfly.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WINGS_FOLDED =
            SynchedEntityData.defineId(Butterfly.class, EntityDataSerializers.BOOLEAN);

    public static final Predicate<net.minecraft.world.entity.LivingEntity> SHOULD_AVOID = entity -> {
        if (!(entity instanceof Player player)) {
            return false;
        }
        return !player.isCreative()
                && !player.isSpectator()
                && !player.level().getBiome(player.blockPosition()).is(Biomes.FLOWER_FOREST)
                && !player.getItemBySlot(EquipmentSlot.HEAD).is(ModTags.BUTTERFLY_FRIENDLY_HEADWEAR);
    };

    private int flyingTicks;
    private int underWaterTicks;
    private float wingRotation = 0.5F;
    private float previousWingFoldProgress;
    private float wingFoldProgress;
    private ButterflyRestGoal restGoal;
    private boolean releasedFromBottle;

    public Butterfly(EntityType<? extends Butterfly> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new FlyingMoveControl(this, 10, false);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, -1.0F);
        this.setPathfindingMalus(PathType.COCOA, -1.0F);
        this.setPathfindingMalus(PathType.FENCE, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.FLYING_SPEED, 2.4D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D);
    }

    public static boolean canSpawn(
            EntityType<Butterfly> type,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        if ((spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION)
                && !ButterflySeasons.allowsNaturalSpawn(level.getLevel())) return false;
        Holder<Biome> biome = level.getBiome(pos);
        boolean validBiome = biome.is(ModTags.BUTTERFLY_SPAWN_BIOMES)
                && biome.is(Tags.Biomes.IS_OVERWORLD)
                && !biome.is(Tags.Biomes.IS_COLD)
                && !biome.is(Tags.Biomes.IS_DENSE_VEGETATION);
        return !shouldHide(level.getLevel())
                && validBiome
                && level.getBlockState(pos.below()).getBlock() instanceof GrassBlock
                && pos.getY() >= level.getSeaLevel()
                && level.getRawBrightness(pos, 0) > 8;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, ButterflyVariant.PEACOCK.index());
        builder.define(TIRED, false);
        builder.define(LANDED, false);
        builder.define(SIZE_MODIFIER, 0.7F);
        builder.define(AT_HIDEOUT, false);
        builder.define(WINGS_FOLDED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new ButterflyHideGoal(this));
        this.goalSelector.addGoal(1, new ButterflyAvoidPlayerGoal(this));
        this.goalSelector.addGoal(2, this.restGoal = new ButterflyRestGoal(this));
        this.goalSelector.addGoal(3, new ButterflyLandOnFlowerGoal(this, 2.4D, 16));
        this.goalSelector.addGoal(4, new ButterflyWanderGoal(this));
        this.goalSelector.addGoal(5, new FloatGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(false);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return level.isEmptyBlock(pos) ? 10.0F : 0.0F;
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnGroupData
    ) {
        this.restrictTo(this.blockPosition(), 22);
        this.setVariant(ButterflyVariant.randomForBiome(level.getBiome(this.blockPosition()), level.getRandom()));
        this.setSizeModifier(this.getVariant().randomSize(this.getRandom()));
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Type", this.getVariant().serializedName());
        tag.putBoolean("IsTired", this.isTired());
        tag.putBoolean("IsLanded", this.isLanded());
        tag.putFloat("SizeModifier", this.getSizeModifier());
        tag.putBoolean(RELEASED_FROM_BOTTLE_TAG, this.wasReleasedFromBottle());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(ButterflyVariant.byName(tag.getString("Type")));
        this.setTired(tag.getBoolean("IsTired"));
        this.setLanded(tag.getBoolean("IsLanded"));
        float storedSize = tag.getFloat("SizeModifier");
        this.setSizeModifier(storedSize > 0.0F ? storedSize : this.getVariant().randomSize(this.getRandom()));
        this.setReleasedFromBottle(tag.getBoolean(RELEASED_FROM_BOTTLE_TAG));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.isUnderWater()) {
            this.underWaterTicks++;
            if (this.underWaterTicks > 20) {
                this.hurt(this.damageSources().drown(), 1.0F);
            }
        } else {
            this.underWaterTicks = 0;
        }

        if (this.isLanded() || this.isAtHideout()) {
            this.flyingTicks = 0;
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            this.flyingTicks++;
            if (this.flyingTicks > 600 && this.getRandom().nextInt(200) == 0) {
                this.setTired(true);
            }
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.8D, 1.0D));
        }
    }

    @Override
    public void tick() {
        this.previousWingFoldProgress = this.wingFoldProgress;
        super.tick();
        this.wingFoldProgress = Mth.approach(
                this.wingFoldProgress,
                this.areWingsFolded() ? 1.0F : 0.0F,
                0.25F
        );
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isAtHideout()) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.travel(travelVector);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean damaged = super.hurt(source, amount);
        if (damaged && this.isLanded()) {
            this.setNotLanded();
        }
        return damaged;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        if (heldStack.getItem() instanceof ButterflyBottleItem butterflyBottle) {
            // Mob's normal interaction path does not call Item#interactLivingEntity for
            // arbitrary items, so explicitly forward filled-bottle interactions here.
            return butterflyBottle.interactLivingEntity(heldStack, player, this, hand);
        }
        if (!heldStack.is(Items.GLASS_BOTTLE)) {
            return super.mobInteract(player, hand);
        }

        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack capturedBottle = ButterflyBottleItem.createWith(this);
        if (!player.getAbilities().instabuild && heldStack.getCount() == 1) {
            player.setItemInHand(hand, capturedBottle);
        } else {
            if (!player.getAbilities().instabuild) {
                heldStack.shrink(1);
            }
            if (!player.getInventory().add(capturedBottle)) {
                player.drop(capturedBottle, false);
            }
        }

        this.level().playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                SoundEvents.BUNDLE_INSERT,
                SoundSource.NEUTRAL,
                0.7F,
                1.25F
        );
        if (this instanceof Shichieichou && player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.awardPocketAMemory(serverPlayer);
        }
        this.discard();
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double yMovement, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean isFlying() {
        return true;
    }

    @Override
    @Nullable
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    @Nullable
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        if (this instanceof Shichieichou || !ModList.get().isLoaded("croptopia")) {
            return;
        }

        BuiltInRegistries.ITEM.getOptional(CROPTOPIA_BUTTER).ifPresent(butter -> {
            int lootingLevel = this.getLootingLevel(level, damageSource);
            float dropChance = Math.min(
                    1.0F,
                    BUTTER_DROP_CHANCE + BUTTER_DROP_CHANCE_PER_LOOTING_LEVEL * lootingLevel
            );
            if (this.getRandom().nextFloat() < dropChance) {
                if (this.spawnAtLocation(new ItemStack(butter)) != null
                        && damageSource.getEntity() instanceof ServerPlayer serverPlayer) {
                    ModAdvancements.awardButterFly(serverPlayer);
                }
            }
        });
    }

    private int getLootingLevel(ServerLevel level, DamageSource damageSource) {
        ItemStack weapon = damageSource.getWeaponItem();
        if (weapon == null || weapon.isEmpty()) {
            return 0;
        }

        Holder<Enchantment> looting = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.LOOTING);
        return weapon.getEnchantmentLevel(looting);
    }

    public ButterflyVariant getVariant() {
        return ButterflyVariant.byIndex(this.entityData.get(VARIANT));
    }

    public void setVariant(ButterflyVariant variant) {
        this.entityData.set(VARIANT, variant.index());
    }

    public ResourceLocation getTexture() {
        return this.getVariant().texture();
    }

    public float getSizeModifier() {
        return this.entityData.get(SIZE_MODIFIER);
    }

    public void setSizeModifier(float sizeModifier) {
        this.entityData.set(SIZE_MODIFIER, sizeModifier);
    }

    public boolean isTired() {
        return this.entityData.get(TIRED);
    }

    public void setTired(boolean tired) {
        this.entityData.set(TIRED, tired);
    }

    public boolean isLanded() {
        return this.entityData.get(LANDED);
    }

    public void setLanded(boolean landed) {
        this.entityData.set(LANDED, landed);
        if (landed) {
            this.setWingsFolded(true);
        } else if (!this.isAtHideout()) {
            this.setWingsFolded(false);
        }
    }

    public void setNotLanded() {
        this.setLanded(false);
        BlockPos pos = this.blockPosition();
        this.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    public boolean isAtHideout() {
        return this.entityData.get(AT_HIDEOUT);
    }

    public void setAtHideout(boolean atHideout) {
        this.entityData.set(AT_HIDEOUT, atHideout);
        if (!atHideout && !this.isLanded()) {
            this.setWingsFolded(false);
        }
    }

    public boolean isResting() {
        return this.isLanded() || this.isAtHideout();
    }

    public boolean areWingsFolded() {
        return this.entityData.get(WINGS_FOLDED);
    }

    public boolean areWingsFullyFolded() {
        return this.wingFoldProgress >= 1.0F;
    }

    public void setWingsFolded(boolean folded) {
        this.entityData.set(WINGS_FOLDED, folded);
    }

    public float getWingFoldProgress(float ageInTicks) {
        float partialTick = Mth.clamp(ageInTicks - this.tickCount, 0.0F, 1.0F);
        return Mth.lerp(partialTick, this.previousWingFoldProgress, this.wingFoldProgress);
    }

    public float getWingRotation(float ageInTicks) {
        this.wingRotation = Math.abs(Mth.sin(ageInTicks / 1.5F));
        return this.wingRotation;
    }

    public ButterflyRestGoal getRestGoal() {
        return this.restGoal;
    }

    public boolean wasReleasedFromBottle() {
        return this.releasedFromBottle;
    }

    public void setReleasedFromBottle(boolean releasedFromBottle) {
        this.releasedFromBottle = releasedFromBottle;
    }

    public static boolean shouldHide(Level level) {
        return level.isNight() || level.isRaining() || level.isThundering();
    }
}
