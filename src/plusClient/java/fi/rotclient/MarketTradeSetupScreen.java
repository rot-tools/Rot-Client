package fi.rotclient;

import com.mojang.blaze3d.platform.InputConstants;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Plus buying workspace. Draft changes and selecting an item never start purchases. */
final class MarketTradeSetupScreen extends Screen {
    private static final String SESSION = "Session budget";
    private static final String PER_BUY = "Maximum per purchase";
    private static final String PERCENT = "Spendable Purse %";
    private static final String RESERVE = "Keep in Purse";
    private static final String PROFIT = "Minimum estimated profit";
    private static final String ROI = "Minimum estimated ROI %";
    private static final String PEERS = "Comparable listings";
    private static final String SELLERS = "Distinct sellers";
    private static final String SPREAD = "Maximum price spread";
    private static final String CHANGE = "Maximum price change %";
    private static final String AGE = "Maximum data age (s)";
    private static final String HOLDINGS = "Holding slots";
    private static final String REFORGE = "Reforge";
    private static final String QUANTITY = "Maximum Bazaar quantity";
    private static final String TAX = "Bazaar sale tax buffer %";
    private static final BooleanSupplier YES = () -> true;

    private final Screen parent;
    private final Map<String, String> values = new LinkedHashMap<>();
    private final Set<String> watchIds = new HashSet<>();
    private final Set<String> pinIds = new HashSet<>();
    private final List<Action> actions = new ArrayList<>();
    private final List<Field> fields = new ArrayList<>();
    private final List<Label> labels = new ArrayList<>();
    private int page, offset, x, y, panelWidth, panelHeight;
    private int left, contentWidth, contentTop, contentBottom, footerTop;
    private int listTop, rowHeight, rows, formCount, formPageSize;
    private boolean filters, purchaseDetails, ah, bz, showPurchaseHud, noticeError;
    private String query = "", rarity = "", selectedPositionId = "", notice = "";

    private record Action(int x, int y, int width, int height, Supplier<String> label,
                          BooleanSupplier selected, BooleanSupplier enabled, String help, Runnable run) {}
    private record Field(EditBox widget, String key, int x, int y, int width, String help) {}
    private record Label(String text, int x, int y, int width, int color) {}
    private record Spec(String key, String help, int kind) {}
    private record Choice(String id, boolean pinned, String name, String detail, String search, String unavailable) {}

    MarketTradeSetupScreen(Screen parent) {
        super(Component.literal("Market buying setup"));
        this.parent = parent;
        var s = MarketTradeRuntime.journal().settings();
        watchIds.addAll(s.watchIds);
        pinIds.addAll(s.pinIds);
        ah = s.auctionEnabled;
        bz = s.bazaarEnabled;
        showPurchaseHud = s.showPurchaseHud;
        rarity = s.rarity;
        put(SESSION, s.sessionBudgetCoins); put(PER_BUY, s.perTradeCoins);
        put(PERCENT, s.pursePercent); put(RESERVE, s.reserveCoins);
        put(PROFIT, s.minProfitCoins); put(ROI, s.minRoiPercent);
        put(PEERS, s.minComparables); put(SELLERS, s.minSellers);
        put(SPREAD, s.maxPeerSpreadRatio); put(CHANGE, s.maxReferenceChangePercent);
        put(AGE, s.maxSnapshotAgeSeconds); put(HOLDINGS, s.maxOpenPositions);
        put(REFORGE, s.reforge); put(QUANTITY, s.maxBazaarQuantity); put(TAX, s.bazaarTaxPercent);
    }

    private void put(String key, Object value) { values.put(key, value.toString()); }
    private boolean editable() { return !MarketTradeRuntime.enabled(); }
    private boolean compact() { return panelHeight < 250; }

