// SPDX-License-Identifier: MIT
// AutoCommands 2.2.0, copyright 2026 Pickaxe Popup contributors.
package fi.rotclient.rebuild.illegal.autocommands;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.PlayerTeam;

import java.util.Locale;

final class SkyBlockLocation {
    private SkyBlockLocation() { }

    static WarpAttempt.Location read(ClientPacketListener listener) {
        var scoreboard = listener.scoreboard();
        var sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        String area = null;
        String server = null;
        if (sidebar != null && WarpFeedback.clean(sidebar.getDisplayName().getString()).toUpperCase(Locale.ROOT).contains("SKYBLOCK")) {
            for (var entry : scoreboard.listPlayerScores(sidebar)) {
                if (entry.isHidden()) continue;
                String line = PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()), entry.ownerName()).getString();
                if (area == null) area = WarpFeedback.area(line);
                if (server == null) server = WarpFeedback.server(line);
            }
        }
        String island = null;
        for (var info : listener.getListedOnlinePlayers()) {
            var display = info.getTabListDisplayName();
            if (display == null) continue;
            for (String line : display.getString().split("\\R")) {
                island = WarpFeedback.island(line);
                if (island != null) break;
            }
            if (island != null) break;
        }
        return new WarpAttempt.Location(area, island, server);
    }
}
