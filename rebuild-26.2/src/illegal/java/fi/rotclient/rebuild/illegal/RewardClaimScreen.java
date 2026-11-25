package fi.rotclient.rebuild.illegal;

import fi.rotclient.rebuild.RotChrome;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

final class RewardClaimScreen extends Screen {
    private static final int PAD = 12;
    private static final int GAP = 8;
    private static final int CARDS_PER_PAGE = 3;
    private final Screen parent;
    private final RewardClaimPolicy.ParsedPage page;
    private final URI link;
    private final RewardClaim runtime;
    private final HttpClient session;
    private final ClientPacketListener connection;
    private final List<ItemStack> icons;
    private int selected = -1;
    private int firstCard;
    private boolean submitting;
    private String result;
    private Button claimButton;
    private int x, y, panelWidth, panelHeight, cardWidth, cardHeight;

    RewardClaimScreen(Screen parent, RewardClaimPolicy.ParsedPage page, URI link, RewardClaim runtime,
                      HttpClient session, ClientPacketListener connection) {
        super(Component.literal("Daily Rewards"));
        this.parent = parent;
        this.page = page;
        this.link = link;
        this.runtime = runtime;
        this.session = session;
        this.connection = connection;
        icons = page.rewards().stream().map(RewardClaimScreen::iconFor).toList();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(520, width - 16);
        cardHeight = Math.clamp(height - 108, 48, 138);
        panelHeight = cardHeight + 88;
        x = (width - panelWidth) / 2;
        y = (height - panelHeight) / 2;
        int cols = Math.min(CARDS_PER_PAGE, page.rewards().size());
        cardWidth = (panelWidth - 2 * PAD - GAP * (cols - 1)) / cols;
        int bottom = y + panelHeight - 29;
        int right = x + panelWidth - PAD;
        button("Close", x + PAD, bottom, 53, false, this::onClose);
        if (page.rewards().size() > CARDS_PER_PAGE) {
            button("‹", x + PAD + 58, bottom, 20, false, () -> changePage(-1)).active = firstCard > 0;
            button("›", x + PAD + 82, bottom, 20, false, () -> changePage(1)).active =
                    firstCard + CARDS_PER_PAGE < page.rewards().size();
        }
        if (page.skippable()) {
            claimButton = button("Claim", right - 60, bottom, 60, true, this::submit);
            claimButton.active = canClaim();
            right -= 66;
        }
        button("Open website", right - 90, bottom, 90, !page.skippable(),
                () -> ConfirmLinkScreen.confirmLinkNow(this, link));
    }

