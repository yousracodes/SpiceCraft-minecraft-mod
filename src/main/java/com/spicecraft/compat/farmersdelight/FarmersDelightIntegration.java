package com.spicecraft.compat.farmersdelight;

import com.spicecraft.item.SeasonedFoodData;
import com.spicecraft.item.SpiceBottleData;
import com.spicecraft.item.SpiceBottleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.joml.Vector3f;

/** Optional integration which never links against Farmer's Delight classes. */
public final class FarmersDelightIntegration {
    private static final ResourceLocation CUTTING_BOARD = new ResourceLocation("farmersdelight", "cutting_board");

    private FarmersDelightIntegration() {}

    public static boolean isCuttingBoard(Level level, BlockPos pos) {
        return CUTTING_BOARD.equals(BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()));
    }

    public static void seasonBoardItem(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        var player = event.getEntity();
        var bottle = event.getItemStack();

        // Own this interaction so the bottle cannot be inserted as the board's next item or used on the off-hand.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide || player.getCooldowns().isOnCooldown(bottle.getItem())) return;

        if (!player.mayBuild() || !level.mayInteract(player, event.getPos())
                || !player.mayUseItemAt(event.getPos(), event.getFace(), bottle)) return;

        var blockEntity = level.getBlockEntity(event.getPos());
        var handler = blockEntity == null
                ? java.util.Optional.<net.minecraftforge.items.IItemHandler>empty()
                : blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve();
        if (handler.isEmpty()) return;

        ItemStack boardItem = handler.get().extractItem(0, 1, true);
        String error = boardItem.isEmpty() || boardItem.getFoodProperties(player) == null
                ? "cutting_board_food" : null;
        if (bottle.getItem() instanceof SpiceBottleItem spiceBottle) {
            var contents = SpiceBottleData.read(bottle, spiceBottle.definition());
            if (contents.servings() == 0) error = "empty";
            else if (error == null) error = SeasonedFoodData.rejection(boardItem, contents.sourceFood());
        }
        if (error != null) {
            player.displayClientMessage(Component.translatable("message.spicecraft." + error), true);
            return;
        }

        int powderColor = SpiceBottleData.read(bottle, ((SpiceBottleItem) bottle.getItem()).definition()).powderColor();
        ItemStack removed = handler.get().extractItem(0, 1, false);
        if (removed.isEmpty()) return;
        ItemStack seasoned = removed.copyWithCount(1);
        if (!SeasonedFoodData.season(seasoned, bottle, player)) {
            handler.get().insertItem(0, removed, false);
            return;
        }
        if (!player.getInventory().add(seasoned)) player.drop(seasoned, false);
        player.getCooldowns().addCooldown(bottle.getItem(), 8);
        player.inventoryMenu.broadcastChanges();

        level.playSound(null, event.getPos(), SoundEvents.SAND_PLACE, SoundSource.PLAYERS, 0.6F, 1.4F);
        var color = new Vector3f((powderColor >> 16 & 255) / 255.0F,
                (powderColor >> 8 & 255) / 255.0F, (powderColor & 255) / 255.0F);
        ((ServerLevel) level).sendParticles(new DustParticleOptions(color, 0.7F),
                event.getPos().getX() + 0.5, event.getPos().getY() + 0.2, event.getPos().getZ() + 0.5,
                8, 0.18, 0.04, 0.18, 0.01);
    }
}
