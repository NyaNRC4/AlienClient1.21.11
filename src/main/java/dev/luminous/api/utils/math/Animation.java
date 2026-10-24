package dev.luminous.api.utils.math;

public class Animation {
    private final FadeUtils fadeUtils = new FadeUtils(0);
    public double from = 0;
    public double to = 0;

    public double get(double target, long length, Easing ease) {
        if (target != to) {
            from = from + (to - from) * fadeUtils.ease(ease);
            to = target;
            fadeUtils.reset();
        }
        fadeUtils.setLength(length);
        return from + (to - from) * fadeUtils.ease(ease);
    }
}
