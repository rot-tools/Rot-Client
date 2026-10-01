package fi.rotclient;

import java.util.Locale;
import java.util.OptionalDouble;
import java.util.regex.Pattern;

/** Observation-only cooldown math. A configured fallback is always labelled estimated. */
final class PickobulusPolicy {
    private static final Pattern COOLDOWN = Pattern.compile("(?i)^Cooldown:\\s*(\\d+(?:\\.\\d+)?)\\s*(s|seconds?|m|minutes?)$");
    static boolean used(String message) {
        String text = CommissionDisplayPolicy.normalizeLine(message);
        return text.matches("(?i)^You used your Pickobulus Pickaxe Ability[!.]*$");
    }
    static boolean ready(String message) {
        String text = CommissionDisplayPolicy.normalizeLine(message).toLowerCase(Locale.ROOT);
        return text.matches("(?:your )?pickobulus(?: pickaxe ability)? (?:is (?:now )?)?(?:ready|available)(?: again)?[!.]*");
    }
    static OptionalDouble seconds(String text) {
        var match = COOLDOWN.matcher(CommissionDisplayPolicy.normalizeLine(text));
        if (!match.matches()) return OptionalDouble.empty();
        double seconds = Double.parseDouble(match.group(1)) * (match.group(2).toLowerCase(Locale.ROOT).startsWith("m") ? 60 : 1);
        return seconds > 0 && seconds <= 600 ? OptionalDouble.of(seconds) : OptionalDouble.empty();
    }
    static final class Timer {
        private long readyAt = -1;
        private boolean authoritative;
        private boolean notified;
        void start(long now, double seconds, boolean authoritative) {
            if (now < 0 || !Double.isFinite(seconds) || seconds <= 0 || seconds > 600) return;
            readyAt = now + (long) Math.ceil(seconds * 1_000);
            this.authoritative = authoritative;
            notified = false;
        }
        void confirmReady(long now) { readyAt = now; authoritative = true; }
        long remaining(long now) { return readyAt < 0 ? -1 : Math.max(0, readyAt - now); }
        boolean takeReadyNotification(long now) {
            if (readyAt < 0 || now < readyAt || notified) return false;
            notified = true; return true;
        }
        boolean authoritative() { return authoritative; }
        String label(long now) {
            long remaining = remaining(now);
            if (remaining < 0) return "Pickobulus: waiting for server status";
            String status = remaining == 0 ? "READY" : String.format(Locale.ROOT, "%.1fs", remaining / 1_000.0);
            return "Pickobulus: " + status + (authoritative ? "" : " (estimated)");
        }
    }
    private PickobulusPolicy() {}
}
