package fi.rotclient;

/**
 * Applies already validated gemstone mining events to persistent tracker
 * state. Detection, packet parsing and event correlation remain outside this
 * class.
 */
final class GemstoneLiveAccounting {
    private GemstoneLiveAccounting() {
    }

    static boolean isActive(
            TrackerConfig config) {
        if (config == null
                || !config.enabled) {
            return false;
        }

        TrackerSelection selection =
                config.selectedSelection();

        return selection != null
                && selection.isGemstone();
    }

    static boolean recordBlock(
            TrackerConfig config,
            GemstoneType gemstone,
            long epochMillis) {
        if (!tracks(
                config,
                gemstone)
                || epochMillis < 0L) {
            return false;
        }

        config.gemstoneState(
                gemstone)
                .recordBlock(
                        epochMillis);

        return true;
    }

    static boolean recordGain(
            TrackerConfig config,
            GemstoneType gemstone,
            GemstoneTier tier,
            long amount) {
        if (!tracks(
                config,
                gemstone)
                || tier == null
                || amount <= 0L) {
            return false;
        }

        config.gemstoneState(
                gemstone)
                .recordGain(
                        tier,
                        amount);

        return true;
    }

    private static boolean tracks(
            TrackerConfig config,
            GemstoneType gemstone) {
        if (!isActive(
                config)
                || gemstone == null) {
            return false;
        }

        return config.selectedGemstone()
                == gemstone;
    }
}
