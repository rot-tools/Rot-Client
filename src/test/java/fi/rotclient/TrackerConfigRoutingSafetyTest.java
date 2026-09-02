package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TrackerConfigRoutingSafetyTest {
    @Test
    void gemstoneSelectionDoesNotExposeGoldForRuntimeRouting() {
        TrackerConfig config = new TrackerConfig();
        config.setSelectedSelection(TrackerSelection.RUBY);

        assertSame(
                TrackingTarget.GOLD,
                config.selectedTarget(),
                "The persisted compatibility fallback must remain intact");
        assertTrue(config.routedMaterials().isEmpty());
        assertFalse(config.routesMaterial(TrackedMaterial.GOLD));
        assertThrows(
                IllegalStateException.class,
                config::requireMaterialTarget);
        assertThrows(
                IllegalStateException.class,
                config::requireSelectedMaterial);
    }

    @Test
    void rubyRoutingCannotReachGoldBlockGainOrSackState() {
        TrackerConfig config = new TrackerConfig();
        MaterialTrackerState gold = config.state(TrackedMaterial.GOLD);
        gold.sessionBlocks = 17L;
        gold.sessionActualRawEquivalent = 2_400L;
        gold.sessionSackRaw = 800L;
        config.enabled = true;
        config.setSelectedSelection(TrackerSelection.RUBY);

        for (TrackedMaterial material : config.routedMaterials()) {
            MaterialTrackerState routed = config.state(material);
            routed.sessionBlocks++;
            routed.sessionActualRawEquivalent++;
            routed.sessionSackRaw++;
        }

        assertFalse(config.routesMaterial(TrackedMaterial.GOLD));
        assertEquals(17L, gold.sessionBlocks);
        assertEquals(2_400L, gold.sessionActualRawEquivalent);
        assertEquals(800L, gold.sessionSackRaw);
    }

    @Test
    void goldSelectionStillRoutesOnlyGold() {
        TrackerConfig config = new TrackerConfig();
        config.setSelectedSelection(TrackerSelection.GOLD);

        assertEquals(
                java.util.List.of(TrackedMaterial.GOLD),
                config.routedMaterials());
        assertTrue(config.routesMaterial(TrackedMaterial.GOLD));
        assertFalse(config.routesMaterial(TrackedMaterial.DIAMOND));
        assertSame(
                TrackingTarget.GOLD,
                config.requireMaterialTarget());
        assertSame(
                TrackedMaterial.GOLD,
                config.requireSelectedMaterial());
    }

    @Test
    void rubyResetDoesNotResetGoldSession() {
        TrackerConfig config = populatedConfig();
        config.setSelectedSelection(TrackerSelection.RUBY);

        config.resetSelectedSessionState();

        assertEquals(
                41L,
                config.state(TrackedMaterial.GOLD).sessionBlocks);
        assertEquals(
                9_600L,
                config.state(TrackedMaterial.GOLD)
                        .sessionActualRawEquivalent);
        assertEquals(
                0L,
                config.gemstoneState(GemstoneType.RUBY).sessionBlocks);
        assertEquals(
                0L,
                config.gemstoneState(GemstoneType.RUBY)
                        .sessionLedger()
                        .quantity(GemstoneTier.ROUGH));
    }

    @Test
    void goldResetDoesNotResetRubySession() {
        TrackerConfig config = populatedConfig();
        config.setSelectedSelection(TrackerSelection.GOLD);

        config.resetSelectedSessionState();

        assertEquals(
                0L,
                config.state(TrackedMaterial.GOLD).sessionBlocks);
        assertEquals(
                0L,
                config.state(TrackedMaterial.GOLD)
                        .sessionActualRawEquivalent);
        assertEquals(
                12L,
                config.gemstoneState(GemstoneType.RUBY).sessionBlocks);
        assertEquals(
                350L,
                config.gemstoneState(GemstoneType.RUBY)
                        .sessionLedger()
                        .quantity(GemstoneTier.ROUGH));
    }

    @Test
    void selectionTransitionsDoNotTransferFamilyState() {
        TrackerConfig config = populatedConfig();

        config.setSelectedSelection(TrackerSelection.RUBY);
        config.setSelectedSelection(TrackerSelection.GOLD);
        config.setSelectedSelection(TrackerSelection.RUBY);

        assertEquals(
                41L,
                config.state(TrackedMaterial.GOLD).sessionBlocks);
        assertEquals(
                9_600L,
                config.state(TrackedMaterial.GOLD)
                        .sessionActualRawEquivalent);
        assertEquals(
                12L,
                config.gemstoneState(GemstoneType.RUBY).sessionBlocks);
        assertEquals(
                350L,
                config.gemstoneState(GemstoneType.RUBY)
                        .sessionLedger()
                        .quantity(GemstoneTier.ROUGH));
    }

    @Test
    void gemstoneFortuneResolutionFailsWithoutMutation() {
        TrackerConfig config = new TrackerConfig();
        config.setSelectedSelection(TrackerSelection.RUBY);
        config.miningFortune = 123.0;
        config.oreFortune = 456.0;
        config.dwarvenMetalFortune = 789.0;
        config.fortuneAuto = true;
        config.fortuneLastDetectedEpochMillis = 321L;
        config.fortuneSource = "existing source";

        assertThrows(
                IllegalStateException.class,
                config::requireSelectedMaterial);

        assertEquals(123.0, config.miningFortune);
        assertEquals(456.0, config.oreFortune);
        assertEquals(789.0, config.dwarvenMetalFortune);
        assertTrue(config.fortuneAuto);
        assertEquals(321L, config.fortuneLastDetectedEpochMillis);
        assertEquals("existing source", config.fortuneSource);
    }

    private static TrackerConfig populatedConfig() {
        TrackerConfig config = new TrackerConfig();
        MaterialTrackerState gold = config.state(TrackedMaterial.GOLD);
        gold.sessionBlocks = 41L;
        gold.sessionActualRawEquivalent = 9_600L;

        GemstoneTrackerState ruby =
                config.gemstoneState(GemstoneType.RUBY);
        ruby.sessionBlocks = 12L;
        ruby.recordGain(GemstoneTier.ROUGH, 350L);
        return config;
    }
}
