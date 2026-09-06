package fi.rotclient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * One process-local Slayer truth used by all live Slayer projections.
 * Observers mutate this engine; HUDs and manager screens only read snapshots.
 */
public final class SlayerSessionEngine {
    public enum QuestState {
        IDLE,
        ACTIVE
    }

    public record SpawnResult(boolean spawned, boolean owned, SlayerPolicy.EntityDescriptor descriptor) {
        static SpawnResult ignored(SlayerPolicy.EntityDescriptor descriptor) {
            return new SpawnResult(false, false, descriptor);
        }
    }

    public record DeathResult(
            boolean bossKilled,
            boolean owned,
            SlayerPolicy.EntityDescriptor descriptor,
            long durationMillis,
            String completedCarryPlayer) {
        static DeathResult ignored() {
            return new DeathResult(false, false, null, 0L, "");
        }

        public boolean carryAdvanced() {
            return completedCarryPlayer != null && !completedCarryPlayer.isBlank();
        }
    }

    public record ActiveBoss(
            int entityId,
            SlayerPolicy.EntityDescriptor descriptor,
            boolean owned,
            long spawnedAtMillis) {
    }

    public record Carry(
            String player,
            SlayerPolicy.SlayerType type,
            int tier,
            int total,
            int completed,
            long startedAtMillis,
            long lastCompletedAtMillis) {
        public boolean complete() {
            return completed >= total;
        }
    }

    public record Snapshot(
            QuestState questState,
            long sessionStartedAtMillis,
            int bossesKilled,
            long totalKillDurationMillis,
            long lastKillDurationMillis,
            int bossesSinceLastDrop,
            String lastDropName,
            Map<String, Integer> dropCounts,
            List<ActiveBoss> activeBosses,
            List<Carry> carries) {
        public double bossesPerHour(long nowMillis) {
            long duration = Math.max(1L, nowMillis - sessionStartedAtMillis);
            return bossesKilled * 3_600_000.0D / duration;
        }

        public long averageKillDurationMillis() {
            return bossesKilled <= 0 ? 0L : totalKillDurationMillis / bossesKilled;
        }
    }

    private final Map<Integer, ActiveBoss> active = new LinkedHashMap<>();
    private final Set<Integer> retiredOwnedBossIds = new HashSet<>();
    private final Map<String, Integer> drops = new LinkedHashMap<>();
    private final List<Carry> carries = new ArrayList<>();
    private QuestState questState = QuestState.IDLE;
    private long sessionStartedAtMillis;
    private long lastOwnedBossKillMillis;
    private int bossesKilled;
    private long totalKillDurationMillis;
    private long lastKillDurationMillis;
    private int bossesSinceLastDrop;
    private String lastDropName = "";

    public synchronized SlayerPolicy.QuestSignal onChat(String line, long nowMillis) {
        return onChat(line, nowMillis, true);
    }

    public synchronized SlayerPolicy.QuestSignal onChat(
            String line,
            long nowMillis,
            boolean detectDrops) {
        SlayerPolicy.QuestSignal signal = SlayerPolicy.questSignal(line);
        switch (signal) {
            case STARTED -> {
                questState = QuestState.ACTIVE;
                active.values().stream()
                        .filter(ActiveBoss::owned)
                        .map(ActiveBoss::entityId)
                        .forEach(retiredOwnedBossIds::add);
                active.values().removeIf(ActiveBoss::owned);
                ensureSessionStarted(nowMillis);
            }
            case COMPLETED -> {
                questState = QuestState.IDLE;
                ActiveBoss ownedBoss = active.values().stream()
                        .filter(ActiveBoss::owned)
                        .filter(candidate -> candidate.descriptor().role() == SlayerPolicy.EntityRole.BOSS)
                        .findFirst()
                        .orElse(null);
                if (ownedBoss != null) {
                    recordDeath(ownedBoss, nowMillis);
                }
                active.clear();
            }
            case FAILED -> {
                questState = QuestState.IDLE;
                active.clear();
            }
            case NONE -> {
                if (detectDrops) {
                    SlayerPolicy.dropObservation(line)
                            .ifPresent(drop -> observeDrop(drop.displayName(), true, nowMillis));
                }
            }
        }
        return signal;
    }

