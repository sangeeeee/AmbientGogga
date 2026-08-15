package com.sange.ambientgogga.client.renderer;

import com.sange.ambientgogga.client.model.ButterflyModel;
import com.sange.ambientgogga.entity.Butterfly;
import com.sange.ambientgogga.entity.Shichieichou;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ButterflyRenderer extends MobRenderer<Butterfly, ButterflyModel> {
    public ButterflyRenderer(EntityRendererProvider.Context context) {
        this(context, ButterflyModel.LAYER_LOCATION);
    }

    protected ButterflyRenderer(EntityRendererProvider.Context context, ModelLayerLocation layer) {
        super(context, new ButterflyModel(context.bakeLayer(layer)), 0.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(Butterfly butterfly) {
        return butterfly.getTexture();
    }

    @Override
    protected RenderType getRenderType(
            Butterfly butterfly,
            boolean bodyVisible,
            boolean translucent,
            boolean glowing
    ) {
        if (butterfly instanceof Shichieichou && bodyVisible) {
            return RenderType.entityCutoutNoCull(this.getTextureLocation(butterfly));
        }
        return super.getRenderType(butterfly, bodyVisible, translucent, glowing);
    }

    @Override
    protected int getBlockLightLevel(Butterfly butterfly, BlockPos pos) {
        return butterfly instanceof Shichieichou
                ? 15
                : super.getBlockLightLevel(butterfly, pos);
    }

    @Override
    protected void scale(Butterfly butterfly, PoseStack poseStack, float partialTick) {
        float scale = butterfly.getSizeModifier();
        poseStack.scale(scale, scale, scale);
    }

    @Override
    protected void setupRotations(
            Butterfly butterfly,
            PoseStack poseStack,
            float ageInTicks,
            float rotationYaw,
            float partialTick,
            float scale
    ) {
        if (!butterfly.isResting()) {
            poseStack.translate(0.0D, 0.1F + Mth.sin(ageInTicks * 0.3F) * 0.1F, 0.0D);
        }
        super.setupRotations(butterfly, poseStack, ageInTicks, rotationYaw, partialTick, scale);
    }
}
