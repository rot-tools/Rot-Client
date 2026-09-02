package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Runtime-derived regression for diagnostic shadow valuation. */
final class MiningSessionPriceValuationRegressionTest {
    @Test
    void mixedSessionValuesSupportedMaterialAndExcludesUnsupportedAndCurrency() {
        Fixture fixture = fixture(
                book(
                        10_000L,
                        Map.of(
                                TrackedMaterial.HARD_STONE.enchantedBazaarId(),
                                new BigDecimal("4.00"),
                                "ROUGH_TOPAZ_GEM",
                                new BigDecimal("2.00"))));

        fixture.engine.onDiagnosticStart(
                true,
                TrackerSelection.RUBY,
                baseline(10_000L),
                10_000L);
        fixture.engine.onConfirmedMaterialBreak(
                TrackedMaterial.HARD_STONE,
                1,
                10_050L);
        fixture.engine.observeSackChanges(
                List.of(new SackChangeParser.Change(
                        12L,
                        "Enchanted Hard Stone",
                        List.of("Enchanted Mining Sack"))),
                10_100L);
        playChestBlock(fixture, 10_200L);

        MiningSessionSnapshot snapshot =
                fixture.engine.snapshot(10_300L).orElseThrow();
        MiningSessionValuation valuation = snapshot.valuation();
        MiningSessionResource enchantedHardStone = enchantedHardStone();
        MiningSessionResource roughTopaz = roughTopaz();
        MiningSessionResource gemstonePowder = gemstonePowder();

        assertEquals(12L, snapshot.quantity(
                MiningSessionCategory.OTHER_MINED,
                enchantedHardStone));
        assertEquals(24L, snapshot.quantity(
                MiningSessionCategory.CHEST_LOOT,
                roughTopaz));
        assertEquals(291L, snapshot.quantity(
                MiningSessionCategory.CURRENCY,
                gemstonePowder));
        assertEquals(2, valuation.resolvedEntryCount());
        assertEquals(2, valuation.unsupportedEntryCount());
        assertEquals(1, valuation.excludedCurrencyEntryCount());
        assertEquals(0, new BigDecimal("96.00").compareTo(
                valuation.resolvedItemValue()));
        assertEquals(
                0,
                new BigDecimal("48.00").compareTo(
                        valuation.valueByCategory(
                                MiningSessionCategory.OTHER_MINED)));
        assertEquals(
                0,
                new BigDecimal("48.00").compareTo(
                        valuation.valueByCategory(
                                MiningSessionCategory.CHEST_LOOT)));
        assertEquals(0L, snapshot.parityMismatchCount());
        assertTrue(snapshot.entries().stream().allMatch(entry ->
                entry.priceResolution().status()
                        == MiningSessionPriceResolution.PriceStatus.UNRESOLVED));
    }

    private static void playChestBlock(Fixture fixture, long timestamp) {
        fixture.engine.observePowderChestChatLine(
                "CHEST LOCKPICKED",
                timestamp++);
        fixture.engine.observePowderChestChatLine(
                "    Rough Topaz Gemstone x24",
                timestamp++);
        fixture.engine.observePowderChestChatLine(
                "    Gold Essence x2",
                timestamp++);
        fixture.engine.observePowderChestChatLine(
                "    Diamond Essence x2",
                timestamp++);
        fixture.engine.observePowderChestChatLine(
                "    Gemstone Powder x291",
                timestamp++);
        fixture.engine.observePowderChestChatLine(
                String.valueOf(PowderChestChatParser.SEPARATOR_CHAR)
                        .repeat(27),
                timestamp);
    }

    private static Fixture fixture(MiningSessionPriceBook book) {
        CaptureSink sink = new CaptureSink();
        MiningResourceCatalog catalog = new MiningResourceCatalog();
        return new Fixture(
                catalog,
                sink,
                new MiningSessionEngine(
                        catalog,
                        new MiningSessionLedger(),
                        sink,
                        0L,
                        0L,
                        0L,
                        now -> book));
    }

    private static MiningSessionPriceBook book(
            long observedAtMillis,
            Map<String, BigDecimal> prices) {
        return MiningSessionPriceBook.available(observedAtMillis, prices);
    }

    private static MiningSessionResource enchantedHardStone() {
        return MiningSessionResource.material(
                TrackedMaterial.HARD_STONE.enchantedBazaarId(),
                TrackedMaterial.HARD_STONE.enchantedItemName(),
                TrackedMaterial.HARD_STONE);
    }

    private static MiningSessionResource roughTopaz() {
        return MiningSessionResource.gemstone(
                "ROUGH_TOPAZ_GEM",
                "Rough Topaz Gemstone",
                GemstoneType.TOPAZ,
                GemstoneTier.ROUGH);
    }

    private static MiningSessionResource gemstonePowder() {
        return MiningSessionResource.currency(
                "GEMSTONE_POWDER",
                "Gemstone Powder");
    }

    private static MiningSessionParity.LiveBaseline baseline(long timestamp) {
        Map<GemstoneTier, Long> quantities =
                new EnumMap<>(GemstoneTier.class);
        for (GemstoneTier tier : GemstoneTier.values()) {
            quantities.put(tier, 0L);
        }
        return MiningSessionParity.LiveBaseline.gemstone(
                TrackerSelection.RUBY,
                quantities,
                0L,
                0L,
                timestamp);
    }

    private record Fixture(
            MiningResourceCatalog catalog,
            CaptureSink sink,
            MiningSessionEngine engine) {
    }

    private static final class CaptureSink
            implements MiningSessionShadowObserver.DiagnosticSink {
        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void record(String marker, String details) {
        }
    }
}
