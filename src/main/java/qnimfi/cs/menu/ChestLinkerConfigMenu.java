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
import qnimfi.cs.FilterType;
import qnimfi.cs.LogisticsManager;
import qnimfi.cs.ReceiverConfig;

public class ChestLinkerConfigMenu extends AbstractContainerMenu {

    public static final int FILTER_SLOT_START = 0;

    private final Container filterContainer;
    private final ContainerData maxCounts;
    private final ContainerData filterItemIds;
    private final ContainerData filterTypes;

    private final ContainerData viewStateData;
    private final BlockPos receiverPos;
    private final int slotCount;
    private final ReceiverConfig receiverConfig;

    // client/packet constructor
    public ChestLinkerConfigMenu(int syncId, Inventory playerInv, ChestLinkerMenuData data) {
        super(ModMenuTypes.CHEST_LINKER_CONFIG, syncId);

        this.receiverPos = data.receiverPos();
        this.slotCount = data.slotCount();
        this.receiverConfig = null;

        this.filterContainer = new SimpleContainer(slotCount);
        this.maxCounts = new SimpleContainerData(slotCount);
        this.filterTypes = new SimpleContainerData(slotCount);

        this.viewStateData = new SimpleContainerData(1);

        this.filterItemIds = new ContainerData() {
            @Override public int get(int index) {
                ItemStack stack = filterContainer.getItem(index);
                return stack.isEmpty() ? 0 : BuiltInRegistries.ITEM.getId(stack.getItem()) + 1;
            }

            @Override public void set(int index, int value) {
                if (value <= 0) {
                    filterContainer.setItem(index, ItemStack.EMPTY);
                    return;
                }

                var item = BuiltInRegistries.ITEM.byId(value - 1);
                filterContainer.setItem(index, item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, 1));
            }

            @Override public int getCount() {
                return slotCount;
            }
        };

