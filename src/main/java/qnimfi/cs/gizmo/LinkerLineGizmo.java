package qnimfi.cs.gizmo;

import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.world.phys.Vec3;

public record LinkerLineGizmo(BlockPos from, BlockPos to, int color) implements Gizmo {

    private static final float LINE_WIDTH = 2.0f;

    @Override
    public void emit(GizmoPrimitives gizmos, float alphaMultiplier) {
        int c = applyAlpha(color, alphaMultiplier);
        gizmos.addLine(Vec3.atCenterOf(from), Vec3.atCenterOf(to), c, LINE_WIDTH);
    }

    private static int applyAlpha(int color, float alphaMultiplier) {
        int a = (int) (((color >>> 24) & 0xFF) * alphaMultiplier);
        return (a << 24) | (color & 0x00FFFFFF);
    }
}