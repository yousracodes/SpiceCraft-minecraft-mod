package com.spicecraft.gametest;

import com.spicecraft.SpiceCraft;
import com.spicecraft.item.SpiceBottleData;
import com.spicecraft.machine.MachineRegistry;
import com.spicecraft.machine.SpiceMakerEntity;
import com.spicecraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
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
