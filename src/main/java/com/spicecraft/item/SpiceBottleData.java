package com.spicecraft.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;

/** All SpiceCraft data stays inside our namespace, preserving other item data. */
public final class SpiceBottleData {
    private static final String KEY = "spicecraft";
    public static final int VERSION = 1;

    private SpiceBottleData() {}

    public record Contents(ResourceLocation sourceFood, int servings, int powderColor) {}

    public static Contents read(ItemStack stack, BottleDefinition definition) {
        CompoundTag root = stack.getTag();
        CompoundTag tag = root == null ? new CompoundTag() : root.getCompound(KEY);
        if (tag.contains("version") && tag.getInt("version") != VERSION) {
            return new Contents(null, 0, definition.defaultPowderColor());
        }
        ResourceLocation source = ResourceLocation.tryParse(tag.getString("source_food"));
        int servings = source == null ? 0 : Math.max(0, Math.min(definition.capacity(), tag.getInt("servings")));
        int color = tag.contains("powder_color") ? tag.getInt("powder_color") & 0xFFFFFF : definition.defaultPowderColor();
        return new Contents(servings == 0 ? null : source, servings, color);
    }

    public static void fill(ItemStack stack, ResourceLocation source, int servings, int color) {
        if (source == null || !BuiltInRegistries.ITEM.containsKey(source)) {
            throw new IllegalArgumentException("Unknown source food");
        }
        fillFromFood(stack, new ItemStack(BuiltInRegistries.ITEM.get(source)), servings, color, null);
    }

    /** Pass the actual input stack to preserve stack-dependent modded food effects. */
    public static void fillFromFood(ItemStack stack, ItemStack food, int servings, int color, LivingEntity eater) {
        if (!(stack.getItem() instanceof SpiceBottleItem bottle)) throw new IllegalArgumentException("Expected spice bottle");
        if (food.isEmpty() || servings < 1 || servings > bottle.definition().capacity()) {
            throw new IllegalArgumentException("Invalid spice contents");
        }
        ListTag effects = SpiceEffects.fromFood(food, eater);
        CompoundTag tag = stack.getOrCreateTagElement(KEY);
        tag.putInt("version", VERSION);
        tag.putString("source_food", BuiltInRegistries.ITEM.getKey(food.getItem()).toString());
        tag.putInt("servings", servings);
        tag.putInt("powder_color", color & 0xFFFFFF);
        tag.put("effects", effects);
    }

    public static ListTag effects(ItemStack stack) {
        if (!(stack.getItem() instanceof SpiceBottleItem bottle)
                || read(stack, bottle.definition()).servings() == 0) return new ListTag();
        CompoundTag tag = stack.getTagElement(KEY);
        if (tag == null) return new ListTag();
        if (tag.contains("effects", Tag.TAG_LIST)) {
            return SpiceEffects.perServing(tag.getList("effects", Tag.TAG_COMPOUND), bottle.definition().capacity());
        }
        // Legacy bottles stored only the food ID. Resolve those without modifying the stack.
        var source = read(stack, bottle.definition()).sourceFood();
        if (!BuiltInRegistries.ITEM.containsKey(source)) return new ListTag();
        ItemStack food = new ItemStack(BuiltInRegistries.ITEM.get(source));
        return food.getFoodProperties(null) == null ? new ListTag()
                : SpiceEffects.perServing(SpiceEffects.fromFood(food, null), bottle.definition().capacity());
    }

    /** Call on the server after a successful seasoning operation. */
    public static boolean consumeServing(ItemStack stack) {
        if (!(stack.getItem() instanceof SpiceBottleItem bottle)) return false;
        Contents contents = read(stack, bottle.definition());
        if (contents.servings() == 0) return false;
        if (contents.servings() == 1) {
            CompoundTag tag = stack.getOrCreateTagElement(KEY);
            tag.remove("source_food");
            tag.remove("servings");
            tag.remove("powder_color");
            tag.remove("effects");
        } else {
            // Keep the full source snapshot untouched: scaling is per serving, never cumulative.
            CompoundTag tag = stack.getOrCreateTagElement(KEY);
            tag.putInt("servings", contents.servings() - 1);
        }
        return true;
    }
}
