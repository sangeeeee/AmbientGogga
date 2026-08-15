package com.sange.ambientgogga;

import com.sange.ambientgogga.client.model.ButterflyModel;
import com.sange.ambientgogga.client.particle.FireflyParticle;
import com.sange.ambientgogga.client.renderer.ButterflyRenderer;
import com.sange.ambientgogga.entity.ModEntities;
import com.sange.ambientgogga.particle.ModParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = AmbientGogga.MODID, dist = Dist.CLIENT)
public class AmbientGoggaClient {
    public AmbientGoggaClient(IEventBus modEventBus) {
        modEventBus.addListener(AmbientGoggaClient::registerParticleProviders);
        modEventBus.addListener(AmbientGoggaClient::registerEntityRenderers);
        modEventBus.addListener(AmbientGoggaClient::registerLayerDefinitions);
    }

    private static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.FIREFLY.get(), FireflyParticle.Provider::new);
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.BUTTERFLY.get(), ButterflyRenderer::new);
    }

    private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ButterflyModel.LAYER_LOCATION, ButterflyModel::createBodyLayer);
    }
}
