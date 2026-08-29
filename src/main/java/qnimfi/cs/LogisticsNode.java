package qnimfi.cs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LogisticsNode {

    public static final Codec<LogisticsNode> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    BlockPos.CODEC.fieldOf("position")
                            .forGetter(LogisticsNode::getPosition),

                    Codec.STRING.xmap(
                                    UUID::fromString,
                                    UUID::toString
                            ).fieldOf("owner")
                            .forGetter(LogisticsNode::getOwner),

                    BlockPos.CODEC.listOf()
                            .fieldOf("receivers")
                            .forGetter(node -> node.getReceivers().stream().toList())

            ).apply(instance, (position, ownerString, receivers) -> {

                LogisticsNode node = new LogisticsNode(
                        position,
                        ownerString
                );

                node.receivers.addAll(receivers);

                return node;
            }));

    private final BlockPos position;
    private final UUID owner;

    private final Set<BlockPos> receivers = new HashSet<>();

    public LogisticsNode(BlockPos position, UUID owner) {
        this.position = position;
        this.owner = owner;
    }

    public BlockPos getPosition() {
        return position;
    }

    public UUID getOwner() {
        return owner;
    }

    public Set<BlockPos> getReceivers() {
        return receivers;
    }

    public boolean addReceiver(BlockPos receiver) {
        return receivers.add(receiver);
    }

    public boolean removeReceiver(BlockPos receiver) {
        return receivers.remove(receiver);
    }

    public boolean hasReceiver(BlockPos receiver) {
        return receivers.contains(receiver);
    }
}