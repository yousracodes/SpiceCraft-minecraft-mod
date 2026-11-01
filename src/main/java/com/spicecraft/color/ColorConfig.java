package com.spicecraft.color;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ColorConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue FALLBACK;
    static {
        var builder = new ForgeConfigSpec.Builder();
        FALLBACK = builder.comment("Fallback powder RGB color (decimal, 0 through 16777215) when no client texture sample is available.")
                .defineInRange("fallbackPowderColor", 0xB87935, 0, 0xFFFFFF);
        SPEC = builder.build();
    }
    private ColorConfig() {}
}
