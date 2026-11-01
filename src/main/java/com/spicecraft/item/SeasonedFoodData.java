package com.spicecraft.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class SeasonedFoodData {
    private SeasonedFoodData() {}

    public static boolean isSeasoned(ItemStack food) {
        CompoundTag tag = food.getTagElement("spicecraft");
        return tag != null && tag.contains("seasoning", Tag.TAG_COMPOUND)
                && tag.getCompound("seasoning").getInt("version") == 1;
    }

    /** Server-side operation on one separated food item, never a whole inventory stack. */
    public static boolean season(ItemStack food, ItemStack bottle, LivingEntity eater) {
        if (eater.level().isClientSide || food.getCount() != 1 || food.getFoodProperties(eater) == null
                || !(bottle.getItem() instanceof SpiceBottleItem item)) return false;
        var contents = SpiceBottleData.read(bottle, item.definition());
        CompoundTag existing = food.getTagElement("spicecraft");
        if (contents.servings() == 0 || (existing != null && existing.contains("seasoning"))) return false;
        CompoundTag seasoning = new CompoundTag();
        seasoning.putInt("version", 1);
        seasoning.putString("source_food", contents.sourceFood().toString());
        seasoning.putInt("powder_color", contents.powderColor());
        seasoning.put("effects", SpiceBottleData.effects(bottle));
        if (!SpiceBottleData.consumeServing(bottle)) return false;
        food.getOrCreateTagElement("spicecraft").put("seasoning", seasoning);
        return true;
    }

    public static void applyOnConsumption(ItemStack food, LivingEntity eater) {
        CompoundTag tag = food.getTagElement("spicecraft");
        if (tag == null || food.getFoodProperties(eater) == null) return;
        CompoundTag seasoning = tag.getCompound("seasoning");
        if (seasoning.getInt("version") == 1) {
            SpiceEffects.apply(seasoning.getList("effects", Tag.TAG_COMPOUND), eater);
        }
    }
}
