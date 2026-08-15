package com.sange.ambientgogga.item;

import com.sange.ambientgogga.AmbientGogga;
import com.sange.ambientgogga.entity.Butterfly;
import com.sange.ambientgogga.entity.ButterflyVariant;
import com.sange.ambientgogga.entity.ModEntities;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ButterflyBottleItem extends Item {
    public static final int MAX_BUTTERFLIES = 3;
    private static final String BUTTERFLIES_TAG = "Butterflies";
    private static final String VARIANT_TAG = "Variant";
    private static final String SIZE_TAG = "SizeModifier";

    public ButterflyBottleItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createWith(Butterfly butterfly) {
        ItemStack stack = new ItemStack(AmbientGogga.BUTTERFLY_BOTTLE.get());
        addButterfly(stack, butterfly);
        return stack;
    }

    public static int getButterflyCount(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0;
        }
        return Math.min(data.copyTag().getList(BUTTERFLIES_TAG, Tag.TAG_COMPOUND).size(), MAX_BUTTERFLIES);
    }

    public static boolean isFull(ItemStack stack) {
        return getButterflyCount(stack) >= MAX_BUTTERFLIES;
    }

    public static boolean addButterfly(ItemStack stack, Butterfly butterfly) {
        int oldCount = getButterflyCount(stack);
        if (!stack.is(AmbientGogga.BUTTERFLY_BOTTLE.get()) || oldCount >= MAX_BUTTERFLIES) {
            return false;
        }

        CompoundTag capturedButterfly = new CompoundTag();
        capturedButterfly.putString(VARIANT_TAG, butterfly.getVariant().serializedName());
        capturedButterfly.putFloat(SIZE_TAG, butterfly.getSizeModifier());
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            ListTag butterflies = root.getList(BUTTERFLIES_TAG, Tag.TAG_COMPOUND);
            butterflies.add(capturedButterfly);
            root.put(BUTTERFLIES_TAG, butterflies);
        });
        updateModel(stack, oldCount + 1);
        return true;
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        if (!(target instanceof Butterfly butterfly)
                || butterfly.getType() != ModEntities.BUTTERFLY.get()) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            return tryRelease(player.level(), player, hand, stack, releasePosition(player));
        }
        if (isFull(stack)) {
            return InteractionResult.FAIL;
        }
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!addButterfly(stack, butterfly)) {
            return InteractionResult.FAIL;
        }

        player.level().playSound(
                null,
                butterfly.getX(),
                butterfly.getY(),
                butterfly.getZ(),
                SoundEvents.BUNDLE_INSERT,
                SoundSource.NEUTRAL,
                0.7F,
                1.25F
        );
        butterfly.discard();
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || getButterflyCount(stack) == 0) {
            return InteractionResultHolder.pass(stack);
        }
        InteractionResult result = tryRelease(level, player, hand, stack, releasePosition(player));
        return new InteractionResultHolder<>(result, player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || !player.isShiftKeyDown() || getButterflyCount(stack) == 0) {
            return InteractionResult.PASS;
        }

        Direction face = context.getClickedFace();
        Vec3 releasePosition = context.getClickLocation().add(
                face.getStepX() * 0.4D,
                face.getStepY() * 0.4D,
                face.getStepZ() * 0.4D
        );
        return tryRelease(context.getLevel(), player, context.getHand(), stack, releasePosition);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);
        ListTag butterflies = getButterflies(stack);
        int displayedCount = Math.min(butterflies.size(), MAX_BUTTERFLIES);
        tooltip.add(Component.translatable(
                "item.ambientgogga.butterfly_bottle.count",
                displayedCount,
                MAX_BUTTERFLIES
        ).withStyle(ChatFormatting.GRAY));
        for (int index = 0; index < displayedCount; index++) {
            ButterflyVariant variant = ButterflyVariant.byName(
                    butterflies.getCompound(index).getString(VARIANT_TAG)
            );
            tooltip.add(Component.literal(" • ")
                    .append(Component.translatable("entity.ambientgogga.butterfly." + variant.serializedName()))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!butterflies.isEmpty()) {
            tooltip.add(Component.translatable("item.ambientgogga.butterfly_bottle.release")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static InteractionResult tryRelease(
            Level level,
            Player player,
            InteractionHand hand,
            ItemStack stack,
            Vec3 position
    ) {
        if (getButterflyCount(stack) == 0) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }

        ListTag butterflies = getButterflies(stack);
        int butterflyCount = Math.min(butterflies.size(), MAX_BUTTERFLIES);
        int released = 0;
        for (int index = 0; index < butterflyCount; index++) {
            CompoundTag data = butterflies.getCompound(index);
            Butterfly butterfly = ModEntities.BUTTERFLY.get().create(serverLevel);
            if (butterfly == null) {
                continue;
            }

            double angle = index * (Math.PI * 2.0D / Math.max(1, butterflyCount));
            double offset = butterflyCount > 1 ? 0.18D : 0.0D;
            Vec3 butterflyPosition = position.add(Math.cos(angle) * offset, index * 0.08D, Math.sin(angle) * offset);
            butterfly.setVariant(ButterflyVariant.byName(data.getString(VARIANT_TAG)));
            float sizeModifier = data.getFloat(SIZE_TAG);
            butterfly.setSizeModifier(sizeModifier > 0.0F
                    ? sizeModifier
                    : butterfly.getVariant().randomSize(serverLevel.getRandom()));
            butterfly.setTired(false);
            butterfly.setLanded(false);
            butterfly.setAtHideout(false);
            butterfly.setReleasedFromBottle(true);
            butterfly.moveTo(
                    butterflyPosition.x,
                    butterflyPosition.y,
                    butterflyPosition.z,
                    player.getYRot(),
                    0.0F
            );
            butterfly.restrictTo(BlockPos.containing(butterflyPosition), 22);
            butterfly.setPersistenceRequired();
            if (serverLevel.addFreshEntity(butterfly)) {
                released++;
            }
        }

        if (released == 0) {
            return InteractionResult.FAIL;
        }
        player.setItemInHand(hand, new ItemStack(Items.GLASS_BOTTLE));
        serverLevel.playSound(
                null,
                position.x,
                position.y,
                position.z,
                SoundEvents.BUNDLE_REMOVE_ONE,
                SoundSource.NEUTRAL,
                0.8F,
                1.2F
        );
        return InteractionResult.SUCCESS;
    }

    private static ListTag getButterflies(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null
                ? new ListTag()
                : data.copyTag().getList(BUTTERFLIES_TAG, Tag.TAG_COMPOUND);
    }

    private static void updateModel(ItemStack stack, int count) {
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(Math.max(1, Math.min(count, MAX_BUTTERFLIES))));
    }

    private static Vec3 releasePosition(Player player) {
        return player.getEyePosition()
                .add(player.getLookAngle().scale(1.35D))
                .add(0.0D, -0.3D, 0.0D);
    }
}
