package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage derived from MiningTracker runtime captures on
 * 2026-08-05 (miningtracker-diagnostic-20260805-025621.log).
 */
final class PowderChestRuntimeRegressionTest {
    private static final String SEPARATOR = separator(27);

    @Test
    void runtimeBlockOneFinalizesWithExactQuantities() {
        Fixture fixture = startedFixture();
        playBlock(
                fixture,
                1_000L,
                List.of(
                        "Rough Topaz Gemstone x24",
                        "Gold Essence x2",
                        "Diamond Essence x2",
                        "Gemstone Powder x291"));

        assertEquals(3, chestLootEntries(fixture));
        assertEquals(24L, ledgerQuantity(fixture, roughTopaz()));
        assertEquals(2L, ledgerQuantity(fixture, goldEssence()));
        assertEquals(2L, ledgerQuantity(fixture, diamondEssence()));
        assertEquals(1, currencyEntries(fixture));
        assertEquals(291L, currencyLedgerQuantity(fixture, gemstonePowder()));
        assertTrue(fixture.sink.contains("CHEST_CURRENCY_CREDITED"));
        assertFalse(fixture.sink.contains("CHEST_CONTEXT_TIMEOUT"));
        assertTrue(fixture.sink.contains("CHEST_CONTEXT_FINALIZED"));
    }

    @Test
    void runtimeObservedBlocksFinalizeWithoutTimeout() {
        Fixture fixture = startedFixture();
        long timestamp = 1_000L;

        timestamp = playBlock(
                fixture,
                timestamp,
                List.of(
                        "Rough Topaz Gemstone x24",
                        "Gold Essence x2",
                        "Diamond Essence x2",
                        "Gemstone Powder x291"));
        assertChestLootCount(fixture, 3);
        assertFalse(fixture.sink.contains("CHEST_CONTEXT_TIMEOUT"));

        timestamp = playBlock(
                fixture,
                timestamp + 100L,
                List.of(
                        "Rough Topaz Gemstone x80",
                        "Gold Essence",
                        "Diamond Essence",
                        "Gemstone Powder x113"));
        assertChestLootCount(fixture, 6);
        assertFalse(fixture.sink.contains("CHEST_CONTEXT_TIMEOUT"));

        timestamp = playBlock(
                fixture,
                timestamp + 100L,
                List.of(
                        "Rough Topaz Gemstone x24",
                        "Yoggie",
                        "Diamond Essence x2",
                        "Gemstone Powder x229"));
        assertChestLootCount(fixture, 8);
        assertTrue(fixture.sink.contains("CHEST_REWARD_UNKNOWN"));
        assertFalse(fixture.sink.contains("CHEST_CONTEXT_TIMEOUT"));

        playBlock(
                fixture,
                timestamp + 100L,
                List.of(
                        "Flawed Topaz Gemstone",
                        "Rough Topaz Gemstone x56",
                        "Diamond Essence x2"));
        assertChestLootCount(fixture, 11);
        assertFalse(fixture.sink.contains("CHEST_CONTEXT_TIMEOUT"));
        assertFalse(fixture.sink.contains("CHEST_CONTEXT_TIMEOUT_INACTIVE"));
        assertFalse(fixture.sink.contains("CHEST_CONTEXT_TIMEOUT_ABSOLUTE"));
    }

    @Test
    void runtimeObservedBlocksProduceExpectedAggregateQuantities() {
        Fixture fixture = startedFixture();
        long timestamp = 1_000L;
        timestamp = playBlock(
                fixture,
                timestamp,
                List.of(
                        "Rough Topaz Gemstone x24",
                        "Gold Essence x2",
                        "Diamond Essence x2",
                        "Gemstone Powder x291"));
        timestamp = playBlock(
                fixture,
                timestamp + 100L,
                List.of(
                        "Rough Topaz Gemstone x80",
                        "Gold Essence",
                        "Diamond Essence",
                        "Gemstone Powder x113"));
        timestamp = playBlock(
                fixture,
                timestamp + 100L,
                List.of(
                        "Rough Topaz Gemstone x24",
                        "Yoggie",
                        "Diamond Essence x2",
                        "Gemstone Powder x229"));
        playBlock(
                fixture,
                timestamp + 100L,
                List.of(
                        "Flawed Topaz Gemstone",
                        "Rough Topaz Gemstone x56",
                        "Diamond Essence x2"));

        assertEquals(
                184L,
                ledgerQuantity(fixture, roughTopaz()));
        assertEquals(
                1L,
                ledgerQuantity(fixture, flawedTopaz()));
        assertEquals(
                3L,
                ledgerQuantity(fixture, goldEssence()));
        assertEquals(
                7L,
                ledgerQuantity(fixture, diamondEssence()));
        assertEquals(633L, currencyLedgerQuantity(fixture, gemstonePowder()));
        assertTrue(fixture.sink.contains("CHEST_CURRENCY_CREDITED"));
    }

