package com.spicecraft.event;

import com.spicecraft.SpiceCraft;
import com.spicecraft.item.SeasonedFoodData;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpiceCraft.MOD_ID)
public final class SeasoningEvents {
    @SubscribeEvent
    public static void finishedEating(LivingEntityUseItemEvent.Finish event) {
        if (!event.getEntity().level().isClientSide) {
            SeasonedFoodData.applyOnConsumption(event.getItem(), event.getEntity());
        }
    }
}
