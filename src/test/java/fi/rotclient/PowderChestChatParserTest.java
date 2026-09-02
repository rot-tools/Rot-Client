package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parser coverage uses synthetic examples. Real Powder Chest runtime fixtures
 * captured by MiningTracker remain required for in-game validation.
 */
final class PowderChestChatParserTest {
    private static final String SEPARATOR =
            String.valueOf(PowderChestChatParser.SEPARATOR_CHAR)
                    .repeat(48);

    @Test
    void recognizesFormattedChestLockpickedStart() {
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse("§6CHEST LOCKPICKED");

        assertInstanceOf(
                PowderChestChatParser.ParsedLine.StartMarker.class,
                parsed);
        assertEquals("CHEST LOCKPICKED", parsed.normalizedLine());
    }

    @Test
    void recognizesUnformattedChestLockpickedStart() {
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse("CHEST LOCKPICKED");

        assertInstanceOf(
                PowderChestChatParser.ParsedLine.StartMarker.class,
                parsed);
    }

    @Test
    void recognizesValidEndSeparator() {
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse(SEPARATOR);

        assertInstanceOf(
                PowderChestChatParser.ParsedLine.EndMarker.class,
                parsed);
    }

    @Test
    void recognizesMinimumLengthEndSeparator() {
        String separator = String.valueOf(
                PowderChestChatParser.SEPARATOR_CHAR).repeat(21);
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse(separator);

        assertInstanceOf(
                PowderChestChatParser.ParsedLine.EndMarker.class,
                parsed);
    }

    @Test
    void recognizesLegacyEndSeparator() {
        String separator = String.valueOf(
                PowderChestChatParser.LEGACY_SEPARATOR_CHAR).repeat(21);
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse(separator);

        assertInstanceOf(
                PowderChestChatParser.ParsedLine.EndMarker.class,
                parsed);
    }

    @Test
    void parsesExplicitQuantityWithoutSpaceAfterX() {
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse(
                        "    Rough Topaz Gemstone x24");

        PowderChestChatParser.ParsedLine.ItemReward reward =
                assertInstanceOf(
                        PowderChestChatParser.ParsedLine.ItemReward.class,
                        parsed);
        assertEquals("Rough Topaz Gemstone", reward.displayName());
        assertEquals(24L, reward.quantity());
    }

    @Test
    void parsesExplicitQuantity() {
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse(
                        "    Rough Ruby Gemstone x12");

        PowderChestChatParser.ParsedLine.ItemReward reward =
                assertInstanceOf(
                        PowderChestChatParser.ParsedLine.ItemReward.class,
                        parsed);
        assertEquals("Rough Ruby Gemstone", reward.displayName());
        assertEquals(12L, reward.quantity());
    }

    @Test
    void defaultsImplicitQuantityToOne() {
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse("    Wishing Compass");

        PowderChestChatParser.ParsedLine.ItemReward reward =
                assertInstanceOf(
                        PowderChestChatParser.ParsedLine.ItemReward.class,
                        parsed);
        assertEquals(1L, reward.quantity());
    }

    @Test
    void parsesCommaSeparatedQuantity() {
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse(
                        "    Gold Essence x1,250");

        PowderChestChatParser.ParsedLine.ItemReward reward =
                assertInstanceOf(
                        PowderChestChatParser.ParsedLine.ItemReward.class,
                        parsed);
        assertEquals(1_250L, reward.quantity());
    }

    @Test
    void stripsGemstoneStatIconPrefix() {
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse(
                        "    \uE102 Rough Ruby Gemstone x3");

        PowderChestChatParser.ParsedLine.ItemReward reward =
                assertInstanceOf(
                        PowderChestChatParser.ParsedLine.ItemReward.class,
                        parsed);
        assertEquals("Rough Ruby Gemstone", reward.displayName());
    }

    @Test
    void classifiesGemstonePowderAsCurrency() {
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse(
                        "    Gemstone Powder x500");

        assertInstanceOf(
                PowderChestChatParser.ParsedLine.CurrencyReward.class,
                parsed);
    }

    @Test
    void classifiesMithrilPowderAsCurrency() {
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse(
                        "    Mithril Powder x1,234");

        assertInstanceOf(
                PowderChestChatParser.ParsedLine.CurrencyReward.class,
                parsed);
        PowderChestChatParser.ParsedLine.CurrencyReward reward =
                assertInstanceOf(
                        PowderChestChatParser.ParsedLine.CurrencyReward.class,
                        parsed);
        assertEquals(1_234L, reward.quantity());
    }

    @Test
    void parsesGemstonePowderExplicitQuantity() {
        PowderChestChatParser.ParsedLine.CurrencyReward reward =
                assertInstanceOf(
                        PowderChestChatParser.ParsedLine.CurrencyReward.class,
                        PowderChestChatParser.parse(
                                "    Gemstone Powder x296"));
        assertEquals("Gemstone Powder", reward.displayName());
        assertEquals(296L, reward.quantity());
    }

