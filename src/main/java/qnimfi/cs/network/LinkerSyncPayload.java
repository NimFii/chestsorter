package qnimfi.cs.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.NonNull;
import qnimfi.cs.ChestSorter;
import qnimfi.cs.FilterType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record LinkerSyncPayload(
        List<NodeLinkData> nodes,
        Optional<BlockPos> activeSender,
        Map<BlockPos, List<ClientFilterEntry>> receiverFilters
) implements CustomPacketPayload {

    public record ClientFilterEntry(Item item, FilterType type) {
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientFilterEntry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(BuiltInRegistries.ITEM.key()), ClientFilterEntry::item,
                ByteBufCodecs.STRING_UTF8.map(FilterType::valueOf, Enum::name), ClientFilterEntry::type,
                ClientFilterEntry::new
        );
    }

    public static final CustomPacketPayload.Type<LinkerSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ChestSorter.MOD_ID, "linker_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkerSyncPayload> CODEC =
            StreamCodec.composite(
                    NodeLinkData.CODEC.apply(ByteBufCodecs.list()), LinkerSyncPayload::nodes,
                    ByteBufCodecs.optional(BlockPos.STREAM_CODEC), LinkerSyncPayload::activeSender,
                    ByteBufCodecs.map(
                            java.util.HashMap::new,
                            BlockPos.STREAM_CODEC,
                            ClientFilterEntry.STREAM_CODEC.apply(ByteBufCodecs.list())
                    ), LinkerSyncPayload::receiverFilters,
                    LinkerSyncPayload::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}