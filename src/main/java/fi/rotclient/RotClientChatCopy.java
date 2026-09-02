package fi.rotclient;

import java.util.List;

/**
 * Small, deterministic copy helper for Rot Client notifications. Keeping the
 * text selection independent from Minecraft makes the visual chat treatment
 * easy to test and keeps every runtime message consistent.
 */
final class RotClientChatCopy {
    private static final List<String> ONE_LINERS = List.of(
            "Freshly calibrated. Probably.",
            "Powered by polite pixels.",
            "No creepers were consulted.",
            "The gears approve.",
            "Zero drama, maximum sparkle.",
            "That went smoother than a slime block.",
            "One less button mystery.",
            "Tiny victory unlocked.");

    private RotClientChatCopy() {
    }

    static String oneLiner(int index) {
        return ONE_LINERS.get(Math.floorMod(index, ONE_LINERS.size()));
    }

    static String normalizedAction(String action) {
        if (action == null || action.isBlank()) {
            return "Ready.";
        }
        return action.trim();
    }

    static int oneLinerCount() {
        return ONE_LINERS.size();
    }
}
