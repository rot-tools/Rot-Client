package fi.rotclient;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Separate Plus workspace. Editing targets never arms trading implicitly. */
final class MarketTradeSetupScreen extends Screen {
    private final Screen parent;
    private final Map<String, String> values = new LinkedHashMap<>();
    private final Set<String> watchIds = new HashSet<>();
    private int page, offset;
    private boolean ah;
    private boolean bz;
    private String error = "";
    private int x, y, panelWidth, panelHeight;
    private final List<String[]> labels = new ArrayList<>();
    MarketTradeSetupScreen(Screen parent) {
        super(Component.literal("Market trading setup")); this.parent = parent;
        var s = MarketTradeRuntime.journal().settings(); watchIds.addAll(s.watchIds); ah = s.auctionEnabled; bz = s.bazaarEnabled;
        put("Total buy budget", s.sessionBudgetCoins); put("Per-trade cap", s.perTradeCoins);
        put("Purse %", s.pursePercent); put("Keep in Purse", s.reserveCoins);
        put("Minimum profit", s.minProfitCoins); put("Minimum ROI %", s.minRoiPercent);
        put("Comparables", s.minComparables); put("Distinct sellers", s.minSellers);
        put("Max price spread", s.maxPeerSpreadRatio); put("Max price change %", s.maxReferenceChangePercent);
        put("Snapshot age (s)", s.maxSnapshotAgeSeconds); put("Open positions", s.maxOpenPositions);
        put("Max listing fee", s.maxListingFeeCoins); put("Listing hours", s.listingHours); put("Undercut coins", s.undercutCoins);
        put("Rarity (blank=Any)", s.rarity); put("Reforge (blank=Any)", s.reforge);
        put("Max Bazaar quantity", s.maxBazaarQuantity); put("Bazaar tax buffer %", s.bazaarTaxPercent);
    }
    private void put(String key, Object value) { values.put(key, value.toString()); }
    private Button button(String text, int left, int top, int width, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(text), b -> action.run()).bounds(left, top, width, 20).build());
    }
    @Override protected void init() {
        panelWidth = Math.min(540, width - 16); panelHeight = Math.min(320, height - 16);
        x = (width - panelWidth) / 2; y = (height - panelHeight) / 2; labels.clear();
        String[] pages = {"Budget", "Risk", "Listing", "Bazaar", "Targets", "Results"}; int tabW = (panelWidth - 24) / 6;
        for (int i = 0; i < pages.length; i++) { int selected = i;
            button(pages[i], x + 12 + i * tabW, y + 28, tabW - 3, () -> { page = selected; offset = 0; rebuildWidgets(); });
        }
        if (panelHeight < 240) {
            error = "Lower GUI scale to edit this workspace";
        } else if (page < 4) {
            String[][] groups = {{"Total buy budget", "Per-trade cap", "Purse %", "Keep in Purse", "Minimum profit", "Minimum ROI %"},
                {"Comparables", "Distinct sellers", "Max price spread", "Max price change %", "Snapshot age (s)", "Open positions"},
                {"Max listing fee", "Listing hours", "Undercut coins", "Rarity (blank=Any)", "Reforge (blank=Any)"},
                {"Max Bazaar quantity", "Bazaar tax buffer %"}};
            int fieldW = (panelWidth - 38) / 2, rowGap = Math.max(34, (panelHeight - 132) / 3);
            for (int i = 0; i < groups[page].length; i++) {
                String key = groups[page][i]; int left = x + 12 + i % 2 * (fieldW + 14), top = y + 66 + i / 2 * rowGap;
                labels.add(new String[]{key, Integer.toString(left), Integer.toString(top - 12)});
                var field = addRenderableWidget(new EditBox(font, left, top, fieldW, 20, Component.literal(key)));
                field.setMaxLength(48); field.setValue(values.get(key)); field.setResponder(v -> values.put(key, v));
            }
            if (page == 2) button("AH: " + (ah ? "On" : "Off"), x + 12 + fieldW + 14, y + 66 + 2 * rowGap, fieldW,
                    () -> { ah = !ah; rebuildWidgets(); });
            if (page == 3) button("Bazaar: " + (bz ? "On" : "Off"), x + 12, y + 66 + rowGap, fieldW,
                    () -> { bz = !bz; rebuildWidgets(); });
        } else if (page == 4) {
            var watches = choices(); int rows = Math.max(1, (panelHeight - 143) / 24);
            for (int i = offset; i < Math.min(watches.size(), offset + rows); i++) {
                var w = watches.get(i); String name = (watchIds.contains(w.id) ? "[x] " : "[ ] ") + w.label;
                button(font.plainSubstrByWidth(name, panelWidth - 36), x + 12, y + 82 + (i - offset) * 24, panelWidth - 24,
                        () -> { if (!watchIds.add(w.id)) watchIds.remove(w.id); rebuildWidgets(); });
            }
            navigation(watches.size(), rows);
        } else { int rows = Math.max(1, (panelHeight - 180) / 24); navigation(MarketTradeRuntime.journal().positions().size(), rows); }
        int bottom = y + panelHeight - 30;
        button("Save", x + 12, bottom, 70, this::save).active = panelHeight >= 240;
        button(MarketTradeRuntime.enabled() ? "Stop" : "Arm", x + 88, bottom, 70, () -> {
            if (MarketTradeRuntime.enabled()) MarketTradeRuntime.stop("Stopped by user");
            else if (save()) MarketTradeRuntime.arm(); rebuildWidgets();
        }).active = MarketTradeRuntime.enabled() || panelHeight >= 240;
        button("Back", x + panelWidth - 82, bottom, 70, this::onClose);
    }
    private record Choice(String id, String label) {}
    private List<Choice> choices() {
        var result = new ArrayList<Choice>();
        for (var w : MarketWatchRuntime.auctionWatches()) result.add(new Choice(w.id,"AH: " + w.itemName + " / " + w.tier + " / " + w.reforge));
        for (var w : MarketWatchRuntime.bazaarWatches()) result.add(new Choice(w.id,"BZ: " + MarketWatchItemCatalog.displayName(w.productId)
                + (w.buyDealPercent > 0 ? " (dynamic watch: manual only)" : "")));
        return result;
    }
    private void navigation(int size, int rows) {
        int top = y + panelHeight - 80;
        button("<", x + panelWidth - 90, top, 32, () -> { offset = Math.max(0, offset - rows); rebuildWidgets(); });
        button(">", x + panelWidth - 52, top, 32, () -> { if (offset + rows < size) offset += rows; rebuildWidgets(); });
    }
    private long whole(String key, long lo, long hi) {
        long result = new java.math.BigDecimal(values.get(key).trim().replace(",", "")).longValueExact();
        if (result < lo || result > hi) throw new IllegalArgumentException(key + ": " + lo + "–" + hi); return result;
    }
    private double number(String key, double lo, double hi) {
        double result = Double.parseDouble(values.get(key).trim());
        if (!Double.isFinite(result) || result < lo || result > hi) throw new IllegalArgumentException(key + ": " + lo + "–" + hi); return result;
    }
    private boolean save() {
        if (MarketTradeRuntime.enabled()) { error = "Stop trading before changing settings"; return false; }
        try {
            var s = MarketTradeJournal.JSON.fromJson(MarketTradeJournal.JSON.toJson(MarketTradeRuntime.journal().settings()), MarketTradeSettings.class);
            s.sessionBudgetCoins = whole("Total buy budget", 0, 100_000_000_000L);
            s.perTradeCoins = whole("Per-trade cap", 0, s.sessionBudgetCoins);
            s.pursePercent = number("Purse %", 0, 100); s.reserveCoins = whole("Keep in Purse", 0, 100_000_000_000L);
            s.minProfitCoins = whole("Minimum profit", 0, 100_000_000_000L); s.minRoiPercent = number("Minimum ROI %", 0, 1000);
            s.minComparables = (int) whole("Comparables", 3, 100); s.minSellers = (int) whole("Distinct sellers", 2, s.minComparables);
            s.maxPeerSpreadRatio = number("Max price spread", 1.01, 10); s.maxReferenceChangePercent = number("Max price change %", 1, 100);
            s.maxSnapshotAgeSeconds = (int) whole("Snapshot age (s)", 5, 120); s.maxOpenPositions = (int) whole("Open positions", 1, 20);
            s.maxListingFeeCoins = whole("Max listing fee", 0, 100_000_000L); s.listingHours = (int) whole("Listing hours", 1, 48);
            if (!List.of(1, 6, 12, 24, 48).contains(s.listingHours)) throw new IllegalArgumentException("Listing hours: 1, 6, 12, 24 or 48");
            s.undercutCoins = whole("Undercut coins", 0, 100_000_000L); s.rarity = values.get("Rarity (blank=Any)").trim().toUpperCase(Locale.ROOT);
            if (!MarketWatchVariantPolicy.TIERS.contains(s.rarity)) throw new IllegalArgumentException("Use a valid rarity or leave blank");
            s.reforge = MarketWatchVariantPolicy.reforgeFilter(values.get("Reforge (blank=Any)"));
            s.auctionEnabled = ah; s.watchIds = new HashSet<>(watchIds);
            s.bazaarEnabled = bz; s.maxBazaarQuantity = whole("Max Bazaar quantity",1,64);
            s.bazaarTaxPercent = number("Bazaar tax buffer %",0,20);
            if (!MarketTradeRuntime.journal().setSettings(s)) throw new IllegalStateException("Journal could not be saved");
            error = "Settings saved; trading remains off"; return true;
        } catch (Exception invalid) { error = invalid.getMessage() == null ? "Invalid setting" : invalid.getMessage(); return false; }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        RotClientUiDraw.drawShadowedPanel(g, x, y, panelWidth, panelHeight);
        text(g, "MARKET TRADING · PLUS", x + 12, y + 10, RotClientTheme.TEXT);
        for (var label : labels) text(g, label[0], Integer.parseInt(label[1]), Integer.parseInt(label[2]), RotClientTheme.TEXT_MUTED);
        if (page == 0) {
            text(g, "Live Purse: " + (MarketTradeRuntime.purse() < 0 ? "unknown" : MarketTradeRuntime.purse())
                    + " · Committed budget: " + MarketTradeRuntime.journal().committedBudget(), x + 12, y + panelHeight - 70, RotClientTheme.TEXT_MUTED);
        } else if (page == 3) {
            text(g,"Buy orders -> sell offers. No instant buys.",x+12,y+panelHeight-86,RotClientTheme.TEXT_MUTED);
            text(g,"Only full owned fills count. No automatic coin collection.",x+12,y+panelHeight-73,RotClientTheme.TEXT_MUTED);
        } else if (page == 4) {
            text(g, "Pinned targets: " + MarketWatchPinnedDealStore.all().size() + " · checked watches only", x + 12, y + 56, RotClientTheme.TEXT_MUTED);
            text(g, "Unknown AH variants and dynamic BZ watches stay manual", x + 12, y + 69, RotClientTheme.TEXT_MUTED);
        } else if (page == 5) {
            var j = MarketTradeRuntime.journal();
            text(g, String.format(Locale.ROOT, "Known-cost gross profit: %,.0f (before sales tax)", j.realizedGrossProfit()), x + 12, y + 58, RotClientTheme.TEXT);
            text(g, "Open positions: " + j.openPositions() + " · Unsold estimates are not profit", x + 12, y + 72, RotClientTheme.TEXT_MUTED);
            var positions = j.positions(); int rows = Math.max(1, (panelHeight - 180) / 24);
            for (int i = offset; i < Math.min(positions.size(), offset + rows); i++) {
                var p = positions.get(positions.size() - 1 - i);
                text(g, p.name + " · " + p.status, x + 12, y + 95 + (i - offset) * 24, RotClientTheme.TEXT_MUTED);
            }
        }
        text(g, MarketTradeRuntime.status(), x + 12, y + panelHeight - 57, RotClientTheme.TEXT_MUTED);
        text(g, error, x + 12, y + panelHeight - 44, RotClientTheme.TEXT_MUTED);
        super.extractRenderState(g, mx, my, delta);
    }
    private void text(GuiGraphicsExtractor g, String line, int left, int top, int color) {
        RotClientUiDraw.text(g, font, font.plainSubstrByWidth(line, panelWidth - 24), left, top, color, false);
    }
    @Override public void onClose() { Minecraft.getInstance().gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
