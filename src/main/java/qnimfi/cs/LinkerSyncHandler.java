package qnimfi.cs;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import qnimfi.cs.network.LinkerSyncPayload;
import qnimfi.cs.network.NodeLinkData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class LinkerSyncHandler {

    private static final int SYNC_INTERVAL_TICKS = 10;

    // Players we last told "you have active links" — so we know
    // who needs an empty-list packet once they stop holding the linker.
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

        boolean holdingLinker =
                player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.CHEST_LINKER);

        UUID uuid = player.getUUID();

        if (!holdingLinker) {
            // If we previously sent them data, clear it now.
            if (ACTIVELY_SYNCED.remove(uuid)) {
                ServerPlayNetworking.send(player, new LinkerSyncPayload(List.of()));
            }
            return;
        }

        ServerLevel world = player.level();
        List<NodeLinkData> owned = LogisticsManager.getOwnedNodeLinks(world, uuid);

        ACTIVELY_SYNCED.add(uuid);
        ServerPlayNetworking.send(player, new LinkerSyncPayload(owned));
    }
}