package fi.rotclient.rebuild;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class AppearanceScreen extends Screen {
    private static final String[] LABELS = {"Panel", "Accent", "HUD background", "HUD text"};
    private final Screen parent;
    private final EditBox[] fields = new EditBox[LABELS.length];
    private String message = "";
    private int left, top, panelWidth;

    AppearanceScreen(Screen parent) {
        super(Component.literal("Appearance"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(340, width - 20);
        left = (width - panelWidth) / 2;
        top = Math.max(4, (height - 156) / 2);
        InterfaceSettings settings = RebuildClient.settings();
        int[] values = {settings.panel, settings.accent, settings.hudBackground, settings.hudText};
        for (int i = 0; i < fields.length; i++) {
            fields[i] = new EditBox(font, left + panelWidth - 105, top + 24 + 23 * i, 91, 18,
                    Component.literal(LABELS[i]));
            fields[i].setMaxLength(7);
            fields[i].setValue(String.format(Locale.ROOT, "#%06X", values[i] & 0xFFFFFF));
            addRenderableWidget(fields[i]);
        }
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(left + 10, top + 128, 52, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Apply"), b -> save())
                .bounds(left + panelWidth - 75, top + 128, 65, 20).build());
    }

    private void save() {
        int[] colors = new int[fields.length];
        for (int i = 0; i < fields.length; i++) {
            String value = fields[i].getValue().strip().replaceFirst("^#", "");
            if (!value.matches("[0-9a-fA-F]{6}")) {
                message = LABELS[i] + " needs #RRGGBB.";
                return;
            }
            colors[i] = Integer.parseInt(value, 16);
        }
        InterfaceSettings settings = RebuildClient.settings();
        settings.panel = colors[0];
        settings.accent = colors[1];
        settings.hudBackground = colors[2];
        settings.hudText = colors[3];
        settings.save();
        onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        InterfaceSettings settings = RebuildClient.settings();
        graphics.fill(left, top, left + panelWidth, top + 156, 0xFF000000 | settings.panel);
        graphics.fill(left, top, left + panelWidth, top + 2, 0xFF000000 | settings.accent);
        graphics.text(font, getTitle(), left + 10, top + 9, 0xFFF8F5FF);
        for (int i = 0; i < LABELS.length; i++) {
            graphics.text(font, LABELS[i], left + 10, top + 29 + 23 * i, 0xFFC8BFD8);
        }
        graphics.text(font, message, left + 10, top + 117, 0xFFFF7996);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
