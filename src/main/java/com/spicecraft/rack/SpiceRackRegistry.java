package com.spicecraft.rack;

import com.spicecraft.machine.MachineRegistry;
import com.spicecraft.registry.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;

public final class SpiceRackRegistry {
    public static final RegistryObject<Block> BLOCK = MachineRegistry.BLOCKS.register("spice_rack",
            () -> new SpiceRackBlock(BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.WOOD).noOcclusion()));
    public static final RegistryObject<Item> ITEM = ModItems.ITEMS.register("spice_rack",
            () -> new BlockItem(BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<SpiceRackEntity>> ENTITY =
            MachineRegistry.ENTITIES.register("spice_rack", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder
                    .of(SpiceRackEntity::new, BLOCK.get()).build(null));
    public static void initialize() { }
    private SpiceRackRegistry() { }
}
