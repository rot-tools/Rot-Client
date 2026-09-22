package fi.rotclient;

/**
 * Turns a noisy stream of context observations into "the context changed"
 * events for the automatic profile switcher.
 *
 * Two rules keep switching predictable:
 *
 * 1. A context must be observed continuously before it counts, so crossing a
 *    border or a half-rendered sidebar never triggers a switch. A recognised
 *    place is positive evidence and only needs {@link #STABLE_MILLIS} (two
 *    sidebar reads in a row). {@link AutoProfileContext#OTHER} is the absence
 *    of evidence, which is also what a half-loaded sidebar looks like, so it
 *    has to hold for the longer {@link #OTHER_STABLE_MILLIS}.
 * 2. It reports each stable context once (edge-triggered, not level-triggered).
 *    If the player manually picks another profile while staying in the same
 *    context, nothing fights that choice; the next context change applies
 *    the rules again.
 *
 * Not thread-safe. Called from the client tick only.
 */
final class AutoProfileSwitchTracker {
    /** The sidebar is read once a second, so this is two reads in a row. */
    static final long STABLE_MILLIS = 1_000L;
    static final long OTHER_STABLE_MILLIS = 3_000L;

    private AutoProfileContext candidate;
    private long candidateSinceMillis;
    private AutoProfileContext reported;

    /**
     * Feeds one observation.
     *
     * @param observed the classified context, or null when nothing reliable
     *                 was seen (not in SkyBlock, sidebar missing)
     * @return the newly stable context exactly once, otherwise null
     */
    AutoProfileContext observe(AutoProfileContext observed, long nowMillis) {
        if (observed == null) {
            candidate = null;
            return null;
        }
        if (observed != candidate || nowMillis < candidateSinceMillis) {
            candidate = observed;
            candidateSinceMillis = nowMillis;
        }
        long hold = candidate == AutoProfileContext.OTHER
                ? OTHER_STABLE_MILLIS
                : STABLE_MILLIS;
        if (candidate == reported
                || nowMillis - candidateSinceMillis < hold) {
            return null;
        }
        reported = candidate;
        return reported;
    }

    /** Forgets everything; call on world change so the next area applies. */
    void reset() {
        candidate = null;
        candidateSinceMillis = 0L;
        reported = null;
    }

    /** The last context reported as stable, or null. */
    AutoProfileContext lastReported() {
        return reported;
    }
}
