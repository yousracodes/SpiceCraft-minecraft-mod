package com.spicecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.spicecraft.SpiceCraft;
import com.spicecraft.rack.SpiceRackBlock;
import com.spicecraft.rack.SpiceRackEntity;
import com.spicecraft.rack.SpiceRackRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpiceCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SpiceRackRenderer implements BlockEntityRenderer<SpiceRackEntity> {
    private static final int VIEW_DISTANCE = 32;
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SpiceRackRegistry.ENTITY.get(), context -> new SpiceRackRenderer());
    }
    @Override public void render(SpiceRackEntity rack, float tick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(.5, 0, .5);
        float angle = switch (rack.getBlockState().getValue(SpiceRackBlock.FACING)) {
            case EAST -> -90; case SOUTH -> 180; case WEST -> 90; default -> 0;
        };
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-.5, 0, -.5);
        for (int slot = 0; slot < SpiceRackEntity.SLOTS; slot++) {
            var bottle = rack.bottleForRender(slot);
            if (bottle.isEmpty()) continue;
            pose.pushPose();
            // Item models are centered on (8,8,8); align their bottom with the shelf top.
            pose.translate((4.625 + (slot % 4) * 2.25) / 16, (2.5 + (slot / 4) * 4) / 16, 8.0 / 16);
            pose.scale(.25F, .25F, .25F);
            Minecraft.getInstance().getItemRenderer().renderStatic(bottle, ItemDisplayContext.NONE, light, overlay,
                    pose, buffers, rack.getLevel(), (int) rack.getBlockPos().asLong() + slot);
            pose.popPose();
        }
        pose.popPose();
    }
    @Override public int getViewDistance() { return VIEW_DISTANCE; }
}
