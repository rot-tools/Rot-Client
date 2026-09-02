package fi.rotclient;

/**
 * Dashboard presentation helpers for Session Analytics button states and
 * empty-state messaging. Derived from the shared view model plus the
 * persistent Current Session state.
 */
final class MiningSessionAnalyticsPresentation {
    private final MiningSessionAnalyticsViewModel viewModel;
    private final boolean startEnabled;
    private final boolean stopEnabled;
    private final boolean pauseEnabled;
    private final boolean resumeEnabled;
    private final boolean startNewEnabled;
    private final boolean resetEnabled;
    private final boolean copyEnabled;
    private final String emptyStateMessage;
    private final String statusBadge;
    private final boolean currentRunning;

    private MiningSessionAnalyticsPresentation(
            MiningSessionAnalyticsViewModel viewModel,
            boolean startEnabled,
            boolean stopEnabled,
            boolean pauseEnabled,
            boolean resumeEnabled,
            boolean startNewEnabled,
            boolean resetEnabled,
            boolean copyEnabled,
            String emptyStateMessage,
            String statusBadge,
            boolean currentRunning) {
        this.viewModel = viewModel;
        this.startEnabled = startEnabled;
        this.stopEnabled = stopEnabled;
        this.pauseEnabled = pauseEnabled;
        this.resumeEnabled = resumeEnabled;
        this.startNewEnabled = startNewEnabled;
        this.resetEnabled = resetEnabled;
        this.copyEnabled = copyEnabled;
        this.emptyStateMessage = emptyStateMessage;
        this.statusBadge = statusBadge;
        this.currentRunning = currentRunning;
    }

    static MiningSessionAnalyticsPresentation from(
            MiningSessionAnalyticsViewModel viewModel) {
        return from(viewModel, null);
    }

    static MiningSessionAnalyticsPresentation from(
            MiningSessionAnalyticsViewModel viewModel,
            RotClientCurrentSessionConfig currentSession) {
        MiningSessionAnalyticsViewModel safe = viewModel == null
                ? MiningSessionAnalyticsViewModel.notStarted()
                : viewModel;
        boolean engineActive = safe.sessionState()
                == MiningSessionAnalyticsViewModel.SessionState.ACTIVE;
        boolean viewable = safe.hasViewableSession();

        if (currentSession == null) {
            // Legacy engine-only presentation (unit tests / direct controller).
            return new MiningSessionAnalyticsPresentation(
                    safe,
                    !engineActive,
                    engineActive,
                    engineActive,
                    !engineActive && viewable,
                    viewable,
                    viewable,
                    viewable,
                    resolveEmptyState(safe, false),
                    switch (safe.sessionState()) {
                        case NOT_STARTED -> "Not started";
                        case ACTIVE -> "Active";
                        case STOPPED -> "Stopped";
                    },
                    engineActive);
        }

        boolean currentActive = currentSession.isActive();
        boolean currentPaused = currentSession.isPaused();
        viewable = viewable || true;
        String badge = currentPaused
                ? "CURRENT · PAUSED"
                : "CURRENT · RUNNING";
        return new MiningSessionAnalyticsPresentation(
                safe,
                currentPaused,
                currentActive && engineActive,
                currentActive,
                currentPaused,
                true,
                viewable,
                viewable,
                resolveEmptyState(safe, currentPaused),
                badge,
                currentActive && !currentPaused);
    }

    private static String resolveEmptyState(
            MiningSessionAnalyticsViewModel model,
            boolean currentPaused) {
        if (currentPaused) {
            return "Current Session paused. Resume to continue collection.";
        }
        if (model.sessionState()
                == MiningSessionAnalyticsViewModel.SessionState.NOT_STARTED) {
            return "Current Session ready — collecting when ACTIVE.";
        }
        if (model.entryCount() == 0
                && model.sessionState()
                == MiningSessionAnalyticsViewModel.SessionState.ACTIVE) {
            return "Session active, no credited entries yet.";
        }
        if (model.entryCount() == 0
                && model.sessionState()
                == MiningSessionAnalyticsViewModel.SessionState.STOPPED) {
            return "Final session contains no credited entries.";
        }
        if (!model.priceBookAvailable()) {
            return "Price book unavailable.";
        }
        if (model.priceBookStale()) {
            return "Price book stale.";
        }
        return null;
    }

    MiningSessionAnalyticsViewModel viewModel() {
        return viewModel;
    }

    boolean startEnabled() {
        return startEnabled;
    }

    boolean stopEnabled() {
        return stopEnabled;
    }

    boolean pauseEnabled() {
        return pauseEnabled;
    }

    boolean resumeEnabled() {
        return resumeEnabled;
    }

    boolean startNewEnabled() {
        return startNewEnabled;
    }

    boolean resetEnabled() {
        return resetEnabled;
    }

    boolean copyEnabled() {
        return copyEnabled;
    }

    String emptyStateMessage() {
        return emptyStateMessage;
    }

    String statusBadge() {
        return statusBadge;
    }

    boolean currentRunning() {
        return currentRunning;
    }
}
