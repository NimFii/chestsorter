package qnimfi.cs;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import qnimfi.cs.item.ModItems;
import qnimfi.cs.network.LinkerSyncPayload;
import qnimfi.cs.network.NodeLinkData;

import java.util.*;


public class LinkerSyncHandler {

    private static final int SYNC_INTERVAL_TICKS = 1;

    private static final Set<UUID> ACTIVELY_SYNCED = new HashSet<>();
    private static int tickCounter = 0;

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {

            tickCounter++;
            if (tickCounter < SYNC_INTERVAL_TICKS) {
                return;
            }
            tickCounter = 0;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                handlePlayer(player);
            }
        });
    }

    private static void handlePlayer(ServerPlayer player) {
        boolean holdingLinker = player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.CHEST_LINKER);
        UUID uuid = player.getUUID();
        LinkerState state = LinkerState.get(player);

        if (!holdingLinker) {
            if (ACTIVELY_SYNCED.remove(uuid)) {
                ServerPlayNetworking.send(player, new LinkerSyncPayload(List.of(), Optional.empty()));
            }
            return;
        }

        ServerLevel world = player.level();
        List<NodeLinkData> ownedNodes = LogisticsManager.getOwnedNodeLinks(world, uuid);
        Optional<BlockPos> activeSender = Optional.ofNullable(state.getSelectedSender());

        ACTIVELY_SYNCED.add(uuid);
        ServerPlayNetworking.send(player, new LinkerSyncPayload(ownedNodes, activeSender));
    }
}