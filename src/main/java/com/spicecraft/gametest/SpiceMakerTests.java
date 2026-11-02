package com.spicecraft.gametest;

import com.spicecraft.SpiceCraft;
import com.spicecraft.item.SpiceBottleData;
import com.spicecraft.machine.MachineRegistry;
import com.spicecraft.machine.SpiceMakerEntity;
import com.spicecraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SpiceCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpiceMakerTests {
    @GameTest(template = "empty")
    public static void colorFamiliesPreferGoldOverShadows(GameTestHelper helper) {
        var gold = new com.spicecraft.color.PowderColors();
        for (int i = 0; i < 30; i++) gold.add(0xFF704014);
        for (int i = 0; i < 12; i++) {
            gold.add(0xFFFFCC30); gold.add(0xFFEFB522); gold.add(0xFFFFDF55);
        }
        int result = gold.color();
        helper.assertTrue((result >> 16 & 255) > 200 && (result >> 8 & 255) > 150 && (result & 255) < 120,
                "Related gold shades must beat the brown shadow bucket");
        var dark = new com.spicecraft.color.PowderColors();
        for (int i = 0; i < 100; i++) dark.add(0xFF503020);
        dark.add(0xFFFFFF20);
        helper.assertTrue(dark.color() == 0x503020, "Tiny highlights must not recolor naturally dark food");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void powderSamplingAndPersistence(GameTestHelper helper) {
        var colors = new com.spicecraft.color.PowderColors();
        colors.add(0x00FF00FF); colors.add(0xFF000000); colors.add(0xFFFFFFFF);
        helper.assertTrue(colors.color() == -1, "Transparent pixels and outlines must not set powder color");
        for (int i = 0; i < 10; i++) colors.add(0xFFCC3020);
        colors.add(0xFF20CC30);
        helper.assertTrue(colors.color() == 0xCC3020, "Dominant red must not average with a green accent");
        var machine = machine(helper);
        inputs(machine);
        helper.assertTrue(!machine.acceptColor(new ItemStack(Items.APPLE), 0xCC3020), "Reject stale or mismatched food reports");
        helper.assertTrue(!machine.acceptColor(machine.getItem(0), -1), "Reject out-of-range color");
        helper.assertTrue(machine.acceptColor(machine.getItem(0), 0xCC3020), "Accept matching source sample");
        helper.assertTrue(!machine.acceptColor(machine.getItem(0), 0x20CC30), "Later viewers must not overwrite the first sample");
        var restored = new SpiceMakerEntity(machine.getBlockPos(), machine.getBlockState());
        restored.load(machine.saveWithoutMetadata());
        tick(helper, restored, 200);
        helper.assertTrue(SpiceBottleData.read(restored.getItem(2), ModItems.GLASS).powderColor() == 0xCC3020,
                "Sampled color must survive save/load and reach output bottle");
        machine.setItem(0, new ItemStack(Items.APPLE));
        tick(helper, machine, 200);
        helper.assertTrue(SpiceBottleData.read(machine.getItem(2), ModItems.GLASS).powderColor()
                == com.spicecraft.color.ColorConfig.FALLBACK.get(), "Changed input without sample must use fallback");
        helper.succeed();
    }
    private static SpiceMakerEntity machine(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(pos, MachineRegistry.BLOCK.get().defaultBlockState(), 3);
        return (SpiceMakerEntity) helper.getLevel().getBlockEntity(pos);
    }
    private static void tick(GameTestHelper helper, SpiceMakerEntity machine, int count) {
        for (int i = 0; i < count; i++) SpiceMakerEntity.tick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
    }
    private static void inputs(SpiceMakerEntity machine) {
        machine.setItem(0, new ItemStack(Items.GOLDEN_APPLE, 2));
        ItemStack bottle = ModItems.SPICE_BOTTLE.get().getDefaultInstance();
        bottle.setHoverName(Component.literal("My jar"));
        machine.setItem(1, bottle);
    }
    @GameTest(template = "empty")
    public static void displayedBottleFollowsInventory(GameTestHelper helper) {
        SpiceMakerEntity machine = machine(helper);
        helper.assertTrue(machine.displayBottle().isEmpty(), "Empty machine must not display a bottle");
        inputs(machine);
        helper.assertTrue(ItemStack.matches(machine.displayBottle(), machine.getItem(1)), "Display input bottle");
        var client = new SpiceMakerEntity(machine.getBlockPos(), machine.getBlockState());
        client.load(machine.getUpdateTag());
        helper.assertTrue(ItemStack.matches(client.displayBottle(), machine.getItem(1)), "Initial chunk data must include bottle");
        machine.acceptColor(machine.getItem(0), 0xCC3020);
        tick(helper, machine, 200);
        helper.assertTrue(ItemStack.matches(machine.displayBottle(), machine.getItem(2)), "Display completed output");
        client.load(machine.getUpdatePacket().getTag());
        helper.assertTrue(SpiceBottleData.read(client.displayBottle(), ModItems.GLASS).powderColor() == 0xCC3020,
                "Update packet must preserve finished bottle color");
        machine.removeItem(2, 1);
        client.load(machine.getUpdateTag());
        helper.assertTrue(client.displayBottle().isEmpty(), "Removing output must clear client display");
        inputs(machine);
        machine.setItem(1, ItemStack.EMPTY);
        helper.assertTrue(machine.displayBottle().isEmpty(), "Removing input must clear display");
        inputs(machine);
        machine.clearContent();
        client.load(machine.getUpdateTag());
        helper.assertTrue(client.displayBottle().isEmpty(), "Clearing inventory must clear client display");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void displayedFoodFollowsInventory(GameTestHelper helper) {
        SpiceMakerEntity machine = machine(helper);
        helper.assertTrue(machine.displayFood().isEmpty(), "Empty hopper must not display food");
        ItemStack food = new ItemStack(Items.APPLE, 2);
        food.setHoverName(Component.literal("Hopper apple"));
        machine.setItem(0, food);
        var client = new SpiceMakerEntity(machine.getBlockPos(), machine.getBlockState());
        client.load(machine.getUpdateTag());
        helper.assertTrue(ItemStack.matches(client.displayFood(), food.copyWithCount(1)),
                "Chunk data must display one food with its item data, even without a bottle");
        machine.setItem(0, new ItemStack(Items.CARROT));
        client.load(machine.getUpdatePacket().getTag());
        helper.assertTrue(client.displayFood().is(Items.CARROT), "Replacing food must update display");
        machine.setItem(1, ModItems.SPICE_BOTTLE.get().getDefaultInstance());
        tick(helper, machine, 200);
        client.load(machine.getUpdatePacket().getTag());
        helper.assertTrue(client.displayFood().isEmpty() && !client.displayBottle().isEmpty(),
                "Consuming the last food must clear hopper while keeping output bottle");
        machine.setItem(0, new ItemStack(Items.APPLE));
        machine.removeItem(0, 1);
        client.load(machine.getUpdateTag());
        helper.assertTrue(client.displayFood().isEmpty(), "Removing food must clear hopper");
        machine.setItem(0, new ItemStack(Items.STONE));
        helper.assertTrue(machine.displayFood().isEmpty(), "Invalid input must not appear as food");
        machine.setItem(0, new ItemStack(Items.APPLE));
        machine.clearContent();
        client.load(machine.getUpdateTag());
        helper.assertTrue(client.displayFood().isEmpty(), "Clearing machine must clear hopper");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void directBottleInteraction(GameTestHelper helper) {
        var machine = machine(helper);
        var player = helper.makeMockPlayer();
        player.getAbilities().instabuild = false;
        player.getAbilities().mayBuild = true;
        player.setShiftKeyDown(true);
        var hand = InteractionHand.MAIN_HAND;
        var hit = new BlockHitResult(Vec3.atCenterOf(machine.getBlockPos()), Direction.UP, machine.getBlockPos(), false);
        var bottle = ModItems.SPICE_BOTTLE.get().getDefaultInstance();
        bottle.setHoverName(Component.literal("Direct jar"));
        var original = bottle.copy();
        player.setItemInHand(hand, bottle);
        var result = ModItems.SPICE_BOTTLE.get().useOn(new UseOnContext(player, hand, hit));
        helper.assertTrue(result.consumesAction() && player.getItemInHand(hand).isEmpty()
                && ItemStack.matches(machine.getItem(1), original), "Sneaking bottle use must transfer bottle into machine");
        var extra = ModItems.SPICE_BOTTLE.get().getDefaultInstance();
        player.setItemInHand(hand, extra);
        ModItems.SPICE_BOTTLE.get().useOn(new UseOnContext(player, hand, hit));
        helper.assertTrue(extra.getCount() == 1 && ItemStack.matches(machine.getItem(1), original),
                "Occupied input must not overwrite or consume a bottle");
        player.setItemInHand(hand, ItemStack.EMPTY);
        machine.getBlockState().use(helper.getLevel(), player, hand, hit);
        helper.assertTrue(machine.getItem(1).isEmpty() && ItemStack.matches(player.getItemInHand(hand), original),
                "Empty hand must recover unprocessed bottle");
        ModItems.SPICE_BOTTLE.get().useOn(new UseOnContext(player, hand, hit));
        machine.setItem(0, new ItemStack(Items.APPLE));
        machine.acceptColor(machine.getItem(0), 0xCC3020);
        tick(helper, machine, 200);
        var filled = machine.getItem(2).copy();
        machine.setItem(1, extra.copy());
        machine.getBlockState().use(helper.getLevel(), player, hand, hit);
        helper.assertTrue(ItemStack.matches(player.getItemInHand(hand), filled) && machine.getItem(2).isEmpty()
                && !machine.getItem(1).isEmpty(), "Take output first and preserve its color and name");
        ModItems.SPICE_BOTTLE.get().useOn(new UseOnContext(player, hand, hit));
        helper.assertTrue(ItemStack.matches(player.getItemInHand(hand), filled), "Filled bottles cannot be inserted");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void droppedFoodIntake(GameTestHelper helper) {
        var machine = machine(helper);
        var pos = machine.getBlockPos();
        var level = helper.getLevel();
        var food = new ItemStack(Items.APPLE, 63);
        food.setHoverName(Component.literal("Dropped apples"));
        var dropped = new ItemEntity(level, pos.getX() + 0.6, pos.getY() + 1.02, pos.getZ() + 0.5, food.copy());
        level.addFreshEntity(dropped);
        tick(helper, machine, 1);
        helper.assertTrue(!dropped.isAlive() && ItemStack.matches(machine.getItem(0), food),
                "Top intake must collect edible stack and preserve item data");
        var overflow = new ItemEntity(level, pos.getX() + 0.6, pos.getY() + 1.02, pos.getZ() + 0.5, food.copyWithCount(3));
        level.addFreshEntity(overflow);
        tick(helper, machine, 1);
        helper.assertTrue(machine.getItem(0).getCount() == 64 && overflow.getItem().getCount() == 2 && overflow.isAlive(),
                "Merge to stack limit and leave excess in world");
        overflow.discard();
        machine.clearContent();
        var stone = new ItemEntity(level, pos.getX() + 0.6, pos.getY() + 1.02, pos.getZ() + 0.5, new ItemStack(Items.STONE));
        var side = new ItemEntity(level, pos.getX() + 1.5, pos.getY() + 1.02, pos.getZ() + 0.5, new ItemStack(Items.APPLE));
        level.addFreshEntity(stone);
        level.addFreshEntity(side);
        tick(helper, machine, 1);
        helper.assertTrue(machine.getItem(0).isEmpty() && stone.isAlive() && side.isAlive(), "Reject non-food and items beside machine");
        machine.setItem(0, new ItemStack(Items.CARROT));
        var mismatch = new ItemEntity(level, pos.getX() + 0.6, pos.getY() + 1.02, pos.getZ() + 0.5, new ItemStack(Items.APPLE));
        level.addFreshEntity(mismatch);
        tick(helper, machine, 1);
        helper.assertTrue(machine.getItem(0).is(Items.CARROT) && machine.getItem(0).getCount() == 1 && mismatch.isAlive(),
                "Different food must not replace occupied input");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void timingAndContents(GameTestHelper helper) {
        SpiceMakerEntity machine = machine(helper);
        inputs(machine);
        tick(helper, machine, 199);
        helper.assertTrue(machine.getItem(2).isEmpty() && machine.getItem(0).getCount() == 2, "Inputs must survive until tick 200");
        tick(helper, machine, 1);
        ItemStack output = machine.getItem(2);
        var contents = SpiceBottleData.read(output, ModItems.GLASS);
        helper.assertTrue(contents.servings() == 5 && contents.sourceFood().toString().equals("minecraft:golden_apple"), "Output must retain source and five servings");
        helper.assertTrue(output.getHoverName().getString().equals("My jar"), "Custom bottle name lost");
        helper.assertTrue(machine.getItem(0).getCount() == 1 && machine.getItem(1).isEmpty(), "Consume exactly one of each input");
        helper.assertTrue(!SpiceBottleData.effects(output).isEmpty(), "Source effects missing");
        machine.setItem(1, ModItems.SPICE_BOTTLE.get().getDefaultInstance());
        tick(helper, machine, 250);
        helper.assertTrue(machine.getItem(0).getCount() == 1 && !machine.getItem(1).isEmpty(), "Occupied output must block consumption");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void persistenceAndInvalidInputs(GameTestHelper helper) {
        SpiceMakerEntity machine = machine(helper);
        inputs(machine);
        tick(helper, machine, 80);
        SpiceMakerEntity restored = new SpiceMakerEntity(machine.getBlockPos(), machine.getBlockState());
        restored.load(machine.saveWithoutMetadata());
        helper.assertTrue(restored.progress() == 80 && restored.getItem(0).getCount() == 2, "Save/load lost inventory or progress");
        tick(helper, restored, 120);
        helper.assertTrue(!restored.getItem(2).isEmpty(), "Restored machine must finish at tick 200");
        machine.setItem(0, new ItemStack(Items.STONE));
        tick(helper, machine, 250);
        helper.assertTrue(machine.progress() == 0 && machine.getItem(2).isEmpty(), "Invalid food must reset progress");
        machine.setItem(0, new ItemStack(Items.APPLE));
        machine.setItem(1, restored.getItem(2).copy());
        tick(helper, machine, 250);
        helper.assertTrue(machine.getItem(2).isEmpty(), "Filled bottles must be rejected");
        helper.succeed();
    }
}
