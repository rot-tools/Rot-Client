package fi.rotclient.rebuild.illegal;

import fi.rotclient.rebuild.RotChrome;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;

final class RewardClaimLoadingScreen extends Screen {
    private final Screen parent;
    private final BookViewScreen book;
    private final RewardClaim runtime;
    private boolean failed;
    private int linkWaitTicks = 60;
    private int left, top, panelWidth;

    RewardClaimLoadingScreen(Screen parent, BookViewScreen book, RewardClaim runtime) {
        super(Component.literal("Daily Rewards"));
        this.parent = parent;
        this.book = book;
        this.runtime = runtime;
    }

    @Override protected void init() {
        panelWidth = Math.min(290, width - 16);
        left = (width - panelWidth) / 2;
        top = (height - 84) / 2;
        addRenderableWidget(RotChrome.button(font, Component.literal("Open book"), left + 12, top + 52,
                90, 20, true, button -> runtime.showBook(book)));
        addRenderableWidget(RotChrome.button(font, Component.literal("Close"), left + panelWidth - 72, top + 52,
                60, 20, false, button -> onClose()));
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mx, int my, float delta) {
        RotChrome.panel(graphics, left, top, panelWidth, 84);
        RotChrome.text(graphics, font, "Daily Rewards", left + 12, top + 10, panelWidth - 24, RotChrome.TEXT);
        RotChrome.text(graphics, font, failed ? "Could not load the reward choices." : "Loading the reward choices…",
                left + 12, top + 28, panelWidth - 24, RotChrome.MUTED);
        super.extractRenderState(graphics, mx, my, delta);
    }

    @Override public void tick() {
        if (linkWaitTicks > 0 && --linkWaitTicks == 0) showFailure();
    }

    boolean awaitingLink() { return linkWaitTicks > 0; }
    void startRequest() { linkWaitTicks = 0; }
    void showFailure() { failed = true; }
    Screen parent() { return parent; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
}