        addDataSlots(maxCounts);
        addDataSlots(filterItemIds);
        addDataSlots(filterTypes);
        addDataSlots(viewStateData);
        addFilterSlots();
        addPlayerInventory(playerInv);
    }

    // server constructor
    public ChestLinkerConfigMenu(int syncId, Inventory playerInv, ServerLevel world, BlockPos receiverPos) {
        super(ModMenuTypes.CHEST_LINKER_CONFIG, syncId);

        this.receiverPos = receiverPos;
        this.receiverConfig = LogisticsManager.getOrCreateReceiverConfig(world, receiverPos);
        this.slotCount = receiverConfig.getSlotCount();

        this.filterContainer = new FilterContainer(receiverConfig, this::broadcastChanges);

        this.maxCounts = new ContainerData() {
            @Override public int get(int index) {
                return receiverConfig.getFilter(index).map(FilterEntry::maxCount).orElse(0);
            }

            @Override public void set(int index, int value) {}

            @Override public int getCount() {
                return slotCount;
            }
        };

        this.filterItemIds = new ContainerData() {
            @Override public int get(int index) {
                return receiverConfig.getFilter(index)
                        .map(f -> BuiltInRegistries.ITEM.getId(f.item()) + 1)
                        .orElse(0);
            }

            @Override public void set(int index, int value) {}

            @Override public int getCount() {
                return slotCount;
            }
        };

        this.filterTypes = new ContainerData() {
            @Override public int get(int index) {
                return receiverConfig.getFilter(index)
                        .map(f -> f.type().ordinal())
                        .orElse(0);
            }

            @Override public void set(int index, int value) {}

            @Override public int getCount() {
                return slotCount;
            }
        };

        this.viewStateData = new ContainerData() {
            private int viewing = 0;
            @Override public int get(int index) { return viewing; }
            @Override public void set(int index, int value) { viewing = value; }
            @Override public int getCount() { return 1; }
        };

        addDataSlots(maxCounts);
        addDataSlots(filterItemIds);
        addDataSlots(filterTypes);
        addDataSlots(viewStateData);
        addFilterSlots();
        addPlayerInventory(playerInv);
    }

    public int getSlotCount() {
        return slotCount;
    }

    public void updateSlotPositions(boolean hideEverything) {
        // Save state to the container data slot so it synchronizes to the server
        viewStateData.set(0, hideEverything ? 1 : 0);

        int activeCount = ChestLinkerGuiLayout.getActiveSlotCount(slotCount, hideEverything);
        int columns = ChestLinkerGuiLayout.FILTER_COLUMNS;

        // Hide or position filter slots
        for (int i = 0; i < slotCount; i++) {
            int targetX;
            int targetY;

            if (hideEverything) {
                targetX = -9999;
                targetY = -9999;
            } else {
                int row = i / columns;
                int col = i % columns;
                int slotsInRow = Math.min(columns, slotCount - row * columns);
                int rowOffset = (columns - slotsInRow) * 9;

                targetX = 8 + rowOffset + col * 18;
                targetY = ChestLinkerGuiLayout.FILTER_ROW_Y + row * 18;
            }

            replaceSlot(i, targetX, targetY);
        }

        // Hide or position player inventory slots
        int slotIdx = slotCount;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int targetX = hideEverything ? -9999 : (8 + col * 18);
                int targetY = hideEverything ? -9999 : (ChestLinkerGuiLayout.getMainInvY(activeCount) + row * 18);
                replaceSlot(slotIdx++, targetX, targetY);
            }
        }

        for (int col = 0; col < 9; col++) {
            int targetX = hideEverything ? -9999 : (8 + col * 18);
            int targetY = hideEverything ? -9999 : ChestLinkerGuiLayout.getHotbarY(activeCount);
            replaceSlot(slotIdx++, targetX, targetY);
        }
    }

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
        int activeCount = ChestLinkerGuiLayout.getActiveSlotCount(slotCount, false);
        int mainInvY = ChestLinkerGuiLayout.getMainInvY(activeCount);
        int hotbarY = ChestLinkerGuiLayout.getHotbarY(activeCount);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(
                        playerInv,
                        col + row * 9 + 9,
                        8 + col * 18,
                        mainInvY + row * 18
                ));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(
                    playerInv,
                    col,
                    8 + col * 18,
                    hotbarY
            ));
        }
    }

    public Item getFilterItem(int slot) {
        if (slot < 0 || slot >= slotCount) return Items.AIR;
        return filterContainer.getItem(slot).getItem();
    }

    public int getMaxCount(int slot) {
        return maxCounts.get(slot);
    }

    public FilterType getFilterType(int slot) {
        if (slot < 0 || slot >= slotCount) return FilterType.ONLY;

        int ordinal = filterTypes.get(slot);
        FilterType[] types = FilterType.values();

        if (ordinal >= 0 && ordinal < types.length) {
            return types[ordinal];
        }

        return FilterType.ONLY;
    }

    public BlockPos getReceiverPos() {
        return receiverPos;
    }

    @Override
    public void clicked(int slotId, int button, @NonNull ContainerInput clickType, @NonNull Player player) {
        // Prevent interaction if slots are hidden out of bounds
        if (slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            if (slot.x == -9999 || slot.y == -9999) {
                return;
            }
        }

        if (slotId >= FILTER_SLOT_START && slotId < slotCount) {
            if (clickType == ContainerInput.QUICK_MOVE) {
                filterContainer.setItem(slotId, ItemStack.EMPTY);
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
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(index);

        // Prevent shift-clicking items from hidden out-of-bound slots
        if (slot.x == -9999 || slot.y == -9999) {
            return ItemStack.EMPTY;
        }

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();

        if (index < slotCount) {
            filterContainer.setItem(index, ItemStack.EMPTY);
        } else if (receiverConfig != null && !receiverConfig.containsItem(stackInSlot.getItem())) {
            int availableSlot = receiverConfig.findFirstAvailableSlot();

            if (availableSlot != -1) {
                receiverConfig.setFilterItem(availableSlot, stackInSlot.getItem());
                filterContainer.setItem(availableSlot, new ItemStack(stackInSlot.getItem(), 1));
                broadcastChanges();
            }
        }

        return ItemStack.EMPTY;
    }
    private void replaceSlot(int index, int newX, int newY) {
        Slot oldSlot = slots.get(index);

        if (oldSlot.x == newX && oldSlot.y == newY) return;

        Slot newSlot = new Slot(
                oldSlot.container,
                oldSlot.getContainerSlot(),
                newX,
                newY
        );

        newSlot.index = oldSlot.index;
        slots.set(index, newSlot);
    }

}