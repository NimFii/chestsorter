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

    /*
     * =========================================================
     * CLIENT CONSTRUCTOR
     * =========================================================
     */
    public ChestLinkerConfigMenu(int syncId, Inventory playerInv, ChestLinkerMenuData data) {
        super(ModMenuTypes.CHEST_LINKER_CONFIG, syncId);
        this.receiverPos = data.receiverPos();
        this.slotCount = data.slotCount();

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
                filterContainer.setItem(index, (item == null || item == Items.AIR) ? ItemStack.EMPTY : new ItemStack(item, 1));
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

        ReceiverConfig config =
                LogisticsManager.getOrCreateReceiverConfig(
                        world,
                        receiverPos
                );

        this.filterContainer =
                new FilterContainer(
                        config,
                        () -> {}
                );
        this.slotCount = config.getSlotCount();
        /*
         * Max count synchronization.
         */
        this.maxCounts =
                new ContainerData() {

                    @Override
                    public int get(int index) {

                        return config
                                .getFilter(index)
                                .map(FilterEntry::maxCount)
                                .orElse(0);
                    }

                    @Override
                    public void set(
                            int index,
                            int value
                    ) {
                        // Client -> server is handled
                        // through AdjustFilterMaxPayload.
                    }

                    @Override
                    public int getCount() { return slotCount; }
                };

        /*
         * Filter item synchronization.
         */
        this.filterItemIds =
                new ContainerData() {

                    @Override
                    public int get(int index) {

                        return config
                                .getFilter(index)
                                .map(FilterEntry::item)
                                .map(item ->
                                        BuiltInRegistries.ITEM.getId(item) + 1
                                )
                                .orElse(0);
                    }

                    @Override
                    public void set(
                            int index,
                            int value
                    ) {
                        // Server doesn't receive these values.
                    }

                    @Override
                    public int getCount() {
                        return slotCount;
                    }
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
        if (slot < 0 || slot >= slotCount) {
            return Items.AIR;
        }
        return filterContainer.getItem(slot).getItem();
    }

    public int getMaxCount(int slot) {
        return maxCounts.get(slot);
    }


    public BlockPos getReceiverPos() {
        return receiverPos;
    }


    /*
     * Prevent vanilla from ever treating the filter slots
     * like real inventory slots.
     *
     * Our screen handles those clicks separately.
     */
    @Override
    public void clicked(int slotId, int button, @NonNull ContainerInput clickType, @NonNull Player player) {

        if (slotId >= FILTER_SLOT_START && slotId < slotCount) {
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }


    @Override
    public boolean stillValid(@NonNull Player player) {
        return true;
    }


    @Override
    public @NonNull ItemStack quickMoveStack(
            @NonNull Player player,
            int index
    ) {
        return ItemStack.EMPTY;
    }
}