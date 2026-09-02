package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

final class MiningSessionAnalyticsViewModelTest {
    @Test
    void notStartedModelIsBounded() {
        MiningSessionAnalyticsViewModel model =
                MiningSessionAnalyticsViewModel.notStarted();
        assertEquals(
                MiningSessionAnalyticsViewModel.SessionState.NOT_STARTED,
                model.sessionState());
        assertFalse(model.hasViewableSession());
        assertTrue(model.targetQuantities().isEmpty());
        assertFalse(model.resolvedValueAvailable());
    }

    @Test
    void createCopiesMapsDefensivelyAndOmitsZeroThroughFactoryInputs() {
        LinkedHashMap<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                source = new LinkedHashMap<>();
        source.put(
                "GOLD_INGOT",
                new MiningSessionAnalyticsViewModel.ResourceQuantity(
                        "GOLD_INGOT",
                        "Gold Ingot",
                        4L));
        MiningSessionAnalyticsViewModel model = modelWith(
                MiningSessionAnalyticsViewModel.SessionState.ACTIVE,
                source,
                Map.of(),
                Map.of(),
                Map.of());
        assertNotSame(source, model.targetQuantities());
        source.clear();
        assertEquals(1, model.targetQuantities().size());
        assertEquals(4L, model.targetQuantities().get("GOLD_INGOT").quantity());
    }

    @Test
    void currencyRemainsSeparateFromItemMaps() {
        MiningSessionAnalyticsViewModel model = modelWith(
                MiningSessionAnalyticsViewModel.SessionState.STOPPED,
                Map.of(
                        "GOLD_INGOT",
                        quantity("GOLD_INGOT", "Gold Ingot", 2L)),
                Map.of(),
                Map.of(),
                Map.of(
                        "GEMSTONE_POWDER",
                        quantity("GEMSTONE_POWDER", "Gemstone Powder", 10L)));
        assertEquals(1, model.targetQuantities().size());
        assertEquals(1, model.currencyQuantities().size());
        assertFalse(model.currencyQuantities().containsKey("GOLD_INGOT"));
    }

    @Test
    void projectorBuildsActiveEmptyAndStoppedModels() {
        MiningSessionAnalyticsProjector projector =
                new MiningSessionAnalyticsProjector();
        assertEquals(
                MiningSessionAnalyticsViewModel.SessionState.NOT_STARTED,
                projector.project(Optional.empty()).sessionState());

        Fixture fixture = fixture();
        start(fixture, 10L);
        MiningSessionAnalyticsViewModel active =
                projector.project(snapshot(fixture, 11L));
        assertEquals(
                MiningSessionAnalyticsViewModel.SessionState.ACTIVE,
                active.sessionState());
        assertEquals(0, active.entryCount());
        assertEquals(
                TrackerSelection.GOLD.displayName(),
                active.selectedTargetDisplayName());

        appendGold(fixture, 3L, 12L, "gold-1");
        fixture.engine.onDiagnosticStop(20L);
        MiningSessionAnalyticsViewModel stopped =
                projector.project(snapshot(fixture, 99L));
        assertEquals(
                MiningSessionAnalyticsViewModel.SessionState.STOPPED,
                stopped.sessionState());
        assertEquals(1, stopped.targetEntryCount());
        assertTrue(stopped.stoppedMillis().isPresent());
    }

    @Test
    void projectorOrdersQuantitiesDeterministicallyAndOmitsZeros() {
        MiningSessionAnalyticsProjector projector =
                new MiningSessionAnalyticsProjector();
        Fixture fixture = fixture(fixedBook(
                10L,
                Map.of(
                        TrackedMaterial.GOLD.rawBazaarId(),
                        new BigDecimal("2.00"),
                        TrackedMaterial.HARD_STONE.enchantedBazaarId(),
                        new BigDecimal("4.00"))));
        start(fixture, TrackerSelection.RUBY, true, 10L);
        appendOtherHardStone(fixture);
        appendGoldAsOther(fixture);

        MiningSessionAnalyticsViewModel model =
                projector.project(snapshot(fixture, 40L));
        Object[] ids = model.otherMinedQuantities().keySet().toArray();
        assertTrue(ids.length >= 2);
        assertTrue(((String) ids[0]).compareToIgnoreCase((String) ids[1]) <= 0);
        for (MiningSessionAnalyticsViewModel.ResourceQuantity quantity
                : model.otherMinedQuantities().values()) {
            assertTrue(quantity.quantity() > 0L);
        }
    }

    @Test
    void projectorPreservesUnsupportedAndUnresolvedCounts() {
        MiningSessionAnalyticsProjector projector =
                new MiningSessionAnalyticsProjector();
        Fixture fixture = fixture(now -> MiningSessionPriceBook.unavailable());
        start(fixture, 10L);
        appendGold(fixture, 2L, 11L, "gold-1");

        MiningSessionAnalyticsViewModel model =
                projector.project(snapshot(fixture, 12L));
        assertFalse(model.resolvedValueAvailable());
        assertTrue(model.unavailableEntryCount() >= 0);
        assertEquals(1, model.targetEntryCount());
    }

    @Test
    void invalidProjectionFailsClosed() {
        MiningSessionAnalyticsProjector projector =
                new MiningSessionAnalyticsProjector();
        MiningSessionAnalyticsViewModel model = projector.project(
                (MiningSessionSnapshot) null);
        assertEquals(
                MiningSessionAnalyticsViewModel.SessionState.NOT_STARTED,
                model.sessionState());
        assertFalse(model.hasViewableSession());
    }

