package fi.rotclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Versioned root document for Session History. Sessions are newest-first and
 * bounded by {@link MiningSessionHistoryStore#MAX_RECORDS}.
 */
final class MiningSessionHistoryDocument {
    private final int schemaVersion;
    private final List<MiningSessionHistoryRecord> sessions;

    private MiningSessionHistoryDocument(
            int schemaVersion,
            List<MiningSessionHistoryRecord> sessions) {
        this.schemaVersion = schemaVersion;
        this.sessions = sessions;
    }

    static MiningSessionHistoryDocument empty(int schemaVersion) {
        return new MiningSessionHistoryDocument(
                schemaVersion,
                List.of());
    }

    static MiningSessionHistoryDocument of(
            int schemaVersion,
            List<MiningSessionHistoryRecord> sessions) {
        if (schemaVersion < 1) {
            throw new IllegalArgumentException(
                    "Schema version must be at least 1");
        }
        if (sessions == null) {
            throw new IllegalArgumentException(
                    "Sessions cannot be null");
        }
        if (sessions.size() > MiningSessionHistoryStore.MAX_RECORDS) {
            throw new IllegalArgumentException(
                    "History exceeds maximum retained sessions");
        }
        ArrayList<MiningSessionHistoryRecord> copy = new ArrayList<>();
        for (MiningSessionHistoryRecord record : sessions) {
            if (record == null) {
                throw new IllegalArgumentException(
                        "Session records cannot be null");
            }
            copy.add(record);
        }
        return new MiningSessionHistoryDocument(
                schemaVersion,
                Collections.unmodifiableList(copy));
    }

    int schemaVersion() {
        return schemaVersion;
    }

    List<MiningSessionHistoryRecord> sessions() {
        return sessions;
    }

    int size() {
        return sessions.size();
    }

    boolean isEmpty() {
        return sessions.isEmpty();
    }
}
