package qnimfi.cs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

public class ChestUtil {

    /**
     * If the chest at `pos` is one half of a double chest, returns the
     * same canonical BlockPos regardless of which half was clicked. For a
     * single chest, just returns pos unchanged.
     */
    public static BlockPos canonicalPos(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof ChestBlock)) {
            return pos;
        }

        ChestType type = state.getValue(ChestBlock.TYPE);

        if (type == ChestType.SINGLE) {
            return pos;
        }

        Direction toOtherHalf = ChestBlock.getConnectedDirection(state);
        BlockPos otherPos = pos.relative(toOtherHalf);

        // Arbitrary but consistent total order — as long as both halves
        // apply the same rule, they always resolve to the same position.
        return pos.asLong() < otherPos.asLong() ? pos : otherPos;
    }
}