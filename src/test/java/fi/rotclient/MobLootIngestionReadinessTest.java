package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class MobLootIngestionReadinessTest {
    @Test
    void reportsBoundedLiveCoverageIncludingGenericDianaDrops() {
        assertTrue(MobLootIngestionReadiness.LIVE_INGESTION_ENABLED);
        assertTrue(MobLootIngestionReadiness.DIANA_IN_SCOPE);
        String report = MobLootIngestionReadiness.report();
        assertTrue(report.contains("BOUNDED_LIVE"));
        assertTrue(report.contains("Diana in generic scope: YES"));
        assertTrue(report.contains("projectile"));
        assertTrue(report.contains("action-bar"));
        assertTrue(report.contains("rare-drop"));
        assertTrue(report.contains("Magic Find"));
    }
}
