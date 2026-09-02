package qnimfi.cs;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NonNull;
import qnimfi.cs.item.ModItems;
import qnimfi.cs.menu.ChestLinkerConfigMenu;
import qnimfi.cs.menu.ChestLinkerMenuData;

public class ChestInteractionHandler {

    public static void initialize() {
        UseBlockCallback.EVENT.register(ChestInteractionHandler::interact);
    }

    private static InteractionResult interact(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!player.getItemInHand(hand).is(ModItems.CHEST_LINKER)) return InteractionResult.PASS;

        var pos = ChestUtil.canonicalPos(world, hitResult.getBlockPos());
        if (!world.getBlockState(pos).is(Blocks.CHEST)) return InteractionResult.PASS;

        if (world.isClientSide()) return InteractionResult.SUCCESS;

        ServerPlayer serverPlayer = (ServerPlayer) player;
        ServerLevel serverLevel = (ServerLevel) world;
        LinkerState state = LinkerState.get(serverPlayer);

        // Crouch + RMB = start connection mode.
        if (player.isCrouching()) {
            return selectSender(serverPlayer, serverLevel, pos, state);
        }

        // We are currently connecting/disconnecting receivers.
        if (state.hasSender()) {
            return handleConnectionClick(serverPlayer, serverLevel, pos, state);
        }

        // Normal RMB outside connection mode:
        boolean isSender = LogisticsManager.getNode(serverLevel, pos) != null
                && LogisticsManager.isOwner(serverLevel, pos, serverPlayer);
        boolean isReceiver = LogisticsManager.canUseReceiver(serverLevel, pos, serverPlayer, AuthorityPermission.FILTER)
                || LogisticsManager.canUseReceiver(serverLevel, pos, serverPlayer, AuthorityPermission.SETTINGS);

        if (isReceiver) {
            openReceiverConfig(serverPlayer, serverLevel, pos);
            return InteractionResult.SUCCESS;
        } else if (isSender) {
            // Prevent Receiver GUI from opening on a pure Sender!
            player.sendSystemMessage(
                    Component.translatable("chest_interaction.chestsorter.config_on_sender")
            );
            return InteractionResult.SUCCESS;
        }

        // Linker on an unrelated chest does nothing.
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult selectSender(ServerPlayer player, ServerLevel world, net.minecraft.core.BlockPos pos, LinkerState state) {
        LogisticsNode node = LogisticsManager.getNode(world, pos);

        if (node != null && !node.getOwner().equals(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("chest_interaction.chestsorter.not_sender_owner"), true);
            return InteractionResult.SUCCESS;
        }

        state.setSelectedSender(pos);

        player.sendSystemMessage(
                Component.translatable("chest_interaction.chestsorter.connection_mode_on"),
                true
        );

        ChestSorter.LOGGER.info("Player {} selected sender at {}", player.getName().getString(), pos);

        return InteractionResult.SUCCESS;
    }

    private static InteractionResult handleConnectionClick(ServerPlayer player, ServerLevel world, net.minecraft.core.BlockPos pos, LinkerState state) {
        var senderPos = state.getSelectedSender();

        // Check reach distance config
        double maxReach = ChestSorterConfig.get().senderReachDistance;
        if (maxReach > 0 && senderPos.distToCenterSqr(player.position()) > maxReach * maxReach) {
            LinkerState.clear(player);
            player.sendSystemMessage(Component.translatable("chest_interaction.chestsorter.out_of_reach"), true);
            return InteractionResult.SUCCESS;
        }

        // Check if the sender chest was destroyed mid-connection
        if (!world.getBlockState(senderPos).is(Blocks.CHEST)) {
            LinkerState.clear(player);
            player.sendSystemMessage(Component.translatable("chest_interaction.chestsorter.sender_missing"), true);
            return InteractionResult.SUCCESS;
        }

        // Clicking the sender again finishes connection mode.
        if (senderPos.equals(pos)) {
            LinkerState.clear(player);
            player.sendSystemMessage(Component.translatable("chest_interaction.chestsorter.connection_mode_off"), true);
            return InteractionResult.SUCCESS;
        }

        LogisticsNode sender = LogisticsManager.getNode(world, senderPos);

        // The sender may have disappeared while editing.
        if (sender != null && !sender.getOwner().equals(player.getUUID())) {
            LinkerState.clear(player);
            player.sendSystemMessage(Component.translatable("chest_interaction.chestsorter.not_sender_owner"), true);
            return InteractionResult.SUCCESS;
        }

        ConnectionResult result;

        if (sender != null && sender.hasReceiver(pos)) {
            result = LogisticsManager.disconnect(world, senderPos, pos, player);
        } else {
            result = LogisticsManager.connect(world, senderPos, pos, player);
        }

        switch (result) {
            case CONNECTED -> {
                player.sendSystemMessage(Component.translatable("chest_interaction.chestsorter.receiver_connected"), true);
                ChestSorter.LOGGER.info("Connected {} -> {}", senderPos, pos);
            }

            case DISCONNECTED -> {
                player.sendSystemMessage(Component.translatable("chest_interaction.chestsorter.receiver_disconnected"), true);
                ChestSorter.LOGGER.info("Disconnected {} -> {}", senderPos, pos);
            }

            case ALREADY_CONNECTED -> player.sendSystemMessage(
                    Component.translatable("chest_interaction.chestsorter.receiver_already_connected"), true
            );

            case NOT_CONNECTED -> player.sendSystemMessage(
                    Component.translatable("chest_interaction.chestsorter.receiver_not_connected"), true
            );

            case NOT_OWNER -> player.sendSystemMessage(
                    Component.translatable("chest_interaction.chestsorter.receiver_not_owner"), true
            );

            case NULL -> player.sendSystemMessage(
                    Component.translatable("chest_interaction.chestsorter.receiver_null"), true
            );
        }

        return InteractionResult.SUCCESS;
    }

    private static void openReceiverConfig(ServerPlayer player, ServerLevel world, net.minecraft.core.BlockPos pos) {
        ReceiverConfig config = LogisticsManager.getOrCreateReceiverConfig(world, pos);

        player.openMenu(new net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider<ChestLinkerMenuData>() {
            @Override
            public @NonNull Component getDisplayName() {
                return Component.translatable("chest_interaction.chestsorter.receiver_config_open");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int syncId,
                    net.minecraft.world.entity.player.@NonNull Inventory inv,
                    @NonNull Player p
            ) {
                return new ChestLinkerConfigMenu(syncId, inv, world, pos);
            }

            @Override
            public @NonNull ChestLinkerMenuData getScreenOpeningData(@NonNull ServerPlayer player) {
                return new ChestLinkerMenuData(pos, config.getSlotCount());
            }
        });
    }
}