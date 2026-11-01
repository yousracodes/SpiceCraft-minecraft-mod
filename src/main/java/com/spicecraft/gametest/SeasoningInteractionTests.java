package com.spicecraft.gametest;

import com.spicecraft.SpiceCraft;
import com.spicecraft.item.SpiceBottleData;
import com.spicecraft.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SpiceCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SeasoningInteractionTests {
    @GameTest(template = "empty")
    public static void fullHungerBothHands(GameTestHelper helper) {
        for (InteractionHand hand : InteractionHand.values()) {
            Player player = helper.makeMockPlayer();
            player.getAbilities().instabuild = false;
            player.getFoodData().setFoodLevel(20);
            ItemStack bottle = bottle(player);
            ItemStack food = new ItemStack(Items.BREAD);
            helper.assertTrue(com.spicecraft.item.SeasonedFoodData.season(food, bottle, player), "Prepare seasoned food");
            player.setItemInHand(hand, food);
            var event = new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem(player, hand);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
            helper.assertTrue(event.isCanceled() && event.getCancellationResult().consumesAction()
                    && player.isUsingItem() && player.getUsedItemHand() == hand,
                    "Full-hunger survival player must start eating from " + hand);
            ItemStack original = food.copy();
            ItemStack result = food.finishUsingItem(helper.getLevel(), player);
            net.minecraftforge.event.ForgeEventFactory.onItemUseFinish(player, original, 0, result);
            helper.assertTrue(result.isEmpty(), "Survival meal must be consumed from " + hand);
            helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION), "Seasoning effects must apply after eating");
            player.stopUsingItem();
            player.setItemInHand(hand, new ItemStack(Items.BREAD));
            var plain = new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem(player, hand);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(plain);
            helper.assertTrue(!plain.isCanceled() && !player.isUsingItem(), "Plain food must keep vanilla hunger rules");
        }
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void seasonedFoodCanBeEatenInSurvival(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        player.getAbilities().instabuild = false;
        player.getFoodData().setFoodLevel(10);
        ItemStack bottle = bottle(player);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BREAD));
        ModItems.SPICE_BOTTLE.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(ModItems.SPICE_BOTTLE.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult()
                == InteractionResult.FAIL, "Filled bottle cooldown must block another seasoning attempt");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        var food = player.getOffhandItem();
        helper.assertTrue(food.use(helper.getLevel(), player, InteractionHand.OFF_HAND).getResult().consumesAction()
                && player.isUsingItem(), "Hungry survival player must be able to start eating seasoned food");
        player.stopUsingItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
        while (SpiceBottleData.consumeServing(bottle)) { }
        helper.assertTrue(ModItems.SPICE_BOTTLE.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult()
                == InteractionResult.PASS, "Empty bottle must also allow eating seasoned food");
        helper.succeed();
    }
    private static ItemStack bottle(Player player) {
        ItemStack bottle = ModItems.SPICE_BOTTLE.get().getDefaultInstance();
        SpiceBottleData.fill(bottle, ResourceLocation.tryParse("minecraft:golden_apple"), 5, 0xFFCC00);
        player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
        return bottle;
    }
    @GameTest(template = "empty")
    public static void stackAndRepeatProtection(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        ItemStack bottle = bottle(player);
        ItemStack food = new ItemStack(Items.BREAD, 3);
        food.getOrCreateTag().putString("custom_test", "preserved");
        player.setItemInHand(InteractionHand.OFF_HAND, food);
        ModItems.SPICE_BOTTLE.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(food.getCount() == 2 && food.getTagElement("spicecraft") == null, "Only one item may be seasoned");
        helper.assertTrue(SpiceBottleData.read(bottle, ModItems.GLASS).servings() == 4, "Use exactly one serving");
        ItemStack seasoned = ItemStack.EMPTY;
        for (ItemStack item : player.getInventory().items) if (item.is(Items.BREAD)) seasoned = item;
        helper.assertTrue(seasoned.getCount() == 1 && seasoned.getTagElement("spicecraft") != null, "Deliver the seasoned item to inventory");
        helper.assertTrue(seasoned.getTag().getString("custom_test").equals("preserved"), "Preserve existing food data");
        player.getCooldowns().removeCooldown(ModItems.SPICE_BOTTLE.get());
        player.setItemInHand(InteractionHand.OFF_HAND, seasoned.copy());
        ModItems.SPICE_BOTTLE.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(SpiceBottleData.read(bottle, ModItems.GLASS).servings() == 4, "Reject repeated seasoning without consuming a serving");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void singleItemAndLastServing(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        ItemStack bottle = bottle(player);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE));
        ModItems.SPICE_BOTTLE.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(SpiceBottleData.read(bottle, ModItems.GLASS).servings() == 5, "Invalid targets must not consume servings");
        for (int i = 0; i < 5; i++) {
            player.getCooldowns().removeCooldown(ModItems.SPICE_BOTTLE.get());
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BREAD));
            ModItems.SPICE_BOTTLE.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getOffhandItem().getTagElement("spicecraft") != null, "Single seasoned item must stay in off-hand");
        }
        helper.assertTrue(SpiceBottleData.read(bottle, ModItems.GLASS).servings() == 0 && SpiceBottleData.effects(bottle).isEmpty(), "Fifth serving must leave an empty reusable bottle");
        helper.succeed();
    }
}
