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

/** Plus-only, opt-in purchases. Only durable server receipts and new ownership complete a buy. */
public final class MarketTradeRuntime {
    public record PurchaseNotice(String itemName, long quantity, String market, long confirmedAtMillis, String transactionId) {}
    private static PurchaseNotice purchaseNotice;
    public static Optional<PurchaseNotice> purchaseNotice() { return Optional.ofNullable(purchaseNotice); }
    public static void cancelOnEscape(net.minecraft.client.input.KeyEvent event, int action) {
        if (enabled && event != null && action == com.mojang.blaze3d.platform.InputConstants.PRESS
                && event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE)
            stop("Cancelled with Esc; review any sent purchase");
    }
    private enum Stage { IDLE, COUNTDOWN, ROOT, BROWSER, SEARCH, FILTERS, VIEW, BUY, OWNED,
        BZ_PRECHECK, BZ_PRODUCT, BZ_AMOUNT, BZ_PRICE, BZ_CONFIRM, BZ_SETUP, BZ_WAIT, BZ_CLAIM }
    private static MarketTradeJournal journal;
    private static boolean enabled;
    private static Stage stage = Stage.IDLE;
    private static String status = "Off — configure budget and select targets first";
    private static String owner = "", profile = "", server = "";
    private static Object connection, world;
    private static long entered, lastScan, lastSnapshot, lastAction, lastTick;
    private static final MarketTradePolicy.Gate gate = new MarketTradePolicy.Gate();
    private static MarketTradeJournal.Position active;
    private static boolean activePersisted;
    private static MarketTradePolicy.Countdown countdown;
    private static String searchName = "";
    private static final Map<String, Long> references = new HashMap<>();
    private static final Map<String, Long> referenceTimes = new HashMap<>();
    private static final Set<String> attempted = new HashSet<>();
    private static boolean bazaarPrechecked;
    private static long lastBazaarSnapshot;
    private static final Map<String, Double> bazaarReferences = new HashMap<>();
    private static MarketWatchAuctionSnapshot indexedSnapshot;
    private static MarketTradeScanPolicy auctionScan;
    private static List<MarketWatchAuction> scanCandidates = List.of();
    private static int candidateCursor;
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
    static boolean markReviewed(String id) {
        if (enabled) { status = "Stop purchases before reviewing a recorded outcome"; return false; }
        boolean saved = journal().markReviewed(id);
        status = saved ? "Reviewed manually; spent budget remains committed" : "Could not save the manual review";
        return saved;
    }
    static boolean arm() {
        var c = Minecraft.getInstance(); var j = journal();
        if (!j.healthy()) { status = "Journal unreadable: preserve it and review manually"; return false; }
        if (j.positions().stream().anyMatch(MarketTradeJournal.Position::unresolved)) {
            status = "Unresolved purchase or legacy sale: manual review required"; return false;
        }
        if (c.player == null || c.level == null || c.getConnection() == null || profile(c).isBlank() || purse() < 0) {
            status = "Need live SkyBlock profile and exact Purse"; return false;
        }
        if (j.settings().allowance(purse(), j.committedBudget()) <= 0 || targets().isEmpty() && bazaarTargets().isEmpty()) {
            status = "Set positive budgets and select a target"; return false;
        }
        if (j.openPositions() >= j.settings().maxOpenPositions) {
            status = "Holding limit reached; review purchases after selling manually"; return false;
        }
        owner = MarketTradeCandidatePolicy.uuid(c.player.getUUID().toString()); profile = profile(c); server = server(c);
        connection = c.getConnection(); world = c.level; enabled = true; active = null; activePersisted = false; stage = Stage.IDLE;
        status = "Watching selected targets for a verified purchase";
        lastScan = lastSnapshot = lastBazaarSnapshot = lastTick = 0;
        references.clear(); referenceTimes.clear(); bazaarReferences.clear(); gate.reset();
        attempted.clear();
        indexedSnapshot = null; auctionScan = null;
        return true;
    }
    static void stop(String reason) {
        enabled = false;
        if (active != null) {
            active.manualReview = active.buySent;
            active.status = (active.buySent ? "REVIEW: " : "CANCELLED: ") + reason;
            if (activePersisted) journal().save();
        }
        active = null; activePersisted = false; stage = Stage.IDLE; status = reason; gate.reset();
    }
    private static List<MarketTradeCandidatePolicy.Target> targets() {
        var result = new ArrayList<MarketTradeCandidatePolicy.Target>();
        if (!journal().settings().auctionEnabled) return List.of();
        for (var pin : MarketWatchPinnedDealStore.all()) if (journal().settings().pinIds.contains(pin.id())
                && pin.market() == MarketWatchOpportunity.Market.AUCTION_HOUSE
                && !pin.itemId().isBlank() && !pin.tier().isBlank() && !pin.reforge().equals("?"))
            result.add(new MarketTradeCandidatePolicy.Target(pin.itemId(), pin.itemName(), pin.tier(),
                    pin.reforge().isBlank() ? "none" : pin.reforge(), 0, 0, 0, 0));
        for (var watch : MarketWatchRuntime.auctionWatches()) if (watch.enabled && journal().settings().watchIds.contains(watch.id))
            result.add(new MarketTradeCandidatePolicy.Target(watch.itemId, watch.itemName, watch.tier, watch.reforge,
                    watch.maxPriceCoins, watch.minProfitCoins, watch.minDiscountPercent, watch.minProfitPercent));
        return List.copyOf(result);
    }
    static void tick(Minecraft c) {
        if (journal == null || !enabled) return;
        long now = System.currentTimeMillis();
        if (!journal.healthy()) { stop("Journal write failed; no further purchases"); return; }
        if (now < lastTick) { stop("Clock changed; review any sent purchase"); return; }
        lastTick = now;
        if (c.player == null || c.level != world || c.getConnection() != connection
                || !owner.equals(MarketTradeCandidatePolicy.uuid(c.player.getUUID().toString()))
                || !server.equals(server(c)) || !profile.equals(profile(c))) { stop("World/account/profile changed"); return; }
        updateAuctionIndex();
        if (stage == Stage.IDLE) { scan(c, now); return; }
        if (stage == Stage.COUNTDOWN) {
            if (waitingForAuctionIndex()) { status = "Checking updated market data before purchase"; return; }
            long remaining = countdown.remaining(now);
            if (remaining < 0) { stop("Clock changed"); return; }
            status = "Good item found: " + active.name + " — " + ((remaining + 999) / 1000) + "s";
            if (!stillEligible(now)) { skipCandidate("Deal, target, market or budget changed"); return; }
            if (remaining > 0) return;
            if (!journal.add(active)) { stop("Could not persist purchase intent"); return; }
            activePersisted = true;
            c.gui.setScreen(null); command(c, "BZ".equals(active.market) ? "bz" : "ah");
            move("BZ".equals(active.market) ? Stage.BZ_PRECHECK : Stage.ROOT, now); return;
        }
        if (now < entered || now - entered > (stage == Stage.BZ_WAIT ? 300_000 : 30_000)) {
            stop("Timed out at " + stage + "; review before retrying"); return;
        }
        if ((stage == Stage.FILTERS || stage == Stage.VIEW || stage == Stage.BUY) && waitingForAuctionIndex()) {
            status = "Checking updated market data before purchase"; return;
        }
        if ((stage == Stage.OWNED || stage == Stage.BZ_CLAIM) && purchaseOwned(c)) {
            completePurchase(c, now); return;
        }
        if (c.gui.screen() instanceof AbstractSignEditScreen sign) {
            if (now - lastAction < 300 || !(sign instanceof MarketTradeSignAccess access)) return;
            List<String> lines = Arrays.asList(access.rotclient$marketLines());
            String text;
            if (stage == Stage.SEARCH && MarketTradeMenuPolicy.searchSign(lines)) text = searchName;
            else if (stage == Stage.BZ_AMOUNT && MarketTradeMenuPolicy.numericSign(lines, "buy_amount")) text = Long.toString(active.quantity);
            else if (stage == Stage.BZ_PRICE && MarketTradeMenuPolicy.numericSign(lines, "price"))
                text = java.math.BigDecimal.valueOf(active.buyUnit).toPlainString();
            else return;
            access.rotclient$marketLines()[0] = text; lastAction = now;
            move(switch (stage) { case SEARCH -> Stage.FILTERS; case BZ_AMOUNT -> Stage.BZ_PRICE; default -> Stage.BZ_CONFIRM; }, now);
            access.rotclient$marketDone(); return;
        }
        var menu = menu(c);
        if (menu == null || !gate.canAct(menu, now)) return;
        if ("BZ".equals(active.market)) { bazaar(c, menu, now); return; }
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
                if (!stillEligible(now)) { skipCandidate("Offer changed before opening"); return; }
                c.player.closeContainer(); command(c, "viewauction " + active.auctionUuid); move(Stage.VIEW, now);
            }
            case VIEW -> {
                if (!menu.title().equals("BIN Auction View") || menu.size() != 54 || !liveItem(menu, 13)) return;
                int buy = menu.unique("Buy Item Right Now", "Buy Item");
                if (buy < 0 || !price(menu, active.buyUnit, "Buy it now", "Price")) return;
                if (!stillEligible(now)) { skipCandidate("Deal no longer qualifies"); return; }
                click(c, menu, buy, Stage.BUY, now);
            }
            case BUY -> {
                if (!menu.title().equals("Confirm Purchase") || menu.size() != 27 || !liveItem(menu, 13)
                        || !price(menu, active.buyUnit, "Price", "Buy it now")) return;
                int confirm = menu.unique("Confirm");
                if (confirm < 0) return;
                if (!stillEligible(now) || !uuidAbsent(c, active.itemUuid)) { skipCandidate("Purchase identity or budget changed"); return; }
                active.baselineClear = true;
                active.sourceUpdatedAt = MarketWatchDataService.currentAuctions().snapshot().lastUpdated();
                active.buySent = true; active.status = "PURCHASE SENT — awaiting receipt and ownership";
                if (!journal.save()) { stop("Could not persist purchase intent"); return; }
                click(c, menu, confirm, Stage.OWNED, now);
            }
            case OWNED -> {
                if (active.receiptObserved && !active.claimSent && menu.title().equals("BIN Auction View")
                        && menu.size() == 54 && liveItem(menu, 13)) {
                    int collect = menu.unique("Collect Auction");
                    if (collect >= 0 && menu.slot(collect).lore().stream().map(CommissionDisplayPolicy::normalizeLine)
                            .anyMatch(line -> line.equals("Click to pick up item!"))) {
                        active.claimSent = true; active.status = "COLLECTION SENT — awaiting exact owned item";
                        if (!journal.save()) { stop("Could not persist collection intent"); return; }
                        click(c, menu, collect, Stage.OWNED, now);
                    }
                }
            }
            default -> { }
        }
    }
    private static boolean uuidAbsent(Minecraft c, String uuid) {
        return inventory(c).stream().noneMatch(item -> uuid.equals(item.uuid()));
    }
    private static boolean purchaseOwned(Minecraft c) {
        if (!MarketTradePolicy.purchaseConfirmed(inventory(c), active)) return false;
        if ("BZ".equals(active.market)) return true;
        var inv = c.player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) if (matches(inv.getItem(slot))) return true;
        return false;
    }
    private static void completePurchase(Minecraft c, long now) {
        active.bought = true; active.confirmedAt = now;
        active.status = "BUY_DONE — server receipt and new owned item; sell manually";
        if (!journal.save()) { stop("Could not persist confirmed purchase"); return; }
        purchaseNotice = new PurchaseNotice(active.name, active.quantity, active.market, now, active.id);
        c.player.closeContainer();
        active = null; activePersisted = false; stage = Stage.IDLE; gate.reset();
        status = "Buy done — item retained; watching for the next selected deal";
    }
    private static void skipCandidate(String reason) {
        if (active != null && active.buySent) { stop(reason); return; }
        if (active != null) { active.status = "SKIPPED: " + reason; if (activePersisted && !journal.save()) { stop("Could not save skipped candidate"); return; } }
        active = null; activePersisted = false; stage = Stage.IDLE; gate.reset(); status = reason + "; watching for another deal";
    }
    private static void scan(Minecraft c, long now) {
        if (now < lastScan || now - lastScan < 1000) return;
        lastScan = now;
        if (journal.openPositions() >= journal.settings().maxOpenPositions) {
            status = "Holding limit reached; sell and review a purchase manually"; return;
        }
        long purse = purse();
        if (purse < 0) { status = "Waiting for exact live Purse"; return; }
        if (journal.settings().allowance(purse, journal.committedBudget()) <= 0) {
            status = "Purchase budget exhausted or protected Purse reserve reached"; return;
        }
        var snapshot = MarketWatchDataService.currentAuctions();
        if (journal.settings().auctionEnabled && auctionScan != null && auctionScan.valid()
                && MarketTradeCandidatePolicy.fresh(snapshot.observedAtMillis(), snapshot.snapshot().lastUpdated(), now, journal.settings())) {
            if (snapshot.observedAtMillis() != lastSnapshot) {
                lastSnapshot = snapshot.observedAtMillis(); scanCandidates = auctionScan.candidates(); candidateCursor = 0;
            }
            var targets = targets();
            // At most four bounded proposals per scan; continue from the same immutable API snapshot next time.
            int checked = 0;
            while (candidateCursor < scanCandidates.size() && checked++ < 4) {
                var a = scanCandidates.get(candidateCursor++);
                if (attempted.contains(a.uuid()) || journal.auctionAttempted(a.uuid()) || targets.stream().noneMatch(t -> t.matches(a))) continue;
                String key = MarketWatchVariantPolicy.comparableKey(a);
                var proposal = MarketTradeCandidatePolicy.propose(a, auctionScan.peers(key), targets,
                        journal.settings(), purse, journal.committedBudget(), owner, now, references.getOrDefault(key, 0L));
                if (proposal == null) continue;
                Long seenAt = referenceTimes.get(key);
                if (seenAt == null) {
                    references.put(key, proposal.sell() + journal.settings().undercutCoins);
                    referenceTimes.put(key, snapshot.snapshot().lastUpdated()); continue;
                }
                if (snapshot.snapshot().lastUpdated() <= seenAt || !uuidAbsent(c, a.variant().itemUuid())) continue;
                references.put(key, proposal.sell() + journal.settings().undercutCoins);
                active = newPosition("AH", now, snapshot.snapshot().lastUpdated());
                active.itemId = a.variant().itemId(); active.itemUuid = a.variant().itemUuid(); active.name = a.itemName();
                active.tier = a.tier(); active.reforge = a.variant().reforge(); active.auctionUuid = a.uuid();
                active.attributesKey = a.variant().attributesKey(); active.sellerUuid = a.auctioneerUuid();
                active.buyUnit = proposal.buy(); active.sellUnit = proposal.sell(); active.quantity = 1;
                active.reserved = proposal.reserved(); active.baselineClear = true;
                searchName = MarketWatchItemCatalog.displayName(active.itemId);
                if (searchName.isBlank() || searchName.length() > 60) { active = null; continue; }
                attempted.add(a.uuid()); countdown = new MarketTradePolicy.Countdown(now); move(Stage.COUNTDOWN, now); return;
            }
        }
        status = "Watching fresh API snapshots; no selected deal passes all checks";
        scanBazaar(c, now);
    }
    private static void updateAuctionIndex() {
        if (!journal.settings().auctionEnabled) return;
        var snapshot = MarketWatchDataService.currentAuctions().snapshot();
        if (indexedSnapshot != snapshot) {
            indexedSnapshot = snapshot; auctionScan = new MarketTradeScanPolicy(snapshot.auctions(), targets(), journal.settings(), owner, System.currentTimeMillis());
            scanCandidates = List.of(); candidateCursor = 0;
        }
        if (!auctionScan.complete()) auctionScan.step(MarketTradeScanPolicy.TICK_LISTINGS);
    }
    private static boolean waitingForAuctionIndex() {
        return active != null && "AH".equals(active.market) && auctionScan != null && !auctionScan.complete();
    }
    private static MarketTradeJournal.Position newPosition(String market, long now, long sourceUpdatedAt) {
        activePersisted = false;
        var p = new MarketTradeJournal.Position(); p.id = UUID.randomUUID().toString(); p.market = market; p.purchaseOnly = true;
        p.ownerUuid = owner; p.profile = profile; p.server = server; p.createdAt = now; p.sourceUpdatedAt = sourceUpdatedAt;
        return p;
    }
    private static boolean stillEligible(long now) {
        if (active == null || journal.openPositions() >= journal.settings().maxOpenPositions) return false;
        if ("BZ".equals(active.market)) {
            var data = MarketWatchDataService.currentBazaar();
            if (!MarketTradeCandidatePolicy.fresh(data.observedAtMillis(), data.snapshot().lastUpdated(), now, journal.settings())
                    || !bazaarTargets().contains(active.itemId) || !journal.settings().bazaarEnabled) return false;
            var p = MarketTradeBazaarPolicy.propose(data.snapshot().product(active.itemId), journal.settings(), purse(),
                    journal.committedBudget(), bazaarReferences.getOrDefault(active.itemId, 0D));
            return p != null && p.buy() == active.buyUnit && p.sell() >= active.sellUnit && p.quantity() >= active.quantity
                    && active.reserved <= journal.settings().allowance(purse(), journal.committedBudget());
        }
        if (!journal.settings().auctionEnabled) return false;
        var data = MarketWatchDataService.currentAuctions();
        if (!MarketTradeCandidatePolicy.fresh(data.observedAtMillis(), data.snapshot().lastUpdated(), now, journal.settings())) return false;
        if (auctionScan == null || !auctionScan.valid() || indexedSnapshot != data.snapshot()) return false;
        var a = auctionScan.auction(active.auctionUuid);
        if (a == null) return false;
        var p = MarketTradeCandidatePolicy.propose(a, auctionScan.peers(MarketWatchVariantPolicy.comparableKey(a)), targets(), journal.settings(),
                purse(), journal.committedBudget(), owner, now, references.getOrDefault(MarketWatchVariantPolicy.comparableKey(a), 0L));
        return p != null && p.buy() == active.buyUnit && p.sell() >= active.sellUnit
                && a.variant().itemUuid().equals(active.itemUuid) && a.variant().attributesKey().equals(active.attributesKey)
                && a.tier().equals(active.tier) && a.variant().reforge().equals(active.reforge)
                && MarketTradeCandidatePolicy.uuid(a.auctioneerUuid()).equals(MarketTradeCandidatePolicy.uuid(active.sellerUuid));
    }
    private static Set<String> bazaarTargets() {
        var result = new HashSet<String>();
        if (!journal().settings().bazaarEnabled) return result;
        for (var pin : MarketWatchPinnedDealStore.all()) if (journal().settings().pinIds.contains(pin.id())
                && pin.market() == MarketWatchOpportunity.Market.BAZAAR && !pin.itemId().isBlank()) result.add(pin.itemId());
        var selected = MarketWatchRuntime.bazaarWatches().stream()
                .filter(w -> w.enabled && journal().settings().watchIds.contains(w.id) && w.buyDealPercent <= 0).toList();
        for (var match : MarketWatchEvaluator.evaluateBazaar(selected, MarketWatchDataService.currentBazaar().snapshot())) result.add(match.productId());
        return result;
    }
    private static void scanBazaar(Minecraft c, long now) {
        if (!journal.settings().bazaarEnabled) return;
        var data = MarketWatchDataService.currentBazaar();
        if (data.observedAtMillis() == lastBazaarSnapshot || !MarketTradeCandidatePolicy.fresh(data.observedAtMillis(),
                data.snapshot().lastUpdated(), now, journal.settings())) return;
        lastBazaarSnapshot = data.observedAtMillis();
        for (String id : bazaarTargets()) {
            String attemptKey = "BZ:" + id + ":" + data.snapshot().lastUpdated();
            if (attempted.contains(attemptKey) || journal.bazaarSnapshotAttempted(id, data.snapshot().lastUpdated())) continue;
            var proposal = MarketTradeBazaarPolicy.propose(data.snapshot().product(id), journal.settings(), purse(), journal.committedBudget(),
                    bazaarReferences.getOrDefault(id, 0D));
            if (proposal == null) continue;
            String key = "BZ:" + id;
            Long seenAt = referenceTimes.get(key);
            if (seenAt == null) {
                bazaarReferences.put(id, proposal.sell() + .1); referenceTimes.put(key, data.snapshot().lastUpdated()); continue;
            }
            if (data.snapshot().lastUpdated() <= seenAt) continue;
            referenceTimes.put(key, data.snapshot().lastUpdated());
            active = newPosition("BZ", now, data.snapshot().lastUpdated()); active.itemId = id;
            active.name = MarketWatchItemCatalog.displayName(id); searchName = active.name;
            if (searchName.isBlank() || searchName.length() > 60) { active = null; continue; }
            active.buyUnit = proposal.buy(); active.sellUnit = proposal.sell(); active.quantity = proposal.quantity(); active.reserved = proposal.reserved();
            active.baselineQuantity = count(c, id); bazaarPrechecked = false;
            bazaarReferences.put(id, data.snapshot().product(id).quickBuyPrice()); attempted.add(attemptKey);
            countdown = new MarketTradePolicy.Countdown(now); move(Stage.COUNTDOWN, now); return;
        }
    }
    private static long count(Minecraft c, String id) {
        return inventory(c).stream().filter(i -> id.equals(i.id())).mapToLong(MarketTradeMenuPolicy.Item::count).sum();
    }
    private static boolean bazaarOrders(MarketTradeMenuPolicy.Menu m) { return m.title().equals("Your Bazaar Orders"); }
    private static boolean bazaarProduct(MarketTradeMenuPolicy.Menu m) {
        return m.title().contains(" ➜ ") && m.slot(13) != null && active.itemId.equals(m.slot(13).id());
    }
    private static void bazaar(Minecraft c, MarketTradeMenuPolicy.Menu m, long now) {
        switch (stage) {
            case BZ_PRECHECK -> {
                if (!bazaarOrders(m)) { click(c, m, m.unique("Manage Orders"), Stage.BZ_PRECHECK, now); return; }
                if (m.unique("Next Page") >= 0 || m.items().stream().anyMatch(i -> active.itemId.equals(i.id())
                        && (i.name().startsWith("BUY ") || i.name().startsWith("SELL ")))) {
                    stop("Existing or paginated Bazaar orders: review manually"); return;
                }
                bazaarPrechecked = true; c.player.closeContainer(); command(c, "bz " + searchName); move(Stage.BZ_PRODUCT, now);
            }
            case BZ_PRODUCT -> {
                if (!bazaarProduct(m)) {
                    var matches = m.items().stream().filter(i -> active.itemId.equals(i.id())).toList();
                    if (matches.size() == 1) click(c, m, matches.getFirst().slot(), stage, now); return;
                }
                click(c, m, m.unique("Create Buy Order"), Stage.BZ_AMOUNT, now);
            }
            case BZ_AMOUNT -> click(c, m, m.unique("Custom Amount"), stage, now);
            case BZ_PRICE -> click(c, m, m.unique("Custom Price"), stage, now);
            case BZ_CONFIRM -> {
                if (!m.title().equals("Confirm Buy Order")) return;
                int confirm = m.unique("Confirm", "Confirm Buy Order");
                if (confirm < 0 || !m.items().stream().anyMatch(i -> active.itemId.equals(i.id()))
                        || !MarketTradeBazaarPolicy.amount(m.items().stream().flatMap(i -> i.lore().stream()).toList(), active.quantity)
                        || !price(m, active.buyUnit, "Price per unit")) return;
                if (!bazaarPrechecked || !stillEligible(now) || count(c, active.itemId) != active.baselineQuantity) {
                    skipCandidate("Bazaar target, budget or inventory changed"); return;
                }
                active.sourceUpdatedAt = MarketWatchDataService.currentBazaar().snapshot().lastUpdated();
                active.buySent = true; active.status = "BUY ORDER SENT — receipt pending";
                if (!journal.save()) { stop("Could not persist Bazaar purchase intent"); return; }
                click(c, m, confirm, Stage.BZ_SETUP, now);
            }
            case BZ_WAIT -> {
                if (!bazaarOrders(m)) { click(c, m, m.unique("Manage Orders"), stage, now); return; }
                if (m.unique("Next Page") >= 0) { stop("Paginated Bazaar orders: review manually"); return; }
                var matches = m.items().stream().filter(i -> active.itemId.equals(i.id())
                        && MarketTradePolicy.bazaarOrder(i, active, false, false)
                        && MarketTradeBazaarPolicy.owner(i.lore(), c.player.getGameProfile().name())).toList();
                if (matches.size() > 1) { stop("Ambiguous Bazaar order identity"); return; }
                if (matches.size() == 1 && MarketTradePolicy.bazaarOrder(matches.getFirst(), active, false, true)) {
                    if (count(c, active.itemId) != active.baselineQuantity) { stop("Inventory changed before Bazaar claim"); return; }
                    if (!active.claimSent && matches.getFirst().lore().stream().map(CommissionDisplayPolicy::normalizeLine)
                            .anyMatch(line -> line.equals("Click to claim!") || line.equals("Click to claim items!"))) {
                        active.fillObserved = true; active.claimSent = true; active.status = "CLAIM SENT — awaiting exact inventory delta";
                        if (!journal.save()) { stop("Could not persist Bazaar fill and claim intent"); return; }
                        click(c, m, matches.getFirst().slot(), Stage.BZ_CLAIM, now);
                    }
                } else if (now - lastAction >= 5000) click(c, m, m.unique("Refresh"), stage, now);
            }
            default -> { }
        }
    }
    static void onChat(Component message) {
        if (!enabled || active == null || !active.buySent) return;
        String line = message == null ? "" : CommissionDisplayPolicy.normalizeLine(message.getString());
        if (line.startsWith("There was an error with the auction house!") || line.startsWith("Escrow refunded ")) {
            stop("Escrow failure; review the sent purchase before retrying"); return;
        }
        var c = Minecraft.getInstance();
        if (c.player == null || c.level != world || c.getConnection() != connection
                || !owner.equals(MarketTradeCandidatePolicy.uuid(c.player.getUUID().toString()))
                || !server.equals(server(c)) || !profile.equals(profile(c))) { stop("Account/profile context changed"); return; }
        if ("BZ".equals(active.market)) {
            if (stage == Stage.BZ_SETUP && MarketTradePolicy.bazaarSetupReceipt(line, active, false)) {
                active.receiptObserved = true; active.status = "BUY ORDER CREATED — full owned fill pending";
                if (!journal.save()) { stop("Could not save Bazaar order receipt"); return; }
                c.player.closeContainer(); command(c, "bz"); move(Stage.BZ_WAIT, System.currentTimeMillis());
            }
        } else if (stage == Stage.OWNED && !active.receiptObserved && MarketTradePolicy.purchaseReceipt(line, active)) {
            active.receiptObserved = true; active.status = "PURCHASE RECEIPT — awaiting exact new owned item";
            if (!journal.save()) stop("Could not persist purchase receipt");
        }
    }
    private static void move(Stage next, long now) {
        if (stage != next) { stage = next; entered = now; }
        if (next != Stage.COUNTDOWN) status = "Buying: " + next;
    }
    private static void command(Minecraft c, String command) { c.getConnection().sendCommand(command); gate.reset(); }
    private static void click(Minecraft c, MarketTradeMenuPolicy.Menu menu, int slot, Stage next, long now) {
        if (slot < 0 || c.gameMode == null || slot >= c.player.containerMenu.slots.size()) return;
        gate.sent(menu, now); lastAction = now;
        move(next, now); c.gameMode.handleContainerInput(menu.containerId(), slot, 0, ContainerInput.PICKUP, c.player);
    }
    private static boolean root(MarketTradeMenuPolicy.Menu m) {
        return m.size() == 54 && (m.title().equals("Auction House") || m.title().equals("Co-op Auction House"));
    }
    private static boolean browser(MarketTradeMenuPolicy.Menu m) {
        return m.size() == 54 && (m.title().equals("Auctions Browser") || m.title().startsWith("Auctions: \""));
    }
    private static boolean named(MarketTradeMenuPolicy.Menu m, int slot, String... names) {
        return m.slot(slot) != null && Arrays.stream(names).anyMatch(n -> n.equalsIgnoreCase(m.slot(slot).name()));
    }
    private static boolean price(MarketTradeMenuPolicy.Menu m, double expected, String... labels) {
        return MarketTradeMenuPolicy.price(m, expected, labels);
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
    private MarketTradeRuntime() {}
}
