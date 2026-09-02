package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Unique-item summary formatting must preserve unknown identities and
 * expose observation paths without requiring Fabric enable().
 */
final class TrackingUniqueItemSummaryTest {
    @AfterEach
    void tearDown() {
        TrackingRuntimeTrace.clearStatusMemory();
    }

    @Test
    void summaryIncludesUnknownAndKnownItems() {
        SkyBlockItemIdentityResolver.Resolved known =
                SkyBlockItemIdentityResolver.fromTextToken("Hard Stone");
        SkyBlockItemIdentityResolver.Resolved unknown =
                SkyBlockItemIdentityResolver.fromTextToken("Brand New Drop");

        TrackingRuntimeTrace.seedUniqueItemForTest(
                new TrackingRuntimeTrace.ObservedGain(
                        "INVENTORY_ITEM_GAIN",
                        TrackingRuntimeTrace.ObservationPath.INVENTORY_DELTA,
                        "Hard Stone",
                        "Hard Stone",
                        "minecraft:stone",
                        "",
                        128L,
                        "MINING",
                        "OTHER",
                        "ACCEPTED",
                        TrackingRuntimeTrace.Reason.ACCEPTED_OTHER_MINING.name(),
                        "",
                        "inventory",
                        known));
        TrackingRuntimeTrace.seedUniqueItemForTest(
                new TrackingRuntimeTrace.ObservedGain(
                        "ACTIONBAR_ITEM_GAIN",
                        TrackingRuntimeTrace.ObservationPath.ACTIONBAR,
                        "Brand New Drop",
                        "Brand New Drop",
                        "minecraft:paper",
                        "",
                        3L,
                        "MINING",
                        "REJECTED",
                        "REJECTED",
                        TrackingRuntimeTrace.Reason.REJECTED_NO_MINING_EVIDENCE
                                .name(),
                        "",
                        "actionbar",
                        unknown));

        String summary = TrackingRuntimeTrace.formatUniqueSummaryForTest();
        assertTrue(summary.contains("schema="));
        assertTrue(summary.contains("unique_items=2"));
        assertTrue(summary.contains("DISPLAY NAME=Hard Stone"));
        assertTrue(summary.contains("RESOLVED ID=HARD_STONE"));
        assertTrue(summary.contains("CATALOG MATCH=YES"));
        assertTrue(summary.contains("DISPLAY NAME=Brand New Drop"));
        assertTrue(summary.contains("CATALOG MATCH=NO"));
        assertTrue(summary.contains("HYPIXEL ID=UNKNOWN"));
        assertTrue(summary.contains("SIGNAL SOURCES=INVENTORY_DELTA")
                || summary.contains("INVENTORY_DELTA"));
        assertTrue(summary.contains("ACTIONBAR"));
        assertFalse(summary.contains("password"));
    }

    @Test
    void duplicateReasonCodeExists() {
        assertTrue(TrackingRuntimeTrace.Reason.REJECTED_DUPLICATE.name()
                .contains("DUPLICATE"));
        assertTrue(TrackingRuntimeTrace.ObservationPath.SACK_MESSAGE.name()
                .contains("SACK"));
    }
}
