package qnimfi.cs;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DirtyChestTracker {

    private static final Map<ResourceKey<Level>, Set<BlockPos>> DIRTY = new HashMap<>();

    public static void markDirty(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        DIRTY.computeIfAbsent(level.dimension(), _ -> Collections.synchronizedSet(new HashSet<>()))
                .add(pos.immutable());
    }

    public static Set<BlockPos> drain(Level level) {
        Set<BlockPos> set = DIRTY.get(level.dimension());
        if (set == null || set.isEmpty()) return Set.of();

        synchronized (set) {
            Set<BlockPos> copy = new HashSet<>(set);
            set.clear();
            return copy;
        }
    }
}