package qnimfi.cs.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmos;
import qnimfi.cs.gizmo.LinkerBoxGizmo;
import qnimfi.cs.gizmo.LinkerLineGizmo;

import java.util.List;
import java.util.Map;

public class LinkerGizmoEmitter {

    private static final int SENDER_COLOR = 0xFF33E5FF;   // ARGB
    private static final int RECEIVER_COLOR = 0xFF4DFF4D;
    private static final int LINE_COLOR = 0xFFFFFF4D;

    // Bridges the gap between client ticks (20/s) and render frames (60+/s)
    // so the gizmo doesn't flicker in and out between tick submissions.
    private static final long PERSIST_MILLIS = 100;

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> emit());
    }

    private static void emit() {
        Map<BlockPos, List<BlockPos>> nodes = ClientLinkerRenderData.getNodes();

        for (Map.Entry<BlockPos, List<BlockPos>> entry : nodes.entrySet()) {
            BlockPos sender = entry.getKey();

            Gizmos.addGizmo(new LinkerBoxGizmo(sender, SENDER_COLOR))
                    .setAlwaysOnTop()
                    .persistForMillis((int) PERSIST_MILLIS);

            for (BlockPos receiver : entry.getValue()) {
                Gizmos.addGizmo(new LinkerBoxGizmo(receiver, RECEIVER_COLOR))
                        .setAlwaysOnTop()
                        .persistForMillis((int) PERSIST_MILLIS);

                Gizmos.addGizmo(new LinkerLineGizmo(sender, receiver, LINE_COLOR))
                        .setAlwaysOnTop()
                        .persistForMillis((int) PERSIST_MILLIS);
            }
        }
    }
}