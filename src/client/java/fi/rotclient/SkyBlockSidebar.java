package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * Plain text of the SkyBlock scoreboard sidebar, used for location checks.
 */
final class SkyBlockSidebar {
    private SkyBlockSidebar() {
    }

    static String text() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) {
            return "";
        }
        Scoreboard scoreboard = client.level.getScoreboard();
        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        if (sidebar.getDisplayName() != null) {
            out.append(sidebar.getDisplayName().getString()).append('\n');
        }
        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
            Component name = PlayerTeam.formatNameForTeam(
                    scoreboard.getPlayersTeam(entry.owner()), entry.ownerName());
            if (name != null) {
                out.append(name.getString()).append('\n');
            }
        }
        return out.toString();
    }
}
