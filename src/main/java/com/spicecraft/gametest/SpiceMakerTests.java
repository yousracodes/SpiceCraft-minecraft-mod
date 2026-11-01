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