    @Override
    protected void init() {
        panelWidth = Math.min(680, Math.max(1, width - 16));
        panelHeight = Math.min(460, Math.max(1, height - 16));
        x = (width - panelWidth) / 2;
        y = (height - panelHeight) / 2;
        left = x + 12;
        contentWidth = Math.max(1, panelWidth - 24);
        int tabsTop = y + (compact() ? 22 : 44);
        contentTop = tabsTop + (compact() ? 24 : 28);
        footerTop = y + panelHeight - (compact() ? 46 : 58);
        contentBottom = footerTop - 6;
        actions.clear(); fields.clear(); labels.clear();
        formCount = formPageSize = 0;
        String[] tabs = {"Targets", "Budget", "Safety", "Purchases"};
        int gap = 4, tabWidth = (contentWidth - gap * 3) / 4;
        for (int i = 0; i < tabs.length; i++) {
            int selected = i;
            action(left + i * (tabWidth + gap), tabsTop, tabWidth, compact() ? 20 : 22,
                    () -> tabs[selected], () -> page == selected, YES, "", () -> changePage(selected));
        }
        if (contentBottom - contentTop < 66) {
            labels.add(new Label("Increase the window size or lower GUI scale.", left, contentTop,
                    contentWidth, RotClientTheme.WARNING));
        } else if (page == 0 && !filters) initTargets();
        else if (page == 3) {
            if (purchaseDetails && selectedPosition() != null) initPurchaseDetails();
            else { purchaseDetails = false; initPurchases(); }
        } else initForm();
        initFooter();
    }

    private void changePage(int next) {
        page = next; filters = false; purchaseDetails = false; offset = 0;
        rebuildWidgets();
    }

    private void action(int ax, int ay, int aw, int ah, Supplier<String> label,
                        BooleanSupplier selected, BooleanSupplier enabled, String help, Runnable run) {
        actions.add(new Action(ax, ay, Math.max(1, aw), ah, label, selected, enabled, help, run));
    }

    private void initFooter() {
        int backWidth = Math.min(78, Math.max(52, contentWidth / 4));
        int saveWidth = Math.min(100, Math.max(58, contentWidth / 4));
        int startWidth = Math.min(156, Math.max(78, contentWidth - saveWidth - backWidth - 16));
        int bottom = y + panelHeight - 28;
        action(left, bottom, saveWidth, 22, () -> "Save", () -> false, this::editable,
                "Save all setup pages. Buying stays off.", this::save);
        action(left + saveWidth + 8, bottom, startWidth, 22,
                () -> MarketTradeRuntime.enabled() ? "Stop buying" : "Start buying",
                () -> MarketTradeRuntime.enabled(), YES,
                "Start saves setup and scans continuously. Stop ends scanning; sent purchases need review.", this::toggleBuying);
        action(x + panelWidth - 12 - backWidth, bottom, backWidth, 22,
                () -> "Back", () -> false, YES, "Return to Market Watch.", this::onClose);
    }

    private void initTargets() {
        int filterWidth = Math.min(86, Math.max(62, contentWidth / 4));
        var search = edit("Search", query, left, contentTop, contentWidth - filterWidth - 8,
                "Search saved watches and pins by item name, ID, rarity or reforge.", true,
                value -> { query = value; offset = 0; });
        search.setHint(Component.literal("Search items / IDs"));
        action(left + contentWidth - filterWidth, contentTop, filterWidth, 22,
                () -> "Filters", () -> false, YES, "Markets, rarity, reforge and pricing requirements.",
                () -> { filters = true; offset = 0; rebuildWidgets(); });
        listTop = contentTop + 26;
        if (contentBottom - listTop >= 100) {
            labels.add(new Label("Choose each watch or pin that may buy. Selection is saved with your setup.",
                    left, listTop, contentWidth, RotClientTheme.TEXT_MUTED));
            listTop += 17;
        }
        rowHeight = contentBottom - listTop >= 95 ? 34 : 24;
        rows = Math.max(1, (contentBottom - listTop - 20) / rowHeight);
        navigation(() -> choices().size(), rows);
    }

