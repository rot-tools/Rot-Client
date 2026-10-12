package fi.rotclient;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MarketTradeScanPolicyTest {
    private static String uuid(int n) { return String.format(Locale.ROOT, "%032x", n); }
    private static MarketWatchAuction auction(int n, String id, long price) {
        return new MarketWatchAuction(uuid(n), "Test", "weapon", "EPIC", 1, 100_000, price, 0, true, "", uuid(n + 30_000),
                new MarketWatchItemVariant(id, "spicy", uuid(n + 60_000), 1, true, "fingerprint"));
    }
    private static List<MarketTradeCandidatePolicy.Target> targets() {
        return List.of(new MarketTradeCandidatePolicy.Target("TEST", "Test", "EPIC", "spicy", 0, 0, 0, 0));
    }
    @Test void incompleteIndexCannotAuthorizeAPurchaseAndEachTickIsBounded() {
        var source = new ArrayList<MarketWatchAuction>();
        for (int n = 1; n <= 600; n++) source.add(auction(n, "TEST", 1000 + n));
        var index = new MarketTradeScanPolicy(source, targets());
        assertEquals(512, index.step(Integer.MAX_VALUE)); assertFalse(index.complete()); assertFalse(index.valid());
        assertTrue(index.candidates().isEmpty()); assertNull(index.auction(uuid(1)));
        assertEquals(88, index.step(Integer.MAX_VALUE)); assertTrue(index.valid());
        assertEquals(600, index.candidates().size()); assertEquals(1001, index.candidates().getFirst().startingBid());
        assertEquals(128, index.peers(MarketWatchVariantPolicy.comparableKey(source.getFirst())).size());
    }
    @Test void unselectedItemsNeverEnterThePurchaseIndex() {
        var selected = auction(1, "TEST", 1000); var unselected = auction(2, "OTHER", 1);
        var index = new MarketTradeScanPolicy(List.of(unselected, selected), targets()); index.step(512);
        assertTrue(index.valid()); assertEquals(List.of(selected), index.candidates()); assertNull(index.auction(uuid(2)));
    }
    @Test void duplicateListingIdentityFailsClosedInsteadOfInflatingReferences() {
        var a = auction(1, "TEST", 1000); var index = new MarketTradeScanPolicy(List.of(a, a), targets());
        index.step(512); assertTrue(index.complete()); assertFalse(index.valid()); assertTrue(index.candidates().isEmpty());
    }
    @Test void overlyBroadSelectionFailsClosedWithoutAnUnboundedIndex() {
        var source = new ArrayList<MarketWatchAuction>();
        for (int n = 1; n <= MarketTradeScanPolicy.MAX_MATCHES + 1; n++) source.add(auction(n, "TEST", n));
        var index = new MarketTradeScanPolicy(source, targets());
        while (!index.complete()) assertTrue(index.step(512) <= 512);
        assertFalse(index.valid()); assertTrue(index.candidates().isEmpty());
    }
    @Test void cheapStacksCannotHideTheNextUniqueEligiblePurchase() {
        var source = new ArrayList<MarketWatchAuction>();
        for (int n = 1; n <= 128; n++) {
            var a = auction(n, "TEST", 1);
            source.add(new MarketWatchAuction(a.uuid(), a.itemName(), a.category(), a.tier(), a.startMillis(), a.endMillis(),
                    a.startingBid(), 0, true, "", a.auctioneerUuid(), new MarketWatchItemVariant("TEST", "spicy", uuid(n + 60_000), 64, true, "fingerprint")));
        }
        var unique = auction(129, "TEST", 100_000); source.add(unique);
        for (int n = 130; n <= 135; n++) source.add(auction(n, "TEST", 200_000 + n));
        var index = new MarketTradeScanPolicy(source, targets()); index.step(512);
        assertTrue(index.valid()); assertEquals(unique, index.candidates().getFirst()); assertEquals(7, index.candidates().size());
        assertEquals(7, index.peers(MarketWatchVariantPolicy.comparableKey(unique)).size());
        var settings = new MarketTradeSettings(); settings.sessionBudgetCoins = 1_000_000; settings.perTradeCoins = 500_000;
        settings.pursePercent = 100; settings.minProfitCoins = 10_000; settings.maxListingFeeCoins = 0;
        assertNotNull(MarketTradeCandidatePolicy.propose(unique, index.peers(MarketWatchVariantPolicy.comparableKey(unique)), targets(),
                settings, 1_000_000, 0, uuid(999), 1, 0));
    }
    @Test void everySelectedVariantGetsAChanceBeforeRepeatingOneCheapVariant() {
        var source = new ArrayList<MarketWatchAuction>();
        for (int n = 1; n <= 100; n++) source.add(auction(n, "TEST", n));
        var other = auction(101, "OTHER", 100_000); source.add(other);
        var selected = new ArrayList<>(targets());
        selected.add(new MarketTradeCandidatePolicy.Target("OTHER", "Other", "EPIC", "spicy", 0, 0, 0, 0));
        var index = new MarketTradeScanPolicy(source, selected); index.step(512);
        assertEquals(source.getFirst(), index.candidates().getFirst()); assertEquals(other, index.candidates().get(1));
    }
}
