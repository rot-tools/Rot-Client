package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Live Hypixel regression fixtures from tracking-runtime-20260808-132304:
 * target GOLD with real Sack observations. Verifies domain handoff classification
 * and Current Session OTHERS ingest, including GOLD_INGOT target-family protection.
 */
final class SackItemGainHandoffLiveFixtureTest {
    private final MiningResourceCatalog catalog = new MiningResourceCatalog();

    @Test
    void titaniumPlus42_goldTarget_isOtherMinedAndIngested() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.TITANIUM, 1, 100L);

        Outcome outcome = handoff(
                fixture,
                sack("Titanium", 42L, "Dwarven Sack"),
                110L);

        assertEquals("OTHER_MINED", outcome.decision.classification());
        assertEquals("ACCEPTED_OTHER_MINING", outcome.decision.result());
        assertTrue(outcome.decision.ingestOthers());
        assertEquals(42L, otherQty(fixture, "TITANIUM_ORE"));
        assertEquals(42L, sessionOtherQty(fixture, "TITANIUM"));
    }

    @Test
    void mithrilPlus1099_goldTarget_isOtherMined() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.MITHRIL, 1, 100L);

        Outcome outcome = handoff(
                fixture,
                sack("Mithril", 1099L, "Mining Sack"),
                110L);

        assertEquals("OTHER_MINED", outcome.decision.classification());
        assertTrue(outcome.decision.ingestOthers());
        assertEquals(1099L, sessionOtherQty(fixture, "MITHRIL_ORE"));
    }

    @Test
    void diamondPlus765_goldTarget_isOtherMined() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.DIAMOND, 1, 100L);

        Outcome outcome = handoff(
                fixture,
                sack("Diamond", 765L, "Mining Sack"),
                110L);

        assertEquals("OTHER_MINED", outcome.decision.classification());
        assertEquals(765L, sessionOtherQty(fixture, "DIAMOND"));
    }

    @Test
    void cobblestonePlus115_goldTarget_isOtherMined() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(
                TrackedMaterial.COBBLESTONE, 1, 100L);

        Outcome outcome = handoff(
                fixture,
                sack("Cobblestone", 115L, "Mining Sack"),
                110L);

        assertEquals("OTHER_MINED", outcome.decision.classification());
        assertEquals(115L, sessionOtherQty(fixture, "COBBLESTONE"));
    }

    @Test
    void goldIngotPlus1346_goldTarget_isNotOthers() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.GOLD, 1, 100L);

        Optional<MiningResourceCatalog.ResourceDefinition> definition =
                catalog.fromExactSackItem("Gold Ingot");
        assertTrue(SackItemGainHandoff.belongsToCurrentTargetFamily(
                definition, TrackerSelection.GOLD));

        Outcome outcome = handoff(
                fixture,
                sack("Gold Ingot", 1346L, "Mining Sack"),
                110L);

        assertTrue(outcome.decision.targetFamilyProtected());
        assertFalse(outcome.decision.ingestOthers());
        assertEquals("TARGET", outcome.decision.classification());
        assertEquals(
                TrackingRuntimeTrace.Reason.REJECTED_TARGET_AUTHORITY.name(),
                outcome.decision.reason());
        assertEquals(0L, sessionOtherQty(fixture, "GOLD_INGOT"));
        assertEquals(0L, otherQty(fixture, "GOLD_INGOT"));
    }

    @Test
    void roughGemstones_goldTarget_otherMinedWhenMiningAttributionValid() {
        assertRoughGemOther(GemstoneType.AMBER, "Rough Amber Gemstone", 80L);
        assertRoughGemOther(GemstoneType.AMETHYST, "Rough Amethyst Gemstone", 80L);
        assertRoughGemOther(GemstoneType.JADE, "Rough Jade Gemstone", 80L);
        assertRoughGemOther(GemstoneType.SAPPHIRE, "Rough Sapphire Gemstone", 80L);
        assertRoughGemOther(GemstoneType.TOPAZ, "Rough Topaz Gemstone", 80L);
    }

    @Test
    void targetDoubleCountPrevented_goldIngotDoesNotEnterOthersProjection() {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.GOLD, 1, 100L);
        handoff(fixture, sack("Gold Ingot", 1346L, "Mining Sack"), 110L);

        fixture.engine.onConfirmedMaterialBreak(TrackedMaterial.TITANIUM, 1, 200L);
        handoff(fixture, sack("Titanium", 42L, "Dwarven Sack"), 210L);

        MiningHudOtherSummary summary = fixture.session.otherHudSummary();
        assertEquals(42L, summary.totalQuantity());
        assertFalse(summary.totalQuantity() >= 1346L);
    }

    private void assertRoughGemOther(
            GemstoneType gemstone,
            String itemName,
            long qty) {
        Fixture fixture = goldSession();
        fixture.engine.onConfirmedGemstoneBreak(gemstone, 100L);
        Outcome outcome = handoff(
                fixture,
                sack(itemName, qty, "Gemstone Sack"),
                110L);
        assertEquals("OTHER_MINED", outcome.decision.classification(), itemName);
        assertTrue(outcome.decision.ingestOthers(), itemName);
        String resourceId = gemstone.bazaarId(GemstoneTier.ROUGH);
        assertEquals(qty, sessionOtherQty(fixture, resourceId), itemName);
    }

    private Outcome handoff(
            Fixture fixture,
            SackChangeParser.Change change,
            long now) {
        Optional<MiningResourceCatalog.ResourceDefinition> definition =
                catalog.fromExactSackItem(change.itemName());
        MiningSessionShadowObserver.ObservationResult result;
        if (SackItemGainHandoff.belongsToCurrentTargetFamily(
                definition, TrackerSelection.GOLD)) {
            result = fixture.engine.observeSackChange(change, now);
            SackItemGainHandoff.Decision decision =
                    SackItemGainHandoff.fromObservation(result);
            assertTrue(decision.targetFamilyProtected());
            assertFalse(decision.ingestOthers());
            return new Outcome(decision, result);
        }
        result = fixture.engine.observeSackChange(change, now);
        SackItemGainHandoff.Decision decision =
                SackItemGainHandoff.fromObservation(result);
        if (decision.ingestOthers()) {
            String itemId;
            SkyBlockItemIdentityResolver.Resolved resolved =
                    SkyBlockItemIdentityResolver.fromTextToken(change.itemName());
            if (resolved.catalogMatch()) {
                itemId = resolved.stableId();
            } else {
                itemId = definition
                        .map(def -> def.resource().resourceId())
                        .orElse(resolved.stableId());
            }
            fixture.session.creditItem(
                    itemId,
                    change.itemName(),
                    change.delta(),
                    SessionSourceType.MINING,
                    MiningClassification.OTHER,
                    SkyBlockArea.DWARVEN_MINES,
                    RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                    0.0,
                    now);
        }
        return new Outcome(decision, result);
    }

    private static long otherQty(Fixture fixture, String resourceId) {
        return fixture.engine.snapshot(1_000L)
                .map(snap -> {
                    long total = 0L;
                    for (Map.Entry<MiningSessionResource, Long> entry
                            : snap.quantities(MiningSessionCategory.OTHER_MINED)
                            .entrySet()) {
                        if (resourceId.equals(entry.getKey().resourceId())) {
                            total += entry.getValue();
                        }
                    }
                    return total;
                })
                .orElse(0L);
    }

    private static long sessionOtherQty(Fixture fixture, String itemId) {
        if (fixture.session.snapshotConfig().items == null) {
            return 0L;
        }
        return fixture.session.snapshotConfig().items.stream()
                .filter(i -> itemId.equals(i.itemId()))
                .filter(i -> i.source() == SessionSourceType.MINING)
                .filter(i -> i.miningClassification() == MiningClassification.OTHER)
                .mapToLong(RotClientCurrentSessionConfig.SessionItemRecord::quantity)
                .sum();
    }

    private Fixture goldSession() {
        MiningSessionEngine engine = new MiningSessionEngine();
        engine.onDiagnosticStart(
                true,
                TrackerSelection.GOLD,
                MiningSessionParity.LiveBaseline.material(
                        TrackerSelection.GOLD,
                        Map.of(),
                        Map.of(),
                        10L),
                10L);
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.ensureActiveForTracker(TrackerSelection.GOLD, 10L);
        return new Fixture(engine, session);
    }

    private static SackChangeParser.Change sack(
            String itemName,
            long delta,
            String sackName) {
        return new SackChangeParser.Change(delta, itemName, java.util.List.of(sackName));
    }

    private record Fixture(
            MiningSessionEngine engine,
            RotClientCurrentSession session) {
    }

    private record Outcome(
            SackItemGainHandoff.Decision decision,
            MiningSessionShadowObserver.ObservationResult result) {
    }
}
