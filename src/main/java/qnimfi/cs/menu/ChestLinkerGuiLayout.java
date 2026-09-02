package qnimfi.cs.menu;

public class ChestLinkerGuiLayout {
    public static final int GUI_WIDTH = 176;
    public static final int FILTER_COLUMNS = 9;
    public static final int SLOT_SIZE = 18;
    public static final int FILTER_ROW_Y = 20;

    public static int getActiveSlotCount(int filterSlotCount, boolean hideInventory) {
        return hideInventory ? 0 : filterSlotCount;
    }

    public static int getFilterPanelHeight(int activeSlotCount) {
        if (activeSlotCount <= 0) return 44; // Compact height for header/buttons when inventory is hidden
        int rows = (int) Math.ceil((double) activeSlotCount / FILTER_COLUMNS);
        return FILTER_ROW_Y + (rows * SLOT_SIZE) + 8;
    }

    public static int getInventoryY(int activeSlotCount) {
        return getFilterPanelHeight(activeSlotCount) + 8;
    }

    public static int getMainInvY(int activeSlotCount) {
        return getInventoryY(activeSlotCount) + 18;
    }

    public static int getHotbarY(int activeSlotCount) {
        return getMainInvY(activeSlotCount) + 58;
    }

    public static int getInventoryHeight() {
        return 101;
    }

    public static int getTotalGuiHeight(int activeSlotCount, boolean hideInventory) {
        int filterHeight = getFilterPanelHeight(activeSlotCount);
        if (hideInventory) {
            return filterHeight + 12; // Only pad slightly for the bottom window border
        }
        return getInventoryY(activeSlotCount) + getInventoryHeight();
    }
}