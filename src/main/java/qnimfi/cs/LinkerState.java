package qnimfi.cs;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.lang.foreign.Linker;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LinkerState {

    private static final Map<UUID, LinkerState> STATES = new HashMap<>();
    private BlockPos selectedSender;

    public static LinkerState get(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), _ -> new LinkerState());
    }

    private int ticksInConnectionMode = 0;

    public BlockPos getSelectedSender() { return selectedSender; }

    public void setSelectedSender(BlockPos pos) {
        this.selectedSender = pos;
        this.ticksInConnectionMode = 0; // Reset timer on start
    }

    public int getTicksInConnectionMode() { return ticksInConnectionMode; }
    public void incrementTicks() { this.ticksInConnectionMode++; }

    public boolean hasSender() {
        return selectedSender != null;
    }

    public static void clear(ServerPlayer player) {
        STATES.remove(player.getUUID());
    }



    // direction: +1 scroll up, -1 scroll down
    //public void cycleMode(int direction) {
    //    int size = MODES.length;
    //    int next = ((mode.ordinal() + direction) % size + size) % size;
    //    mode = MODES[next];
    //}
}