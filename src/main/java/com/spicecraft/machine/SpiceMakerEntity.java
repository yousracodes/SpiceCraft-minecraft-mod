package com.spicecraft.machine;

import com.spicecraft.item.SpiceBottleData;
import com.spicecraft.item.SpiceBottleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SpiceMakerEntity extends BlockEntity implements Container, MenuProvider {
    public static final int PROCESS_TICKS = 200;
    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int progress;
    final ContainerData data = new ContainerData() {
        public int get(int index) { return progress; }
        public void set(int index, int value) { progress = value; }
        public int getCount() { return 1; }
    };

    public SpiceMakerEntity(BlockPos pos, BlockState state) { super(MachineRegistry.ENTITY.get(), pos, state); }
    public static boolean isFood(ItemStack stack) { return !stack.isEmpty() && stack.getFoodProperties(null) != null; }
    public static boolean isEmptyBottle(ItemStack stack) {
        return stack.getItem() instanceof SpiceBottleItem bottle && SpiceBottleData.read(stack, bottle.definition()).servings() == 0;
    }
    public static void tick(Level level, BlockPos pos, BlockState state, SpiceMakerEntity machine) {
        if (!isFood(machine.items.get(0)) || !isEmptyBottle(machine.items.get(1))) {
            if (machine.progress != 0) { machine.progress = 0; machine.setChanged(); }
            return;
        }
        if (!machine.items.get(2).isEmpty()) return;
        if (++machine.progress >= PROCESS_TICKS) {
            ItemStack result = machine.items.get(1).copyWithCount(1);
            SpiceBottleItem bottle = (SpiceBottleItem) result.getItem();
            SpiceBottleData.fillFromFood(result, machine.items.get(0), bottle.definition().capacity(), bottle.definition().defaultPowderColor(), null);
            machine.items.get(0).shrink(1);
            machine.items.get(1).shrink(1);
            machine.items.set(2, result);
            machine.progress = 0;
        }
        machine.setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("Progress", progress);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, items);
        progress = Math.max(0, Math.min(PROCESS_TICKS - 1, tag.getInt("Progress")));
    }
    @Override public Component getDisplayName() { return Component.translatable("block.spicecraft.spice_maker"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new SpiceMakerMenu(id, inventory, this, data, worldPosition); }
    @Override public int getContainerSize() { return 3; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(items, slot, count);
        if (!result.isEmpty()) { if (slot < 2) progress = 0; setChanged(); }
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { if (slot < 2) progress = 0; return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < 2 && !ItemStack.isSameItemSameTags(items.get(slot), stack)) progress = 0;
        items.set(slot, stack);
        setChanged();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 ? isFood(stack) : slot == 1 && isEmptyBottle(stack); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); progress = 0; setChanged(); }
    public int progress() { return progress; }
}
