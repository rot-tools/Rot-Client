package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

/** Client bridge for the Rot-native custom SkyBlock sidebar. */
final class CustomScoreboardRuntime {
    private static final CustomScoreboardPolicy.DeltaBook DELTAS =
            new CustomScoreboardPolicy.DeltaBook();
    private static List<CustomScoreboardPolicy.Row> cachedRows = List.of();
    private static long cacheUntilMillis;
    private static long lastUnknownWarnAt;
    private static final Set<String> warnedUnknown = new HashSet<>();
    private static int lastX;
    private static int lastY;
    private static int lastW;
    private static int lastH;
    private static boolean lastVisible;

    private CustomScoreboardRuntime() {
    }

    static boolean visible(QolUtilityConfig qol) {
        if (qol == null || !qol.isModuleEnabled(CustomScoreboardPolicy.MODULE_ID)) {
            lastVisible = false;
            return false;
        }
        CustomScoreboardSettings board = qol.extras().board();
        Minecraft client = Minecraft.getInstance();
        boolean skyblock = SkyBlockAreaDetector.isInSkyblock();
        if (!skyblock && !board.showOutsideSkyblock) {
            lastVisible = false;
            return false;
        }
        return client != null && client.level != null;
    }

    static boolean hideVanilla(QolUtilityConfig qol) {
        return visible(qol) && qol.extras().board().hideVanilla;
    }

    static void render(GuiGraphicsExtractor graphics, QolUtilityConfig qol) {
        if (graphics == null || !visible(qol)) {
            lastVisible = false;
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        CustomScoreboardSettings board = qol.extras().board();
        CustomScoreboardPolicy.Options options = board.options();
        long now = System.currentTimeMillis();
        List<CustomScoreboardPolicy.Row> rows = rows(client, board, options, now);
        if (rows.isEmpty() && !qolHudEditor(qol)) {
            lastVisible = false;
            return;
        }
        int lineH = CustomScoreboardPolicy.lineAdvance(options.lineSpacing());
        int border = Math.max(0, board.bgBorder);
        int maxW = 20;
        for (CustomScoreboardPolicy.Row row : rows) {
            maxW = Math.max(maxW, RotClientFonts.vanillaWidth(font, row.text()));
        }
        int panelW = maxW + border * 2 + 8;
        int panelH = Math.max(lineH, rows.size() * lineH) + border * 2 + 6;
        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();
        float[] pose = qol.pose(CustomScoreboardPolicy.POSE_ID);
        int x = CustomScoreboardPolicy.panelX(
                screenW, panelW, options.alignH(), options.margin(), Math.round(pose[0]));
        int y = CustomScoreboardPolicy.panelY(
                screenH, panelH, options.alignV(), options.margin(), Math.round(pose[1]));
        lastX = x;
        lastY = y;
        lastW = panelW;
        lastH = panelH;
        lastVisible = true;
        float scale = pose.length > 2 ? Math.max(0.4F, pose[2]) : 1.0F;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        drawPanel(graphics, board, 0, 0, panelW, panelH);
        int textY = border + 3;
        for (CustomScoreboardPolicy.Row row : rows) {
            int textX = textX(font, row, panelW, border);
            RotClientUiDraw.vanillaText(graphics, font, row.text(), textX, textY, 0xFFFFFFFF, true);
            textY += lineH;
        }
        graphics.pose().popMatrix();
        lastW = Math.round(panelW * scale);
        lastH = Math.round(panelH * scale);
    }

    static boolean hit(double mouseX, double mouseY) {
        return lastVisible
                && mouseX >= lastX
                && mouseX < lastX + lastW
                && mouseY >= lastY
                && mouseY < lastY + lastH;
    }

    static int editorWidth() {
        return Math.max(120, lastW);
    }

    static int editorHeight() {
        return Math.max(80, lastH);
    }

    static void clearCache() {
        cachedRows = List.of();
        cacheUntilMillis = 0L;
        warnedUnknown.clear();
        lastVisible = false;
    }

    private static boolean qolHudEditor(QolUtilityConfig qol) {
        return RotClientClient.qolHud() != null && RotClientClient.qolHud().editorOpen();
    }

    private static List<CustomScoreboardPolicy.Row> rows(
            Minecraft client,
            CustomScoreboardSettings board,
            CustomScoreboardPolicy.Options options,
            long now) {
        if (options.cacheOnSwitch() && now < cacheUntilMillis && !cachedRows.isEmpty()) {
            return cachedRows;
        }
        SidebarCapture capture = capture(client);
        CustomScoreboardPolicy.BoardView view = new CustomScoreboardPolicy.BoardView(
                SkyBlockAreaDetector.isInSkyblock(),
                capture.alpha,
                capture.title,
                capture.lines,
                SkyBlockTabSnapshotRuntime.lines(client, now),
                islandName(),
                locationHint(capture.lines),
                profileName(),
                profileType(capture.lines),
                quiverCurrent(),
                quiverMax(),
                liveStats(),
                now);
        CustomScoreboardPolicy.ComposeResult result =
                CustomScoreboardPolicy.compose(view, options, DELTAS);
        cachedRows = result.rows();
        warnUnknown(board, result.unknownPlain(), now);
        return cachedRows;
    }

    static void onWorldChange() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (qol != null && qol.extras().board().cacheOnSwitch) {
            cacheUntilMillis = System.currentTimeMillis() + 7_000L;
        } else {
            cachedRows = List.of();
        }
    }

