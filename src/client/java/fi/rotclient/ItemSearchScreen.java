package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Map;

/** Searchable local view of bundled item, recipe, source and museum data. */
final class ItemSearchScreen extends Screen {
    private static final int WIDTH = 820;
    private static final int HEIGHT = 460;
    private final Screen parent;
    private String query;
    private String selectedId = "";

    ItemSearchScreen(Screen parent, String prefill) {
        super(Component.literal("Item Search"));
        this.parent = parent;
        this.query = prefill == null ? "" : prefill;
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta) {

        int x =
                panelX();

        int y =
                panelY();

        RotClientUiDraw.drawShadowedPanel(
                graphics,
                x,
                y,
                WIDTH,
                HEIGHT);

        RotClientUiDraw.drawHeaderBar(
                graphics,
                font,
                x,
                y,
                WIDTH,
                40,
                "Item Search",
                "Search bundled SkyBlock item, recipe, source and museum data.");

        /*
         * Search/results workspace.
         */
        RotClientUiDraw.drawElevatedCard(
                graphics,
                x + 12,
                y + 48,
                382,
                360);

        /*
         * Item detail workspace.
         */
        RotClientUiDraw.drawElevatedCard(
                graphics,
                x + 404,
                y + 48,
                404,
                360);

        /*
         * Search remains keyboard-focused while this screen is open.
         * Geometry intentionally matches the existing implementation.
         */
        RotClientTheme.drawInset(
                graphics,
                x + 18,
                y + 54,
                370,
                26,
                true);

        String searchText =
                query.isBlank()
                        ? "Type an item name or ID..."
                        : RotClientUiDraw.ellipsize(
                                font,
                                query,
                                344);

        RotClientUiDraw.text(
                graphics,
                font,
                searchText,
                x + 28,
                y + 62,
                query.isBlank()
                        ? RotClientTheme.TEXT_MUTED
                        : RotClientTheme.TEXT,
                false);

        List<RotItemIndex.ItemDef> results =
                results();

        RotClientUiDraw.sectionLabel(
                graphics,
                font,
                "SEARCH RESULTS",
                x + 18,
                y + 94);

        String count =
                results.size()
                        + (results.size() == 1
                        ? " result"
                        : " results");

        RotClientUiDraw.text(
                graphics,
                font,
                count,
                x + 388 - font.width(count),
                y + 94,
                RotClientTheme.TEXT_MUTED,
                false);

        RotItemIndex.ItemDef visibleSelection =
                selected(results);

        int rowY =
                y + 114;

        for (RotItemIndex.ItemDef item
                : results) {

            boolean selected =
                    visibleSelection != null
                            && item.id().equals(
                            visibleSelection.id());

            boolean hover =
                    RotClientUiDraw.inside(
                            mouseX,
                            mouseY,
                            x + 18,
                            rowY,
                            370,
                            30);

            RotClientUiDraw.drawInteractiveSurface(
                    graphics,
                    x + 18,
                    rowY,
                    370,
                    30,
                    hover
                            ? 1.0F
                            : 0.0F,
                    selected,
                    RotClientTheme.HUD_ACCENT,
                    RotClientUiDraw.RADIUS_SM);

            if (selected) {
                graphics.fill(
                        x + 19,
                        rowY + 5,
                        x + 22,
                        rowY + 25,
                        RotClientTheme.HUD_ACCENT);
            }

            RotClientUiDraw.text(
                    graphics,
                    font,
                    RotClientUiDraw.ellipsize(
                            font,
                            item.name(),
                            166),
                    x + 28,
                    rowY + 5,
                    selected
                            ? RotClientTheme.TEXT
                            : RotClientTheme.TEXT_DIM,
                    true);

            RotClientUiDraw.text(
                    graphics,
                    font,
                    RotClientUiDraw.ellipsize(
                            font,
                            item.id(),
                            164),
                    x + 208,
                    rowY + 5,
                    RotClientTheme.TEXT_MUTED,
                    false);

            rowY += 34;
        }

        if (results.isEmpty()) {

            RotClientUiDraw.text(
                    graphics,
                    font,
                    query.isBlank()
                            ? "Start typing to search"
                            : "No matching items",
                    x + 125,
                    y + 220,
                    RotClientTheme.TEXT_DIM,
                    true);

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    query.isBlank()
                            ? "Search by item name or internal ID."
                            : "Try a shorter name or a different ID.",
                    x + 95,
                    y + 238);
        }

        drawDetails(
                graphics,
                x,
                y,
                visibleSelection);

