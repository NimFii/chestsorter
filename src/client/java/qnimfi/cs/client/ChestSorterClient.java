package qnimfi.cs.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import qnimfi.cs.client.gizmo.ClientGizmoManager;
import qnimfi.cs.client.gizmo.ClientGizmoRenderer;
import qnimfi.cs.client.gizmo.ClientLinkerHudRenderer;
import qnimfi.cs.item.ModItems;
import qnimfi.cs.menu.ModMenuTypes;
import qnimfi.cs.network.AuthorityDataPayload;

public class ChestSorterClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		MenuScreens.register(ModMenuTypes.CHEST_LINKER_CONFIG, ChestLinkerConfigScreen::new);

		ItemTooltipCallback.EVENT.register((stack, _, _, tooltip) -> {
			if (stack.is(ModItems.CHEST_LINKER)) {
				tooltip.add(Component.translatable("tooltip.chestsorter.chest_linker.line1"));
				tooltip.add(Component.translatable("tooltip.chestsorter.chest_linker.line2"));
			}
		});

		ClientGizmoManager.initialize();
		ClientGizmoRenderer.initialize();
		ClientLinkerHudRenderer.initialize();

		ClientPlayNetworking.registerGlobalReceiver(AuthorityDataPayload.TYPE, (payload, context) -> context.client().execute(() -> {
            if (context.client().gui.screen() instanceof ChestLinkerConfigScreen configScreen) {
                configScreen.setAuthorityPlayers(payload.players());
            }
        }));


	}
}