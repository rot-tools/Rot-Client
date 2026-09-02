package fi.rotclient;

/**
 * Compares Hypixel/vanilla damage-packet entity ids to the local player.
 * Unused packet ids are 0 and must not match a real player.
 */
final class MobLootPlayerCause {
    private MobLootPlayerCause() {
    }

    static boolean matchesIds(int playerId, int causeId, int directId) {
        if (playerId <= 0) {
            return false;
        }
        return causeId == playerId || directId == playerId;
    }
}
