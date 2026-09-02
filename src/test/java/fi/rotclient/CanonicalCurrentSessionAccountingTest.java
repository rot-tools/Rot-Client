package fi.rotclient;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Canonical Current Session accounting integrity suite.
 */
final class CanonicalCurrentSessionAccountingTest {
    private final MiningResourceCatalog catalog = new MiningResourceCatalog();

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void clearTrace() {
        TrackingRuntimeTrace.clearStatusMemory();
    }

    @Test
    void persistedCurrentSessionSurvivesEngineRestartAndFirstNewEvent() {
        Fixture fixture = goldSession();
        creditDirect(fixture, "COBBLESTONE", "Cobblestone", 5_000L, 100L);
        creditDirect(fixture, "DIAMOND", "Diamond", 2_000L, 110L);
        creditDirect(fixture, "MITHRIL", "Mithril", 800L, 120L);

        fixture.engine.clearAnalyticsSession(200L);
        fixture.engine.onDiagnosticStart(
                true,
                TrackerSelection.GOLD,
                baseline(TrackerSelection.GOLD, 210L),
                210L);
        fixture.collectionActive.set(true);

        fixture.session.syncFromSnapshot(
                fixture.engine.snapshot(220L).orElseThrow(),
                TrackerSelection.GOLD,
                220L);
        assertEquals(5_000L, sessionQty(fixture, "COBBLESTONE"));
        assertEquals(2_000L, sessionQty(fixture, "DIAMOND"));
        assertEquals(800L, sessionQty(fixture, "MITHRIL"));

        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.TITANIUM, 1, 300L);
        assertTrue(submit(
                fixture, sack("Titanium", 12L, "Mining Sack"), 310L, 10_000L)
                .creditedOthers());

