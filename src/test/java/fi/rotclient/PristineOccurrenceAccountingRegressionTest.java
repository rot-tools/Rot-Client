package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

/**
 * Live regression: two legitimate non-target Pristine messages with identical
 * item/tier/quantity must both credit Current Session when they are distinct
 * message occurrences. Same occurrence replay must remain at-most-once.
 */
final class PristineOccurrenceAccountingRegressionTest {
    private static final String PRISTINE_TOPAZ_X13 =
            "PRISTINE! You found Flawed Topaz Gemstone x13!";

    @Test
    void twoIdenticalPristineAmountsAtDifferentOccurrencesBothCredit() {
        MiningSessionEngine engine = activeEngine(TrackerSelection.AMBER);
        RotClientCurrentSession current = new RotClientCurrentSession();

        engine.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 1_000L);
        PristineComponentIngress.Parsed firstMessage =
                PristineComponentIngress.parse(
                        Component.literal(PRISTINE_TOPAZ_X13)).orElseThrow();
        assertTrue(PristineItemGainPipeline.submit(
                engine,
                current,
                firstMessage.reward(),
                SkyBlockArea.CRYSTAL_HOLLOWS,
                1_100L,
                firstMessage.deliveryIdentity()).creditedOthers());
        assertEquals(13L, flawedTopazQuantity(current));

        engine.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 65_000L);
        PristineComponentIngress.Parsed secondMessage =
                PristineComponentIngress.parse(
                        Component.literal(PRISTINE_TOPAZ_X13)).orElseThrow();
        assertTrue(PristineItemGainPipeline.submit(
                engine,
                current,
                secondMessage.reward(),
                SkyBlockArea.CRYSTAL_HOLLOWS,
                65_100L,
                secondMessage.deliveryIdentity()).creditedOthers());

        assertFalse(firstMessage.deliveryIdentity().equals(
                secondMessage.deliveryIdentity()));
        assertEquals(26L, flawedTopazQuantity(current));
        assertEquals(1, current.snapshotConfig().items.size());
        assertEquals(MiningClassification.OTHER,
                current.snapshotConfig().items.getFirst().miningClassification());
    }

    @Test
    void samePristineOccurrenceReplayCreditsOnce() {
        MiningSessionEngine engine = activeEngine(TrackerSelection.AMBER);
        RotClientCurrentSession current = new RotClientCurrentSession();
        engine.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 1_000L);
        engine.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 1_001L);

        Component message = Component.literal(PRISTINE_TOPAZ_X13);
        PristineComponentIngress.Parsed first =
                PristineComponentIngress.parse(message).orElseThrow();
        PristineComponentIngress.Parsed replay =
                PristineComponentIngress.parse(message).orElseThrow();
        assertEquals(first.deliveryIdentity(), replay.deliveryIdentity());

        assertTrue(PristineItemGainPipeline.submit(
                engine,
                current,
                first.reward(),
                SkyBlockArea.CRYSTAL_HOLLOWS,
                1_100L,
                first.deliveryIdentity()).creditedOthers());
        assertFalse(PristineItemGainPipeline.submit(
                engine,
                current,
                replay.reward(),
                SkyBlockArea.CRYSTAL_HOLLOWS,
                1_101L,
                replay.deliveryIdentity()).creditedOthers());

        assertEquals(13L, flawedTopazQuantity(current));
        // Two break contexts remain: the consumed one still has its Sack
        // channel, and the unused sibling still has both channels.
        assertEquals(2, engine.pendingOtherContextCount());
    }

    @Test
    void pausedPristineDoesNotCredit() {
        MiningSessionEngine engine = activeEngine(TrackerSelection.AMBER);
        RotClientCurrentSession current = new RotClientCurrentSession();
        current.pause(90L);
        engine.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        PristineComponentIngress.Parsed parsed =
                PristineComponentIngress.parse(
                        Component.literal(PRISTINE_TOPAZ_X13)).orElseThrow();

        assertFalse(PristineItemGainPipeline.submit(
                engine,
                current,
                parsed.reward(),
                SkyBlockArea.CRYSTAL_HOLLOWS,
                110L,
                parsed.deliveryIdentity()).creditedOthers());
        assertTrue(current.snapshotConfig().items.isEmpty());
    }

    @Test
    void targetPristineDoesNotLeakIntoOthers() {
        MiningSessionEngine engine = activeEngine(TrackerSelection.TOPAZ);
        RotClientCurrentSession current = new RotClientCurrentSession();
        engine.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        PristineComponentIngress.Parsed parsed =
                PristineComponentIngress.parse(
                        Component.literal(PRISTINE_TOPAZ_X13)).orElseThrow();

        assertFalse(PristineItemGainPipeline.submit(
                engine,
                current,
                parsed.reward(),
                SkyBlockArea.CRYSTAL_HOLLOWS,
                110L,
                parsed.deliveryIdentity()).creditedOthers());
        assertTrue(current.snapshotConfig().items.isEmpty());
        assertEquals(0, engine.snapshot(120L).orElseThrow().entryCount());
    }

    @Test
    void pristineThenSackConfirmationDoesNotDoubleCredit() {
        MiningSessionEngine engine = activeEngine(TrackerSelection.AMBER);
        RotClientCurrentSession current = new RotClientCurrentSession();
        engine.onConfirmedGemstoneBreak(GemstoneType.TOPAZ, 100L);
        PristineComponentIngress.Parsed parsed =
                PristineComponentIngress.parse(
                        Component.literal(PRISTINE_TOPAZ_X13)).orElseThrow();

        assertTrue(PristineItemGainPipeline.submit(
                engine,
                current,
                parsed.reward(),
                SkyBlockArea.CRYSTAL_HOLLOWS,
                110L,
                parsed.deliveryIdentity()).creditedOthers());

        MiningSessionShadowObserver.ObservationResult sack =
                engine.observeSackChange(
                        new SackChangeParser.Change(
                                13L,
                                "Flawed Topaz Gemstone",
                                java.util.List.of("Gemstone Sack")),
                        180L,
                        0L,
                        "sack-confirm-after-pristine");

        assertEquals(
                MiningSessionClassification.ReasonCode.UNSUPPORTED_EVIDENCE,
                sack.reason());
        assertFalse(sack.appended());
        assertEquals(13L, flawedTopazQuantity(current));
        assertEquals(1, engine.snapshot(200L).orElseThrow().entryCount());
    }

    private static long flawedTopazQuantity(RotClientCurrentSession current) {
        return current.snapshotConfig().items.stream()
                .filter(row -> "FLAWED_TOPAZ_GEM".equals(row.itemId()))
                .mapToLong(RotClientCurrentSessionConfig.SessionItemRecord::quantity)
                .sum();
    }

    private static MiningSessionEngine activeEngine(TrackerSelection selection) {
        MiningSessionEngine engine = new MiningSessionEngine();
        engine.onDiagnosticStart(
                true,
                selection,
                MiningSessionParity.LiveBaseline.gemstone(
                        selection, Map.of(), 0L, 0L, 10L),
                10L);
        return engine;
    }
}
