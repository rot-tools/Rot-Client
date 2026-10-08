package fi.rotclient;

import java.util.Optional;
import java.util.Objects;

/** Solves only the NPC's explicit arrow sequence. No block/entity scanning. */
final class PuzzlerPolicy {
    static final long TARGET_FRESH_MILLIS = 120_000L;
    private static final String NPC_PREFIX = "[NPC] Puzzler:";
    record Target(int x, int y, int z) {}
    static Optional<Target> solve(String message) {
        String text = CommissionDisplayPolicy.normalizeLine(message);
        if (!text.startsWith(NPC_PREFIX)) return Optional.empty();
        String arrows = text.substring(NPC_PREFIX.length()).replace(" ", "");
        // The observed NPC challenge has ten arrows. Short/partial chat is not a challenge.
        if (!arrows.matches("[▲▶▼◀]{10}")) return Optional.empty();
        int x = 181, z = 135;
        for (char arrow : arrows.toCharArray()) {
            switch (arrow) {
                case '▲' -> z++;
                case '▼' -> z--;
                case '▶' -> x--;
                case '◀' -> x++;
                default -> throw new IllegalStateException();
            }
        }
        return Optional.of(new Target(x, 195, z));
    }

    /** World/player/config use identity: a reloaded profile must not inherit an old answer. */
    record Scope(Object world, Object player, Object config, String profileId) {
        boolean matches(Scope other) {
            return other != null && world == other.world && player == other.player
                    && config == other.config && Objects.equals(profileId, other.profileId);
        }
    }

    static final class State {
        private Target target;
        private Scope scope;
        private long seenAt = -1, lastClock = -1;

        void clear() { target = null; scope = null; seenAt = lastClock = -1; }

        void observe(String message, Scope currentScope, long now) {
            current(currentScope, now);
            if (currentScope == null || now < 0) return;
            String text = CommissionDisplayPolicy.normalizeLine(message);
            if (!text.startsWith(NPC_PREFIX)) return;
            // Completion, a wrong answer and another challenge all retire the previous target.
            clear();
            solve(text).ifPresent(answer -> { target = answer; scope = currentScope; seenAt = lastClock = now; });
        }

        Optional<Target> current(Scope currentScope, long now) {
            if (target != null && (currentScope == null || !scope.matches(currentScope)
                    || now < lastClock || now - seenAt >= TARGET_FRESH_MILLIS)) clear();
            if (target != null) lastClock = now;
            return Optional.ofNullable(target);
        }
    }
    private PuzzlerPolicy() {}
}
