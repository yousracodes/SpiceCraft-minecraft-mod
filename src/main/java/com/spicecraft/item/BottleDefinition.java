package com.spicecraft.item;

import net.minecraft.resources.ResourceLocation;

/** Fixed properties of a bottle type; contents belong to individual stacks. */
public record BottleDefinition(ResourceLocation id, int capacity, int defaultPowderColor) {
    public BottleDefinition {
        if (capacity < 1) throw new IllegalArgumentException("Bottle capacity must be positive");
        defaultPowderColor &= 0xFFFFFF;
    }
}
