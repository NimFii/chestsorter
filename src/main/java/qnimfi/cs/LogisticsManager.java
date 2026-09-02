package qnimfi.cs;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import qnimfi.cs.network.NodeLinkData;

import java.util.*;

public class LogisticsManager {

    static ChestSorterData getData(ServerLevel world) {
        return world.getDataStorage().computeIfAbsent(
                ChestSorterData.TYPE
        );
    }

    public static LogisticsNode getNode(
            ServerLevel world,
            BlockPos position
    ) {
        return getData(world).getNode(position);
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

        if (sender.getReceivers().isEmpty()) {
            data.removeNode(senderPos);

            // Clear active selection if this was the player's active sender
            LinkerState state = LinkerState.get(player);
            if (senderPos.equals(state.getSelectedSender())) {
                state.setSelectedSender(null);
                player.sendSystemMessage(Component.translatable("chest_interaction.chestsorter.connection_mode_off"));
            }

            ChestSorter.LOGGER.info("Sender Chest cleared automatically (zero receivers left) at {}", senderPos);
        }
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
                ownerPlayer.sendSystemMessage(Component.translatable("logistics.chestsorter.sender_cleared", pos.toShortString()));
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

    public static Map<BlockPos, ReceiverConfig> getReceiverConfigsForPlayer(ServerLevel level, UUID playerId) {
        Map<BlockPos, ReceiverConfig> map = new HashMap<>();
        for (NodeLinkData node : getOwnedNodeLinks(level, playerId)) {
            for (BlockPos receiverPos : node.receivers()) {
                ReceiverConfig config = getConfigIfPresent(level, receiverPos);
                if (config != null) {
                    map.put(receiverPos, config);
                }
            }
        }
        return map;
    }

    public static boolean canUseReceiver(ServerLevel world, BlockPos receiverPos, ServerPlayer player, AuthorityPermission permission) {
        if (canConfigureReceiver(world, receiverPos, player)) {
            return true;
        }

        ReceiverConfig config = getConfigIfPresent(world, receiverPos);
        if (config == null) return false;

        return config.getPermissions(player.getUUID()).permissions(permission);
    }

    public static boolean hasAuthority(ServerLevel world, BlockPos receiverPos, ServerPlayer player) {
        return !canUseReceiver(world, receiverPos, player, AuthorityPermission.AUTHORITY);
    }

    public static void setPermission(ServerLevel world, BlockPos receiverPos, UUID targetPlayer, AuthorityPermission permission, boolean enabled) {
        ChestSorterData data = getData(world);
        ReceiverConfig config = data.getOrCreateConfig(receiverPos);

        ReceiverConfig.ReceiverPermissions old = config.getPermissions(targetPlayer);

        ReceiverConfig.ReceiverPermissions updated = switch (permission) {
            case GIZMOS -> new ReceiverConfig.ReceiverPermissions(enabled, old.filter(), old.settings(), old.authority(), old.connect());
            case FILTER -> new ReceiverConfig.ReceiverPermissions(old.gizmos(), enabled, old.settings(), old.authority(), old.connect());
            case SETTINGS -> new ReceiverConfig.ReceiverPermissions(old.gizmos(), old.filter(), enabled, old.authority(), old.connect());
            case AUTHORITY -> new ReceiverConfig.ReceiverPermissions(old.gizmos(), old.filter(), old.settings(), enabled, old.connect());
            case CONNECT -> new ReceiverConfig.ReceiverPermissions(old.gizmos(), old.filter(), old.settings(), old.authority(), enabled);
        };

        config.setPermissions(targetPlayer, updated);
        data.setDirty();
    }
}