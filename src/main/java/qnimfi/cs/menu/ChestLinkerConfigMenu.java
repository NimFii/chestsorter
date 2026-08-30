package qnimfi.cs.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.NonNull;
import qnimfi.cs.FilterEntry;
import qnimfi.cs.LogisticsManager;
import qnimfi.cs.ReceiverConfig;

public class ChestLinkerConfigMenu extends AbstractContainerMenu {

    public static final int FILTER_SLOT_START = 0;

    private final Container filterContainer;
    private final ContainerData maxCounts;
    private final ContainerData filterItemIds;

    private final BlockPos receiverPos;
    private final int slotCount;
    private final ReceiverConfig receiverConfig; // Kept as field for quick-move logic

    /*
     * =========================================================
     * CLIENT CONSTRUCTOR
     * =========================================================
     */
    public ChestLinkerConfigMenu(int syncId, Inventory playerInv, ChestLinkerMenuData data) {
        super(ModMenuTypes.CHEST_LINKER_CONFIG, syncId);
        this.receiverPos = data.receiverPos();
        this.slotCount = data.slotCount();
        this.receiverConfig = null; // Client uses container sync data

        this.filterContainer = new SimpleContainer(slotCount);
        this.maxCounts = new SimpleContainerData(slotCount);
        this.filterItemIds = new ContainerData() {
            @Override public int get(int index) {
                ItemStack stack = filterContainer.getItem(index);
                return stack.isEmpty() ? 0 : BuiltInRegistries.ITEM.getId(stack.getItem()) + 1;
            }
            @Override public void set(int index, int value) {
                if (value <= 0) { filterContainer.setItem(index, ItemStack.EMPTY); return; }
                var item = BuiltInRegistries.ITEM.byId(value - 1);
                filterContainer.setItem(index, item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, 1));
            }
            @Override public int getCount() { return slotCount; }
        };

        addDataSlots(maxCounts);
        addDataSlots(filterItemIds);
        addFilterSlots();
        addPlayerInventory(playerInv);
    }

    /*
     * =========================================================
     * SERVER CONSTRUCTOR
     * =========================================================
     */
    public ChestLinkerConfigMenu(
            int syncId,
            Inventory playerInv,
            ServerLevel world,
            BlockPos receiverPos
    ) {
        super(ModMenuTypes.CHEST_LINKER_CONFIG, syncId);

        this.receiverPos = receiverPos;
        this.receiverConfig = LogisticsManager.getOrCreateReceiverConfig(world, receiverPos);
        this.slotCount = receiverConfig.getSlotCount();

        this.filterContainer = new FilterContainer(receiverConfig, () -> {});

        this.maxCounts = new ContainerData() {
            @Override public int get(int index) {
                return receiverConfig.getFilter(index).map(FilterEntry::maxCount).orElse(0);
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return slotCount; }
        };

        this.filterItemIds = new ContainerData() {
            @Override public int get(int index) {
                return receiverConfig.getFilter(index).map(f -> BuiltInRegistries.ITEM.getId(f.item()) + 1).orElse(0);
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return slotCount; }
        };

        addDataSlots(maxCounts);
        addDataSlots(filterItemIds);
        addFilterSlots();
        addPlayerInventory(playerInv);
    }

    public int getSlotCount() { return slotCount; }

    private void addFilterSlots() {
        int columns = ChestLinkerGuiLayout.FILTER_COLUMNS;
        for (int i = 0; i < slotCount; i++) {
            int row = i / columns;
            int col = i % columns;
            int slotsInRow = Math.min(columns, slotCount - row * columns);
            int rowOffset = (columns - slotsInRow) * 9;

            int x = 8 + rowOffset + col * 18;
            int y = ChestLinkerGuiLayout.FILTER_ROW_Y + row * 18;

            addSlot(new Slot(filterContainer, i, x, y));
        }
    }

    private void addPlayerInventory(Inventory playerInv) {
        int mainInvY = ChestLinkerGuiLayout.getMainInvY(slotCount);
        int hotbarY = ChestLinkerGuiLayout.getHotbarY(slotCount);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, mainInvY + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, hotbarY));
        }
    }

    public Item getFilterItem(int slot) {
        if (slot < 0 || slot >= slotCount) return Items.AIR;
        return filterContainer.getItem(slot).getItem();
    }

    public int getMaxCount(int slot) {
        return maxCounts.get(slot);
    }

    public BlockPos getReceiverPos() {
        return receiverPos;
    }

    @Override
    public void clicked(int slotId, int button, @NonNull ContainerInput clickType, @NonNull Player player) {
        // Block regular item pickup/placement into filter slots via cursor dragging
        if (slotId >= FILTER_SLOT_START && slotId < slotCount) {
            if (clickType == ContainerInput.QUICK_MOVE) {
                // Clear filter slot on shift-clicking the filter itself
                filterContainer.setItem(slotId, ItemStack.EMPTY);
                return;
            }
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return true;
    }

    @Override
    public @NonNull ItemStack quickMoveStack(@NonNull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();

        if (index < slotCount) {
            // Clicked inside filter slot -> clear it
            filterContainer.setItem(index, ItemStack.EMPTY);
        } else {
            // Clicked inside player inventory -> add to the first available filter slot
            if (receiverConfig != null && !receiverConfig.containsItem(stackInSlot.getItem())) {
                int availableSlot = receiverConfig.findFirstAvailableSlot();
                if (availableSlot != -1) {
                    receiverConfig.setFilterItem(availableSlot, stackInSlot.getItem());
                    filterContainer.setItem(availableSlot, new ItemStack(stackInSlot.getItem(), 1));
                    this.broadcastChanges();
                }
            }
        }

        return ItemStack.EMPTY;
    }

}