        assertEquals(5_000L, sessionQty(fixture, "COBBLESTONE"));
        assertEquals(2_000L, sessionQty(fixture, "DIAMOND"));
        assertEquals(800L, sessionQty(fixture, "MITHRIL"));
        assertEquals(12L, sessionQty(fixture, "TITANIUM"));
    }

    @Test
    void analyticsAndHudShareCanonicalOtherMined() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 100L);
        submit(fixture, sack("Diamond", 40L, "Mining Sack"), 110L, 24_000L);

        MiningSessionAnalyticsViewModel analytics =
                new MiningSessionAnalyticsProjector().project(
                        fixture.engine.snapshot(1_000L),
                        fixture.session.snapshotConfig());
        MiningHudOtherSummary hud = fixture.session.otherHudSummary();

        assertEquals(40L, analytics.otherMinedQuantities().get("DIAMOND").quantity());
        assertEquals(40L, hud.totalQuantity());
    }

    @Test
    void resetNeverLeavesCollectionSilentlyOffWhileSessionRunning() {
        Fixture fixture = goldSession();
        assertTrue(fixture.session.isActive());
        assertTrue(fixture.engine.isCollectionActive());

        fixture.session.creditItem(
                "DIAMOND", "Diamond", 10L,
                SessionSourceType.MINING, MiningClassification.OTHER,
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0, 50L);

        MiningSessionAnalyticsController controller =
                new MiningSessionAnalyticsController(
                        fixture.engine,
                        text -> {
                        },
                        (selection, now) -> baseline(selection, now),
                        () -> fixture.clock.get());
        controller.reset();
        fixture.session.clearCreditedItems(fixture.clock.get());

        if (fixture.session.isActive()
                && !fixture.engine.isCollectionActive()) {
            fixture.engine.onDiagnosticStart(
                    true,
                    TrackerSelection.GOLD,
                    baseline(TrackerSelection.GOLD, fixture.clock.get()),
                    fixture.clock.get());
        }

        assertTrue(fixture.session.isActive());
        assertTrue(fixture.engine.isCollectionActive());
        assertEquals(0L, sessionQty(fixture, "DIAMOND"));
    }

    @Test
    void realComponentHoverIngress_last24s_mithrilOnce() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.MITHRIL, 1, 100L);

        Component message = sackSystemMessage(
                "[Sacks] +64 items (Last 24s.)",
                "Added items:\n+64 Mithril (Mining Sack)");

        SackComponentIngress.Batch batch = SackComponentIngress.parse(message)
                .orElseThrow();
        assertEquals(24_000L, batch.coveredBatchMillis());
        assertEquals(1, batch.parsedHovers().size());
        List<SackChangeParser.Change> changes = batch.parsedHovers()
                .getFirst()
                .parseResult()
                .changes();
        assertEquals(1, changes.size());

        SackItemGainPipeline.Outcome outcome = submit(
                fixture, changes.getFirst(), 110L, batch.coveredBatchMillis());
        assertTrue(outcome.creditedOthers());
        assertEquals("OTHER_MINED", outcome.decision().classification());
        assertEquals("MITHRIL", outcome.creditedItemId());
        assertEquals(64L, sessionQty(fixture, "MITHRIL"));
    }

    @Test
    void oneSackComponentCanCreditSeveralIndependentMinedResources() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.COAL, 15, 100L);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 17, 101L);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.IRON, 16, 102L);

        String componentIdentity = "sack-component:runtime-multi-item";
        assertTrue(submit(
                fixture,
                sack("Coal", 1_170L, "Mining Sack"),
                110L,
                24_000L,
                componentIdentity).creditedOthers());
        assertTrue(submit(
                fixture,
                sack("Diamond", 1_455L, "Mining Sack"),
                110L,
                24_000L,
                componentIdentity).creditedOthers());
        assertTrue(submit(
                fixture,
                sack("Iron Ingot", 1_055L, "Mining Sack"),
                110L,
                24_000L,
                componentIdentity).creditedOthers());

        assertEquals(1_170L, sessionOtherQty(fixture, "COAL"));
        assertEquals(1_455L, sessionOtherQty(fixture, "DIAMOND"));
        assertEquals(1_055L, sessionOtherQty(fixture, "IRON"));
    }

    @Test
    void replayedSackComponentStillCreditsOneMaterialOnlyOnce() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 100L);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 101L);

        String componentIdentity = "sack-component:replayed";
        assertTrue(submit(
                fixture,
                sack("Diamond", 64L, "Mining Sack"),
                110L,
                24_000L,
                componentIdentity).creditedOthers());
        SackItemGainPipeline.Outcome replay = submit(
                fixture,
                sack("Diamond", 64L, "Mining Sack"),
                111L,
                24_000L,
                componentIdentity);

        assertFalse(replay.creditedOthers());
        assertEquals(
                TrackingRuntimeTrace.Reason.REJECTED_DUPLICATE.name(),
                replay.decision().reason());
        assertEquals(64L, sessionOtherQty(fixture, "DIAMOND"));
    }

    @Test
    void redstoneDustTargetSackDoesNotCreateUnattributedDuplicate() {
        Fixture fixture = session(TrackerSelection.REDSTONE);
        fixture.session.creditItem(
                "REDSTONE",
                "Redstone",
                3_465L,
                SessionSourceType.MINING,
                MiningClassification.TARGET,
                SkyBlockArea.DWARVEN_MINES,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0,
                100L);

        SackItemGainPipeline.Outcome outcome = submit(
                fixture,
                sack("Redstone Dust", 3_465L, "Mining Sack"),
                110L,
                24_000L,
                "sack-component:redstone-runtime");

        assertFalse(outcome.creditedOthers());
        assertTrue(outcome.decision().targetFamilyProtected());
        assertEquals(3_465L, sessionQty(fixture, "REDSTONE"));
        assertEquals(1L, fixture.session.snapshotConfig().items.stream()
                .filter(item -> item != null
                        && "REDSTONE".equals(item.itemId()))
                .count());
        assertFalse(fixture.session.snapshotConfig().items.stream()
                .anyMatch(item -> item != null
                        && "REDSTONE".equals(item.itemId())
                        && item.source() == SessionSourceType.UNATTRIBUTED));
    }

    @Test
    void nonSackComponentNeverEntersTheProductionSackBoundary() {
        assertTrue(SackComponentIngress.parse(
                Component.literal("+64 Mithril")).isEmpty());
    }

    @Test
    void enchantedBlockKeepsExactCanonicalAndBazaarIdentity() {
        MiningResourceCatalog.ResourceDefinition definition = catalog
                .fromExactSackItem("Enchanted Gold Block")
                .orElseThrow();
        String canonical = CurrentSessionCanonicalIds.canonicalItemId(
                definition.resource());

        assertEquals("ENCHANTED_GOLD_BLOCK", canonical);
        assertEquals(
                "ENCHANTED_GOLD_BLOCK",
                CurrentSessionCanonicalIds.bazaarProductId(canonical)
                        .orElseThrow());
    }

    @Test
    void goldTargetFamilyExcludedFromOthers() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.GOLD, 1, 100L);
        assertFalse(submit(
                fixture, sack("Gold Ingot", 10L, "Mining Sack"), 110L, 10_000L)
                .creditedOthers());
        assertFalse(submit(
                fixture, sack("Enchanted Gold", 1L, "Mining Sack"), 120L, 10_000L)
                .creditedOthers());
        assertEquals(0L, sessionOtherQty(fixture, "GOLD"));
        assertEquals(0L, sessionOtherQty(fixture, "ENCHANTED_GOLD"));
    }

    @Test
    void cobbleDiamondMithrilTitaniumAcceptedAsOtherWithEvidence() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.COBBLESTONE, 1, 100L);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 101L);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.MITHRIL, 1, 102L);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.TITANIUM, 1, 103L);
        assertTrue(submit(fixture, sack("Cobblestone", 5L, "Mining Sack"), 110L, 10_000L)
                .creditedOthers());
        assertTrue(submit(fixture, sack("Diamond", 6L, "Mining Sack"), 111L, 10_000L)
                .creditedOthers());
        assertTrue(submit(fixture, sack("Mithril", 7L, "Mining Sack"), 112L, 10_000L)
                .creditedOthers());
        assertTrue(submit(fixture, sack("Titanium", 8L, "Mining Sack"), 113L, 10_000L)
                .creditedOthers());
        assertEquals(5L, sessionOtherQty(fixture, "COBBLESTONE"));
        assertEquals(6L, sessionOtherQty(fixture, "DIAMOND"));
        assertEquals(7L, sessionOtherQty(fixture, "MITHRIL"));
        assertEquals(8L, sessionOtherQty(fixture, "TITANIUM"));
    }

    @Test
    void unknownStableIdRetainedWithoutCatalogMapping() {
        Fixture fixture = goldSession();
        SackItemGainPipeline.Outcome outcome = submit(
                fixture,
                sack("Weird Stable Ore", 3L, "Mining Sack"),
                110L,
                10_000L);
        assertFalse(outcome.creditedOthers());
        assertEquals("UNATTRIBUTED", outcome.decision().classification());
        String id = SkyBlockItemId.normalize("Weird Stable Ore");
        assertEquals(3L, sessionQty(fixture, id));
        RotClientCurrentSessionConfig.SessionItemRecord row =
                fixture.session.snapshotConfig().items.stream()
                        .filter(i -> id.equals(i.itemId()))
                        .findFirst()
                        .orElseThrow();
        assertEquals(SessionSourceType.UNATTRIBUTED, row.source());
        assertNotEquals("HARD_STONE", row.itemId());
    }

    @Test
    void crossChannelActionBarInventoryDelayedSackCreditsOnce() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.HARD_STONE, 1, 100L);

        assertTrue(fixture.engine.onMaterialInventoryGain(
                TrackedMaterial.HARD_STONE, 64L, 110L).appended());
        fixture.session.creditItem(
                "HARD_STONE", "Hard Stone", 64L,
                SessionSourceType.MINING, MiningClassification.OTHER,
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0, 110L);

        assertFalse(fixture.engine.onMaterialInventoryGain(
                TrackedMaterial.HARD_STONE, 64L, 200L).appended());
        assertFalse(submit(
                fixture, sack("Hard Stone", 64L, "Mining Sack"), 4_000L, 24_000L)
                .creditedOthers());
        assertEquals(64L, sessionOtherQty(fixture, "HARD_STONE"));
    }

    @Test
    void twoGenuineEqualGainsCloseTogetherBothSurvive() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 100L);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 105L);
        assertTrue(submit(fixture, sack("Diamond", 10L, "Mining Sack"), 110L, 10_000L)
                .creditedOthers());
        // Identical qty+source within 2.5s is suppressed (no Hypixel packet id).
        // Distinct quantity is sufficient separate-gain evidence inside the window.
        assertTrue(submit(fixture, sack("Diamond", 11L, "Mining Sack"), 120L, 10_000L)
                .creditedOthers());
        assertEquals(21L, sessionOtherQty(fixture, "DIAMOND"));
    }

    @Test
    void twoGenuineEqualGainsAfterFingerprintWindowBothSurvive() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 100L);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 105L);
        assertTrue(submit(fixture, sack("Diamond", 10L, "Mining Sack"), 110L, 10_000L)
                .creditedOthers());
        assertTrue(submit(fixture, sack("Diamond", 10L, "Mining Sack"), 3_000L, 10_000L)
                .creditedOthers());
        assertEquals(20L, sessionOtherQty(fixture, "DIAMOND"));
    }

    @Test
    void mixedResolvedUnresolvedValuationIsPerResource() {
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.creditItem(
                "DIAMOND", "Diamond", 10L,
                SessionSourceType.MINING, MiningClassification.OTHER,
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                RotClientCurrentSessionConfig.PriceStatus.RESOLVED_BAZAAR,
                500.0, 10L);
        session.creditItem(
                "WEIRD_STABLE_ORE", "Weird Stable Ore", 5L,
                SessionSourceType.MINING, MiningClassification.OTHER,
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0, 11L);

        MiningSessionPriceBook book = MiningSessionPriceBook.available(
                20L,
                Map.of("DIAMOND", BigDecimal.valueOf(50)));
        session.refreshPerResourceValuation(book, 20L);

        RotClientCurrentSessionConfig cfg = session.snapshotConfig();
        RotClientCurrentSessionConfig.SessionItemRecord diamond = cfg.items.stream()
                .filter(i -> "DIAMOND".equals(i.itemId()))
                .findFirst()
                .orElseThrow();
        RotClientCurrentSessionConfig.SessionItemRecord unknown = cfg.items.stream()
                .filter(i -> "WEIRD_STABLE_ORE".equals(i.itemId()))
                .findFirst()
                .orElseThrow();
        assertEquals(
                RotClientCurrentSessionConfig.PriceStatus.RESOLVED_BAZAAR,
                diamond.price());
        assertEquals(500.0, diamond.resolvedGrossValue(), 0.001);
        assertEquals(
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                unknown.price());
        assertEquals(0.0, unknown.resolvedGrossValue(), 0.0);
        assertEquals(5L, unknown.quantity());
    }

    @Test
    void titaniumCanonicalIdentityStableAcrossSurfaces() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.TITANIUM, 1, 100L);
        SackItemGainPipeline.Outcome outcome = submit(
                fixture, sack("Titanium", 9L, "Mining Sack"), 110L, 10_000L);
        assertEquals("TITANIUM", outcome.creditedItemId());
        assertEquals(9L, sessionOtherQty(fixture, "TITANIUM"));

        MiningSessionAnalyticsViewModel analytics =
                new MiningSessionAnalyticsProjector().project(
                        fixture.engine.snapshot(200L),
                        fixture.session.snapshotConfig());
        assertTrue(analytics.otherMinedQuantities().containsKey("TITANIUM"));
        assertFalse(analytics.otherMinedQuantities().containsKey("TITANIUM_ORE"));
        assertEquals(9L, fixture.session.otherHudSummary().totalQuantity());

        fixture.engine.clearAnalyticsSession(300L);
        fixture.session.syncFromSnapshot(null, TrackerSelection.GOLD, 310L);
        assertEquals(9L, sessionOtherQty(fixture, "TITANIUM"));
    }

    @Test
    void targetSwitchDelayedSackDoesNotReclassifyAcceptedRows() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.GOLD, 1, 100L);
        SackItemGainPipeline.Outcome gold = submit(
                fixture, sack("Gold Ingot", 20L, "Mining Sack"), 110L, 10_000L);
        assertFalse(gold.creditedOthers());
        assertTrue(gold.decision().targetFamilyProtected());

        fixture.engine.onSelectionChanged(
                TrackerSelection.DIAMOND,
                true,
                baseline(TrackerSelection.DIAMOND, 200L),
                200L);
        fixture.selection.set(TrackerSelection.DIAMOND);

        SackItemGainPipeline.Outcome delayed = submit(
                fixture, sack("Gold Ingot", 20L, "Mining Sack"), 250L, 24_000L);
        assertFalse(delayed.creditedOthers());
        assertEquals(0L, sessionOtherQty(fixture, "GOLD"));
    }

    @Test
    void firstDeliveryAfterTargetSwitchCannotUsePriorTargetBreakEvidence() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.GOLD, 1, 100L);

        fixture.engine.onSelectionChanged(
                TrackerSelection.DIAMOND,
                true,
                baseline(TrackerSelection.DIAMOND, 200L),
                200L);
        fixture.selection.set(TrackerSelection.DIAMOND);

        SackItemGainPipeline.Outcome delayed = submit(
                fixture, sack("Gold Ingot", 20L, "Mining Sack"), 250L, 24_000L);

        assertFalse(delayed.creditedOthers());
        assertEquals("UNATTRIBUTED", delayed.decision().classification());
        assertEquals(0L, sessionOtherQty(fixture, "GOLD"));
        assertEquals(0L, sessionQty(fixture, "GOLD"));
    }

    @Test
    void targetAutoPauseWhileCurrentSessionRunningStillCollects() {
        Fixture fixture = goldSession();
        fixture.collectionActive.set(true);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.MITHRIL, 1, 100L);
        assertTrue(submit(
                fixture, sack("Mithril", 7L, "Mining Sack"), 110L, 24_000L)
                .creditedOthers());
        assertTrue(fixture.session.isActive());
    }

    @Test
    void explicitCurrentSessionPauseBlocksCollection() {
        Fixture fixture = goldSession();
        fixture.session.pause(50L);
        fixture.collectionActive.set(false);
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 100L);
        assertFalse(submit(
                fixture, sack("Diamond", 50L, "Mining Sack"), 110L, 24_000L)
                .creditedOthers());
    }

    @Test
    void hardStoneBlockEvidenceOnlyDoesNotFabricateQuantity() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(
                TrackedMaterial.HARD_STONE, 1, 100L);
        assertEquals(0L, sessionOtherQty(fixture, "HARD_STONE"));
        HardStoneQuantitySignals.Assessment assessment =
                HardStoneQuantitySignals.assess(true, false, false, false);
        assertEquals(HardStoneQuantitySignals.Signal.NONE, assessment.quantitySignal());
    }

    @Test
    void hardStoneActualQuantityUnderMaterialTarget() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(
                TrackedMaterial.HARD_STONE, 1, 100L);
        assertTrue(submit(
                fixture, sack("Hard Stone", 32L, "Mining Sack"), 110L, 10_000L)
                .creditedOthers());
        assertEquals(32L, sessionOtherQty(fixture, "HARD_STONE"));
    }

    @Test
    void hardStoneActualQuantityUnderGemstoneTarget() {
        Fixture fixture = gemstoneSession(TrackerSelection.RUBY);
        fixture.engine.onConfirmedMaterialBreak(
                TrackedMaterial.HARD_STONE, 1, 100L);
        assertTrue(submit(
                fixture, sack("Hard Stone", 48L, "Mining Sack"), 110L, 10_000L)
                .creditedOthers());
        assertEquals(48L, sessionOtherQty(fixture, "HARD_STONE"));
    }

    @Test
    void ordinaryStoneDoesNotBecomeHardStone() {
        assertEquals(
                "STONE",
                SkyBlockItemIdentityResolver.fromTextToken("Stone").stableId());
        assertNotEquals(
                "HARD_STONE",
                SkyBlockItemIdentityResolver.fromTextToken("Stone").stableId());
    }

    @Test
    void diagnosticsOnOffAccountingParity() {
        Fixture on = goldSession();
        Fixture off = goldSession();
        on.engine.onConfirmedMaterialBreak(TrackedMaterial.COBBLESTONE, 1, 100L);
        off.engine.onConfirmedMaterialBreak(TrackedMaterial.COBBLESTONE, 1, 100L);

        TrackingRuntimeTrace.clearStatusMemory();
        assertTrue(submit(off, sack("Cobblestone", 11L, "Mining Sack"), 110L, 10_000L)
                .creditedOthers());
        TrackingRuntimeTrace.seedUniqueItemForTest(
                new TrackingRuntimeTrace.ObservedGain(
                        "SACK_ITEM_GAIN",
                        TrackingRuntimeTrace.ObservationPath.SACK_MESSAGE,
                        "Cobblestone",
                        "Cobblestone",
                        "",
                        "",
                        11L,
                        "MINING",
                        "OTHER",
                        "ACCEPTED",
                        TrackingRuntimeTrace.Reason.ACCEPTED_OTHER_MINING.name(),
                        "",
                        "sack",
                        SkyBlockItemIdentityResolver.fromTextToken("Cobblestone")));
        assertTrue(submit(on, sack("Cobblestone", 11L, "Mining Sack"), 110L, 10_000L)
                .creditedOthers());

        assertEquals(
                sessionOtherQty(on, "COBBLESTONE"),
                sessionOtherQty(off, "COBBLESTONE"));
        assertEquals(
                on.engine.snapshot(200L)
                        .map(s -> s.totalItemQuantity(MiningSessionCategory.OTHER_MINED))
                        .orElse(0L),
                off.engine.snapshot(200L)
                        .map(s -> s.totalItemQuantity(MiningSessionCategory.OTHER_MINED))
                        .orElse(0L));
    }

    private static void creditDirect(
            Fixture fixture,
            String id,
            String name,
            long qty,
            long now) {
        fixture.session.creditItem(
                id,
                name,
                qty,
                SessionSourceType.MINING,
                MiningClassification.OTHER,
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0,
                now);
    }

    private static Component sackSystemMessage(String plain, String hover) {
        Component hoverComponent = Component.literal(hover);
        Style style = Style.EMPTY.withHoverEvent(
                new HoverEvent.ShowText(hoverComponent));
        return Component.literal(plain).setStyle(style);
    }

    private static SackChangeParser.Change sack(
            String item,
            long delta,
            String sackName) {
        return new SackChangeParser.Change(delta, item, List.of(sackName));
    }

    private SackItemGainPipeline.Outcome submit(
            Fixture fixture,
            SackChangeParser.Change change,
            long now,
            long coveredBatchMillis) {
        return submit(fixture, change, now, coveredBatchMillis, "");
    }

    private SackItemGainPipeline.Outcome submit(
            Fixture fixture,
            SackChangeParser.Change change,
            long now,
            long coveredBatchMillis,
            String deliveryIdentity) {
        fixture.clock.set(now);
        return SackItemGainPipeline.submit(
                fixture.context(catalog),
                change,
                coveredBatchMillis,
                deliveryIdentity);
    }

    private static long sessionQty(Fixture fixture, String itemId) {
        String id = SkyBlockItemId.normalize(itemId);
        return fixture.session.snapshotConfig().items.stream()
                .filter(i -> i != null && id.equals(i.itemId()))
                .mapToLong(RotClientCurrentSessionConfig.SessionItemRecord::quantity)
                .sum();
    }

    private static long sessionOtherQty(Fixture fixture, String itemId) {
        String id = SkyBlockItemId.normalize(itemId);
        return fixture.session.snapshotConfig().items.stream()
                .filter(i -> i != null && id.equals(i.itemId()))
                .filter(i -> i.source() == SessionSourceType.MINING)
                .filter(i -> i.miningClassification() == MiningClassification.OTHER)
                .mapToLong(RotClientCurrentSessionConfig.SessionItemRecord::quantity)
                .sum();
    }

    private static MiningSessionParity.LiveBaseline baseline(
            TrackerSelection selection,
            long now) {
        if (selection.isGemstone()) {
            return MiningSessionParity.LiveBaseline.gemstone(
                    selection,
                    Map.of(),
                    0L,
                    0L,
                    now);
        }
        return MiningSessionParity.LiveBaseline.material(
                selection,
                Map.of(),
                Map.of(),
                now);
    }

    private Fixture goldSession() {
        return session(TrackerSelection.GOLD);
    }

    private Fixture gemstoneSession(TrackerSelection selection) {
        return session(selection);
    }

    private Fixture session(TrackerSelection selection) {
        MiningSessionEngine engine = new MiningSessionEngine();
        engine.onDiagnosticStart(
                true,
                selection,
                baseline(selection, 10L),
                10L);
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.ensureActiveForTracker(selection, 10L);
        return new Fixture(engine, session, selection);
    }

    private static final class Fixture {
        private final MiningSessionEngine engine;
        private final RotClientCurrentSession session;
        private final AtomicBoolean collectionActive = new AtomicBoolean(true);
        private final AtomicLong clock = new AtomicLong(10L);
        private final AtomicReference<TrackerSelection> selection;

        private Fixture(
                MiningSessionEngine engine,
                RotClientCurrentSession session,
                TrackerSelection initial) {
            this.engine = engine;
            this.session = session;
            this.selection = new AtomicReference<>(initial);
        }

        private SackItemGainPipeline.Context context(MiningResourceCatalog catalog) {
            return new SackItemGainPipeline.Context(
                    engine,
                    session,
                    catalog,
                    selection::get,
                    () -> SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                    collectionActive::get,
                    clock::get);
        }
    }
}