    @Test
    void viewModelDoesNotExposePrivateIdentifiersInPublicFields() {
        MiningSessionAnalyticsViewModel model = modelWith(
                MiningSessionAnalyticsViewModel.SessionState.ACTIVE,
                Map.of(
                        "GOLD_INGOT",
                        quantity("GOLD_INGOT", "Gold Ingot", 1L)),
                Map.of(),
                Map.of(),
                Map.of());
        String blob = model.selectedTargetDisplayName()
                + model.parityStatusLabel()
                + model.priceBasisLabel()
                + model.targetQuantities().keySet();
        assertFalse(blob.toLowerCase().contains("uuid"));
        assertFalse(blob.toLowerCase().contains("sessionid"));
        assertFalse(blob.toLowerCase().contains("correlation"));
        assertFalse(blob.contains("eventId"));
    }

    private static MiningSessionAnalyticsViewModel modelWith(
            MiningSessionAnalyticsViewModel.SessionState state,
            Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity> target,
            Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity> other,
            Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity> chest,
            Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                    currency) {
        return MiningSessionAnalyticsViewModel.create(
                state,
                "Gold",
                true,
                OptionalLong.of(10L),
                OptionalLong.of(20L),
                state == MiningSessionAnalyticsViewModel.SessionState.STOPPED
                        ? OptionalLong.of(20L)
                        : OptionalLong.empty(),
                target.size(),
                target.size(),
                other.size(),
                chest.size(),
                currency.size(),
                "MATCH",
                0L,
                MiningSessionValuation.PRICE_BASIS_LABEL,
                true,
                new BigDecimal("10.00"),
                1,
                0,
                0,
                0,
                0,
                currency.size(),
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                OptionalLong.of(1_000L),
                true,
                false,
                target,
                other,
                chest,
                currency);
    }

    private static MiningSessionAnalyticsViewModel.ResourceQuantity quantity(
            String id,
            String name,
            long amount) {
        return new MiningSessionAnalyticsViewModel.ResourceQuantity(
                id,
                name,
                amount);
    }

    private static Fixture fixture() {
        return fixture(fixedBook(10L, Map.of(
                TrackedMaterial.GOLD.rawBazaarId(),
                new BigDecimal("2.00"))));
    }

    private static Fixture fixture(MiningSessionPriceProvider provider) {
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
                        provider));
    }

    private static MiningSessionPriceProvider fixedBook(
            long observedAt,
            Map<String, BigDecimal> prices) {
        MiningSessionPriceBook book =
                MiningSessionPriceBook.available(observedAt, prices);
        return nowMillis -> book;
    }

    private static void start(Fixture fixture, long at) {
        start(fixture, TrackerSelection.GOLD, true, at);
    }

    private static void start(
            Fixture fixture,
            TrackerSelection selection,
            boolean enabled,
            long at) {
        fixture.engine.onDiagnosticStart(
                enabled,
                selection,
                baseline(selection, at),
                at);
    }

    private static MiningSessionSnapshot snapshot(Fixture fixture, long at) {
        return fixture.engine.snapshot(at).orElseThrow();
    }

    private static void appendGold(
            Fixture fixture,
            long quantity,
            long at,
            String eventId) {
        fixture.engine.onAcceptedTargetMaterialQuantity(
                TrackedMaterial.GOLD,
                quantity,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                quantity,
                at,
                eventId,
                "test");
    }

    private static void appendOtherHardStone(Fixture fixture) {
        fixture.engine.onConfirmedMaterialBreak(
                TrackedMaterial.HARD_STONE,
                1,
                20L);
        fixture.engine.observeSackChanges(
                java.util.List.of(new SackChangeParser.Change(
                        12L,
                        "Enchanted Hard Stone",
                        java.util.List.of("Enchanted Mining Sack"))),
                25L);
    }

    private static void appendGoldAsOther(Fixture fixture) {
        fixture.engine.onConfirmedMaterialBreak(
                TrackedMaterial.GOLD,
                1,
                30L);
        fixture.engine.observeSackChanges(
                java.util.List.of(new SackChangeParser.Change(
                        3L,
                        "Gold Ingot",
                        java.util.List.of("Mining Sack"))),
                35L);
    }

    private static MiningSessionParity.LiveBaseline baseline(
            TrackerSelection selection,
            long at) {
        if (selection.isGemstone()) {
            Map<GemstoneTier, Long> quantities =
                    new EnumMap<>(GemstoneTier.class);
            for (GemstoneTier tier : GemstoneTier.values()) {
                quantities.put(tier, 0L);
            }
            return MiningSessionParity.LiveBaseline.gemstone(
                    selection,
                    quantities,
                    0L,
                    0L,
                    at);
        }
        Map<TrackedMaterial, Long> quantities =
                new EnumMap<>(TrackedMaterial.class);
        Map<TrackedMaterial, Long> blocks =
                new EnumMap<>(TrackedMaterial.class);
        for (TrackedMaterial material : selection.materialTarget().materials()) {
            quantities.put(material, 0L);
            blocks.put(material, 0L);
        }
        return MiningSessionParity.LiveBaseline.material(
                selection,
                quantities,
                blocks,
                at);
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
