package fi.rotclient;

/**
 * Operational Overview homepage: status strip, one notice, then Go-to cards.
 * Minecraft-free so layout and copy stay in lockstep between draw and clicks.
 */
public final class OverviewLandingPolicy {
    public static final String TITLE = "Overview";
    public static final String SUBTITLE =
            "Session and tracker status. Open a section to change settings.";
    public static final String GOTO_LABEL = "Go to";
    public static final int TITLE_Y_OFFSET = 12;
    public static final int SUBTITLE_Y_OFFSET = 28;
    public static final int CHIP_Y_OFFSET = 52;
    public static final int SECTION_GAP = 16;
    public static final int CHIP_GAP = 8;
    public static final int CHIP_HEIGHT = 56;
    public static final int NOTICE_HEIGHT = 40;
    public static final int CARD_GAP = 10;
    public static final int CARD_HEIGHT = 84;
    public static final int GOTO_LABEL_HEIGHT = 16;
    public static final int TARGET_MAX_CHARS = 18;
    public static final String PAUSED_NOTICE =
            "Current Session paused. Resume to continue collection.";
    public static final String ACTIVE_EMPTY_NOTICE =
            "Session active, no credited entries yet.";
    public static final String STALE_NOTICE = "Price book stale.";

    public enum Tone {
        GOOD,
        WARN,
        MUTED
    }

    public enum Hit {
        NONE,
        SESSION,
        TRACKER,
        POWDER,
        MODULES,
        LOOK,
        MINING,
        EVENTS
    }

    public record Rect(int x, int y, int width, int height) {
        static final Rect EMPTY = new Rect(0, 0, 0, 0);

        public boolean contains(int mouseX, int mouseY) {
            return width > 0
                    && height > 0
                    && mouseX >= x
                    && mouseY >= y
                    && mouseX < x + width
                    && mouseY < y + height;
        }
    }

    public record Kpi(String title, String value, String hint, Tone tone) {
        public Kpi {
            title = title == null ? "" : title;
            value = value == null || value.isBlank() ? "—" : value;
            hint = hint == null ? "" : hint;
            tone = tone == null ? Tone.MUTED : tone;
        }
    }

    public record Model(
            Kpi session,
            Kpi tracker,
            Kpi powder,
            String notice,
            boolean miningCardActive) {
        public Model {
            if (session == null || tracker == null || powder == null) {
                throw new IllegalArgumentException("Overview KPIs required");
            }
            notice = notice == null || notice.isBlank() ? null : notice;
        }

        public boolean hasNotice() {
            return notice != null;
        }
    }

    public record Layout(
            int originY,
            Rect session,
            Rect tracker,
            Rect powder,
            Rect notice,
            int gotoY,
            Rect modules,
            Rect look,
            Rect mining,
            Rect events,
            int communityY) {
        public int chipY() {
            return session.y();
        }
    }

    private OverviewLandingPolicy() {
    }

    public static Model from(
            boolean sessionRunning,
            boolean sessionPaused,
            int entryCount,
            boolean trackerEnabled,
            String trackerTarget,
            boolean powderEnabled,
            long powderChests,
            boolean priceBookStale) {
        Tone sessionTone;
        String sessionValue;
        String sessionHint;
        if (sessionRunning) {
            sessionValue = "RUNNING";
            sessionHint = "";
            sessionTone = Tone.GOOD;
        } else if (sessionPaused) {
            sessionValue = "PAUSED";
            sessionHint = "";
            sessionTone = Tone.WARN;
        } else {
            sessionValue = "—";
            sessionHint = "Not collecting";
            sessionTone = Tone.MUTED;
        }
        Kpi session = new Kpi("Session", sessionValue, sessionHint, sessionTone);
        Kpi tracker = new Kpi(
                "Tracker",
                truncate(trackerTarget, TARGET_MAX_CHARS),
                trackerEnabled ? "On" : "Off",
                trackerEnabled ? Tone.GOOD : Tone.MUTED);
        Kpi powder = new Kpi(
                "Powder",
                powderChests <= 0L ? "empty" : powderChests + " chests",
                powderEnabled ? "On" : "Off",
                powderEnabled ? Tone.GOOD : Tone.MUTED);
        String notice = null;
        if (sessionPaused) {
            notice = PAUSED_NOTICE;
        } else if (sessionRunning && entryCount <= 0) {
            notice = ACTIVE_EMPTY_NOTICE;
        } else if (priceBookStale) {
            notice = STALE_NOTICE;
        }
        return new Model(session, tracker, powder, notice, trackerEnabled);
    }

