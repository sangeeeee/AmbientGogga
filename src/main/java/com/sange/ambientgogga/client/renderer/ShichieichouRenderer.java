package com.sange.ambientgogga.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.sange.ambientgogga.AmbientGogga;
import com.sange.ambientgogga.client.ShichieichouClientConfig;
import com.sange.ambientgogga.client.model.ShichieichouAnimation;
import com.sange.ambientgogga.client.model.ShichieichouModel;
import com.sange.ambientgogga.entity.Shichieichou;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class ShichieichouRenderer extends MobRenderer<Shichieichou, ShichieichouModel> {
    private static final ResourceLocation ANATOMY = ResourceLocation.fromNamespaceAndPath(
            AmbientGogga.MODID, "textures/entity/butterfly/shichieichou_anatomy.png");

    public ShichieichouRenderer(EntityRendererProvider.Context context) {
        super(context, new ShichieichouModel(), 0.12F);
        this.addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack stack, MultiBufferSource buffers, int light, Shichieichou entity,
                               float limbSwing, float limbSwingAmount, float partialTick, float age,
                               float headYaw, float headPitch) {
                if (!entity.isInvisible()) {
                    RenderType type = entity.isClientFadeGhost()
                            ? RenderType.entityTranslucent(ANATOMY) : RenderType.entityCutoutNoCull(ANATOMY);
                    int bodyLight = LightTexture.pack(Math.max(LightTexture.block(light),
                            Math.min(12, ShichieichouClientConfig.MINIMUM_LIGHT.get() + 3)), LightTexture.sky(light));
                    getParentModel().renderAnatomy(stack, buffers.getBuffer(type), bodyLight,
                            getOverlayCoords(entity, 0.0F), -1);
                }
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(Shichieichou butterfly) {
        return butterfly.getTexture();
    }

    @Override
    protected RenderType getRenderType(Shichieichou entity, boolean visible, boolean translucent, boolean glowing) {
        if (visible && entity.isClientFadeGhost()) {
            return RenderType.entityTranslucent(this.getTextureLocation(entity));
        }
        return super.getRenderType(entity, visible, translucent, glowing);
    }

    @Override
    protected int getBlockLightLevel(Shichieichou entity, BlockPos pos) {
        return Math.max(ShichieichouClientConfig.MINIMUM_LIGHT.get(), super.getBlockLightLevel(entity, pos));
    }

    @Override
    protected void scale(Shichieichou entity, PoseStack stack, float partialTick) {
        float size = entity.getSizeModifier();
        stack.scale(size, size, size);
    }

    @Override
    protected void setupRotations(Shichieichou entity, PoseStack stack, float age, float yaw, float partialTick, float scale) {
        float phase = ShichieichouAnimation.phase(age - 1, entity.getId());
        stack.translate(0.0D, 0.12D + Mth.sin(phase - 0.5F) * 0.018D, 0.0D);
        super.setupRotations(entity, stack, age, yaw, partialTick, scale);
    }
}
