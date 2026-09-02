package qnimfi.cs.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import qnimfi.cs.ChestSorter;

public record SetFilterPayload(int slot, int itemId)
        implements CustomPacketPayload {

    public static final Type<SetFilterPayload> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath(
                            ChestSorter.MOD_ID,
                            "set_filter"
                    )
            );

    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            SetFilterPayload
            > CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    SetFilterPayload::slot,

                    ByteBufCodecs.VAR_INT,
                    SetFilterPayload::itemId,

                    SetFilterPayload::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}