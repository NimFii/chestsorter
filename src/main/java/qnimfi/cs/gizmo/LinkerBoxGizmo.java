package qnimfi.cs.gizmo;

import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.world.phys.Vec3;

public record LinkerBoxGizmo(BlockPos pos, int color) implements Gizmo {

    private static final double PAD = 0.02;
    private static final float LINE_WIDTH = 1.5f;

    @Override
    public void emit(GizmoPrimitives gizmos, float alphaMultiplier) {
        double minX = pos.getX() - PAD, minY = pos.getY() - PAD, minZ = pos.getZ() - PAD;
        double maxX = pos.getX() + 1 + PAD, maxY = pos.getY() + 1 + PAD, maxZ = pos.getZ() + 1 + PAD;

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
}