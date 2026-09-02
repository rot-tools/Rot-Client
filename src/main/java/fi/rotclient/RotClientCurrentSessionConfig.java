package fi.rotclient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Persistent always-on Current Session configuration (schema v2).
 * This is the canonical durable ledger for generic Current Session accounting.
 * {@code MiningSessionEngine} is ephemeral correlation/diagnostics state only.
 */
final class RotClientCurrentSessionConfig {
    static final int SCHEMA_VERSION = 2;
    static final String STATE_ACTIVE = "ACTIVE";
    static final String STATE_PAUSED = "PAUSED";

    enum PriceStatus {
        RESOLVED_BAZAAR,
        UNAVAILABLE,
        UNSUPPORTED,
        STALE;

        static PriceStatus fromName(String name) {
            if (name == null || name.isBlank()) {
                return UNAVAILABLE;
            }
            String normalized = name.trim().toUpperCase(Locale.ROOT);
            for (PriceStatus status : values()) {
                if (status.name().equals(normalized)) {
                    return status;
                }
            }
            return UNAVAILABLE;
        }
    }

    /** Open segment uses endedAtMillis == 0. */
    record TargetSegment(
            String targetId,
            long startedAtMillis,
            long endedAtMillis) {
        TargetSegment {
            targetId = targetId == null || targetId.isBlank()
                    ? ""
                    : targetId.trim();
            startedAtMillis = Math.max(0L, startedAtMillis);
            endedAtMillis = Math.max(0L, endedAtMillis);
        }

        boolean isOpen() {
            return endedAtMillis <= 0L;
        }

        TargetSegment copy() {
            return new TargetSegment(targetId, startedAtMillis, endedAtMillis);
        }
    }

    /** Open segment uses endedAtMillis == 0. */
    record AreaSegment(
            String areaId,
            long startedAtMillis,
            long endedAtMillis) {
        AreaSegment {
            areaId = SkyBlockArea.fromId(areaId).id();
            startedAtMillis = Math.max(0L, startedAtMillis);
            endedAtMillis = Math.max(0L, endedAtMillis);
        }

        boolean isOpen() {
            return endedAtMillis <= 0L;
        }

        AreaSegment copy() {
            return new AreaSegment(areaId, startedAtMillis, endedAtMillis);
        }
    }

    record SessionItemRecord(
            String itemId,
            String displayName,
            long quantity,
            String sourceType,
            String miningClass,
            String areaId,
            boolean known,
            String priceStatus,
            double resolvedGrossValue) {
        SessionItemRecord {
            itemId = SkyBlockItemId.normalize(itemId);
            if (itemId.isEmpty()) {
                itemId = "UNKNOWN";
            }
            displayName = displayName == null || displayName.isBlank()
                    ? itemId
                    : displayName.trim();
            quantity = Math.max(0L, quantity);
            sourceType = SessionSourceType.fromName(sourceType).name();
            MiningClassification classified =
                    MiningClassification.fromName(miningClass);
            miningClass = classified == null ? null : classified.name();
            areaId = SkyBlockArea.fromId(areaId).id();
            PriceStatus normalizedPrice = PriceStatus.fromName(priceStatus);
            priceStatus = normalizedPrice.name();
            resolvedGrossValue = Double.isFinite(resolvedGrossValue)
                    ? Math.max(0.0, resolvedGrossValue)
                    : 0.0;
            if (normalizedPrice != PriceStatus.RESOLVED_BAZAAR
                    && normalizedPrice != PriceStatus.STALE) {
                resolvedGrossValue = 0.0;
            }
        }

        SessionItemRecord copy() {
            return new SessionItemRecord(
                    itemId,
                    displayName,
                    quantity,
                    sourceType,
                    miningClass,
                    areaId,
                    known,
                    priceStatus,
                    resolvedGrossValue);
        }

        SessionSourceType source() {
            return SessionSourceType.fromName(sourceType);
        }

        MiningClassification miningClassification() {
            return MiningClassification.fromName(miningClass);
        }

        PriceStatus price() {
            return PriceStatus.fromName(priceStatus);
        }
    }

