package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

final class MiningSessionSummaryFormatterTest {
    private final MiningSessionSummaryFormatter formatter =
            new MiningSessionSummaryFormatter();

    @Test
    void formatsDeterministicCompleteSummary() {
        String first = formatter.format(completeModel());
        String second = formatter.format(completeModel());
        assertEquals(first, second);
        assertTrue(first.startsWith("Rot Client Session Analytics"));
        assertTrue(first.contains("State: Stopped"));
        assertTrue(first.contains("Target: Gold")
                || first.contains("Target: Pure Gold"));
        assertTrue(first.contains("Tracker: ON"));
        assertTrue(first.contains("TARGET_MINED: 1"));
        assertTrue(first.contains("OTHER_MINED: 1"));
        assertTrue(first.contains("CHEST_LOOT: 2"));
        assertTrue(first.contains("CURRENCY: 1"));
        assertTrue(first.contains("Resolved item value: 1,700.30 coins"));
        assertTrue(first.contains("Price basis: Bazaar instant sell (gross)"));
        assertTrue(first.contains("GOLD_INGOT: 4"));
        assertTrue(first.contains("ENCHANTED_HARD_STONE: 12"));
        assertTrue(first.contains("ROUGH_TOPAZ_GEM: 24"));
        assertTrue(first.contains("FLAWED_RUBY_GEM: 2"));
        assertTrue(first.contains("GEMSTONE_POWDER: 291"));
        assertTrue(first.contains("Parity: MATCH"));
        assertTrue(first.contains("Mismatches: 0"));
        assertTrue(first.contains("Excluded currency entries:"));
        assertFalse(first.toLowerCase(Locale.ROOT).contains("profit"));
    }

    @Test
    void activeAndUnavailablePriceBookRenderSafely() {
        MiningSessionAnalyticsViewModel active = MiningSessionAnalyticsViewModel.create(
                MiningSessionAnalyticsViewModel.SessionState.ACTIVE,
                "Ruby",
                true,
                OptionalLong.of(10L),
                OptionalLong.of(11L),
                OptionalLong.empty(),
                0,
                0,
                0,
                0,
                0,
                "NOT_CHECKED",
                0L,
                MiningSessionValuation.PRICE_BASIS_LABEL,
                false,
                BigDecimal.ZERO,
                0,
                0,
                0,
                0,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                OptionalLong.empty(),
                false,
                false,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of());
        String text = formatter.format(active);
        assertTrue(text.contains("State: Active"));
        assertTrue(text.contains("Resolved item value: unavailable"));
        assertTrue(text.contains("Price-book age: unavailable"));
        assertTrue(text.contains("(none)"));
    }