    private void initForm() {
        int formTop = contentTop;
        if (page == 0 && contentBottom - contentTop >= 110) {
            action(left, formTop, Math.min(150, contentWidth), 22, () -> "Back to targets", () -> false, YES,
                    "Your target selection is kept while editing filters.",
                    () -> { filters = false; offset = 0; rebuildWidgets(); });
            formTop += 28;
        } else if (page == 1 && contentBottom - contentTop >= 150) formTop += 56;
        List<Spec> specs = specs();
        formCount = specs.size();
        int columns = contentWidth >= 460 ? 2 : 1;
        int available = contentBottom - formTop - 20;
        int cellHeight = available >= 52 ? 52 : 38;
        int visibleRows = Math.max(1, available / cellHeight);
        formPageSize = columns * visibleRows;
        offset = Math.min(offset, Math.max(0, (specs.size() - 1) / formPageSize * formPageSize));
        int fieldWidth = (contentWidth - (columns - 1) * 14) / columns;
        for (int i = offset; i < Math.min(specs.size(), offset + formPageSize); i++) {
            var spec = specs.get(i);
            int n = i - offset;
            int fx = left + n % columns * (fieldWidth + 14);
            int fy = formTop + n / columns * cellHeight;
            labels.add(new Label(spec.key(), fx, fy, fieldWidth, RotClientTheme.TEXT_DIM));
            if (spec.kind() == 0) edit(spec.key(), values.get(spec.key()), fx, fy + 12, fieldWidth,
                    spec.help(), editable(), value -> values.put(spec.key(), value));
            else action(fx, fy + 12, fieldWidth, 22, () -> switch (spec.kind()) {
                case 1 -> ah ? "Enabled" : "Off";
                case 2 -> bz ? "Enabled" : "Off";
                case 3 -> rarity.isBlank() ? "Any rarity" : rarity;
                default -> showPurchaseHud ? "Visible" : "Hidden";
            }, () -> spec.kind() == 1 ? ah : spec.kind() == 2 ? bz : spec.kind() == 3 ? !rarity.isBlank() : showPurchaseHud,
                    this::editable, spec.help(), () -> {
                        if (spec.kind() == 1) ah = !ah;
                        else if (spec.kind() == 2) bz = !bz;
                        else if (spec.kind() == 4) showPurchaseHud = !showPurchaseHud;
                        else {
                            var tiers = MarketWatchVariantPolicy.TIERS;
                            rarity = tiers.get((Math.max(0, tiers.indexOf(rarity)) + 1) % tiers.size());
                        }
                    });
            if (cellHeight >= 52) labels.add(new Label(spec.help(), fx, fy + 39, fieldWidth, RotClientTheme.TEXT_MUTED));
        }
        navigation(() -> formCount, formPageSize);
    }

    private List<Spec> specs() {
        if (page == 0) return List.of(
                new Spec("Auction House BIN", "Only selected BIN targets may buy.", 1),
                new Spec("Bazaar buy orders", "Selected fixed watches / pins; full fills only.", 2),
                new Spec("Auction rarity", "Extra filter; each watch's own filters also apply.", 3),
                new Spec(REFORGE, "Blank = Any; none = unreforged; e.g. ancient.", 0),
                new Spec(PROFIT, "Coins after conservative forecast sale fees.", 0),
                new Spec(ROI, "Estimated return for a later manual sale.", 0),
                new Spec(QUANTITY, "Maximum units per Bazaar buy order (1–64).", 0),
                new Spec(TAX, "Forecast only; a 5% minimum buffer applies.", 0));
        if (page == 1) return List.of(
                new Spec(SESSION, "Total purchase spending; prior buys still count.", 0),
                new Spec(PER_BUY, "Hard coin cap on one purchase / buy order.", 0),
                new Spec(PERCENT, "Percentage of live Purse after your reserve.", 0),
                new Spec(RESERVE, "These coins stay untouched in your Purse.", 0));
        return List.of(
                new Spec(PEERS, "Required matching Auction House samples (3–100).", 0),
                new Spec(SELLERS, "Required independent sellers (2 or more).", 0),
                new Spec(SPREAD, "Highest / lowest comparable price, e.g. 1.5.", 0),
                new Spec(CHANGE, "Reject a larger change between fresh snapshots.", 0),
                new Spec(AGE, "Maximum snapshot age in seconds (5–120).", 0),
                new Spec(HOLDINGS, "Pending / held buys until you release their slots.", 0),
                new Spec("Purchase HUD", "Show continuous scan and purchase progress.", 4));
    }

    private EditBox edit(String key, String value, int fx, int fy, int fw, String help,
                         boolean enabled, java.util.function.Consumer<String> responder) {
        var field = addRenderableWidget(new EditBox(font, fx + 6, fy + 6, Math.max(1, fw - 12), 14, Component.literal(key)));
        field.setBordered(false); field.setMaxLength(80);
        field.setTextColor(RotClientTheme.TEXT); field.setTextColorUneditable(RotClientTheme.TEXT_MUTED);
        field.setEditable(enabled); field.setValue(value == null ? "" : value); field.setResponder(responder);
        fields.add(new Field(field, key, fx, fy, fw, help));
        return field;
    }

