package com.sange.ambientgogga;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.sange.ambientgogga.particle.ModParticles;
import com.sange.ambientgogga.entity.ModEntities;
import com.sange.ambientgogga.item.ButterflyBottleItem;
import com.sange.ambientgogga.world.ShichieichouSpawner;

@Mod(AmbientGogga.MODID)
public class AmbientGogga {
    public static final String MODID = "ambientgogga";
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredItem<ButterflyBottleItem> BUTTERFLY_BOTTLE = ITEMS.register(
            "butterfly_bottle",
            () -> new ButterflyBottleItem(new Item.Properties().stacksTo(1))
    );

    public static final DeferredItem<SpawnEggItem> BUTTERFLY_SPAWN_EGG = ITEMS.register(
            "butterfly_spawn_egg",
            () -> new SpawnEggItem(ModEntities.BUTTERFLY.get(), 12_594_947, 987_158, new Item.Properties())
    );

    public static final DeferredItem<Item> SHICHIEICHOU_MEMORY = ITEMS.register(
            "shichieichou_memory",
            () -> new Item(new Item.Properties())
    );

    public AmbientGogga(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ModParticles.register(modEventBus);
        ModEntities.register(modEventBus);
        ShichieichouSpawner.register();
        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(BUTTERFLY_SPAWN_EGG);
        }
    }
}
