package com.sange.ambientgogga.client.renderer;

import com.sange.ambientgogga.client.model.ButterflyModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public final class ShichieichouRenderer extends ButterflyRenderer {
    public ShichieichouRenderer(EntityRendererProvider.Context context) {
        super(context, ButterflyModel.SHICHIEICHOU_LAYER_LOCATION);
    }
}
