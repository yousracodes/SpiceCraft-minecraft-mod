package com.spicecraft.machine;

import com.spicecraft.item.SpiceBottleItem;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

public class SpiceMakerBlock extends BaseEntityBlock {
    public static final double MODEL_HEIGHT_PIXELS = 22.85714;
    public static final double MODEL_HEIGHT_BLOCKS = MODEL_HEIGHT_PIXELS / 16.0;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, MODEL_HEIGHT_PIXELS, 16);

    public SpiceMakerBlock(Properties properties) { super(properties); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SpiceMakerEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, MachineRegistry.ENTITY.get(), SpiceMakerEntity::tick);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            if (!player.mayBuild() || !level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, hit.getDirection(), player.getItemInHand(hand))) return InteractionResult.FAIL;
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof SpiceMakerEntity machine) {
                ItemStack held = player.getItemInHand(hand);
                if (held.getItem() instanceof SpiceBottleItem) {
                    if (!SpiceMakerEntity.isEmptyBottle(held)) {
                        player.displayClientMessage(Component.translatable("message.spicecraft.machine_empty_bottle"), true);
                    } else if (!machine.getItem(1).isEmpty()) {
                        player.displayClientMessage(Component.translatable("message.spicecraft.machine_bottle_occupied"), true);
                    } else {
                        machine.setItem(1, held.split(1));
                    }
                } else {
                    int slot = machine.getItem(2).isEmpty() ? 1 : 2;
                    ItemStack bottle = machine.removeItem(slot, 1);
                    if (held.isEmpty()) player.setItemInHand(hand, bottle);
                    else if (!bottle.isEmpty() && !player.getInventory().add(bottle)) player.drop(bottle, false);
                }
                player.inventoryMenu.broadcastChanges();
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide && player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof SpiceMakerEntity machine) {
            NetworkHooks.openScreen(server, machine, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.spicecraft.machine_insert").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.spicecraft.machine_take").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.spicecraft.machine_food").withStyle(ChatFormatting.GRAY));
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof SpiceMakerEntity machine) {
            Containers.dropContents(level, pos, machine);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
