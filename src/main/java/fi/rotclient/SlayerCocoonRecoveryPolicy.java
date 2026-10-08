package fi.rotclient;

import java.util.Optional;

/** Exact local cocoon/restart lifecycle; entity ownership is resolved separately. */
public final class SlayerCocoonRecoveryPolicy {
    /**
     * Upstream expiry is 140 server ticks; this wall-clock bound is a conservative
     * seven-second eligibility window, not a claim about server TPS or ability timing.
     * Adapted from Starred's BSD-3-Clause Athen cocoon fix 879d583861:
     * https://github.com/skies-starred/Athen/blob/0869043ae54428e7761383a7f60f44d97cf206ac/src/main/kotlin/foo/starred/athen/api/slayers/resolver/impl/CocoonedSlayerBossResolver.kt
     * Copyright (c) 2025-2026, Starred. Full notice: docs/third-party/Athen-LICENSE.txt.
     */
    public static final long MAX_CHAIN_MILLIS = 7_000L;

    public record Restart(SlayerSpawnPolicy.Announcement announcement, long announcedAtMillis) {
    }

    public static final class State {
        private SlayerPolicy.EntityDescriptor source;
        private int sourceEntityId;
        private long cocoonedAtMillis;
        private long restartedAtMillis = -1L;
        private long lastAcceptedCocoonAtMillis = -1L;

        public boolean observeCocoon(String line, int entityId, SlayerPolicy.EntityDescriptor descriptor,
                                     String localPlayer, long nowMillis) {
            if (!SlayerPolicy.isCocooned(line) || nowMillis < 0L) {
                return false;
            }
            if (lastAcceptedCocoonAtMillis >= 0L
                    && (nowMillis < lastAcceptedCocoonAtMillis
                    || nowMillis - lastAcceptedCocoonAtMillis <= MAX_CHAIN_MILLIS)) {
                return false; // Duplicate delivery cannot extend the chain.
            }
            clearPending();
            if (descriptor == null || descriptor.role() != SlayerPolicy.EntityRole.BOSS
                    || descriptor.type() == null || descriptor.tier() <= 0
                    || localPlayer == null || !localPlayer.matches("[A-Za-z0-9_]{1,16}")
                    || !descriptor.owner().equalsIgnoreCase(localPlayer)) {
                return false;
            }
            source = descriptor;
            sourceEntityId = entityId;
            cocoonedAtMillis = nowMillis;
            lastAcceptedCocoonAtMillis = nowMillis;
            return true;
        }

        public boolean observeRestart(String line, long nowMillis) {
            if (!SlayerPolicy.normalize(line).equals("SLAYER QUEST STARTED!") || !validAt(nowMillis)) {
                return false;
            }
            if (restartedAtMillis >= 0L) {
                return false;
            }
            restartedAtMillis = nowMillis;
            return true;
        }

        public Optional<Restart> resolveRestart(SlayerFightPolicy.QuestRef visibleQuest, long nowMillis) {
            if (!validAt(nowMillis) || restartedAtMillis < 0L) {
                return Optional.empty();
            }
            if (nowMillis < restartedAtMillis
                    || nowMillis - restartedAtMillis > SlayerSpawnPolicy.RESOLUTION_WINDOW_MILLIS) {
                clearPending();
                return Optional.empty();
            }
            if (visibleQuest == null) {
                return Optional.empty();
            }
            if (visibleQuest.type() != source.type() || visibleQuest.tier() != source.tier()) {
                reset();
                return Optional.empty();
            }
            Restart result = new Restart(new SlayerSpawnPolicy.Announcement(source.type(), source.tier()),
                    restartedAtMillis);
            clearPending(); // Retain dedupe after the resumed body has been registered.
            return Optional.of(result);
        }

        public boolean pending(long nowMillis) {
            return validAt(nowMillis);
        }

        public int sourceEntityId() {
            return sourceEntityId;
        }

        private boolean validAt(long nowMillis) {
            if (source == null) return false;
            if (nowMillis < cocoonedAtMillis || nowMillis - cocoonedAtMillis > MAX_CHAIN_MILLIS) {
                clearPending();
                return false;
            }
            return true;
        }

        public void reset() {
            clearPending();
            lastAcceptedCocoonAtMillis = -1L;
        }

        public void clearPending() {
            source = null;
            sourceEntityId = Integer.MIN_VALUE;
            cocoonedAtMillis = 0L;
            restartedAtMillis = -1L;
        }
    }

    private SlayerCocoonRecoveryPolicy() {
    }
}
