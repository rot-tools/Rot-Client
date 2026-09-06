package fi.rotclient;

/** Ephemeral readiness for one opened menu, not a cache or item ledger. */
public final class StorageMenuReadinessPolicy {
    private int containerId = -1;
    private int expectedSlots;
    private boolean ready;

    /** Call even when a reopened menu reuses the previous container id. */
    public void beginVisit(int id, int slots) {
        containerId = id;
        expectedSlots = slots;
        ready = false;
    }

    /** A full, matching server snapshot is authoritative, including all-empty pages. */
    public boolean acceptContents(int id, int slots) {
        if (containerId < 0 || id != containerId || expectedSlots <= 36 || slots != expectedSlots) {
            return false;
        }
        ready = true;
        return true;
    }

    public boolean ready() {
        return ready;
    }

    public void reset() {
        beginVisit(-1, 0);
    }
}
