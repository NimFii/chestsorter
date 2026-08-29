package qnimfi.cs;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.commands.MsgCommand;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerEntityGetter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import org.apache.logging.log4j.core.jmx.Server;
import qnimfi.cs.menu.ChestLinkerConfigMenu;
import qnimfi.cs.network.NodeLinkData;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class LogisticsManager {

    private static ChestSorterData getData(ServerLevel world) {

        ChestSorter.LOGGER.info(
                "Getting ChestSorter SavedData for world: {}",
                world.dimension().identifier()
        );

        ChestSorterData data = world.getDataStorage().computeIfAbsent(
                ChestSorterData.TYPE
        );

        ChestSorter.LOGGER.info(
                "ChestSorter SavedData obtained. Nodes: {}, dirty: {}",
                data.nodes.size(),
                data.isDirty()
        );

        return data;
    }

    public static LogisticsNode getNode(
            ServerLevel world,
            BlockPos position
    ) {
        return getData(world).getNode(position);
    }

    public static LogisticsNode getOrCreateNode(
            ServerLevel world,
            BlockPos position,
            ServerPlayer player
    ) {
        ChestSorterData data = getData(world);

        LogisticsNode node = data.getNode(position);

        if (node == null) {
            node = new LogisticsNode(
                    position,
                    player.getUUID()
            );

            data.addNode(node);
        }

        return node;
    }

    public static ConnectionResult connect(
            ServerLevel world,
            BlockPos senderPos,
            BlockPos receiverPos,
            ServerPlayer player
    ) {
        ChestSorterData data = getData(world);

        LogisticsNode sender = data.getNode(senderPos);

        // The chest has never been configured as a sender.
        // This player becomes its owner.
        if (sender == null) {
            sender = new LogisticsNode(
                    senderPos,
                    player.getUUID()
            );

            data.addNode(sender);
        }

        // Somebody else owns this sender.
        if (!sender.getOwner().equals(player.getUUID())) {
            return ConnectionResult.NOT_OWNER;
        }

        if (sender.hasReceiver(receiverPos)) {
            return ConnectionResult.ALREADY_CONNECTED;
        }

        sender.addReceiver(receiverPos);
        data.setDirty();

        return ConnectionResult.CONNECTED;
    }

    public static boolean isOwner(
            ServerLevel world,
            BlockPos position,
            ServerPlayer player
    ) {
        LogisticsNode node = getNode(world, position);

        if (node == null) {
            return false;
        }

        return node.getOwner().equals(player.getUUID());
    }

    public static ConnectionResult disconnect(
            ServerLevel world,
            BlockPos senderPos,
            BlockPos receiverPos,
            ServerPlayer player
    ) {
        ChestSorterData data = getData(world);
        LogisticsNode sender = data.getNode(senderPos);

        // No sender registered here at all.
        if (sender == null) {
            return ConnectionResult.NULL;
        }

        // Somebody else owns this sender.
        if (!sender.getOwner().equals(player.getUUID())) {
            return ConnectionResult.NOT_OWNER;
        }

        // Wasn't connected in the first place.
        if (!sender.hasReceiver(receiverPos)) {
            return ConnectionResult.NOT_CONNECTED;
        }

        sender.removeReceiver(receiverPos);
        data.setDirty();

        return ConnectionResult.DISCONNECTED;
    }

    public static List<NodeLinkData> getOwnedNodeLinks(ServerLevel world, java.util.UUID owner) {
        ChestSorterData data = getData(world);

        List<NodeLinkData> result = new java.util.ArrayList<>();

        for (LogisticsNode node : data.nodes.values()) {
            if (node.getOwner().equals(owner)) {
                result.add(new NodeLinkData(
                        node.getPosition(),
                        node.getReceivers().stream().toList()
                ));
            }
        }

        return result;
    }

    public static void removeChest(ServerLevel world, BlockPos pos) {
        ChestSorterData data = getData(world);

        LogisticsNode existingNode = data.getNode(pos);
        UUID ownerUuid = existingNode != null ? existingNode.getOwner() : null;
        ServerPlayer ownerPlayer = ownerUuid != null
                ? PlayerLookup.level(world).stream()
                .filter(player -> player.getUUID().equals(ownerUuid))
                .findFirst()
                .orElse(null)
                : null;

        if (existingNode != null) {
            data.removeNode(pos);
            ChestSorter.LOGGER.info("Sorter Chest (sender) cleared at {}", pos);
            if (ownerPlayer != null) {
                ownerPlayer.sendSystemMessage(Component.literal("Sorter Chest cleared at " + pos.toShortString()));
            }
        }

        data.removeConfig(pos);

        boolean changed = false;
        for (LogisticsNode node : data.nodes.values()) {
            if (node.removeReceiver(pos)) {
                changed = true;
            }
        }
        if (changed) {
            data.setDirty();
            ChestSorter.LOGGER.info("Receiver Chest (receiver) cleared at {}", pos);
        }
    }

    public static ReceiverConfig getOrCreateReceiverConfig(ServerLevel world, BlockPos pos) {
        return getData(world).getOrCreateConfig(pos);
    }

    public static void setFilterItem(ServerLevel world, BlockPos pos, int slot, Item item) {
        ChestSorterData data = getData(world);
        ReceiverConfig config = data.getOrCreateConfig(pos);

        if (slot < 0 || slot >= config.getSlotCount()) {
            return;
        }

        config.setFilterItem(slot, item);
        data.setDirty();
    }

    public static void setFilterMax(ServerLevel world, BlockPos pos, int slot, int max) {
        ChestSorterData data = getData(world);
        data.getOrCreateConfig(pos).setFilterMax(slot, max);
        data.setDirty();
    }

    /**
     * Only lets a player configure a chest they own as a receiver via one
     * of their own sender nodes. Ties config permission to the connection
     * graph rather than adding a separate receiver-ownership concept.
     */
    public static boolean canConfigureReceiver(ServerLevel world, BlockPos receiverPos, ServerPlayer player) {
        ChestSorterData data = getData(world);
        return data.nodes.values().stream()
                .anyMatch(node -> node.getOwner().equals(player.getUUID()) && node.hasReceiver(receiverPos));
    }

    public static Container getContainerAt(ServerLevel world, BlockPos pos) {
        return net.minecraft.world.level.block.entity.HopperBlockEntity.getContainerAt(world, pos);
    }

    public static Collection<LogisticsNode> getAllNodes(ServerLevel world) {
        return getData(world).nodes.values();
    }

    public static ReceiverConfig getConfigIfPresent(ServerLevel world, BlockPos pos) {
        return getData(world).receiverConfigs.get(pos);
    }
}