package com.spicecraft.bottle;

import com.spicecraft.machine.MachineRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;

public final class PlacedBottleRegistry {
    public static final RegistryObject<Block> BLOCK = MachineRegistry.BLOCKS.register("placed_spice_bottle",
            () -> new PlacedBottleBlock(BlockBehaviour.Properties.of().strength(0).dynamicShape().noOcclusion().noLootTable().sound(SoundType.GLASS)));
    public static final RegistryObject<BlockEntityType<PlacedBottleEntity>> ENTITY = MachineRegistry.ENTITIES.register("placed_spice_bottle",
            () -> BlockEntityType.Builder.of(PlacedBottleEntity::new, BLOCK.get()).build(null));
    public static void initialize() { }
    private PlacedBottleRegistry() {}
}
