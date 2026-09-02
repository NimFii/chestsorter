package qnimfi.cs.client.gizmo;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmo;
import qnimfi.cs.network.LinkerSyncPayload;
import qnimfi.cs.network.NodeLinkData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class ClientGizmoManager {

    private static final List<Gizmo> ACTIVE_GIZMOS = Collections.synchronizedList(new ArrayList<>());

    private static List<NodeLinkData> storedNodes = List.of();
    private static BlockPos storedActiveSender = null;
    private static final Map<BlockPos, List<LinkerSyncPayload.ClientFilterEntry>> storedReceiverFilters = new HashMap<>();

    public static List<LinkerSyncPayload.ClientFilterEntry> getFilters(BlockPos pos) {
        return storedReceiverFilters.get(pos);
    }

    public static void addGizmo(Gizmo gizmo) {
        ACTIVE_GIZMOS.add(gizmo);
    }

    public static void clear() {
        ACTIVE_GIZMOS.clear();
        storedReceiverFilters.clear();
    }

    public static List<Gizmo> getGizmos() {
        return ACTIVE_GIZMOS;
    }

    public static List<NodeLinkData> getStoredNodes() {
        return storedNodes;
    }

    public static Optional<BlockPos> getStoredActiveSender() {
        return Optional.ofNullable(storedActiveSender);
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(LinkerSyncPayload.TYPE, (payload, context) -> context.client().execute(() -> {
            clear();

            storedNodes = payload.nodes();
            storedActiveSender = payload.activeSender().orElse(null);
            storedReceiverFilters.putAll(payload.receiverFilters());

            for (NodeLinkData node : storedNodes) {
                addGizmo(new LinkerBoxGizmo(node.sender(), 0x00FF00));
                for (BlockPos receiverPos : node.receivers()) {
                    addGizmo(new LinkerBoxGizmo(receiverPos, 0x0000FF));
                }

                boolean isActiveSender = storedActiveSender != null
                        && storedActiveSender.equals(node.sender());

                if (isActiveSender) {
                    for (BlockPos receiverPos : node.receivers()) {
                        addGizmo(new LinkerArrowPathGizmo(node.sender(), receiverPos, 0xFFFF00));
                    }
                }
            }
        }));
    }
}