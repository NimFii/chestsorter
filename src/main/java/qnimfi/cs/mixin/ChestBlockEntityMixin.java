package qnimfi.cs.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qnimfi.cs.ChestUtil;
import qnimfi.cs.DirtyChestTracker;

@Mixin(BlockEntity.class)
public abstract class ChestBlockEntityMixin {

    @Inject(method = "setChanged*", at = @At("HEAD"))
    private void chestsorter$onSetChanged(CallbackInfo ci) {
        BlockEntity self = (BlockEntity) (Object) this;

        if (!(self instanceof ChestBlockEntity chest)) {
            return;
        }

        Level level = chest.getLevel();
        if (level == null || level.isClientSide()) return;

        var canonicalPos = ChestUtil.canonicalPos(level, chest.getBlockPos());
        DirtyChestTracker.markDirty(level, canonicalPos);
    }
}