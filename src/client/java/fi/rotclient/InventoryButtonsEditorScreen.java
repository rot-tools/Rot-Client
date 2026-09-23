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

/** Editor for inventory shortcut buttons: list on the left, details on the right. */
final class InventoryButtonsEditorScreen extends Screen {
    private static final int PANEL_W = 720;
    private static final int PANEL_H = 440;
    private final Screen parent;
    private int selected;
    private int field;

    InventoryButtonsEditorScreen(Screen parent) {
        super(Component.literal("Inventory Buttons Editor"));
        this.parent = parent;
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor g,
            int mx,
            int my,
            float delta) {

        int x =
                Math.max(
                        12,
                        (width - PANEL_W) / 2);

        int y =
                Math.max(
                        12,
                        (height - PANEL_H) / 2);

        RotClientUiDraw.drawShadowedPanel(
                g,
                x,
                y,
                PANEL_W,
                PANEL_H);

        RotClientUiDraw.drawHeaderBar(
                g,
                font,
                x,
                y,
                PANEL_W,
                42,
                "Inventory Buttons",
                "Create inventory shortcuts, choose their icons and position them.");

        var buttons =
                extras().inventoryButtons;

        RotClientUiDraw.drawElevatedCard(
                g,
                x + 16,
                y + 56,
                234,
                304);

        RotClientUiDraw.drawElevatedCard(
                g,
                x + 266,
                y + 56,
                PANEL_W - 282,
                304);

        RotClientUiDraw.text(
                g,
                font,
                "BUTTONS",
                x + 28,
                y + 66,
                RotClientTheme.VIOLET,
                true);

        String countText =
                buttons.size()
                        + " / 12";

        RotClientUiDraw.text(
                g,
                font,
                countText,
                x + 238 - font.width(countText),
                y + 66,
                RotClientTheme.TEXT_MUTED,
                false);

        if (buttons.isEmpty()) {

            RotClientUiDraw.text(
                    g,
                    font,
                    "No buttons yet",
                    x + 76,
                    y + 180,
                    RotClientTheme.TEXT_DIM,
                    true);

            RotClientUiDraw.helpText(
                    g,
                    font,
                    "Add one or load a preset.",
                    x + 60,
                    y + 197);

        } else {

            for (int i = 0;
                 i < Math.min(
                         12,
                         buttons.size());
                 i++) {

                int row =
                        y + 88 + i * 22;

                boolean active =
                        i == selected;

                boolean hover =
                        RotClientUiDraw.inside(
                                mx,
                                my,
                                x + 22,
                                row - 3,
                                222,
                                20);

                RotClientUiDraw.drawInteractiveSurface(
                        g,
                        x + 22,
                        row - 3,
                        222,
                        20,
                        hover
                                ? 1.0F
                                : 0.0F,
                        active,
                        RotClientTheme.HUD_ACCENT,
                        RotClientUiDraw.RADIUS_SM);

                if (active) {
                    g.fill(
                            x + 23,
                            row + 1,
                            x + 26,
                            row + 13,
                            RotClientTheme.HUD_ACCENT);
                }

                var b =
                        buttons.get(i);

                String label =
                        (i + 1)
                                + ".  /"
                                + InventoryButtonsPolicy.normalizeCommand(
                                b.command);

                RotClientUiDraw.text(
                        g,
                        font,
                        RotClientUiDraw.ellipsize(
                                font,
                                label,
                                194),
                        x + 30,
                        row,
                        active
                                ? RotClientTheme.TEXT
                                : RotClientTheme.TEXT_DIM,
                        true);
            }
        }

        RotClientUiDraw.text(
                g,
                font,
                "SELECTED BUTTON",
                x + 280,
                y + 66,
                RotClientTheme.VIOLET,
                true);

        if (!buttons.isEmpty()) {

            var b =
                    current();

            int rx =
                    x + 280;

            drawField(
                    g,
                    rx,
                    y + 88,
                    400,
                    "Command (without /)",
                    b.command,
                    field == 1);

            drawField(
                    g,
                    rx,
                    y + 140,
                    400,
                    "Icon item id",
                    b.icon,
                    field == 2);

            RotClientUiDraw.text(
                    g,
                    font,
                    "Position",
                    rx,
                    y + 196,
                    RotClientTheme.TEXT_MUTED,
                    true);

            drawStepper(
                    g,
                    mx,
                    my,
                    rx,
                    y + 212,
                    "X offset",
                    b.x);

            drawStepper(
                    g,
                    mx,
                    my,
                    rx + 210,
                    y + 212,
                    "Y offset",
                    b.y);

            RotClientUiDraw.text(
                    g,
                    font,
                    "Size and anchors",
                    rx,
                    y + 252,
                    RotClientTheme.TEXT_MUTED,
                    true);

            drawToggle(
                    g,
                    mx,
                    my,
                    rx,
                    y + 270,
                    "Large button",
                    b.large);

            drawToggle(
                    g,
                    mx,
                    my,
                    rx + 210,
                    y + 270,
                    "Anchor right",
                    b.anchorRight);

            drawToggle(
                    g,
                    mx,
                    my,
                    rx,
                    y + 304,
                    "Anchor bottom",
                    b.anchorBottom);

        } else {

            RotClientUiDraw.text(
                    g,
                    font,
                    "Nothing selected",
                    x + 410,
                    y + 184,
                    RotClientTheme.TEXT_DIM,
                    true);

            RotClientUiDraw.helpText(
                    g,
                    font,
                    "Create a button or choose a preset below.",
                    x + 362,
                    y + 202);
        }

        g.fill(
                x + 16,
                y + 366,
                x + PANEL_W - 16,
                y + 367,
                RotClientTheme.DIVIDER);

        RotClientUiDraw.drawPremiumButton(
                g,
                font,
                mx,
                my,
                x + 16,
                y + 376,
                88,
                24,
                "Add",
                false,
                buttons.size() < 12);

        RotClientUiDraw.drawPremiumButton(
                g,
                font,
                mx,
                my,
                x + 112,
                y + 376,
                88,
                24,
                "Delete",
                false,
                !buttons.isEmpty());

        RotClientUiDraw.drawPremiumButton(
                g,
                font,
                mx,
                my,
                x + 208,
                y + 376,
                110,
                24,
                "Duplicate",
                false,
                !buttons.isEmpty()
                        && buttons.size() < 12);

        RotClientUiDraw.drawPremiumButton(
                g,
                font,
                mx,
                my,
                x + 326,
                y + 376,
                110,
                24,
                "Simple",
                false,
                true);

        RotClientUiDraw.drawPremiumButton(
                g,
                font,
                mx,
                my,
                x + 444,
                y + 376,
                110,
                24,
                "Warps",
                false,
                true);

        RotClientUiDraw.drawPremiumButton(
                g,
                font,
                mx,
                my,
                x + 562,
                y + 376,
                142,
                24,
                "Done",
                true,
                true);

        super.extractRenderState(
                g,
                mx,
                my,
                delta);
    }
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        int mx = (int) Math.round(event.x());
        int my = (int) Math.round(event.y());
        int x = Math.max(12, (width - PANEL_W) / 2);
        int y = Math.max(12, (height - PANEL_H) / 2);
        var list = extras().inventoryButtons;
        for (int i = 0; i < Math.min(12, list.size()); i++) {
            if (inside(mx, my, x + 22, y + 85 + i * 22, 222, 20)) {
                selected = i;
                field = 0;
                return true;
            }
        }
        if (!list.isEmpty()) {
            var b = current();
            int rx = x + 280;
            if (inside(mx, my, rx, y + 88, 400, 44)) {
                field = 1;
                return true;
            }
            if (inside(mx, my, rx, y + 140, 400, 44)) {
                field = 2;
                return true;
            }
            if (hitStepperMinus(mx, my, rx, y + 212)) {
                b.x--;
                save();
                return true;
            }
            if (hitStepperPlus(mx, my, rx, y + 212)) {
                b.x++;
                save();
                return true;
            }
            if (hitStepperMinus(mx, my, rx + 210, y + 212)) {
                b.y--;
                save();
                return true;
            }
            if (hitStepperPlus(mx, my, rx + 210, y + 212)) {
                b.y++;
                save();
                return true;
            }
            if (inside(mx, my, rx, y + 270, 190, 24)) {
                b.large = !b.large;
                save();
                return true;
            }
            if (inside(mx, my, rx + 210, y + 270, 190, 24)) {
                b.anchorRight = !b.anchorRight;
                save();
                return true;
            }
            if (inside(mx, my, rx, y + 304, 190, 24)) {
                b.anchorBottom = !b.anchorBottom;
                save();
                return true;
            }
        }
        if (inside(mx, my, x + 16, y + 376, 88, 24)) {
            list.add(new InventoryButtonsPolicy.Button(-20, list.size() * 20, false, false,
                    "minecraft:command_block", "storage", false));
            selected = list.size() - 1;
            save();
            return true;
        }
        if (inside(mx, my, x + 112, y + 376, 88, 24) && !list.isEmpty()) {
            list.remove(selected);
            selected = Math.max(0, Math.min(selected, list.size() - 1));
            save();
            return true;
        }
        if (inside(mx, my, x + 208, y + 376, 110, 24) && !list.isEmpty() && list.size() < 12) {
            list.add(InventoryButtonsPolicy.duplicatedBeside(current()));
            selected = list.size() - 1;
            save();
            return true;
        }
        if (inside(mx, my, x + 326, y + 376, 110, 24)) {
            replace(InventoryButtonsPolicy.simplePreset());
            return true;
        }
        if (inside(mx, my, x + 444, y + 376, 110, 24)) {
            replace(InventoryButtonsPolicy.allWarpsPreset());
            return true;
        }
        if (inside(mx, my, x + 562, y + 376, 142, 24)) {
            onClose();
            return true;
        }
        field = 0;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (field == 0 || extras().inventoryButtons.isEmpty() || !event.isAllowedChatCharacter()) {
            return super.charTyped(event);
        }
        var b = current();
        String next = (field == 1 ? b.command : b.icon) + event.codepointAsString();
        if (field == 1) {
            b.command = next.substring(0, Math.min(128, next.length()));
        } else {
            b.icon = next.substring(0, Math.min(96, next.length()));
        }
        save();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (field != 0 && !extras().inventoryButtons.isEmpty()) {
            if (event.key() == InputConstants.KEY_BACKSPACE) {
                var b = current();
                String value = field == 1 ? b.command : b.icon;
                if (value != null && !value.isEmpty()) {
                    value = value.substring(0, value.offsetByCodePoints(value.length(), -1));
                }
                if (field == 1) {
                    b.command = value;
                } else {
                    b.icon = value;
                }
                save();
                return true;
            }
            if (event.key() == InputConstants.KEY_TAB || event.key() == InputConstants.KEY_RETURN) {
                field = field == 1 ? 2 : 1;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        save();
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawField(
            GuiGraphicsExtractor g,
            int x,
            int y,
            int w,
            String label,
            String value,
            boolean focused) {

        RotClientUiDraw.text(
                g,
                font,
                label,
                x,
                y,
                focused
                        ? RotClientTheme.HUD_ACCENT
                        : RotClientTheme.TEXT_MUTED,
                focused);

        RotClientTheme.drawInset(
                g,
                x,
                y + 14,
                w,
                24,
                focused);

        String shown =
                value == null
                        ? ""
                        : RotClientUiDraw.ellipsize(
                                font,
                                value,
                                w - 16);

        if (!shown.isEmpty()) {
            RotClientUiDraw.text(
                    g,
                    font,
                    shown,
                    x + 8,
                    y + 22,
                    RotClientTheme.TEXT,
                    false);
        }

        if (focused) {

            int cursorX =
                    Math.min(
                            x + w - 6,
                            x + 8
                                    + font.width(shown));

            g.fill(
                    cursorX,
                    y + 19,
                    cursorX + 1,
                    y + 33,
                    RotClientTheme.TEXT);
        }
    }
    private void drawStepper(
            GuiGraphicsExtractor g,
            int mx,
            int my,
            int x,
            int y,
            String label,
            int value) {

        RotClientUiDraw.text(
                g,
                font,
                label + ":  " + value,
                x,
                y + 6,
                RotClientTheme.TEXT,
                false);

        RotClientUiDraw.drawPremiumButton(
                g,
                font,
                mx,
                my,
                x + 130,
                y,
                28,
                24,
                "-",
                false,
                true);

        RotClientUiDraw.drawPremiumButton(
                g,
                font,
                mx,
                my,
                x + 162,
                y,
                28,
                24,
                "+",
                false,
                true);
    }
    private static boolean hitStepperMinus(int mx, int my, int x, int y) {
        return inside(mx, my, x + 130, y, 28, 24);
    }

    private static boolean hitStepperPlus(int mx, int my, int x, int y) {
        return inside(mx, my, x + 162, y, 28, 24);
    }

    private void drawToggle(
            GuiGraphicsExtractor g,
            int mx,
            int my,
            int x,
            int y,
            String label,
            boolean value) {

        boolean hover =
                inside(
                        mx,
                        my,
                        x,
                        y,
                        190,
                        24);

        RotClientUiDraw.drawInteractiveSurface(
                g,
                x,
                y,
                190,
                24,
                hover
                        ? 1.0F
                        : 0.0F,
                false,
                value
                        ? RotClientTheme.HUD_ACCENT
                        : RotClientTheme.BORDER_BRIGHT,
                RotClientUiDraw.RADIUS_SM);

        RotClientUiDraw.text(
                g,
                font,
                label,
                x + 8,
                y + 7,
                value
                        ? RotClientTheme.TEXT
                        : RotClientTheme.TEXT_DIM,
                false);

        RotClientUiDraw.drawToggle(
                g,
                x + 150,
                y + 2,
                value,
                hover);
    }
    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private InventoryButtonsPolicy.Button current() {
        return extras().inventoryButtons.get(Math.max(0, Math.min(selected, extras().inventoryButtons.size() - 1)));
    }

    private void replace(java.util.List<InventoryButtonsPolicy.Button> values) {
        extras().inventoryButtons = new ArrayList<>();
        for (var b : values) {
            extras().inventoryButtons.add(b.copy());
        }
        selected = 0;
        save();
    }

    private static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }

    private static void save() {
        RotClientClient.save();
    }
}
