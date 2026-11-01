package com.spicecraft.bottle;

import com.spicecraft.item.SpiceBottleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PlacedBottleEntity extends BlockEntity {
    public static final int SLOTS = 4;
    private final NonNullList<ItemStack> bottles = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    public PlacedBottleEntity(BlockPos pos, BlockState state) { super(PlacedBottleRegistry.ENTITY.get(), pos, state); }
    public ItemStack bottle(int slot) { return bottles.get(slot).copy(); }
    public ItemStack bottle() { for (var bottle : bottles) if (!bottle.isEmpty()) return bottle.copy(); return ItemStack.EMPTY; }
    public boolean isEmpty() { return bottles.stream().allMatch(ItemStack::isEmpty); }
    public boolean insert(int slot, ItemStack stack) {
        if (!bottles.get(slot).isEmpty() || !(stack.getItem() instanceof SpiceBottleItem)) return false;
        bottles.set(slot, stack.copyWithCount(1)); sync(); return true;
    }
    public void setBottle(ItemStack stack) { bottles.set(0, ItemStack.EMPTY); insert(0, stack); }
    public ItemStack takeBottle(int slot) { var result = bottles.get(slot); bottles.set(slot, ItemStack.EMPTY); sync(); return result; }
    public ItemStack takeBottle() { for (int i = 0; i < SLOTS; i++) if (!bottles.get(i).isEmpty()) return takeBottle(i); return ItemStack.EMPTY; }
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        net.minecraft.world.ContainerHelper.saveAllItems(tag, bottles);
        tag.putInt("LayoutVersion", 2);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        bottles.clear();
        if (tag.contains("Items")) net.minecraft.world.ContainerHelper.loadAllItems(tag, bottles);
        else if (tag.contains("Bottle")) bottles.set(0, ItemStack.of(tag.getCompound("Bottle")));
        for (int i = 0; i < SLOTS; i++) {
            var saved = bottles.get(i);
            bottles.set(i, saved.getItem() instanceof SpiceBottleItem ? saved.copyWithCount(1) : ItemStack.EMPTY);
        }
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
