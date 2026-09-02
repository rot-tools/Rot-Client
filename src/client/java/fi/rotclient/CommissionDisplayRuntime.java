package fi.rotclient;

import fi.rotclient.mixin.PlayerTabOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Parses tab-list commission rows for the movable Commission Display HUD.
 * Hypixel mining widgets live in tab-list team prefixes, not always in
 * {@link PlayerInfo#getTabListDisplayName()}, so names are read the same
 * way the overlay draws them.
 */
final class CommissionDisplayRuntime {
    private static List<CommissionDisplayPolicy.Commission> commissions = List.of();

    private CommissionDisplayRuntime() {
    }

    static void clear() {
        commissions = List.of();
        SkyBlockTabSnapshotRuntime.clear();
    }

    static List<CommissionDisplayPolicy.Commission> commissions() {
        return commissions;
    }

    static void tick(Minecraft client) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.commissionDisplayEnabled || client == null) {
            commissions = List.of();
            return;
        }
        commissions = CommissionDisplayPolicy.parseTabLines(
                tabLines(client));
    }

    static List<String> hudLines(QolUtilityConfig qol) {
        List<String> lines = new ArrayList<>();
        lines.add(CommissionDisplayPolicy.formatTitle(qol.commissionDisplayTitle));
        if (commissions.isEmpty()) {
            lines.add(CommissionDisplayPolicy.formatNone(qol.commissionDisplayNone));
            return lines;
        }
        for (CommissionDisplayPolicy.Commission commission : commissions) {
            lines.add(CommissionDisplayPolicy.formatLine(
                    qol.commissionDisplayRow, commission, qol.commissionDisplayColoredPercent));
        }
        return lines;
    }

    static List<String> tabLines(Minecraft client) {
        return SkyBlockTabSnapshotRuntime.lines(
                client, System.currentTimeMillis());
    }

    static List<String> captureTabLines(Minecraft client) {
        List<String> lines = new ArrayList<>();
        PlayerTabOverlay overlay = null;
        if (client.gui != null && client.gui.hud != null) {
            overlay = client.gui.hud.getTabList();
        }
        if (client.getConnection() != null) {
            List<PlayerInfo> infos = new ArrayList<>(client.getConnection().getOnlinePlayers());
            infos.sort(Comparator.comparing(
                    CommissionDisplayRuntime::teamSortKey, String.CASE_INSENSITIVE_ORDER));
            for (PlayerInfo info : infos) {
                appendSplit(lines, displayName(overlay, info));
            }
        }
        if (overlay != null) {
            PlayerTabOverlayAccessor tab = (PlayerTabOverlayAccessor) overlay;
            appendSplit(lines, tab.rotclient$getHeader());
            appendSplit(lines, tab.rotclient$getFooter());
        }
        return lines;
    }

    private static Component displayName(PlayerTabOverlay overlay, PlayerInfo info) {
        if (overlay != null) {
            Component drawn = ((PlayerTabOverlayAccessor) overlay).rotclient$getNameForDisplay(info);
            if (drawn != null) {
                return drawn;
            }
        }
        Component explicit = info.getTabListDisplayName();
        if (explicit != null) {
            return explicit;
        }
        return PlayerTeam.formatNameForTeam(info.getTeam(), Component.literal(""));
    }

    private static String teamSortKey(PlayerInfo info) {
        PlayerTeam team = info.getTeam();
        return team == null ? "" : team.getName();
    }

    private static void appendSplit(List<String> lines, Component component) {
        if (component == null) {
            return;
        }
        String text = component.getString();
        if (text == null || text.isBlank()) {
            return;
        }
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
    }
}
