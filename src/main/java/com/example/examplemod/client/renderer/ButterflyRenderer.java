package com.example.examplemod.client.renderer;

import com.example.examplemod.client.model.ButterflyModel;
import com.example.examplemod.entity.Butterfly;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class ButterflyRenderer extends MobRenderer<Butterfly, ButterflyModel> {
    public ButterflyRenderer(EntityRendererProvider.Context context) {
        super(context, new ButterflyModel(context.bakeLayer(ButterflyModel.LAYER_LOCATION)), 0.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(Butterfly butterfly) {
        return butterfly.getTexture();
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
