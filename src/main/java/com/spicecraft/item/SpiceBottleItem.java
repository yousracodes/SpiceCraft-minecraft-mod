package com.spicecraft.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class SpiceBottleItem extends Item {
    private final BottleDefinition definition;

    public SpiceBottleItem(Properties properties, BottleDefinition definition) {
        super(properties.stacksTo(1));
        this.definition = java.util.Objects.requireNonNull(definition);
    }

    public BottleDefinition definition() { return definition; }

    @Override
    public Component getName(ItemStack stack) {
        var contents = SpiceBottleData.read(stack, definition);
        if (contents.sourceFood() == null) return super.getName(stack);
        Component food = BuiltInRegistries.ITEM.containsKey(contents.sourceFood())
                ? BuiltInRegistries.ITEM.get(contents.sourceFood()).getDescription()
                : Component.literal(contents.sourceFood().toString());
        return Component.translatable("item.spicecraft.spice_bottle.filled", food);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        var contents = SpiceBottleData.read(stack, definition);
        tooltip.add(Component.translatable("tooltip.spicecraft.servings", contents.servings(), definition.capacity())
                .withStyle(ChatFormatting.GRAY));
        for (var entry : SpiceEffects.read(SpiceBottleData.effects(stack))) {
            var effect = entry.effect();
            tooltip.add(Component.translatable("tooltip.spicecraft.effect",
                    Component.translatable(effect.getDescriptionId()), effect.getAmplifier() + 1,
                    effect.getDuration(), Math.round(entry.chance() * 100))
                    .withStyle(effect.getEffect().isBeneficial() ? ChatFormatting.BLUE : ChatFormatting.RED));
        }
        if (flag.isAdvanced() && contents.sourceFood() != null) {
            tooltip.add(Component.literal(contents.sourceFood().toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
