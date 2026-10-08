package fi.rotclient;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MarketTradeCandidatePolicyTest {
    private static String uuid(int n) { return String.format(Locale.ROOT,"%032x",n); }
    private static MarketWatchAuction auction(int n, long price, String tier, String attributes) {
        return new MarketWatchAuction(uuid(n),"Spicy Test Sword","weapon",tier,1,1000000,price,0,true,"",uuid(n+100),
                new MarketWatchItemVariant("TEST_SWORD","spicy",uuid(n+200),1,true,attributes));
    }
    private static MarketTradeSettings settings() {
        var s = new MarketTradeSettings(); s.sessionBudgetCoins = 10000000; s.perTradeCoins = 5000000;
        s.pursePercent = 100; s.reserveCoins = 100000; s.minProfitCoins = 10000; s.maxListingFeeCoins = 10000; return s;
    }
    private static List<MarketWatchAuction> peers() {
        var result = new ArrayList<MarketWatchAuction>();
        for (int i=2;i<8;i++) result.add(auction(i,200000+i*1000,"EPIC","fingerprint")); return result;
    }
    private static List<MarketTradeCandidatePolicy.Target> targets() {
        return List.of(new MarketTradeCandidatePolicy.Target("TEST_SWORD","Test Sword","EPIC","spicy",150000,10000,5,5));
    }
    @Test void onlyAnExplicitMatchingTargetCanAuthorizeTheSameVariant() {
        var a = auction(1,100000,"EPIC","fingerprint");
        var p = MarketTradeCandidatePolicy.propose(a,peers(),targets(),settings(),1000000,0,uuid(999),100,0);
        assertNotNull(p); assertEquals(100000,p.buy()); assertEquals(201999,p.sell()); assertEquals(110000,p.reserved());
        assertNull(MarketTradeCandidatePolicy.propose(a,peers(),List.of(),settings(),1000000,0,uuid(999),100,0));
    }
    @Test void mixedRarityOrUpgradesCannotInflateAReference() {
        var a = auction(1,100000,"EPIC","fingerprint");
        var mixed = new ArrayList<MarketWatchAuction>();
        for (int i=2;i<8;i++) mixed.add(auction(i,500000,"LEGENDARY","other upgrades"));
        assertNull(MarketTradeCandidatePolicy.propose(a,mixed,targets(),settings(),1000000,0,uuid(999),100,0));
    }
    @Test void fundsOwnAuctionsAndTargetPriceConstraintsAreRechecked() {
        var a = auction(1,100000,"EPIC","fingerprint");
        assertNull(MarketTradeCandidatePolicy.propose(a,peers(),targets(),settings(),150000,0,uuid(999),100,0));
        assertNull(MarketTradeCandidatePolicy.propose(a,peers(),targets(),settings(),1000000,0,uuid(101),100,0));
        var tight = List.of(new MarketTradeCandidatePolicy.Target("TEST_SWORD","Test Sword","EPIC","spicy",99999,0,0,0));
        assertNull(MarketTradeCandidatePolicy.propose(a,peers(),tight,settings(),1000000,0,uuid(999),100,0));
    }
    @Test void guessedIdentityAndInvalidAuctionUuidCannotTrade() {
        var a = new MarketWatchAuction("bad","Test","weapon","EPIC",1,1000000,100000,0,true,"",uuid(101));
        assertNull(MarketTradeCandidatePolicy.propose(a,peers(),targets(),settings(),1000000,0,uuid(999),100,0));
    }
    private static MarketWatchBazaarProduct bazaar(double bid,double ask,long volume) {
        return new MarketWatchBazaarProduct("TEST",ask,bid,100000,100000,volume,volume,
                List.of(new MarketWatchBazaarProduct.OrderLevel(100000,ask),new MarketWatchBazaarProduct.OrderLevel(100000,ask+.1),
                        new MarketWatchBazaarProduct.OrderLevel(100000,ask+.2)),
                List.of(new MarketWatchBazaarProduct.OrderLevel(100000,bid),new MarketWatchBazaarProduct.OrderLevel(100000,bid-.1),
                        new MarketWatchBazaarProduct.OrderLevel(100000,bid-.2)));
    }
    @Test void bazaarUsesBidForBuyOrderAndAskForSellOffer() {
        var s = settings(); s.minProfitCoins = 1000;
        var p = MarketTradeBazaarPolicy.propose(bazaar(100,125,20000),s,1000000,0,125);
        assertNotNull(p); assertEquals(100.1,p.buy()); assertEquals(124.9,p.sell()); assertEquals(64,p.quantity());
        assertEquals(6407,p.reserved());
    }
    @Test void invertedThinOrExtremeBazaarSpreadNeverSpends() {
        var s = settings(); s.minProfitCoins = 0;
        assertNull(MarketTradeBazaarPolicy.propose(bazaar(125,100,20000),s,1000000,0,0));
        assertNull(MarketTradeBazaarPolicy.propose(bazaar(100,125,500),s,1000000,0,0));
        assertNull(MarketTradeBazaarPolicy.propose(bazaar(100,1000,20000),s,1000000,0,0));
        assertNull(MarketTradeBazaarPolicy.propose(bazaar(100,125,20000),s,1000000,0,80));
    }
    @Test void bazaarOfferEvidenceRequiresFullQuantityPriceAndOwner() {
        var p = new MarketTradeJournal.Position(); p.name = "Coal"; p.quantity = 64; p.buyUnit = 100.1; p.sellUnit = 124.9;
        var item = new MarketTradeMenuPolicy.Item(10,"SELL Coal","COAL","","","",64,
                List.of("Offer amount: 64x","Price per unit: 124.9 coins","Filled: 64/64 (100%)","By: [MVP+] Owner"));
        assertTrue(MarketTradePolicy.bazaarOrder(item,p,true,true));
        assertTrue(MarketTradeBazaarPolicy.owner(item.lore(),"Owner")); assertFalse(MarketTradeBazaarPolicy.owner(item.lore(),"Other"));
        p.quantity = 32; assertFalse(MarketTradePolicy.bazaarOrder(item,p,true,true));
    }
    @Test void setupReceiptMustMatchExactKindQuantityNameAndTotal() {
        var p = new MarketTradeJournal.Position(); p.name = "Coal"; p.quantity = 64; p.buyUnit = 100.1; p.sellUnit = 124.9;
        assertTrue(MarketTradePolicy.bazaarSetupReceipt("[Bazaar] Buy Order Setup! 64x Coal for 6,406.4 coins.",p,false));
        assertFalse(MarketTradePolicy.bazaarSetupReceipt("[Bazaar] Sell Offer Setup! 64x Coal for 6,406.4 coins.",p,false));
        assertFalse(MarketTradePolicy.bazaarSetupReceipt("[Bazaar] Buy Order Setup! 63x Coal for 6,406.4 coins.",p,false));
    }
}
