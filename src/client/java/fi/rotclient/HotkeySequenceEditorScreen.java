package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

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
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta) {

        int x = panelX();
        int y = panelY();

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
                "Hotkey Sequences",
                "Create ordered command sequences and bind them to a key.");

        /*
         * Consistent list/detail editor layout.
         */
        RotClientUiDraw.drawElevatedCard(
                graphics,
                x + 8,
                y + 48,
                240,
                350);

        RotClientUiDraw.drawElevatedCard(
                graphics,
                x + 258,
                y + 48,
                WIDTH - 272,
                350);

        drawList(
                graphics,
                x,
                y,
                mouseX,
                mouseY);

        drawEditor(
                graphics,
                x,
                y,
                mouseX,
                mouseY);

        /*
         * Footer separation keeps actions distinct from editor content.
         */
        graphics.fill(
                x + 10,
                y + HEIGHT - 46,
                x + WIDTH - 10,
                y + HEIGHT - 45,
                RotClientTheme.DIVIDER);

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 14,
                y + HEIGHT - 36,
                104,
                24,
                "Add",
                false,
                true);

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + 126,
                y + HEIGHT - 36,
                104,
                24,
                "Delete",
                false,
                selected >= 0);

        String error =
                firstError();

        if (!error.isEmpty()) {
            RotClientUiDraw.text(
                    graphics,
                    font,
                    RotClientUiDraw.ellipsize(
                            font,
                            error,
                            250),
                    x + 264,
                    y + HEIGHT - 29,
                    RotClientTheme.ERROR,
                    false);
        }

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + WIDTH - 238,
                y + HEIGHT - 36,
                104,
                24,
                "Save",
                true,
                error.isEmpty());

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                x + WIDTH - 126,
                y + HEIGHT - 36,
                104,
                24,
                "Cancel",
                false,
                true);

        super.extractRenderState(
                graphics,
                mouseX,
                mouseY,
                delta);
    }
    private void drawList(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int mouseX,
            int mouseY) {

        int rowY =
                y + 54;

        if (drafts.isEmpty()) {

            RotClientUiDraw.text(
                    graphics,
                    font,
                    "No sequences yet",
                    x + 69,
                    y + 180,
                    RotClientTheme.TEXT_DIM,
                    true);

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    "Use Add below to create one.",
                    x + 45,
                    y + 197);

            return;
        }

        for (int i = 0;
             i < Math.min(
                     drafts.size(),
                     11);
             i++) {

            RingPolicy.MacroDef macro =
                    drafts.get(i);

            boolean active =
                    i == selected;

            boolean hover =
                    RotClientUiDraw.inside(
                            mouseX,
                            mouseY,
                            x + 10,
                            rowY,
                            228,
                            28);

            RotClientUiDraw.drawInteractiveSurface(
                    graphics,
                    x + 10,
                    rowY,
                    228,
                    28,
                    hover
                            ? 1.0F
                            : 0.0F,
                    active,
                    RotClientTheme.HUD_ACCENT,
                    RotClientUiDraw.RADIUS_SM);

            if (active) {
                graphics.fill(
                        x + 11,
                        rowY + 5,
                        x + 14,
                        rowY + 23,
                        RotClientTheme.HUD_ACCENT);
            }

            String label =
                    macro.key().isBlank()
                            ? "Unbound"
                            : macro.key();

            RotClientUiDraw.text(
                    graphics,
                    font,
                    RotClientUiDraw.ellipsize(
                            font,
                            label,
                            128),
                    x + 20,
                    rowY + 6,
                    active
                            ? RotClientTheme.TEXT
                            : RotClientTheme.TEXT_DIM,
                    true);

            String steps =
                    macro.messages().size()
                            + " step"
                            + (macro.messages().size() == 1
                            ? ""
                            : "s");

            RotClientUiDraw.text(
                    graphics,
                    font,
                    steps,
                    x + 228 - font.width(steps),
                    rowY + 6,
                    RotClientTheme.TEXT_MUTED,
                    false);

            rowY += 32;
        }
    }
    private void drawEditor(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int mouseX,
            int mouseY) {

        int left =
                x + 270;

        if (selected < 0) {

            RotClientUiDraw.text(
                    graphics,
                    font,
                    "Select a sequence",
                    left + 154,
                    y + 180,
                    RotClientTheme.TEXT_DIM,
                    true);

            RotClientUiDraw.helpText(
                    graphics,
                    font,
                    "Choose one from the list or create a new sequence.",
                    left + 88,
                    y + 198);

            return;
        }

        RingPolicy.MacroDef macro =
                drafts.get(selected);

        RotClientUiDraw.sectionLabel(
                graphics,
                font,
                "SEQUENCE DETAILS",
                left,
                y + 53);

        drawField(
                graphics,
                left,
                y + 66,
                470,
                "Key",
                macro.key(),
                field == 1);

        drawField(
                graphics,
                left,
                y + 124,
                470,
                "Ordered actions",
                RingPolicy.formatMessageSequence(
                        macro.messages()),
                field == 2);

        RotClientUiDraw.helpText(
                graphics,
                font,
                "Separate steps with ,, and use @ticks for a per-step delay.",
                left,
                y + 166);

        drawField(
                graphics,
                left,
                y + 196,
                220,
                "Delay between activations (ticks)",
                Integer.toString(
                        macro.spaceTicks()),
                field == 3);

        RotClientUiDraw.drawPremiumButton(
                graphics,
                font,
                mouseX,
                mouseY,
                left + 244,
                y + 218,
                226,
                24,
                macro.useRatelimit()
                        ? "Rate limit: On"
                        : "Rate limit: Off",
                macro.useRatelimit(),
                true);

        RotClientUiDraw.drawElevatedCard(
                graphics,
                left,
                y + 270,
                470,
                54);

        RotClientUiDraw.text(
                graphics,
                font,
                "Example",
                left + 10,
                y + 279,
                RotClientTheme.TEXT_DIM,
                true);

        RotClientUiDraw.helpText(
                graphics,
                font,
                "/pets,, /warp hub@20,, hello",
                left + 10,
                y + 294);

        RotClientUiDraw.helpText(
                graphics,
                font,
                "Key capture accepts keyboard keys. Preset binds remain unchanged.",
                left + 10,
                y + 308);
    }
    private void drawField(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            String label,
            String value,
            boolean focused) {

        RotClientUiDraw.text(
                graphics,
                font,
                label,
                x,
                y,
                focused
                        ? RotClientTheme.HUD_ACCENT
                        : RotClientTheme.TEXT_DIM,
                true);

        RotClientTheme.drawInset(
                graphics,
                x,
                y + 18,
                width,
                24,
                focused);

        boolean empty =
                value == null
                        || value.isEmpty();

        String shown =
                empty
                        ? (focused && field == 1
                        ? "Press a key..."
                        : "")
                        : RotClientUiDraw.ellipsize(
                                font,
                                value,
                                width - 16);

        if (!shown.isEmpty()) {

            RotClientUiDraw.text(
                    graphics,
                    font,
                    shown,
                    x + 8,
                    y + 25,
                    empty
                            ? RotClientTheme.TEXT_MUTED
                            : RotClientTheme.TEXT,
                    false);
        }

        if (focused) {

            int cursorX =
                    x + 8
                            + (shown.isEmpty()
                            ? 0
                            : font.width(shown));

            cursorX =
                    Math.min(
                            x + width - 6,
                            cursorX);

            graphics.fill(
                    cursorX,
                    y + 23,
                    cursorX + 1,
                    y + 37,
                    RotClientTheme.TEXT);
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
            if (event.key() == InputConstants.KEY_ESCAPE) {
                field = 0;
                return true;
            }
            String key = QolKeybindNames.formatGlfwKey(event.key());
            update(withKey(current(), key));
            field = 0;
            return true;
        }
        if (selected >= 0 && field != 0 && event.key() == InputConstants.KEY_BACKSPACE) {
            String value = fieldValue(current());
            if (!value.isEmpty()) {
                value = value.substring(0, value.offsetByCodePoints(value.length(), -1));
                replaceField(value);
            }
            return true;
        }
        if (event.key() == InputConstants.KEY_TAB && selected >= 0) {
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
