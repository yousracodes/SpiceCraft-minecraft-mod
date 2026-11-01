package com.spicecraft.item;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class SeasonedFoodData {
    public static final int MAX_SEASONINGS = 3;
    private SeasonedFoodData() {}

    /** Read-only compatibility with the original single-seasoning format. */
    public static List<CompoundTag> seasonings(ItemStack food) {
        CompoundTag root = food.getTagElement("spicecraft");
        if (root == null) return List.of();
        ListTag stored = root.getList("seasonings", Tag.TAG_COMPOUND);
        if (root.contains("seasonings") && root.getInt("seasoning_version") != 2) return List.of();
        if (!root.contains("seasonings") && root.contains("seasoning", Tag.TAG_COMPOUND)) {
            stored = new ListTag(); stored.add(root.getCompound("seasoning"));
        }
        List<CompoundTag> result = new ArrayList<>();
        Set<ResourceLocation> sources = new HashSet<>();
        for (int i = 0; i < Math.min(stored.size(), MAX_SEASONINGS); i++) {
            CompoundTag entry = stored.getCompound(i);
            ResourceLocation source = ResourceLocation.tryParse(entry.getString("source_food"));
            if (entry.getInt("version") != 1 || source == null || !sources.add(source)) continue;
            CompoundTag copy = entry.copy();
            ListTag effects = new ListTag();
            var original = entry.getList("effects", Tag.TAG_COMPOUND);
            for (int j = 0; j < Math.min(original.size(), SpiceEffects.MAX_EFFECTS); j++) effects.add(original.getCompound(j).copy());
            copy.put("effects", effects);
            result.add(copy);
        }
        return List.copyOf(result);
    }

    public static boolean isSeasoned(ItemStack food) { return !seasonings(food).isEmpty(); }

    /** Null means eligible. Unknown versions are never overwritten. */
    public static String rejection(ItemStack food, ResourceLocation source) {
        CompoundTag root = food.getTagElement("spicecraft");
        if (root != null && ((root.contains("seasonings") && root.getInt("seasoning_version") != 2)
                || (root.contains("seasoning") && root.getCompound("seasoning").getInt("version") != 1))) return "unsupported_seasoning";
        var entries = seasonings(food);
        for (var entry : entries) if (entry.getString("source_food").equals(source.toString())) return "duplicate_spice";
        return entries.size() >= MAX_SEASONINGS ? "seasoning_limit" : null;
    }

    /** Server operation on one separated food item. Validate completely before consuming a serving. */
    public static boolean season(ItemStack food, ItemStack bottle, LivingEntity eater) {
        if (eater.level().isClientSide || food.getCount() != 1 || food.getFoodProperties(eater) == null
                || !(bottle.getItem() instanceof SpiceBottleItem item)) return false;
        var contents = SpiceBottleData.read(bottle, item.definition());
        if (contents.servings() == 0 || rejection(food, contents.sourceFood()) != null) return false;
        ListTag entries = new ListTag();
        for (var entry : seasonings(food)) entries.add(entry);
        CompoundTag seasoning = new CompoundTag();
        seasoning.putInt("version", 1);
        seasoning.putString("source_food", contents.sourceFood().toString());
        seasoning.putInt("powder_color", contents.powderColor());
        seasoning.put("effects", SpiceBottleData.effects(bottle));
        entries.add(seasoning);
        if (!SpiceBottleData.consumeServing(bottle)) return false;
        CompoundTag root = food.getOrCreateTagElement("spicecraft");
        root.putInt("seasoning_version", 2);
        root.put("seasonings", entries);
        root.remove("seasoning");
        return true;
    }

    public static void applyOnConsumption(ItemStack food, LivingEntity eater) {
        if (eater.level().isClientSide || food.getFoodProperties(eater) == null) return;
        ListTag effects = new ListTag();
        for (var seasoning : seasonings(food)) {
            for (Tag effect : seasoning.getList("effects", Tag.TAG_COMPOUND)) effects.add(effect.copy());
        }
        SpiceEffects.apply(effects, eater);
    }
}