    static void disableAutoAlign(QolUtilityConfig qol) {
        if (qol == null) {
            return;
        }
        CustomScoreboardSettings board = qol.extras().board();
        if (!"Don't Align".equals(board.alignH) || !"Don't Align".equals(board.alignV)) {
            board.alignH = "Don't Align";
            board.alignV = "Don't Align";
            board.hudX = lastX;
            board.hudY = lastY;
        }
    }

    private static void drawPanel(
            GuiGraphicsExtractor graphics,
            CustomScoreboardSettings board,
            int x,
            int y,
            int w,
            int h) {
        if (!board.bgEnabled) {
            return;
        }
        int fill = board.bgColor;
        if (board.customBgImage) {
            int opacity = Math.max(0, Math.min(100, board.customBgOpacity));
            fill = RotClientUiDraw.withAlpha(fill, (int) Math.round(opacity * 2.55D));
        }
        RotClientUiDraw.roundedFill(graphics, x, y, x + w, y + h, fill, Math.max(0, board.bgRound));
        if (board.outline) {
            int thickness = Math.max(1, board.outlineThickness);
            for (int i = 0; i < thickness; i++) {
                float t = h <= 1 ? 0.0F : i / (float) Math.max(1, h);
                int color = lerpColor(board.outlineTop, board.outlineBottom, t);
                int a = Math.max(40, Math.min(255, (int) Math.round(255.0D * (1.0D - board.outlineBlur * 0.4D))));
                color = RotClientUiDraw.withAlpha(color, a);
                graphics.fill(x + i, y + i, x + w - i, y + i + 1, color);
                graphics.fill(x + i, y + h - i - 1, x + w - i, y + h - i, color);
                graphics.fill(x + i, y + i, x + i + 1, y + h - i, color);
                graphics.fill(x + w - i - 1, y + i, x + w - i, y + h - i, color);
            }
        }
    }

    private static int textX(Font font, CustomScoreboardPolicy.Row row, int panelW, int border) {
        int width = RotClientFonts.vanillaWidth(font, row.text());
        return switch (row.align()) {
            case CENTER -> Math.max(border, (panelW - width) / 2);
            case RIGHT -> Math.max(border, panelW - border - 4 - width);
            default -> border + 4;
        };
    }

