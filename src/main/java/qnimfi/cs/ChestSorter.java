package qnimfi.cs;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qnimfi.cs.item.ModItems;
import qnimfi.cs.menu.ChestLinkerConfigMenu;
import qnimfi.cs.menu.ModMenuTypes;
import qnimfi.cs.network.AdjustFilterMaxPayload;
import qnimfi.cs.network.CycleFilterTypePayload;
import qnimfi.cs.network.LinkerSyncPayload;
import qnimfi.cs.network.SetFilterPayload;
import qnimfi.cs.network.AuthorityDataPayload;
import qnimfi.cs.network.RequestAuthorityDataPayload;
import qnimfi.cs.network.SetAuthorityPermissionPayload;

import java.util.List;

public class ChestSorter implements ModInitializer {
    public static final String MOD_ID = "chestsorter";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing items...");
        ModItems.initialize();

        LOGGER.info("Registering interaction handlers...");
        ChestInteractionHandler.initialize();

        LOGGER.info("Loading configuration...");
        ChestSorterConfig.get();

        LOGGER.info("Registering menu types...");
        ModMenuTypes.initialize();

        LOGGER.info("Setting up item transfer and synchronization handlers...");
        ItemTransferHandler.initialize();
        LinkerSyncHandler.initialize();

        ServerPlayConnectionEvents.DISCONNECT.register((handler, _) -> LinkerState.clear(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((_, newPlayer, _) -> LinkerState.clear(newPlayer));

        LOGGER.info("Registering network payloads...");
        PayloadTypeRegistry.clientboundPlay().register(LinkerSyncPayload.TYPE, LinkerSyncPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetFilterPayload.TYPE, SetFilterPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(AdjustFilterMaxPayload.TYPE, AdjustFilterMaxPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CycleFilterTypePayload.TYPE, CycleFilterTypePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(AuthorityDataPayload.TYPE, AuthorityDataPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestAuthorityDataPayload.TYPE, RequestAuthorityDataPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetAuthorityPermissionPayload.TYPE, SetAuthorityPermissionPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SetFilterPayload.TYPE, (payload, context) -> context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (!(player.containerMenu instanceof ChestLinkerConfigMenu menu)) return;

            if (payload.slot() < 0 || payload.slot() >= menu.getSlotCount()) return;

            BlockPos receiverPos = menu.getReceiverPos();
            if (!LogisticsManager.canConfigureReceiver(player.level(), receiverPos, player)) return;
            if (receiverPos.distToCenterSqr(player.position()) > 8.0 * 8.0) return;

            if (payload.itemId() < 0) {
                LogisticsManager.setFilterItem(player.level(), receiverPos, payload.slot(), Items.AIR);
                return;
            }

            if (payload.itemId() >= BuiltInRegistries.ITEM.size()) return;
            Item item = BuiltInRegistries.ITEM.byId(payload.itemId());
            if (item == Items.AIR) return;

            ItemStack carried = menu.getCarried();
            if (carried.isEmpty() || carried.getItem() != item) return;

            LogisticsManager.setFilterItem(player.level(), receiverPos, payload.slot(), item);
        }));

        ServerPlayNetworking.registerGlobalReceiver(AdjustFilterMaxPayload.TYPE, (payload, context) -> context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (!(player.containerMenu instanceof ChestLinkerConfigMenu menu)) return;

            ServerLevel level = player.level();
            BlockPos receiverPos = menu.getReceiverPos();

            int containerSlots = 27;
            BlockState blockState = level.getBlockState(receiverPos);
            BlockEntity blockEntity = level.getBlockEntity(receiverPos);
            if (blockEntity instanceof Container container) {
                containerSlots = container.getContainerSize();
                if (blockState.hasProperty(ChestBlock.TYPE) && blockState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                    containerSlots = 54;
                }
            }

            var filterOpt = LogisticsManager.getOrCreateReceiverConfig(level, receiverPos).getFilter(payload.slot());
            if (filterOpt.isEmpty()) return;

            int maxStack = new ItemStack(filterOpt.get().item()).getMaxStackSize();
            int maxCap = containerSlots * maxStack;
            int currentMax = filterOpt.get().maxCount();

            int newMax = currentMax == -1 ? maxCap : Math.clamp(currentMax + payload.delta(), 1, maxCap);
            LogisticsManager.setFilterMax(level, receiverPos, payload.slot(), newMax);
            menu.broadcastChanges();
        }));

