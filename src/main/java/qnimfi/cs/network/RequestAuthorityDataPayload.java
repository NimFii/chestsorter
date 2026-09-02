package qnimfi.cs.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import qnimfi.cs.ChestSorter;

public record RequestAuthorityDataPayload(BlockPos receiverPos) implements CustomPacketPayload {

    public static final Type<RequestAuthorityDataPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ChestSorter.MOD_ID, "request_authority_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestAuthorityDataPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    RequestAuthorityDataPayload::receiverPos,
                    RequestAuthorityDataPayload::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}