    public static Layout layout(
            int contentLeft,
            int originY,
            int contentWidth,
            boolean hasNotice) {
        int safeWidth = Math.max(1, contentWidth);
        int chipY = originY + CHIP_Y_OFFSET;
        int chipWidth = Math.max(1, (safeWidth - CHIP_GAP * 2) / 3);
        Rect session = new Rect(contentLeft, chipY, chipWidth, CHIP_HEIGHT);
        Rect tracker = new Rect(
                contentLeft + chipWidth + CHIP_GAP, chipY, chipWidth, CHIP_HEIGHT);
        int powderX = contentLeft + (chipWidth + CHIP_GAP) * 2;
        int powderW = Math.max(1, contentLeft + safeWidth - powderX);
        Rect powder = new Rect(powderX, chipY, powderW, CHIP_HEIGHT);

        int y = chipY + CHIP_HEIGHT + SECTION_GAP;
        Rect notice = Rect.EMPTY;
        if (hasNotice) {
            notice = new Rect(contentLeft, y, safeWidth, NOTICE_HEIGHT);
            y += NOTICE_HEIGHT + SECTION_GAP;
        }
        int gotoY = y;
        int cardY = gotoY + GOTO_LABEL_HEIGHT;
        int cardWidth = Math.max(1, (safeWidth - CARD_GAP) / 2);
        Rect modules = new Rect(contentLeft, cardY, cardWidth, CARD_HEIGHT);
        Rect look = new Rect(
                contentLeft + cardWidth + CARD_GAP, cardY, cardWidth, CARD_HEIGHT);
        int row2Y = cardY + CARD_HEIGHT + CARD_GAP;
        Rect mining = new Rect(contentLeft, row2Y, cardWidth, CARD_HEIGHT);
        Rect events = new Rect(
                contentLeft + cardWidth + CARD_GAP, row2Y, cardWidth, CARD_HEIGHT);
        int communityY = row2Y + CARD_HEIGHT + SECTION_GAP;
        return new Layout(
                originY,
                session,
                tracker,
                powder,
                notice,
                gotoY,
                modules,
                look,
                mining,
                events,
                communityY);
    }

    public static Hit hit(int mouseX, int mouseY, Layout layout) {
        if (layout == null) {
            return Hit.NONE;
        }
        if (layout.session().contains(mouseX, mouseY)) {
            return Hit.SESSION;
        }
        if (layout.tracker().contains(mouseX, mouseY)) {
            return Hit.TRACKER;
        }
        if (layout.powder().contains(mouseX, mouseY)) {
            return Hit.POWDER;
        }
        if (layout.modules().contains(mouseX, mouseY)) {
            return Hit.MODULES;
        }
        if (layout.look().contains(mouseX, mouseY)) {
            return Hit.LOOK;
        }
        if (layout.mining().contains(mouseX, mouseY)) {
            return Hit.MINING;
        }
        if (layout.events().contains(mouseX, mouseY)) {
            return Hit.EVENTS;
        }
        return Hit.NONE;
    }

    static String truncate(String value, int maxChars) {
        if (value == null) {
            return "—";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "—";
        }
        int limit = Math.max(1, maxChars);
        if (trimmed.length() <= limit) {
            return trimmed;
        }
        if (limit == 1) {
            return "…";
        }
        return trimmed.substring(0, limit - 1) + "…";
    }
}