    @Test
    void stalePriceBookAgeIsShownWithoutPrivateData() {
        MiningSessionAnalyticsViewModel stale = MiningSessionAnalyticsViewModel.create(
                MiningSessionAnalyticsViewModel.SessionState.STOPPED,
                "Gold",
                false,
                OptionalLong.of(10L),
                OptionalLong.of(20L),
                OptionalLong.of(20L),
                1,
                1,
                0,
                0,
                0,
                "MATCH",
                0L,
                MiningSessionValuation.PRICE_BASIS_LABEL,
                true,
                new BigDecimal("5.00"),
                1,
                0,
                1,
                0,
                0,
                0,
                new BigDecimal("5.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                OptionalLong.of(65_000L),
                true,
                true,
                Map.of(
                        "GOLD_INGOT",
                        new MiningSessionAnalyticsViewModel.ResourceQuantity(
                                "GOLD_INGOT",
                                "Gold Ingot",
                                1L)),
                Map.of(),
                Map.of(),
                Map.of());
        String text = formatter.format(stale);
        assertTrue(text.contains("Price-book age: 65s"));
        assertTrue(text.contains("Stale entries: 1"));
        assertFalse(text.contains("uuid"));
        assertFalse(text.contains("sessionId"));
        assertFalse(text.contains("correlation"));
        assertFalse(text.contains("eventId"));
        assertFalse(text.contains("/home/"));
        assertFalse(text.contains("C:\\"));
        assertFalse(text.toLowerCase(Locale.ROOT).contains("chat"));
        assertFalse(text.contains("x="));
        assertFalse(text.contains("y="));
        assertFalse(text.contains("z="));
    }

    @Test
    void outputIsBounded() {
        LinkedHashMap<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                many = new LinkedHashMap<>();
        for (int i = 0; i < 80; i++) {
            String id = String.format(Locale.ROOT, "ITEM_%02d", i);
            many.put(
                    id,
                    new MiningSessionAnalyticsViewModel.ResourceQuantity(
                            id,
                            id,
                            i + 1L));
        }
        MiningSessionAnalyticsViewModel model = MiningSessionAnalyticsViewModel.create(
                MiningSessionAnalyticsViewModel.SessionState.ACTIVE,
                "Gold",
                true,
                OptionalLong.of(1L),
                OptionalLong.of(2L),
                OptionalLong.empty(),
                80,
                80,
                0,
                0,
                0,
                "MATCH",
                0L,
                MiningSessionValuation.PRICE_BASIS_LABEL,
                false,
                BigDecimal.ZERO,
                0,
                0,
                0,
                0,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                OptionalLong.empty(),
                false,
                false,
                many,
                Map.of(),
                Map.of(),
                Map.of());
        String text = formatter.format(model);
        assertTrue(text.length()
                <= MiningSessionSummaryFormatter.MAX_SUMMARY_LENGTH);
        assertTrue(text.contains(" more") || text.contains("...truncated"));
    }

    @Test
    void localeRootTwoDecimalFormatting() {
        assertTrue(formatter.format(completeModel())
                .contains("1,700.30"));
    }

    private static MiningSessionAnalyticsViewModel completeModel() {
        LinkedHashMap<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                target = new LinkedHashMap<>();
        target.put(
                "GOLD_INGOT",
                new MiningSessionAnalyticsViewModel.ResourceQuantity(
                        "GOLD_INGOT",
                        "Gold Ingot",
                        4L));
        LinkedHashMap<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                other = new LinkedHashMap<>();
        other.put(
                "ENCHANTED_HARD_STONE",
                new MiningSessionAnalyticsViewModel.ResourceQuantity(
                        "ENCHANTED_HARD_STONE",
                        "Enchanted Hard Stone",
                        12L));
        LinkedHashMap<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                chest = new LinkedHashMap<>();
        chest.put(
                "FLAWED_RUBY_GEM",
                new MiningSessionAnalyticsViewModel.ResourceQuantity(
                        "FLAWED_RUBY_GEM",
                        "Flawed Ruby Gemstone",
                        2L));
        chest.put(
                "ROUGH_TOPAZ_GEM",
                new MiningSessionAnalyticsViewModel.ResourceQuantity(
                        "ROUGH_TOPAZ_GEM",
                        "Rough Topaz Gemstone",
                        24L));
        LinkedHashMap<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                currency = new LinkedHashMap<>();
        currency.put(
                "GEMSTONE_POWDER",
                new MiningSessionAnalyticsViewModel.ResourceQuantity(
                        "GEMSTONE_POWDER",
                        "Gemstone Powder",
                        291L));
        return MiningSessionAnalyticsViewModel.create(
                MiningSessionAnalyticsViewModel.SessionState.STOPPED,
                "Gold",
                true,
                OptionalLong.of(10L),
                OptionalLong.of(20L),
                OptionalLong.of(20L),
                5,
                1,
                1,
                2,
                1,
                "MATCH",
                0L,
                MiningSessionValuation.PRICE_BASIS_LABEL,
                true,
                new BigDecimal("1700.30"),
                4,
                0,
                0,
                0,
                1,
                1,
                new BigDecimal("8.00"),
                new BigDecimal("48.00"),
                new BigDecimal("1644.30"),
                OptionalLong.of(12_000L),
                true,
                false,
                target,
                other,
                chest,
                currency);
    }
}
