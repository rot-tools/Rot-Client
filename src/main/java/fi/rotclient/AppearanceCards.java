package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * Content and geometry of the Appearance dropdown cards shown in the dashboard.
 *
 * <p>Drawing and clicking both walk the same {@link Layout}, so a row can never be drawn in one
 * place and hit-tested in another. The old customizer screen kept two copies of every list, one
 * for drawing and one for clicks, that had to be edited together.
 *
 * <p>Nothing here draws. Card bodies are laid out fully open and their height is scaled by the
 * open amount, so the same numbers give the closing animation; items that fall outside the
 * animated body carry a clip range and are hidden or scissored by the caller.
 */
public final class AppearanceCards {
    public static final int CARD_HEIGHT = 48;
    public static final int CARD_GAP = 8;
    public static final int BODY_INSET = 12;
    public static final int BODY_PAD = 6;
    public static final int GROUP_HEIGHT = 22;
    public static final int GROUP_STEP = 28;
    public static final int GROUP_GAP = 6;
    public static final int COLOR_HEIGHT = 26;
    public static final int COLOR_STEP = 28;
    public static final int HELP_HEIGHT = 12;
    public static final int HELP_STEP = 14;
    public static final int TOGGLE_HEIGHT = 34;
    public static final int TOGGLE_STEP = 40;
    public static final int FILE_HEIGHT = 22;
    public static final int FILE_STEP = 26;
    public static final int BUTTON_HEIGHT = 26;
    public static final int BUTTON_STEP = 32;
    public static final int BUTTON_GAP = 8;

    public static final String CARD_UNDO = "card.undo";
    public static final String CARD_RESET = "card.reset";
    public static final String BG_TOGGLE = "background.enabled";
    public static final String BG_HINT = "background.hint";
    public static final String BG_EMPTY = "background.empty";
    public static final String BG_STATS = "background.stats";
    public static final String BG_OPACITY_DOWN = "background.opacity_down";
    public static final String BG_OPACITY_UP = "background.opacity_up";
    public static final String BG_DIM_DOWN = "background.dim_down";
    public static final String BG_DIM_UP = "background.dim_up";
    public static final String BG_FIT_FILL = "background.fit_fill";
    public static final String BG_FIT_FIT = "background.fit_fit";
    public static final String BG_FIT_STRETCH = "background.fit_stretch";
    public static final String BG_OPEN_FOLDER = "background.open_folder";
    public static final String BG_REFRESH = "background.refresh";
    public static final String RESET_UNDO_ALL = "reset.undo_all";
    public static final String RESET_ALL = "reset.all";
    public static final String RESET_LAYOUT = "reset.layout";
    public static final String RESET_HUD = "reset.hud";

    private static final int NO_CLIP_TOP = Integer.MIN_VALUE;
    private static final int NO_CLIP_BOTTOM = Integer.MAX_VALUE;

    private static final List<String> BACKGROUND_FIELDS = List.of(
            "customBackgroundEnabled",
            "customBackgroundFile",
            "customBackgroundOpacity",
            "customBackgroundDim",
            "customBackgroundFit");

    public record ColorField(String field, String label, String help) {
        ColorField(String field, String label) {
            this(field, label, "");
        }
    }

    /** A titled sub-dropdown inside a card. A blank title means "always open, no header". */
    public record Group(String id, String title, List<ColorField> colors) {
    }

    public record Card(String id, String title, String subtitle, List<Group> groups) {
        public boolean isBackground() {
            return "background".equals(id);
        }

        public boolean isReset() {
            return "reset".equals(id);
        }

        /** The {@code RotClientAppearanceConfig} field names this card owns, for undo and reset. */
        public List<String> fields() {
            List<String> out = new ArrayList<>();
            for (Group group : groups) {
                for (ColorField color : group.colors()) {
                    out.add(color.field());
                }
            }
            if (isBackground()) {
                out.addAll(BACKGROUND_FIELDS);
            }
            return List.copyOf(out);
        }

        public String expandId() {
            return RotClientAppearanceNav.CARD_PREFIX + id;
        }
    }

    public enum Kind {
        CARD,
        GROUP,
        COLOR,
        HELP,
        TEXT,
        TOGGLE,
        FILE,
        BUTTONS
    }

    /** A button inside a {@link Kind#BUTTONS} row; {@code x} is relative to the body's left edge. */
    public record Button(String id, String label, int x, int width, boolean accent) {
    }

