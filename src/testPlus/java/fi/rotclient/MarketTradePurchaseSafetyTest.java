package fi.rotclient;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class MarketTradePurchaseSafetyTest {
    @TempDir Path directory;
    private static MarketTradeJournal.Position position(String market) {
        var p = new MarketTradeJournal.Position(); p.id = UUID.randomUUID().toString(); p.market = market;
        p.itemId = "TEST"; p.name = "Test"; p.itemUuid = "AH".equals(market) ? "item" : "";
        p.auctionUuid = "auction"; p.tier = "EPIC"; p.reforge = "spicy";
        p.quantity = 1; p.buyUnit = 100_000; p.sellUnit = 200_000; p.reserved = 100_000;
        p.purchaseOnly = true; p.buySent = true; p.baselineClear = true; p.sourceUpdatedAt = 1000;
        return p;
    }
    private static MarketTradeMenuPolicy.Item item(String uuid, int count) {
        return new MarketTradeMenuPolicy.Item(0, "Test", "TEST", uuid, "EPIC", "spicy", count, List.of());
    }
    @Test void inventoryAloneOrReceiptAloneNeverCompletesAnAuctionBuy() {
        var p = position("AH"); var owned = List.of(item("item", 1));
        assertFalse(MarketTradePolicy.purchaseConfirmed(owned, p));
        p.receiptObserved = true;
        assertFalse(MarketTradePolicy.purchaseConfirmed(List.of(), p));
        assertTrue(MarketTradePolicy.purchaseConfirmed(owned, p));
        p.baselineClear = false; assertFalse(MarketTradePolicy.purchaseConfirmed(owned, p));
    }
    @Test void duplicateUuidWrongQuantityOrAlreadyReviewedOutcomeCannotCreditABuy() {
        var p = position("AH"); p.receiptObserved = true;
        assertFalse(MarketTradePolicy.purchaseConfirmed(List.of(item("item", 1), item("item", 1)), p));
        assertFalse(MarketTradePolicy.purchaseConfirmed(List.of(item("item", 2)), p));
        assertFalse(MarketTradePolicy.purchaseConfirmed(List.of(item("other", 1)), p));
        p.manuallyReviewed = true; assertFalse(MarketTradePolicy.purchaseConfirmed(List.of(item("item", 1)), p));
    }
    @Test void bazaarRequiresSavedReceiptOwnedFullFillClaimAndExactNewQuantity() {
        var p = position("BZ"); p.quantity = 10; p.baselineQuantity = 3;
        assertFalse(MarketTradePolicy.purchaseConfirmed(List.of(item("", 13)), p));
        p.receiptObserved = true; p.fillObserved = true;
        assertFalse(MarketTradePolicy.purchaseConfirmed(List.of(item("", 13)), p));
        p.claimSent = true;
        assertTrue(MarketTradePolicy.purchaseConfirmed(List.of(item("", 13)), p));
        assertFalse(MarketTradePolicy.purchaseConfirmed(List.of(item("", 12)), p));
        assertFalse(MarketTradePolicy.purchaseConfirmed(List.of(item("", 14)), p));
    }
    @Test void claimedBuySurvivesRestartWithoutForcingSaleOrClaimingProfit() {
        var path = directory.resolve("journal.json"); var j = new MarketTradeJournal(path); var p = position("AH");
        p.bought = p.receiptObserved = true; p.confirmedAt = 2000; p.status = "BUY_DONE";
        assertTrue(j.add(p)); var restored = new MarketTradeJournal(path);
        assertTrue(restored.healthy()); assertTrue(restored.positions().getFirst().completedPurchase());
        assertFalse(restored.positions().getFirst().unresolved()); assertEquals(1, restored.openPositions());
        assertEquals(100_000, restored.committedBudget()); assertEquals(0, restored.realizedGrossProfit());
    }
    @Test void manualReviewReleasesHoldingButKeepsBudgetAndDurableAuctionDuplicateGuard() {
        var path = directory.resolve("journal.json"); var j = new MarketTradeJournal(path); var p = position("AH");
        assertTrue(j.add(p)); assertTrue(j.positions().getFirst().unresolved());
        assertTrue(j.markReviewed(p.id)); assertFalse(j.markReviewed(p.id));
        var restored = new MarketTradeJournal(path); assertTrue(restored.healthy());
        assertFalse(restored.positions().getFirst().unresolved()); assertEquals(0, restored.openPositions());
        assertEquals(100_000, restored.committedBudget()); assertTrue(restored.auctionAttempted("auction"));
        assertFalse(restored.positions().getFirst().bought); assertEquals(0, restored.realizedGrossProfit());
    }
    @Test void bazaarCanBuyAgainOnlyAfterConfirmedOutcomeAndNewerApiSnapshot() {
        var j = new MarketTradeJournal(directory.resolve("journal.json")); var p = position("BZ");
        assertTrue(j.add(p)); assertTrue(j.bazaarSnapshotAttempted("TEST", 1001));
        p.receiptObserved = p.fillObserved = p.claimSent = p.bought = true; p.confirmedAt = 2000;
        assertTrue(j.save()); assertTrue(j.bazaarSnapshotAttempted("TEST", 999));
        assertTrue(j.bazaarSnapshotAttempted("TEST", 1000)); assertFalse(j.bazaarSnapshotAttempted("TEST", 1001));
        assertEquals(100_000, j.committedBudget());
    }
    @Test void oldPendingListingNeedsReviewAndCannotResumeASaleOnMigration() throws Exception {
        var path = directory.resolve("journal.json"); var p = position("AH"); p.purchaseOnly = false;
        p.bought = p.listed = true; p.status = "LISTING SENT";
        String old = "{\"schemaVersion\":1,\"settings\":{\"schemaVersion\":1},\"positions\":["
                + MarketTradeJournal.JSON.toJson(p) + "]}";
        Files.writeString(path, old); var j = new MarketTradeJournal(path);
        assertTrue(j.healthy()); assertTrue(j.positions().getFirst().manualReview);
        assertTrue(j.positions().getFirst().unresolved()); assertTrue(j.positions().getFirst().listed);
        assertFalse(j.positions().getFirst().sold); assertEquals(old, Files.readString(path));
    }
    @Test void corruptBoughtFlagWithoutReceiptIsPreservedAndFailsClosed() throws Exception {
        var path = directory.resolve("journal.json"); var p = position("AH"); p.bought = true; p.confirmedAt = 2000;
        String corrupt = "{\"schemaVersion\":2,\"settings\":{},\"positions\":[" + MarketTradeJournal.JSON.toJson(p) + "]}";
        Files.writeString(path, corrupt); var j = new MarketTradeJournal(path);
        assertFalse(j.healthy()); assertFalse(j.save()); assertEquals(corrupt, Files.readString(path));
    }
    @Test void contradictoryOrRoundedBazaarFillAndOwnerEvidenceCannotClaim() {
        var p = position("BZ"); p.name = "Coal"; p.quantity = 64; p.buyUnit = 100.1;
        var exact = new MarketTradeMenuPolicy.Item(10, "BUY Coal", "COAL", "", "", "", 64,
                List.of("Order amount: 64x", "Price per unit: 100.1 coins", "Filled: 64/64 (100%)", "By: Owner"));
        assertTrue(MarketTradePolicy.bazaarOrder(exact, p, false, true));
        var wrong = new MarketTradeMenuPolicy.Item(10, "BUY Coal", "COAL", "", "", "", 64,
                List.of("Order amount: 64x", "Price per unit: 100.1 coins", "Filled: 1/64 (100%)"));
        assertFalse(MarketTradePolicy.bazaarOrder(wrong, p, false, true));
        var rounded = new MarketTradeMenuPolicy.Item(10, "BUY Coal", "COAL", "", "", "", 64,
                List.of("Order amount: 64x", "Price per unit: 100.1 coins", "Filled: 0.064k/0.064k (100%)"));
        assertFalse(MarketTradePolicy.bazaarOrder(rounded, p, false, true));
        assertFalse(MarketTradeBazaarPolicy.owner(List.of("By: Owner", "By: Other"), "Owner"));
    }
    @Test void readingAllowanceDoesNotNormalizeOrMutateDraftSelections() {
        var s = new MarketTradeSettings(); s.pinIds = null; s.watchIds = null; s.pursePercent = Double.NaN;
        assertEquals(0, s.allowance(1_000_000, 0)); assertNull(s.pinIds); assertNull(s.watchIds);
        s.normalize(); assertNotNull(s.pinIds); assertTrue(s.pinIds.isEmpty()); assertTrue(s.watchIds.isEmpty());
        assertEquals(0, s.sessionBudgetCoins); assertEquals(0, s.perTradeCoins);
    }
    @Test void contradictoryMenuPriceOrQuantityCannotHideBehindAnotherCorrectButton() {
        var bad = new MarketTradeMenuPolicy.Item(11, "Confirm", "", "", "", "", 1,
                List.of("Price: 100,000 coins", "Price: 200,000 coins"));
        var good = new MarketTradeMenuPolicy.Item(13, "Test", "TEST", "item", "EPIC", "spicy", 1,
                List.of("Price: 100,000 coins"));
        var menu = new MarketTradeMenuPolicy.Menu(new Object(), 1, 1, "Confirm Purchase", 27, List.of(bad, good));
        assertFalse(MarketTradeMenuPolicy.price(menu, 100_000, "Price"));
        assertTrue(MarketTradeMenuPolicy.coins(List.of("Price: 100,000 coins", "Price: 100k coins"), "Price").isEmpty());
        assertFalse(MarketTradeBazaarPolicy.amount(List.of("Order amount: 64x", "Order amount: 32x"), 64));
    }
}
