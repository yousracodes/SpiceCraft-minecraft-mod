package com.spicecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.spicecraft.SpiceCraft;
import com.spicecraft.bottle.PlacedBottleBlock;
import com.spicecraft.bottle.PlacedBottleEntity;
import com.spicecraft.bottle.PlacedBottleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpiceCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PlacedBottleRenderer implements BlockEntityRenderer<PlacedBottleEntity> {
    private static final int VIEW_DISTANCE = 32;
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(PlacedBottleRegistry.ENTITY.get(), context -> new PlacedBottleRenderer());
    }
    @Override public void render(PlacedBottleEntity entity, float tick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        for (int slot = 0; slot < 4; slot++) {
        var bottle = entity.bottleForRender(slot);
        if (bottle.isEmpty()) continue;
        pose.pushPose();
        pose.translate(0.25 + (slot % 2) * 0.5, PlacedBottleBlock.BOTTLE_SCALE / 2.0,
                0.25 + (slot / 2) * 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-entity.getBlockState().getValue(PlacedBottleBlock.FACING).toYRot()));
        pose.scale(PlacedBottleBlock.BOTTLE_SCALE, PlacedBottleBlock.BOTTLE_SCALE, PlacedBottleBlock.BOTTLE_SCALE);
        Minecraft.getInstance().getItemRenderer().renderStatic(bottle, ItemDisplayContext.NONE, light, overlay,
                pose, buffers, entity.getLevel(), (int) entity.getBlockPos().asLong());
        pose.popPose();
        }
    }
    @Override public int getViewDistance() { return VIEW_DISTANCE; }
}
