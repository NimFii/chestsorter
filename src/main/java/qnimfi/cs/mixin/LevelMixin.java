package qnimfi.cs.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qnimfi.cs.LogisticsManager;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            at = @At("HEAD"))
    private void chestsorter$onSetBlock(
            BlockPos pos,
            BlockState blockState,
            int updateFlags,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Level self = (Level) (Object) this;

        if (self.isClientSide()) {
            return;
        }

        if (!(self instanceof ServerLevel serverLevel)) {
            return;
        }

        // Read the OLD state before it's overwritten.
        BlockState oldState = self.getBlockState(pos);

        // Only care about a chest actually disappearing.
        if (!oldState.is(Blocks.CHEST)) {
            return;
        }

        if (blockState.is(Blocks.CHEST)) {
            // Still a chest (e.g. a state-only update), nothing to clean up.
            return;
        }

        LogisticsManager.removeChest(serverLevel, pos.immutable());
    }
}