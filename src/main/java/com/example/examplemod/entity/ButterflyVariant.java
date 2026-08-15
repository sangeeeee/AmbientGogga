package com.example.examplemod.entity;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.world.ModTags;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

public enum ButterflyVariant {
    PEACOCK(0, "peacock", 0.60F, 0.15F),
    BRIMSTONE(1, "brimstone", 0.50F, 0.10F),
    CABBAGE_WHITE(2, "cabbage_white", 0.60F, 0.15F),
    COMMON_BLUE(3, "common_blue", 0.50F, 0.10F),
    ORANGE_TIP(4, "orange_tip", 0.50F, 0.15F),
    MONARCH(5, "monarch", 0.70F, 0.15F),
    WHITE_ADMIRAL(6, "white_admiral", 0.60F, 0.10F),
    RED_ADMIRAL(7, "red_admiral", 0.60F, 0.15F),
    SPECKLED_WOOD(8, "speckled_wood", 0.50F, 0.10F),
    PURPLE_EMPEROR(9, "purple_emperor", 0.70F, 0.15F),
    RINGLET(10, "ringlet", 0.70F, 0.15F),
    SWALLOWTAIL(11, "swallowtail", 0.70F, 0.15F),
    MOURNING_CLOAK(12, "mourning_cloak", 0.70F, 0.15F);

    private static final ButterflyVariant[] VALUES = Arrays.stream(values())
            .sorted((left, right) -> Integer.compare(left.index, right.index))
            .toArray(ButterflyVariant[]::new);
    private static final Map<String, ButterflyVariant> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(ButterflyVariant::serializedName, Function.identity()));

    private static final WeightedVariant[] COMMON = {
            new WeightedVariant(CABBAGE_WHITE, 5),
            new WeightedVariant(PEACOCK, 4),
            new WeightedVariant(MONARCH, 3),
            new WeightedVariant(RED_ADMIRAL, 2)
    };
    private static final WeightedVariant[] PLAINS = {
            new WeightedVariant(BRIMSTONE, 5),
            new WeightedVariant(COMMON_BLUE, 4),
            new WeightedVariant(ORANGE_TIP, 1)
    };
    private static final WeightedVariant[] FOREST = {
            new WeightedVariant(WHITE_ADMIRAL, 5),
            new WeightedVariant(SPECKLED_WOOD, 4),
            new WeightedVariant(PURPLE_EMPEROR, 1)
    };
    private static final WeightedVariant[] MOUNTAIN = {
            new WeightedVariant(RINGLET, 5),
            new WeightedVariant(SWALLOWTAIL, 3),
            new WeightedVariant(MOURNING_CLOAK, 1)
    };

    private final int index;
    private final String serializedName;
    private final ResourceLocation texture;
    private final float averageSize;
    private final float sizeVariation;

    ButterflyVariant(int index, String serializedName, float averageSize, float sizeVariation) {
        this.index = index;
        this.serializedName = serializedName;
        this.texture = ResourceLocation.fromNamespaceAndPath(
                ExampleMod.MODID,
                "textures/entity/butterfly/" + serializedName + ".png"
        );
        this.averageSize = averageSize;
        this.sizeVariation = sizeVariation;
    }

    public int index() {
        return this.index;
    }

    public String serializedName() {
        return this.serializedName;
    }

    public ResourceLocation texture() {
        return this.texture;
    }

    public float randomSize(RandomSource random) {
        return Mth.lerp(random.nextFloat(), this.averageSize - this.sizeVariation, this.averageSize + this.sizeVariation);
    }

    public static ButterflyVariant byIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : PEACOCK;
    }

    public static ButterflyVariant byName(String name) {
        return BY_NAME.getOrDefault(name, PEACOCK);
    }

    public static ButterflyVariant randomForBiome(Holder<Biome> biome, RandomSource random) {
        WeightedVariant[] pool = COMMON;
        if (random.nextDouble() < 0.66D) {
            if (biome.is(ModTags.SPAWNS_PLAINS_BUTTERFLIES)) {
                pool = PLAINS;
            } else if (biome.is(ModTags.SPAWNS_FOREST_BUTTERFLIES)) {
                pool = FOREST;
            } else if (biome.is(ModTags.SPAWNS_MOUNTAIN_BUTTERFLIES)) {
                pool = MOUNTAIN;
            }
        }
        return pickWeighted(pool, random);
    }

    private static ButterflyVariant pickWeighted(WeightedVariant[] pool, RandomSource random) {
        int totalWeight = 0;
        for (WeightedVariant entry : pool) {
            totalWeight += entry.weight;
        }

        int selected = random.nextInt(totalWeight);
        for (WeightedVariant entry : pool) {
            selected -= entry.weight;
            if (selected < 0) {
                return entry.variant;
            }
        }
        return PEACOCK;
    }

    private record WeightedVariant(ButterflyVariant variant, int weight) {
    }
}
