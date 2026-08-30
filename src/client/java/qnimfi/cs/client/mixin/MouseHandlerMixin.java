package qnimfi.cs.client.mixin;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qnimfi.cs.item.ModItems;
import qnimfi.cs.LinkerScrollPayload;

// THIS MIXIN IS ARCHIVED
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void chestsorter$onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || !player.isCrouching()) {
            return;
        }

        if (!player.getItemInHand(InteractionHand.MAIN_HAND).is(ModItems.CHEST_LINKER)) {
            return;
        }

        // Stop vanilla from changing the hotbar slot.
        ci.cancel();

        if (yOffset == 0) {
            return;
        }

        int direction = yOffset > 0 ? 1 : -1;
        ClientPlayNetworking.send(new LinkerScrollPayload(direction));
    }
}