package fi.rotclient;

import net.minecraft.client.Minecraft;

import java.util.OptionalDouble;

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

    /**
     * Speed is available directly from the local player's movement state even
     * when Hypixel does not include a Speed fragment in the action bar.
     */
    void observeClientPlayer(
            Minecraft client) {

        if (client == null
                || client.player == null) {

            return;
        }

        double speed =
                skyBlockSpeed(
                        client.player.getSpeed(),
                        client.player.isSprinting());

        if (!Double.isFinite(speed)
                || speed < 0.0D) {

            return;
        }

        SkyBlockStatBarParser.Stats previous =
                stats;

        stats =
                new SkyBlockStatBarParser.Stats(
                        previous.health(),
                        previous.maxHealth(),
                        previous.defense(),
                        previous.mana(),
                        previous.maxMana(),
                        previous.overflowMana(),
                        OptionalDouble.of(
                                speed),
                        previous.vitality());
    }

    static double skyBlockSpeed(
            float movementSpeed,
            boolean sprinting) {

        if (!Float.isFinite(
                movementSpeed)
                || movementSpeed < 0.0F) {

            return -1.0D;
        }

        double normalized =
                sprinting
                        ? movementSpeed / 1.3D
                        : movementSpeed;

        return Math.max(
                0.0D,
                Math.round(
                        normalized
                                * 1000.0D));
    }

    private void observe(
            String raw,
            boolean actionBar) {

        if (raw == null
                || raw.isBlank()) {

            return;
        }

        lastRaw = raw;

        SkyBlockStatBarParser.Stats parsed =
                actionBar
                        ? SkyBlockStatBarParser.parse(
                                raw)
                        : SkyBlockStatBarParser.parseHudSources(
                                raw);

        stats =
                merge(
                        stats,
                        parsed);
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
                next.health().isPresent()
                        ? next.health()
                        : previous.health(),
                next.maxHealth().isPresent()
                        ? next.maxHealth()
                        : previous.maxHealth(),
                next.defense().isPresent()
                        ? next.defense()
                        : previous.defense(),
                next.mana().isPresent()
                        ? next.mana()
                        : previous.mana(),
                next.maxMana().isPresent()
                        ? next.maxMana()
                        : previous.maxMana(),
                next.overflowMana().isPresent()
                        ? next.overflowMana()
                        : previous.overflowMana(),
                next.speed().isPresent()
                        ? next.speed()
                        : previous.speed(),
                next.vitality().isPresent()
                        ? next.vitality()
                        : previous.vitality());
    }
}