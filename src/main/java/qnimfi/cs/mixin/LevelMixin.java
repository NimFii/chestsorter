package qnimfi.cs.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import qnimfi.cs.ChestUtil;
import qnimfi.cs.DirtyChestTracker;
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
        if (self.isClientSide() || !(self instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockState oldState = self.getBlockState(pos);

        if (!oldState.is(Blocks.CHEST)) {
            return;
        }

        if (!blockState.is(Blocks.CHEST)) {
            LogisticsManager.removeChest(serverLevel, pos.immutable());
            return;
        }

        // Handle double chest shrinking to single chest state transition
        if (oldState.hasProperty(ChestBlock.TYPE) && blockState.hasProperty(ChestBlock.TYPE)) {
            if (oldState.getValue(ChestBlock.TYPE) != ChestType.SINGLE && blockState.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
                DirtyChestTracker.markDirty(serverLevel, ChestUtil.canonicalPos(serverLevel, pos));
            }
        }
    }
}