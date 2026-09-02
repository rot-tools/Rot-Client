package fi.rotclient;

/**
 * Holds latest SkyBlock action-bar stats for Player Display HUDs.
 */
final class SkyBlockStatTracker {
    private volatile SkyBlockStatBarParser.Stats stats =
            SkyBlockStatBarParser.Stats.empty();
    private volatile String lastRaw = "";

    void observeActionBar(String raw) {
        observe(raw, true);
    }

    void observeHudSources(String raw) {
        observe(raw, false);
    }

    private void observe(String raw, boolean actionBar) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        lastRaw = raw;
        SkyBlockStatBarParser.Stats parsed = actionBar
                ? SkyBlockStatBarParser.parse(raw)
                : SkyBlockStatBarParser.parseHudSources(raw);
        stats = merge(stats, parsed);
    }

    SkyBlockStatBarParser.Stats stats() {
        return stats;
    }

    String lastRaw() {
        return lastRaw;
    }

    private static SkyBlockStatBarParser.Stats merge(
            SkyBlockStatBarParser.Stats previous,
            SkyBlockStatBarParser.Stats next) {
        return new SkyBlockStatBarParser.Stats(
                next.health().isPresent() ? next.health() : previous.health(),
                next.maxHealth().isPresent() ? next.maxHealth() : previous.maxHealth(),
                next.defense().isPresent() ? next.defense() : previous.defense(),
                next.mana().isPresent() ? next.mana() : previous.mana(),
                next.maxMana().isPresent() ? next.maxMana() : previous.maxMana(),
                next.overflowMana().isPresent()
                        ? next.overflowMana()
                        : previous.overflowMana(),
                next.speed().isPresent() ? next.speed() : previous.speed(),
                next.vitality().isPresent() ? next.vitality() : previous.vitality());
    }
}
