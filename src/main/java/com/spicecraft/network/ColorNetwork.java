package com.spicecraft.network;

import com.spicecraft.SpiceCraft;
import com.spicecraft.machine.SpiceMakerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.Optional;
import java.util.function.Supplier;

public final class ColorNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SpiceCraft.MOD_ID, "colors"), () -> "1", "1"::equals, "1"::equals);
    public static void register() {
        CHANNEL.registerMessage(0, Report.class, Report::encode, Report::decode, Report::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    public record Report(int containerId, BlockPos pos, ItemStack source, int color) {
        private void encode(FriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId); buffer.writeBlockPos(pos); buffer.writeItem(source); buffer.writeInt(color);
        }
        private static Report decode(FriendlyByteBuf buffer) {
            return new Report(buffer.readVarInt(), buffer.readBlockPos(), buffer.readItem(), buffer.readInt());
        }
        private void handle(Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> {
                var player = context.getSender();
                if (player != null && player.containerMenu instanceof SpiceMakerMenu menu
                        && menu.containerId == containerId && menu.position().equals(pos) && menu.stillValid(player)
                        && color >= 0 && color <= 0xFFFFFF) {
                    menu.acceptColor(source, color);
                }
            });
            context.setPacketHandled(true);
        }
    }
    private ColorNetwork() {}
}
