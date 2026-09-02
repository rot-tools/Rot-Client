package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Local, non-pausing editor for ordered command-key sequences. */
final class HotkeySequenceEditorScreen extends Screen {
    private static final int WIDTH = 780;
    private static final int HEIGHT = 450;
    private final Screen parent;
    private final List<RingPolicy.MacroDef> drafts;
    private int selected;
    private int field;

    HotkeySequenceEditorScreen(Screen parent) {
        super(Component.literal("Hotkey Sequences"));
        this.parent = parent;
        QolUtilityConfig config = RotClientClient.qolConfigPublic();
        this.drafts = new ArrayList<>(RingPolicy.parseMacroList(
                config.commandBindMacros,
                config.commandBindSendMode,
                config.commandBindConflict,
                config.commandBindActivation,
                config.commandBindUseRatelimit));
        selected = drafts.isEmpty() ? -1 : 0;
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int x = panelX();
        int y = panelY();
        RotClientUiDraw.drawShadowedPanel(graphics, x, y, WIDTH, HEIGHT);
        RotClientUiDraw.drawHeaderBar(
                graphics, font, x, y, WIDTH, 40,
                "Hotkey Sequences",
                "Ordered commands run locally through the existing rate limiter");
        graphics.fill(x + 1, y + 41, x + 248, y + HEIGHT - 1, RotClientTheme.SURFACE_ALT);
        drawList(graphics, x, y);
        drawEditor(graphics, x, y);
        drawButton(graphics, mouseX, mouseY, x + 14, y + HEIGHT - 36, 104, "Add", true);
        drawButton(graphics, mouseX, mouseY, x + 126, y + HEIGHT - 36, 104,
                "Delete", selected >= 0);
        String error = firstError();
        drawButton(graphics, mouseX, mouseY, x + WIDTH - 238, y + HEIGHT - 36, 104,
                "Save", error.isEmpty());
        drawButton(graphics, mouseX, mouseY, x + WIDTH - 126, y + HEIGHT - 36, 104,
                "Cancel", true);
        if (!error.isEmpty()) {
            RotClientUiDraw.text(
                    graphics, font, error,
                    x + 264, y + HEIGHT - 31,
                    RotClientTheme.ERROR, false);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void drawList(GuiGraphicsExtractor graphics, int x, int y) {
        int rowY = y + 54;
        if (drafts.isEmpty()) {
            RotClientUiDraw.text(
                    graphics, font, "No custom sequences",
                    x + 50, rowY + 120, RotClientTheme.TEXT_MUTED, true);
            return;
        }
        for (int i = 0; i < Math.min(drafts.size(), 11); i++) {
            RingPolicy.MacroDef macro = drafts.get(i);
            int fill = i == selected ? RotClientTheme.SELECTED_ROW : RotClientTheme.SURFACE;
            graphics.fill(x + 10, rowY, x + 238, rowY + 28, fill);
            if (i == selected) {
                graphics.fill(x + 10, rowY, x + 13, rowY + 28, RotClientTheme.HUD_ACCENT);
            }
            String label = macro.key().isBlank() ? "Unbound" : macro.key();
            RotClientUiDraw.text(graphics, font, label, x + 20, rowY + 6,
                    RotClientTheme.TEXT, true);
            RotClientUiDraw.text(
                    graphics, font, macro.messages().size() + " steps",
                    x + 164, rowY + 6, RotClientTheme.TEXT_MUTED, false);
            rowY += 32;
        }
    }

    private void drawEditor(GuiGraphicsExtractor graphics, int x, int y) {
        int left = x + 270;
        if (selected < 0) {
            RotClientUiDraw.text(
                    graphics, font, "Add or select a sequence to edit it.",
                    left + 120, y + 190, RotClientTheme.TEXT_MUTED, true);
            return;
        }
        RingPolicy.MacroDef macro = drafts.get(selected);
        drawField(graphics, left, y + 66, 470, "Key", macro.key(), field == 1);
        drawField(graphics, left, y + 124, 470, "Ordered actions", RingPolicy.formatMessageSequence(macro.messages()), field == 2);
        RotClientUiDraw.text(
                graphics, font,
                "Separate steps with  ,,  and add @ticks for per-step delay",
                left, y + 166, RotClientTheme.TEXT_MUTED, false);
        drawField(graphics, left, y + 196, 220, "Delay between activations (ticks)",
                Integer.toString(macro.spaceTicks()), field == 3);
        drawButton(graphics, -1, -1, left + 244, y + 218, 226,
                "Rate limit: " + (macro.useRatelimit() ? "On" : "Off"), true);
        RotClientUiDraw.text(
                graphics, font,
                "Examples: /pets,, /warp hub@20,, hello",
                left, y + 284, RotClientTheme.TEXT_DIM, false);
        RotClientUiDraw.text(
                graphics, font,
                "Key capture accepts keyboard keys. Existing preset binds are unchanged.",
                left, y + 302, RotClientTheme.TEXT_MUTED, false);
    }

    private void drawField(
            GuiGraphicsExtractor graphics, int x, int y, int width,
            String label, String value, boolean focused) {
        RotClientUiDraw.text(graphics, font, label, x, y, RotClientTheme.TEXT_DIM, true);
        graphics.fill(
                x, y + 18, x + width, y + 42,
                focused ? RotClientTheme.FIELD_ACTIVE : RotClientTheme.FIELD);
        String shown = value == null || value.isEmpty()
                ? (focused && field == 1 ? "Press a key..." : "")
                : value;
        RotClientUiDraw.text(
                graphics, font,
                RotClientUiDraw.ellipsize(font, shown, width - 16),
                x + 8, y + 25,
                value == null || value.isEmpty()
                        ? RotClientTheme.TEXT_MUTED
                        : RotClientTheme.TEXT,
                false);
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
        int rowY = y + 54;
        for (int i = 0; i < Math.min(drafts.size(), 11); i++) {
            if (RotClientUiDraw.inside(mx, my, x + 10, rowY, 228, 28)) {
                selected = i;
                field = 0;
                return true;
            }
            rowY += 32;
        }
        if (RotClientUiDraw.inside(mx, my, x + 14, y + HEIGHT - 36, 104, 24)) {
            drafts.add(defaultMacro());
            selected = drafts.size() - 1;
            field = 1;
            return true;
        }
        if (selected >= 0 && RotClientUiDraw.inside(
                mx, my, x + 126, y + HEIGHT - 36, 104, 24)) {
            drafts.remove(selected);
            selected = drafts.isEmpty() ? -1 : Math.min(selected, drafts.size() - 1);
            field = 0;
            return true;
        }
        if (selected >= 0) {
            int left = x + 270;
            if (RotClientUiDraw.inside(mx, my, left, y + 84, 470, 24)) {
                field = 1;
                return true;
            }
            if (RotClientUiDraw.inside(mx, my, left, y + 142, 470, 24)) {
                field = 2;
                return true;
            }
            if (RotClientUiDraw.inside(mx, my, left, y + 214, 220, 24)) {
                field = 3;
                return true;
            }
            if (RotClientUiDraw.inside(mx, my, left + 244, y + 218, 226, 24)) {
                update(useRateLimit(current(), !current().useRatelimit()));
                return true;
            }
        }
        if (RotClientUiDraw.inside(mx, my, x + WIDTH - 238, y + HEIGHT - 36, 104, 24)
                && firstError().isEmpty()) {
            saveAndClose();
            return true;
        }
        if (RotClientUiDraw.inside(mx, my, x + WIDTH - 126, y + HEIGHT - 36, 104, 24)) {
            closeToParent();
            return true;
        }
        field = 0;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (selected < 0 || (field != 2 && field != 3) || !event.isAllowedChatCharacter()) {
            return super.charTyped(event);
        }
        String value = fieldValue(current()) + event.codepointAsString();
        replaceField(value);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (selected >= 0 && field == 1) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                field = 0;
                return true;
            }
            String key = QolKeybindNames.formatGlfwKey(event.key());
            update(withKey(current(), key));
            field = 0;
            return true;
        }
        if (selected >= 0 && field != 0 && event.key() == GLFW.GLFW_KEY_BACKSPACE) {
            String value = fieldValue(current());
            if (!value.isEmpty()) {
                value = value.substring(0, value.offsetByCodePoints(value.length(), -1));
                replaceField(value);
            }
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_TAB && selected >= 0) {
            field = field % 3 + 1;
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

    private void replaceField(String value) {
        RingPolicy.MacroDef macro = current();
        if (field == 2) {
            update(withMessages(macro, RingPolicy.parseMessageSequence(value)));
        } else if (field == 3) {
            int ticks;
            try {
                ticks = Integer.parseInt(value.isBlank() ? "0" : value);
            } catch (NumberFormatException ignored) {
                return;
            }
            update(withSpaceTicks(macro, Math.min(200, Math.max(0, ticks))));
        }
    }

    private String fieldValue(RingPolicy.MacroDef macro) {
        return switch (field) {
            case 2 -> RingPolicy.formatMessageSequence(macro.messages());
            case 3 -> Integer.toString(macro.spaceTicks());
            default -> "";
        };
    }

    private void saveAndClose() {
        QolUtilityConfig config = RotClientClient.qolConfigPublic();
        config.commandBindMacros = RingPolicy.serializeMacroList(drafts);
        TrackerStore.save(RotClientClient.trackerConfig());
        closeToParent();
    }

    private void closeToParent() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    private String firstError() {
        for (int i = 0; i < drafts.size(); i++) {
            String error = RingPolicy.validationError(drafts.get(i));
            if (!error.isEmpty()) {
                return "Sequence " + (i + 1) + ": " + error;
            }
        }
        return "";
    }

    private RingPolicy.MacroDef current() {
        return drafts.get(selected);
    }

    private void update(RingPolicy.MacroDef macro) {
        drafts.set(selected, macro);
    }

    private static RingPolicy.MacroDef defaultMacro() {
        return new RingPolicy.MacroDef(
                "", "", "", List.of(new RingPolicy.MacroMessage("/pets", 0)),
                RingPolicy.MODE_SEND, RingPolicy.STRATEGY_ASSERT,
                RingPolicy.ACTIVATION_HOLD, 0, 0, false, true);
    }

    private static RingPolicy.MacroDef withKey(RingPolicy.MacroDef m, String key) {
        return copy(m, key, m.messages(), m.spaceTicks(), m.useRatelimit());
    }

    private static RingPolicy.MacroDef withMessages(
            RingPolicy.MacroDef m, List<RingPolicy.MacroMessage> messages) {
        return copy(m, m.key(), messages, m.spaceTicks(), m.useRatelimit());
    }

    private static RingPolicy.MacroDef withSpaceTicks(RingPolicy.MacroDef m, int ticks) {
        return copy(m, m.key(), m.messages(), ticks, m.useRatelimit());
    }

    private static RingPolicy.MacroDef useRateLimit(RingPolicy.MacroDef m, boolean enabled) {
        return copy(m, m.key(), m.messages(), m.spaceTicks(), enabled);
    }

    private static RingPolicy.MacroDef copy(
            RingPolicy.MacroDef m,
            String key,
            List<RingPolicy.MacroMessage> messages,
            int spaceTicks,
            boolean useRatelimit) {
        return new RingPolicy.MacroDef(
                key, m.limitKey(), m.altKey(), messages, m.sendMode(),
                m.conflictStrategy(), m.activationType(), spaceTicks,
                m.maxRepeats(), m.skyblockOnly(), useRatelimit);
    }

    private void drawButton(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            int x, int y, int width, String label, boolean enabled) {
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY,
                x, y, width, label, false, enabled);
    }

    private int panelX() {
        return Math.max(8, (this.width - WIDTH) / 2);
    }

    private int panelY() {
        return Math.max(8, (this.height - HEIGHT) / 2);
    }
}
