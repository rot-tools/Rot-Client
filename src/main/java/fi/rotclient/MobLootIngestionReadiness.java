package fi.rotclient;

/** Readiness notes for generic, area-independent MOB loot ingestion. */
final class MobLootIngestionReadiness {
    static final String STATUS = "BOUNDED_LIVE";
    static final boolean LIVE_INGESTION_ENABLED = true;
    static final boolean DIANA_IN_SCOPE = true;

    private MobLootIngestionReadiness() {
    }

    static String report() {
        return "MOB LIVE INGESTION: " + STATUS
                + "\nDiana in generic scope: YES"
                + "\nDomain API ready: YES (SessionSourceType.MOB)"
                + "\nCoverage: melee and player-caused projectile damage,"
                + " Combat/Slayer/Dungeon Sack, action-bar +N lines, rare-drop"
                + " chat, and +N coins inside the kill window. Displayed Magic"
                + " Find is recorded as session context only. Remote-ability"
                + " rewards remain a future evidence channel.";
    }
}
