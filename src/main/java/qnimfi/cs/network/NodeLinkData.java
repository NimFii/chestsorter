package qnimfi.cs.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public record NodeLinkData(BlockPos sender, List<BlockPos> receivers) {

    public static final StreamCodec<RegistryFriendlyByteBuf, NodeLinkData> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, NodeLinkData::sender,
                    BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), NodeLinkData::receivers,
                    NodeLinkData::new
            );
}