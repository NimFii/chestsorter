package qnimfi.cs;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LinkerScrollPayload(int direction) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LinkerScrollPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(ChestSorter.MOD_ID, "linker_scroll")
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkerScrollPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, LinkerScrollPayload::direction,
                    LinkerScrollPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}