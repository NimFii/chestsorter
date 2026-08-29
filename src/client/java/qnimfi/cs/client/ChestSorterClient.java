package qnimfi.cs.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;
import qnimfi.cs.menu.ModMenuTypes;
import qnimfi.cs.network.LinkerSyncPayload;

public class ChestSorterClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenuTypes.CHEST_LINKER_CONFIG, ChestLinkerConfigScreen::new);

		ClientPlayNetworking.registerGlobalReceiver(
				LinkerSyncPayload.TYPE,
				(payload, context) -> context.client().execute(
						() -> ClientLinkerRenderData.update(payload.nodes())
				)
		);

		LinkerGizmoEmitter.initialize();
	}
}