package qnimfi.cs.menu;

public class ChestLinkerGuiLayout {
    public static final int GUI_WIDTH = 176;
    public static final int FILTER_COLUMNS = 9;
    public static final int SLOT_SIZE = 18;

    public static final int FILTER_ROW_Y = 20;

    public static int getFilterPanelHeight(int slotCount) {
        int rows = (int) Math.ceil((double) slotCount / FILTER_COLUMNS);
        return FILTER_ROW_Y + (rows * SLOT_SIZE) + 8; // 46px for 1 row
    }

    public static int getInventoryY(int slotCount) {
        return getFilterPanelHeight(slotCount) + 8; // 8px gap between panels
    }

    public static int getMainInvY(int slotCount) {
        return getInventoryY(slotCount) + 18;
    }

    public static int getHotbarY(int slotCount) {
        return getMainInvY(slotCount) + 58;
    }

    public static int getInventoryHeight() {
        return 101; // Exact height needed to fully enclose hotbar + padding
    }

    public static int getTotalGuiHeight(int slotCount) {
        return getInventoryY(slotCount) + getInventoryHeight();
    }
}