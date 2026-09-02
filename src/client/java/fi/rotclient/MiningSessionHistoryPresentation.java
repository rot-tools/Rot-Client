package fi.rotclient;

import java.util.List;
import java.util.Optional;

/**
 * Dashboard presentation helpers for Session History list and detail states.
 */
final class MiningSessionHistoryPresentation {
    private final boolean available;
    private final String warning;
    private final boolean empty;
    private final boolean saveEnabled;
    private final boolean clearEnabled;
    private final boolean detailOpen;
    private final List<MiningSessionHistoryRecord> sessions;
    private final Optional<MiningSessionHistoryRecord> selected;
    private final MiningSessionAnalyticsViewModel selectedViewModel;
    private final String emptyStateMessage;
    private final boolean awaitingClearConfirm;

    private MiningSessionHistoryPresentation(
            boolean available,
            String warning,
            boolean empty,
            boolean saveEnabled,
            boolean clearEnabled,
            boolean detailOpen,
            List<MiningSessionHistoryRecord> sessions,
            Optional<MiningSessionHistoryRecord> selected,
            MiningSessionAnalyticsViewModel selectedViewModel,
            String emptyStateMessage,
            boolean awaitingClearConfirm) {
        this.available = available;
        this.warning = warning;
        this.empty = empty;
        this.saveEnabled = saveEnabled;
        this.clearEnabled = clearEnabled;
        this.detailOpen = detailOpen;
        this.sessions = sessions;
        this.selected = selected;
        this.selectedViewModel = selectedViewModel;
        this.emptyStateMessage = emptyStateMessage;
        this.awaitingClearConfirm = awaitingClearConfirm;
    }

    static MiningSessionHistoryPresentation from(
            MiningSessionHistoryController controller,
            MiningSessionAnalyticsViewModel currentAnalytics) {
        boolean available = controller.available();
        List<MiningSessionHistoryRecord> sessions =
                controller.sessionsNewestFirst();
        Optional<MiningSessionHistoryRecord> selected =
                controller.selectedRecord();
        boolean awaiting = controller.pendingClearToken() != null;
        String emptyMessage;
        if (!available) {
            emptyMessage = controller.loadWarning().isBlank()
                    ? "Session History is unavailable."
                    : controller.loadWarning();
        } else if (sessions.isEmpty()) {
            emptyMessage = "No saved sessions yet.";
        } else {
            emptyMessage = null;
        }
        boolean saveEnabled = available
                && controller.hasUnsavedStoppedSnapshot();
        return new MiningSessionHistoryPresentation(
                available,
                controller.loadWarning(),
                sessions.isEmpty(),
                saveEnabled,
                available && !sessions.isEmpty(),
                selected.isPresent(),
                sessions,
                selected,
                selected.map(MiningSessionHistoryRecord::toViewModel)
                        .orElse(MiningSessionAnalyticsViewModel.notStarted()),
                emptyMessage,
                awaiting);
    }

    boolean available() {
        return available;
    }

    String warning() {
        return warning;
    }

    boolean empty() {
        return empty;
    }

    boolean saveEnabled() {
        return saveEnabled;
    }

    boolean clearEnabled() {
        return clearEnabled;
    }

    boolean detailOpen() {
        return detailOpen;
    }

    List<MiningSessionHistoryRecord> sessions() {
        return sessions;
    }

    Optional<MiningSessionHistoryRecord> selected() {
        return selected;
    }

    MiningSessionAnalyticsViewModel selectedViewModel() {
        return selectedViewModel;
    }

    String emptyStateMessage() {
        return emptyStateMessage;
    }

    boolean awaitingClearConfirm() {
        return awaitingClearConfirm;
    }
}
