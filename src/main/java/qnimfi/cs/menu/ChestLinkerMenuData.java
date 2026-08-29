package qnimfi.cs.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ChestLinkerMenuData(BlockPos receiverPos, int slotCount) {
    public static final StreamCodec<RegistryFriendlyByteBuf, ChestLinkerMenuData> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ChestLinkerMenuData::receiverPos,
                    ByteBufCodecs.VAR_INT, ChestLinkerMenuData::slotCount,
                    ChestLinkerMenuData::new
            );
}