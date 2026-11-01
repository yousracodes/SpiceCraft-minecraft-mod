package com.spicecraft.bottle;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PlacedBottleBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static int slot(double x, double z) { return (x >= 0.5 ? 1 : 0) + (z >= 0.5 ? 2 : 0); }
    public static int slot(net.minecraft.world.phys.Vec3 hit, BlockPos pos) { return slot(hit.x - pos.getX(), hit.z - pos.getZ()); }
    public PlacedBottleBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction face, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return face == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, face, neighbor, level, pos, neighborPos);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = net.minecraft.world.phys.shapes.Shapes.empty();
        if (level.getBlockEntity(pos) instanceof PlacedBottleEntity entity) {
            for (int i = 0; i < 4; i++) if (!entity.bottle(i).isEmpty()) {
                int x = (i % 2) * 8, z = (i / 2) * 8;
                shape = net.minecraft.world.phys.shapes.Shapes.or(shape, Block.box(x + 1, 0, z + 1, x + 7, 13, z + 7));
            }
        }
        return shape;
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
    @Override public PushReaction getPistonPushReaction(BlockState state) { return PushReaction.DESTROY; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PlacedBottleEntity(pos, state); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        // The item's placement callback supplies the exact clicked quarter.
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof PlacedBottleEntity entity) {
            for (int i = 0; i < 4; i++) Block.popResource(level, pos, entity.takeBottle(i));
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
    @Override public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player) {
        return level.getBlockEntity(pos) instanceof PlacedBottleEntity entity ? entity.bottle(slot(target.getLocation(), pos)) : ItemStack.EMPTY;
    }

    @Override public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean harvest,
            net.minecraft.world.level.material.FluidState fluid) {
        if (!level.isClientSide) {
            var eye = player.getEyePosition();
            double reach = player.getAttributeValue(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get());
            var hit = level.clip(new net.minecraft.world.level.ClipContext(eye, eye.add(player.getLookAngle().scale(reach)),
                    net.minecraft.world.level.ClipContext.Block.OUTLINE, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos)) removeBottle(level, pos, slot(hit.getLocation(), pos));
        }
        // Individual removal replaces vanilla's whole-block destruction, including creative mode.
        return false;
    }

    public static void removeBottle(Level level, BlockPos pos, int slot) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof PlacedBottleEntity entity)) return;
        ItemStack bottle = entity.takeBottle(slot);
        if (bottle.isEmpty()) return;
        Block.popResource(level, pos, bottle);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.GLASS_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 0.4F, 1.3F);
        if (entity.isEmpty()) level.removeBlock(pos, false);
    }
}
