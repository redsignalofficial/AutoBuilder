package dev.autobuilder.util;

import net.minecraft.util.math.MathHelper;

public final class MathUtils {
    private MathUtils() {}

    public static float wrapDegrees(float degrees) {
        return MathHelper.wrapDegrees(degrees);
    }

    public static float approachDegrees(float current, float target, float step) {
        float diff = wrapDegrees(target - current);
        if (Math.abs(diff) <= step) {
            return target;
        }
        return current + Math.signum(diff) * step;
    }

    public static double distanceSq(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(distanceSq(x1, y1, z1, x2, y2, z2));
    }

    public static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    public static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
