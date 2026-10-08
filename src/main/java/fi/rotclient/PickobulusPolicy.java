package fi.rotclient;

import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.regex.Pattern;

/** Observation-only cooldown math. A configured fallback is always labelled estimated. */
final class PickobulusPolicy {
    static final long SERVER_COUNTDOWN_FRESH_MILLIS = 3_000L;
    static final long USE_DEDUP_MILLIS = 250L;
    static final long ACCEPTED_BREAK_WINDOW_MILLIS = 5_000L;
    private static final Pattern COOLDOWN = Pattern.compile("(?i)^Cooldown:\\s*(\\d+(?:\\.\\d+)?)\\s*(s|seconds?|m|minutes?)$");
    private static final Pattern ABILITY_HEADER = Pattern.compile("(?i)^(?:⦾\\s*)?(?:Pickaxe )?Ability:\\s*(.+)$");
    private static final Pattern PICKOBULUS_HEADER = Pattern.compile(
            "(?i)^(?:⦾\\s*)?(?:Pickaxe )?Ability:\\s*Pickobulus(?:\\s+RIGHT CLICK)?$");
    private static final Pattern TAB_STATUS = Pattern.compile("(?i)^Pickobulus:\\s*(.+)$");
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
        try {
            double seconds = Double.parseDouble(match.group(1)) * (match.group(2).toLowerCase(Locale.ROOT).startsWith("m") ? 60 : 1);
            return Double.isFinite(seconds) && seconds > 0 && seconds <= 600
                    ? OptionalDouble.of(seconds) : OptionalDouble.empty();
        } catch (NumberFormatException ignored) {
            return OptionalDouble.empty();
        }
    }
    static boolean hasPickobulusAbility(List<String> lore) {
        return lore != null && lore.stream().limit(128).anyMatch(line ->
                PICKOBULUS_HEADER.matcher(CommissionDisplayPolicy.normalizeLine(line)).matches());
    }
    static OptionalDouble loreCooldown(List<String> lore) {
        if (lore == null) return OptionalDouble.empty();
        boolean withinPickobulus = false;
        int headerCount = 0, cooldownCount = 0, linesInBlock = 0;
        double result = 0;
        for (String raw : lore.stream().limit(128).toList()) {
            String line = CommissionDisplayPolicy.normalizeLine(raw);
            if (ABILITY_HEADER.matcher(line).matches()) {
                withinPickobulus = PICKOBULUS_HEADER.matcher(line).matches();
                if (withinPickobulus) headerCount++;
                linesInBlock = 0;
                continue;
            }
            if (!withinPickobulus || ++linesInBlock > 24) continue;
            var parsed = seconds(line);
            if (parsed.isPresent()) { result = parsed.getAsDouble(); cooldownCount++; }
        }
        return headerCount == 1 && cooldownCount == 1 ? OptionalDouble.of(result) : OptionalDouble.empty();
    }
    static Optional<String> tabStatus(List<String> lines) {
        if (lines == null) return Optional.empty();
        String status = null;
        for (String line : lines.stream().limit(512).toList()) {
            var matcher = TAB_STATUS.matcher(CommissionDisplayPolicy.normalizeLine(line));
            if (!matcher.matches()) continue;
            String candidate = statusKey(matcher.group(1));
            if (!isReadyStatus(candidate) && seconds("Cooldown: " + candidate).isEmpty()) continue;
            if (status != null && !status.equals(candidate)) return Optional.empty();
            status = candidate;
        }
        return Optional.ofNullable(status);
    }
    private static String statusKey(String text) {
        return CommissionDisplayPolicy.normalizeLine(text).toLowerCase(Locale.ROOT);
    }
    private static boolean isReadyStatus(String text) {
        return text.equals("ready") || text.equals("available");
    }
    private static long deadline(long now, long duration) {
        if (now < 0 || duration < 0) return -1;
        try { return Math.addExact(now, duration); }
        catch (ArithmeticException ignored) { return -1; }
    }
    static final class Timer {
        private long readyAt = -1;
        private long lastClock = -1, serverCountdownAt = -1;
        private boolean readyConfirmed, serverCountdown, awaitingCooldownProof;
        private boolean notificationArmed, notified = true;
        private String lastTabStatus = "";
        boolean start(long now, double seconds, boolean authoritative) {
            if (now < 0 || !Double.isFinite(seconds) || seconds <= 0 || seconds > 600) return false;
            long deadline = deadline(now, (long) Math.ceil(seconds * 1_000));
            if (deadline < 0 || !clock(now)) return false;
            readyAt = deadline;
            readyConfirmed = false;
            serverCountdown = authoritative;
            serverCountdownAt = authoritative ? now : -1;
            awaitingCooldownProof = !authoritative;
            notificationArmed = true;
            notified = false;
            return true;
        }
        boolean startUse(long now, double seconds, String baselineStatus) {
            if (!start(now, seconds, false)) return false;
            lastTabStatus = statusKey(baselineStatus);
            return true;
        }
        boolean observeTabStatus(String rawStatus, long now) {
            if (!clock(now)) return false;
            String status = statusKey(rawStatus);
            if (status.equals(lastTabStatus)) return false;
            if (isReadyStatus(status)) {
                lastTabStatus = status;
                // READY cached before a just-observed use is not a completion event.
                if (awaitingCooldownProof) return false;
                confirmReady(now);
                return true;
            }
            var seconds = seconds("Cooldown: " + status);
            if (seconds.isEmpty()) return false;
            long deadline = deadline(now, (long) Math.ceil(seconds.getAsDouble() * 1_000));
            if (deadline < 0) return false;
            lastTabStatus = status;
            if (readyAt < 0 || readyConfirmed) {
                notificationArmed = true;
                notified = false;
            }
            readyAt = deadline;
            readyConfirmed = false;
            serverCountdown = true;
            serverCountdownAt = now;
            awaitingCooldownProof = false;
            return true;
        }
        void confirmReady(long now) {
            if (!clock(now)) return;
            readyAt = now;
            readyConfirmed = true;
            serverCountdown = false;
            awaitingCooldownProof = false;
        }
        long remaining(long now) {
            if (!clock(now) || readyAt < 0) return -1;
            return Math.max(0, readyAt - now);
        }
        boolean takeReadyNotification(long now) {
            if (remaining(now) != 0 || !notificationArmed || notified) return false;
            notified = true; return true;
        }
        boolean authoritative() {
            return readyConfirmed || serverCountdown && readyAt > lastClock && serverCountdownAt >= 0
                    && lastClock >= serverCountdownAt && lastClock - serverCountdownAt <= SERVER_COUNTDOWN_FRESH_MILLIS;
        }
        String label(long now) {
            long remaining = remaining(now);
            if (remaining < 0) return "Pickobulus: waiting for server status";
            String status = remaining == 0 ? "READY" : String.format(Locale.ROOT, "%.1fs", remaining / 1_000.0);
            return "Pickobulus: " + status + (authoritative() ? " (server observed)" : " (estimated)");
        }
        private boolean clock(long now) {
            if (now < 0 || lastClock >= 0 && now < lastClock) {
                reset();
                lastClock = Math.max(-1, now);
                return false;
            }
            lastClock = now;
            return true;
        }
        void reset() {
            readyAt = serverCountdownAt = lastClock = -1;
            readyConfirmed = serverCountdown = awaitingCooldownProof = notificationArmed = false;
            notified = true;
            lastTabStatus = "";
        }
    }
    static final class ShotProjection {
        private long shotAt = -1, accepted, selected;
        private String selectionAtUse = "";
        private boolean selectionChanged;
        boolean start(long now, String selection) {
            if (now < 0) return false;
            if (shotAt >= 0 && now < shotAt) { reset(); return false; }
            if (shotAt >= 0 && now - shotAt <= USE_DEDUP_MILLIS) return false;
            shotAt = now;
            selectionAtUse = selection == null ? "" : selection;
            accepted = selected = 0;
            selectionChanged = false;
            return true;
        }
        void observeSelection(String selection, long now) {
            if (inWindow(now) && !selectionAtUse.equals(selection)) selectionChanged = true;
        }
        void acceptedBreak(long now, long count, boolean selectedTarget, String currentSelection) {
            if (!inWindow(now) || count <= 0) return;
            observeSelection(currentSelection, now);
            accepted = saturatedAdd(accepted, count);
            if (selectedTarget && !selectionChanged) selected = saturatedAdd(selected, count);
        }
        private boolean inWindow(long now) {
            return shotAt >= 0 && now >= shotAt && now - shotAt <= ACCEPTED_BREAK_WINDOW_MILLIS;
        }
        long accepted() { return accepted; }
        long selected() { return selected; }
        boolean selectionChanged() { return selectionChanged; }
        boolean observedUse() { return shotAt >= 0; }
        void reset() { shotAt = -1; accepted = selected = 0; selectionAtUse = ""; selectionChanged = false; }
    }
    static long saturatedAdd(long current, long amount) {
        if (amount <= 0) return Math.max(0, current);
        try { return Math.addExact(Math.max(0, current), amount); }
        catch (ArithmeticException ignored) { return Long.MAX_VALUE; }
    }
    static final class Popup {
        private String text = "";
        private long expiresAt = -1, lastClock = -1;
        void show(String text, long now) {
            long deadline = deadline(now, 2_500L);
            if (deadline < 0) { clear(); return; }
            this.text = text == null ? "" : text;
            expiresAt = deadline;
            lastClock = now;
        }
        String text(long now) {
            if (now < 0 || now < lastClock || now >= expiresAt) { clear(); return ""; }
            lastClock = now;
            return text;
        }
        void clear() { text = ""; expiresAt = lastClock = -1; }
    }
    static final class Context {
        record Observation(boolean active, boolean reset) { }
        private Object level, connection;
        private String profile;
        private long lastClock = -1;
        Observation observe(Object level, Object connection, String profile, boolean enabled,
                            boolean playerPresent, boolean inSkyblock, long now) {
            if (!enabled || !playerPresent || !inSkyblock || level == null || connection == null || now < 0) {
                clear();
                return new Observation(false, true);
            }
            boolean reset = this.level != level || this.connection != connection
                    || !java.util.Objects.equals(this.profile, profile) || lastClock >= 0 && now < lastClock;
            this.level = level;
            this.connection = connection;
            this.profile = profile;
            lastClock = now;
            return new Observation(true, reset);
        }
        void clear() { level = connection = null; profile = null; lastClock = -1; }
    }
    private PickobulusPolicy() {}
}