    public synchronized SpawnResult observeEntity(
            int entityId,
            SlayerPolicy.EntityDescriptor descriptor,
            String localPlayer,
            long nowMillis) {
        if (descriptor == null) {
            return SpawnResult.ignored(descriptor);
        }
        if (retiredOwnedBossIds.contains(entityId)) {
            return SpawnResult.ignored(descriptor);
        }
        ActiveBoss existing = active.get(entityId);
        if (existing != null) {
            boolean owned = samePlayer(descriptor.owner(), localPlayer);
            active.put(entityId, new ActiveBoss(
                    entityId, descriptor, owned, existing.spawnedAtMillis()));
            return SpawnResult.ignored(descriptor);
        }
        if (descriptor.role() == SlayerPolicy.EntityRole.BOSS
                && descriptor.owner().isBlank()) {
            return SpawnResult.ignored(descriptor);
        }
        boolean owned = samePlayer(descriptor.owner(), localPlayer);
        if (descriptor.role() == SlayerPolicy.EntityRole.BOSS && owned
                && hasOwnedBossOfType(descriptor.type(), entityId)) {
            return SpawnResult.ignored(descriptor);
        }
        ActiveBoss observed = new ActiveBoss(entityId, descriptor, owned, Math.max(0L, nowMillis));
        active.put(entityId, observed);
        ensureSessionStarted(nowMillis);
        return new SpawnResult(true, owned, descriptor);
    }

    public synchronized DeathResult onEntityDeath(int entityId, long nowMillis) {
        if (retiredOwnedBossIds.remove(entityId)) {
            return DeathResult.ignored();
        }
        ActiveBoss removed = active.remove(entityId);
        if (removed == null) {
            return DeathResult.ignored();
        }
        long duration = Math.max(0L, nowMillis - removed.spawnedAtMillis());
        SlayerPolicy.EntityDescriptor descriptor = removed.descriptor();
        if (descriptor.role() != SlayerPolicy.EntityRole.BOSS) {
            return new DeathResult(false, removed.owned(), descriptor, duration, "");
        }
        if (SlayerFightPolicy.isTarantulaTierFivePhaseOne(descriptor)) {
            return new DeathResult(false, removed.owned(), descriptor, duration, "");
        }
        if (removed.owned()
                && duration < SlayerTimeMessagePolicy.MIN_KILL_DURATION_MILLIS
                && lastOwnedBossKillMillis > 0L
                && nowMillis - lastOwnedBossKillMillis < 2_000L) {
            return new DeathResult(false, true, descriptor, duration, "");
        }
        String completedCarryPlayer = recordDeath(removed, nowMillis);
        return new DeathResult(true, removed.owned(), descriptor, duration, completedCarryPlayer);
    }

    public synchronized void observeQuestVisible(boolean visible, long nowMillis) {
        if (visible && questState != QuestState.ACTIVE) {
            questState = QuestState.ACTIVE;
            ensureSessionStarted(nowMillis);
        }
    }

    public synchronized boolean addCarry(
            String player,
            SlayerPolicy.SlayerType type,
            int tier,
            int total,
            long nowMillis) {
        String safePlayer = player == null ? "" : player.trim();
        if (!safePlayer.matches("[A-Za-z0-9_]{1,16}") || type == null || total <= 0) {
            return false;
        }
        int safeTier = Math.max(0, Math.min(5, tier));
        for (Carry carry : carries) {
            if (samePlayer(carry.player(), safePlayer)) {
                return false;
            }
        }
        carries.add(new Carry(safePlayer, type, safeTier, total, 0, Math.max(0L, nowMillis), 0L));
        return true;
    }

    public synchronized boolean removeCarry(String player) {
        return carries.removeIf(carry -> samePlayer(carry.player(), player));
    }

    public synchronized boolean completeCarry(String player, long nowMillis) {
        for (int i = 0; i < carries.size(); i++) {
            Carry carry = carries.get(i);
            if (samePlayer(carry.player(), player) && !carry.complete()) {
                carries.set(i, new Carry(
                        carry.player(), carry.type(), carry.tier(), carry.total(),
                        carry.total(), carry.startedAtMillis(), Math.max(0L, nowMillis)));
                return true;
            }
        }
        return false;
    }

    public synchronized void resetStats(long nowMillis) {
        sessionStartedAtMillis = Math.max(0L, nowMillis);
        bossesKilled = 0;
        totalKillDurationMillis = 0L;
        lastKillDurationMillis = 0L;
        bossesSinceLastDrop = 0;
        lastDropName = "";
        drops.clear();
    }

