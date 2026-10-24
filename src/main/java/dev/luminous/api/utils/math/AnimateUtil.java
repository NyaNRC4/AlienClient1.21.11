package dev.luminous.api.utils.math;

import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;

public class AnimateUtil implements Wrapper {
    public static float deltaTime() {
        return Alien.FPS.getFps() > 5 ? (1f / Alien.FPS.getFps()) : 0.016f;
    }

    public static float fast(float end, float start, float multiple) {
        float clampedDelta = MathUtil.clamp(deltaTime() * multiple, 0f, 1f);
        return (1f - clampedDelta) * end + clampedDelta * start;
    }

    public static double animate(double current, double endPoint, double speed) {
        if (speed >= 1) return endPoint;
        if (speed == 0) return current;

        boolean shouldContinueAnimation = endPoint > current;

        double dif = Math.max(endPoint, current) - Math.min(endPoint, current);
        if (Math.abs(dif) <= 0.001) return endPoint;
        double factor = dif * speed;
        return current + (shouldContinueAnimation ? factor : -factor);
    }
}