    private void navigation(Supplier<Integer> size, int pageSize) {
        int top = contentBottom - 18;
        action(left + contentWidth - 62, top, 28, 18, () -> "<", () -> false, () -> offset > 0,
                "Previous page.", () -> { offset = Math.max(0, offset - pageSize); rebuildWidgets(); });
        action(left + contentWidth - 28, top, 28, 18, () -> ">", () -> false,
                () -> offset + pageSize < size.get(), "Next page.", () -> { offset += pageSize; rebuildWidgets(); });
    }

    private List<Choice> choices() {
        var result = new ArrayList<Choice>();
        for (var watch : MarketWatchRuntime.auctionWatches()) {
            String name = watch.itemName.isBlank() ? watch.itemId : watch.itemName;
            String detail = "AH watch · " + any(watch.tier) + " · " + any(watch.reforge)
                    + (watch.maxPriceCoins > 0 ? " · max " + coins(watch.maxPriceCoins) : " · budget cap");
            result.add(new Choice(watch.id, false, name, detail, watch.itemId + " " + name + " " + detail,
                    !watch.enabled ? "Watch is disabled in Market Watch" : !ah ? "Auction House buying is off" : ""));
        }
        for (var watch : MarketWatchRuntime.bazaarWatches()) {
            String name = MarketWatchItemCatalog.displayName(watch.productId);
            String detail = "Bazaar watch · " + (watch.maxInstantBuyPrice > 0 ? "max unit " + coins(watch.maxInstantBuyPrice) : "budget cap");
            String unavailable = watch.buyDealPercent > 0 ? "Dynamic Bazaar watch: manual only"
                    : !watch.enabled ? "Watch is disabled in Market Watch" : !bz ? "Bazaar buying is off" : "";
            result.add(new Choice(watch.id, false, name, detail, watch.productId + " " + name + " " + detail, unavailable));
        }
        for (var pin : MarketWatchPinnedDealStore.all()) {
            boolean auction = pin.market() == MarketWatchOpportunity.Market.AUCTION_HOUSE;
            String detail = (auction ? "AH pin · " + any(pin.tier()) + " · " + (pin.reforge().isBlank() ? "unreforged" : pin.reforge()) : "Bazaar pin")
                    + " · fresh prices rechecked";
            String unavailable = auction && (pin.itemId().isBlank() || pin.tier().isBlank() || pin.reforge().equals("?"))
                    ? "Pinned auction variant is unknown: manual only" : auction && !ah ? "Auction House buying is off"
                    : !auction && !bz ? "Bazaar buying is off" : "";
            result.add(new Choice(pin.id(), true, pin.displayName(), detail, pin.itemId() + " " + pin.displayName() + " " + detail, unavailable));
        }
        String needle = query.strip().toLowerCase(Locale.ROOT);
        return result.stream().filter(choice -> needle.isBlank() || choice.search().toLowerCase(Locale.ROOT).contains(needle)).toList();
    }

    private boolean selected(Choice choice) { return (choice.pinned() ? pinIds : watchIds).contains(choice.id()); }
    private int selectedCount() {
        return (int) (MarketWatchRuntime.auctionWatches().stream().filter(watch -> watchIds.contains(watch.id)).count()
                + MarketWatchRuntime.bazaarWatches().stream().filter(watch -> watchIds.contains(watch.id)).count()
                + MarketWatchPinnedDealStore.all().stream().filter(pin -> pinIds.contains(pin.id())).count());
    }

    private void initPurchases() {
        listTop = contentTop;
        if (contentBottom - contentTop >= 105) {
            labels.add(new Label("Buys are kept for you to sell manually. Choose a purchase to review its evidence.", left, listTop, contentWidth, RotClientTheme.TEXT_MUTED));
            labels.add(new Label("Committed: " + coins(MarketTradeRuntime.journal().committedBudget()) + " coins · Holding slots: "
                    + MarketTradeRuntime.journal().openPositions(), left, listTop + 14, contentWidth, RotClientTheme.TEXT_DIM));
            listTop += 34;
        }
        rowHeight = contentBottom - listTop >= 95 ? 34 : 24;
        rows = Math.max(1, (contentBottom - listTop - 20) / rowHeight);
        navigation(() -> MarketTradeRuntime.journal().positions().size(), rows);
    }

    private MarketTradeJournal.Position selectedPosition() {
        return MarketTradeRuntime.journal().positions().stream().filter(position -> selectedPositionId.equals(position.id)).findFirst().orElse(null);
    }