    @Test
    void ledgerRemainsEmptyUntilEachRuntimeSeparator() {
        Fixture fixture = startedFixture();
        fixture.engine.observePowderChestChatLine(
                "CHEST LOCKPICKED",
                1_000L);
        assertChestLootCount(fixture, 0);

        fixture.engine.observePowderChestChatLine(
                "    Rough Topaz Gemstone x24",
                1_001L);
        assertChestLootCount(fixture, 0);

        fixture.engine.observePowderChestChatLine(
                "    Gemstone Powder x291",
                1_002L);
        assertChestLootCount(fixture, 0);
        assertEquals(0, currencyEntries(fixture));

        fixture.engine.observePowderChestChatLine(SEPARATOR, 1_003L);
        assertChestLootCount(fixture, 1);
        assertEquals(1, currencyEntries(fixture));
        assertEquals(291L, currencyLedgerQuantity(fixture, gemstonePowder()));
    }

    @Test
    void runtimeSeparatorUsesBlackRectangle() {
        PowderChestChatParser.ParsedLine parsed =
                PowderChestChatParser.parse(SEPARATOR);

        assertInstanceOf(
                PowderChestChatParser.ParsedLine.EndMarker.class,
                parsed);
    }

    @Test
    void runtimeRubyBlockCreditsCurrencyAtomically() {
        Fixture fixture = startedRubyFixture();
        playBlock(
                fixture,
                1_000L,
                List.of(
                        "Rough Ruby Gemstone x48",
                        "Gemstone Powder x296"));

        MiningSessionResource ruby = resource(
                GemstoneType.RUBY,
                GemstoneTier.ROUGH);
        assertEquals(2, totalLedgerEntries(fixture));
        assertEquals(48L, ledgerQuantityFor(fixture, ruby));
        assertEquals(296L, currencyLedgerQuantity(fixture, gemstonePowder()));
        assertTrue(fixture.sink.contains("CHEST_CURRENCY_CREDITED"));
        assertTrue(fixture.sink.contains("CHEST_REWARD_CREDITED"));
        assertFalse(fixture.sink.contains("CHEST_CONTEXT_TIMEOUT"));
    }

    @Test
    void runtimeExplicitQuantitySyntaxIsParsed() {
        assertItem(
                "    Rough Topaz Gemstone x24",
                "Rough Topaz Gemstone",
                24L);
        assertItem("    Gold Essence x2", "Gold Essence", 2L);
        assertItem("    Gold Essence", "Gold Essence", 1L);
        assertCurrency(
                "    Gemstone Powder x291",
                "Gemstone Powder",
                291L);
        assertCurrency(
                "    Mithril Powder x1,234",
                "Mithril Powder",
                1_234L);
    }

    private static void assertItem(
            String line,
            String displayName,
            long quantity) {
        PowderChestChatParser.ParsedLine.ItemReward reward =
                assertInstanceOf(
                        PowderChestChatParser.ParsedLine.ItemReward.class,
                        PowderChestChatParser.parse(line));
        assertEquals(displayName, reward.displayName());
        assertEquals(quantity, reward.quantity());
    }

    private static void assertCurrency(
            String line,
            String displayName,
            long quantity) {
        PowderChestChatParser.ParsedLine.CurrencyReward reward =
                assertInstanceOf(
                        PowderChestChatParser.ParsedLine.CurrencyReward.class,
                        PowderChestChatParser.parse(line));
        assertEquals(displayName, reward.displayName());
        assertEquals(quantity, reward.quantity());
    }

    private static long playBlock(
            Fixture fixture,
            long startTimestamp,
            List<String> rewardLines) {
        fixture.engine.observePowderChestChatLine(
                "CHEST LOCKPICKED",
                startTimestamp);
        long timestamp = startTimestamp + 1L;
        for (String rewardLine : rewardLines) {
            fixture.engine.observePowderChestChatLine(
                    "    " + rewardLine,
                    timestamp++);
        }
        fixture.engine.observePowderChestChatLine(SEPARATOR, timestamp);
        return timestamp + 1L;
    }

    private static String separator(int length) {
        return String.valueOf(PowderChestChatParser.SEPARATOR_CHAR)
                .repeat(length);
    }

    private static void assertChestLootCount(Fixture fixture, int expected) {
        assertEquals(expected, chestLootEntries(fixture));
    }

