package com.spicecraft.event;

import com.spicecraft.SpiceCraft;
import com.spicecraft.item.SeasonedFoodData;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.InteractionResult;

@Mod.EventBusSubscriber(modid = SpiceCraft.MOD_ID)
public final class SeasoningEvents {
    @SubscribeEvent
    public static void startEating(PlayerInteractEvent.RightClickItem event) {
        var player = event.getEntity();
        var food = event.getItemStack();
        if (!player.isSpectator() && !player.isUsingItem() && !player.canEat(false)
                && !player.getCooldowns().isOnCooldown(food.getItem())
                && SeasonedFoodData.isSeasoned(food) && food.getFoodProperties(player) != null) {
            // Vanilla rejects ordinary food at full hunger. Seasoned meals can be eaten for their effects.
            // Run on both logical sides so the eating animation and server use timer agree.
            player.startUsingItem(event.getHand());
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void finishedEating(LivingEntityUseItemEvent.Finish event) {
        if (!event.getEntity().level().isClientSide) {
            SeasonedFoodData.applyOnConsumption(event.getItem(), event.getEntity());
        }
    }
}
