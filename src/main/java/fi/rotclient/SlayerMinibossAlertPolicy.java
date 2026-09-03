package fi.rotclient;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Miniboss alerts follow the local player's Hypixel spawn chat, not nearby
 * holograms. Other players' minibosses do not send that line.
 */
public final class SlayerMinibossAlertPolicy {
    public static final long ANNOUNCE_COOLDOWN_MILLIS = 2_000L;
    private static final Pattern SPAWN_CHAT = Pattern.compile(
            "(?i)^SLAYER MINI-BOSS\\s+(.+?)\\s+has spawned!?$");

    private SlayerMinibossAlertPolicy() {
    }

    public static boolean isSpawnChat(String raw) {
        return SPAWN_CHAT.matcher(SlayerPolicy.normalize(raw)).matches();
    }

    public static Optional<SlayerPolicy.EntityDescriptor> spawnFromChat(String raw) {
        Matcher matcher = SPAWN_CHAT.matcher(SlayerPolicy.normalize(raw));
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return SlayerPolicy.classifyTag(matcher.group(1), "");
    }

    public static boolean shouldAnnounce(
            boolean enabled,
            boolean spawnChat,
            long nowMillis,
            long lastAnnounceAtMillis) {
        if (!enabled || !spawnChat) {
            return false;
        }
        return nowMillis - lastAnnounceAtMillis >= ANNOUNCE_COOLDOWN_MILLIS;
    }

    /**
     * Nearby hosts are not a spawn source. A friend's Voidling next to you
     * during your own quest must not alert.
     */
    public static boolean shouldAnnounceNearbyHost(
            boolean owned,
            boolean localQuestSameFamily) {
        return false;
    }
}
