package fi.rotclient;

/**
 * Controls how often the custom scoreboard rebuilds its logical rows.
 *
 * <p>Rendering still occurs every frame. Only sidebar/tab parsing and row
 * composition are rate-limited.
 */
final class CustomScoreboardRefreshPolicy {
    static final long RECOMPOSE_INTERVAL_MILLIS = 50L;

    private CustomScoreboardRefreshPolicy() {
    }

    static boolean shouldRecompose(
            long nowMillis,
            long lastComposeMillis,
            boolean hasRows) {

        if (!hasRows) {
            return true;
        }

        if (lastComposeMillis <= 0L) {
            return true;
        }

        if (nowMillis < lastComposeMillis) {
            return true;
        }

        return nowMillis - lastComposeMillis
                >= RECOMPOSE_INTERVAL_MILLIS;
    }
}