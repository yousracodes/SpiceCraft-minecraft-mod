package com.spicecraft.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class SpiceBottleItem extends Item {
    private final BottleDefinition definition;

    public SpiceBottleItem(Properties properties, BottleDefinition definition) {
        super(properties.stacksTo(1));
        this.definition = java.util.Objects.requireNonNull(definition);
    }

    public BottleDefinition definition() { return definition; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack bottle = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(bottle);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(bottle);
        ItemStack food = player.getOffhandItem();
        String error = SpiceBottleData.read(bottle, definition).servings() == 0 ? "empty"
                : food.isEmpty() || food.getFoodProperties(player) == null ? "hold_food"
                : food.getTagElement("spicecraft") != null && food.getTagElement("spicecraft").contains("seasoning")
                ? "already_seasoned" : null;
        if (error != null) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.spicecraft." + error), true);
            // Consume the interaction even on failure, preventing accidental off-hand eating.
            return InteractionResultHolder.consume(bottle);
        }
        if (!level.isClientSide) {
            ItemStack seasoned = food.copyWithCount(1);
            if (!SeasonedFoodData.season(seasoned, bottle, player)) return InteractionResultHolder.fail(bottle);
            if (food.getCount() == 1) {
                player.setItemInHand(InteractionHand.OFF_HAND, seasoned);
            } else {
                food.shrink(1);
                if (!player.getInventory().add(seasoned)) player.drop(seasoned, false);
            }
            player.getCooldowns().addCooldown(this, 8);
            player.inventoryMenu.broadcastChanges();
            level.playSound(null, player.blockPosition(), SoundEvents.SAND_PLACE, SoundSource.PLAYERS, 0.6F, 1.4F);
            var point = player.getEyePosition().add(player.getLookAngle().scale(0.6)).add(0, -0.3, 0);
            ((ServerLevel) level).sendParticles(ParticleTypes.COMPOSTER, point.x, point.y, point.z, 8, 0.12, 0.08, 0.12, 0.01);
        }
        return InteractionResultHolder.sidedSuccess(bottle, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return context.getPlayer() == null ? InteractionResult.PASS
                : use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }

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
