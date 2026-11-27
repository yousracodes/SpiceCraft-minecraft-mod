package com.spicecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.spicecraft.SpiceCraft;
import com.spicecraft.machine.MachineRegistry;
import com.spicecraft.machine.SpiceMakerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpiceCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SpiceMakerRenderer implements BlockEntityRenderer<SpiceMakerEntity> {
    // The compact bay keeps the bottle readable without letting it dominate the machine frame.
    private static final float BOTTLE_SCALE = 0.6F;
    private static final float MACHINE_CENTER = 0.5F;
    private static final float TRAY_TOP_Y = 1.45714F;
    private static final float FOOD_Y = 22.45714F;
    private static final int VIEW_DISTANCE = 32;

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MachineRegistry.ENTITY.get(), context -> new SpiceMakerRenderer());
    }

    @Override
    public void render(SpiceMakerEntity machine, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        var bottle = machine.displayBottleForRender();
        if (!bottle.isEmpty()) {
            pose.pushPose();
            // Item rendering centers the model at (8,8,8); compensate to rest its base on the tray.
            pose.translate(MACHINE_CENTER, TRAY_TOP_Y / 16.0 + BOTTLE_SCALE / 2.0, 0.5);
            pose.scale(BOTTLE_SCALE, BOTTLE_SCALE, BOTTLE_SCALE);
            Minecraft.getInstance().getItemRenderer().renderStatic(bottle, ItemDisplayContext.NONE,
                    light, overlay, pose, buffers, machine.getLevel(), (int) machine.getBlockPos().asLong());
            pose.popPose();
        }

        var food = machine.displayFoodForRender();
        if (food.isEmpty()) return;
        var renderer = Minecraft.getInstance().getItemRenderer();
        boolean solidModel = renderer.getModel(food, machine.getLevel(), null, 0).isGui3d();
        pose.pushPose();
        // Center on the hopper mouth. Tilt flat food sprites so they are visible from above.
        pose.translate(MACHINE_CENTER, FOOD_Y / 16.0, 0.5);
        if (!solidModel) pose.mulPose(Axis.XP.rotationDegrees(65));
        float foodScale = solidModel ? 0.24F : 0.30F;
        pose.scale(foodScale, foodScale, foodScale);
        renderer.renderStatic(food, ItemDisplayContext.NONE, light, overlay,
                pose, buffers, machine.getLevel(), (int) machine.getBlockPos().asLong());
        pose.popPose();
    }

    @Override public int getViewDistance() { return VIEW_DISTANCE; }
}
