package qnimfi.cs;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ItemTransferHandler {

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                tick(level);
            }
        });
    }

    private static void tick(ServerLevel level) {
        Set<BlockPos> dirty = DirtyChestTracker.drain(level);
        if (dirty.isEmpty()) return;

        List<LogisticsNode> allNodes = List.copyOf(LogisticsManager.getAllNodes(level));
        Set<BlockPos> sendersToProcess = new HashSet<>();

        for (BlockPos p : dirty) {
            for (LogisticsNode node : allNodes) {
                if (node.getPosition().equals(p) || node.hasReceiver(p)) {
                    sendersToProcess.add(node.getPosition());
                }
            }
        }

        for (BlockPos senderPos : sendersToProcess) {
            LogisticsNode node = LogisticsManager.getNode(level, senderPos);
            if (node != null) {
                processSender(level, node);
            }
        }
    }

    private static void processSender(ServerLevel level, LogisticsNode senderNode) {
        BlockPos senderPos = senderNode.getPosition();
        if (!level.isLoaded(senderPos)) return;

        Container senderContainer = LogisticsManager.getContainerAt(level, senderPos);
        if (senderContainer == null) return;

        List<BlockPos> receivers = senderNode.getReceivers().stream().toList();
        if (receivers.isEmpty()) return;

        List<BlockPos> filteredReceivers = new java.util.ArrayList<>();
        List<BlockPos> trashReceivers = new java.util.ArrayList<>();

        for (BlockPos receiverPos : receivers) {
            if (!level.isLoaded(receiverPos)) continue;
            ReceiverConfig config = LogisticsManager.getConfigIfPresent(level, receiverPos);
            if (config == null || hasNoFilters(config)) {
                trashReceivers.add(receiverPos);
            } else {
                filteredReceivers.add(receiverPos);
            }
        }

        List<BlockPos> orderedReceivers = new java.util.ArrayList<>(filteredReceivers);
        orderedReceivers.addAll(trashReceivers);

        for (int slot = 0; slot < senderContainer.getContainerSize(); slot++) {
            ItemStack stack = senderContainer.getItem(slot);
            if (stack.isEmpty()) continue;

            for (BlockPos receiverPos : orderedReceivers) {
                if (stack.isEmpty()) break;
                if (!level.isLoaded(receiverPos)) continue;

                ReceiverConfig config = LogisticsManager.getConfigIfPresent(level, receiverPos);
                Container receiverContainer = LogisticsManager.getContainerAt(level, receiverPos);
                if (receiverContainer == null) continue;

                if (config == null || hasNoFilters(config)) {
                    int moved = insertIntoContainer(receiverContainer, stack, stack.getCount());
                    if (moved > 0) {
                        stack.shrink(moved);
                        senderContainer.setChanged();
                        receiverContainer.setChanged();
                    }
                    continue;
                }

                // Check BURN mode first
                if (config.isBurnItem(stack.getItem())) {
                    int burnCount = stack.getCount();
                    Item burnedItem = stack.getItem();
                    stack.setCount(0);
                    senderContainer.setChanged();
                    ChestSorter.LOGGER.info("Burned {} items of type {} at receiver {}", burnCount, burnedItem, receiverPos);
                    continue;
                }

                Optional<FilterEntry> filterOpt = config.getFilterFor(stack.getItem());
                if (filterOpt.isEmpty()) continue;

                FilterEntry filter = filterOpt.get();
                if (!filter.type().allows(filter.item(), stack.getItem())) continue;

                int currentCount = countItem(receiverContainer, stack.getItem());
                int maxAllowed = filter.maxCount();
                int room = (maxAllowed < 0) ? Integer.MAX_VALUE : (maxAllowed - currentCount);
                if (room <= 0) continue;

                int toMove = Math.min(stack.getCount(), room);
                int moved = insertIntoContainer(receiverContainer, stack, toMove);

                if (moved > 0) {
                    stack.shrink(moved);
                    senderContainer.setChanged();
                    receiverContainer.setChanged();
                }
            }
        }
    }

    private static boolean hasNoFilters(ReceiverConfig config) {
        for (int i = 0; i < config.getSlotCount(); i++) {
            if (config.getItem(i) != Items.AIR) {
                return false;
            }
        }
        return true;
    }

    private static int countItem(Container container, Item item) {
        int total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack s = container.getItem(i);
            if (s.getItem() == item) total += s.getCount();
        }
        return total;
    }

    private static int insertIntoContainer(Container container, ItemStack stack, int amount) {
        int remaining = amount;

        for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
            ItemStack slotStack = container.getItem(i);
            if (slotStack.isEmpty() || slotStack.getItem() != stack.getItem()) continue;

            int space = Math.min(container.getMaxStackSize(), slotStack.getMaxStackSize()) - slotStack.getCount();
            if (space <= 0) continue;

            int add = Math.min(space, remaining);
            slotStack.grow(add);
            remaining -= add;
        }

        for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
            if (!container.getItem(i).isEmpty()) continue;

            int add = Math.min(Math.min(container.getMaxStackSize(), stack.getMaxStackSize()), remaining);
            container.setItem(i, stack.copyWithCount(add));
            remaining -= add;
        }

        return amount - remaining;
    }
}