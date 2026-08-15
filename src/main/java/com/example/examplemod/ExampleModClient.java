package com.example.examplemod;

import com.example.examplemod.client.model.ButterflyModel;
import com.example.examplemod.client.particle.FireflyParticle;
import com.example.examplemod.client.renderer.ButterflyRenderer;
import com.example.examplemod.entity.ModEntities;
import com.example.examplemod.particle.ModParticles;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = ExampleMod.MODID, dist = Dist.CLIENT)
public class ExampleModClient {
    public ExampleModClient(ModContainer container, IEventBus modEventBus) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(ExampleModClient::onClientSetup);
        modEventBus.addListener(ExampleModClient::registerParticleProviders);
        modEventBus.addListener(ExampleModClient::registerEntityRenderers);
        modEventBus.addListener(ExampleModClient::registerLayerDefinitions);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        ExampleMod.LOGGER.info("HELLO FROM CLIENT SETUP");
        ExampleMod.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
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
