package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.List;

/**
 * Plain text of the SkyBlock scoreboard sidebar, used for location checks.
 */
final class SkyBlockSidebar {
    // Eight features ask for this text each tick, and every ask rebuilt the whole scoreboard.
    private static final long CACHE_NANOS = 50_000_000L;
    private static String cachedText = "";
    private static Object cachedLevel;
    private static long cachedAtNanos;

    private SkyBlockSidebar() {
    }

    static String text() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) {
            return "";
        }
        long now = System.nanoTime();
        if (client.level == cachedLevel && now - cachedAtNanos < CACHE_NANOS) {
            return cachedText;
        }
        cachedText = build(client);
        cachedLevel = client.level;
        cachedAtNanos = now;
        return cachedText;
    }

    private static String build(Minecraft client) {
        Scoreboard scoreboard = client.level.getScoreboard();
        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        if (sidebar.getDisplayName() != null) {
            out.append(sidebar.getDisplayName().getString()).append('\n');
        }
        // listPlayerScores has no order; the lines are put in the order the game draws them.
        List<CustomScoreboardLines.SidebarEntry> entries = new ArrayList<>();
        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
            if (entry.isHidden()) {
                continue;
            }
            Component name = PlayerTeam.formatNameForTeam(
                    scoreboard.getPlayersTeam(entry.owner()), entry.ownerName());
            if (name != null) {
                entries.add(new CustomScoreboardLines.SidebarEntry(
                        entry.owner(), entry.value(), name.getString()));
            }
        }
        for (String line : CustomScoreboardLines.orderSidebar(entries)) {
            out.append(line).append('\n');
        }
        return out.toString();
    }
}
