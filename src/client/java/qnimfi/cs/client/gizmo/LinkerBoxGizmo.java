package qnimfi.cs.client.gizmo;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

public record LinkerBoxGizmo(BlockPos pos, int color) implements Gizmo {

    private static final double PAD = 0.02;
    private static final float LINE_WIDTH = 3.0f;

    @Override
    public void emit(@NonNull GizmoPrimitives gizmos, float alphaMultiplier) {
        Level level = Minecraft.getInstance().level;
        AABB box = getChestBounds(level, pos);

        double minX = box.minX - PAD, minY = box.minY - PAD, minZ = box.minZ - PAD;
        double maxX = box.maxX + PAD, maxY = box.maxY + PAD, maxZ = box.maxZ + PAD;

        int c = applyAlpha(color, alphaMultiplier);

        Vec3[] bottom = {
                new Vec3(minX, minY, minZ), new Vec3(maxX, minY, minZ),
                new Vec3(maxX, minY, maxZ), new Vec3(minX, minY, maxZ)
        };
        Vec3[] top = {
                new Vec3(minX, maxY, minZ), new Vec3(maxX, maxY, minZ),
                new Vec3(maxX, maxY, maxZ), new Vec3(minX, maxY, maxZ)
        };

        for (int i = 0; i < 4; i++) {
            int next = (i + 1) % 4;
            gizmos.addLine(bottom[i], bottom[next], c, LINE_WIDTH);
            gizmos.addLine(top[i], top[next], c, LINE_WIDTH);
            gizmos.addLine(bottom[i], top[i], c, LINE_WIDTH);
        }
    }

    private static int applyAlpha(int color, float alphaMultiplier) {
        int a = (int) (((color >>> 24) & 0xFF) * alphaMultiplier);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    // Adjusts gizmo depending on double chest or single block
    public static AABB getChestBounds(Level level, BlockPos pos) {
        if (level != null) {
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(ChestBlock.TYPE)) {
                ChestType type = state.getValue(ChestBlock.TYPE);
                if (type != ChestType.SINGLE) {
                    BlockPos companionPos = pos.relative(ChestBlock.getConnectedDirection(state));
                    AABB box1 = new AABB(pos);
                    AABB box2 = new AABB(companionPos);
                    return new AABB(
                            Math.min(box1.minX, box2.minX),
                            Math.min(box1.minY, box2.minY),
                            Math.min(box1.minZ, box2.minZ),
                            Math.max(box1.maxX, box2.maxX),
                            Math.max(box1.maxY, box2.maxY),
                            Math.max(box1.maxZ, box2.maxZ)
                    );
                }
            }
        }
        return new AABB(pos);
    }
}