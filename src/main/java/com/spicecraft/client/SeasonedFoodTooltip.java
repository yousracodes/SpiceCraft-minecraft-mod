package com.spicecraft.client;

import com.spicecraft.SpiceCraft;
import com.spicecraft.item.SpiceEffects;
import com.spicecraft.item.SeasonedFoodData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Adds seasoning information to vanilla and modded items without replacing their tooltips. */
@Mod.EventBusSubscriber(modid = SpiceCraft.MOD_ID, value = Dist.CLIENT)
public final class SeasonedFoodTooltip {
    private SeasonedFoodTooltip() {}

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        for (var seasoning : SeasonedFoodData.seasonings(event.getItemStack())) {
        var source = ResourceLocation.tryParse(seasoning.getString("source_food"));
        if (source == null) continue;
        Component sourceName = BuiltInRegistries.ITEM.containsKey(source)
                ? BuiltInRegistries.ITEM.get(source).getDescription()
                : Component.literal(source.toString());
        Component spiceName = Component.translatable("item.spicecraft.spice_bottle.filled", sourceName);
        int color = seasoning.contains("powder_color", Tag.TAG_INT)
                ? seasoning.getInt("powder_color") & 0xFFFFFF : 0xFFAA00;
        event.getToolTip().add(Component.translatable("tooltip.spicecraft.seasoned_with", spiceName)
                .withStyle(style -> style.withColor(color)));
        for (var entry : SpiceEffects.read(seasoning.getList("effects", Tag.TAG_COMPOUND))) {
            var effect = entry.effect();
            event.getToolTip().add(Component.translatable("tooltip.spicecraft.effect",
                    Component.translatable(effect.getDescriptionId()), effect.getAmplifier() + 1,
                    effect.getDuration(), Math.round(entry.chance() * 100))
                    .withStyle(effect.getEffect().isBeneficial() ? ChatFormatting.BLUE : ChatFormatting.RED));
        }
        }
    }
}
