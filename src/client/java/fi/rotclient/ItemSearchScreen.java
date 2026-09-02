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
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = panelX();
        int y = panelY();
        RotClientUiDraw.drawShadowedPanel(graphics, x, y, WIDTH, HEIGHT);
        RotClientUiDraw.drawHeaderBar(
                graphics, font, x, y, WIDTH, 40,
                "Item Search", "Bundled offline recipes, sources and museum sets");
        graphics.fill(x + 18, y + 54, x + 388, y + 80, RotClientTheme.FIELD_ACTIVE);
        RotClientUiDraw.text(
                graphics, font,
                query.isBlank() ? "Type an item name or ID..." : query,
                x + 28, y + 62,
                query.isBlank() ? RotClientTheme.TEXT_MUTED : RotClientTheme.TEXT,
                false);
        RotClientUiDraw.text(
                graphics, font, "Search results",
                x + 18, y + 94, RotClientTheme.HUD_ACCENT, true);
        List<RotItemIndex.ItemDef> results = results();
        int rowY = y + 114;
        for (RotItemIndex.ItemDef item : results) {
            boolean selected = item.id().equals(selectedId);
            graphics.fill(
                    x + 18, rowY, x + 388, rowY + 30,
                    selected ? RotClientTheme.SELECTED_ROW : RotClientTheme.SURFACE_ALT);
            RotClientUiDraw.text(
                    graphics, font, item.name(),
                    x + 28, rowY + 5, RotClientTheme.TEXT, true);
            RotClientUiDraw.text(
                    graphics, font, item.id(),
                    x + 208, rowY + 5, RotClientTheme.TEXT_MUTED, false);
            rowY += 34;
        }
        drawDetails(graphics, x, y, selected(results));
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY,
                x + WIDTH - 126, y + HEIGHT - 36, 104,
                "Done", false, true);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawDetails(
            GuiGraphicsExtractor graphics, int x, int y, RotItemIndex.ItemDef item) {
        int left = x + 414;
        if (item == null) {
            RotClientUiDraw.text(
                    graphics, font, "Select an item to inspect it.",
                    left + 70, y + 210, RotClientTheme.TEXT_MUTED, true);
            return;
        }
        RotClientUiDraw.text(graphics, font, item.name(), left, y + 58, RotClientTheme.TEXT, true);
        RotClientUiDraw.text(graphics, font, item.id(), left, y + 76, RotClientTheme.TEXT_MUTED, false);
        RotClientUiDraw.text(
                graphics, font, "Source: " + blank(item.source(), "Unknown"),
                left, y + 106, RotClientTheme.TEXT_DIM, false);
        RotClientUiDraw.text(
                graphics, font, "Museum: " + blank(item.museumSet(), "Not indexed"),
                left, y + 124, RotClientTheme.TEXT_DIM, false);
        QolUtilityConfig config = RotClientClient.qolConfigPublic();
        if (!config.isModuleEnabled("qol.storage_overlay")
                || !config.extras().storageCraftHelper) {
            RotClientUiDraw.text(
                    graphics, font, "Craft helper disabled",
                    left, y + 158, RotClientTheme.TEXT_MUTED, false);
            return;
        }
        RotClientUiDraw.text(graphics, font, "Craft tree", left, y + 158, RotClientTheme.HUD_ACCENT, true);
        drawTree(graphics, RotItemIndex.recipeTree(item.id(), 1), left, y + 180, 0, new int[]{0});
        Map<String, Integer> totals = RotItemIndex.aggregateIngredients(item.id(), 1);
        int totalY = y + 326;
        RotClientUiDraw.text(graphics, font, "Base ingredients", left, totalY, RotClientTheme.HUD_ACCENT, true);
        int line = 0;
        for (Map.Entry<String, Integer> entry : totals.entrySet()) {
            if (line++ >= 4) {
                break;
            }
            RotClientUiDraw.text(
                    graphics, font,
                    entry.getKey() + ": " + ItemToolsRuntime.ownedCount(entry.getKey())
                            + " / " + entry.getValue()
                            + (ItemToolsRuntime.ownedCount(entry.getKey()) >= entry.getValue()
                                    ? " §aowned" : " §cmissing"),
                    left, totalY + 18 * line, RotClientTheme.TEXT_DIM, false);
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
        Minecraft.getInstance().gui.setScreen(parent);
    }

    private int panelX() {
        return Math.max(8, (width - WIDTH) / 2);
    }

    private int panelY() {
        return Math.max(8, (height - HEIGHT) / 2);
    }
}