    private void initPurchaseDetails() {
        if (contentBottom - contentTop >= 100) action(left, contentTop, Math.min(146, contentWidth), 20,
                () -> "Back to purchases", () -> false, YES, "Return to the purchase journal.",
                () -> { purchaseDetails = false; rebuildWidgets(); });
        var position = selectedPosition();
        if (position == null) return;
        action(left, contentBottom - 22, Math.min(220, contentWidth), 22,
                () -> position.manuallyReviewed ? "Holding slot released" : "Mark reviewed / release slot",
                () -> position.manuallyReviewed, () -> editable() && position.buySent && !position.manuallyReviewed,
                "After checking the outcome or selling manually, release this holding slot. Spending stays committed. "
                        + "Review does not verify an unknown purchase or record a profit.",
                () -> {
                    boolean saved = MarketTradeRuntime.markReviewed(position.id);
                    notice = saved ? "Review saved; spending stays committed." : "Stop buying and check the journal before reviewing.";
                    noticeError = !saved;
                });
    }

    private long whole(String key, long low, long high) {
        try {
            long number = new BigDecimal(values.get(key).trim().replace(",", "")).longValueExact();
            if (number < low || number > high) throw new IllegalArgumentException();
            return number;
        } catch (RuntimeException invalid) { throw new IllegalArgumentException(key + ": use " + coins(low) + "–" + coins(high)); }
    }

    private double number(String key, double low, double high) {
        try {
            double number = Double.parseDouble(values.get(key).trim());
            if (!Double.isFinite(number) || number < low || number > high) throw new IllegalArgumentException();
            return number;
        } catch (RuntimeException invalid) { throw new IllegalArgumentException(key + ": use " + low + "–" + high); }
    }

    private MarketTradeSettings budgetDraft() {
        var s = new MarketTradeSettings();
        s.sessionBudgetCoins = whole(SESSION, 0, 100_000_000_000L);
        s.perTradeCoins = whole(PER_BUY, 0, s.sessionBudgetCoins);
        s.pursePercent = number(PERCENT, 0, 100);
        s.reserveCoins = whole(RESERVE, 0, 100_000_000_000L);
        s.normalize();
        return s;
    }

    private MarketTradeSettings draft() {
        var s = MarketTradeJournal.JSON.fromJson(MarketTradeJournal.JSON.toJson(MarketTradeRuntime.journal().settings()), MarketTradeSettings.class);
        var budget = budgetDraft();
        s.sessionBudgetCoins = budget.sessionBudgetCoins; s.perTradeCoins = budget.perTradeCoins;
        s.pursePercent = budget.pursePercent; s.reserveCoins = budget.reserveCoins;
        s.minProfitCoins = whole(PROFIT, 0, 100_000_000_000L); s.minRoiPercent = number(ROI, 0, 1000);
        s.minComparables = (int) whole(PEERS, 3, 100); s.minSellers = (int) whole(SELLERS, 2, s.minComparables);
        s.maxPeerSpreadRatio = number(SPREAD, 1.01, 10); s.maxReferenceChangePercent = number(CHANGE, 1, 100);
        s.maxSnapshotAgeSeconds = (int) whole(AGE, 5, 120); s.maxOpenPositions = (int) whole(HOLDINGS, 1, 20);
        s.rarity = rarity; s.reforge = MarketWatchVariantPolicy.reforgeFilter(values.get(REFORGE));
        s.maxBazaarQuantity = whole(QUANTITY, 1, 64); s.bazaarTaxPercent = number(TAX, 0, 20);
        s.auctionEnabled = ah; s.bazaarEnabled = bz;
        s.showPurchaseHud = showPurchaseHud;
        s.watchIds = new HashSet<>(watchIds); s.pinIds = new HashSet<>(pinIds);
        return s;
    }

    private boolean save() {
        if (!editable()) { notice = "Stop buying before saving setup changes."; noticeError = true; return false; }
        try {
            if (!MarketTradeRuntime.journal().setSettings(draft())) throw new IllegalStateException("Purchase journal could not be saved.");
            notice = "Setup saved. Buying is off until you press Start buying."; noticeError = false;
            return true;
        } catch (Exception invalid) {
            notice = invalid.getMessage() == null ? "Check the setup values." : invalid.getMessage(); noticeError = true;
            return false;
        }
    }

