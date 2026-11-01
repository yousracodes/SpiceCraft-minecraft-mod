package com.spicecraft.client;

import com.spicecraft.machine.MachineRegistry;
import com.spicecraft.machine.SpiceMakerEntity;
import com.spicecraft.machine.SpiceMakerMenu;
import com.spicecraft.SpiceCraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = SpiceCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SpiceMakerScreen extends AbstractContainerScreen<SpiceMakerMenu> {
    public SpiceMakerScreen(SpiceMakerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 180;
        inventoryLabelY = 87;
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) { event.enqueueWork(() -> MenuScreens.register(MachineRegistry.MENU.get(), SpiceMakerScreen::new)); }
    private void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        graphics.fill(x, y, x + 17, y + 17, 0xFFFFFFFF);
        graphics.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF373737);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFFFFFFF);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + imageHeight - 4, 0xFFC6C6C6);
        slot(g, x + 44, y + 24); slot(g, x + 44, y + 57); slot(g, x + 116, y + 40);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) slot(g, x + 8 + col * 18, y + 98 + row * 18);
        for (int col = 0; col < 9; col++) slot(g, x + 8 + col * 18, y + 156);
        g.drawString(font, Component.translatable("gui.spicecraft.food"), x + 8, y + 28, 0x404040, false);
        g.drawString(font, Component.translatable("gui.spicecraft.bottle"), x + 8, y + 61, 0x404040, false);
        g.fill(x + 76, y + 46, x + 104, y + 51, 0xFF8B8B8B);
        int width = menu.progress() * 28 / SpiceMakerEntity.PROCESS_TICKS;
        g.fill(x + 76, y + 46, x + 76 + width, y + 51, 0xFFF0C65A);
        g.drawString(font, Component.translatable("gui.spicecraft.seconds", (SpiceMakerEntity.PROCESS_TICKS - menu.progress() + 19) / 20), x + 77, y + 62, 0x404040, false);
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
