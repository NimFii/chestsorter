package qnimfi.cs.menu;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;
import qnimfi.cs.ReceiverConfig;

public class FilterContainer implements Container {

    private final ReceiverConfig config;
    private final Runnable onChange;

    public FilterContainer(ReceiverConfig config, Runnable onChange) {
        this.config = config;
        this.onChange = onChange;
    }

    @Override
    public int getContainerSize() {
        return config.getSlotCount();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!getItem(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public @NonNull ItemStack getItem(int slot) {
        return config.getFilter(slot)
                .map(f -> new ItemStack(f.item(), 1))
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public @NonNull ItemStack removeItem(int slot, int amount) {
        ItemStack existing = getItem(slot);
        config.setFilterItem(slot, Items.AIR);
        onChange.run();
        return existing;
    }

    @Override
    public @NonNull ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, 1);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        config.setFilterItem(slot, stack.isEmpty() ? Items.AIR : stack.getItem());
        onChange.run();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
        onChange.run();
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < getContainerSize(); i++) {
            removeItem(i, 1);
        }
    }
}