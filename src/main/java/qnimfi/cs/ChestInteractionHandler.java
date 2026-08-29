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
import qnimfi.cs.menu.ChestLinkerConfigMenu;

public class ChestInteractionHandler {

    public static void initialize() {

        UseBlockCallback.EVENT.register(ChestInteractionHandler::interact);

    }

    private static InteractionResult interact(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {

        // We only care about the main hand.
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        // We only care about the Chest Linker.
        if (!player.getItemInHand(hand).is(ModItems.CHEST_LINKER)) {
            return InteractionResult.PASS;
        }

        var pos = ChestUtil.canonicalPos(world, hitResult.getBlockPos());
        var blockState = world.getBlockState(pos);

        // We only intercept actual chests.
        if (!blockState.is(Blocks.CHEST)) {
            return InteractionResult.PASS;
        }

        /*
         * IMPORTANT:
         *
         * Consume the interaction on the client too.
         * Otherwise, Minecraft may continue with the normal
         * chest interaction and open the chest.
         */
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        ServerLevel serverLevel = (ServerLevel) world;

        LinkerState state = LinkerState.get(serverPlayer);

        /*
         * SHIFT + RIGHT CLICK
         *
         * Select a sender.
         */
        if (!player.isCrouching() && state.getMode() == LinkerMode.CONFIGURE) {

            if (!LogisticsManager.canConfigureReceiver(serverLevel, pos, serverPlayer)) {
                serverPlayer.sendSystemMessage(Component.literal("This chest isn't one of your receivers."), true);
                return InteractionResult.SUCCESS;
            }

            ReceiverConfig config = LogisticsManager.getOrCreateReceiverConfig(serverLevel, pos);

            serverPlayer.openMenu(new net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider<qnimfi.cs.menu.ChestLinkerMenuData>() {

                @Override
                public @NonNull Component getDisplayName() {
                    return Component.literal("Configure Receiver");
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                        int syncId, net.minecraft.world.entity.player.@NonNull Inventory inv, net.minecraft.world.entity.player.Player p) {
                    return new ChestLinkerConfigMenu(syncId, inv, serverLevel, pos);
                }

                @Override
                public qnimfi.cs.menu.@NonNull ChestLinkerMenuData getScreenOpeningData(@NonNull ServerPlayer player) {
                    return new qnimfi.cs.menu.ChestLinkerMenuData(pos, config.getSlotCount());
                }
            });

            return InteractionResult.SUCCESS;
        }

        if (player.isCrouching()) {

            LogisticsNode node = LogisticsManager.getNode(
                    serverLevel,
                    pos
            );

            // This chest already has a logistics configuration.
            if (node != null &&
                    !node.getOwner().equals(player.getUUID())) {

                serverPlayer.sendSystemMessage(
                        Component.literal(
                                "You don't own this sender."
                        ),
                        true
                );

                // We handled the click, so DO NOT open the chest.
                return InteractionResult.SUCCESS;
            }

            state.setSelectedSender(pos);

            serverPlayer.sendSystemMessage(
                    Component.literal(
                            "Edit mode - ON\nRight-click receiver chests."
                    ),
                    true
            );

            ChestSorter.LOGGER.info(
                    "Player {} selected sender at {}",
                    player.getName().getString(),
                    pos
            );

            // We handled the click.
            return InteractionResult.SUCCESS;
        }

        /*
         * NORMAL RIGHT CLICK
         *
         * If we already have a sender selected,
         * connect this chest to it.
         */
        if (state.hasSender()) {

            var senderPos = state.getSelectedSender();

            // Don't connect a chest to itself.
            if (senderPos.equals(pos)) {

                state.clear();

                serverPlayer.sendSystemMessage(
                        Component.literal(
                                "Edit mode - OFF."
                        ),
                        true
                );

                return InteractionResult.SUCCESS;
            }

            ConnectionResult result = ConnectionResult.NULL;
            result = switch (state.getMode()) {
                case CONNECT -> LogisticsManager.connect(
                        serverLevel,
                        senderPos,
                        pos,
                        serverPlayer
                );
                case DISCONNECT -> LogisticsManager.disconnect(
                        serverLevel,
                        senderPos,
                        pos,
                        serverPlayer
                );
                default -> result;
            };

            switch (result) {

                case CONNECTED -> {
                    serverPlayer.sendSystemMessage(Component.literal("Receiver connected!"), true);
                    ChestSorter.LOGGER.info("Connected {} -> {}", senderPos, pos);
                }

                case DISCONNECTED -> {
                    serverPlayer.sendSystemMessage(Component.literal("Receiver disconnected!"), true);
                    ChestSorter.LOGGER.info("Disconnected {} -> {}", senderPos, pos);
                }

                case ALREADY_CONNECTED -> serverPlayer.sendSystemMessage(Component.literal("Receiver is already connected."), true);

                case NOT_CONNECTED -> serverPlayer.sendSystemMessage(Component.literal("Nothing to disconnect."), true);

                case NOT_OWNER -> serverPlayer.sendSystemMessage(Component.literal("You don't own this sender."), true);
            }

            // We handled the click.
            return InteractionResult.SUCCESS;
        }

        /*
         * The linker was used on a chest, but there is
         * currently no selected sender.
         *
         * Still consume the click so the chest doesn't open.
         */
        return InteractionResult.SUCCESS;
    }
}