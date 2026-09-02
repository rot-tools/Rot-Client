package fi.rotclient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Canonical persistent Current Session ledger for generic accounting.
 *
 * <h2>Ownership model</h2>
 * <ul>
 *   <li>ONE accepted gameplay observation → ONE {@link #creditItem} /
 *       {@link #creditUnknown} mutation here</li>
 *   <li>{@code MiningSessionEngine} may retain transient classifier /
 *       correlation / dedupe state, but must not independently own persisted
 *       Current Session quantities</li>
 *   <li>Analytics Other Mined, HUD OTHERS, and future Session History freeze
 *       are read-only projections of this ledger</li>
 *   <li>Live target tracker remains authoritative for target-family
 *       accounting and is never replaced by this ledger</li>
 *   <li>Engine snapshots never wholesale replace persisted categories</li>
 * </ul>
 */
final class RotClientCurrentSession {
    private static final long VALUATION_REFRESH_INTERVAL_MILLIS = 2_500L;

    /** Test-visible valuation heartbeat used by Current Session sync. */
    static long valuationRefreshIntervalMillis() {
        return VALUATION_REFRESH_INTERVAL_MILLIS;
    }

    @FunctionalInterface
    interface Persistence {
        boolean save(RotClientCurrentSessionConfig config);
    }

    /** One canonical reward row from one finalized Powder Chest message. */
    record PowderChestCredit(
            String itemId,
            String displayName,
            long quantity,
            SessionSourceType sourceType) {
        PowderChestCredit {
            itemId = SkyBlockItemId.normalize(itemId);
            displayName = displayName == null ? "" : displayName.trim();
            if (itemId.isEmpty()) {
                throw new IllegalArgumentException(
                        "Powder Chest item ID cannot be blank");
            }
            if (quantity <= 0L) {
                throw new IllegalArgumentException(
                        "Powder Chest quantity must be positive");
            }
            if (sourceType != SessionSourceType.CHEST
                    && sourceType != SessionSourceType.CURRENCY) {
                throw new IllegalArgumentException(
                        "Powder Chest source must be CHEST or CURRENCY");
            }
        }
    }

    private final Persistence persistence;
    private RotClientCurrentSessionConfig config =
            RotClientCurrentSessionConfig.defaults();
    private boolean dirty;
    private boolean persistenceBlocked;
    private String persistenceWarning = "";
    private boolean persistenceWarningPending;
    private long nextSaveAttemptMillis;
    private long lastMutationMillis;
    private long lastSyncMillis;
    /** Last engine ledger event timestamp successfully projected. */
    private long lastSyncedEventTimestamp = -1L;
    private final SkyBlockContentRegistry registry =
            SkyBlockContentRegistry.builtin();

    RotClientCurrentSession() {
        this(RotClientCurrentSessionStore::save);
    }

    RotClientCurrentSession(Persistence persistence) {
        if (persistence == null) {
            throw new IllegalArgumentException("Persistence cannot be null");
        }
        this.persistence = persistence;
    }

    void loadFromDisk() {
        restoreLoadedState(RotClientCurrentSessionStore.load());
    }

    void restoreLoadedState(
            RotClientCurrentSessionStore.LoadResult loaded) {
        if (loaded == null) {
            loaded = new RotClientCurrentSessionStore.LoadResult(
                    RotClientCurrentSessionConfig.defaults(),
                    RotClientCurrentSessionStore.LoadStatus.RECOVERY_REQUIRED,
                    RotClientCurrentSessionStore.RECOVERY_WARNING);
        }
        config = loaded.config();
        config = RotClientCurrentSessionMath.ensureSession1IfMissing(config);
        config = RotClientCurrentSessionMath.recoverUncleanShutdown(config);
        persistenceBlocked = !loaded.writable();
        persistenceWarning = loaded.warning();
        persistenceWarningPending = persistenceBlocked
                && !persistenceWarning.isBlank();
        if (persistenceBlocked) {
            config.state = RotClientCurrentSessionConfig.STATE_PAUSED;
            config.pausedAtMillis = Math.max(
                    config.startedAtMillis,
                    Math.max(config.pausedAtMillis,
                            config.lastHeartbeatMillis));
            config.offlineAutoPaused = false;
            config.cleanShutdown = true;
        }
        dirty = false;
        nextSaveAttemptMillis = 0L;
        lastMutationMillis = 0L;
        lastSyncMillis = 0L;
        lastSyncedEventTimestamp = -1L;
        if (config.isPaused() && config.offlineAutoPaused) {
            // Persist recovered pause boundary without waiting for a tick.
            dirty = true;
            lastMutationMillis = System.currentTimeMillis();
        }
    }

    RotClientCurrentSessionConfig snapshotConfig() {
        return config.copy();
    }

    boolean isActive() {
        return config.isActive();
    }

    boolean isPaused() {
        return config.isPaused();
    }

    String persistenceWarning() {
        return persistenceWarning == null ? "" : persistenceWarning;
    }

    String takePersistenceWarning() {
        if (!persistenceWarningPending) {
            return "";
        }
        persistenceWarningPending = false;
        return persistenceWarning();
    }

    int displayNumber() {
        return config.displayNumber;
    }

    String sessionId() {
        return config.sessionId;
    }

    boolean acceptsTargetMaterial(TrackedMaterial material) {
        TrackerSelection selection = TrackerSelection.findKnown(
                config.currentTargetId);
        return material != null
                && selection != null
                && selection.isMaterial()
                && selection.materialTarget().includes(material);
    }

    boolean acceptsTargetGemstone(GemstoneType gemstone) {
        TrackerSelection selection = TrackerSelection.findKnown(
                config.currentTargetId);
        return gemstone != null
                && selection != null
                && selection.isGemstone()
                && selection.gemstone() == gemstone;
    }

    /**
     * HUD OTHERS projection over this session's item ledger. Always AVAILABLE
     * when a session exists (Session 1 is created on load). Never collapses
     * underlying source taxonomy — Analytics still separates OTHER_MINED /
     * MOB / CHEST.
     */
    MiningHudOtherSummary otherHudSummary() {
        return RotClientCurrentSessionMath.toHudOtherSummary(config);
    }

    void ensureActiveForTracker(TrackerSelection selection, long now) {
        config = RotClientCurrentSessionMath.ensureSession1IfMissing(config);
        if (config.isPaused()) {
            return;
        }
        config.state = RotClientCurrentSessionConfig.STATE_ACTIVE;
        if (selection != null) {
            String targetId = selection.id();
            if (config.currentTargetId == null
                    || config.currentTargetId.isBlank()) {
                onTargetChanged(targetId, now);
            } else if (!targetId.equals(config.currentTargetId)) {
                onTargetChanged(targetId, now);
            }
        }
        markDirty(now);
    }

    void onTargetChanged(String newTargetId, long now) {
        String target = newTargetId == null ? "" : newTargetId.trim();
        if (target.equals(config.currentTargetId)) {
            return;
        }
        config.currentTargetId = target;
        if (config.isActive()) {
            config.targetSegments =
                    RotClientCurrentSessionMath.closeSegmentAndOpen(
                            config.targetSegments, target, now);
        }
        markDirty(now);
    }

    void onAreaChanged(SkyBlockArea newArea, long now) {
        SkyBlockArea area = newArea == null
                ? SkyBlockArea.UNKNOWN_SKYBLOCK_AREA
                : newArea;
        String areaId = area.id();
        if (areaId.equals(config.currentAreaId)) {
            return;
        }
        config.currentAreaId = areaId;
        if (config.isActive()) {
            config.areaSegments =
                    RotClientCurrentSessionMath.closeAreaSegmentAndOpen(
                            config.areaSegments, areaId, now);
        }
        if (area != SkyBlockArea.UNKNOWN_SKYBLOCK_AREA) {
            noteArea(areaId);
        }
        markDirty(now);
    }

    void creditItem(
            String itemId,
            String displayName,
            long quantity,
            SessionSourceType sourceType,
            MiningClassification miningClass,
            SkyBlockArea area,
            RotClientCurrentSessionConfig.PriceStatus priceStatus,
            double resolvedGrossValue,
            long now) {
        if (!config.isActive() || quantity <= 0L) {
            return;
        }
        String id = SkyBlockItemId.normalize(itemId);
        if (id.isEmpty()) {
            id = "UNKNOWN";
        }
        SessionSourceType source = sourceType == null
                ? SessionSourceType.UNATTRIBUTED
                : sourceType;
        String miningName = miningClass == null ? null : miningClass.name();
        String areaId = area == null
                ? SkyBlockArea.UNKNOWN_SKYBLOCK_AREA.id()
                : area.id();
        onAreaChanged(area, now);
        boolean known = registry.isKnown(id);
        String name = displayName;
        if (name == null || name.isBlank()) {
            Optional<SkyBlockContentRegistry.KnownItem> knownItem =
                    registry.lookup(id);
            name = knownItem.map(SkyBlockContentRegistry.KnownItem::displayName)
                    .orElse(id);
        }
        RotClientCurrentSessionConfig.PriceStatus status = priceStatus == null
                ? RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE
                : priceStatus;

        if (config.items == null) {
            config.items = new ArrayList<>();
        }
        for (int i = 0; i < config.items.size(); i++) {
            RotClientCurrentSessionConfig.SessionItemRecord existing =
                    config.items.get(i);
            if (existing == null) {
                continue;
            }
            if (!id.equals(existing.itemId())) {
                continue;
            }
            if (existing.source() != source) {
                continue;
            }
            if (!areaId.equals(existing.areaId())) {
                continue;
            }
            MiningClassification existingClass =
                    existing.miningClassification();
            if (miningClass == null
                    ? existingClass != null
                    : !miningClass.equals(existingClass)) {
                continue;
            }
            RotClientCurrentSessionConfig.PriceStatus existingStatus =
                    existing.price();
            boolean fullyResolved = existingStatus
                    == RotClientCurrentSessionConfig.PriceStatus.RESOLVED_BAZAAR
                    && status
                    == RotClientCurrentSessionConfig.PriceStatus.RESOLVED_BAZAAR
                    && Double.isFinite(existing.resolvedGrossValue())
                    && existing.resolvedGrossValue() > 0.0
                    && Double.isFinite(resolvedGrossValue)
                    && resolvedGrossValue > 0.0;
            RotClientCurrentSessionConfig.PriceStatus mergedStatus =
                    fullyResolved
                            ? RotClientCurrentSessionConfig.PriceStatus
                            .RESOLVED_BAZAAR
                            : unresolvedMergeStatus(existingStatus, status);
            double nextValue = fullyResolved
                    ? existing.resolvedGrossValue() + resolvedGrossValue
                    : 0.0;
            config.items.set(i, new RotClientCurrentSessionConfig.SessionItemRecord(
                    existing.itemId(),
                    existing.displayName(),
                    Math.addExact(existing.quantity(), quantity),
                    existing.sourceType(),
                    existing.miningClass(),
                    existing.areaId(),
                    existing.known() || known,
                    mergedStatus.name(),
                    nextValue));
            noteArea(areaId);
            markDirty(now);
            return;
        }

        boolean resolved = status
                == RotClientCurrentSessionConfig.PriceStatus.RESOLVED_BAZAAR
                && Double.isFinite(resolvedGrossValue)
                && resolvedGrossValue > 0.0;
        RotClientCurrentSessionConfig.PriceStatus initialStatus = resolved
                ? status
                : (status
                == RotClientCurrentSessionConfig.PriceStatus.RESOLVED_BAZAAR
                ? RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE
                : status);
        config.items.add(new RotClientCurrentSessionConfig.SessionItemRecord(
                id,
                name,
                quantity,
                source.name(),
                miningName,
                areaId,
                known,
                initialStatus.name(),
                resolved ? resolvedGrossValue : 0.0));
        noteArea(areaId);
        markDirty(now);
    }

    /**
     * Records the last displayed Magic Find total for this session. Does not
     * compute drop rates and does not invent a missing value as zero.
     */
    void noteMagicFind(int magicFind, long now) {
        if (!config.isActive() || magicFind < 0) {
            return;
        }
        if (config.lastMagicFind == magicFind
                && config.lastMagicFindAtMillis > 0L) {
            return;
        }
        config.lastMagicFind = magicFind;
        config.lastMagicFindAtMillis = Math.max(0L, now);
        markDirty(now);
    }

    /**
     * Atomically credits all known rewards from one finalized Powder Chest.
     * Reward quantities remain owned by Current Session; the standalone
     * Powder Chest module is only a read-only projection of these rows.
     */
    boolean creditPowderChestBatch(
            List<PowderChestCredit> credits,
            SkyBlockArea area,
            long now) {
        if (!config.isActive() || credits == null) {
            return false;
        }
        SkyBlockArea safeArea = area == null
                ? SkyBlockArea.UNKNOWN_SKYBLOCK_AREA
                : area;
        String areaId = safeArea.id();
        Map<String, Long> preflightQuantities = new HashMap<>();
        try {
            Math.addExact(config.powderChestsOpened, 1L);
            for (PowderChestCredit credit : credits) {
                if (credit == null) {
                    return false;
                }
                String key = credit.itemId()
                        + '|' + credit.sourceType().name()
                        + '|' + areaId;
                long current = preflightQuantities.computeIfAbsent(
                        key,
                        ignored -> currentQuantity(
                                credit.itemId(), credit.sourceType(), areaId));
                preflightQuantities.put(
                        key,
                        Math.addExact(current, credit.quantity()));
            }
        } catch (ArithmeticException overflow) {
            return false;
        }

        for (PowderChestCredit credit : credits) {
            creditItem(
                    credit.itemId(),
                    credit.displayName(),
                    credit.quantity(),
                    credit.sourceType(),
                    null,
                    safeArea,
                    RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                    0.0,
                    now);
        }
        config.powderChestsOpened = Math.addExact(
                config.powderChestsOpened, 1L);
        markDirty(now);
        return true;
    }

    private long currentQuantity(
            String itemId,
            SessionSourceType source,
            String areaId) {
        if (config.items == null) {
            return 0L;
        }
        for (RotClientCurrentSessionConfig.SessionItemRecord item
                : config.items) {
            if (item != null
                    && item.itemId().equals(itemId)
                    && item.source() == source
                    && item.areaId().equals(areaId)
                    && item.miningClassification() == null) {
                return item.quantity();
            }
        }
        return 0L;
    }

    /** Credits an unknown / uncatalogued item without discarding it. */
    void creditUnknown(
            String itemId,
            long quantity,
            SessionSourceType source,
            long now) {
        creditItem(
                itemId,
                itemId,
                quantity,
                source == null ? SessionSourceType.UNATTRIBUTED : source,
                null,
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                RotClientCurrentSessionConfig.PriceStatus.UNSUPPORTED,
                0.0,
                now);
    }

    void pause(long now) {
        if (!config.isPaused()) {
            config.pausedAtMillis = Math.max(0L, now);
            closeActivitySegments(now);
        }
        config.offlineAutoPaused = false;
        config.state = RotClientCurrentSessionConfig.STATE_PAUSED;
        config.cleanShutdown = true;
        config.lastHeartbeatMillis = Math.max(config.lastHeartbeatMillis, now);
        markDirty(now);
    }

    /**
     * Pause for disconnect / client stop so wall-clock offline time is
     * excluded from {@link #activeDurationMillis}. Distinct from explicit
     * user pause so reconnect can auto-resume safely.
     */
    void pauseForOffline(long now) {
        if (config.isPaused()) {
            config.cleanShutdown = true;
            markDirty(now);
            return;
        }
        config.pausedAtMillis = Math.max(0L, now);
        closeActivitySegments(now);
        config.offlineAutoPaused = true;
        config.state = RotClientCurrentSessionConfig.STATE_PAUSED;
        config.cleanShutdown = true;
        config.lastHeartbeatMillis = Math.max(config.lastHeartbeatMillis, now);
        markDirty(now);
    }

    boolean resume(long now) {
        if (persistenceBlocked) {
            config.state = RotClientCurrentSessionConfig.STATE_PAUSED;
            config.offlineAutoPaused = false;
            return false;
        }
        boolean wasPaused = config.isPaused();
        if (wasPaused && config.pausedAtMillis > 0L) {
            long delta = Math.max(0L, now - config.pausedAtMillis);
            config.totalPausedMillis =
                    Math.addExact(config.totalPausedMillis, delta);
        }
        config.pausedAtMillis = 0L;
        config.offlineAutoPaused = false;
        config.state = RotClientCurrentSessionConfig.STATE_ACTIVE;
        config.cleanShutdown = false;
        config.lastHeartbeatMillis = Math.max(0L, now);
        if (wasPaused) {
            reopenActivitySegments(now);
        }
        markDirty(now);
        return true;
    }

    private void closeActivitySegments(long now) {
        config.targetSegments =
                RotClientCurrentSessionMath.closeSegmentAndOpen(
                        config.targetSegments, "", now);
        config.areaSegments =
                RotClientCurrentSessionMath.closeAreaSegmentAndOpen(
                        config.areaSegments,
                        SkyBlockArea.UNKNOWN_SKYBLOCK_AREA.id(),
                        now);
    }

    private void reopenActivitySegments(long now) {
        config.targetSegments =
                RotClientCurrentSessionMath.closeSegmentAndOpen(
                        config.targetSegments, config.currentTargetId, now);
        config.areaSegments =
                RotClientCurrentSessionMath.closeAreaSegmentAndOpen(
                        config.areaSegments, config.currentAreaId, now);
    }

    /** Resume only when the pause was entered by {@link #pauseForOffline}. */
    boolean resumeAfterOffline(long now) {
        if (!config.isPaused() || !config.offlineAutoPaused) {
            return false;
        }
        return resume(now);
    }

    /**
     * Bounded activity heartbeat while RUNNING. Debounced persistence via
     * existing dirty flush — not every tick.
     */
    void heartbeat(long now) {
        if (!config.isActive()) {
            return;
        }
        config.cleanShutdown = false;
        long safeNow = Math.max(0L, now);
        if (safeNow - config.lastHeartbeatMillis
                < RotClientCurrentSessionMath.HEARTBEAT_INTERVAL_MILLIS) {
            return;
        }
        config.lastHeartbeatMillis = safeNow;
        markDirty(safeNow);
    }

    /** Session age minus paused time; never negative. See {@link
     * RotClientCurrentSessionMath#activeDurationMillis}. */
    long activeDurationMillis(long now) {
        return RotClientCurrentSessionMath.activeDurationMillis(config, now);
    }

    RotClientSessionFreeze freeze(long now) {
        return RotClientSessionFreeze.fromCurrentSession(config, now);
    }

    /**
     * Archives the immutable freeze before replacing Current Session. A failed
     * archive leaves identity, quantities, and lifecycle untouched.
     */
    boolean startNewSession(
            Predicate<RotClientSessionFreeze> archiveCallback,
            long now) {
        RotClientCurrentSessionConfig prior = config.copy();
        RotClientSessionFreeze frozen =
                RotClientSessionFreeze.fromCurrentSession(prior, now);
        if (archiveCallback != null) {
            try {
                if (!archiveCallback.test(frozen)) {
                    return false;
                }
            } catch (RuntimeException ignored) {
                return false;
            }
        }
        config = RotClientCurrentSessionMath.archiveAndStartNext(prior, now);
        markDirty(now);
        if (saveNow(now)) {
            return true;
        }
        config = prior;
        dirty = true;
        lastMutationMillis = Math.max(0L, now);
        return false;
    }

    /**
     * Aligns target-segment metadata. Never replaces persisted item
     * quantities from an engine snapshot — Current Session is the sole
     * owner of those rows.
     *
     * @deprecated name retained for call-site compatibility; prefer
     *             {@link #refreshFromEngineMetadata}.
     */
    @Deprecated
    void syncFromSnapshot(
            MiningSessionSnapshot snap,
            TrackerSelection selection,
            long now) {
        refreshFromEngineMetadata(snap, selection, null, now);
    }

    void refreshFromEngineMetadata(
            MiningSessionSnapshot snap,
            TrackerSelection selection,
            long now) {
        refreshFromEngineMetadata(snap, selection, null, now);
    }

    /**
     * Updates target segments from the live selection and refreshes each
     * existing row's own Bazaar instant-sell valuation independently.
     * Does not add, remove, or rescale quantities.
     */
    void refreshFromEngineMetadata(
            MiningSessionSnapshot snap,
            TrackerSelection selection,
            MiningSessionPriceBook priceBook,
            long now) {
        if (selection != null
                && selection.id() != null
                && !selection.id().equals(config.currentTargetId)) {
            onTargetChanged(selection.id(), now);
        }
        long eventTs = snap == null
                ? lastSyncedEventTimestamp
                : snap.lastAcceptedEventTimestamp().orElse(
                lastSyncedEventTimestamp);
        refreshPerResourceValuation(priceBook, now);
        lastSyncMillis = now;
        lastSyncedEventTimestamp = eventTs;
    }

    /**
     * Re-resolves each canonical row independently. Never distributes a
     * category total by quantity share. Unresolved rows stay unresolved and
     * are never forced to "0 coins" as a resolved value.
     */
    void refreshPerResourceValuation(
            MiningSessionPriceBook priceBook,
            long now) {
        boolean bookUsable = priceBook != null
                && priceBook.isAvailable()
                && !priceBook.invalidTimestamp()
                && now >= 0L
                && MiningSessionPriceBook.hasValidTimestampOrdering(
                now, priceBook.observedAtMillis())
                && !priceBook.isStale(now);
        if (bookUsable
                && config.lastPriceBookObservedAtMillis
                != priceBook.observedAtMillis()) {
            config.lastPriceBookObservedAtMillis = priceBook.observedAtMillis();
            markDirty(now);
        }
        if (config.items == null || config.items.isEmpty()) {
            return;
        }
        boolean changed = false;
        List<RotClientCurrentSessionConfig.SessionItemRecord> next =
                new ArrayList<>(config.items.size());
        for (RotClientCurrentSessionConfig.SessionItemRecord item
                : config.items) {
            if (item == null) {
                continue;
            }
            if (RotClientCurrentSessionMath.isNativeCoins(item)) {
                double nativeValue = (double) item.quantity();
                next.add(withPrice(
                        item,
                        RotClientCurrentSessionConfig.PriceStatus
                                .RESOLVED_BAZAAR,
                        nativeValue));
                if (item.price()
                        != RotClientCurrentSessionConfig.PriceStatus
                        .RESOLVED_BAZAAR
                        || Double.compare(
                                item.resolvedGrossValue(), nativeValue)
                        != 0) {
                    changed = true;
                }
                continue;
            }
            SessionSourceType source = item.source();
            if (source == SessionSourceType.CURRENCY) {
                next.add(new RotClientCurrentSessionConfig.SessionItemRecord(
                        item.itemId(),
                        item.displayName(),
                        item.quantity(),
                        item.sourceType(),
                        item.miningClass(),
                        item.areaId(),
                        item.known(),
                        RotClientCurrentSessionConfig.PriceStatus.UNSUPPORTED
                                .name(),
                        0.0));
                if (item.price()
                        != RotClientCurrentSessionConfig.PriceStatus.UNSUPPORTED
                        || item.resolvedGrossValue() != 0.0) {
                    changed = true;
                }
                continue;
            }
            Optional<String> productId =
                    CurrentSessionCanonicalIds.bazaarProductId(item.itemId());
            if (productId.isEmpty()) {
                RotClientCurrentSessionConfig.PriceStatus status =
                        item.known()
                                ? RotClientCurrentSessionConfig.PriceStatus
                                .UNSUPPORTED
                                : RotClientCurrentSessionConfig.PriceStatus
                                .UNAVAILABLE;
                next.add(withPrice(item, status, 0.0));
                if (item.price() != status || item.resolvedGrossValue() != 0.0) {
                    changed = true;
                }
                continue;
            }
            if (!bookUsable) {
                // Preserve prior resolved gross when the book is temporarily
                // unavailable; do not invent zeros.
                next.add(item.copy());
                continue;
            }
            Optional<java.math.BigDecimal> unit =
                    priceBook.unitPrice(productId.get());
            if (unit.isEmpty()) {
                next.add(withPrice(
                        item,
                        RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                        0.0));
                if (item.price()
                        != RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE
                        || item.resolvedGrossValue() != 0.0) {
                    changed = true;
                }
                continue;
            }
            double gross = MiningSessionPriceBook.entryValue(
                    unit.get(), item.quantity()).doubleValue();
            next.add(withPrice(
                    item,
                    RotClientCurrentSessionConfig.PriceStatus.RESOLVED_BAZAAR,
                    gross));
            if (item.price()
                    != RotClientCurrentSessionConfig.PriceStatus.RESOLVED_BAZAAR
                    || Double.compare(item.resolvedGrossValue(), gross) != 0) {
                changed = true;
            }
        }
        if (changed) {
            config.items = next;
            markDirty(now);
        }
    }

    /** Clears credited rows and derived counters, preserving identity/lifecycle. */
    boolean clearCreditedItems(long now) {
        boolean hasItems = config.items != null && !config.items.isEmpty();
        boolean hasPowderChests = config.powderChestsOpened > 0L;
        if (!hasItems && !hasPowderChests) {
            return false;
        }
        config.items = new ArrayList<>();
        config.powderChestsOpened = 0L;
        markDirty(now);
        return true;
    }

    boolean shouldSync(long now) {
        return shouldSync(now, lastSyncedEventTimestamp);
    }

    /**
     * Valuation refresh cadence while ACTIVE. Quantity ownership no longer
     * follows engine ledger advances.
     */
    boolean shouldSync(long now, long engineEventTimestamp) {
        if (config.isPaused()) {
            return false;
        }
        return now - lastSyncMillis >= VALUATION_REFRESH_INTERVAL_MILLIS;
    }

    void flushIfDirty() {
        flushIfDirty(System.currentTimeMillis(), false);
    }

    void flushIfDirty(long now, boolean force) {
        if (!dirty) {
            return;
        }
        if (!force
                && now - lastMutationMillis
                < RotClientCurrentSessionStore.SAVE_DEBOUNCE_MILLIS) {
            return;
        }
        if (!force && nextSaveAttemptMillis > 0L
                && now < nextSaveAttemptMillis) {
            return;
        }
        saveNow(now);
    }

    boolean saveNow() {
        return saveNow(System.currentTimeMillis());
    }

    private boolean saveNow(long attemptAtMillis) {
        RotClientCurrentSessionConfig safe = config.copy();
        safe.normalize();
        if (persistenceBlocked) {
            config = safe;
            dirty = true;
            scheduleRetry(attemptAtMillis);
            return false;
        }
        try {
            if (persistence.save(safe)) {
                config = safe;
                dirty = false;
                nextSaveAttemptMillis = 0L;
                persistenceWarning = "";
                persistenceWarningPending = false;
                return true;
            }
        } catch (RuntimeException ignored) {
            // Fabric config dir / I/O may be unavailable in pure unit tests.
            // Keep the in-memory ledger; never wipe quantities on save failure.
        }
        config = safe;
        dirty = true;
        persistenceWarning =
                "Current Session could not be saved. In-memory quantities "
                        + "were retained and persistence will retry.";
        persistenceWarningPending = true;
        scheduleRetry(attemptAtMillis);
        return false;
    }

    private void scheduleRetry(long attemptAtMillis) {
        long safeAttempt = Math.max(0L, attemptAtMillis);
        long backoff = RotClientCurrentSessionStore
                .SAVE_RETRY_BACKOFF_MILLIS;
        nextSaveAttemptMillis = safeAttempt > Long.MAX_VALUE - backoff
                ? Long.MAX_VALUE
                : safeAttempt + backoff;
    }

    private static RotClientCurrentSessionConfig.PriceStatus
            unresolvedMergeStatus(
                    RotClientCurrentSessionConfig.PriceStatus left,
                    RotClientCurrentSessionConfig.PriceStatus right) {
        if (left == RotClientCurrentSessionConfig.PriceStatus.UNSUPPORTED
                || right
                == RotClientCurrentSessionConfig.PriceStatus.UNSUPPORTED) {
            return RotClientCurrentSessionConfig.PriceStatus.UNSUPPORTED;
        }
        if (left == RotClientCurrentSessionConfig.PriceStatus.STALE
                && right == RotClientCurrentSessionConfig.PriceStatus.STALE) {
            return RotClientCurrentSessionConfig.PriceStatus.STALE;
        }
        return RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE;
    }

    private static RotClientCurrentSessionConfig.SessionItemRecord withPrice(
            RotClientCurrentSessionConfig.SessionItemRecord item,
            RotClientCurrentSessionConfig.PriceStatus status,
            double resolvedGrossValue) {
        return new RotClientCurrentSessionConfig.SessionItemRecord(
                item.itemId(),
                item.displayName(),
                item.quantity(),
                item.sourceType(),
                item.miningClass(),
                item.areaId(),
                item.known(),
                status.name(),
                resolvedGrossValue);
    }

    private void noteArea(String areaId) {
        String id = SkyBlockArea.fromId(areaId).id();
        if (config.areasVisited == null) {
            config.areasVisited = new ArrayList<>();
        }
        if (!config.areasVisited.contains(id)) {
            config.areasVisited.add(id);
        }
    }

    private void markDirty(long now) {
        dirty = true;
        lastMutationMillis = Math.max(0L, now);
    }
}
