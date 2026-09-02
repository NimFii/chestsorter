package qnimfi.cs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChestSorterData extends SavedData {

    public final Map<BlockPos, LogisticsNode> nodes = new HashMap<>();
    public final Map<BlockPos, ReceiverConfig> receiverConfigs = new HashMap<>();

    private record SerialForm(List<LogisticsNode> nodes, List<ReceiverConfig> configs) {
        static final Codec<SerialForm> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LogisticsNode.CODEC.listOf().fieldOf("nodes").forGetter(SerialForm::nodes),
                ReceiverConfig.CODEC.listOf().fieldOf("receiver_configs").forGetter(SerialForm::configs)
        ).apply(instance, SerialForm::new));
    }

    public static final Codec<ChestSorterData> CODEC = SerialForm.CODEC.xmap(
            serial -> {
                ChestSorterData data = new ChestSorterData();
                for (LogisticsNode node : serial.nodes()) {
                    data.nodes.put(node.getPosition(), node);
                }
                for (ReceiverConfig config : serial.configs()) {
                    data.receiverConfigs.put(config.getPosition(), config);
                }
                return data;
            },
            data -> new SerialForm(
                    data.nodes.values().stream().toList(),
                    data.receiverConfigs.values().stream().toList()
            )
    );

    public static final SavedDataType<ChestSorterData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(ChestSorter.MOD_ID, "chest_sorter"),
            ChestSorterData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    public ChestSorterData() {}

    public void addNode(LogisticsNode node) {
        nodes.put(node.getPosition(), node);
        setDirty();
    }

    public LogisticsNode getNode(BlockPos position) {
        return nodes.get(position);
    }

    public void removeNode(BlockPos position) {
        if (nodes.remove(position) != null) {
            setDirty();
        }
    }

    public ReceiverConfig getOrCreateConfig(BlockPos position) {
        return receiverConfigs.computeIfAbsent(position, p -> {
            setDirty();
            return new ReceiverConfig(p);
        });
    }

    public void removeConfig(BlockPos position) {
        if (receiverConfigs.remove(position) != null) {
            setDirty();
        }
    }
}