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
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        com.spicecraft.machine.MachineRegistry.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        LOGGER.info("SpiceCraft initialized");
    }
}
