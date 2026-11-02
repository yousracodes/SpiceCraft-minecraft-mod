package com.spicecraft.gametest;

import com.spicecraft.SpiceCraft;
import com.spicecraft.crafting.CraftingConfig;
import com.spicecraft.crafting.SeasoningCrafting;
import com.spicecraft.item.SeasonedFoodData;
import com.spicecraft.item.SpiceEffects;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SpiceCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CraftingSeasoningTests {
    private static final ResourceLocation ID = new ResourceLocation(SpiceCraft.MOD_ID, "test_seasoned_salad");

    private static net.minecraftforge.common.util.FakePlayer player(GameTestHelper helper) {
        return new net.minecraftforge.common.util.FakePlayer(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "SpiceCraftTest"));
    }

    private static CompoundTag spice(String source, int color, int duration) {
        var spice = new CompoundTag();
        spice.putInt("version", 1);
        spice.putString("source_food", source);
        spice.putInt("powder_color", color);
        var effect = new CompoundTag();
        effect.put("effect", new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration).save(new CompoundTag()));
        effect.putFloat("chance", 1);
        var effects = new ListTag();
        effects.add(effect);
        spice.put("effects", effects);
        return spice;
    }

    private static ItemStack food(CompoundTag... spices) {
        var food = new ItemStack(Items.BAKED_POTATO);
        var entries = new ListTag();
        for (var spice : spices) entries.add(spice.copy());
        var root = food.getOrCreateTagElement("spicecraft");
        root.putInt("seasoning_version", 2);
        root.put("seasonings", entries);
        return food;
    }

    private static ShapelessRecipe recipe(ItemStack output) {
        return new ShapelessRecipe(ID, "", CraftingBookCategory.MISC, output,
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.BAKED_POTATO), Ingredient.of(Items.CARROT)));
    }

    @GameTest(template = "empty")
    public static void inheritanceBudgetsAndCompatibility(GameTestHelper helper) {
        var player = player(helper);
        var grid = player.inventoryMenu.getCraftSlots();
        var apple = spice("minecraft:apple", 0xCC3020, 101);
        var carrot = spice("minecraft:carrot", 0xFF8800, 80);
        grid.setItem(0, food(apple));
        var other = food(apple, carrot);
        // Only the item type changes: retain independent ingredient seasoning data.
        var seasonedCarrot = new ItemStack(Items.CARROT);
        seasonedCarrot.setTag(other.getTag().copy());
        grid.setItem(1, seasonedCarrot);
        var template = new ItemStack(Items.MUSHROOM_STEW, 2);
        template.getOrCreateTag().putString("othermod", "preserved");
        template.getOrCreateTagElement("spicecraft").putString("unrelated", "preserved");
        var recipe = recipe(template);
        var output = SeasoningCrafting.assemble(recipe, grid, helper.getLevel().registryAccess());
        var spices = SeasonedFoodData.seasonings(output);
        helper.assertTrue(spices.size() == 2, "Combine unique spices without duplicate source entries");
        helper.assertTrue(spices.get(0).getInt("powder_color") == 0xCC3020
                && SpiceEffects.read(spices.get(0).getList("effects", Tag.TAG_COMPOUND)).get(0).effect().getDuration() == 50,
                "Keep color and divide finite duration by recipe output count, rounding down");
        helper.assertTrue(output.getTag().getString("othermod").equals("preserved")
                && output.getTagElement("spicecraft").getString("unrelated").equals("preserved")
                && !SeasonedFoodData.isSeasoned(template), "Preserve output NBT without mutating recipe template");
        helper.assertTrue(SpiceEffects.read(SeasonedFoodData.seasonings(grid.getItem(0)).get(0)
                .getList("effects", Tag.TAG_COMPOUND)).get(0).effect().getDuration() == 101, "Preview must not mutate ingredients");

        var third = spice("minecraft:cookie", 0x886633, 60);
        var fourth = spice("minecraft:melon_slice", 0xEE4466, 60);
        grid.setItem(0, food(apple, carrot, third));
        var fourthFood = new ItemStack(Items.CARROT);
        fourthFood.setTag(food(fourth).getTag().copy());
        grid.setItem(1, fourthFood);
        helper.assertTrue(SeasonedFoodData.seasonings(SeasoningCrafting.assemble(recipe, grid,
                helper.getLevel().registryAccess())).size() == 3, "Crafting must respect three-spice limit");

        var ownOutput = food(fourth);
        ownOutput.getOrCreateTag().putString("custom", "recipe-owned");
        var owned = SeasoningCrafting.assemble(recipe(ownOutput), grid, helper.getLevel().registryAccess());
        helper.assertTrue(ItemStack.matches(owned, ownOutput), "Recipe-owned seasoning must remain untouched");
        var unknown = new ItemStack(Items.BREAD);
        unknown.getOrCreateTagElement("spicecraft").putInt("seasoning_version", 999);
        helper.assertTrue(ItemStack.matches(SeasoningCrafting.assemble(recipe(unknown), grid,
                helper.getLevel().registryAccess()), unknown), "Unknown output seasoning format must remain untouched");
        helper.assertTrue(!SeasonedFoodData.isSeasoned(SeasoningCrafting.assemble(recipe(new ItemStack(Items.STICK)),
                grid, helper.getLevel().registryAccess())), "Non-food output cannot inherit");

        boolean enabled = CraftingConfig.ENABLED.get();
        var exclusions = CraftingConfig.EXCLUDED_RECIPES.get();
        try {
            CraftingConfig.ENABLED.set(false);
            helper.assertTrue(!SeasonedFoodData.isSeasoned(SeasoningCrafting.assemble(recipe, grid,
                    helper.getLevel().registryAccess())), "Config can disable inheritance");
            CraftingConfig.ENABLED.set(true);
            CraftingConfig.EXCLUDED_RECIPES.set(List.of(ID.toString()));
            helper.assertTrue(!SeasonedFoodData.isSeasoned(SeasoningCrafting.assemble(recipe, grid,
                    helper.getLevel().registryAccess())), "Recipe exclusion must disable inheritance");
        } finally {
            CraftingConfig.ENABLED.set(enabled);
            CraftingConfig.EXCLUDED_RECIPES.set(exclusions);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void specialEffectsAndReusableIngredients(GameTestHelper helper) {
        var player = player(helper);
        var grid = player.inventoryMenu.getCraftSlots();
        var spice = spice("minecraft:apple", 0xCC3020, 1);
        var effects = spice.getList("effects", Tag.TAG_COMPOUND);
        for (var instance : List.of(new MobEffectInstance(MobEffects.HEAL, 1),
                new MobEffectInstance(MobEffects.NIGHT_VISION, -1))) {
            var tag = new CompoundTag();
            tag.put("effect", instance.save(new CompoundTag()));
            tag.putFloat("chance", 1);
            effects.add(tag);
        }
        grid.setItem(0, food(spice));
        grid.setItem(1, new ItemStack(Items.CARROT));
        var recipe = recipe(new ItemStack(Items.COOKIE, 8));
        var output = SeasoningCrafting.assemble(recipe, grid, helper.getLevel().registryAccess());
        var divided = SpiceEffects.read(SeasonedFoodData.seasonings(output).get(0).getList("effects", Tag.TAG_COMPOUND));
        helper.assertTrue(divided.size() == 2 && divided.stream().allMatch(entry -> entry.chance() == 0.125F),
                "Drop sub-tick effects and divide instant/infinite probability across portions");

        var reusable = new ShapelessRecipe(ID, "", CraftingBookCategory.MISC, new ItemStack(Items.BREAD), recipe.getIngredients()) {
            @Override public NonNullList<ItemStack> getRemainingItems(net.minecraft.world.inventory.CraftingContainer input) {
                var remaining = NonNullList.withSize(input.getContainerSize(), ItemStack.EMPTY);
                remaining.set(0, input.getItem(0).copyWithCount(1));
                return remaining;
            }
        };
        helper.assertTrue(!SeasonedFoodData.isSeasoned(SeasoningCrafting.assemble(reusable, grid,
                helper.getLevel().registryAccess())), "Reusable seasoned ingredient must not generate free seasoning");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void actualGridPreviewPickupAndShiftCrafting(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        var originals = new ArrayList<>(manager.getRecipes());
        var recipes = new ArrayList<>(originals);
        recipes.add(recipe(new ItemStack(Items.MUSHROOM_STEW)));
        try {
            manager.replaceRecipes(recipes);
            var player = player(helper);
            var small = player.inventoryMenu;
            player.containerMenu = small;
            small.getCraftSlots().setItem(0, food(spice("minecraft:apple", 0xCC3020, 100)));
            small.getCraftSlots().setItem(1, new ItemStack(Items.CARROT));
            helper.assertTrue(SeasonedFoodData.isSeasoned(small.getSlot(0).getItem()),
                    "Real 2x2 preview must inherit before output is taken");
            small.clicked(0, 0, ClickType.PICKUP, player);
            helper.assertTrue(SeasonedFoodData.isSeasoned(small.getCarried()) && small.getCraftSlots().isEmpty(),
                    "Normal crafting must return seasoned output and consume ingredients");
            small.setCarried(ItemStack.EMPTY);

            var large = new CraftingMenu(7, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(),
                    helper.absolutePos(new BlockPos(1, 1, 1))));
            player.containerMenu = large;
            var potatoes = food(spice("minecraft:apple", 0xCC3020, 100));
            potatoes.setCount(2);
            large.getSlot(1).set(potatoes);
            large.getSlot(2).set(new ItemStack(Items.CARROT, 2));
            helper.assertTrue(SeasonedFoodData.isSeasoned(large.getSlot(0).getItem()), "Real 3x3 preview must inherit");
            large.clicked(0, 0, ClickType.QUICK_MOVE, player);
            int count = 0;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                var stack = player.getInventory().getItem(slot);
                if (stack.is(Items.MUSHROOM_STEW) && SeasonedFoodData.isSeasoned(stack)) count += stack.getCount();
            }
            helper.assertTrue(count == 2 && large.getSlot(1).getItem().isEmpty() && large.getSlot(2).getItem().isEmpty(),
                    "Shift crafting must repeatedly produce seasoned meals without losing data or duplicating ingredients");
        } finally {
            manager.replaceRecipes(originals);
        }
        helper.succeed();
    }
}
