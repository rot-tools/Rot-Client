package fi.rotclient;

import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Passive item-variant filters, shared by both editions. Changes apply only on Save. */
final class MarketWatchVariantScreen extends Screen {
    private final Screen parent;
    private final BiConsumer<String, String> save;
    private String tier, reforge;
    private EditBox field;
    MarketWatchVariantScreen(Screen parent, String tier, String reforge, BiConsumer<String, String> save) {
        super(Component.literal("Market Watch item variant"));
        this.parent = parent; this.tier = tier; this.reforge = reforge; this.save = save;
    }
    @Override protected void init() {
        int x = (width - 330) / 2, y = (height - 210) / 2;
        addRenderableWidget(Button.builder(Component.literal("Rarity: " + (tier.isBlank() ? "Any" : tier)), b -> {
            var tiers = MarketWatchVariantPolicy.TIERS;
            tier = tiers.get((Math.max(0, tiers.indexOf(tier)) + 1) % tiers.size());
            b.setMessage(Component.literal("Rarity: " + (tier.isBlank() ? "Any" : tier)));
        }).bounds(x + 16, y + 46, 298, 22).build());
        field = addRenderableWidget(new EditBox(font, x + 16, y + 92, 298, 22, Component.literal("Reforge")));
        field.setMaxLength(48); field.setValue(reforge == null ? "" : reforge);
        addRenderableWidget(Button.builder(Component.literal("Save"), b -> {
            save.accept(tier, MarketWatchVariantPolicy.reforgeFilter(field.getValue())); onClose();
        }).bounds(x + 16, y + 172, 140, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(x + 174, y + 172, 140, 22).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mx, int my, float delta) {
        int x = (width - 330) / 2, y = (height - 210) / 2;
        RotClientUiDraw.drawShadowedPanel(graphics, x, y, 330, 210);
        RotClientUiDraw.text(graphics, font, "ITEM VARIANT", x + 16, y + 17, RotClientTheme.TEXT, true);
        RotClientUiDraw.helpText(graphics, font, "Reforge modifier ID (blank = Any, none = unreforged)", x + 16, y + 77);
        RotClientUiDraw.helpText(graphics, font, "Examples: spicy, fierce, ancient, renowned", x + 16, y + 123);
        RotClientUiDraw.helpText(graphics, font, "Unknown item data cannot match a specific modifier.", x + 16, y + 139);
        super.extractRenderState(graphics, mx, my, delta);
    }
    @Override public void onClose() { Minecraft.getInstance().gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
