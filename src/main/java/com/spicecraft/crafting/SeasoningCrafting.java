package com.spicecraft.crafting;

import com.spicecraft.item.SeasonedFoodData;
import com.spicecraft.item.SpiceEffects;
import java.util.HashSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;

/** Called at crafting-grid result assembly, before previews or inventory transfers copy the output. */
public final class SeasoningCrafting {
    private SeasoningCrafting() {}

    public static ItemStack assemble(CraftingRecipe recipe, Container container, RegistryAccess registries) {
        var grid = (CraftingContainer) container;
        ItemStack output = recipe.assemble(grid, registries);
        if (!CraftingConfig.ENABLED.get() || output.isEmpty() || output.getFoodProperties(null) == null
                || CraftingConfig.EXCLUDED_RECIPES.get().contains(recipe.getId().toString())) return output;
        // Let recipes that explicitly supply seasoning (including unknown future formats) own their output.
        CompoundTag existing = output.getTagElement("spicecraft");
        if (existing != null && (existing.contains("seasonings") || existing.contains("seasoning")
                || existing.contains("seasoning_version"))) return output;

        boolean seasonedInput = false;
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            if (SeasonedFoodData.isSeasoned(grid.getItem(slot))) { seasonedInput = true; break; }
        }
        if (!seasonedInput) return output;

        var remaining = recipe.getRemainingItems(grid);
        var returnedSources = new HashSet<String>();
        for (var remainder : remaining) {
            for (var spice : SeasonedFoodData.seasonings(remainder)) returnedSources.add(spice.getString("source_food"));
        }
        var sources = new HashSet<String>();
        var inherited = new ListTag();
        for (int slot = 0; slot < grid.getContainerSize(); slot++) {
            ItemStack input = grid.getItem(slot);
            if (input.isEmpty() || input.getFoodProperties(null) == null) continue;
            // A returned seasoned ingredient wasn't spent, so it cannot fund another seasoned meal.
            if (slot < remaining.size() && ItemStack.isSameItemSameTags(input, remaining.get(slot))) continue;
            for (CompoundTag seasoning : SeasonedFoodData.seasonings(input)) {
                if (inherited.size() >= SeasonedFoodData.MAX_SEASONINGS) break;
                if (returnedSources.contains(seasoning.getString("source_food"))) continue;
                if (!sources.add(seasoning.getString("source_food"))) continue;
                seasoning.put("effects", splitEffects(seasoning.getList("effects", Tag.TAG_COMPOUND), output.getCount()));
                inherited.add(seasoning);
            }
        }
        if (inherited.isEmpty()) return output;
        // A custom recipe may return a shared template; never mutate the recipe's original stack.
        output = output.copy();
        CompoundTag root = output.getOrCreateTagElement("spicecraft");
        root.putInt("seasoning_version", 2);
        root.put("seasonings", inherited);
        return output;
    }

    private static ListTag splitEffects(ListTag effects, int portions) {
        var result = new ListTag();
        for (var entry : SpiceEffects.read(effects)) {
            if (result.size() >= SpiceEffects.MAX_EFFECTS) break;
            CompoundTag effect = entry.effect().save(new CompoundTag());
            boolean nonFinite = containsNonFiniteEffect(effect);
            if (!splitDuration(effect, portions)) continue;
            var scaled = new CompoundTag();
            scaled.put("effect", effect);
            // Instant and infinite effects have no finite duration budget; divide their probability instead.
            scaled.putFloat("chance", entry.chance() / (nonFinite ? portions : 1));
            result.add(scaled);
        }
        return result;
    }

    private static boolean containsNonFiniteEffect(CompoundTag tag) {
        var effect = MobEffectInstance.load(tag);
        return effect != null && (effect.getEffect().isInstantenous() || effect.getDuration() == -1
                || (tag.contains("HiddenEffect", Tag.TAG_COMPOUND) && containsNonFiniteEffect(tag.getCompound("HiddenEffect"))));
    }

    private static boolean splitDuration(CompoundTag tag, int portions) {
        var effect = MobEffectInstance.load(tag);
        if (effect == null) return false;
        if (!effect.getEffect().isInstantenous() && effect.getDuration() != -1) {
            int duration = effect.getDuration() / portions;
            if (duration <= 0) return false; // Rounding up would create effect duration in recipe loops.
            tag.putInt("Duration", duration);
        }
        if (tag.contains("HiddenEffect", Tag.TAG_COMPOUND)
                && !splitDuration(tag.getCompound("HiddenEffect"), portions)) tag.remove("HiddenEffect");
        return true;
    }
}
