package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Per-region inventory background colors, opened from the in-game wrench. */
final class InventoryChromeColorsScreen extends Screen {
    private record Row(String id, String label) {}

    private static final Row[] ROWS = {
            new Row("qol.inventory_overlay.chrome_panel", "Whole inventory"),
            new Row("qol.inventory_overlay.chrome_header", "Top / armor"),
            new Row("qol.inventory_overlay.chrome_main", "Main inventory"),
            new Row("qol.inventory_overlay.chrome_hotbar", "Hotbar"),
            new Row("qol.inventory_overlay.chrome_border", "Border"),
            new Row("qol.storage_overlay.panel_color", "Storage panel"),
            new Row("qol.storage_overlay.card_color", "Storage cards"),
            new Row("qol.storage_overlay.card_active_color", "Open storage"),
            new Row("qol.storage_overlay.player_color", "Storage inventory")
    };

    private final Screen parent;

    InventoryChromeColorsScreen(Screen parent) {
        super(Component.literal("Inventory colors"));
        this.parent = parent;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int width = 360;
        int height = 58 + ROWS.length * 32 + 40;
        int x = Math.max(12, (this.width - width) / 2);
        int y = Math.max(12, (this.height - height) / 2);
        RotClientUiDraw.drawShadowedPanel(graphics, x, y, width, height);
        RotClientUiDraw.drawHeaderBar(graphics, font, x, y, width, 38,
                "Inventory colors", "Pick a background tint for each region");
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        for (int i = 0; i < ROWS.length; i++) {
            Row row = ROWS[i];
            int rowY = y + 50 + i * 32;
            Integer color = qol.readColor(row.id);
            int fill = color == null ? 0xFF22222C : color;
            RotClientUiDraw.text(graphics, font, row.label, x + 18, rowY + 8, RotClientTheme.TEXT, false);
            RotClientUiDraw.drawColorSwatch(graphics, x + width - 52, rowY + 4, 28, 22, fill, false);
        }
        RotClientUiDraw.drawButton(
                graphics, font, mouseX, mouseY, x + width - 116, y + height - 34, 96, "Done", true, true);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        int mx = (int) Math.round(event.x());
        int my = (int) Math.round(event.y());
        int width = 360;
        int height = 58 + ROWS.length * 32 + 40;
        int x = Math.max(12, (this.width - width) / 2);
        int y = Math.max(12, (this.height - height) / 2);
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        for (int i = 0; i < ROWS.length; i++) {
            int rowY = y + 50 + i * 32;
            if (mx >= x + 12 && mx < x + width - 12 && my >= rowY && my < rowY + 28) {
                Row row = ROWS[i];
                Integer current = qol.readColor(row.id);
                int initial = current == null ? 0xA01C1C24 : current;
                Minecraft.getInstance().gui.setScreen(
                        new RotClientColorPickerScreen(
                                this,
                                row.label,
                                initial,
                                color -> {
                                    qol.writeColor(row.id, color);
                                    TrackerStore.save(RotClientClient.trackerConfig());
                                }));
                return true;
            }
        }
        if (mx >= x + width - 116 && mx < x + width - 20 && my >= y + height - 34 && my < y + height - 10) {
            onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}
