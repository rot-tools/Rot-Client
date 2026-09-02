package fi.rotclient;

import java.util.Optional;

/**
 * Shared Session Analytics lifecycle and presentation controller used by
 * commands and dashboard controls. Does not mutate live tracker state,
 * TrackerStore, or Bazaar caches, and never triggers networking.
 */
final class MiningSessionAnalyticsController {
    enum StartResult {
        STARTED,
        ALREADY_ACTIVE,
        PERSISTENCE_BLOCKED
    }

    enum StopResult {
        STOPPED,
        ALREADY_STOPPED,
        NOTHING_TO_STOP
    }

    enum ResetResult {
        CLEARED,
        NOTHING_TO_CLEAR
    }

    enum CopyResult {
        COPIED,
        NOTHING_TO_COPY,
        FAILED
    }

    @FunctionalInterface
    interface ClipboardAccess {
        void copy(String text) throws Exception;
    }

    @FunctionalInterface
    interface LiveBaselineProvider {
        MiningSessionParity.LiveBaseline baseline(
                TrackerSelection selection,
                long capturedAtMillis);
    }

    @FunctionalInterface
    interface Clock {
        long nowMillis();
    }

    private final MiningSessionEngine engine;
    private final MiningSessionAnalyticsProjector projector;
    private final MiningSessionSummaryFormatter formatter;
    private final ClipboardAccess clipboard;
    private final LiveBaselineProvider baselineProvider;
    private final Clock clock;
    private boolean actionInFlight;

    MiningSessionAnalyticsController(
            MiningSessionEngine engine,
            ClipboardAccess clipboard,
            LiveBaselineProvider baselineProvider,
            Clock clock) {
        if (engine == null
                || clipboard == null
                || baselineProvider == null
                || clock == null) {
            throw new IllegalArgumentException(
                    "Analytics controller dependencies cannot be null");
        }
        this.engine = engine;
        this.projector = new MiningSessionAnalyticsProjector();
        this.formatter = new MiningSessionSummaryFormatter();
        this.clipboard = clipboard;
        this.baselineProvider = baselineProvider;
        this.clock = clock;
    }

    MiningSessionAnalyticsProjector projector() {
        return projector;
    }

    MiningSessionSummaryFormatter formatter() {
        return formatter;
    }

    MiningSessionAnalyticsViewModel viewModel() {
        return projector.project(engine.snapshot(clock.nowMillis()));
    }

    /**
     * Prefer this when Current Session is the canonical OTHER_MINED source.
     */
    MiningSessionAnalyticsViewModel viewModel(
            RotClientCurrentSessionConfig currentSession) {
        return projector.project(
                engine.snapshot(clock.nowMillis()),
                currentSession);
    }

    MiningSessionAnalyticsPresentation presentation() {
        return MiningSessionAnalyticsPresentation.from(viewModel());
    }

    /**
     * Compact OTHER_MINED aggregate for the Mining Tracker HUD.
     * When a Current Session is supplied, quantities come from that canonical
     * ledger (same source as live HUD OTHERS).
     */
    MiningHudOtherSummary hudOtherSummary() {
        return hudOtherSummary(null);
    }

    MiningHudOtherSummary hudOtherSummary(
            RotClientCurrentSessionConfig currentSession) {
        if (currentSession != null) {
            return RotClientCurrentSessionMath.toHudOtherSummary(currentSession);
        }
        Optional<MiningSessionSnapshot> snapshot =
                engine.snapshot(clock.nowMillis());
        if (snapshot.isEmpty()) {
            return MiningHudOtherSummary.analyticsInactive();
        }
        MiningSessionAnalyticsViewModel model = projector.project(snapshot.get());
        if (!model.hasViewableSession()) {
            return MiningHudOtherSummary.analyticsInactive();
        }
        long quantity = 0L;
        for (MiningSessionAnalyticsViewModel.ResourceQuantity entry
                : model.otherMinedQuantities().values()) {
            quantity = Math.addExact(quantity, entry.quantity());
        }
        MiningSessionValuation valuation = snapshot.get().valuation();
        if (valuation == null) {
            valuation = MiningSessionValuation.unavailable();
        }
        int unresolvedOther = valuation.unresolvedEntryCount(
                MiningSessionCategory.OTHER_MINED);
        double resolvedGross = model.otherMinedValue() == null
                ? 0.0
                : model.otherMinedValue().doubleValue();
        return MiningHudOtherSummary.available(
                quantity,
                resolvedGross,
                unresolvedOther);
    }

    String statusText() {
        return formatter.format(viewModel());
    }

    String statusText(RotClientCurrentSessionConfig currentSession) {
        return formatter.format(viewModel(currentSession));
    }

    StartResult start(
            boolean trackerEnabled,
            TrackerSelection selection) {
        if (!beginAction()) {
            return engine.isDiagnosticsActive()
                    ? StartResult.ALREADY_ACTIVE
                    : StartResult.STARTED;
        }
        try {
            if (engine.isDiagnosticsActive()) {
                return StartResult.ALREADY_ACTIVE;
            }
            long now = clock.nowMillis();
            engine.onDiagnosticStart(
                    trackerEnabled,
                    selection,
                    baselineProvider.baseline(selection, now),
                    now);
            return StartResult.STARTED;
        } finally {
            endAction();
        }
    }

    StopResult stop() {
        if (!beginAction()) {
            return engine.isDiagnosticsActive()
                    ? StopResult.STOPPED
                    : StopResult.ALREADY_STOPPED;
        }
        try {
            if (!engine.isDiagnosticsActive()) {
                return engine.hasRetainedFinalSnapshot()
                        || engine.hasSessionIdentity()
                        ? StopResult.ALREADY_STOPPED
                        : StopResult.NOTHING_TO_STOP;
            }
            engine.onDiagnosticStop(clock.nowMillis());
            return StopResult.STOPPED;
        } finally {
            endAction();
        }
    }

    ResetResult reset() {
        if (!beginAction()) {
            return ResetResult.CLEARED;
        }
        try {
            MiningSessionAnalyticsViewModel before = viewModel();
            if (!before.hasViewableSession()
                    && !engine.isDiagnosticsActive()
                    && !engine.hasRetainedFinalSnapshot()
                    && !engine.hasSessionIdentity()) {
                return ResetResult.NOTHING_TO_CLEAR;
            }
            engine.clearAnalyticsSession(clock.nowMillis());
            return ResetResult.CLEARED;
        } finally {
            endAction();
        }
    }

    CopyResult copySummary() {
        return copySummary(null);
    }

    CopyResult copySummary(RotClientCurrentSessionConfig currentSession) {
        if (!beginAction()) {
            return CopyResult.FAILED;
        }
        try {
            MiningSessionAnalyticsViewModel model = currentSession == null
                    ? viewModel()
                    : viewModel(currentSession);
            if (!model.hasViewableSession()) {
                return CopyResult.NOTHING_TO_COPY;
            }
            clipboard.copy(formatter.format(model));
            return CopyResult.COPIED;
        } catch (Exception ignored) {
            return CopyResult.FAILED;
        } finally {
            endAction();
        }
    }

    private boolean beginAction() {
        if (actionInFlight) {
            return false;
        }
        actionInFlight = true;
        return true;
    }

    private void endAction() {
        actionInFlight = false;
    }
}