    public synchronized boolean observeDrop(String displayName) {
        return observeDrop(displayName, true);
    }

    /**
     * Records every recognized drop in the canonical session. The caller may
     * independently decide whether it resets the selected since-last counter.
     */
    public synchronized boolean observeDrop(String displayName, boolean resetsSinceLastCounter) {
        return observeDrop(displayName, resetsSinceLastCounter, 0L);
    }

    /**
     * Records a verified in-session drop. A timestamp supplied by a gameplay
     * observer can establish the session boundary without relying on a HUD.
     */
    public synchronized boolean observeDrop(
            String displayName,
            boolean resetsSinceLastCounter,
            long nowMillis) {
        String name = displayName == null ? "" : displayName.trim();
        if (name.isEmpty()) return false;
        if (nowMillis > 0L) {
            ensureSessionStarted(nowMillis);
        }
        drops.merge(name, 1, Integer::sum);
        if (resetsSinceLastCounter) {
            bossesSinceLastDrop = 0;
        }
        lastDropName = name;
        return true;
    }

    public synchronized void resetWorld() {
        active.clear();
        retiredOwnedBossIds.clear();
        questState = QuestState.IDLE;
    }

    public synchronized List<Carry> carries() {
        return List.copyOf(carries);
    }

    /** Read-only lifecycle state for overlays that must not start a session. */
    public synchronized QuestState questState() {
        return questState;
    }

    /**
     * Read-only snapshot. A display, command, or renderer must never create
     * a Slayer session; only verified gameplay observations may do that.
     */
    public synchronized Snapshot snapshot(long nowMillis) {
        return snapshotView();
    }

    /**
     * Read-only projection for HUDs. Unlike {@link #snapshot(long)}, this never
     * creates a session merely because a display was opened.
     */
    public synchronized Snapshot viewSnapshot() {
        return snapshotView();
    }

    private Snapshot snapshotView() {
        return new Snapshot(
                questState,
                sessionStartedAtMillis,
                bossesKilled,
                totalKillDurationMillis,
                lastKillDurationMillis,
                bossesSinceLastDrop,
                lastDropName,
                Map.copyOf(drops),
                List.copyOf(active.values()),
                List.copyOf(carries));
    }

    private String recordDeath(ActiveBoss removed, long nowMillis) {
        long duration = Math.max(0L, nowMillis - removed.spawnedAtMillis());
        SlayerPolicy.EntityDescriptor descriptor = removed.descriptor();
        if (removed.owned()) {
            bossesKilled++;
            bossesSinceLastDrop++;
            totalKillDurationMillis += duration;
            lastKillDurationMillis = duration;
            lastOwnedBossKillMillis = nowMillis;
        }
        return completeMatchingCarry(descriptor, nowMillis);
    }

    private String completeMatchingCarry(
            SlayerPolicy.EntityDescriptor descriptor,
            long nowMillis) {
        for (int i = 0; i < carries.size(); i++) {
            Carry carry = carries.get(i);
            if (!carry.complete()
                    && samePlayer(carry.player(), descriptor.owner())
                    && carry.type() == descriptor.type()
                    && (carry.tier() == 0 || carry.tier() == descriptor.tier())) {
                carries.set(i, increment(carry, nowMillis));
                return carry.player();
            }
        }
        return "";
    }

    private static Carry increment(Carry carry, long nowMillis) {
        return new Carry(
                carry.player(),
                carry.type(),
                carry.tier(),
                carry.total(),
                Math.min(carry.total(), carry.completed() + 1),
                carry.startedAtMillis(),
                Math.max(0L, nowMillis));
    }

    private void ensureSessionStarted(long nowMillis) {
        if (sessionStartedAtMillis <= 0L) {
            sessionStartedAtMillis = Math.max(1L, nowMillis);
        }
    }

    private boolean hasOwnedBossOfType(SlayerPolicy.SlayerType type, int exceptEntityId) {
        for (ActiveBoss existing : active.values()) {
            if (existing.entityId() == exceptEntityId) {
                continue;
            }
            if (existing.owned()
                    && existing.descriptor().role() == SlayerPolicy.EntityRole.BOSS
                    && existing.descriptor().type() == type) {
                return true;
            }
        }
        return false;
    }

    private static boolean samePlayer(String left, String right) {
        return left != null && right != null
                && left.trim().toLowerCase(Locale.ROOT)
                .equals(right.trim().toLowerCase(Locale.ROOT));
    }
}
