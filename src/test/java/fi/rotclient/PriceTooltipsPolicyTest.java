package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class PriceTooltipsPolicyTest {
    @Test
    void remoteQuotesAreFetchedOnlyForAnEnabledTooltipModule() {
        assertFalse(PriceTooltipsPolicy.shouldFetchRemoteQuotes(false));
        assertTrue(PriceTooltipsPolicy.shouldFetchRemoteQuotes(true));
    }

    @Test
    void npcAndPaidLinesIncludeStackQuantity() {
        List<String> lines = PriceTooltipsPolicy.lines(
                true, false, false, true, false, true, 0, false, 2,
                new PriceTooltipsPolicy.Quote(0, 0, 0, 100, 0),
                5000);
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("NPC Price"));
        assertTrue(lines.get(0).contains("2x"));
        assertTrue(lines.get(1).contains("Price Paid"));
    }

    @Test
    void motesUseBurgerBonusOnlyInRift() {
        List<String> rift = PriceTooltipsPolicy.lines(
                true, false, false, false, true, false, 5, true, 1,
                new PriceTooltipsPolicy.Quote(0, 0, 0, 0, 100),
                0);
        List<String> hub = PriceTooltipsPolicy.lines(
                true, false, false, false, true, false, 5, false, 1,
                new PriceTooltipsPolicy.Quote(0, 0, 0, 0, 100),
                0);
        assertEquals(1, rift.size());
        assertTrue(rift.get(0).contains("Motes"));
        assertTrue(hub.isEmpty());
        assertEquals(5, PriceTooltipsPolicy.clampBurgers(99));
    }

    @Test
    void loreParsersReadNpcAndPurchaseCost() {
        assertEquals(12_000.0D, PriceTooltipsPolicy.npcFromLore(List.of("Sell for: 12,000 coins")));
        assertEquals(250L, PriceTooltipsPolicy.purchaseCostFromLore(List.of("Cost: 250 coins")).orElse(0L));
        assertEquals(64, PriceTooltipsPolicy.stackQuantity(List.of("Stored: 64/640"), 1));
    }
}
