package com.spicecraft.gametest;

import com.spicecraft.SpiceCraft;
import com.spicecraft.item.*;
import com.spicecraft.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SpiceCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MultiSeasoningTests {
    private static ItemStack bottle(String source, int color) {
        var bottle = ModItems.SPICE_BOTTLE.get().getDefaultInstance();
        SpiceBottleData.fill(bottle, ResourceLocation.tryParse(source), 5, color);
        return bottle;
    }
    @GameTest(template = "empty")
    public static void limitsMigrationAndPersistence(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        var food = new ItemStack(Items.BREAD);
        food.getOrCreateTag().putString("custom", "keep");
        var apple = bottle("minecraft:apple", 0xCC3020);
        helper.assertTrue(SeasonedFoodData.season(food, apple, player), "First spice must succeed");
        // Recreate a pre-list meal, then add another spice to trigger lossless migration.
        CompoundTag legacy = SeasonedFoodData.seasonings(food).get(0);
        food.getOrCreateTagElement("spicecraft").remove("seasonings");
        food.getOrCreateTagElement("spicecraft").remove("seasoning_version");
        food.getOrCreateTagElement("spicecraft").put("seasoning", legacy);
        var carrot = bottle("minecraft:carrot", 0xFF8800);
        helper.assertTrue(SeasonedFoodData.season(food, carrot, player), "Legacy meal must accept a second spice");
        helper.assertTrue(!food.getTagElement("spicecraft").contains("seasoning") && SeasonedFoodData.seasonings(food).size() == 2, "Migrate to list");
        helper.assertTrue(SeasonedFoodData.seasonings(food).get(0).getInt("powder_color") == 0xCC3020, "Preserve old spice color");
        helper.assertTrue(!SeasonedFoodData.season(food, apple, player) && SpiceBottleData.read(apple, ModItems.GLASS).servings() == 4, "Duplicate must not consume a serving");
        helper.assertTrue(SeasonedFoodData.season(food, bottle("minecraft:cookie", 0x885522), player), "Third spice must succeed");
        var fourth = bottle("minecraft:potato", 0xDDBB77);
        helper.assertTrue(!SeasonedFoodData.season(food, fourth, player) && SpiceBottleData.read(fourth, ModItems.GLASS).servings() == 5, "Fourth spice must not consume a serving");
        var restored = ItemStack.of(food.save(new CompoundTag()));
        helper.assertTrue(SeasonedFoodData.seasonings(restored).size() == 3 && restored.getTag().getString("custom").equals("keep"), "Save/load preserves list and unrelated NBT");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void normalClickAddsEverySpice(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        var food = new ItemStack(Items.BREAD);
        SeasonedFoodData.season(food, bottle("minecraft:apple", 0xCC3020), player);
        player.setItemInHand(InteractionHand.OFF_HAND, food);
        var carrot = bottle("minecraft:carrot", 0xFF8800);
        player.setItemInHand(InteractionHand.MAIN_HAND, carrot);
        ModItems.SPICE_BOTTLE.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(SeasonedFoodData.seasonings(player.getOffhandItem()).size() == 2
                && SpiceBottleData.read(carrot, ModItems.GLASS).servings() == 4, "Normal click adds the second spice without Shift");
        player.getCooldowns().removeCooldown(ModItems.SPICE_BOTTLE.get());
        var cookie = bottle("minecraft:cookie", 0x885522);
        player.setItemInHand(InteractionHand.MAIN_HAND, cookie);
        ModItems.SPICE_BOTTLE.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(SeasonedFoodData.seasonings(player.getOffhandItem()).size() == 3
                && SpiceBottleData.read(cookie, ModItems.GLASS).servings() == 4, "Normal click adds the third spice without Shift");
        player.getCooldowns().removeCooldown(ModItems.SPICE_BOTTLE.get());
        var fourth = bottle("minecraft:potato", 0xDDBB77);
        player.setItemInHand(InteractionHand.MAIN_HAND, fourth);
        ModItems.SPICE_BOTTLE.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(SpiceBottleData.read(fourth, ModItems.GLASS).servings() == 5 && !player.isUsingItem(),
                "Limit rejection must not consume spice or start eating");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void overlappingEffectsDoNotStack(GameTestHelper helper) {
        var player = helper.makeMockPlayer();
        ListTag effects = new ListTag();
        add(effects, 100, 0, 1); add(effects, 30, 1, 1); add(effects, 80, 1, 1); add(effects, 999, 4, 0);
        SpiceEffects.apply(effects, player);
        var speed = player.getEffect(MobEffects.MOVEMENT_SPEED);
        helper.assertTrue(speed != null && speed.getAmplifier() == 1 && speed.getDuration() == 80,
                "Select strongest successful effect and longest duration at that level; never sum or use failed chance");
        helper.succeed();
    }
    private static void add(ListTag effects, int duration, int amplifier, float chance) {
        var entry = new CompoundTag();
        entry.put("effect", new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, amplifier).save(new CompoundTag()));
        entry.putFloat("chance", chance);
        effects.add(entry);
    }
}
