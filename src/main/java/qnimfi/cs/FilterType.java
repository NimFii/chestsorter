package qnimfi.cs;

import net.minecraft.world.item.Item;

public enum FilterType {
    ONLY(0xFF5555FF) {
        @Override
        public FilterType next() { return EXCEPT; }
        @Override
        public boolean allows(Item filterItem, Item targetItem) { return filterItem == targetItem; }
    },
    EXCEPT(0xFF55FF55) {
        @Override
        public FilterType next() { return BURN; }
        @Override
        public boolean allows(Item filterItem, Item targetItem) { return filterItem != targetItem; }
    },
    BURN(0xFFFF5555) {
        @Override
        public FilterType next() { return ONLY; }
        @Override
        public boolean allows(Item filterItem, Item targetItem) { return false; }
    };

    private final int colorArgb;

    FilterType(int colorArgb) {
        this.colorArgb = colorArgb;
    }

    public int getColorArgb() { return colorArgb; }
    public abstract FilterType next();
    public abstract boolean allows(net.minecraft.world.item.Item filterItem, net.minecraft.world.item.Item targetItem);
}