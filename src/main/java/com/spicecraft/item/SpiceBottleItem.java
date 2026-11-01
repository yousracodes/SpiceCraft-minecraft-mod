package com.spicecraft.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class SpiceBottleItem extends BlockItem {
    private final BottleDefinition definition;

    public SpiceBottleItem(Properties properties, BottleDefinition definition) {
        super(com.spicecraft.bottle.PlacedBottleRegistry.BLOCK.get(), properties.stacksTo(1));
        this.definition = java.util.Objects.requireNonNull(definition);
    }

    public BottleDefinition definition() { return definition; }

    @Override public String getDescriptionId() { return getOrCreateDescriptionId(); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack bottle = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(bottle);
        ItemStack food = player.getOffhandItem();
        if (SpiceBottleData.read(bottle, definition).servings() == 0 && SeasonedFoodData.isSeasoned(food)) {
            // Filled bottles always season; empty bottles can pass through to eating.
            return InteractionResultHolder.pass(bottle);
        }
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(bottle);
        String error = SpiceBottleData.read(bottle, definition).servings() == 0 ? "empty"
                : food.isEmpty() || food.getFoodProperties(player) == null ? "hold_food" : null;
        if (error == null) error = SeasonedFoodData.rejection(food, SpiceBottleData.read(bottle, definition).sourceFood());
        if (error != null) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.spicecraft." + error), true);
            // Consume the interaction even on failure, preventing accidental off-hand eating.
            return InteractionResultHolder.consume(bottle);
        }
        if (!level.isClientSide) {
            // Capture before seasoning: the last serving clears the bottle's saved color.
            int powderColor = SpiceBottleData.read(bottle, definition).powderColor();
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
            var particles = new DustParticleOptions(new Vector3f(
                    (powderColor >> 16 & 255) / 255.0F,
                    (powderColor >> 8 & 255) / 255.0F,
                    (powderColor & 255) / 255.0F), 0.7F);
            ((ServerLevel) level).sendParticles(particles, point.x, point.y, point.z, 8, 0.12, 0.08, 0.12, 0.01);
        }
        return InteractionResultHolder.sidedSuccess(bottle, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            var level = context.getLevel();
            var clicked = context.getClickedPos();
            // Aim at either an existing bottle or the exposed counter beneath an empty quarter.
            var target = level.getBlockState(clicked).is(com.spicecraft.bottle.PlacedBottleRegistry.BLOCK.get()) ? clicked : clicked.above();
            if (level.getBlockEntity(target) instanceof com.spicecraft.bottle.PlacedBottleEntity entity) {
                var player = context.getPlayer();
                if (!player.mayBuild() || !level.mayInteract(player, target)
                        || !player.mayUseItemAt(target, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
                int slot = com.spicecraft.bottle.PlacedBottleBlock.slot(context.getClickLocation(), target);
                if (!entity.bottle(slot).isEmpty()) return InteractionResult.FAIL;
                if (!level.isClientSide && entity.insert(slot, context.getItemInHand())) {
                    if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
                    level.playSound(null, target, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1, 1);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (context.getClickedFace() != net.minecraft.core.Direction.UP) return InteractionResult.FAIL;
            return super.useOn(context);
        }
        return context.getPlayer() == null ? InteractionResult.PASS
                : use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }

    @Override protected boolean placeBlock(net.minecraft.world.item.context.BlockPlaceContext context,
            net.minecraft.world.level.block.state.BlockState state) {
        if (!super.placeBlock(context, state)) return false;
        if (!context.getLevel().isClientSide && context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof com.spicecraft.bottle.PlacedBottleEntity entity) {
            entity.insert(com.spicecraft.bottle.PlacedBottleBlock.slot(context.getClickLocation(), context.getClickedPos()), context.getItemInHand());
        }
        return true;
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
