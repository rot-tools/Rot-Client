package fi.rotclient.rebuild.illegal;

import java.util.random.RandomGenerator;

/** Converts a per-tick rate into at most one click on each active tick. */
final class AutoClickerCadence {
    private static final int TICKS_PER_SECOND = 20;
    private int remainder;
    private boolean needsRelease = true;

    static int variedRate(int cps, int jitter, RandomGenerator random) {
        int rate = Math.clamp(cps, AutoClickerSettings.MIN_CPS, AutoClickerSettings.MAX_CPS);
        int spread = 2 * Math.clamp(jitter, 0, AutoClickerSettings.MAX_JITTER);
        int sample = spread == 0 ? 0 : random.nextInt(-spread, spread + 1);
        return Math.clamp(rate + sample, 1, AutoClickerSettings.MAX_CPS);
    }

    boolean active(boolean down) {
        if (!down) {
            needsRelease = false;
            clear();
        }
        return down && !needsRelease;
    }

    boolean tick(int rate) {
        remainder += rate;
        if (remainder < TICKS_PER_SECOND) return false;
        remainder -= TICKS_PER_SECOND;
        return true;
    }

    void clear() {
        remainder = 0;
    }

    void disarm() {
        needsRelease = true;
        clear();
    }
}
