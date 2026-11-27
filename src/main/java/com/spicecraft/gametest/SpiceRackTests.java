package com.spicecraft.gametest;

import com.spicecraft.SpiceCraft;
import com.spicecraft.rack.*;
import com.spicecraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SpiceCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpiceRackTests {
    @GameTest(template = "empty")
    public static void storageAndFacing(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var level = helper.getLevel();
        var player = helper.makeMockPlayer();
        player.getAbilities().instabuild = false;
        for (var facing : Direction.Plane.HORIZONTAL) {
            var state = SpiceRackRegistry.BLOCK.get().defaultBlockState().setValue(SpiceRackBlock.FACING, facing);
            level.setBlock(pos, state, 3);
            var rack = (SpiceRackEntity) level.getBlockEntity(pos);
            for (int slot = 0; slot < 16; slot++) {
                double x = (4.625 + slot % 4 * 2.25) / 16, z = .875;
                double wx = switch (facing) { case EAST -> 1-z; case SOUTH -> 1-x; case WEST -> z; default -> x; };
                double wz = switch (facing) { case EAST -> x; case SOUTH -> 1-z; case WEST -> 1-x; default -> z; };
                var point = new Vec3(pos.getX()+wx, pos.getY()+(slot/4*4+2.5)/16.0, pos.getZ()+wz);
                helper.assertTrue(SpiceRackBlock.slot(state, point, pos) == slot, "Slot selection follows facing");
                var bottle = ModItems.SPICE_BOTTLE.get().getDefaultInstance();
                bottle.setHoverName(Component.literal("Bottle " + slot));
                com.spicecraft.item.SpiceBottleData.fill(bottle, net.minecraft.resources.ResourceLocation.tryParse("minecraft:apple"), 3, 0xAABB00+slot);
                var original = bottle.copy();
                player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
                var hit = new BlockHitResult(point, facing, pos, false);
                state.use(level, player, InteractionHand.MAIN_HAND, hit);
                helper.assertTrue(bottle.isEmpty() && ItemStack.isSameItemSameTags(rack.bottle(slot), original), "Insert consumes one and preserves data");
                player.setItemInHand(InteractionHand.MAIN_HAND, original.copy());
                state.use(level, player, InteractionHand.MAIN_HAND, hit);
                helper.assertTrue(player.getMainHandItem().getCount() == 1, "Occupied slot rejects without consuming");
            }
            var restored = new SpiceRackEntity(pos, state);
            restored.load(rack.saveWithoutMetadata());
            for (int i=0; i<16; i++) helper.assertTrue(ItemStack.isSameItemSameTags(rack.bottle(i), restored.bottle(i)), "All 16 slots survive save/load");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            var point = new Vec3(pos.getX()+.5, pos.getY()+.15, pos.getZ()+.5);
            int target = SpiceRackBlock.slot(state, point, pos);
            var expected = rack.bottle(target);
            state.use(level, player, InteractionHand.MAIN_HAND, new BlockHitResult(point, facing, pos, false));
            helper.assertTrue(rack.bottle(target).isEmpty() && ItemStack.isSameItemSameTags(expected, player.getMainHandItem()), "Empty hand retrieves the selected bottle");
            level.removeBlock(pos, false);
        }
        helper.succeed();
    }
}