    private void toggleBuying() {
        if (MarketTradeRuntime.enabled()) {
            MarketTradeRuntime.stop("Buying stopped by user");
            notice = "Buying stopped. Review any sent purchase in Purchases."; noticeError = false;
        } else if (save()) {
            boolean started = MarketTradeRuntime.arm();
            notice = started ? "Scanning selected targets continuously. Sell purchases manually." : MarketTradeRuntime.status();
            noticeError = !started;
        }
        rebuildWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        RotClientUiDraw.beginHoverFrame(mouseX, mouseY);
        RotClientUiDraw.drawShadowedPanel(g, x, y, panelWidth, panelHeight);
        RotClientUiDraw.drawHeaderBar(g, font, x, y, panelWidth, compact() ? 20 : 38,
                fit("MARKET BUYING · ROT CLIENT+", contentWidth), compact() ? "" : fit("Choose targets, set spending limits, then start continuous buying.", contentWidth));
        for (var label : labels) text(g, label.text(), label.x(), label.y(), label.width(), label.color());
        if (contentBottom - contentTop >= 66) {
            if (page == 0 && !filters) drawTargets(g, mouseX, mouseY);
            else if (page == 1) drawBudget(g, mouseX, mouseY);
            else if (page == 3) {
                if (purchaseDetails) drawPurchaseDetails(g); else drawPurchases(g, mouseX, mouseY);
            }
        }
        if (formCount > 0) drawPageCount(g, formCount, formPageSize, "fields");
        for (var field : fields) {
            field.widget().setEditable(field.key().equals("Search") || editable());
            RotClientTheme.drawInset(g, field.x(), field.y(), field.width(), 24, field.widget().isFocused());
            if (RotClientUiDraw.inside(mouseX, mouseY, field.x(), field.y(), field.width(), 24)) RotClientUiDraw.noteHoverTip(field.key() + ": " + field.help());
        }
        RotClientTheme.drawDivider(g, left, footerTop - 1, contentWidth);
        String status = compact() && noticeError ? notice : compact() && page == 1 ? compactBudget() : MarketTradeRuntime.status();
        text(g, status, left, footerTop + 3, contentWidth, compact() && noticeError ? RotClientTheme.ERROR : RotClientTheme.TEXT_DIM);
        if (!compact()) {
            String message = !notice.isBlank() ? notice : page == 1 ? compactBudget() : MarketTradeRuntime.enabled()
                    ? "Buying is active. Stop to edit this setup." : "Save keeps buying off. Start saves and begins scanning.";
            text(g, message, left, footerTop + 15, contentWidth, noticeError ? RotClientTheme.ERROR : RotClientTheme.TEXT_MUTED);
        }
        for (var action : actions) {
            RotClientUiDraw.drawPremiumButton(g, font, mouseX, mouseY, action.x(), action.y(), action.width(), action.height(),
                    fit(action.label().get(), action.width() - 12), action.selected().getAsBoolean(), action.enabled().getAsBoolean());
            if (RotClientUiDraw.inside(mouseX, mouseY, action.x(), action.y(), action.width(), action.height())) RotClientUiDraw.noteHoverTip(action.help());
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
        RotClientUiDraw.drawHoverFrame(g, font, width, height);
    }

    private void drawTargets(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        List<Choice> choices = choices(); clampListOffset(choices.size());
        for (int i = offset; i < Math.min(choices.size(), offset + rows); i++) {
            var choice = choices.get(i); int top = listTop + (i - offset) * rowHeight;
            boolean hover = RotClientUiDraw.inside(mouseX, mouseY, left, top, contentWidth, rowHeight - 3);
            RotClientUiDraw.drawInteractiveSurface(g, left, top, contentWidth, rowHeight - 3, hover ? 1 : 0, selected(choice), RotClientTheme.HUD_ACCENT, RotClientUiDraw.RADIUS_SM);
            text(g, (selected(choice) ? "[x] " : "[ ] ") + choice.name(), left + 8, top + 5, contentWidth - 16, RotClientTheme.TEXT);
            if (rowHeight >= 32) text(g, choice.unavailable().isBlank() ? choice.detail() : choice.unavailable(), left + 8, top + 18,
                    contentWidth - 16, choice.unavailable().isBlank() ? RotClientTheme.TEXT_MUTED : RotClientTheme.WARNING);
            if (hover) RotClientUiDraw.noteHoverTip(choice.name() + "\n" + choice.detail() + (choice.unavailable().isBlank() ? "" : "\n" + choice.unavailable())
                    + "\nSaved watch price, rarity and reforge requirements also apply.");
        }
        if (choices.isEmpty()) text(g, query.isBlank() ? "No targets. Create a Market Watch or pin a deal first." : "No matching targets. Try an item name or ID.", left, listTop + 5, contentWidth, RotClientTheme.TEXT_MUTED);
        drawPageCount(g, choices.size(), rows, selectedCount() + " selected");
    }

    private void drawBudget(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (contentBottom - contentTop < 150) return;
        RotClientUiDraw.drawElevatedCard(g, left, contentTop, contentWidth, 47);
        int half = contentWidth / 2;
        text(g, "LIVE PURSE", left + 9, contentTop + 7, half - 16, RotClientTheme.TEXT_MUTED);
        text(g, MarketTradeRuntime.purse() < 0 ? "Unknown — buying blocked" : coins(MarketTradeRuntime.purse()) + " coins", left + 9, contentTop + 21, half - 16, RotClientTheme.TEXT);
        text(g, "MAXIMUM BUY NOW", left + half + 5, contentTop + 7, half - 14, RotClientTheme.TEXT_MUTED);
        text(g, allowanceText(), left + half + 5, contentTop + 21, half - 14, RotClientTheme.HUD_ACCENT);
        if (RotClientUiDraw.inside(mouseX, mouseY, left, contentTop, contentWidth, 47))
            RotClientUiDraw.noteHoverTip("Maximum buy is the smallest of: remaining session budget, per-purchase cap, "
                    + "spendable Purse percentage and live Purse above your reserve. "
                    + "Prior purchase spending stays committed. An unknown Purse blocks buying.");
    }

    private String compactBudget() {
        return "Purse: " + (MarketTradeRuntime.purse() < 0 ? "unknown" : coins(MarketTradeRuntime.purse())) + " · Max buy: " + allowanceText()
                + " · committed " + coins(MarketTradeRuntime.journal().committedBudget());
    }

    private String allowanceText() {
        try { return coins(budgetDraft().allowance(MarketTradeRuntime.purse(), MarketTradeRuntime.journal().committedBudget())) + " coins"; }
        catch (RuntimeException invalid) { return "Check budget values"; }
    }

    private void drawPurchases(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        var positions = MarketTradeRuntime.journal().positions(); clampListOffset(positions.size());
        for (int i = offset; i < Math.min(positions.size(), offset + rows); i++) {
            var position = positions.get(positions.size() - 1 - i); int top = listTop + (i - offset) * rowHeight;
            boolean hover = RotClientUiDraw.inside(mouseX, mouseY, left, top, contentWidth, rowHeight - 3);
            RotClientUiDraw.drawInteractiveSurface(g, left, top, contentWidth, rowHeight - 3, hover ? 1 : 0, selectedPositionId.equals(position.id), RotClientTheme.HUD_ACCENT, RotClientUiDraw.RADIUS_SM);
            text(g, position.name + " · " + coins(position.cost()) + " coins", left + 8, top + 5, contentWidth - 16, RotClientTheme.TEXT);
            if (rowHeight >= 32) text(g, position.status, left + 8, top + 18, contentWidth - 16, position.manualReview ? RotClientTheme.WARNING : RotClientTheme.TEXT_MUTED);
            if (hover) RotClientUiDraw.noteHoverTip(position.name + "\n" + position.status + "\nSelect to see purchase evidence and review the holding slot.");
        }
        if (positions.isEmpty()) text(g, "No purchases yet. Buys appear here after a persisted intent.", left, listTop + 5, contentWidth, RotClientTheme.TEXT_MUTED);
        drawPageCount(g, positions.size(), rows, "purchases");
    }

    private void drawPurchaseDetails(GuiGraphicsExtractor g) {
        var p = selectedPosition(); if (p == null) return;
        int top = contentTop + (contentBottom - contentTop >= 100 ? 26 : 0), bottom = contentBottom - 28;
        List<String> lines = List.of(p.name + " · " + p.quantity + " × " + coins(p.buyUnit) + " coins", p.status,
                "Committed purchase budget: " + coins(p.reserved) + " coins",
                "Receipt: " + (p.receiptObserved ? "observed" : "unconfirmed") + " · Owned item: " + (p.bought ? "verified" : "unconfirmed"),
                "Bazaar fill: " + (p.fillObserved ? "observed" : "not observed"),
                p.manuallyReviewed ? "Holding slot released; spending stays committed." : "Holding slot remains counted until you review it.",
                "Purchased items stay yours to sell manually.", "Review records your check; it does not confirm a sale or profit.");
        for (String line : lines) {
            if (top + 10 > bottom) break;
            text(g, line, left, top, contentWidth, p.manualReview ? RotClientTheme.WARNING : RotClientTheme.TEXT_DIM); top += 15;
        }
    }

    private void drawPageCount(GuiGraphicsExtractor g, int total, int pageSize, String suffix) {
        String range = total == 0 ? "0" : (offset + 1) + "–" + Math.min(total, offset + pageSize) + " / " + total;
        text(g, range + " · " + suffix, left, contentBottom - 13, contentWidth - 72, RotClientTheme.TEXT_MUTED);
    }

    private void clampListOffset(int total) { offset = Math.min(offset, Math.max(0, (total - 1) / Math.max(1, rows) * Math.max(1, rows))); }
    private static String any(String value) { return value == null || value.isBlank() ? "Any" : value; }
    private static String coins(double value) { return String.format(Locale.ROOT, "%,.0f", value); }
    private String fit(String value, int width) { return RotClientUiDraw.ellipsize(font, value, Math.max(1, width)); }
    private void text(GuiGraphicsExtractor g, String value, int tx, int ty, int width, int color) {
        RotClientUiDraw.text(g, font, RotClientUiDraw.ellipsizeAndHover(font, value, Math.max(1, width), tx, ty, 11), tx, ty, color, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);
        int mouseX = (int) Math.round(event.x()), mouseY = (int) Math.round(event.y());
        for (var action : actions) if (RotClientUiDraw.inside(mouseX, mouseY, action.x(), action.y(), action.width(), action.height())) {
            if (action.enabled().getAsBoolean()) { setFocused(null); action.run().run(); } return true;
        }
        for (var field : fields) if (RotClientUiDraw.inside(mouseX, mouseY, field.x(), field.y(), field.width(), 24)) {
            setFocused(field.widget());
            if (RotClientUiDraw.inside(mouseX, mouseY, field.x() + 6, field.y() + 6, Math.max(1, field.width() - 12), 14))
                field.widget().mouseClicked(event, doubleClick);
            return true;
        }
        if (page == 0 && !filters && editable()) {
            var choices = choices();
            for (int i = offset; i < Math.min(choices.size(), offset + rows); i++) {
                int top = listTop + (i - offset) * rowHeight;
                if (RotClientUiDraw.inside(mouseX, mouseY, left, top, contentWidth, rowHeight - 3)) {
                    var choice = choices.get(i); var selectedIds = choice.pinned() ? pinIds : watchIds;
                    if (!selectedIds.add(choice.id())) selectedIds.remove(choice.id()); return true;
                }
            }
        } else if (page == 3 && !purchaseDetails) {
            var positions = MarketTradeRuntime.journal().positions();
            for (int i = offset; i < Math.min(positions.size(), offset + rows); i++) {
                int top = listTop + (i - offset) * rowHeight;
                if (RotClientUiDraw.inside(mouseX, mouseY, left, top, contentWidth, rowHeight - 3)) {
                    selectedPositionId = positions.get(positions.size() - 1 - i).id; purchaseDetails = true; rebuildWidgets(); return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (!RotClientUiDraw.inside((int) mouseX, (int) mouseY, left, contentTop, contentWidth, contentBottom - contentTop)
                || vertical == 0 || purchaseDetails) return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        for (var field : fields) if (RotClientUiDraw.inside((int) mouseX, (int) mouseY, field.x(), field.y(), field.width(), 24))
            return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
        int total = formCount > 0 ? formCount : page == 0 ? choices().size() : MarketTradeRuntime.journal().positions().size();
        int step = formCount > 0 ? formPageSize : rows;
        int next = Math.max(0, Math.min(Math.max(0, (total - 1) / Math.max(1, step) * Math.max(1, step)), offset + (vertical < 0 ? step : -step)));
        if (next != offset) { offset = next; rebuildWidgets(); } return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE && (filters || purchaseDetails)) {
            filters = purchaseDetails = false; offset = 0; rebuildWidgets(); return true;
        }
        return super.keyPressed(event);
    }

    @Override public void onClose() { Minecraft.getInstance().gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
