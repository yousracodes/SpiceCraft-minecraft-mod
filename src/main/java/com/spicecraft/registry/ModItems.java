package com.spicecraft.registry;

import com.spicecraft.SpiceCraft;
import com.spicecraft.item.BottleDefinition;
import com.spicecraft.item.SpiceBottleItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SpiceCraft.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SpiceCraft.MOD_ID);
    public static final BottleDefinition GLASS = new BottleDefinition(new ResourceLocation(SpiceCraft.MOD_ID, "glass"), 5, 0xB87935);
    public static final RegistryObject<SpiceBottleItem> SPICE_BOTTLE = ITEMS.register("spice_bottle",
            () -> new SpiceBottleItem(new Item.Properties(), GLASS));
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("spicecraft", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.spicecraft"))
            .icon(() -> SPICE_BOTTLE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(SPICE_BOTTLE.get());
                output.accept(com.spicecraft.machine.MachineRegistry.ITEM.get());
                output.accept(com.spicecraft.rack.SpiceRackRegistry.ITEM.get());
            }).build());

    private ModItems() {}
}
