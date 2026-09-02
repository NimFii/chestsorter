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

    public record ReceiverPermissions(boolean gizmos, boolean filter, boolean settings, boolean authority, boolean connect) {
        public static final ReceiverPermissions DEFAULT_GUEST = new ReceiverPermissions(false, false, false, false, false);
        public static final ReceiverPermissions DEFAULT_OWNER = new ReceiverPermissions(true, true, true, true, true);

        public boolean permissions(AuthorityPermission permission) {
            return switch (permission) {
                case GIZMOS -> gizmos;
                case FILTER -> filter;
                case SETTINGS -> settings;
                case AUTHORITY -> authority;
                case CONNECT -> connect;
            };
        }

        public static final Codec<ReceiverPermissions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.fieldOf("gizmos").orElse(false).forGetter(ReceiverPermissions::gizmos),
                Codec.BOOL.fieldOf("filter").orElse(false).forGetter(ReceiverPermissions::filter),
                Codec.BOOL.fieldOf("settings").orElse(false).forGetter(ReceiverPermissions::settings),
                Codec.BOOL.fieldOf("authority").orElse(false).forGetter(ReceiverPermissions::authority),
                Codec.BOOL.fieldOf("connect").orElse(false).forGetter(ReceiverPermissions::connect)
        ).apply(instance, ReceiverPermissions::new));
    }

    private final Map<java.util.UUID, ReceiverPermissions> playerPermissions = new HashMap<>();

    public ReceiverPermissions getPermissions(java.util.UUID playerUuid) {
        return playerPermissions.getOrDefault(playerUuid, ReceiverPermissions.DEFAULT_GUEST);
    }

    public void setPermissions(java.util.UUID playerUuid, ReceiverPermissions permissions) {
        if (permissions == null) {
            playerPermissions.remove(playerUuid);
        } else {
            playerPermissions.put(playerUuid, permissions);
        }
    }

    public static final int INFINITY_MAX = -1;

    private final BlockPos position;
    private final Map<Integer, FilterEntry> filters = new HashMap<>();
    private final int slotCount;

    public ReceiverConfig(BlockPos position) {
        this(position, ChestSorterConfig.get().filterSlots);
    }

    public ReceiverConfig(BlockPos position, int slotCount) {
        this.position = position;
        this.slotCount = slotCount;
    }

    public BlockPos getPosition() { return position; }
    public int getSlotCount() { return slotCount; }

    public Optional<FilterEntry> getFilter(int slot) {
        return Optional.ofNullable(filters.get(slot));
    }

    public Item getItem(int slot) {
        FilterEntry entry = filters.get(slot);
        return entry != null ? entry.item() : Items.AIR;
    }

    public void setFilterItem(int slot, Item item) {
        if (slot < 0 || slot >= slotCount) return;

        if (item == Items.AIR) {
            filters.remove(slot);
            return;
        }

        for (Map.Entry<Integer, FilterEntry> entry : filters.entrySet()) {
            if (!entry.getKey().equals(slot) && entry.getValue().item() == item) {
                return;
            }
        }

        FilterEntry existing = filters.get(slot);
        int keepMax = existing != null ? existing.maxCount() : INFINITY_MAX;
        FilterType keepType = existing != null ? existing.type() : FilterType.ONLY;
        filters.put(slot, new FilterEntry(item, keepMax, keepType));
    }

    public void setFilterMax(int slot, int max) {
        FilterEntry existing = filters.get(slot);
        if (existing == null) return;

        // Wrap-around logic for infinity (-1) and scrolling bounds
        int newMax = max;
        if (max < -1) {
            newMax = INFINITY_MAX;
        }
        filters.put(slot, new FilterEntry(existing.item(), newMax, existing.type()));
    }

    public void cycleFilterType(int slot) {
        FilterEntry entry = filters.get(slot);
        if (entry != null) {
            FilterType nextType = switch (entry.type()) {
                case ONLY -> FilterType.EXCEPT;
                case EXCEPT -> FilterType.BURN;
                case BURN -> FilterType.ONLY;
            };

            int newMax = entry.type() == FilterType.BURN ? -1 : entry.maxCount();

            filters.put(slot, new FilterEntry(entry.item(), newMax, nextType));
        }
    }

    public boolean isBurnItem(Item item) {
        return filters.values().stream()
                .anyMatch(f -> f.item() == item && f.type() == FilterType.BURN);
    }

    public int findFirstAvailableSlot() {
        for (int i = 0; i < slotCount; i++) {
            if (!filters.containsKey(i)) return i;
        }
        return -1;
    }

    public boolean containsItem(Item item) {
        return filters.values().stream().anyMatch(f -> f.item() == item);
    }

    // ---- Serialization ----

    private record FilterSlotEntry(int slot, FilterEntry entry) {
        static final Codec<FilterSlotEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("slot").forGetter(FilterSlotEntry::slot),
                FilterEntry.CODEC.fieldOf("filter").forGetter(FilterSlotEntry::entry)
        ).apply(instance, FilterSlotEntry::new));
    }

    private record PlayerPermissionEntry(java.util.UUID uuid, ReceiverPermissions permissions) {
        static final Codec<PlayerPermissionEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                net.minecraft.core.UUIDUtil.CODEC.fieldOf("uuid").forGetter(PlayerPermissionEntry::uuid),
                ReceiverPermissions.CODEC.fieldOf("permissions").forGetter(PlayerPermissionEntry::permissions)
        ).apply(instance, PlayerPermissionEntry::new));
    }

    private record SerialForm(
            BlockPos position,
            int slotCount,
            List<FilterSlotEntry> filters,
            List<PlayerPermissionEntry> permissions
    ) {
        static final Codec<SerialForm> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("position").forGetter(SerialForm::position),
                Codec.INT.fieldOf("slot_count").forGetter(SerialForm::slotCount),
                FilterSlotEntry.CODEC.listOf().fieldOf("filters").forGetter(SerialForm::filters),
                PlayerPermissionEntry.CODEC.listOf().optionalFieldOf("permissions", List.of()).forGetter(SerialForm::permissions)
        ).apply(instance, SerialForm::new));
    }

    public static final Codec<ReceiverConfig> CODEC = SerialForm.CODEC.xmap(
            serial -> {
                ReceiverConfig config = new ReceiverConfig(serial.position(), serial.slotCount());
                for (FilterSlotEntry e : serial.filters()) {
                    config.filters.put(e.slot(), e.entry());
                }
                for (PlayerPermissionEntry p : serial.permissions()) {
                    config.playerPermissions.put(p.uuid(), p.permissions());
                }
                return config;
            },
            config -> new SerialForm(
                    config.position,
                    config.slotCount,
                    config.filters.entrySet().stream()
                            .map(en -> new FilterSlotEntry(en.getKey(), en.getValue()))
                            .toList(),
                    config.playerPermissions.entrySet().stream()
                            .map(en -> new PlayerPermissionEntry(en.getKey(), en.getValue()))
                            .toList()
            )
    );

    public Optional<FilterEntry> getFilterFor(Item item) {
        // 1. Direct match check (ONLY, EXCEPT, BURN)
        for (FilterEntry entry : filters.values()) {
            if (entry.item() == item) {
                return Optional.of(entry);
            }
        }

        // 2. If item is not explicitly listed, check if EXCEPT filters are present.
        // If there are EXCEPT filters, unlisted items are allowed.
        boolean hasExcept = filters.values().stream().anyMatch(f -> f.type() == FilterType.EXCEPT);
        if (hasExcept) {
            return Optional.of(new FilterEntry(item, INFINITY_MAX, FilterType.EXCEPT));
        }

        return Optional.empty();
    }
}