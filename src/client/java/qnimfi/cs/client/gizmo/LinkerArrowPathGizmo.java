package qnimfi.cs.client.gizmo;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

public record LinkerArrowPathGizmo(Vec3 start, Vec3 end, int color) implements Gizmo {

    // Overload for BlockPos
    public LinkerArrowPathGizmo(BlockPos from, BlockPos to, int color) {
        this(Vec3.atCenterOf(from), Vec3.atCenterOf(to), color);
    }

    private static final float LINE_WIDTH = 2.0f;
    private static final double BASE_STEP_INTERVAL = 0.6;
    private static final double ARROW_BACK = 0.18;
    private static final double ARROW_WING = 0.18;

    @Override
    public void emit(@NonNull GizmoPrimitives gizmos, float alphaMultiplier) {
        int c = applyAlpha(color, alphaMultiplier);
        Vec3 vec = end.subtract(start);
        double totalDist = vec.length();
        if (totalDist < 0.1) return;

        Vec3 dir = vec.normalize();
        Vec3 camPos = Minecraft.getInstance().gameRenderer.mainCamera().position();
        Vec3 midPoint = start.add(vec.scale(0.5));
        double camDist = camPos.distanceTo(midPoint);
        double stepInterval = Math.max(BASE_STEP_INTERVAL, camDist * 0.04);

        for (double d = 0.7; d <= totalDist - 0.5; d += stepInterval) {
            Vec3 tip = start.add(dir.scale(d));
            Vec3 back = tip.subtract(dir.scale(ARROW_BACK));

            Vec3 toCam = camPos.subtract(tip);
            Vec3 right = dir.cross(toCam);
            if (right.lengthSqr() < 1e-5) {
                right = dir.cross(new Vec3(0, 1, 0));
            }
            right = right.normalize().scale(ARROW_WING);

            gizmos.addLine(tip, back.add(right), c, LINE_WIDTH);
            gizmos.addLine(tip, back.subtract(right), c, LINE_WIDTH);
        }
    }

    private static int applyAlpha(int color, float alphaMultiplier) {
        int a = (int) (((color >>> 24) & 0xFF) * alphaMultiplier);
        return (a << 24) | (color & 0x00FFFFFF);
    }
}