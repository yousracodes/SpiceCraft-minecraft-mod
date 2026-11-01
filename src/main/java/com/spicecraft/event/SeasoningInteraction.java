package com.spicecraft.event;

import com.spicecraft.SpiceCraft;
import com.spicecraft.item.SpiceBottleItem;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpiceCraft.MOD_ID)
public final class SeasoningInteraction {
    private SeasoningInteraction() {}

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getItemStack().getItem() instanceof SpiceBottleItem) {
            // Let the bottle's useOn handle seasoning instead of opening/activating the target block.
            event.setUseBlock(Event.Result.DENY);
            event.setUseItem(Event.Result.ALLOW);
        }
    }
}
