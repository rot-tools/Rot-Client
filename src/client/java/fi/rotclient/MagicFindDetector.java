package fi.rotclient;

import fi.rotclient.mixin.PlayerTabOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.OptionalInt;

/**
 * Reads the displayed Magic Find total from live tab/sidebar text and
 * publishes it to {@link PlayerStateService}. Never adds gear bonuses on top.
 */
final class MagicFindDetector {
    private final PlayerStateService playerState;
    private int ticks;

    MagicFindDetector(PlayerStateService playerState) {
        this.playerState = playerState;
    }

    void tick(Minecraft client) {
        if (client == null || playerState == null) {
            return;
        }
        if (++ticks % 20 != 0) {
            return;
        }
        OptionalInt best = OptionalInt.empty();
        if (client.gui != null && client.gui.hud != null) {
            PlayerTabOverlayAccessor tab =
                    (PlayerTabOverlayAccessor) client.gui.hud.getTabList();
            best = max(best, parseComponent(tab.rotclient$getHeader()));
            best = max(best, parseComponent(tab.rotclient$getFooter()));
        }
        if (client.level != null) {
            Scoreboard scoreboard = client.level.getScoreboard();
            Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (sidebar != null) {
                best = max(best, parseComponent(sidebar.getDisplayName()));
                for (PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
                    Component line = PlayerTeam.formatNameForTeam(
                            scoreboard.getPlayersTeam(entry.owner()),
                            entry.ownerName());
                    best = max(best, parseComponent(line));
                }
            }
        }
        if (best.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        playerState.publish(new PlayerStateService.Snapshot(
                best.getAsInt(),
                playerState.latest().petLuck(),
                playerState.latest().miningFortune(),
                playerState.latest().miningSpeed(),
                playerState.latest().pristine(),
                playerState.latest().breakingPower(),
                SkyBlockAreaDetector.detect(),
                playerState.latest().heldItemId(),
                0L,
                now,
                PlayerStateService.Confidence.AUTHORITATIVE));
    }

    void inspectMessage(String plain, long nowMillis) {
        OptionalInt parsed = MagicFindParser.parse(plain);
        if (parsed.isEmpty() || playerState == null) {
            return;
        }
        playerState.publish(new PlayerStateService.Snapshot(
                parsed.getAsInt(),
                playerState.latest().petLuck(),
                playerState.latest().miningFortune(),
                playerState.latest().miningSpeed(),
                playerState.latest().pristine(),
                playerState.latest().breakingPower(),
                SkyBlockAreaDetector.detect(),
                playerState.latest().heldItemId(),
                0L,
                nowMillis,
                PlayerStateService.Confidence.OBSERVED));
    }

    private static OptionalInt parseComponent(Component component) {
        if (component == null) {
            return OptionalInt.empty();
        }
        return MagicFindParser.parse(component.getString());
    }

    private static OptionalInt max(OptionalInt left, OptionalInt right) {
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty()) {
            return left;
        }
        return OptionalInt.of(Math.max(left.getAsInt(), right.getAsInt()));
    }
}
