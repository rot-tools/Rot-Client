package fi.rotclient;

import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class MarketTradePolicyTest {
    @TempDir Path directory;
    private static MarketTradeSettings settings() {
        var s = new MarketTradeSettings(); s.sessionBudgetCoins = 1000000; s.perTradeCoins = 500000;
        s.reserveCoins = 100000; s.pursePercent = 20; return s;
    }
    private static MarketTradeJournal.Position position() {
        var p = new MarketTradeJournal.Position(); p.id = "id"; p.market = "AH"; p.itemId = "TEST";
        p.itemUuid = "item"; p.auctionUuid = "auction"; p.name = "Test"; p.tier = "EPIC"; p.reforge = "spicy";
        p.buyUnit = 100000; p.sellUnit = 200000; p.quantity = 1; p.reserved = 110000; return p;
    }
    private static List<MarketTradeRiskPolicy.Peer> peers(long... prices) {
        var result = new ArrayList<MarketTradeRiskPolicy.Peer>();
        for (int i = 0; i < prices.length; i++) result.add(new MarketTradeRiskPolicy.Peer(prices[i], "seller" + i)); return result;
    }
    @Test void zeroBudgetOrUnknownPurseNeverSpends() {
        assertEquals(0, new MarketTradeSettings().allowance(10000000, 0));
        assertEquals(0, settings().allowance(-1, 0)); assertEquals(0, settings().allowance(99999, 0));
        assertEquals(0, settings().allowance(10000000, 1000001));
    }
    @Test void liveFractionReservePerTradeAndRecordedBudgetAllApply() {
        var s = settings(); assertEquals(180000, s.allowance(1000000, 0));
        assertEquals(100000, s.allowance(600000, 0)); assertEquals(50000, s.allowance(1000000, 950000));
        s.pursePercent = 100; assertEquals(500000, s.allowance(1000000, 0));
        s.pursePercent = Double.NaN; assertEquals(0, s.allowance(1000000, 0));
    }
    @Test void purseRequiresExactUnambiguousScoreboardEvidence() {
        assertEquals(12345, MarketTradeCandidatePolicy.purse(List.of("Purse: 12,345.9")).orElseThrow());
        assertTrue(MarketTradeCandidatePolicy.purse(List.of("Purse: 1.2M", "some Purse: 50")).isEmpty());
        assertTrue(MarketTradeCandidatePolicy.purse(List.of("Purse: 50", "Piggy: 60")).isEmpty());
        assertTrue(MarketTradeCandidatePolicy.purse(List.of("Purse: 9999999999999999999999999999")).isEmpty());
    }
    @Test void concentratedSparseWideAndJumpingReferencesAreRejected() {
        var s = settings(); assertFalse(MarketTradeRiskPolicy.assess(peers(100, 101), s, 0).eligible());
        assertFalse(MarketTradeRiskPolicy.assess(List.of(new MarketTradeRiskPolicy.Peer(100,"one"),
                new MarketTradeRiskPolicy.Peer(101,"one"), new MarketTradeRiskPolicy.Peer(102,"one"),
                new MarketTradeRiskPolicy.Peer(103,"one"), new MarketTradeRiskPolicy.Peer(104,"two")), s, 0).eligible());
        assertFalse(MarketTradeRiskPolicy.assess(peers(100,101,200,201,202), s, 0).eligible());
        assertFalse(MarketTradeRiskPolicy.assess(peers(100,101,102,103,104), s, 50).eligible());
        var good = MarketTradeRiskPolicy.assess(peers(100,101,102,103,104), s, 100);
        assertTrue(good.eligible()); assertEquals(100, good.reference());
    }
    @Test void countdownWaitsFourSecondsAndRejectsClockReversal() {
        var timer = new MarketTradePolicy.Countdown(1000); assertEquals(1, timer.remaining(4999));
        assertEquals(0, timer.remaining(5000)); assertEquals(-1, timer.remaining(4999));
    }
    @Test void clicksWaitForNewAcknowledgementAndMinimumCadence() {
        var gate = new MarketTradePolicy.Gate(); Object id = new Object();
        var first = new MarketTradeMenuPolicy.Menu(id,1,1,"Menu",54,List.of());
        assertTrue(gate.canAct(first,1000)); gate.sent(first,1000); assertFalse(gate.canAct(first,2000));
        var next = new MarketTradeMenuPolicy.Menu(id,1,2,"Menu",54,List.of());
        assertFalse(gate.canAct(next,1299)); assertTrue(gate.canAct(next,1300)); assertFalse(gate.canAct(next,999));
    }
    @Test void menuActionsExcludeInventoryAndAmbiguousButtons() {
        var item = new MarketTradeMenuPolicy.Item(11,"Confirm","","","","",1,List.of());
        var m = new MarketTradeMenuPolicy.Menu(new Object(),1,0,"Confirm Purchase",27,List.of(item,
                new MarketTradeMenuPolicy.Item(30,"Confirm","","","","",1,List.of())));
        assertEquals(11, m.unique("Confirm"));
        assertEquals(-1,new MarketTradeMenuPolicy.Menu(new Object(),1,0,"Confirm Purchase",27,List.of(item,
                new MarketTradeMenuPolicy.Item(12,"Confirm","","","","",1,List.of()))).unique("Confirm"));
    }
    @Test void liveVariantIncludesUuidRarityModifierAndQuantity() {
        var item = new MarketTradeMenuPolicy.Item(13,"Test","TEST","item","EPIC","spicy",1,List.of());
        assertTrue(MarketTradeMenuPolicy.variant(item,"TEST","item","EPIC","spicy",1));
        assertFalse(MarketTradeMenuPolicy.variant(item,"TEST","other","EPIC","spicy",1));
        assertFalse(MarketTradeMenuPolicy.variant(item,"TEST","item","LEGENDARY","spicy",1));
        assertFalse(MarketTradeMenuPolicy.variant(item,"TEST","item","EPIC","fabled",1));
        assertFalse(MarketTradeMenuPolicy.variant(item,"TEST","item","EPIC","spicy",2));
    }
    @Test void priceProofRefusesRoundedContradictoryOrInfiniteNumbers() {
        assertEquals(1234.5,MarketTradeMenuPolicy.coins(List.of("Price: 1,234.5 coins"),"Price").orElseThrow());
        assertTrue(MarketTradeMenuPolicy.coins(List.of("Price: 1.2M coins"),"Price").isEmpty());
        assertTrue(MarketTradeMenuPolicy.coins(List.of("Price: 10 coins","Price: 11 coins"),"Price").isEmpty());
        assertFalse(MarketTradeMenuPolicy.equal(Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY));
    }
    @Test void signAutofillNeedsTheExpectedInputGrammar() {
        assertTrue(MarketTradeMenuPolicy.searchSign(List.of("","","","Enter query")));
        assertTrue(MarketTradeMenuPolicy.searchSign(List.of("","^^^^","Enter query","")));
        assertFalse(MarketTradeMenuPolicy.searchSign(List.of("","","","to order")));
        assertTrue(MarketTradeMenuPolicy.numericSign(List.of("","^^^^","Enter amount","to order"),"buy_amount"));
        assertFalse(MarketTradeMenuPolicy.numericSign(List.of("","^^^^","Enter amount","to sell"),"buy_amount"));
    }
    @Test void futureJournalIsPreservedAndFailsClosed() throws Exception {
        var path = directory.resolve("journal.json"); String future = "{\"schemaVersion\":999}";
        Files.writeString(path,future); var j = new MarketTradeJournal(path);
        assertFalse(j.healthy()); assertFalse(j.setSettings(settings())); assertFalse(j.add(position()));
        assertEquals(future,Files.readString(path));
    }
    @Test void uncertainIntentSurvivesRestartAndUnsoldForecastIsNotProfit() {
        var path = directory.resolve("journal.json"); var j = new MarketTradeJournal(path); var p = position();
        assertTrue(j.add(p)); assertEquals(0,j.committedBudget());
        p.buySent = true; assertTrue(j.save()); var reload = new MarketTradeJournal(path);
        assertTrue(reload.healthy()); assertTrue(reload.positions().getFirst().unresolved());
        assertEquals(110000,reload.committedBudget()); assertEquals(0,reload.realizedGrossProfit());
    }
    @Test void observedSaleGrossIsCountedOnceAndListingFeesAreDeducted() {
        var j = new MarketTradeJournal(directory.resolve("journal.json")); var p = position();
        p.buySent = p.bought = p.listed = p.sold = p.listingFeeKnown = true; p.soldGross = 200000; p.listingFee = 1000;
        assertTrue(j.add(p)); assertFalse(j.add(p)); assertEquals(99000,j.realizedGrossProfit());
        assertEquals(0,j.openPositions());
    }
    @Test void writeFailureBlocksFurtherMutation() throws Exception {
        var parent = directory.resolve("file"); Files.writeString(parent,"occupied");
        var j = new MarketTradeJournal(parent.resolve("journal.json")); assertFalse(j.add(position()));
        assertFalse(j.healthy()); assertFalse(j.save());
    }
    @Test void anchoredReceiptsCannotCreditAnotherPurchase() {
        var p = position(); assertTrue(MarketTradePolicy.purchaseReceipt("[Auction] You purchased Test for 100,000 coins!",p));
        assertFalse(MarketTradePolicy.purchaseReceipt("[Auction] You purchased Test for 90,000 coins!",p));
        assertFalse(MarketTradePolicy.purchaseReceipt("Party > [Auction] You purchased Test for 100,000 coins!",p));
        assertFalse(MarketTradePolicy.purchaseReceipt("[Auction] You purchased Test for 100,000 coins! extra",p));
    }
    @Test void staleServerDataRemainsStaleEvenAfterANewPoll() {
        var s = settings(); assertFalse(MarketTradeCandidatePolicy.fresh(200000,100000,200001,s));
        assertFalse(MarketTradeCandidatePolicy.fresh(200000,200000,199999,s));
        assertTrue(MarketTradeCandidatePolicy.fresh(200000,200000,200001,s));
    }
}