    /**
     * One laid-out row. {@code y} is relative to the top of the whole layout. Rows inside a
     * dropdown that is animating open or closed keep their fully-open position and are only
     * visible between {@code clipTop} and {@code clipBottom}.
     */
    public record Item(
            Kind kind,
            String id,
            String label,
            String detail,
            int y,
            int height,
            int clipTop,
            int clipBottom,
            List<Button> buttons) {

        public boolean visible() {
            return y < clipBottom && y + height > clipTop;
        }

        public boolean containsY(int py) {
            return py >= Math.max(y, clipTop) && py < Math.min(y + height, clipBottom);
        }

        public boolean clipped() {
            return y < clipTop || y + height > clipBottom;
        }

        /** The button under {@code px}, given where this row's body starts, or {@code null}. */
        public Button buttonAt(int px, int bodyLeft) {
            for (Button button : buttons) {
                int left = bodyLeft + button.x();
                if (px >= left && px < left + button.width()) {
                    return button;
                }
            }
            return null;
        }

        private Item moved(int dy, int parentTop, int parentBottom) {
            return new Item(
                    kind, id, label, detail,
                    y + dy,
                    height,
                    Math.max(shift(clipTop, dy, NO_CLIP_TOP), parentTop),
                    Math.min(shift(clipBottom, dy, NO_CLIP_BOTTOM), parentBottom),
                    buttons);
        }

        private static int shift(int value, int dy, int unbounded) {
            return value == unbounded ? value : value + dy;
        }
    }

    public record Layout(List<Item> items, int height) {
        /** The visible row at {@code py}, in layout coordinates, or {@code null}. */
        public Item itemAt(int py) {
            for (Item item : items) {
                if (item.containsY(py)) {
                    return item;
                }
            }
            return null;
        }
    }

    /**
     * @param amount     0..1 open amount of a card or group expand id
     * @param files      background image names found on disk
     * @param confirming button ids waiting for a second click
     */
    public record Input(ToDoubleFunction<String> amount, List<String> files, Set<String> confirming) {
    }

    private AppearanceCards() {
    }

    public static List<Card> cards() {
        return List.of(
                build("dashboard", List.of(
                        new Group(RotClientAppearanceNav.DASHBOARD_BASICS, "", List.of(
                                new ColorField("dashboardBackdrop", "Main background"),
                                new ColorField("dashboardSurface", "Main panel"),
                                new ColorField("dashboardSurfaceAlt", "Secondary panel / card"),
                                new ColorField("dashboardHeader", "Header background"),
                                new ColorField("dashboardSidebar", "Sidebar background"),
                                new ColorField("dashboardBorder", "Border"),
                                new ColorField("dashboardAccent", "Accent / highlight"))))),
                build("colors", List.of(
                        new Group(RotClientAppearanceNav.HUD_BASICS, "HUD surfaces", List.of(
                                new ColorField("hudBackground", "HUD background"),
                                new ColorField("hudPanel", "HUD panel / card"),
                                new ColorField("hudPanelAlt", "HUD alternate panel"),
                                new ColorField("hudHeader", "HUD header"),
                                new ColorField("hudBorder", "HUD border"),
                                new ColorField("hudAccent", "HUD accent"))),
                        new Group(RotClientAppearanceNav.DASHBOARD_TEXT, "Dashboard text", List.of(
                                new ColorField("dashboardSectionTitle", "Section title"),
                                new ColorField("dashboardTextPrimary", "Primary text"),
                                new ColorField("dashboardTextSecondary", "Secondary / muted text"),
                                new ColorField("dashboardTextSuccess", "Positive / enabled"),
                                new ColorField("dashboardTextWarning", "Warning / experimental"))),
                        new Group(RotClientAppearanceNav.HUD_TEXT, "HUD text", List.of(
                                new ColorField("hudTitle", "HUD title"),
                                new ColorField("hudTextPrimary", "HUD primary text"),
                                new ColorField("hudTextSecondary", "HUD secondary text"))),
                        new Group(RotClientAppearanceNav.BUTTONS_BORDERS, "Buttons & borders", List.of(
                                new ColorField("dashboardButton", "Button background"),
                                new ColorField("dashboardButtonHover", "Button hover"),
                                new ColorField("dashboardButtonSelected", "Button selected"),
                                new ColorField("dashboardButtonText", "Button text"),
                                new ColorField("dashboardField", "Field background"),
                                new ColorField("dashboardFieldActive", "Field focused"),
                                new ColorField("dashboardHoverRow", "Hover row"),
                                new ColorField("dashboardDivider", "Divider"),
                                new ColorField("dashboardShadow", "Shadow"),
                                new ColorField("dashboardError", "Error"))))),
                build("background", List.of()),
                build("charts", List.of(
                        new Group("appearance.charts", "", List.of(
                                new ColorField("hudChartLine", "Rate chart line",
                                        "Primary stroke of the mining rate sparkline."),
                                new ColorField("hudChartGlow", "Rate chart glow",
                                        "Soft bloom drawn around the rate line."),
                                new ColorField("hudChartFill", "Rate chart fill",
                                        "Area fill beneath the rate line."),
                                new ColorField("hudChartGrid", "Rate chart grid",
                                        "Horizontal/vertical guide lines inside the chart."),
                                new ColorField("hudChartWell", "Rate chart well",
                                        "Background well behind the rate chart."))))),
                build("reset", List.of()));
    }

