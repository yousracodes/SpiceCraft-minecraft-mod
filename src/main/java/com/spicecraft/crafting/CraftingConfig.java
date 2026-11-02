package com.spicecraft.crafting;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

public final class CraftingConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_RECIPES;
    static {
        var builder = new ForgeConfigSpec.Builder();
        ENABLED = builder.comment("Carry seasonings from consumed ingredients into edible crafting-grid results.")
                .define("inheritSeasoning", true);
        EXCLUDED_RECIPES = builder.comment("Exact recipe IDs excluded from inheritance, e.g. othermod:salad.")
                .defineListAllowEmpty("excludedRecipes", List.of(),
                        value -> value instanceof String id && ResourceLocation.tryParse(id) != null);
        SPEC = builder.build();
    }
    private CraftingConfig() {}
}
