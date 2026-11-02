package com.spicecraft.machine;

import com.spicecraft.SpiceCraft;
import com.spicecraft.registry.ModItems;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MachineRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SpiceCraft.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SpiceCraft.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, SpiceCraft.MOD_ID);
    public static final RegistryObject<Block> BLOCK = BLOCKS.register("spice_maker", () -> new SpiceMakerBlock(
            BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.STONE).noOcclusion()));
    public static final RegistryObject<Item> ITEM = ModItems.ITEMS.register("spice_maker", () -> new BlockItem(BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<SpiceMakerEntity>> ENTITY = ENTITIES.register("spice_maker",
            () -> BlockEntityType.Builder.of(SpiceMakerEntity::new, BLOCK.get()).build(null));
    public static final RegistryObject<MenuType<SpiceMakerMenu>> MENU = MENUS.register("spice_maker",
            () -> IForgeMenuType.create((id, inventory, buffer) -> new SpiceMakerMenu(id, inventory,
                    new net.minecraft.world.SimpleContainer(3), new net.minecraft.world.inventory.SimpleContainerData(1), buffer.readBlockPos())));
    public static void register(IEventBus bus) { BLOCKS.register(bus); ENTITIES.register(bus); MENUS.register(bus); }
    private MachineRegistry() {}
}
