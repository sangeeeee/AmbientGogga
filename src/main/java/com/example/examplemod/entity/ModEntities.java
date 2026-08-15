package com.example.examplemod.entity;

import com.example.examplemod.ExampleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ExampleMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Butterfly>> BUTTERFLY =
            ENTITY_TYPES.register("butterfly", () -> EntityType.Builder
                    .of(Butterfly::new, MobCategory.AMBIENT)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(5)
                    .build(ExampleMod.MODID + ":butterfly"));

    private ModEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModEntities::registerAttributes);
        modEventBus.addListener(ModEntities::registerSpawnPlacements);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(BUTTERFLY.get(), Butterfly.createAttributes().build());
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                BUTTERFLY.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Butterfly::canSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }
}
