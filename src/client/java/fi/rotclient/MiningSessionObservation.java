package fi.rotclient;

record MiningSessionObservation(
        MiningSessionResource resource,
        long quantity,
        EvidenceType evidenceType,
        TrackerSelection selectedTracker,
        long selectionEpoch,
        long observedAtMillis,
        String eventId,
        String correlationId,
        String sourceName,
        String diagnosticText) {
    static final int MAX_DIAGNOSTIC_TEXT_LENGTH = 512;

    MiningSessionObservation {
        if (resource == null) {
            throw new IllegalArgumentException(
                    "Resource cannot be null");
        }
        if (evidenceType == null) {
            throw new IllegalArgumentException(
                    "Evidence type cannot be null");
        }
        if (selectedTracker == null) {
            throw new IllegalArgumentException(
                    "Selected tracker cannot be null");
        }
        if (selectionEpoch < 0L) {
            throw new IllegalArgumentException(
                    "Selection epoch cannot be negative");
        }
        if (observedAtMillis < 0L) {
            throw new IllegalArgumentException(
                    "Observation timestamp cannot be negative");
        }

        eventId = optionalText(eventId);
        correlationId = optionalText(correlationId);
        sourceName = optionalText(sourceName);
        diagnosticText = optionalText(diagnosticText);

        if (diagnosticText != null
                && diagnosticText.length()
                > MAX_DIAGNOSTIC_TEXT_LENGTH) {
            throw new IllegalArgumentException(
                    "Diagnostic text exceeds the bounded length");
        }
    }

    enum EvidenceType {
        DIRECT_BREAK,
        PRISTINE_MESSAGE,
        SACK_CHANGE,
        INVENTORY_CHANGE,
        CHEST_OPEN,
        CHEST_REWARD_MESSAGE,
        CHEST_SACK_CONFIRMATION,
        BAZAAR_PURCHASE,
        MANUAL_TRANSFER,
        UNKNOWN
    }

    private static String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
