package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Old Session History schema files must load safely without destroying data.
 */
final class MiningSessionHistoryMigrationTest {
    @TempDir
    Path tempDir;

    @Test
    void representativeV1FileLoadsAndPreservesFrozenValuation() throws Exception {
        MiningSessionAnalyticsViewModel model = stoppedModel();
        MiningSessionHistoryRecord provisional =
                MiningSessionHistoryRecord.fromStoppedViewModel(
                        "hmigration0000001",
                        MiningSessionHistoryStore.LEGACY_SCHEMA_VERSION,
                        "placeholder",
                        model,
                        OptionalLong.of(1_000L));
        String fingerprint = MiningSessionHistoryCodec.fingerprintPayload(
                provisional);
        MiningSessionHistoryRecord record =
                MiningSessionHistoryRecord.fromStoppedViewModel(
                        "hmigration0000001",
                        MiningSessionHistoryStore.LEGACY_SCHEMA_VERSION,
                        fingerprint,
                        model,
                        OptionalLong.of(1_000L));
        MiningSessionHistoryDocument document =
                MiningSessionHistoryDocument.of(
                        MiningSessionHistoryStore.LEGACY_SCHEMA_VERSION,
                        java.util.List.of(record));
        Path file = tempDir.resolve("rotclient-session-history.json");
        Files.writeString(
                file,
                MiningSessionHistoryCodec.toJsonString(document),
                StandardCharsets.UTF_8);

        MiningSessionHistoryStore.LoadResult loaded =
                MiningSessionHistoryStore.load(file);
        assertTrue(loaded.available());
        assertEquals(1, loaded.document().sessions().size());
        assertEquals(MiningSessionHistoryStore.LEGACY_SCHEMA_VERSION,
                loaded.document().schemaVersion());
        MiningSessionHistoryRecord restored =
                loaded.document().sessions().get(0);
        assertEquals(record.resolvedItemValue(), restored.resolvedItemValue());
        assertEquals(record.otherEntryCount(), restored.otherEntryCount());
        assertEquals(
                record.otherMinedQuantities().get("COBBLESTONE").quantity(),
                restored.otherMinedQuantities().get("COBBLESTONE").quantity());
        assertTrue(MiningSessionHistorySummaries.durationMillis(restored) > 0L);
        assertTrue(MiningSessionHistorySummaries.listSubtitle(restored)
                .contains("FROZEN")
                || MiningSessionHistorySummaries.listTitle(restored, 1)
                .contains("FROZEN"));

        RotClientCurrentSession current = new RotClientCurrentSession();
        long currentStart = current.snapshotConfig().startedAtMillis;
        current.creditUnknown(
                "MIGRATED_MOB_DROP",
                2L,
                SessionSourceType.MOB,
                currentStart + 1L);
        MiningSessionHistoryStore.SaveResult upgraded =
                MiningSessionHistoryStore.saveCurrentSession(
                        file,
                        loaded.document(),
                        current.freeze(currentStart + 100L),
                        "hmigration2000001");
        assertEquals(MiningSessionHistoryStore.SaveOutcome.SAVED,
                upgraded.outcome());
        assertEquals(MiningSessionHistoryStore.SCHEMA_VERSION,
                upgraded.document().schemaVersion());
        assertEquals(2, upgraded.document().size());
        MiningSessionHistoryStore.LoadResult reloaded =
                MiningSessionHistoryStore.load(file);
        assertTrue(reloaded.available());
        assertEquals(2, reloaded.document().size());
        assertTrue(reloaded.document().sessions().get(0)
                .currentSessionFreeze().isPresent());
        assertTrue(reloaded.document().sessions().get(1)
                .currentSessionFreeze().isEmpty());
    }

    @Test
    void idempotentReloadDoesNotDuplicate() throws Exception {
        MiningSessionAnalyticsViewModel model = stoppedModel();
        String fingerprint = MiningSessionHistoryCodec.contentFingerprint(
                model, OptionalLong.of(1_000L));
        MiningSessionHistoryRecord record =
                MiningSessionHistoryRecord.fromStoppedViewModel(
                        "hmigration0000002",
                        MiningSessionHistoryStore.SCHEMA_VERSION,
                        fingerprint,
                        model,
                        OptionalLong.of(1_000L));
        Path file = tempDir.resolve("hist.json");
        Files.writeString(
                file,
                MiningSessionHistoryCodec.toJsonString(
                        MiningSessionHistoryDocument.of(
                                MiningSessionHistoryStore.SCHEMA_VERSION,
                                java.util.List.of(record))),
                StandardCharsets.UTF_8);

        MiningSessionHistoryStore.LoadResult first =
                MiningSessionHistoryStore.load(file);
        MiningSessionHistoryStore.LoadResult second =
                MiningSessionHistoryStore.load(file);
        assertEquals(1, first.document().sessions().size());
        assertEquals(1, second.document().sessions().size());
        assertEquals(
                first.document().sessions().get(0).contentFingerprint(),
                second.document().sessions().get(0).contentFingerprint());
    }

    @Test
    void unknownFutureSchemaFailsClosedWithoutThrowingThroughStore()
            throws Exception {
        Path file = tempDir.resolve("future.json");
        Files.writeString(
                file,
                """
                        {
                          "schemaVersion": 99,
                          "sessions": []
                        }
                        """,
                StandardCharsets.UTF_8);
        MiningSessionHistoryStore.LoadResult loaded =
                MiningSessionHistoryStore.load(file);
        assertFalse(loaded.available());
        assertEquals(0, loaded.document().sessions().size());
        assertTrue(Files.isRegularFile(file));
        assertTrue(Files.readString(file, StandardCharsets.UTF_8)
                .contains("\"schemaVersion\": 99"));
        assertFalse(Files.exists(tempDir.resolve(
                MiningSessionHistoryStore.CORRUPT_FILE_NAME)));
    }

    private static MiningSessionAnalyticsViewModel stoppedModel() {
        return MiningSessionAnalyticsViewModel.create(
                MiningSessionAnalyticsViewModel.SessionState.STOPPED,
                "Gold",
                true,
                OptionalLong.of(1_000L),
                OptionalLong.of(10_000L),
                OptionalLong.of(10_000L),
                2,
                1,
                1,
                0,
                0,
                "MATCH",
                0L,
                MiningSessionValuation.PRICE_BASIS_LABEL,
                true,
                new BigDecimal("12.5"),
                1,
                1,
                0,
                0,
                0,
                0,
                new BigDecimal("10.0"),
                new BigDecimal("2.5"),
                BigDecimal.ZERO,
                OptionalLong.of(0L),
                true,
                false,
                Map.of(
                        "GOLD_INGOT",
                        new MiningSessionAnalyticsViewModel.ResourceQuantity(
                                "GOLD_INGOT", "Gold Ingot", 5L)),
                Map.of(
                        "COBBLESTONE",
                        new MiningSessionAnalyticsViewModel.ResourceQuantity(
                                "COBBLESTONE", "Cobblestone", 40L)),
                Map.of(),
                Map.of());
    }
}