    private static int chestLootEntries(Fixture fixture) {
        return fixture.engine.snapshot(9_999L)
                .map(MiningSessionSnapshot::chestLootEntryCount)
                .orElse(0);
    }

    private static int currencyEntries(Fixture fixture) {
        return fixture.engine.snapshot(9_999L)
                .map(MiningSessionSnapshot::currencyEntryCount)
                .orElse(0);
    }

    private static int totalLedgerEntries(Fixture fixture) {
        return fixture.engine.snapshot(9_999L)
                .map(MiningSessionSnapshot::entryCount)
                .orElse(0);
    }

    private static long ledgerQuantity(
            Fixture fixture,
            MiningSessionResource resource) {
        return ledgerQuantityFor(fixture, resource);
    }

    private static long ledgerQuantityFor(
            Fixture fixture,
            MiningSessionResource resource) {
        return fixture.engine.snapshot(9_999L)
                .map(snapshot -> snapshot.quantity(
                        MiningSessionCategory.CHEST_LOOT,
                        resource))
                .orElse(0L);
    }

    private static long currencyLedgerQuantity(
            Fixture fixture,
            MiningSessionResource resource) {
        return fixture.engine.snapshot(9_999L)
                .map(snapshot -> snapshot.quantity(
                        MiningSessionCategory.CURRENCY,
                        resource))
                .orElse(0L);
    }

    private static MiningSessionResource gemstonePowder() {
        return MiningSessionResource.currency(
                "GEMSTONE_POWDER",
                "Gemstone Powder");
    }

    private static MiningSessionResource roughTopaz() {
        return resource(GemstoneType.TOPAZ, GemstoneTier.ROUGH);
    }

    private static MiningSessionResource flawedTopaz() {
        return resource(GemstoneType.TOPAZ, GemstoneTier.FLAWED);
    }

    private static MiningSessionResource goldEssence() {
        return MiningSessionResource.genericItem(
                "GOLD_ESSENCE",
                "Gold Essence");
    }

    private static MiningSessionResource diamondEssence() {
        return MiningSessionResource.genericItem(
                "DIAMOND_ESSENCE",
                "Diamond Essence");
    }

    private static MiningSessionResource resource(
            GemstoneType gemstone,
            GemstoneTier tier) {
        return new MiningResourceCatalog()
                .fromGemstone(gemstone, tier)
                .orElseThrow()
                .resource();
    }

    private static Fixture startedRubyFixture() {
        CaptureSink sink = new CaptureSink();
        MiningSessionEngine engine = new MiningSessionEngine(
                new MiningResourceCatalog(),
                new MiningSessionLedger(),
                sink,
                0L,
                0L,
                0L);
        Map<GemstoneTier, Long> quantities =
                new EnumMap<>(GemstoneTier.class);
        for (GemstoneTier tier : GemstoneTier.values()) {
            quantities.put(tier, 0L);
        }
        engine.onDiagnosticStart(
                true,
                TrackerSelection.RUBY,
                MiningSessionParity.LiveBaseline.gemstone(
                        TrackerSelection.RUBY,
                        quantities,
                        0L,
                        0L,
                        10L),
                10L);
        return new Fixture(sink, engine);
    }

    private static Fixture startedFixture() {
        CaptureSink sink = new CaptureSink();
        MiningSessionEngine engine = new MiningSessionEngine(
                new MiningResourceCatalog(),
                new MiningSessionLedger(),
                sink,
                0L,
                0L,
                0L);
        engine.onDiagnosticStart(
                true,
                TrackerSelection.TOPAZ,
                baseline(10L),
                10L);
        return new Fixture(sink, engine);
    }

    private static MiningSessionParity.LiveBaseline baseline(long timestamp) {
        Map<GemstoneTier, Long> quantities =
                new EnumMap<>(GemstoneTier.class);
        for (GemstoneTier tier : GemstoneTier.values()) {
            quantities.put(tier, 0L);
        }
        return MiningSessionParity.LiveBaseline.gemstone(
                TrackerSelection.TOPAZ,
                quantities,
                0L,
                0L,
                timestamp);
    }

    private record Fixture(CaptureSink sink, MiningSessionEngine engine) {
    }

    private static final class CaptureSink
            implements MiningSessionShadowObserver.DiagnosticSink {
        private final List<Marker> markers = new ArrayList<>();

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void record(String marker, String details) {
            markers.add(new Marker(marker, details));
        }

        private boolean contains(String marker) {
            return markers.stream().anyMatch(value ->
                    value.name().equals(marker));
        }

        private List<String> detailsFor(String marker) {
            return markers.stream()
                    .filter(value -> value.name().equals(marker))
                    .map(Marker::details)
                    .toList();
        }
    }

    private record Marker(String name, String details) {
    }
}
