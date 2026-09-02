package fi.rotclient;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerItemProfitPolicyTest {
    @Test
    void groundGateUsesOnlyKnownSkyBlockIds() {
        assertTrue(SlayerItemProfitPolicy.isKnownSlayerDropId("WARDEN_HEART"));
        assertFalse(SlayerItemProfitPolicy.isKnownSlayerDropId("minecraft:diamond"));
        assertFalse(SlayerItemProfitPolicy.isKnownSlayerDropId(""));
    }

    @Test
    void valuesOnlyRecognizedDropsWithAValidLivePrice() {
        SlayerItemProfitPolicy.Projection projection = SlayerItemProfitPolicy.project(
                Map.of("Revenant flesh", 3, "Warden heart", 1, "Uncatalogued thing", 2),
                Map.of("REVENANT_FLESH", new BigDecimal("12.5")));

        assertEquals(new BigDecimal("37.50"), projection.totalValue());
        assertEquals(3, projection.pricedItems());
        assertEquals(3, projection.unpricedItems());
        assertEquals("Revenant flesh", projection.rows().getFirst().displayName());
        assertTrue(projection.rows().getFirst().priced());
        assertFalse(projection.rows().getLast().priced());
    }

    @Test
    void rejectsZeroAndNegativeQuotesInsteadOfInventingProfit() {
        SlayerItemProfitPolicy.Projection projection = SlayerItemProfitPolicy.project(
                Map.of("Wolf tooth", 4), Map.of("WOLF_TOOTH", BigDecimal.ZERO));

        assertEquals(BigDecimal.ZERO.setScale(2), projection.totalValue());
        assertEquals(0, projection.pricedItems());
        assertEquals(4, projection.unpricedItems());
    }
}
