package com.spicecraft.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SpiceMakerMenu extends AbstractContainerMenu {
    private final Container machine;
    private final ContainerData data;
    private final BlockPos position;
    public SpiceMakerMenu(int id, Inventory inventory, Container machine, ContainerData data, BlockPos pos) {
        super(MachineRegistry.MENU.get(), id);
        checkContainerSize(machine, 3);
        checkContainerDataCount(data, 1);
        this.machine = machine;
        this.data = data;
        this.position = pos.immutable();
        addSlot(new Slot(machine, 0, 44, 24) { @Override public boolean mayPlace(ItemStack stack) { return SpiceMakerEntity.isFood(stack); } });
        addSlot(new Slot(machine, 1, 44, 57) { @Override public boolean mayPlace(ItemStack stack) { return SpiceMakerEntity.isEmptyBottle(stack); } });
        addSlot(new Slot(machine, 2, 116, 40) { @Override public boolean mayPlace(ItemStack stack) { return false; } });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 98 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 156));
        addDataSlots(data);
    }
    public int progress() { return data.get(0); }
    public BlockPos position() { return position; }
    public void acceptColor(ItemStack source, int color) {
        if (machine instanceof SpiceMakerEntity entity) entity.acceptColor(source, color);
    }
    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 3) {
            if (!moveItemStackTo(stack, 3, 39, true)) return ItemStack.EMPTY;
        } else if (SpiceMakerEntity.isEmptyBottle(stack)) {
            if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else if (SpiceMakerEntity.isFood(stack)) {
            if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else if (index < 30) {
            if (!moveItemStackTo(stack, 30, 39, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 3, 30, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        if (original.getCount() == stack.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
