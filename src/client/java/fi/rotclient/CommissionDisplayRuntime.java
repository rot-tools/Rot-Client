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
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Parses tab-list commission rows for the movable Commission Display HUD.
 * Hypixel mining widgets live in tab-list team prefixes, not always in
 * {@link PlayerInfo#getTabListDisplayName()}, so names are read the same
 * way the overlay draws them.
 */
final class CommissionDisplayRuntime {
    private static final Pattern LINE_BREAK = Pattern.compile("\\R");
    private static List<CommissionDisplayPolicy.Commission> commissions = List.of();
    // The tab snapshot is rebuilt every 250 ms but this ran every tick: the same lines were
    // parsed a dozen times over. The parse is redone only when a new snapshot arrives.
    private static List<String> parsedLines;
    private static List<CommissionDisplayPolicy.Commission> parsed = List.of();
    private static long lastSeenMillis;
    private static List<String> hudCache = List.of();
    private static List<CommissionDisplayPolicy.Commission> hudCacheFor;
    private static String hudTitle;
    private static String hudNone;
    private static String hudRow;
    private static boolean hudColored;

    private CommissionDisplayRuntime() {
    }

    static void clear() {
        commissions = List.of();
        parsed = List.of();
        parsedLines = null;
        lastSeenMillis = 0L;
        hudCacheFor = null;
        SkyBlockTabSnapshotRuntime.clear();
    }

    static List<CommissionDisplayPolicy.Commission> commissions() {
        return commissions;
    }

    static void tick(Minecraft client) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.commissionDisplayEnabled || client == null) {
            commissions = List.of();
            parsedLines = null;
            return;
        }
        long now = System.currentTimeMillis();
        List<String> lines = tabLines(client);
        if (lines != parsedLines) {
            parsedLines = lines;
            parsed = CommissionDisplayPolicy.parseTabLines(lines);
            if (!parsed.isEmpty()) {
                lastSeenMillis = now;
            }
        }
        // A refresh that briefly loses the widget keeps the last list up instead of flashing
        // "No commissions available!".
        commissions = CommissionDisplayPolicy.retain(parsed, commissions, lastSeenMillis, now);
    }

    static List<String> hudLines(QolUtilityConfig qol) {
        if (commissions == hudCacheFor
                && qol.commissionDisplayColoredPercent == hudColored
                && Objects.equals(qol.commissionDisplayTitle, hudTitle)
                && Objects.equals(qol.commissionDisplayNone, hudNone)
                && Objects.equals(qol.commissionDisplayRow, hudRow)) {
            return hudCache;
        }
        List<String> lines = new ArrayList<>();
        lines.add(CommissionDisplayPolicy.formatTitle(qol.commissionDisplayTitle));
        if (commissions.isEmpty()) {
            lines.add(CommissionDisplayPolicy.formatNone(qol.commissionDisplayNone));
        } else {
            for (CommissionDisplayPolicy.Commission commission : commissions) {
                lines.add(CommissionDisplayPolicy.formatLine(
                        qol.commissionDisplayRow, commission, qol.commissionDisplayColoredPercent));
            }
        }
        hudCache = List.copyOf(lines);
        hudCacheFor = commissions;
        hudColored = qol.commissionDisplayColoredPercent;
        hudTitle = qol.commissionDisplayTitle;
        hudNone = qol.commissionDisplayNone;
        hudRow = qol.commissionDisplayRow;
        return hudCache;
    }

    static List<String> tabLines(Minecraft client) {
        return SkyBlockTabSnapshotRuntime.lines(
                client, System.currentTimeMillis());
    }

    static List<String> captureTabLines(Minecraft client) {
        List<String> lines = new ArrayList<>();
        PlayerTabOverlay overlay = null;
        if (client.gui != null && client.gui != null) {
            overlay = client.gui.getTabList();
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
        for (String line : LINE_BREAK.split(text)) {
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
    }
}
