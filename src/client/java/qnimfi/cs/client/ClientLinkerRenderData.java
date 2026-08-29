package qnimfi.cs.client;

import net.minecraft.core.BlockPos;
import qnimfi.cs.network.NodeLinkData;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClientLinkerRenderData {

    private static volatile Map<BlockPos, List<BlockPos>> nodes = Map.of();

    public static void update(List<NodeLinkData> data) {
        nodes = data.stream().collect(Collectors.toMap(
                NodeLinkData::sender,
                NodeLinkData::receivers
        ));
    }

    public static Map<BlockPos, List<BlockPos>> getNodes() {
        return nodes;
    }
}