    private static int lerpColor(int from, int to, float t) {
        float u = Math.max(0.0F, Math.min(1.0F, t));
        int a = ((from >>> 24) & 0xFF);
        int r = (int) (((from >> 16) & 0xFF) * (1.0F - u) + ((to >> 16) & 0xFF) * u);
        int g = (int) (((from >> 8) & 0xFF) * (1.0F - u) + ((to >> 8) & 0xFF) * u);
        int b = (int) ((from & 0xFF) * (1.0F - u) + (to & 0xFF) * u);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static void warnUnknown(CustomScoreboardSettings board, List<String> unknown, long now) {
        if (!board.unknownWarning || unknown == null || unknown.isEmpty()) {
            return;
        }
        if (now - lastUnknownWarnAt < 15_000L) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        for (String line : unknown) {
            String plain = CustomScoreboardPolicy.strip(line);
            if (plain.isBlank() || !warnedUnknown.add(plain)) {
                continue;
            }
            lastUnknownWarnAt = now;
            client.player.sendSystemMessage(RotClientChat.message(
                    "Custom Scoreboard saw an unknown sidebar line: " + plain));
            return;
        }
    }

    private static SidebarCapture capture(Minecraft client) {
        List<String> lines = new ArrayList<>();
        String title = "";
        boolean alpha = false;
        if (client.level == null) {
            return new SidebarCapture(title, lines, alpha);
        }
        Scoreboard scoreboard = client.level.getScoreboard();
        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) {
            return new SidebarCapture(title, lines, alpha);
        }
        title = sectionString(sidebar.getDisplayName());
        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
            Component name = PlayerTeam.formatNameForTeam(
                    scoreboard.getPlayersTeam(entry.owner()), entry.ownerName());
            String line = sectionString(name);
            lines.add(line);
            if (CustomScoreboardPolicy.strip(line).toLowerCase().contains("alpha.hypixel.net")) {
                alpha = true;
            }
        }
        return new SidebarCapture(title, lines, alpha);
    }

    private static String sectionString(Component component) {
        if (component == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        component.visit((style, text) -> {
            out.append(styleCodes(style));
            out.append(text);
            return Optional.empty();
        }, Style.EMPTY);
        return out.toString();
    }

    private static String styleCodes(Style style) {
        if (style == null || style.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder("§r");
        TextColor color = style.getColor();
        if (color != null) {
            char code = rgbCode(color.getValue());
            if (code != 0) {
                out.append('§').append(code);
            }
        }
        if (style.isBold()) {
            out.append("§l");
        }
        if (style.isItalic()) {
            out.append("§o");
        }
        if (style.isUnderlined()) {
            out.append("§n");
        }
        if (style.isStrikethrough()) {
            out.append("§m");
        }
        if (style.isObfuscated()) {
            out.append("§k");
        }
        return out.toString();
    }

    private static char rgbCode(int rgb) {
        int value = rgb & 0xFFFFFF;
        return switch (value) {
            case 0x000000 -> '0';
            case 0x0000AA -> '1';
            case 0x00AA00 -> '2';
            case 0x00AAAA -> '3';
            case 0xAA0000 -> '4';
            case 0xAA00AA -> '5';
            case 0xFFAA00 -> '6';
            case 0xAAAAAA -> '7';
            case 0x555555 -> '8';
            case 0x5555FF -> '9';
            case 0x55FF55 -> 'a';
            case 0x55FFFF -> 'b';
            case 0xFF5555 -> 'c';
            case 0xFF55FF -> 'd';
            case 0xFFFF55 -> 'e';
            case 0xFFFFFF -> 'f';
            default -> 0;
        };
    }

    private static String islandName() {
        SkyBlockArea area = SkyBlockAreaDetector.detect();
        return area == null || area == SkyBlockArea.UNKNOWN_SKYBLOCK_AREA ? "" : area.displayName();
    }

    private static String locationHint(List<String> lines) {
        for (String line : lines) {
            if (line != null && line.contains("⏣")) {
                return line;
            }
        }
        return "";
    }

    private static String profileName() {
        Minecraft client = Minecraft.getInstance();
        return SkyBlockProfileIdentity.detect(
                        SkyBlockTabSnapshotRuntime.lines(client, System.currentTimeMillis()))
                .orElse("");
    }

    private static String profileType(List<String> lines) {
        for (String line : lines) {
            String plain = CustomScoreboardPolicy.strip(line).toLowerCase();
            if (plain.contains("ironman") || plain.contains("stranded") || plain.contains("bingo")) {
                return CustomScoreboardPolicy.strip(line);
            }
        }
        return "";
    }

    private static OptionalLong quiverCurrent() {
        IotaPolicy.ArrowSnapshot arrows = IotaRuntime.arrows();
        return arrows == null || !arrows.tracked()
                ? OptionalLong.empty()
                : OptionalLong.of(arrows.count());
    }

    private static OptionalLong quiverMax() {
        return OptionalLong.empty();
    }

    private static Map<CustomScoreboardPolicy.ChunkStat, String> liveStats() {
        Map<CustomScoreboardPolicy.ChunkStat, String> out = new EnumMap<>(CustomScoreboardPolicy.ChunkStat.class);
        SkyBlockStatBarParser.Stats stats = RotClientClient.qolHud() == null
                ? SkyBlockStatBarParser.Stats.empty()
                : RotClientClient.qolHud().statsTracker().stats();
        putStat(out, CustomScoreboardPolicy.ChunkStat.HEALTH, stats.health());
        putStat(out, CustomScoreboardPolicy.ChunkStat.DEFENSE, stats.defense());
        putStat(out, CustomScoreboardPolicy.ChunkStat.MANA, stats.mana());
        putStat(out, CustomScoreboardPolicy.ChunkStat.OVERFLOW, stats.overflowMana());
        putStat(out, CustomScoreboardPolicy.ChunkStat.SPEED, stats.speed());
        putStat(out, CustomScoreboardPolicy.ChunkStat.VITALITY, stats.vitality());
        return out;
    }

    private static void putStat(
            Map<CustomScoreboardPolicy.ChunkStat, String> out,
            CustomScoreboardPolicy.ChunkStat stat,
            java.util.OptionalDouble value) {
        if (value != null && value.isPresent()) {
            out.put(stat, String.valueOf(Math.round(value.getAsDouble())));
        }
    }

    private record SidebarCapture(String title, List<String> lines, boolean alpha) {
    }
}
