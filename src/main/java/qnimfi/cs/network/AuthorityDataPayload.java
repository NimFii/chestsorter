package qnimfi.cs.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import qnimfi.cs.ChestSorter;

import java.util.List;
import java.util.UUID;

public record AuthorityDataPayload(List<PlayerData> players) implements CustomPacketPayload {

    public record PlayerData(
            UUID uuid,
            String name,
            boolean gizmos,
            boolean filter,
            boolean settings,
            boolean authority,
            boolean connect
    ) {
    }

    public static final Type<AuthorityDataPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ChestSorter.MOD_ID, "authority_data"));

    private static final StreamCodec<RegistryFriendlyByteBuf, PlayerData> PLAYER_CODEC =
            StreamCodec.composite(
                    net.minecraft.core.UUIDUtil.STREAM_CODEC, PlayerData::uuid,
                    ByteBufCodecs.STRING_UTF8, PlayerData::name,
                    ByteBufCodecs.BOOL, PlayerData::gizmos,
                    ByteBufCodecs.BOOL, PlayerData::filter,
                    ByteBufCodecs.BOOL, PlayerData::settings,
                    ByteBufCodecs.BOOL, PlayerData::authority,
                    ByteBufCodecs.BOOL, PlayerData::connect,
                    PlayerData::new
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, AuthorityDataPayload> CODEC =
            PLAYER_CODEC.apply(ByteBufCodecs.list()).map(
                    AuthorityDataPayload::new,
                    AuthorityDataPayload::players
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}