    /** Title and subtitle come from the landing policy, which is the one list of Appearance cards. */
    private static Card build(String id, List<Group> groups) {
        for (AppearanceLandingPolicy.Card landing : AppearanceLandingPolicy.cards()) {
            if (id.equals(AppearanceLandingPolicy.sectionId(landing.actionId()))) {
                return new Card(id, landing.title(), landing.subtitle(), groups);
            }
        }
        throw new IllegalStateException("No Appearance landing card for " + id);
    }

    public static Card card(String id) {
        for (Card card : cards()) {
            if (card.id().equals(id)) {
                return card;
            }
        }
        return null;
    }

    /** Every color field the cards edit, in card order. */
    public static List<String> allFields() {
        List<String> out = new ArrayList<>();
        for (Card card : cards()) {
            out.addAll(card.fields());
        }
        return List.copyOf(out);
    }

    /** Expand ids the animation state must know about: each card and each titled group. */
    public static List<String> allExpandIds() {
        List<String> out = new ArrayList<>();
        for (Card card : cards()) {
            out.add(card.expandId());
            for (Group group : card.groups()) {
                if (!group.title().isBlank()) {
                    out.add(group.id());
                }
            }
        }
        return List.copyOf(out);
    }

    public static Layout layout(Input input) {
        List<Item> out = new ArrayList<>();
        int y = 0;
        for (Card card : cards()) {
            out.add(item(Kind.CARD, card.id(), card.title(), card.subtitle(), y, CARD_HEIGHT));
            y += CARD_HEIGHT;
            double amount = clamp01(input.amount().applyAsDouble(card.expandId()));
            if (amount > 0.001D) {
                List<Item> body = new ArrayList<>();
                int full = body(card, input, body);
                int shown = (int) Math.round(full * amount);
                y += 4;
                for (Item item : body) {
                    Item placed = item.moved(y, y, y + shown);
                    if (placed.visible()) {
                        out.add(placed);
                    }
                }
                y += shown + BODY_PAD;
            }
            y += CARD_GAP;
        }
        return new Layout(List.copyOf(out), y);
    }

    private static int body(Card card, Input input, List<Item> out) {
        if (card.isBackground()) {
            return backgroundBody(input, out);
        }
        if (card.isReset()) {
            return resetBody(input, out);
        }
        int y = 0;
        for (Group group : card.groups()) {
            if (group.title().isBlank()) {
                y = colors(group, y, out);
                continue;
            }
            out.add(item(Kind.GROUP, group.id(), group.title(),
                    group.colors().size() + " colors", y, GROUP_HEIGHT));
            y += GROUP_STEP;
            double amount = clamp01(input.amount().applyAsDouble(group.id()));
            if (amount > 0.001D) {
                List<Item> rows = new ArrayList<>();
                int full = colors(group, 0, rows);
                int shown = (int) Math.round(full * amount);
                for (Item row : rows) {
                    Item placed = row.moved(y, y, y + shown);
                    if (placed.visible()) {
                        out.add(placed);
                    }
                }
                y += shown;
            }
            y += GROUP_GAP;
        }
        out.add(buttons("card." + card.id(), y,
                button(CARD_UNDO, "Undo changes", 0, 130, false),
                button(CARD_RESET, "Reset to default", 130 + BUTTON_GAP, 150, false)));
        return y + BUTTON_STEP;
    }

    private static int colors(Group group, int y, List<Item> out) {
        for (ColorField color : group.colors()) {
            out.add(item(Kind.COLOR, color.field(), color.label(), "", y, COLOR_HEIGHT));
            y += COLOR_STEP;
            if (!color.help().isBlank()) {
                out.add(item(Kind.HELP, color.field() + ".help", color.help(), "", y, HELP_HEIGHT));
                y += HELP_STEP;
            }
        }
        return y;
    }

