package qnimfi.cs.client.gizmo;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmo;
import qnimfi.cs.network.LinkerSyncPayload;
import qnimfi.cs.network.NodeLinkData;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Optional;

public class ClientGizmoManager {

    private static final List<Gizmo> ACTIVE_GIZMOS = Collections.synchronizedList(new ArrayList<>());

    // Storage fields for the emitter loop
    private static List<NodeLinkData> storedNodes = List.of();
    private static Optional<BlockPos> storedActiveSender = Optional.empty();

    public static void addGizmo(Gizmo gizmo) {
        ACTIVE_GIZMOS.add(gizmo);
    }

    public static void clear() {
        ACTIVE_GIZMOS.clear();
    }

    public static List<Gizmo> getGizmos() {
        return ACTIVE_GIZMOS;
    }

    // Getters for LinkerGizmoEmitter
    public static List<NodeLinkData> getStoredNodes() {
        return storedNodes;
    }

    public static Optional<BlockPos> getStoredActiveSender() {
        return storedActiveSender;
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(LinkerSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                clear();

                // Save incoming payload data here so the emitter can read it
                storedNodes = payload.nodes();
                storedActiveSender = payload.activeSender();

                for (NodeLinkData node : storedNodes) {
                    addGizmo(new LinkerBoxGizmo(node.sender(), 0x00FF00));
                    for (BlockPos receiverPos : node.receivers()) {
                        addGizmo(new LinkerBoxGizmo(receiverPos, 0x0000FF));
                    }

                    boolean isActiveSender = storedActiveSender.isPresent()
                            && storedActiveSender.get().equals(node.sender());

                    if (isActiveSender) {
                        for (BlockPos receiverPos : node.receivers()) {
                            addGizmo(new LinkerArrowPathGizmo(node.sender(), receiverPos, 0xFFFF00));
                        }
                    }
                }
            });
        });
    }
}