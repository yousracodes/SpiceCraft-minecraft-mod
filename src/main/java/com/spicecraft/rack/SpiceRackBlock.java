package com.spicecraft.rack;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Compact four-shelf rack; bottles render at one quarter size. */
public final class SpiceRackBlock extends net.minecraft.world.level.block.BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape NORTH = Shapes.or(
            Block.box(3, 0, 15.5, 13, 16, 16),
            Block.box(3, 0, 12, 3.5, 16, 15.5),
            Block.box(12.5, 0, 12, 13, 16, 15.5),
            Block.box(3.5, 0, 12.5, 12.5, 0.5, 15.5),
            Block.box(3.5, 0.5, 12.25, 12.5, 0.85, 12.6),
            Block.box(3.5, 4, 12.5, 12.5, 4.5, 15.5),
            Block.box(3.5, 4.5, 12.25, 12.5, 4.85, 12.6),
            Block.box(3.5, 8, 12.5, 12.5, 8.5, 15.5),
            Block.box(3.5, 8.5, 12.25, 12.5, 8.85, 12.6),
            Block.box(3.5, 12, 12.5, 12.5, 12.5, 15.5),
            Block.box(3.5, 12.5, 12.25, 12.5, 12.85, 12.6),
            Block.box(3.5, 15.5, 15, 12.5, 16, 15.5));
    private static final VoxelShape[] SHAPES = new VoxelShape[4];
    static {
        SHAPES[0] = NORTH;
        for (int i = 1; i < 4; i++) {
            var parts = new java.util.ArrayList<VoxelShape>();
            SHAPES[i - 1].forAllBoxes((x1, y1, z1, x2, y2, z2) ->
                    parts.add(Shapes.box(1 - z2, y1, x1, 1 - z1, y2, x2)));
            SHAPES[i] = Shapes.or(Shapes.empty(), parts.toArray(VoxelShape[]::new));
        }
    }
    public SpiceRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[switch (state.getValue(FACING)) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; }];
    }
    @Override public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpiceRackEntity(pos, state);
    }
    public static int slot(BlockState state, net.minecraft.world.phys.Vec3 hit, BlockPos pos) {
        double x = (hit.x - pos.getX()) * 16, z = (hit.z - pos.getZ()) * 16;
        double localX = switch (state.getValue(FACING)) {
            case EAST -> z; case SOUTH -> 16 - x; case WEST -> 16 - z; default -> x;
        };
        int column = net.minecraft.util.Mth.clamp((int) Math.floor((localX - 3.5) / 2.25), 0, 3);
        int row = net.minecraft.util.Mth.clamp((int) Math.floor((hit.y - pos.getY()) * 4), 0, 3);
        return row * 4 + column;
    }
    @Override public net.minecraft.world.InteractionResult use(BlockState state, net.minecraft.world.level.Level level,
            BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        var held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof com.spicecraft.item.SpiceBottleItem) && !held.isEmpty())
            return net.minecraft.world.InteractionResult.PASS;
        if (!player.mayBuild() || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), held))
            return net.minecraft.world.InteractionResult.FAIL;
        if (!(level.getBlockEntity(pos) instanceof SpiceRackEntity rack)) return net.minecraft.world.InteractionResult.FAIL;
        int slot = slot(state, hit.getLocation(), pos);
        if (held.isEmpty()) {
            if (rack.bottle(slot).isEmpty()) return net.minecraft.world.InteractionResult.FAIL;
            if (!level.isClientSide) player.setItemInHand(hand, rack.takeBottle(slot));
        } else {
            if (!rack.bottle(slot).isEmpty()) return net.minecraft.world.InteractionResult.FAIL;
            if (!level.isClientSide && rack.insert(slot, held) && !player.getAbilities().instabuild) held.shrink(1);
        }
        if (!level.isClientSide) {
            player.inventoryMenu.broadcastChanges();
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.GLASS_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, .5F, 1.2F);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof SpiceRackEntity rack)
            for (int i = 0; i < SpiceRackEntity.SLOTS; i++) Block.popResource(level, pos, rack.takeBottle(i));
        super.onRemove(state, level, pos, replacement, moving);
    }
    @Override public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state) {
        return net.minecraft.world.level.material.PushReaction.BLOCK;
    }
    @Override public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.spicecraft.rack_size").withStyle(ChatFormatting.GRAY));
    }
}
