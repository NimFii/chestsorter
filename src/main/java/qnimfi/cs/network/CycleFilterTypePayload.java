package qnimfi.cs.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record CycleFilterTypePayload(int slot) implements CustomPacketPayload {
    public static final Type<CycleFilterTypePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("chestsorter", "cycle_filter_type"));

    public static final StreamCodec<FriendlyByteBuf, CycleFilterTypePayload> STREAM_CODEC = StreamCodec.of(
            (buf, value) -> buf.writeInt(value.slot),
            buf -> new CycleFilterTypePayload(buf.readInt())
    );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}