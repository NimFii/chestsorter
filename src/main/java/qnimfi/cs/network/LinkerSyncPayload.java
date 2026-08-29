package qnimfi.cs.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import qnimfi.cs.ChestSorter;

import java.util.List;

public record LinkerSyncPayload(List<NodeLinkData> nodes) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LinkerSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(ChestSorter.MOD_ID, "linker_sync")
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkerSyncPayload> CODEC =
            StreamCodec.composite(
                    NodeLinkData.CODEC.apply(ByteBufCodecs.list()), LinkerSyncPayload::nodes,
                    LinkerSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}