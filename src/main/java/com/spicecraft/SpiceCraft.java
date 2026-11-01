package com.spicecraft;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** Common entry point, loaded on both clients and dedicated servers. */
@Mod(SpiceCraft.MOD_ID)
public final class SpiceCraft {
    public static final String MOD_ID = "spicecraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SpiceCraft() {
        LOGGER.info("SpiceCraft initialized");
    }
}
