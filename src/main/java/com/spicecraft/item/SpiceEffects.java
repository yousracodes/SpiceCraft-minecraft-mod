package com.spicecraft.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.common.ForgeHooks;

/** Snapshots standard food effects, including the probability of each effect. */
public final class SpiceEffects {
    public record Entry(MobEffectInstance effect, float chance) {
        public Entry {
            effect = new MobEffectInstance(effect);
            if (!Float.isFinite(chance) || chance < 0 || chance > 1) {
                throw new IllegalArgumentException("Effect chance must be between zero and one");
            }
        }
    }

    private SpiceEffects() {}

    /** Source snapshots stay at full duration; only a serving receives this copy. */
    public static ListTag perServing(ListTag source, int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive");
        ListTag result = source.copy();
        for (Tag raw : result) {
            if (raw instanceof CompoundTag entry) divideDuration(entry.getCompound("effect"), capacity);
        }
        return result;
    }

    private static void divideDuration(CompoundTag tag, int capacity) {
        MobEffectInstance effect = MobEffectInstance.load(tag);
        if (effect == null) return;
        // Instant and infinite effects have no finite duration to distribute.
        if (!effect.getEffect().isInstantenous() && effect.getDuration() > 0) {
            tag.putInt("Duration", Math.max(1, effect.getDuration() / capacity));
        }
        if (tag.contains("HiddenEffect", Tag.TAG_COMPOUND)) {
            divideDuration(tag.getCompound("HiddenEffect"), capacity);
        }
    }

    public static ListTag fromFood(ItemStack food, LivingEntity eater) {
        var properties = food.getFoodProperties(eater);
        if (properties == null) throw new IllegalArgumentException("Source must be a standard edible item");
        ListTag result = new ListTag();
        for (var pair : properties.getEffects()) {
            if (pair.getFirst() == null) continue;
            add(result, pair.getFirst(), pair.getSecond());
        }
        if (food.getItem() instanceof SuspiciousStewItem && food.hasTag()) {
            ListTag effects = food.getTag().getList(SuspiciousStewItem.EFFECTS_TAG, Tag.TAG_COMPOUND);
            for (Tag raw : effects) {
                CompoundTag entry = (CompoundTag) raw;
                MobEffect effect = ForgeHooks.loadMobEffect(entry, "forge:effect_id",
                        MobEffect.byId(entry.getInt(SuspiciousStewItem.EFFECT_ID_TAG)));
                int duration = entry.contains(SuspiciousStewItem.EFFECT_DURATION_TAG, Tag.TAG_ANY_NUMERIC)
                        ? entry.getInt(SuspiciousStewItem.EFFECT_DURATION_TAG) : SuspiciousStewItem.DEFAULT_DURATION;
                if (effect != null) add(result, new MobEffectInstance(effect, duration), 1);
            }
        }
        return result;
    }

    private static void add(ListTag result, MobEffectInstance effect, float chance) {
        if (!Float.isFinite(chance) || chance < 0 || chance > 1) return;
        CompoundTag entry = new CompoundTag();
        entry.put("effect", effect.save(new CompoundTag()));
        entry.putFloat("chance", chance);
        result.add(entry);
    }

    public static List<Entry> read(ListTag tags) {
        List<Entry> result = new ArrayList<>();
        for (Tag raw : tags) {
            if (!(raw instanceof CompoundTag tag)) continue;
            MobEffectInstance effect = MobEffectInstance.load(tag.getCompound("effect"));
            float chance = tag.getFloat("chance");
            if (effect != null && Float.isFinite(chance) && chance >= 0 && chance <= 1) {
                result.add(new Entry(effect, chance));
            }
        }
        return List.copyOf(result);
    }

    public static void apply(ListTag tags, LivingEntity eater) {
        if (eater.level().isClientSide) return;
        for (Entry entry : read(tags)) {
            if (entry.chance() > 0 && eater.getRandom().nextFloat() < entry.chance()) {
                eater.addEffect(new MobEffectInstance(entry.effect()));
            }
        }
    }
}
