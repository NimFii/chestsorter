package qnimfi.cs.client.gizmo;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.Vec3;
import qnimfi.cs.network.NodeLinkData;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ClientGizmoRenderer {

    private static final int SENDER_COLOR = 0xFF33E5FF;   // ARGB
    private static final int RECEIVER_COLOR = 0xFF4DFF4D;
    private static final int ARROW_COLOR = 0xFFFFEE4D;    // Yellow for 2D arrows

    private static final long PERSIST_MILLIS = 100;

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientGizmoRenderer::emit);
    }

    private static void emit(Minecraft client) {
        List<NodeLinkData> nodes = ClientGizmoManager.getStoredNodes();
        Optional<BlockPos> activeSender = ClientGizmoManager.getStoredActiveSender();

        Set<BlockPos> allReceivers = new HashSet<>();
        for (NodeLinkData node : nodes) {
            allReceivers.addAll(node.receivers());
        }

        Set<BlockPos> allSenders = nodes.stream().map(NodeLinkData::sender).collect(Collectors.toSet());

        for (NodeLinkData node : nodes) {
            BlockPos sender = node.sender();
            boolean isBoth = allReceivers.contains(sender);

            int senderColor = isBoth ? 0xFFFF8000 : SENDER_COLOR;

            Gizmos.addGizmo(new LinkerBoxGizmo(sender, senderColor))
                    .setAlwaysOnTop()
                    .persistForMillis((int) PERSIST_MILLIS);

            boolean isActiveSender = activeSender.isPresent() && activeSender.get().equals(sender);

            for (BlockPos receiver : node.receivers()) {
                boolean receiverIsAlsoSender = allSenders.contains(receiver);
                int receiverColor = receiverIsAlsoSender ? 0xFFFF8000 : RECEIVER_COLOR;

                Gizmos.addGizmo(new LinkerBoxGizmo(receiver, receiverColor))
                        .setAlwaysOnTop()
                        .persistForMillis((int) PERSIST_MILLIS);

                if (isActiveSender) {
                    Gizmos.addGizmo(new LinkerArrowPathGizmo(sender, receiver, ARROW_COLOR))
                            .setAlwaysOnTop()
                            .persistForMillis((int) PERSIST_MILLIS);
                }
            }
        }

        if (activeSender.isPresent() && client.player != null) {
            BlockPos sender = activeSender.get();

            Vec3 eyePos = client.player.getEyePosition();
            Vec3 lookDir = client.player.getLookAngle();
            Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();

            boolean isMainHandRight = client.player.getMainArm().ordinal() == 1; // 1 = RIGHT, 0 = LEFT
            double sideMultiplier = isMainHandRight ? 0.3 : -0.3;

            Vec3 handPos = eyePos
                    .add(0, -0.35, 0)
                    .add(lookDir.scale(0.5))
                    .add(rightDir.scale(sideMultiplier));

            Gizmos.addGizmo(new LinkerArrowPathGizmo(Vec3.atCenterOf(sender), handPos, ARROW_COLOR))
                    .setAlwaysOnTop();
        } else {
            // Only show hover arrows when NOT in connection mode
            List<LinkerArrowPathGizmo> hoverGizmos = ClientLinkerHudRenderer.getHoverGizmos(client.level);
            for (LinkerArrowPathGizmo gizmo : hoverGizmos) {
                Gizmos.addGizmo(gizmo)
                        .setAlwaysOnTop()
                        .persistForMillis((int) PERSIST_MILLIS);
            }
        }
    }
}