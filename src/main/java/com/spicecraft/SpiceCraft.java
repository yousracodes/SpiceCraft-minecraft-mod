package com.spicecraft;

import com.mojang.logging.LogUtils;
import com.spicecraft.registry.ModItems;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/** Common entry point, loaded on both clients and dedicated servers. */
@Mod(SpiceCraft.MOD_ID)
public final class SpiceCraft {
    public static final String MOD_ID = "spicecraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SpiceCraft() {
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,
                com.spicecraft.crafting.CraftingConfig.SPEC, "spicecraft-crafting-server.toml");
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                com.spicecraft.color.ColorConfig.SPEC);
        com.spicecraft.network.ColorNetwork.register();
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        com.spicecraft.bottle.PlacedBottleRegistry.initialize();
        com.spicecraft.rack.SpiceRackRegistry.initialize();
        com.spicecraft.machine.MachineRegistry.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        LOGGER.info("SpiceCraft initialized");
    }
}
