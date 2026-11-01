package com.spicecraft.gametest;

import com.spicecraft.SpiceCraft;
import com.spicecraft.bottle.PlacedBottleEntity;
import com.spicecraft.bottle.PlacedBottleRegistry;
import com.spicecraft.item.SpiceBottleData;
import com.spicecraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SpiceCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlacedBottleTests {
    @GameTest(template = "empty")
    public static void fourBottlesAndIndividualRecovery(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer();
        player.getAbilities().instabuild = false;
        player.setShiftKeyDown(true);
        var support = helper.absolutePos(new BlockPos(1, 0, 1));
        var pos = support.above();
        level.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
        ItemStack[] originals = new ItemStack[4];
        for (int slot = 0; slot < 4; slot++) {
            var bottle = ModItems.SPICE_BOTTLE.get().getDefaultInstance();
            SpiceBottleData.fill(bottle, ResourceLocation.tryParse("minecraft:apple"), slot + 1, 0xCC3020 + slot);
            bottle.setHoverName(Component.literal("Jar " + slot));
            originals[slot] = bottle.copy();
            player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
            var hit = new Vec3(support.getX() + 0.25 + (slot % 2) * 0.5, support.getY() + 1,
                    support.getZ() + 0.25 + (slot / 2) * 0.5);
            var result = ModItems.SPICE_BOTTLE.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(hit, Direction.UP, support, false)));
            helper.assertTrue(result.consumesAction() && bottle.isEmpty(), "Place quarter " + slot);
        }
        var entity = (PlacedBottleEntity) level.getBlockEntity(pos);
        var restored = new PlacedBottleEntity(pos, entity.getBlockState());
        restored.load(entity.saveWithoutMetadata());
        for (int slot = 0; slot < 4; slot++) helper.assertTrue(ItemStack.isSameItemSameTags(restored.bottle(slot), originals[slot]), "Preserve each bottle independently");
        var extra = ModItems.SPICE_BOTTLE.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, extra);
        var rejected = ModItems.SPICE_BOTTLE.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(new Vec3(pos.getX() + 0.25, pos.getY(), pos.getZ() + 0.25), Direction.UP, support, false)));
        helper.assertTrue(!rejected.consumesAction() && extra.getCount() == 1, "Occupied quarter must not consume or overwrite");
        player.setPos(pos.getX() + 0.25, pos.getY(), pos.getZ() - 1);
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                new Vec3(pos.getX() + 0.25, pos.getY() + 0.3, pos.getZ() + 0.25));
        var state = level.getBlockState(pos);
        state.onDestroyedByPlayer(level, pos, player, true, level.getFluidState(pos));
        helper.assertTrue(entity.bottle(0).isEmpty() && !entity.bottle(1).isEmpty() && !entity.bottle(2).isEmpty()
                && !entity.bottle(3).isEmpty(), "Left-click ray must remove only the aimed-at bottle");
        level.removeBlock(support, false);
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        helper.assertTrue(drops.size() == 4, "Individual removal plus support removal must return exactly four bottles");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void placeSaveAndRecover(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer();
        player.getAbilities().instabuild = false;
        player.setShiftKeyDown(true);
        BlockPos support = helper.absolutePos(new BlockPos(1, 0, 1));
        BlockPos pos = support.above();
        level.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
        var bottle = ModItems.SPICE_BOTTLE.get().getDefaultInstance();
        SpiceBottleData.fill(bottle, ResourceLocation.tryParse("minecraft:golden_apple"), 3, 0xCC3020);
        bottle.setHoverName(Component.literal("Kitchen jar"));
        ItemStack original = bottle.copy();
        player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
        var result = ModItems.SPICE_BOTTLE.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false)));
        helper.assertTrue(result.consumesAction() && bottle.isEmpty(), "Survival placement must consume one bottle");
        helper.assertTrue(level.getBlockState(pos).is(PlacedBottleRegistry.BLOCK.get()), "Place bottle above the surface");
        var entity = (PlacedBottleEntity) level.getBlockEntity(pos);
        helper.assertTrue(ItemStack.isSameItemSameTags(entity.bottle(), original), "Placement must preserve all bottle data");
        var restored = new PlacedBottleEntity(pos, entity.getBlockState());
        restored.load(entity.saveWithoutMetadata());
        helper.assertTrue(ItemStack.isSameItemSameTags(restored.bottle(), original), "Save/load must preserve bottle");
        helper.assertTrue(entity.getUpdateTag().contains("Items"), "Client sync must include bottle contents");
        helper.assertTrue(level.getBlockState(pos).getDestroySpeed(level, pos) == 0, "Bottle must break in one click");
        level.destroyBlock(pos, true);
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().getCount() == 1
                && ItemStack.isSameItemSameTags(drops.get(0).getItem(), original), "Breaking must return exactly one original bottle");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void removedSupportReturnsEmptyBottle(GameTestHelper helper) {
        var level = helper.getLevel();
        var support = helper.absolutePos(new BlockPos(1, 0, 1));
        var pos = support.above();
        level.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, PlacedBottleRegistry.BLOCK.get().defaultBlockState(), 3);
        ((PlacedBottleEntity) level.getBlockEntity(pos)).setBottle(ModItems.SPICE_BOTTLE.get().getDefaultInstance());
        level.removeBlock(support, false);
        helper.assertTrue(level.getBlockState(pos).isAir(), "Bottle cannot float without support");
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(ModItems.SPICE_BOTTLE.get()), "Support removal returns one empty bottle");
        helper.succeed();
    }
}
