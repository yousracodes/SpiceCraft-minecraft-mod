package com.spicecraft.machine;

import com.spicecraft.item.SpiceBottleData;
import com.spicecraft.item.SpiceBottleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
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
    private ItemStack colorSource = ItemStack.EMPTY;
    private int sampledColor = -1;
    private ItemStack lastDisplayedBottle = ItemStack.EMPTY;
    private ItemStack lastDisplayedFood = ItemStack.EMPTY;

    public ItemStack displayFood() {
        return isFood(items.get(0)) ? items.get(0).copyWithCount(1) : ItemStack.EMPTY;
    }

    public ItemStack displayBottle() {
        ItemStack bottle = items.get(2).isEmpty() ? items.get(1) : items.get(2);
        return bottle.getItem() instanceof SpiceBottleItem ? bottle.copyWithCount(1) : ItemStack.EMPTY;
    }

    @Override public void setChanged() {
        super.setChanged();
        if (level == null || level.isClientSide) return;
        ItemStack displayed = displayBottle();
        ItemStack food = displayFood();
        // Processing marks the inventory dirty every tick; only send when a displayed item changes.
        if (!ItemStack.matches(lastDisplayedBottle, displayed) || !ItemStack.matches(lastDisplayedFood, food)) {
            lastDisplayedBottle = displayed;
            lastDisplayedFood = food;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    public boolean acceptColor(ItemStack source, int color) {
        if (color < 0 || color > 0xFFFFFF || !isFood(items.get(0))
                || !ItemStack.isSameItemSameTags(items.get(0), source)) return false;
        // First valid report owns this input's appearance; later viewers cannot recolor it.
        if (sampledColor >= 0 && ItemStack.isSameItemSameTags(colorSource, source)) return false;
        colorSource = source.copyWithCount(1);
        sampledColor = color;
        setChanged();
        return true;
    }
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
        if (level.isClientSide) return;
        machine.collectFood(level, pos);
        if (!isFood(machine.items.get(0)) || !isEmptyBottle(machine.items.get(1))) {
            if (machine.progress != 0) { machine.progress = 0; machine.setChanged(); }
            return;
        }
        if (!machine.items.get(2).isEmpty()) return;
        if (++machine.progress >= PROCESS_TICKS) {
            ItemStack result = machine.items.get(1).copyWithCount(1);
            SpiceBottleItem bottle = (SpiceBottleItem) result.getItem();
            int color = machine.sampledColor >= 0 && ItemStack.isSameItemSameTags(machine.colorSource, machine.items.get(0))
                    ? machine.sampledColor : com.spicecraft.color.ColorConfig.FALLBACK.get();
            SpiceBottleData.fillFromFood(result, machine.items.get(0), bottle.definition().capacity(), color, null);
            machine.items.get(0).shrink(1);
            machine.items.get(1).shrink(1);
            machine.items.set(2, result);
            machine.progress = 0;
        }
        machine.setChanged();
    }
    private void collectFood(Level level, BlockPos pos) {
        ItemStack input = items.get(0);
        int limit = input.isEmpty() ? getMaxStackSize() : Math.min(getMaxStackSize(), input.getMaxStackSize());
        if (!input.isEmpty() && (!isFood(input) || input.getCount() >= limit)) return;
        // The block has a full-height collision box, so dropped items rest at y + 1.
        AABB intake = new AABB(pos.getX(), pos.getY() + 1, pos.getZ(),
                pos.getX() + 1, pos.getY() + 1.25, pos.getZ() + 1);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, intake, candidate -> candidate.isAlive())) {
            ItemStack dropped = entity.getItem().copy();
            if (!isFood(dropped) || (!input.isEmpty() && !ItemStack.isSameItemSameTags(input, dropped))) continue;
            int capacity = Math.min(getMaxStackSize(), dropped.getMaxStackSize()) - input.getCount();
            int count = Math.min(capacity, dropped.getCount());
            if (count <= 0) break;
            if (input.isEmpty()) {
                setItem(0, dropped.copyWithCount(count));
                input = items.get(0);
            } else {
                input.grow(count);
                setChanged();
            }
            dropped.shrink(count);
            if (dropped.isEmpty()) entity.discard();
            else entity.setItem(dropped);
            if (input.getCount() >= Math.min(getMaxStackSize(), input.getMaxStackSize())) break;
        }
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("Progress", progress);
        if (sampledColor >= 0) {
            tag.put("ColorSource", colorSource.save(new CompoundTag()));
            tag.putInt("SampledColor", sampledColor);
        }
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
        progress = Math.max(0, Math.min(PROCESS_TICKS - 1, tag.getInt("Progress")));
        colorSource = ItemStack.of(tag.getCompound("ColorSource"));
        sampledColor = tag.contains("SampledColor") ? tag.getInt("SampledColor") : -1;
        if (sampledColor < 0 || sampledColor > 0xFFFFFF) sampledColor = -1;
        lastDisplayedBottle = displayBottle();
        lastDisplayedFood = displayFood();
    }
    @Override public Component getDisplayName() { return Component.translatable("block.spicecraft.spice_maker"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new SpiceMakerMenu(id, inventory, this, data, worldPosition); }
    @Override public int getContainerSize() { return 3; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(items, slot, count);
        if (slot == 0 && items.get(0).isEmpty()) { sampledColor = -1; colorSource = ItemStack.EMPTY; }
        if (!result.isEmpty()) { if (slot < 2) progress = 0; setChanged(); }
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (slot < 2) progress = 0;
        if (slot == 0) { sampledColor = -1; colorSource = ItemStack.EMPTY; }
        ItemStack result = ContainerHelper.takeItem(items, slot);
        if (!result.isEmpty()) setChanged();
        return result;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot == 0 && !ItemStack.isSameItemSameTags(colorSource, stack)) { sampledColor = -1; colorSource = ItemStack.EMPTY; }
        if (slot < 2 && !ItemStack.isSameItemSameTags(items.get(slot), stack)) progress = 0;
        items.set(slot, stack);
        setChanged();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 ? isFood(stack) : slot == 1 && isEmptyBottle(stack); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); progress = 0; sampledColor = -1; colorSource = ItemStack.EMPTY; setChanged(); }
    public int progress() { return progress; }
}