        graphics.fill(
                x + 12,
                y + HEIGHT - 46,
                x + WIDTH - 12,
                y + HEIGHT - 45,
                RotClientTheme.DIVIDER);

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + WIDTH - 126,
                y + HEIGHT - 36,
                104,
                24,
                "Done",
                true,
                true);

        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta);
    }
    private void drawDetails(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            RotItemIndex.ItemDef item) {

        int left =
                x + 414;

        RotClientUiDraw.sectionLabel(
                graphics,
                font,
                "ITEM DETAILS",
                left,
                y + 58);

        if (item == null) {

            RotClientUiDraw.text(
                    graphics,
                    font,
                    "Nothing selected",
                    left + 118,
                    y + 208,
                    RotClientTheme.TEXT_DIM,
                    true);

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    "Choose an item from the search results.",
                    left + 80,
                    y + 226);

            return;
        }

        RotClientUiDraw.text(
                graphics,
                font,
                RotClientUiDraw.ellipsize(
                        font,
                        item.name(),
                        360),
                left,
                y + 78,
                RotClientTheme.TEXT,
                true);

        RotClientUiDraw.helpText(
                graphics,
                font,
                RotClientUiDraw.ellipsize(
                        font,
                        item.id(),
                        360),
                left,
                y + 94);

        /*
         * Basic metadata gets its own visual group instead of being mixed
         * into the recipe tree.
         */
        RotClientUiDraw.drawElevatedCard(
                graphics,
                left,
                y + 112,
                382,
                52);

        RotClientUiDraw.text(
                graphics,
                font,
                "Source",
                left + 10,
                y + 121,
                RotClientTheme.TEXT_MUTED,
                true);

        RotClientUiDraw.text(
                graphics,
                font,
                RotClientUiDraw.ellipsize(
                        font,
                        blank(
                                item.source(),
                                "Unknown"),
                        276),
                left + 92,
                y + 121,
                RotClientTheme.TEXT,
                false);

        RotClientUiDraw.text(
                graphics,
                font,
                "Museum",
                left + 10,
                y + 141,
                RotClientTheme.TEXT_MUTED,
                true);

        RotClientUiDraw.text(
                graphics,
                font,
                RotClientUiDraw.ellipsize(
                        font,
                        blank(
                                item.museumSet(),
                                "Not indexed"),
                        276),
                left + 92,
                y + 141,
                RotClientTheme.TEXT,
                false);

        QolUtilityConfig config =
                RotClientClient.qolConfigPublic();

        boolean craftHelper =
                config.isModuleEnabled(
                        "qol.storage_overlay")
                        && config.extras().storageCraftHelper;

        RotClientUiDraw.sectionLabel(
                graphics,
                font,
                "CRAFT TREE",
                left,
                y + 180);

        if (!craftHelper) {

            RotClientUiDraw.drawElevatedCard(
                    graphics,
                    left,
                    y + 198,
                    382,
                    62);

            RotClientUiDraw.text(
                    graphics,
                    font,
                    "Craft helper is disabled",
                    left + 12,
                    y + 211,
                    RotClientTheme.TEXT_DIM,
                    true);

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    "Enable Storage Overlay > Craft Helper to show recipes and owned materials.",
                    left + 12,
                    y + 229);

            return;
        }

        drawTree(
                graphics,
                RotItemIndex.recipeTree(
                        item.id(),
                        1),
                left,
                y + 200,
                0,
                new int[]{0});

        Map<String, Integer> totals =
                RotItemIndex.aggregateIngredients(
                        item.id(),
                        1);

        int totalY =
                y + 326;

        RotClientUiDraw.sectionLabel(
                graphics,
                font,
                "BASE INGREDIENTS",
                left,
                totalY);

        int line =
                0;

        for (Map.Entry<String, Integer> entry
                : totals.entrySet()) {

            if (line++ >= 4) {
                break;
            }

            int owned =
                    ItemToolsRuntime.ownedCount(
                            entry.getKey());

            boolean complete =
                    owned >= entry.getValue();

            String text =
                    entry.getKey()
                            + ": "
                            + owned
                            + " / "
                            + entry.getValue()
                            + (complete
                            ? "  owned"
                            : "  missing");

            RotClientUiDraw.text(
                    graphics,
                    font,
                    RotClientUiDraw.ellipsize(
                            font,
                            text,
                            370),
                    left,
                    totalY + 18 * line,
                    complete
                            ? RotClientTheme.SUCCESS
                            : RotClientTheme.TEXT_DIM,
                    false);
        }

        if (totals.isEmpty()) {

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    "No base ingredients indexed for this item.",
                    left,
                    totalY + 20);
        }
    }
    private void drawTree(
            GuiGraphicsExtractor graphics,
            RotItemIndex.RecipeNode node,
            int x,
            int y,
            int depth,
            int[] row) {
        if (row[0] >= 7) {
            return;
        }
        int lineY = y + row[0]++ * 18;
        String prefix = depth == 0 ? "" : "  ".repeat(Math.min(5, depth)) + "↳ ";
        String suffix = node.cycle() ? " [cycle]" : "";
        RotClientUiDraw.text(
                graphics, font,
                prefix + node.quantity() + " × " + node.itemId() + suffix,
                x, lineY,
                node.cycle() ? RotClientTheme.ERROR : RotClientTheme.TEXT_DIM,
                false);
        for (RotItemIndex.RecipeNode child : node.children()) {
            drawTree(graphics, child, x, y, depth + 1, row);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        int mx = (int) Math.round(event.x());
        int my = (int) Math.round(event.y());
        int x = panelX();
        int y = panelY();
        int rowY = y + 114;
        for (RotItemIndex.ItemDef item : results()) {
            if (RotClientUiDraw.inside(mx, my, x + 18, rowY, 370, 30)) {
                selectedId = item.id();
                return true;
            }
            rowY += 34;
        }
        if (RotClientUiDraw.inside(mx, my, x + WIDTH - 126, y + HEIGHT - 36, 104, 24)) {
            closeToParent();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!event.isAllowedChatCharacter() || query.length() >= 80) {
            return super.charTyped(event);
        }
        query += event.codepointAsString();
        selectedId = "";
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
            query = query.substring(0, query.offsetByCodePoints(query.length(), -1));
            selectedId = "";
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        closeToParent();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<RotItemIndex.ItemDef> results() {
        return RotItemIndex.search(query, 9);
    }

    private RotItemIndex.ItemDef selected(List<RotItemIndex.ItemDef> results) {
        if (!selectedId.isBlank()) {
            RotItemIndex.ItemDef item = RotItemIndex.find(selectedId);
            if (item != null) {
                return item;
            }
        }
        return results.isEmpty() ? null : results.getFirst();
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void closeToParent() {
        Minecraft.getInstance().setScreen(parent);
    }

    private int panelX() {
        return Math.max(8, (width - WIDTH) / 2);
    }

    private int panelY() {
        return Math.max(8, (height - HEIGHT) / 2);
    }
}