    @Test
    void rejectsImplicitGemstonePowderQuantity() {
        PowderChestChatParser.ParsedLine.Ignored ignored =
                assertInstanceOf(
                        PowderChestChatParser.ParsedLine.Ignored.class,
                        PowderChestChatParser.parse(
                                "    Gemstone Powder"));
        assertEquals("missing-explicit-quantity", ignored.reason());
    }

    @Test
    void rejectsImplicitMithrilPowderQuantity() {
        PowderChestChatParser.ParsedLine.Ignored ignored =
                assertInstanceOf(
                        PowderChestChatParser.ParsedLine.Ignored.class,
                        PowderChestChatParser.parse(
                                "    Mithril Powder"));
        assertEquals("missing-explicit-quantity", ignored.reason());
    }

    @Test
    void rejectsGemstonePowderZeroQuantity() {
        PowderChestChatParser.ParsedLine.Ignored ignored =
                assertInstanceOf(
                        PowderChestChatParser.ParsedLine.Ignored.class,
                        PowderChestChatParser.parse(
                                "    Gemstone Powder x0"));
        assertEquals("non-positive-quantity", ignored.reason());
    }

    @Test
    void rejectsGemstonePowderNegativeQuantity() {
        assertInstanceOf(
                PowderChestChatParser.ParsedLine.Ignored.class,
                PowderChestChatParser.parse(
                        "    Gemstone Powder x-1"));
    }

    @Test
    void rejectsGemstonePowderMalformedQuantity() {
        PowderChestChatParser.ParsedLine.Ignored ignored =
                assertInstanceOf(
                        PowderChestChatParser.ParsedLine.Ignored.class,
                        PowderChestChatParser.parse(
                                "    Gemstone Powder xabc"));
        assertEquals("malformed-quantity", ignored.reason());
    }

    @Test
    void rejectsGemstonePowderOverflowingQuantity() {
        PowderChestChatParser.ParsedLine.Ignored ignored =
                assertInstanceOf(
                        PowderChestChatParser.ParsedLine.Ignored.class,
                        PowderChestChatParser.parse(
                                "    Gemstone Powder x"
                                        + Long.MAX_VALUE));
        assertEquals("overflowing-quantity", ignored.reason());
    }

    @Test
    void powderNeverBecomesItemReward() {
        assertInstanceOf(
                PowderChestChatParser.ParsedLine.CurrencyReward.class,
                PowderChestChatParser.parse(
                        "    Gemstone Powder x50"));
        assertInstanceOf(
                PowderChestChatParser.ParsedLine.CurrencyReward.class,
                PowderChestChatParser.parse(
                        "    Mithril Powder x50"));
    }

    @Test
    void ignoresHotmExperience() {
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse("+1,200 HOTM Experience");

        assertInstanceOf(
                PowderChestChatParser.ParsedLine.Ignored.class,
                parsed);
    }

    @Test
    void rejectsMalformedQuantity() {
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse(
                        "    Rough Ruby Gemstone xabc");

        PowderChestChatParser.ParsedLine.Ignored ignored =
                assertInstanceOf(
                        PowderChestChatParser.ParsedLine.Ignored.class,
                        parsed);
        assertEquals("malformed-quantity", ignored.reason());
    }

    @Test
    void rejectsZeroQuantity() {
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse(
                        "    Rough Ruby Gemstone x0");

        PowderChestChatParser.ParsedLine.Ignored ignored =
                assertInstanceOf(
                        PowderChestChatParser.ParsedLine.Ignored.class,
                        parsed);
        assertEquals("non-positive-quantity", ignored.reason());
    }

    @Test
    void rejectsNegativeQuantity() {
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse(
                        "    Rough Ruby Gemstone x-4");

        assertInstanceOf(
                PowderChestChatParser.ParsedLine.Ignored.class,
                parsed);
    }

    @Test
    void rejectsOverflowingQuantity() {
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse(
                        "    Rough Ruby Gemstone x"
                                + Long.MAX_VALUE);

        PowderChestChatParser.ParsedLine.Ignored ignored =
                assertInstanceOf(
                        PowderChestChatParser.ParsedLine.Ignored.class,
                        parsed);
        assertEquals("overflowing-quantity", ignored.reason());
    }

    @Test
    void ignoresUnrelatedChat() {
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse(
                        "You found a secret!");

        PowderChestChatParser.ParsedLine.Ignored ignored =
                assertInstanceOf(
                        PowderChestChatParser.ParsedLine.Ignored.class,
                        parsed);
        assertTrue(ignored.reason().contains("unrecognized"));
    }

    @Test
    void rejectsRewardWithoutLeadingIndentation() {
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse(
                        "Rough Ruby Gemstone x2");

        assertInstanceOf(
                PowderChestChatParser.ParsedLine.Ignored.class,
                parsed);
    }
}
