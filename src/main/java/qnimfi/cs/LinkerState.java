package qnimfi.cs;

import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LinkerState {

    private static final LinkerMode[] MODES = LinkerMode.values();

    private LinkerMode mode = LinkerMode.CONNECT;
    private static final Map<UUID, LinkerState> STATES = new HashMap<>();
    private net.minecraft.core.BlockPos selectedSender;

    public LinkerMode getMode() {
        return mode;
    }

    public void setMode(LinkerMode mode) {
        this.mode = mode;
    }

    // direction: +1 scroll up, -1 scroll down
    public void cycleMode(int direction) {
        int size = MODES.length;
        int next = ((mode.ordinal() + direction) % size + size) % size;
        mode = MODES[next];
    }

    public static LinkerState get(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), uuid -> new LinkerState());
    }

    public net.minecraft.core.BlockPos getSelectedSender() {
        return selectedSender;
    }

    public void setSelectedSender(net.minecraft.core.BlockPos selectedSender) {
        this.selectedSender = selectedSender;
    }

    public void clear() {
        selectedSender = null;
    }

    public boolean hasSender() {
        return selectedSender != null;
    }
}