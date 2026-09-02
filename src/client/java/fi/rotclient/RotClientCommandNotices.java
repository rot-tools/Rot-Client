package fi.rotclient;

/**
 * Alias-specific deprecation notices for legacy command roots.
 */
final class RotClientCommandNotices {
    private RotClientCommandNotices() {
    }

    static String deprecationNotice(String legacyAlias) {
        if (legacyAlias == null || legacyAlias.isBlank()) {
            return "";
        }
        return switch (legacyAlias) {
            case "miningui" ->
                    "Rot Client: /miningui is deprecated; use /rot.";
            case "rotclient" ->
                    "Rot Client: /rotclient is deprecated; use /rot.";
            case "MiningTracker" ->
                    "Rot Client: /MiningTracker is deprecated; use /rot.";
            default ->
                    "Rot Client: /" + legacyAlias
                            + " is deprecated; use /rot.";
        };
    }
}