    int schemaVersion = SCHEMA_VERSION;
    String sessionId = "";
    int displayNumber = 1;
    long startedAtMillis;
    String state = STATE_ACTIVE;
    /** 0 when not currently paused; timestamp the current pause began. */
    long pausedAtMillis;
    /** Cumulative paused duration from prior, already-closed pauses. */
    long totalPausedMillis;
    /**
     * True when the current PAUSED state was entered automatically because
     * the client disconnected / stopped. Explicit user pauses leave this
     * false so reconnect does not silently resume them.
     */
    boolean offlineAutoPaused;
    /**
     * Last persisted activity heartbeat. Used to close open activity after an
     * unclean shutdown so offline wall-clock is not counted as active time.
     */
    long lastHeartbeatMillis;
    /**
     * True after a graceful pause/disconnect flush. False while the session is
     * RUNNING so a crash is detectable on next load.
     */
    boolean cleanShutdown = true;
    String currentTargetId = "";
    List<TargetSegment> targetSegments = new ArrayList<>();
    String currentAreaId = SkyBlockArea.UNKNOWN_SKYBLOCK_AREA.id();
    List<AreaSegment> areaSegments = new ArrayList<>();
    List<SessionItemRecord> items = new ArrayList<>();
    /** Canonical count of finalized Powder Chests in this Current Session. */
    long powderChestsOpened;
    /**
     * Last observed Magic Find total for this Current Session. {@code -1}
     * means unset. A zero timestamp means the quantity is not trusted
     * (including Gson default 0 on older files).
     */
    int lastMagicFind = -1;
    long lastMagicFindAtMillis;
    List<String> areasVisited = new ArrayList<>();
    /** Timestamp of the immutable price basis currently projected on rows. */
    long lastPriceBookObservedAtMillis;
    int nextDisplayNumber = 2;

    static RotClientCurrentSessionConfig defaults() {
        RotClientCurrentSessionConfig config =
                new RotClientCurrentSessionConfig();
        long now = System.currentTimeMillis();
        config.schemaVersion = SCHEMA_VERSION;
        config.sessionId = UUID.randomUUID().toString();
        config.displayNumber = 1;
        config.nextDisplayNumber = 2;
        config.startedAtMillis = now;
        config.state = STATE_ACTIVE;
        config.pausedAtMillis = 0L;
        config.totalPausedMillis = 0L;
        config.offlineAutoPaused = false;
        config.lastHeartbeatMillis = now;
        config.cleanShutdown = true;
        config.currentTargetId = "";
        config.targetSegments = new ArrayList<>();
        config.currentAreaId = SkyBlockArea.UNKNOWN_SKYBLOCK_AREA.id();
        config.areaSegments = new ArrayList<>();
        config.items = new ArrayList<>();
        config.powderChestsOpened = 0L;
        config.lastMagicFind = -1;
        config.lastMagicFindAtMillis = 0L;
        config.areasVisited = new ArrayList<>();
        config.lastPriceBookObservedAtMillis = 0L;
        return config;
    }

    RotClientCurrentSessionConfig copy() {
        RotClientCurrentSessionConfig copy =
                new RotClientCurrentSessionConfig();
        copy.schemaVersion = schemaVersion;
        copy.sessionId = sessionId;
        copy.displayNumber = displayNumber;
        copy.startedAtMillis = startedAtMillis;
        copy.state = state;
        copy.pausedAtMillis = pausedAtMillis;
        copy.totalPausedMillis = totalPausedMillis;
        copy.offlineAutoPaused = offlineAutoPaused;
        copy.lastHeartbeatMillis = lastHeartbeatMillis;
        copy.cleanShutdown = cleanShutdown;
        copy.currentTargetId = currentTargetId;
        copy.currentAreaId = currentAreaId;
        copy.lastPriceBookObservedAtMillis = lastPriceBookObservedAtMillis;
        copy.powderChestsOpened = powderChestsOpened;
        copy.lastMagicFind = lastMagicFind;
        copy.lastMagicFindAtMillis = lastMagicFindAtMillis;
        copy.nextDisplayNumber = nextDisplayNumber;
        copy.targetSegments = new ArrayList<>();
        if (targetSegments != null) {
            for (TargetSegment segment : targetSegments) {
                if (segment != null) {
                    copy.targetSegments.add(segment.copy());
                }
            }
        }
        copy.areaSegments = new ArrayList<>();
        if (areaSegments != null) {
            for (AreaSegment segment : areaSegments) {
                if (segment != null) {
                    copy.areaSegments.add(segment.copy());
                }
            }
        }
        copy.items = new ArrayList<>();
        if (items != null) {
            for (SessionItemRecord item : items) {
                if (item != null) {
                    copy.items.add(item.copy());
                }
            }
        }
        copy.areasVisited = areasVisited == null
                ? new ArrayList<>()
                : new ArrayList<>(areasVisited);
        return copy;
    }