        ServerPlayNetworking.registerGlobalReceiver(CycleFilterTypePayload.TYPE, (payload, context) -> context.server().execute(() -> {
            if (context.player().containerMenu instanceof ChestLinkerConfigMenu menu) {
                ServerLevel level = context.player().level();
                ChestSorterData data = LogisticsManager.getData(level);
                ReceiverConfig config = data.getOrCreateConfig(menu.getReceiverPos());

                config.cycleFilterType(payload.slot());
                data.setDirty();
                menu.broadcastChanges();
            }
        }));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int timeoutConfig = ChestSorterConfig.get().connectionTimeoutTicks;

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                LinkerState state = LinkerState.get(player);
                if (state.getSelectedSender() != null) {
                    boolean holdingLinker = player.getMainHandItem().is(ModItems.CHEST_LINKER) ||
                            player.getOffhandItem().is(ModItems.CHEST_LINKER);
                    boolean senderExists = player.level().getBlockState(state.getSelectedSender()).is(Blocks.CHEST);

                    state.incrementTicks();
                    boolean timedOut = timeoutConfig > 0 && state.getTicksInConnectionMode() >= timeoutConfig;

                    if (!holdingLinker || !senderExists || timedOut) {
                        LinkerState.clear(player);
                        String translationKey = timedOut ?
                                "chest_interaction.chestsorter.connection_timeout" :
                                "chest_interaction.chestsorter.connection_mode_off";

                        player.sendSystemMessage(Component.translatable(translationKey), true);
                    }
                }
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestAuthorityDataPayload.TYPE, (payload, context) -> context.server().execute(() -> {
            ServerPlayer player = context.player();

            if (!(player.containerMenu instanceof ChestLinkerConfigMenu menu)) return;

            BlockPos receiverPos = menu.getReceiverPos();

            if (!receiverPos.equals(payload.receiverPos())) return;
            if (LogisticsManager.hasAuthority(player.level(), receiverPos, player)) return;

            List<AuthorityDataPayload.PlayerData> players = new java.util.ArrayList<>();

            for (ServerPlayer target : player.level().getServer().getPlayerList().getPlayers()) {
                ReceiverConfig.ReceiverPermissions permissions =
                        LogisticsManager.getOrCreateReceiverConfig(player.level(), receiverPos)
                                .getPermissions(target.getUUID());

                if (LogisticsManager.canConfigureReceiver(player.level(), receiverPos, target)) {
                    permissions = ReceiverConfig.ReceiverPermissions.DEFAULT_OWNER;
                }

                players.add(new AuthorityDataPayload.PlayerData(
                        target.getUUID(),
                        target.getName().getString(),
                        permissions.gizmos(),
                        permissions.filter(),
                        permissions.settings(),
                        permissions.authority(),
                        permissions.connect()
                ));
            }

            ServerPlayNetworking.send(player, new AuthorityDataPayload(players));
        }));

        ServerPlayNetworking.registerGlobalReceiver(SetAuthorityPermissionPayload.TYPE, (payload, context) -> context.server().execute(() -> {
            ServerPlayer player = context.player();

            if (!(player.containerMenu instanceof ChestLinkerConfigMenu menu)) return;

            ServerLevel level = player.level();
            BlockPos receiverPos = menu.getReceiverPos();

            if (!receiverPos.equals(payload.receiverPos())) return;
            if (LogisticsManager.hasAuthority(level, receiverPos, player)) return;
            if (receiverPos.distToCenterSqr(player.position()) > 8.0 * 8.0) return;

            ServerPlayer target = player.level().getServer().getPlayerList().getPlayer(payload.targetPlayer());
            if (target == null) return;

            if (LogisticsManager.canConfigureReceiver(level, receiverPos, target)) {
                return;
            }

            AuthorityPermission[] permissions = AuthorityPermission.values();
            if (payload.permission() < 0 || payload.permission() >= permissions.length) return;

            LogisticsManager.setPermission(
                    level,
                    receiverPos,
                    target.getUUID(),
                    permissions[payload.permission()],
                    payload.enabled()
            );

            sendAuthorityData(player, receiverPos);
        }));

        LOGGER.info("Loading success.");
    }

    private static void sendAuthorityData(ServerPlayer player, BlockPos receiverPos) {
        ReceiverConfig config = LogisticsManager.getOrCreateReceiverConfig(player.level(), receiverPos);
        List<AuthorityDataPayload.PlayerData> players = new java.util.ArrayList<>();

        for (ServerPlayer target : player.level().getServer().getPlayerList().getPlayers()) {
            ReceiverConfig.ReceiverPermissions permissions = config.getPermissions(target.getUUID());

            if (LogisticsManager.canConfigureReceiver(player.level(), receiverPos, target)) {
                permissions = ReceiverConfig.ReceiverPermissions.DEFAULT_OWNER;
            }

            players.add(new AuthorityDataPayload.PlayerData(
                    target.getUUID(),
                    target.getName().getString(),
                    permissions.gizmos(),
                    permissions.filter(),
                    permissions.settings(),
                    permissions.authority(),
                    permissions.connect()
            ));
        }

        ServerPlayNetworking.send(player, new AuthorityDataPayload(players));
    }

}