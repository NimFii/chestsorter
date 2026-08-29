package qnimfi.cs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReceiverConfig {

    public static final int DEFAULT_MAX = 64;

    private final BlockPos position;
    private final Map<Integer, FilterEntry> filters = new HashMap<>();
    private int slotCount;

    public ReceiverConfig(BlockPos position) {
        this(position, ChestSorterConfig.get().filterSlots);
    }

    public ReceiverConfig(BlockPos position, int slotCount) {
        this.position = position;
        this.slotCount = slotCount;
    }

    public BlockPos getPosition() { return position; }
    public int getSlotCount() { return slotCount; }

    public void setSlotCount(int slotCount) {
        this.slotCount = Math.max(1, slotCount);
        filters.keySet().removeIf(slot -> slot >= this.slotCount);
    }

    public Optional<FilterEntry> getFilter(int slot) {
        return Optional.ofNullable(filters.get(slot));
    }

    public void setFilterItem(int slot, Item item) {
        if (slot < 0 || slot >= slotCount) return;

        if (item == Items.AIR) {
            filters.remove(slot);
            return;
        }

        int keepMax = filters.containsKey(slot) ? filters.get(slot).maxCount() : DEFAULT_MAX;
        filters.put(slot, new FilterEntry(item, keepMax));
    }

    public void setFilterMax(int slot, int max) {
        FilterEntry existing = filters.get(slot);
        if (existing == null) return;
        filters.put(slot, new FilterEntry(existing.item(), Math.max(0, max)));
    }

    public boolean acceptsItem(Item item) {
        return filters.values().stream().anyMatch(f -> f.item() == item);
    }

    // ---- Serialization ----

    private record FilterSlotEntry(int slot, FilterEntry entry) {
        static final Codec<FilterSlotEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("slot").forGetter(FilterSlotEntry::slot),
                FilterEntry.CODEC.fieldOf("filter").forGetter(FilterSlotEntry::entry)
        ).apply(instance, FilterSlotEntry::new));
    }

    private record SerialForm(BlockPos position, int slotCount, List<FilterSlotEntry> filters) {
        static final Codec<SerialForm> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("position").forGetter(SerialForm::position),
                Codec.INT.fieldOf("slot_count").forGetter(SerialForm::slotCount),
                FilterSlotEntry.CODEC.listOf().fieldOf("filters").forGetter(SerialForm::filters)
        ).apply(instance, SerialForm::new));
    }

    public static final Codec<ReceiverConfig> CODEC = SerialForm.CODEC.xmap(
            serial -> {
                ReceiverConfig config = new ReceiverConfig(serial.position(), serial.slotCount());
                for (FilterSlotEntry e : serial.filters()) {
                    config.filters.put(e.slot(), e.entry());
                }
                return config;
            },
            config -> new SerialForm(
                    config.position,
                    config.slotCount,
                    config.filters.entrySet().stream()
                            .map(en -> new FilterSlotEntry(en.getKey(), en.getValue()))
                            .toList()
            )
    );

    public Optional<FilterEntry> getFilterFor(Item item) {
        return filters.values().stream().filter(f -> f.item() == item).findFirst();
    }
}