    private static int backgroundBody(Input input, List<Item> out) {
        int y = 0;
        out.add(item(Kind.TOGGLE, BG_TOGGLE, "Enable custom background", "", y, TOGGLE_HEIGHT));
        y += TOGGLE_STEP;
        out.add(item(Kind.TEXT, BG_HINT, "", "", y, HELP_HEIGHT));
        y += HELP_STEP;
        if (input.files().isEmpty()) {
            out.add(item(Kind.TEXT, BG_EMPTY, "No images found yet.", "", y, HELP_HEIGHT));
            y += 24;
        } else {
            for (String file : input.files()) {
                out.add(item(Kind.FILE, file, file, "", y, FILE_HEIGHT));
                y += FILE_STEP;
            }
        }
        out.add(item(Kind.TEXT, BG_STATS, "", "", y, HELP_HEIGHT));
        y += 20;
        out.add(buttons("background.amounts", y,
                button(BG_OPACITY_DOWN, "Opacity -", 0, 110, false),
                button(BG_OPACITY_UP, "Opacity +", 118, 110, false),
                button(BG_DIM_DOWN, "Dim -", 236, 90, false),
                button(BG_DIM_UP, "Dim +", 334, 90, false)));
        y += BUTTON_STEP;
        out.add(buttons("background.fit", y,
                button(BG_FIT_FILL, "Fill", 0, 90, false),
                button(BG_FIT_FIT, "Fit", 98, 90, false),
                button(BG_FIT_STRETCH, "Stretch", 196, 100, false)));
        y += BUTTON_STEP;
        out.add(buttons("background.files", y,
                button(BG_OPEN_FOLDER, "Open Folder", 0, 140, false),
                button(BG_REFRESH, "Refresh", 148, 120, false)));
        y += BUTTON_STEP;
        out.add(buttons("card.background", y,
                button(CARD_UNDO, "Undo changes", 0, 130, false),
                button(CARD_RESET, "Reset to default", 130 + BUTTON_GAP, 150, false)));
        return y + BUTTON_STEP;
    }

    private static int resetBody(Input input, List<Item> out) {
        int y = 0;
        out.add(item(Kind.TEXT, "reset.blurb1", "Restore defaults for appearance only.", "", y, HELP_HEIGHT));
        y += HELP_STEP;
        out.add(item(Kind.TEXT, "reset.blurb2",
                "Tracker, session, and history settings are never changed here.", "", y, HELP_HEIGHT));
        y += 18;
        out.add(buttons(RESET_UNDO_ALL, y,
                button(RESET_UNDO_ALL, "Undo everything this session", 0, 240, false)));
        y += BUTTON_STEP + 2;
        y = confirmable(out, input, y, RESET_ALL, "Reset all appearance",
                "Confirm reset all appearance", "Click again to confirm reset all", true);
        y = confirmable(out, input, y, RESET_LAYOUT, "Reset UI Positions",
                "Confirm reset UI positions",
                "Click again to reset Client UI + Mining HUD positions", false);
        y = confirmable(out, input, y, RESET_HUD, "Reset HUD Visibility",
                "Confirm reset HUD visibility",
                "Click again to restore all HUD show toggles", false);
        out.add(item(Kind.TEXT, "reset.footer",
                "Reset all also disables the custom background.", "", y, HELP_HEIGHT));
        return y + 20;
    }

    private static int confirmable(
            List<Item> out,
            Input input,
            int y,
            String id,
            String label,
            String confirmLabel,
            String warning,
            boolean accent) {
        boolean waiting = input.confirming().contains(id);
        out.add(buttons(id, y, button(id, waiting ? confirmLabel : label, 0, 240, accent)));
        y += BUTTON_STEP + 2;
        if (waiting) {
            out.add(item(Kind.TEXT, id + ".warning", warning, "", y, HELP_HEIGHT));
            y += HELP_STEP;
        }
        return y;
    }

    private static Item item(Kind kind, String id, String label, String detail, int y, int height) {
        return new Item(kind, id, label, detail, y, height, NO_CLIP_TOP, NO_CLIP_BOTTOM, List.of());
    }

    private static Item buttons(String id, int y, Button... buttons) {
        return new Item(Kind.BUTTONS, id, "", "", y, BUTTON_HEIGHT,
                NO_CLIP_TOP, NO_CLIP_BOTTOM, List.of(buttons));
    }

    private static Button button(String id, String label, int x, int width, boolean accent) {
        return new Button(id, label, x, width, accent);
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}
