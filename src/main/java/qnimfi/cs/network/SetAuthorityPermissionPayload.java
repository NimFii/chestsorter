package qnimfi.cs.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import qnimfi.cs.ChestSorter;

import java.util.UUID;

public record SetAuthorityPermissionPayload(
        BlockPos receiverPos,
        UUID targetPlayer,
        int permission,
        boolean enabled
) implements CustomPacketPayload {

    public static final Type<SetAuthorityPermissionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ChestSorter.MOD_ID, "set_authority_permission"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetAuthorityPermissionPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetAuthorityPermissionPayload::receiverPos,
                    net.minecraft.core.UUIDUtil.STREAM_CODEC, SetAuthorityPermissionPayload::targetPlayer,
                    ByteBufCodecs.VAR_INT, SetAuthorityPermissionPayload::permission,
                    ByteBufCodecs.BOOL, SetAuthorityPermissionPayload::enabled,
                    SetAuthorityPermissionPayload::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}