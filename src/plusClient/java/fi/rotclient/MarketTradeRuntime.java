package fi.rotclient;

import fi.rotclient.mixin.MarketTradeSignAccess;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

/** Plus-only, opt-in transaction controller. Server evidence, not clicks, advances money outcomes. */
public final class MarketTradeRuntime {
    public static void cancelOnEscape(net.minecraft.client.input.KeyEvent event, int action) {
        if (enabled && event != null && action == com.mojang.blaze3d.platform.InputConstants.PRESS
                && event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) stop("Cancelled with Esc; review any sent transaction");
    }
    private enum Stage { IDLE, COUNTDOWN, ROOT, BROWSER, SEARCH, FILTERS, VIEW, BUY, OWNED,
        LIST_ROOT, DRAFT, PUT_ITEM, PRICE, DURATION, CREATE, LIST_CONFIRM, LISTED,
        BZ_PRECHECK, BZ_PRODUCT, BZ_AMOUNT, BZ_PRICE, BZ_CONFIRM, BZ_SETUP, BZ_WAIT,
        BZ_CLAIM, BZ_SELL_PRODUCT, BZ_SELL_AMOUNT, BZ_SELL_PRICE, BZ_SELL_CONFIRM, BZ_SELL_SETUP, BZ_SELL_WAIT }
    private static MarketTradeJournal journal;
    private static boolean enabled;
    private static Stage stage = Stage.IDLE;
    private static String status = "Off — configure budget and targets first";
    private static String owner = "", profile = "", server = "";
    private static Object connection, world;
    private static long entered, lastScan, lastSnapshot, lastAction;
    private static final MarketTradePolicy.Gate gate = new MarketTradePolicy.Gate();
    private static MarketTradeJournal.Position active;
    private static MarketTradePolicy.Countdown countdown;
    private static String searchName = "";
    private static final Map<String, Long> references = new HashMap<>();
    private static final Map<String, Long> referenceTimes = new HashMap<>();
    private static final Set<String> attempted = new HashSet<>();
    private static List<String> baselineInventory = List.of();
    private static long baselineQuantity;
    private static boolean bazaarPrechecked;
    private static long lastBazaarSnapshot;
    private static final Map<String, Double> bazaarReferences = new HashMap<>();
    private static final java.net.http.HttpClient HTTP = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(8)).build();
    private static long lastSalesPoll;
    private static long lastReconcile, reconciledAuctionSnapshot;
    private static volatile List<Sale> sales = List.of();
    private static boolean salesPending;
    private record Sale(String auctionUuid, String seller, String itemUuid, double gross, long endedAt) {}
    static MarketTradeJournal journal() {
        if (journal == null) journal = new MarketTradeJournal(FabricLoader.getInstance().getConfigDir()
                .resolve("rotclient-market-trades.json"));
        return journal;
    }
    static boolean enabled() { return enabled; }
    static String status() { return status; }
    static long purse() { return MarketTradeCandidatePolicy.purse(Arrays.asList(SkyBlockSidebar.text().split("\\R"))).orElse(-1); }
    static String profile(Minecraft c) { return SkyBlockProfileIdentity.detectRaw(CommissionDisplayRuntime.tabLines(c)).orElse(""); }
    static String server(Minecraft c) { return c.getCurrentServer() == null ? "local" : c.getCurrentServer().ip; }
    static boolean arm() {
        var c = Minecraft.getInstance(); var j = journal();
        if (!j.healthy()) { status = "Journal unreadable: preserve it and review manually"; return false; }
        if (j.positions().stream().anyMatch(MarketTradeJournal.Position::unresolved)) {
            status = "Unresolved purchase/listing: manual review required"; return false;
        }
        if (c.player == null || c.level == null || c.getConnection() == null || profile(c).isBlank() || purse() < 0) {
            status = "Need live SkyBlock profile and exact Purse"; return false;
        }
        if (j.settings().allowance(purse(), j.committedBudget()) <= 0 || targets().isEmpty() && bazaarTargets().isEmpty()) {
            status = "Set positive budgets and select a target"; return false;
        }
        owner = MarketTradeCandidatePolicy.uuid(c.player.getUUID().toString()); profile = profile(c); server = server(c);
        connection = c.getConnection(); world = c.level; enabled = true; active = null; stage = Stage.IDLE;
        status = "Armed — waiting for a verified deal"; lastScan = lastSnapshot = lastBazaarSnapshot = 0;
        references.clear(); referenceTimes.clear(); bazaarReferences.clear();
        return true;
    }
    static void stop(String reason) {
        enabled = false;
        if (active != null) { active.status = (active.buySent ? "REVIEW: " : "CANCELLED: ") + reason; journal().save(); }
        active = null; stage = Stage.IDLE; status = reason; gate.reset();
    }
    private static List<MarketTradeCandidatePolicy.Target> targets() {
        var result = new ArrayList<MarketTradeCandidatePolicy.Target>();
        for (var pin : MarketWatchPinnedDealStore.all()) if (pin.market() == MarketWatchOpportunity.Market.AUCTION_HOUSE
                && !pin.itemId().isBlank() && !pin.tier().isBlank() && !pin.reforge().equals("?"))
            result.add(new MarketTradeCandidatePolicy.Target(pin.itemId(), pin.itemName(), pin.tier(),
                    pin.reforge().isBlank() ? "none" : pin.reforge(), 0, 0, 0, 0));
        for (var watch : MarketWatchRuntime.auctionWatches()) if (watch.enabled && journal().settings().watchIds.contains(watch.id))
            result.add(new MarketTradeCandidatePolicy.Target(watch.itemId, watch.itemName, watch.tier, watch.reforge,
                    watch.maxPriceCoins, watch.minProfitCoins, watch.minDiscountPercent, watch.minProfitPercent));
        return List.copyOf(result);
    }
    static void tick(Minecraft c) {
        if (journal == null) return;
        long now = System.currentTimeMillis();
        reconcile(c, now);
        if (!enabled) return;
        if (!journal.healthy()) { stop("Journal write failed; no further trades"); return; }
        if (c.player == null || c.level != world || c.getConnection() != connection
                || !owner.equals(MarketTradeCandidatePolicy.uuid(c.player.getUUID().toString()))
                || !server.equals(server(c)) || !profile.equals(profile(c))) { stop("World/account/profile changed"); return; }
        if (stage == Stage.IDLE) { scan(c, now); return; }
        if (stage == Stage.COUNTDOWN) {
            long remaining = countdown.remaining(now);
            if (remaining < 0) { stop("Clock changed"); return; }
            status = "Good item found: " + active.name + " — " + ((remaining + 999) / 1000) + "s";
            if (!stillEligible(now)) { stop("Deal, target, market or budget changed"); return; }
            if (remaining > 0) return;
            if (!journal.add(active)) { stop("Could not persist trade intent"); return; }
            c.gui.setScreen(null); command(c, "BZ".equals(active.market) ? "bz" : "ah");
            move("BZ".equals(active.market) ? Stage.BZ_PRECHECK : Stage.ROOT, now); return;
        }
        if (now < entered || now - entered > (stage == Stage.BZ_WAIT || stage == Stage.BZ_SELL_WAIT ? 300_000 : stage == Stage.LISTED ? 120_000 : 30_000)) { stop("Timed out at " + stage + "; no automatic retry"); return; }
        if (stage == Stage.BZ_CLAIM && count(c, active.itemId) - baselineQuantity == active.quantity) {
            active.bought = true; active.status = "BOUGHT — exact Bazaar inventory delta";
            if (!journal.save()) { stop("Could not persist Bazaar claim"); return; }
            c.player.closeContainer(); command(c, "bz " + searchName); move(Stage.BZ_SELL_PRODUCT, now); return;
        }
        if (stage == Stage.OWNED && owned(c).size() == 1 && !baselineInventory.contains(active.itemUuid)) {
            active.bought = true; active.status = "BOUGHT — unique item in inventory";
            if (!journal.save()) { stop("Purchase observation could not be saved"); return; }
            c.player.closeContainer(); command(c, "ah"); move(Stage.LIST_ROOT, now); return;
        }
        if (stage == Stage.LISTED && active.listed) {
            active = null; stage = Stage.IDLE; status = "Listed — watching for confirmed sale"; return;
        }
        if (c.gui.screen() instanceof AbstractSignEditScreen sign) {
            if (now - lastAction < 300 || !(sign instanceof MarketTradeSignAccess access)) return;
            List<String> lines = Arrays.asList(access.rotclient$marketLines());
            String text;
            if (stage == Stage.SEARCH && MarketTradeMenuPolicy.searchSign(lines)) text = searchName;
            else if (stage == Stage.PRICE && MarketTradeMenuPolicy.numericSign(lines, "price")) text = Long.toString((long) active.sellUnit);
            else if ((stage == Stage.BZ_AMOUNT || stage == Stage.BZ_SELL_AMOUNT) && MarketTradeMenuPolicy.numericSign(lines,
                    stage == Stage.BZ_AMOUNT ? "buy_amount" : "sell_amount")) text = Long.toString(active.quantity);
            else if ((stage == Stage.BZ_PRICE || stage == Stage.BZ_SELL_PRICE) && MarketTradeMenuPolicy.numericSign(lines,"price"))
                text = java.math.BigDecimal.valueOf(stage == Stage.BZ_PRICE ? active.buyUnit : active.sellUnit).toPlainString();
            else return;
            access.rotclient$marketLines()[0] = text; lastAction = now;
            move(switch(stage) {
                case SEARCH -> Stage.FILTERS; case BZ_AMOUNT -> Stage.BZ_PRICE; case BZ_SELL_AMOUNT -> Stage.BZ_SELL_PRICE;
                case BZ_PRICE -> Stage.BZ_CONFIRM; case BZ_SELL_PRICE -> Stage.BZ_SELL_CONFIRM; default -> Stage.DURATION;
            }, now);
            access.rotclient$marketDone(); return;
        }
        var menu = menu(c);
        if (menu == null || !gate.canAct(menu, now)) return;
        if (active != null && "BZ".equals(active.market)) { bazaar(c, menu, now); return; }
        switch (stage) {
            case ROOT -> {
                if (root(menu)) click(c, menu, menu.unique("Browse Auctions", "Auctions Browser"), Stage.BROWSER, now);
            }
            case BROWSER -> {
                if (browser(menu) && named(menu, 48, "Search")) click(c, menu, 48, Stage.SEARCH, now);
            }
            case FILTERS -> {
                if (!browser(menu)) return;
                if (!MarketTradeMenuPolicy.selected(menu.slot(52), "BIN Only")) {
                    if (named(menu, 52, "Auction Type")) click(c, menu, 52, Stage.FILTERS, now); return;
                }
                if (!MarketTradeMenuPolicy.selected(menu.slot(51), active.tier)) {
                    if (named(menu, 51, "Item Tier", "Rarity")) click(c, menu, 51, Stage.FILTERS, now); return;
                }
                if (!stillEligible(now)) { stop("Offer changed before opening"); return; }
                c.player.closeContainer(); command(c, "viewauction " + active.auctionUuid); move(Stage.VIEW, now);
            }
            case VIEW -> {
                if (!menu.title().equals("BIN Auction View") || menu.size() != 54 || !liveItem(menu, 13)) return;
                int buy = menu.unique("Buy Item Right Now", "Buy Item");
                if (buy < 0 || !price(menu, active.buyUnit, "Buy it now", "Price")) return;
                if (!stillEligible(now)) { stop("Deal no longer qualifies"); return; }
                click(c, menu, buy, Stage.BUY, now);
            }
            case BUY -> {
                if (!menu.title().equals("Confirm Purchase") || menu.size() != 27 || !liveItem(menu, 13)
                        || !price(menu, active.buyUnit, "Price", "Buy it now")) return;
                int confirm = menu.unique("Confirm");
                if (confirm < 0) return;
                if (!stillEligible(now) || !owned(c).isEmpty()) { stop("Purchase identity or budget changed"); return; }
                active.buySent = true; active.status = "PURCHASE SENT — awaiting server evidence";
                if (!journal.save()) { stop("Could not persist purchase intent"); return; }
                click(c, menu, confirm, Stage.OWNED, now);
            }
            case OWNED -> {
                if (owned(c).size() == 1 && !baselineInventory.contains(active.itemUuid)) {
                    active.bought = true; active.status = "BOUGHT — unique item in inventory";
                    if (!journal.save()) { stop("Purchase observation could not be saved"); return; }
                    c.player.closeContainer(); command(c, "ah"); move(Stage.LIST_ROOT, now); return;
                }
                if (menu.title().equals("BIN Auction View") && menu.size() == 54 && liveItem(menu, 13)) {
                    int collect = menu.unique("Collect Auction");
                    if (collect >= 0 && menu.slot(collect).lore().stream().anyMatch(s ->
                            CommissionDisplayPolicy.normalizeLine(s).equals("Click to pick up item!")))
                        click(c, menu, collect, Stage.OWNED, now);
                }
            }
            case LIST_ROOT -> {
                if (root(menu) || menu.title().equals("Manage Auctions")) {
                    int create = menu.unique("Create Auction", "Create BIN Auction");
                    if (create >= 0) click(c, menu, create, Stage.DRAFT, now);
                    else if (root(menu)) click(c, menu, menu.unique("Manage Auctions"), Stage.LIST_ROOT, now);
                }
            }
            case DRAFT -> {
                if (menu.size() != 54 || !(menu.title().equals("Create Auction") || menu.title().equals("Create BIN Auction"))) return;
                if (menu.title().equals("Create Auction")) { click(c, menu, menu.unique("Switch to BIN"), Stage.DRAFT, now); return; }
                if (liveItem(menu, 13)) { move(Stage.PRICE, now); return; }
                // The draft slot must be genuinely empty or the documented item placeholder.
                var target = menu.slot(13);
                if (target != null && !(target.name().equals("Auction Item") && target.id().isBlank())) return;
                if (!c.player.containerMenu.getCarried().isEmpty() || owned(c).size() != 1) return;
                int slot = inventorySlot(c, menu);
                if (slot >= 0) click(c, menu, slot, Stage.PUT_ITEM, now);
            }
            case PUT_ITEM -> {
                if (!menu.title().equals("Create BIN Auction") || menu.size() != 54) return;
                if (liveItem(menu, 13)) { move(Stage.PRICE, now); return; }
                if (matches(c.player.containerMenu.getCarried())) click(c, menu, 13, Stage.PUT_ITEM, now);
            }
            case PRICE -> {
                if (menu.title().equals("Create BIN Auction") && menu.size() == 54 && liveItem(menu, 13)) {
                    int slot = menu.unique("Auction Price");
                    if (slot >= 0) {
                        var price = MarketTradeMenuPolicy.coins(menu.slot(slot).lore(), "Price");
                        if (price.isPresent() && MarketTradeMenuPolicy.equal(price.getAsDouble(), active.sellUnit)) move(Stage.DURATION, now);
                        else click(c, menu, slot, Stage.PRICE, now);
                    }
                }
            }
            case DURATION -> {
                if (menu.title().equals("Create BIN Auction") && liveItem(menu, 13)) {
                    int slot = menu.unique("Auction Duration");
                    if (slot < 0) return;
                    if (duration(menu.slot(slot), journal.settings().listingHours)) move(Stage.CREATE, now);
                    else click(c, menu, slot, Stage.DURATION, now);
                } else if (menu.title().equals("Auction Duration")) {
                    int hours = journal.settings().listingHours;
                    int slot = menu.unique(hours == 48 ? "2 Days" : hours == 24 ? "1 Day" : hours + (hours == 1 ? " Hour" : " Hours"));
                    if (slot >= 0) click(c, menu, slot, Stage.DURATION, now);
                }
            }
            case CREATE -> {
                if (!menu.title().equals("Create BIN Auction") || menu.size() != 54 || !liveItem(menu, 13)
                        || !price(menu, active.sellUnit, "Price")) return;
                int duration = menu.unique("Auction Duration");
                if (duration < 0 || !duration(menu.slot(duration), journal.settings().listingHours)) return;
                click(c, menu, menu.unique("Create BIN Auction"), Stage.LIST_CONFIRM, now);
            }
            case LIST_CONFIRM -> {
                if (!menu.title().equals("Confirm BIN Auction") || !price(menu, active.sellUnit, "Price")) return;
                int confirm = menu.unique("Confirm Auction");
                if (confirm < 0) return;
                var fee = MarketTradeMenuPolicy.coins(menu.slot(confirm).lore(), "Creation fee", "Listing fee", "Fee", "Cost");
                if (fee.isEmpty()) return; // Unobserved fee grammar is never guessed.
                if (fee.getAsDouble() > journal.settings().maxListingFeeCoins || purse() < 0
                        || purse() - journal.settings().reserveCoins < fee.getAsDouble()) { stop("Listing fee exceeds budget"); return; }
                active.listingFee = fee.getAsDouble(); active.listingFeeKnown = true;
                active.status = "LISTING SENT — awaiting own auction UUID";
                if (!journal.save()) { stop("Could not persist listing intent"); return; }
                click(c, menu, confirm, Stage.LISTED, now);
            }
            case LISTED -> { if (active.listed) { active = null; stage = Stage.IDLE; status = "Listed — watching for confirmed sale"; } }
            default -> { }
        }
    }
    private static void scan(Minecraft c, long now) {
        if (now < lastScan || now - lastScan < 1000) return;
        lastScan = now;
        var snapshot = MarketWatchDataService.currentAuctions();
        if (!MarketTradeCandidatePolicy.fresh(snapshot.observedAtMillis(), snapshot.snapshot().lastUpdated(), now, journal.settings())) {
            status = "Waiting for fresh auction data"; scanBazaar(c, now); return;
        }
        if (journal.openPositions() >= journal.settings().maxOpenPositions) {
            status = "Open-position limit reached"; return;
        }
        if (!journal.settings().auctionEnabled || snapshot.observedAtMillis() == lastSnapshot) { scanBazaar(c, now); return; }
        lastSnapshot = snapshot.observedAtMillis();
        long purse = purse(); if (purse < 0) { status = "Exact live Purse unavailable"; return; }
        var targets = targets();
        var candidates = snapshot.snapshot().auctions().stream().filter(a -> !attempted.contains(a.uuid())
                && targets.stream().anyMatch(t -> t.matches(a))).sorted(Comparator.comparingLong(MarketWatchAuction::startingBid)).toList();
        Map<String, List<MarketWatchAuction>> groups = new HashMap<>();
        for (var a : snapshot.snapshot().auctions()) if (a.bin()) groups.computeIfAbsent(
                MarketWatchVariantPolicy.comparableKey(a), k -> new ArrayList<>()).add(a);
        for (var a : candidates) {
            String key = MarketWatchVariantPolicy.comparableKey(a);
            var proposal = MarketTradeCandidatePolicy.propose(a, groups.getOrDefault(key, List.of()), targets,
                    journal.settings(), purse, journal.committedBudget(), owner, now, references.getOrDefault(key, 0L));
            if (proposal == null) continue;
            Long seenAt = referenceTimes.get(key);
            if (seenAt == null) {
                references.put(key,proposal.sell() + journal.settings().undercutCoins);
                referenceTimes.put(key,snapshot.snapshot().lastUpdated());
                status = "Waiting for a second independent market snapshot"; continue;
            }
            if (snapshot.snapshot().lastUpdated() <= seenAt) continue;
            references.put(key, proposal.sell() + journal.settings().undercutCoins);
            referenceTimes.put(key,snapshot.snapshot().lastUpdated());
            if (inventory(c).stream().anyMatch(item -> item.uuid().equals(a.variant().itemUuid()))) continue;
            active = new MarketTradeJournal.Position(); active.id = UUID.randomUUID().toString(); active.market = "AH";
            active.itemId = a.variant().itemId(); active.itemUuid = a.variant().itemUuid(); active.name = a.itemName();
            active.tier = a.tier(); active.reforge = a.variant().reforge(); active.auctionUuid = a.uuid();
            active.attributesKey = a.variant().attributesKey(); active.sellerUuid = a.auctioneerUuid();
            active.ownerUuid = owner; active.profile = profile; active.server = server;
            active.buyUnit = proposal.buy(); active.sellUnit = proposal.sell(); active.quantity = 1;
            active.reserved = proposal.reserved(); active.createdAt = now; searchName = MarketWatchItemCatalog.displayName(active.itemId);
            if (searchName.isBlank() || searchName.length() > 60) { active = null; continue; }
            attempted.add(a.uuid()); baselineInventory = inventory(c).stream().map(MarketTradeMenuPolicy.Item::uuid).toList();
            countdown = new MarketTradePolicy.Countdown(now); move(Stage.COUNTDOWN, now); return;
        }
        status = "No selected deal passes identity, budget and manipulation checks";
        scanBazaar(c, now);
    }
    private static boolean stillEligible(long now) {
        if ("BZ".equals(active.market)) {
            var data = MarketWatchDataService.currentBazaar();
            if (!MarketTradeCandidatePolicy.fresh(data.observedAtMillis(), data.snapshot().lastUpdated(), now, journal.settings())
                    || !bazaarTargets().contains(active.itemId) || !journal.settings().bazaarEnabled) return false;
            var p = MarketTradeBazaarPolicy.propose(data.snapshot().product(active.itemId), journal.settings(), purse(),
                    journal.committedBudget(), bazaarReferences.getOrDefault(active.itemId, 0D));
            return p != null && p.buy() <= active.buyUnit && p.sell() >= active.sellUnit && p.quantity() >= active.quantity;
        }
        var data = MarketWatchDataService.currentAuctions();
        if (!MarketTradeCandidatePolicy.fresh(data.observedAtMillis(), data.snapshot().lastUpdated(), now, journal.settings())) return false;
        var a = data.snapshot().auctions().stream().filter(row -> row.uuid().equals(active.auctionUuid)).findFirst().orElse(null);
        var p = MarketTradeCandidatePolicy.propose(a, data.snapshot().auctions(), targets(), journal.settings(),
                purse(), journal.committedBudget(), owner, now, references.getOrDefault(a == null ? "" : MarketWatchVariantPolicy.comparableKey(a), 0L));
        return p != null && p.buy() == active.buyUnit && p.sell() >= active.sellUnit
                && a.variant().itemUuid().equals(active.itemUuid) && a.variant().attributesKey().equals(active.attributesKey);
    }
    private static Set<String> bazaarTargets() {
        var result = new HashSet<String>();
        for (var pin : MarketWatchPinnedDealStore.all()) if (pin.market() == MarketWatchOpportunity.Market.BAZAAR) result.add(pin.itemId());
        var selected = MarketWatchRuntime.bazaarWatches().stream()
                .filter(w -> w.enabled && journal().settings().watchIds.contains(w.id) && w.buyDealPercent <= 0).toList();
        for (var match : MarketWatchEvaluator.evaluateBazaar(selected, MarketWatchDataService.currentBazaar().snapshot())) result.add(match.productId());
        return result;
    }
    private static void scanBazaar(Minecraft c, long now) {
        if (!journal.settings().bazaarEnabled || journal.openPositions() >= journal.settings().maxOpenPositions) return;
        var data = MarketWatchDataService.currentBazaar();
        if (data.observedAtMillis() == lastBazaarSnapshot || !MarketTradeCandidatePolicy.fresh(data.observedAtMillis(),
                data.snapshot().lastUpdated(),now,journal.settings())) return;
        lastBazaarSnapshot = data.observedAtMillis();
        for (String id : bazaarTargets()) {
            if (attempted.contains("BZ:" + id)) continue;
            var proposal = MarketTradeBazaarPolicy.propose(data.snapshot().product(id),journal.settings(),purse(),journal.committedBudget(),
                    bazaarReferences.getOrDefault(id,0D));
            if (proposal == null) continue;
            String key = "BZ:" + id;
            Long seenAt = referenceTimes.get(key);
            if (seenAt == null) {
                bazaarReferences.put(id,proposal.sell() + .1); referenceTimes.put(key,data.snapshot().lastUpdated());
                status = "Waiting for a second independent Bazaar snapshot"; continue;
            }
            if (data.snapshot().lastUpdated() <= seenAt) continue;
            referenceTimes.put(key,data.snapshot().lastUpdated());
            active = new MarketTradeJournal.Position(); active.id = UUID.randomUUID().toString(); active.market = "BZ";
            active.itemId = id; active.itemUuid = active.auctionUuid = active.tier = active.reforge = active.sellerUuid = "";
            active.name = MarketWatchItemCatalog.displayName(id); searchName = active.name;
            if (searchName.isBlank() || searchName.length() > 60) { active = null; continue; }
            active.ownerUuid = owner; active.profile = profile; active.server = server;
            active.buyUnit = proposal.buy(); active.sellUnit = proposal.sell(); active.quantity = proposal.quantity();
            active.reserved = proposal.reserved(); active.createdAt = now;
            baselineQuantity = count(c,id); bazaarPrechecked = false;
            bazaarReferences.put(id,data.snapshot().product(id).quickBuyPrice()); attempted.add("BZ:" + id);
            countdown = new MarketTradePolicy.Countdown(now); move(Stage.COUNTDOWN,now); return;
        }
    }
    private static long count(Minecraft c, String id) {
        return inventory(c).stream().filter(i -> id.equals(i.id())).mapToLong(MarketTradeMenuPolicy.Item::count).sum();
    }
    private static boolean bazaarOrders(MarketTradeMenuPolicy.Menu m) {
        return m.title().equals("Your Bazaar Orders") || m.title().equals("Co-op Bazaar Orders");
    }
    private static boolean bazaarProduct(MarketTradeMenuPolicy.Menu m) {
        return m.title().contains(" ➜ ") && m.slot(13) != null && active.itemId.equals(m.slot(13).id());
    }
    private static void bazaar(Minecraft c, MarketTradeMenuPolicy.Menu m, long now) {
        switch (stage) {
            case BZ_PRECHECK -> {
                if (!bazaarOrders(m)) { click(c,m,m.unique("Manage Orders"),Stage.BZ_PRECHECK,now); return; }
                if (m.unique("Next Page") >= 0 || m.items().stream().anyMatch(i -> active.itemId.equals(i.id())
                        && (i.name().startsWith("BUY ") || i.name().startsWith("SELL ")))) {
                    stop("Existing or paginated Bazaar orders: review manually"); return;
                }
                bazaarPrechecked = true; c.player.closeContainer(); command(c,"bz " + searchName); move(Stage.BZ_PRODUCT,now);
            }
            case BZ_PRODUCT, BZ_SELL_PRODUCT -> {
                boolean sell = stage == Stage.BZ_SELL_PRODUCT;
                if (!bazaarProduct(m)) {
                    var matches = m.items().stream().filter(i -> active.itemId.equals(i.id())).toList();
                    if (matches.size() == 1) click(c,m,matches.getFirst().slot(),stage,now); return;
                }
                if (sell && count(c,active.itemId) - baselineQuantity != active.quantity) { stop("Bazaar owned quantity changed"); return; }
                click(c,m,m.unique(sell ? "Create Sell Offer" : "Create Buy Order"),sell ? Stage.BZ_SELL_AMOUNT : Stage.BZ_AMOUNT,now);
            }
            case BZ_AMOUNT, BZ_SELL_AMOUNT -> click(c,m,m.unique("Custom Amount"),stage,now);
            case BZ_PRICE, BZ_SELL_PRICE -> click(c,m,m.unique("Custom Price"),stage,now);
            case BZ_CONFIRM, BZ_SELL_CONFIRM -> {
                boolean sell = stage == Stage.BZ_SELL_CONFIRM;
                if (!m.title().equals(sell ? "Confirm Sell Offer" : "Confirm Buy Order")) return;
                int confirm = m.unique("Confirm",sell ? "Confirm Sell Offer" : "Confirm Buy Order");
                if (confirm < 0 || !m.items().stream().anyMatch(i -> active.itemId.equals(i.id()))
                        || !m.items().stream().anyMatch(i -> MarketTradeBazaarPolicy.amount(i.lore(),active.quantity))
                        || !price(m,sell ? active.sellUnit : active.buyUnit,"Price per unit")) return;
                if (!sell && (!bazaarPrechecked || !stillEligible(now) || count(c,active.itemId) != baselineQuantity)) {
                    stop("Bazaar target, budget or inventory changed"); return;
                }
                if (sell && count(c,active.itemId) - baselineQuantity != active.quantity) { stop("Bazaar quantity changed"); return; }
                if (!sell) active.buySent = true;
                active.status = sell ? "SELL OFFER SENT — receipt pending" : "BUY ORDER SENT — receipt pending";
                if (!journal.save()) { stop("Could not persist Bazaar transaction intent"); return; }
                click(c,m,confirm,sell ? Stage.BZ_SELL_SETUP : Stage.BZ_SETUP,now);
            }
            case BZ_WAIT, BZ_SELL_WAIT -> {
                boolean sell = stage == Stage.BZ_SELL_WAIT;
                if (!bazaarOrders(m)) { click(c,m,m.unique("Manage Orders"),stage,now); return; }
                var matches = m.items().stream().filter(i -> active.itemId.equals(i.id())
                        && MarketTradePolicy.bazaarOrder(i,active,sell,false)
                        && MarketTradeBazaarPolicy.owner(i.lore(),c.player.getGameProfile().name())).toList();
                if (matches.size() > 1) { stop("Ambiguous Bazaar order identity"); return; }
                if (matches.size() == 1 && MarketTradePolicy.bazaarOrder(matches.getFirst(),active,sell,true)) {
                    if (sell) {
                        active.sold = true; active.soldGross = active.sellUnit * active.quantity; active.soldAt = now;
                        active.status = "SOLD — full owned offer, tax/coin collection not verified";
                        if (!journal.save()) { stop("Could not save confirmed Bazaar fill"); return; }
                        active = null; stage = Stage.IDLE; status = "Bazaar sale confirmed; coins remain for manual collection";
                    } else if (matches.getFirst().lore().stream().map(CommissionDisplayPolicy::normalizeLine)
                            .anyMatch(line -> line.equals("Click to claim!") || line.equals("Click to claim items!")))
                        click(c,m,matches.getFirst().slot(),Stage.BZ_CLAIM,now);
                } else if (now - lastAction >= 5000) click(c,m,m.unique("Refresh"),stage,now);
            }
            default -> { }
        }
    }
    static void onChat(Component message) {
        String escrow = message == null ? "" : CommissionDisplayPolicy.normalizeLine(message.getString());
        if (enabled && (escrow.startsWith("There was an error with the auction house!") || escrow.startsWith("Escrow refunded "))) {
            stop("Escrow failure; review the sent transaction before retrying"); return;
        }
        var current = Minecraft.getInstance();
        if (active != null && (current.player == null || current.getConnection() != connection
                || !profile.equals(profile(current)))) { stop("Account/profile context changed"); return; }
        if (active != null && active.buySent && "BZ".equals(active.market)) {
            String line = message == null ? "" : message.getString();
            if (stage == Stage.BZ_SETUP && MarketTradePolicy.bazaarSetupReceipt(line,active,false)) {
                active.status = "BUY ORDER CREATED — fill pending";
                if (!journal.save()) { stop("Could not save Bazaar order receipt"); return; }
                var c = Minecraft.getInstance(); c.player.closeContainer(); command(c,"bz"); move(Stage.BZ_WAIT,System.currentTimeMillis());
            } else if (stage == Stage.BZ_SELL_SETUP && MarketTradePolicy.bazaarSetupReceipt(line,active,true)) {
                active.listed = true; active.listingFeeKnown = true; active.status = "SELL OFFER CREATED — sale pending";
                if (!journal.save()) { stop("Could not save Bazaar offer receipt"); return; }
                var c = Minecraft.getInstance(); c.player.closeContainer(); command(c,"bz"); move(Stage.BZ_SELL_WAIT,System.currentTimeMillis());
            }
            return;
        }
        if (active != null && active.buySent && MarketTradePolicy.purchaseReceipt(message == null ? "" : message.getString(), active)) {
            active.status = "PURCHASE RECEIPT — awaiting unique owned item"; journal.save();
        }
    }
    private static void move(Stage next, long now) {
        if (stage != next) { stage = next; entered = now; }
        if (next != Stage.COUNTDOWN) status = "Trading: " + next;
    }
    private static void command(Minecraft c, String command) { c.getConnection().sendCommand(command); gate.reset(); }
    private static void click(Minecraft c, MarketTradeMenuPolicy.Menu menu, int slot, Stage next, long now) {
        if (slot < 0 || c.gameMode == null || slot >= c.player.containerMenu.slots.size()) return;
        gate.sent(menu, now); lastAction = now;
        c.gameMode.handleContainerInput(menu.containerId(), slot, 0, ContainerInput.PICKUP, c.player); move(next, now);
    }
    private static boolean root(MarketTradeMenuPolicy.Menu m) { return m.size() == 54 && (m.title().equals("Auction House") || m.title().equals("Co-op Auction House")); }
    private static boolean browser(MarketTradeMenuPolicy.Menu m) { return m.size() == 54 && (m.title().equals("Auctions Browser") || m.title().startsWith("Auctions: \"")); }
    private static boolean named(MarketTradeMenuPolicy.Menu m, int slot, String... names) {
        return m.slot(slot) != null && Arrays.stream(names).anyMatch(n -> n.equalsIgnoreCase(m.slot(slot).name()));
    }
    private static boolean price(MarketTradeMenuPolicy.Menu m, double expected, String... labels) {
        var prices = m.items().stream().filter(i -> i.slot() < m.size()).map(i -> MarketTradeMenuPolicy.coins(i.lore(), labels))
                .filter(OptionalDouble::isPresent).mapToDouble(OptionalDouble::getAsDouble).toArray();
        return prices.length > 0 && Arrays.stream(prices).allMatch(p -> MarketTradeMenuPolicy.equal(p, expected));
    }
    private static boolean duration(MarketTradeMenuPolicy.Item item, int hours) {
        return item != null && item.lore().stream().map(CommissionDisplayPolicy::normalizeLine).anyMatch(row ->
                row.equals("Duration: " + (hours == 48 ? "2 Days" : hours == 24 ? "1 Day" : hours + (hours == 1 ? " Hour" : " Hours"))));
    }
    private static boolean liveItem(MarketTradeMenuPolicy.Menu m, int slot) {
        var item = m.slot(slot);
        return MarketTradeMenuPolicy.variant(item, active.itemId, active.itemUuid, active.tier, active.reforge, 1)
                && active.attributesKey.equals(attributes(m.identity(), slot));
    }
    private static String attributes(Object identity, int slot) {
        if (!(identity instanceof ChestMenu menu) || slot < 0 || slot >= menu.slots.size()) return "";
        return MarketWatchItemVariant.attributesKey(SkyBlockItemData.extraAttributes(menu.getSlot(slot).getItem()));
    }
    private static boolean matches(ItemStack stack) {
        return MarketTradeMenuPolicy.variant(item(-1, stack), active.itemId, active.itemUuid, active.tier, active.reforge, 1)
                && active.attributesKey.equals(MarketWatchItemVariant.attributesKey(SkyBlockItemData.extraAttributes(stack)));
    }
    private static List<MarketTradeMenuPolicy.Item> owned(Minecraft c) {
        return inventory(c).stream().filter(i -> MarketTradeMenuPolicy.variant(i, active.itemId, active.itemUuid,
                active.tier, active.reforge, 1)).toList();
    }
    private static int inventorySlot(Minecraft c, MarketTradeMenuPolicy.Menu m) {
        var slots = c.player.containerMenu.slots;
        int found = -1;
        for (int i = m.size(); i < slots.size(); i++) if (matches(slots.get(i).getItem())) { if (found >= 0) return -1; found = i; }
        return found;
    }
    private static List<MarketTradeMenuPolicy.Item> inventory(Minecraft c) {
        var result = new ArrayList<MarketTradeMenuPolicy.Item>(); if (c.player == null) return result;
        var inv = c.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (!inv.getItem(i).isEmpty()) result.add(item(i, inv.getItem(i)));
        return result;
    }
    private static MarketTradeMenuPolicy.Item item(int slot, ItemStack stack) {
        var lore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines().stream().map(Component::getString)
                .map(CommissionDisplayPolicy::normalizeLine).toList();
        var extra = SkyBlockItemData.extraAttributes(stack);
        return new MarketTradeMenuPolicy.Item(slot, CommissionDisplayPolicy.normalizeLine(stack.getHoverName().getString()),
                SkyBlockItemIdentity.skyBlockId(stack), SkyBlockItemData.uuid(stack), MarketTradeMenuPolicy.tier(lore),
                extra == null ? "" : extra.getString("modifier").orElse(""), stack.getCount(), lore);
    }
    private static MarketTradeMenuPolicy.Menu menu(Minecraft c) {
        if (!(c.gui.screen() instanceof AbstractContainerScreen<?> screen) || !(screen.getMenu() instanceof ChestMenu menu)) return null;
        var items = new ArrayList<MarketTradeMenuPolicy.Item>(); int size = menu.getContainer().getContainerSize();
        for (int i = 0; i < size; i++) if (!menu.getSlot(i).getItem().isEmpty()) items.add(item(i, menu.getSlot(i).getItem()));
        return new MarketTradeMenuPolicy.Menu(menu, menu.containerId, menu.getStateId(),
                CommissionDisplayPolicy.normalizeLine(screen.getTitle().getString()), size, items);
    }
    private static void reconcile(Minecraft c, long now) {
        if (now >= lastReconcile && now - lastReconcile < 1000) return;
        lastReconcile = now;
        if (c.player == null || c.getConnection() == null || profile(c).isBlank()) return;
        String player = MarketTradeCandidatePolicy.uuid(c.player.getUUID().toString());
        var data = MarketWatchDataService.currentAuctions();
        boolean newSnapshot = data.observedAtMillis() != reconciledAuctionSnapshot;
        reconciledAuctionSnapshot = data.observedAtMillis();
        boolean changed = false;
        for (var p : journal.positions()) if ("AH".equals(p.market) && p.buySent && !p.sold && p.ownerUuid.equals(player)
                && p.server.equals(server(c)) && p.profile.equals(profile(c))) {
            if (!p.bought && inventory(c).stream().anyMatch(i -> MarketTradeMenuPolicy.variant(i,p.itemId,p.itemUuid,p.tier,p.reforge,1))) {
                p.bought = true; p.status = "BOUGHT — recovered unique inventory evidence"; changed = true;
            }
            if (!p.listed && newSnapshot) {
                var matches = MarketWatchDataService.currentAuctions().snapshot().auctions().stream().filter(a -> a.bin()
                        && MarketTradeCandidatePolicy.uuid(a.auctioneerUuid()).equals(player)
                        && a.variant().itemUuid().equals(p.itemUuid) && a.variant().attributesKey().equals(p.attributesKey)
                        && a.startingBid() == p.sellUnit && a.startMillis() >= p.createdAt).toList();
                if (matches.size() == 1) {
                    p.listingUuid = matches.getFirst().uuid(); p.listed = true; p.status = "LISTED — sale pending"; changed = true;
                    p.bought = true;
                }
            }
            for (var sale : sales) if (p.listed && MarketTradeCandidatePolicy.uuid(p.listingUuid).equals(sale.auctionUuid)
                    && player.equals(sale.seller) && p.itemUuid.equals(sale.itemUuid)
                    && MarketTradeMenuPolicy.equal(p.sellUnit * p.quantity, sale.gross) && sale.endedAt >= p.createdAt) {
                p.sold = true; p.soldGross = sale.gross; p.soldAt = sale.endedAt;
                p.status = "SOLD — API gross proceeds, tax/collection not verified"; changed = true;
            }
        }
        if (changed && !journal.save() && enabled) stop("Journal save failed during reconciliation");
        if (!salesPending && now >= lastSalesPoll && now - lastSalesPoll >= 60_000 && journal.positions().stream()
                .anyMatch(p -> p.listed && !p.sold && p.ownerUuid.equals(player) && p.server.equals(server(c)) && p.profile.equals(profile(c)))) {
            lastSalesPoll = now; salesPending = true;
            var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create("https://api.hypixel.net/v2/skyblock/auctions_ended"))
                    .timeout(java.time.Duration.ofSeconds(10)).GET().build();
            HTTP.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofInputStream()).whenComplete((response, error) -> {
                var result = new ArrayList<Sale>();
                if (error == null && response != null) try (var stream = response.body()) {
                    byte[] bytes = stream.readNBytes(2_097_153);
                    if (response.statusCode() == 200 && bytes.length <= 2_097_152) {
                        var json = com.google.gson.JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                        if (json.has("success") && json.get("success").getAsBoolean() && json.has("auctions")) for (var raw : json.getAsJsonArray("auctions")) {
                            var row = raw.getAsJsonObject(); var variant = MarketWatchItemVariant.decode(row.get("item_bytes").getAsString());
                            result.add(new Sale(MarketTradeCandidatePolicy.uuid(row.get("auction_id").getAsString()),
                                    MarketTradeCandidatePolicy.uuid(row.get("seller").getAsString()), variant.itemUuid(),
                                    row.get("price").getAsDouble(), row.get("timestamp").getAsLong()));
                        }
                    }
                } catch (Exception malformed) { result.clear(); }
                sales = List.copyOf(result); c.execute(() -> salesPending = false);
            });
        }
    }
    private MarketTradeRuntime() {}
}
