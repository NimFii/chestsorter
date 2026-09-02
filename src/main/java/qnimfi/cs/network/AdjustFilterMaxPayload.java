package qnimfi.cs.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import qnimfi.cs.ChestSorter;

public record AdjustFilterMaxPayload(int slot, int delta) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AdjustFilterMaxPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ChestSorter.MOD_ID, "adjust_filter_max"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdjustFilterMaxPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, AdjustFilterMaxPayload::slot,
                    ByteBufCodecs.VAR_INT, AdjustFilterMaxPayload::delta,
                    AdjustFilterMaxPayload::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}