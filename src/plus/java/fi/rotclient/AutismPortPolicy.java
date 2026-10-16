// SPDX-License-Identifier: GPL-3.0-only
// Adapted from AUTISM Client 5.0 by Melonik and contributors.
// Sources: SafeWalkModule, BuiltinModules.NoInteractModule, util.RegistryListCodec.
// Rot Tools: pure policy boundaries, clock/reset guards and bounded list input.
package fi.rotclient;

import java.util.List;
import java.util.Locale;

public final class AutismPortPolicy {
    private static final java.util.regex.Pattern LIST_SEPARATOR = java.util.regex.Pattern.compile("[,;\\s]+");
    private AutismPortPolicy() {}

    public static List<String> registryEntries(String text) {
        if (text == null || text.isBlank()) return List.of();
        return LIST_SEPARATOR.splitAsStream(text.toLowerCase(Locale.ROOT))
                .filter(s -> !s.isBlank()).limit(128).distinct().toList();
    }

    public static boolean blocks(String registryId, List<String> entries, String mode) {
        if (registryId == null || registryId.isBlank() || entries == null || entries.isEmpty()) return false;
        boolean listed = false;
        for (String entry : entries) {
            String normalized = entry.trim().toLowerCase(Locale.ROOT);
            if (registryId.equalsIgnoreCase(normalized)
                    || registryId.toLowerCase(Locale.ROOT).endsWith(":" + normalized)) {
                listed = true;
                break;
            }
        }
        return "WhiteList".equalsIgnoreCase(mode) ? !listed : listed;
    }

    public static boolean handMatches(String mode, boolean offhand) {
        return "Both".equals(mode) || (offhand ? "Offhand" : "Mainhand").equals(mode);
    }

    public static final class SneakHold {
        private boolean holding;
        private long holdUntil, lastNow;
        private int clearTicks;

        public boolean update(boolean eligible, boolean edge, long now, long holdMillis) {
            if (!eligible || now < lastNow) { reset(); return false; }
            lastNow = now;
            clearTicks = edge ? 0 : Math.min(3, clearTicks + 1);
            if (holding && now < holdUntil) return true;
            if (edge) {
                holding = true;
                holdUntil = now + Math.max(0, Math.min(500, holdMillis));
                return true;
            }
            if (holding && clearTicks < 3) return true;
            holding = false;
            return false;
        }
        public void reset() { holding = false; holdUntil = lastNow = 0; clearTicks = 0; }
    }

    public record Tool(int slot, float speed, boolean silk, boolean damageable, int remaining) {}

    public static boolean mayFillSign(String[] current, boolean replaceExisting) {
        if (current == null || current.length == 0) return false;
        if (replaceExisting) return true;
        for (String line : current) if (line != null && !line.isEmpty()) return false;
        return true;
    }

    public record Look(float yaw, float pitch) {}

    public static Look look(float yaw, float pitch, double x, double y, boolean limitPitch) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) return new Look(yaw, pitch);
        double nextYaw = yaw + x * .15, nextPitch = pitch + y * .15;
        nextYaw = ((nextYaw % 360) + 540) % 360 - 180;
        nextPitch = limitPitch ? Math.clamp(nextPitch, -90, 90) : ((nextPitch % 360) + 540) % 360 - 180;
        return new Look((float) nextYaw, (float) nextPitch);
    }

    /** A manual slot selection cancels this mining session's tool ownership until release. */
    public static final class ToolOwnership {
        private int original = -1, assigned = -1;
        private boolean cancelled;

        public void observe(int selected) {
            int expected = assigned >= 0 ? assigned : original;
            if (expected >= 0 && selected != expected) cancelled = true;
        }
        public boolean begin(int selected) {
            observe(selected);
            if (cancelled) return false;
            if (original < 0) original = selected;
            return true;
        }
        public void assigned(int selected) { assigned = selected; }
        public int restoreSlot(int selected) {
            observe(selected);
            return !cancelled && assigned >= 0 ? original : -1;
        }
        public void reset() { original = assigned = -1; cancelled = false; }
    }

    public static int bestTool(List<Tool> tools, int selected, boolean ignoreDurability, boolean preferSilk) {
        int best = selectTool(tools, selected, ignoreDurability, preferSilk);
        return best < 0 && preferSilk ? selectTool(tools, selected, ignoreDurability, false) : best;
    }
    private static int selectTool(List<Tool> tools, int selected, boolean ignoreDurability, boolean requireSilk) {
        Tool best = null;
        for (Tool tool : tools) {
            if (tool.slot < 0 || tool.slot > 8 || !Float.isFinite(tool.speed) || tool.speed <= 1
                    || !ignoreDurability && tool.damageable && tool.remaining <= 2 || requireSilk && !tool.silk) continue;
            if (best == null || tool.speed > best.speed || tool.speed == best.speed
                    && Math.abs(tool.slot - selected) < Math.abs(best.slot - selected)) best = tool;
        }
        return best == null ? -1 : best.slot;
    }
}
