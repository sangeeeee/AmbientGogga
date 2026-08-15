package com.sange.ambientgogga.world;

import com.sange.ambientgogga.AmbientGogga;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;

public final class ModTags {
    public static final TagKey<Biome> BUTTERFLY_SPAWN_BIOMES = biomeTag("butterfly_spawn_biomes");
    public static final TagKey<Biome> SPAWNS_PLAINS_BUTTERFLIES = biomeTag("spawns_plains_butterflies");
    public static final TagKey<Biome> SPAWNS_FOREST_BUTTERFLIES = biomeTag("spawns_forest_butterflies");
    public static final TagKey<Biome> SPAWNS_MOUNTAIN_BUTTERFLIES = biomeTag("spawns_mountain_butterflies");

    /** Headwear in this tag does not frighten butterflies, matching TerrainCognita's wreath interaction. */
    public static final TagKey<Item> BUTTERFLY_FRIENDLY_HEADWEAR = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(AmbientGogga.MODID, "butterfly_friendly_headwear")
    );

    private ModTags() {
    }

    private static TagKey<Biome> biomeTag(String path) {
        return TagKey.create(
                Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath(AmbientGogga.MODID, path)
        );
    }
}
