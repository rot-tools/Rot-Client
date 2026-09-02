package fi.rotclient;

import java.util.ArrayDeque;
import java.util.Deque;

/** Reports only synthetic Auto Clicker input from the last rolling second. */
public final class AutoClickerCpsMeter {
    private static final long WINDOW_MILLIS = 1_000L;
    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();

    public void recordLeft(long nowMillis) { record(leftClicks, nowMillis); }
    public void recordRight(long nowMillis) { record(rightClicks, nowMillis); }

    public Snapshot snapshot(long nowMillis) {
        prune(leftClicks, nowMillis);
        prune(rightClicks, nowMillis);
        return new Snapshot(leftClicks.size(), rightClicks.size());
    }

    private static void record(Deque<Long> clicks, long nowMillis) {
        clicks.addLast(nowMillis);
        prune(clicks, nowMillis);
    }

    private static void prune(Deque<Long> clicks, long nowMillis) {
        long oldestAllowed = nowMillis - WINDOW_MILLIS;
        while (!clicks.isEmpty() && clicks.peekFirst() <= oldestAllowed) {
            clicks.removeFirst();
        }
    }

    public record Snapshot(int leftCps, int rightCps) {
        public int totalCps() { return leftCps + rightCps; }
    }
}
