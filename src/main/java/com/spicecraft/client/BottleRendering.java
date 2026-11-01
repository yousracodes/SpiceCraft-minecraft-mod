package com.spicecraft.client;

import com.spicecraft.SpiceCraft;
import com.spicecraft.item.SpiceBottleData;
import com.spicecraft.item.SpiceBottleItem;
import com.spicecraft.registry.ModItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpiceCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BottleRendering {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.SPICE_BOTTLE.get(),
                new ResourceLocation(SpiceCraft.MOD_ID, "filled"),
                (stack, level, entity, seed) -> SpiceBottleData.read(stack,
                        ((SpiceBottleItem) stack.getItem()).definition()).servings() > 0 ? 1 : 0));
    }
    @SubscribeEvent
    public static void colors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> tint == 0
                ? 0xFF000000 | SpiceBottleData.read(stack,
                        ((SpiceBottleItem) stack.getItem()).definition()).powderColor() : -1, ModItems.SPICE_BOTTLE.get());
    }
}