    void normalize() {
        schemaVersion = SCHEMA_VERSION;
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        displayNumber = Math.max(1, displayNumber);
        nextDisplayNumber = Math.max(displayNumber + 1, nextDisplayNumber);
        startedAtMillis = Math.max(0L, startedAtMillis);
        if (startedAtMillis == 0L) {
            startedAtMillis = System.currentTimeMillis();
        }
        if (!STATE_PAUSED.equalsIgnoreCase(state)) {
            state = STATE_ACTIVE;
            pausedAtMillis = 0L;
            offlineAutoPaused = false;
        } else {
            state = STATE_PAUSED;
            if (pausedAtMillis <= 0L) {
                // Legacy data (pre-pause-accounting schema) or a corrupt
                // value: treat the pause as starting now rather than
                // guessing, so we never fabricate paused duration.
                pausedAtMillis = System.currentTimeMillis();
            }
        }
        totalPausedMillis = Math.max(0L, totalPausedMillis);
        lastHeartbeatMillis = Math.max(0L, lastHeartbeatMillis);
        if (lastHeartbeatMillis <= 0L) {
            lastHeartbeatMillis = startedAtMillis;
        }
        currentTargetId = currentTargetId == null ? "" : currentTargetId.trim();
        currentAreaId = SkyBlockArea.fromId(currentAreaId).id();
        lastPriceBookObservedAtMillis =
                Math.max(0L, lastPriceBookObservedAtMillis);
        powderChestsOpened = Math.max(0L, powderChestsOpened);
        lastMagicFindAtMillis = Math.max(0L, lastMagicFindAtMillis);
        if (lastMagicFind < 0 || lastMagicFindAtMillis <= 0L) {
            lastMagicFind = -1;
            lastMagicFindAtMillis = 0L;
        }

        if (targetSegments == null) {
            targetSegments = new ArrayList<>();
        }
        List<TargetSegment> cleanedSegments = new ArrayList<>();
        for (TargetSegment segment : targetSegments) {
            if (segment == null) {
                continue;
            }
            cleanedSegments.add(new TargetSegment(
                    segment.targetId(),
                    segment.startedAtMillis(),
                    segment.endedAtMillis()));
        }
        targetSegments = cleanedSegments;

        if (areaSegments == null) {
            areaSegments = new ArrayList<>();
        }
        List<AreaSegment> cleanedAreaSegments = new ArrayList<>();
        for (AreaSegment segment : areaSegments) {
            if (segment == null) {
                continue;
            }
            cleanedAreaSegments.add(new AreaSegment(
                    segment.areaId(),
                    segment.startedAtMillis(),
                    segment.endedAtMillis()));
        }
        areaSegments = cleanedAreaSegments;

        if (items == null) {
            items = new ArrayList<>();
        }
        List<SessionItemRecord> cleanedItems = new ArrayList<>();
        for (SessionItemRecord item : items) {
            if (item == null) {
                continue;
            }
            // Reconstruct to normalize fields; unknown ids survive.
            cleanedItems.add(new SessionItemRecord(
                    item.itemId(),
                    item.displayName(),
                    item.quantity(),
                    item.sourceType(),
                    item.miningClass(),
                    item.areaId(),
                    item.known(),
                    item.priceStatus(),
                    item.resolvedGrossValue()));
        }
        items = cleanedItems;

        if (areasVisited == null) {
            areasVisited = new ArrayList<>();
        }
        LinkedHashSet<String> uniqueAreas = new LinkedHashSet<>();
        for (String area : areasVisited) {
            uniqueAreas.add(SkyBlockArea.fromId(area).id());
        }
        areasVisited = new ArrayList<>(uniqueAreas);
    }

    boolean isActive() {
        return STATE_ACTIVE.equals(state);
    }

    boolean isPaused() {
        return STATE_PAUSED.equals(state);
    }

    static int nextDisplayNumberAfter(int currentDisplayNumber) {
        return Math.max(1, currentDisplayNumber) + 1;
    }
}
