package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MarketWatchDealPolicyTest {
    @Test
    void twentyPercentMeansLiteralTwentyPercent() {
        MarketWatchDealPolicy.Analysis analysis =
                MarketWatchDealPolicy.analyzeAuction(
                        120_000L,
                        repeated(
                                150_000L,
                                10),
                        20.0D);

        assertTrue(
                analysis.usable());

        assertTrue(
                analysis.trigger());

        assertEquals(
                150_000L,
                analysis.referencePrice());

        assertEquals(
                120_000L,
                analysis.thresholdPrice());

        assertEquals(
                20.0D,
                analysis.requestedDiscountPercent(),
                0.0001D);

        assertEquals(
                20.0D,
                analysis.requiredDiscountPercent(),
                0.0001D);

        assertEquals(
                20.0D,
                analysis.actualDiscountPercent(),
                0.0001D);

        assertEquals(
                30_000L,
                analysis.potentialSpreadCoins());
    }


    @Test
    void oneCoinAboveTwentyPercentThresholdDoesNotTrigger() {
        MarketWatchDealPolicy.Analysis analysis =
                MarketWatchDealPolicy.analyzeAuction(
                        120_001L,
                        repeated(
                                150_000L,
                                10),
                        20.0D);

        assertFalse(
                analysis.trigger());
    }


    @Test
    void expensiveNineteenPointFiveMillionDoesNotTriggerAtTwentyPercent() {
        MarketWatchDealPolicy.Analysis analysis =
                MarketWatchDealPolicy.analyzeAuction(
                        19_500_000L,
                        repeated(
                                20_000_000L,
                                12),
                        20.0D);

        assertEquals(
                20_000_000L,
                analysis.referencePrice());

        assertEquals(
                16_000_000L,
                analysis.thresholdPrice());

        assertEquals(
                2.5D,
                analysis.actualDiscountPercent(),
                0.0001D);

        assertFalse(
                analysis.trigger());
    }


    @Test
    void expensiveSixteenMillionTriggersAtTwentyPercent() {
        MarketWatchDealPolicy.Analysis analysis =
                MarketWatchDealPolicy.analyzeAuction(
                        16_000_000L,
                        repeated(
                                20_000_000L,
                                12),
                        20.0D);

        assertEquals(
                4_000_000L,
                analysis.potentialSpreadCoins());

        assertEquals(
                20.0D,
                analysis.actualDiscountPercent(),
                0.0001D);

        assertTrue(
                analysis.trigger());
    }


    @Test
    void sparseMarketUsesLowerMedianInsteadOfInflatedMidpoint() {
        MarketWatchDealPolicy.Analysis analysis =
                MarketWatchDealPolicy.analyzeAuction(
                        100_000L,
                        List.of(
                                125_000L,
                                500_000L),
                        20.0D);

        assertEquals(
                125_000L,
                analysis.referencePrice());

        assertEquals(
                100_000L,
                analysis.thresholdPrice());

        assertTrue(
                analysis.trigger());
    }


    @Test
    void hugeHighOutliersDoNotInflateReference() {
        MarketWatchDealPolicy.Analysis analysis =
                MarketWatchDealPolicy.analyzeAuction(
                        80_000_000L,
                        List.of(
                                100_000_000L,
                                101_000_000L,
                                102_000_000L,
                                103_000_000L,
                                104_000_000L,
                                105_000_000L,
                                106_000_000L,
                                1_000_000_000L,
                                2_000_000_000L),
                        20.0D);

        assertEquals(
                103_000_000L,
                analysis.referencePrice());

        assertTrue(
                analysis.highOutliersRemoved()
                        >= 2);

        assertTrue(
                analysis.trigger());
    }


    @Test
    void noComparisonCannotTrigger() {
        MarketWatchDealPolicy.Analysis analysis =
                MarketWatchDealPolicy.analyzeAuction(
                        50_000L,
                        List.of(),
                        20.0D);

        assertFalse(
                analysis.usable());

        assertFalse(
                analysis.trigger());
    }


    private static List<Long> repeated(
            long value,
            int count) {

        List<Long> result =
                new ArrayList<>();

        for (int i = 0;
                i < count;
                i++) {

            result.add(
                    value);
        }

        return List.copyOf(
                result);
    }
}