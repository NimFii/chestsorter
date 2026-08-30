package qnimfi.cs;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qnimfi.cs.item.ModItems;
import qnimfi.cs.menu.ChestLinkerConfigMenu;
import qnimfi.cs.menu.ModMenuTypes;
import qnimfi.cs.network.AdjustFilterMaxPayload;
import qnimfi.cs.network.LinkerSyncPayload;
import qnimfi.cs.network.SetFilterPayload;

public class ChestSorter implements ModInitializer {
	public static final String MOD_ID = "chestsorter";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.initialize();
		ChestInteractionHandler.initialize();

		ChestSorterConfig.get();
		ModMenuTypes.initialize();

		ItemTransferHandler.initialize();

		LinkerSyncHandler.initialize();
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			// Clear state when player logs out
			LinkerState.clear(handler.player);
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			// Clear state on dimension travel / respawn
			LinkerState.clear(newPlayer);
		});

		PayloadTypeRegistry.clientboundPlay().register(LinkerSyncPayload.TYPE, LinkerSyncPayload.CODEC);
		//PayloadTypeRegistry.serverboundPlay().register(LinkerScrollPayload.TYPE, LinkerScrollPayload.CODEC);

		//ServerPlayNetworking.registerGlobalReceiver(LinkerScrollPayload.TYPE, (payload, context) -> {
		//	context.server().execute(() -> {
		//		ServerPlayer player = context.player();

		//		if (!player.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).is(ModItems.CHEST_LINKER)) {
		//			return;
		//		}

		//		LinkerState state = LinkerState.get(player);
		//		state.cycleMode(payload.direction());

		//		player.sendSystemMessage(
		//				Component.literal("Linker mode: " + state.getMode()),
		//				true
		//		);
		//	});
		//});

		PayloadTypeRegistry.serverboundPlay().register(
				SetFilterPayload.TYPE,
				SetFilterPayload.CODEC
		);

		ServerPlayNetworking.registerGlobalReceiver(
				SetFilterPayload.TYPE,
				(payload, context) -> {

					context.server().execute(() -> {

						ServerPlayer player =
								context.player();

						/*
						 * Make sure the player actually has
						 * our configuration menu open.
						 */
						if (!(player.containerMenu
								instanceof ChestLinkerConfigMenu menu)) {

							return;
						}

						/*
						 * Validate slot.
						 */
						if (payload.slot() < 0 ||
								payload.slot() >= menu.getSlotCount()) {

							return;
						}

						BlockPos receiverPos =
								menu.getReceiverPos();

						/*
						 * Validate permission.
						 */
						if (!LogisticsManager.canConfigureReceiver(
                                player.level(),
								receiverPos,
								player
						)) {

							return;
						}

						/*
						 * Validate distance.
						 *
						 * 8 blocks is plenty for the configuration GUI.
						 */
						if (receiverPos.distToCenterSqr(player.position()) > 8.0 * 8.0) {

							return;
						}

						/*
						 * =================================================
						 * CLEAR FILTER
						 * =================================================
						 */
						if (payload.itemId() < 0) {

							LogisticsManager.setFilterItem(
                                    player.level(),
									receiverPos,
									payload.slot(),
									Items.AIR
							);

							return;
						}

						/*
						 * =================================================
						 * SET FILTER
						 * =================================================
						 */

						if (payload.itemId() >=
								BuiltInRegistries.ITEM.size()) {

							return;
						}

						var item =
								BuiltInRegistries.ITEM.byId(
										payload.itemId()
								);

						if (item == Items.AIR) {

							return;
						}

						ItemStack carried =
								menu.getCarried();

						if (carried.isEmpty() ||
								carried.getItem() != item) {

							return;
						}

						LogisticsManager.setFilterItem(
                                player.level(),
								receiverPos,
								payload.slot(),
								item
						);
					});
				});

		PayloadTypeRegistry.serverboundPlay().register(
				AdjustFilterMaxPayload.TYPE,
				AdjustFilterMaxPayload.CODEC
		);

		ServerPlayNetworking.registerGlobalReceiver(AdjustFilterMaxPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();

			// Ensure the player actually has the config menu open
			if (player.containerMenu instanceof ChestLinkerConfigMenu menu) {
				ReceiverConfig config = LogisticsManager.getOrCreateReceiverConfig(player.level(), menu.getReceiverPos());

				// Update the filter limit on the server
				config.getFilter(payload.slot()).ifPresent(entry -> {
					int current = entry.maxCount();
					// Clamp the value to a minimum of 1
					int updated = Math.max(1, current + payload.delta());
					config.setFilterMax(payload.slot(), updated);
				});
			}
		});

		LOGGER.info("Loading success.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}