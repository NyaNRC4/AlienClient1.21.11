package dev.luminous.api.utils.math;

public class Timer {

    private long startTime = -1L;

    public Timer() {
        reset();
    }

    public void reset() {
        startTime = System.currentTimeMillis();
    }

    public long getMs() {
        return System.currentTimeMillis() - startTime;
    }

    public boolean passedS(double s) {
        return passed((long) s * 1000L);
    }

    public boolean passedMs(double ms) {
        return passed((long) ms);
    }

    public void setMs(long ms) {
        startTime = System.currentTimeMillis() - ms;
    }

    public boolean passed(long ms) {
        return System.currentTimeMillis() - startTime >= ms;
    }
}
