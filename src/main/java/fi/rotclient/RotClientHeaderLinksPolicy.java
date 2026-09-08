package fi.rotclient;

/**
 * GitHub and Discord icon buttons in the dashboard omnibox chrome. Layout and
 * glyph masks stay Minecraft-free so hit-testing matches the draw path.
 */
public final class RotClientHeaderLinksPolicy {
    public static final String GITHUB_URL = "https://github.com/rot-tools/Rot-Client";
    public static final String DISCORD_URL = "https://discord.gg/8UpMfvZugq";
    public static final String GITHUB_TIP = "GitHub";
    public static final String DISCORD_TIP = "Discord";

    public static final int ICON = 16;
    public static final int HIT = 20;
    public static final int GAP = 6;
    public static final int RIGHT_INSET = 10;

    public enum Kind {
        NONE,
        GITHUB,
        DISCORD
    }

    public record Rect(int x, int y, int width, int height) {
        public boolean contains(int mouseX, int mouseY) {
            return width > 0
                    && height > 0
                    && mouseX >= x
                    && mouseY >= y
                    && mouseX < x + width
                    && mouseY < y + height;
        }

        public int iconX() {
            return x + Math.max(0, (width - ICON) / 2);
        }

        public int iconY() {
            return y + Math.max(0, (height - ICON) / 2);
        }
    }

    public record Layout(Rect github, Rect discord) {
        public Layout {
            if (github == null || discord == null) {
                throw new IllegalArgumentException("Header link rects required");
            }
        }
    }

    private static final String[] GITHUB_GLYPH = {
            "......XXXX......",
            "....XXXXXXXX....",
            "...XXXXXXXXXX...",
            "..XXXXXXXXXXXX..",
            "..XXXXX..XXXXX..",
            ".XXXX......XXXX.",
            ".XXX........XXX.",
            ".XXX.XX..XX.XXX.",
            ".XXX........XXX.",
            ".XXXX......XXXX.",
            "..XXXX....XXXX..",
            "..XXXXX..XXXXX..",
            "...XXXXXXXXXX...",
            "....XX....XX....",
            "...XXX....XXX...",
            "..XXXX....XXXX.."
    };

    private static final String[] DISCORD_GLYPH = {
            "................",
            "................",
            "...XXXXXXXXXX...",
            "..XXXXXXXXXXXX..",
            ".XXXXXXXXXXXXXX.",
            ".XXX..XXXX..XXX.",
            ".XXX..XXXX..XXX.",
            ".XXXXXXXXXXXXXX.",
            ".XXXXXXXXXXXXXX.",
            "..XXXX....XXXX..",
            "...XXXXXXXXXX...",
            "....XXX..XXX....",
            "................",
            "................",
            "................",
            "................"
    };

    private RotClientHeaderLinksPolicy() {
    }

    public static Layout layout(int panelX, int panelY) {
        int y = RotClientDashboardLayout.omniboxY(panelY)
                + Math.max(0, (RotClientDashboardLayout.OMNIBOX_HEIGHT - HIT) / 2);
        int discordX = panelX + RotClientDashboardLayout.SIDEBAR_WIDTH - RIGHT_INSET - HIT;
        int githubX = discordX - GAP - HIT;
        return new Layout(
                new Rect(githubX, y, HIT, HIT),
                new Rect(discordX, y, HIT, HIT));
    }

    public static Kind hit(int mouseX, int mouseY, Layout layout) {
        if (layout == null) {
            return Kind.NONE;
        }
        if (layout.github().contains(mouseX, mouseY)) {
            return Kind.GITHUB;
        }
        if (layout.discord().contains(mouseX, mouseY)) {
            return Kind.DISCORD;
        }
        return Kind.NONE;
    }

    public static String url(Kind kind) {
        return switch (kind) {
            case GITHUB -> GITHUB_URL;
            case DISCORD -> DISCORD_URL;
            case NONE -> "";
        };
    }

    public static String tip(Kind kind) {
        return switch (kind) {
            case GITHUB -> GITHUB_TIP;
            case DISCORD -> DISCORD_TIP;
            case NONE -> "";
        };
    }

    public static String[] githubGlyph() {
        return GITHUB_GLYPH.clone();
    }

    public static String[] discordGlyph() {
        return DISCORD_GLYPH.clone();
    }

    static int filledCells(String[] rows) {
        if (rows == null) {
            return 0;
        }
        int count = 0;
        for (String row : rows) {
            if (row == null) {
                continue;
            }
            for (int i = 0; i < row.length(); i++) {
                char cell = row.charAt(i);
                if (cell != '.' && cell != ' ') {
                    count++;
                }
            }
        }
        return count;
    }
}