    private Button button(String label, int x, int y, int width, boolean primary, Runnable action) {
        return addRenderableWidget(RotChrome.button(font, Component.literal(label), x, y, width, 20, primary,
                ignored -> action.run()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mx, int my, float delta) {
        RotChrome.panel(graphics, x, y, panelWidth, panelHeight);
        graphics.fill(x + PAD, y + 1, x + PAD + 66, y + 3, RotChrome.accent());
        RotChrome.text(graphics, font, "Daily Rewards", x + PAD, y + 9, panelWidth - 2 * PAD, RotChrome.TEXT);
        RotChrome.text(graphics, font, RewardClaimPolicy.streakSummary(page), x + PAD, y + 21,
                panelWidth - 2 * PAD, RotChrome.MUTED);
        int visibleCards = Math.min(CARDS_PER_PAGE, page.rewards().size() - firstCard);
        for (int slot = 0; slot < visibleCards; slot++) {
            int index = firstCard + slot;
            var reward = page.rewards().get(index);
            int left = x + PAD + slot * (cardWidth + GAP);
            int top = y + 37;
            boolean hover = mx >= left && mx < left + cardWidth && my >= top && my < top + cardHeight;
            drawCard(graphics, left, top, index, hover);
            if (hover) {
                var title = Component.literal(reward.title());
                var tooltip = reward.subtitle().isBlank() ? List.<Component>of(title)
                        : List.<Component>of(title, Component.literal(reward.subtitle()));
                graphics.setComponentTooltipForNextFrame(font, tooltip, mx, my);
            }
        }
        String status = result != null ? result
                : submitting ? "Sending claim…"
                : !page.skippable() ? "Complete this reward on the official website."
                : selected < 0 ? "Choose one reward." : page.rewards().get(selected).title();
        RotChrome.text(graphics, font, status, x + PAD, y + 43 + cardHeight, panelWidth - 2 * PAD, RotChrome.MUTED);
        super.extractRenderState(graphics, mx, my, delta);
    }

    private void drawCard(GuiGraphicsExtractor graphics, int left, int top, int index, boolean hover) {
        var reward = page.rewards().get(index);
        int rarity = RewardClaimPolicy.rarityColor(reward.rarity());
        RotChrome.row(graphics, left, top, cardWidth, cardHeight, hover || index == selected);
        graphics.fill(left + 5, top + 2, left + cardWidth - 5, top + 4, rarity);
        if (index == selected) {
            graphics.fill(left + 5, top + cardHeight - 3, left + cardWidth - 5, top + cardHeight - 1, RotChrome.accent());
        }
        int scale = cardHeight >= 88 ? 2 : 1;
        int iconSize = 16 * scale;
        int iconX = left + (cardWidth - iconSize) / 2;
        int iconY = top + (cardHeight - iconSize - 26) / 2;
        var pose = graphics.pose();
        pose.pushMatrix();
        try {
            pose.translate(iconX, iconY);
            pose.scale(scale, scale);
            graphics.item(icons.get(index), 0, 0);
        } finally {
            pose.popMatrix();
        }
        RotChrome.text(graphics, font, reward.title(), left + 6, iconY + iconSize + 3, cardWidth - 12, RotChrome.TEXT);
        String rarityLabel = reward.rarity().isBlank() ? "REWARD" : reward.rarity().toUpperCase(Locale.ROOT);
        RotChrome.text(graphics, font, rarityLabel, left + 6, iconY + iconSize + 14, cardWidth - 12, rarity);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && event.y() >= y + 37
                && event.y() < y + 37 + cardHeight && event.x() >= x + PAD) {
            int offset = (int) (event.x() - x - PAD);
            int slot = offset / (cardWidth + GAP);
            int index = firstCard + slot;
            if (slot < CARDS_PER_PAGE && index < page.rewards().size() && offset % (cardWidth + GAP) < cardWidth) {
                select(index);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT) {
            int direction = key == GLFW.GLFW_KEY_LEFT ? -1 : 1;
            select(selected < 0 ? firstCard : Math.floorMod(selected + direction, page.rewards().size()));
            return true;
        }
        if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_3) {
            int index = firstCard + key - GLFW.GLFW_KEY_1;
            if (index < page.rewards().size()) select(index);
            return true;
        }
        return super.keyPressed(event);
    }

    private void select(int index) {
        if (submitting || result != null) return;
        selected = index;
        int offset = index / CARDS_PER_PAGE * CARDS_PER_PAGE;
        if (offset != firstCard) {
            firstCard = offset;
            rebuildWidgets();
        } else if (claimButton != null) {
            claimButton.active = canClaim();
        }
    }

    private void changePage(int direction) {
        int next = firstCard + direction * CARDS_PER_PAGE;
        if (next < 0 || next >= page.rewards().size()) return;
        firstCard = next;
        rebuildWidgets();
    }

    private boolean canClaim() {
        return page.skippable() && selected >= 0 && !submitting && result == null;
    }

    private void submit() {
        if (!canClaim()) return;
        submitting = true;
        claimButton.active = false;
        runtime.claim(this, page, page.rewards().get(selected), session, connection);
    }

    void finishClaim(String status) {
        submitting = false;
        result = status;
        if (claimButton != null) claimButton.active = false;
    }

    Screen parent() { return parent; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }

    private static ItemStack iconFor(RewardClaimPolicy.RewardOption reward) {
        return new ItemStack(switch (reward.type().toLowerCase(Locale.ROOT)) {
            case "coins" -> Items.GOLD_NUGGET;
            case "tokens" -> Items.SUNFLOWER;
            case "dust" -> Items.GLOWSTONE_DUST;
            case "experience" -> Items.EXPERIENCE_BOTTLE;
            case "adsense_token" -> Items.NAME_TAG;
            case "souls" -> Items.SOUL_LANTERN;
            case "mystery_box" -> Items.ENDER_CHEST;
            case "gift_box" -> Items.CHEST;
            case "housing_package" -> reward.pack().endsWith("chest") ? Items.CHEST : Items.PLAYER_HEAD;
            case "add_vanity" -> Items.ARMOR_STAND;
            default -> Items.BUNDLE;
        });
    }
}
