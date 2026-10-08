package fi.rotclient;

/** Bounded observation age, independent of the actual game's day/rollover phase. */
final class SkyMallObservationPolicy {
    // Preserves the existing HUD cache's maximum age; this is not a remaining-buff timer.
    static final long MAX_OBSERVATION_AGE_MILLIS = 1_200_000L;

    static final class State {
        private MiningLeftoverPolicy.SkyMall current;
        private long observedAt;
        private String lastTabPerk = "";
        private Object guiMenu;
        private String lastGuiPerk = "";

        void observeTab(MiningLeftoverPolicy.SkyMall observation, long now) {
            if (observation == null) return;
            String perk = key(observation);
            if (perk.equals(lastTabPerk)) return;
            lastTabPerk = perk;
            accept(observation, now);
        }

        void observeGui(Object menu, MiningLeftoverPolicy.SkyMall observation, long now) {
            if (menu == null) {
                guiMenu = null;
                lastGuiPerk = "";
                return;
            }
            if (observation == null) return; // An unpopulated tooltip is not a new selection.
            String perk = key(observation);
            if (menu == guiMenu && perk.equals(lastGuiPerk)) return;
            guiMenu = menu;
            lastGuiPerk = perk;
            accept(observation, now);
        }

        void observeChat(MiningLeftoverPolicy.SkyMall observation, long now) {
            if (observation != null) accept(observation, now);
        }

        private void accept(MiningLeftoverPolicy.SkyMall observation, long now) {
            if (now < 0) return;
            current = observation;
            observedAt = now;
        }

        MiningLeftoverPolicy.SkyMall current(long now) {
            if (current != null && (now < observedAt || now - observedAt >= MAX_OBSERVATION_AGE_MILLIS))
                invalidate();
            return current;
        }

        void invalidate() {
            current = null;
            observedAt = 0;
            // Keep passive fingerprints: an unchanged cached tooltip/tab must not resurrect it.
        }

        void clear() {
            invalidate();
            lastTabPerk = "";
            guiMenu = null;
            lastGuiPerk = "";
        }

        private static String key(MiningLeftoverPolicy.SkyMall observation) {
            return CommissionDisplayPolicy.normalizeLine(observation.perk()).toLowerCase(java.util.Locale.ROOT);
        }
    }

    private SkyMallObservationPolicy() { }
}
