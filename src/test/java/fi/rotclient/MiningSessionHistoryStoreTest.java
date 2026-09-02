package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class MiningSessionHistoryStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void missingAndEmptyFilesLoadAvailableEmpty() throws Exception {
        Path missing = tempDir.resolve("missing.json");
        MiningSessionHistoryStore.LoadResult missingResult =
                MiningSessionHistoryStore.load(missing);
        assertTrue(missingResult.available());
        assertTrue(missingResult.document().isEmpty());

        Path empty = tempDir.resolve("empty.json");
        Files.writeString(empty, "", StandardCharsets.UTF_8);
        MiningSessionHistoryStore.LoadResult emptyResult =
                MiningSessionHistoryStore.load(empty);
        assertTrue(emptyResult.available());
        assertTrue(emptyResult.document().isEmpty());
    }

    @Test
    void versionOneRoundTripIsDeterministicAndImmutable() {
        MiningSessionHistoryRecord record = sampleRecord("habcdef012345678");
        MiningSessionHistoryDocument document =
                MiningSessionHistoryDocument.of(
                        MiningSessionHistoryStore.SCHEMA_VERSION,
                        List.of(record));
        String first = MiningSessionHistoryCodec.toJsonString(document);
        String second = MiningSessionHistoryCodec.toJsonString(
                MiningSessionHistoryCodec.fromJsonString(first));
        assertEquals(first, second);

        MiningSessionHistoryDocument loaded =
                MiningSessionHistoryCodec.fromJsonString(first);
        assertThrows(
                UnsupportedOperationException.class,
                () -> loaded.sessions().add(record));
        assertThrows(
                UnsupportedOperationException.class,
                () -> loaded.sessions().get(0).targetQuantities()
                        .put("x", quantity("x", "X", 1L)));
    }

    @Test
    void newestFirstOrderAndMaxTwentyWithOldestEviction() {
        Path path = tempDir.resolve("history.json");
        MiningSessionHistoryDocument document =
                MiningSessionHistoryDocument.empty(
                        MiningSessionHistoryStore.SCHEMA_VERSION);
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            MiningSessionAnalyticsViewModel model = stoppedModel(
                    10_000L + i,
                    "Gold",
                    i + 1);
            MiningSessionHistoryStore.SaveResult result =
                    MiningSessionHistoryStore.saveStoppedSession(
                            path,
                            document,
                            model,
                            OptionalLong.of(900L),
                            "h" + String.format("%016d", i));
            assertEquals(
                    MiningSessionHistoryStore.SaveOutcome.SAVED,
                    result.outcome());
            document = result.document();
            ids.add(result.record().orElseThrow().recordId());
        }
        assertEquals(20, document.size());
        assertEquals(ids.get(20), document.sessions().get(0).recordId());
        assertEquals(ids.get(1), document.sessions().get(19).recordId());
        assertFalse(document.sessions().stream()
                .anyMatch(record -> record.recordId().equals(ids.get(0))));
    }

    @Test
    void duplicateSaveIsIdempotent() {
        Path path = tempDir.resolve("history.json");
        MiningSessionAnalyticsViewModel model = stoppedModel(
                2_000L,
                "Gold",
                4);
        MiningSessionHistoryStore.SaveResult first =
                MiningSessionHistoryStore.saveStoppedSession(
                        path,
                        MiningSessionHistoryDocument.empty(
                                MiningSessionHistoryStore.SCHEMA_VERSION),
                        model,
                        OptionalLong.of(1_500L),
                        "h1111111111111111");
        assertEquals(
                MiningSessionHistoryStore.SaveOutcome.SAVED,
                first.outcome());
        MiningSessionHistoryStore.SaveResult second =
                MiningSessionHistoryStore.saveStoppedSession(
                        path,
                        first.document(),
                        model,
                        OptionalLong.of(1_500L),
                        "h2222222222222222");
        assertEquals(
                MiningSessionHistoryStore.SaveOutcome.ALREADY_SAVED,
                second.outcome());
        assertEquals(1, second.document().size());
        assertEquals(
                first.record().orElseThrow().recordId(),
                second.record().orElseThrow().recordId());
    }

    @Test
    void malformedUnknownVersionPartialNegativeAndInvalidValuesFailClosed()
            throws Exception {
        Path malformed = tempDir.resolve("malformed.json");
        Files.writeString(malformed, "{not-json", StandardCharsets.UTF_8);
        MiningSessionHistoryStore.LoadResult malformedResult =
                MiningSessionHistoryStore.load(malformed);
        assertFalse(malformedResult.available());
        assertTrue(malformedResult.document().isEmpty());
        assertFalse(malformedResult.warning().isBlank());

        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionHistoryCodec.fromJsonString("""
                        {"schemaVersion":99,"sessions":[]}
                        """));
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionHistoryCodec.fromJsonString("""
                        {"schemaVersion":1,"sessions":[{"recordId":"h1"}]}
                        """));
        JsonObject valid = MiningSessionHistoryCodec.toJson(
                MiningSessionHistoryDocument.of(
                        MiningSessionHistoryStore.SCHEMA_VERSION,
                        List.of(sampleRecord("habcdef012345678"))));
        JsonObject negative = valid.deepCopy();
        negative.getAsJsonArray("sessions")
                .get(0)
                .getAsJsonObject()
                .addProperty("entryCount", -1);
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionHistoryCodec.fromJson(negative));

        JsonObject badDecimal = valid.deepCopy();
        badDecimal.getAsJsonArray("sessions")
                .get(0)
                .getAsJsonObject()
                .addProperty("resolvedItemValue", "not-a-number");
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionHistoryCodec.fromJson(badDecimal));

        JsonObject badTimestamp = valid.deepCopy();
        badTimestamp.getAsJsonArray("sessions")
                .get(0)
                .getAsJsonObject()
                .addProperty("stoppedMillis", -5);
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionHistoryCodec.fromJson(badTimestamp));
    }

    @Test
    void rejectsDuplicateIdsForgedFingerprintsAndUnsafeText() {
        JsonObject valid = MiningSessionHistoryCodec.toJson(
                MiningSessionHistoryDocument.of(
                        MiningSessionHistoryStore.SCHEMA_VERSION,
                        List.of(sampleRecord("habcdef012345678"))));

        JsonObject forged = valid.deepCopy();
        forged.getAsJsonArray("sessions")
                .get(0)
                .getAsJsonObject()
                .addProperty(
                        "contentFingerprint",
                        "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionHistoryCodec.fromJson(forged));

        MiningSessionHistoryRecord first = sampleRecord("habcdef012345678");
        MiningSessionHistoryRecord second = sampleRecord("hfedcba876543210");
        JsonObject duplicateIds = MiningSessionHistoryCodec.toJson(
                MiningSessionHistoryDocument.of(
                        MiningSessionHistoryStore.SCHEMA_VERSION,
                        List.of(first)));
        JsonObject duplicateRecord = MiningSessionHistoryCodec.toJson(
                        MiningSessionHistoryDocument.of(
                                MiningSessionHistoryStore.SCHEMA_VERSION,
                                List.of(second)))
                .getAsJsonArray("sessions")
                .get(0)
                .getAsJsonObject()
                .deepCopy();
        duplicateRecord.addProperty("recordId", first.recordId());
        duplicateRecord.addProperty(
                "contentFingerprint",
                second.contentFingerprint());
        duplicateIds.getAsJsonArray("sessions").add(duplicateRecord);
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionHistoryCodec.fromJson(duplicateIds));

        JsonObject unsafe = valid.deepCopy();
        unsafe.getAsJsonArray("sessions")
                .get(0)
                .getAsJsonObject()
                .addProperty(
                        "selectedTargetDisplayName",
                        "PlayerUUID-c:\\users\\secret");
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionHistoryCodec.fromJson(unsafe));

        JsonObject badBasis = valid.deepCopy();
        badBasis.getAsJsonArray("sessions")
                .get(0)
                .getAsJsonObject()
                .addProperty("priceBasisLabel", "profit estimate");
        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionHistoryCodec.fromJson(badBasis));

        assertThrows(
                IllegalArgumentException.class,
                () -> MiningSessionHistoryDocument.of(
                        MiningSessionHistoryStore.SCHEMA_VERSION,
                        List.of(
                                sampleRecord("h0000000000000001"),
                                sampleRecord("h0000000000000002"),
                                sampleRecord("h0000000000000003"),
                                sampleRecord("h0000000000000004"),
                                sampleRecord("h0000000000000005"),
                                sampleRecord("h0000000000000006"),
                                sampleRecord("h0000000000000007"),
                                sampleRecord("h0000000000000008"),
                                sampleRecord("h0000000000000009"),
                                sampleRecord("h0000000000000010"),
                                sampleRecord("h0000000000000011"),
                                sampleRecord("h0000000000000012"),
                                sampleRecord("h0000000000000013"),
                                sampleRecord("h0000000000000014"),
                                sampleRecord("h0000000000000015"),
                                sampleRecord("h0000000000000016"),
                                sampleRecord("h0000000000000017"),
                                sampleRecord("h0000000000000018"),
                                sampleRecord("h0000000000000019"),
                                sampleRecord("h0000000000000020"),
                                sampleRecord("h0000000000000021"))));
    }

    @Test
    void failedWritePreservesPreviousFileAndNeverTouchesTrackerStore()
            throws Exception {
        Path history = tempDir.resolve("history.json");
        Path tracker = tempDir.resolve("rotclient.json");
        Files.writeString(tracker, "{\"dataVersion\":8}", StandardCharsets.UTF_8);
        String trackerBefore = Files.readString(tracker, StandardCharsets.UTF_8);

        MiningSessionHistoryStore.SaveResult first =
                MiningSessionHistoryStore.saveStoppedSession(
                        history,
                        MiningSessionHistoryDocument.empty(
                                MiningSessionHistoryStore.SCHEMA_VERSION),
                        stoppedModel(3_000L, "Gold", 2),
                        OptionalLong.of(2_500L),
                        "h3333333333333333");
        assertEquals(
                MiningSessionHistoryStore.SaveOutcome.SAVED,
                first.outcome());
        String previous = Files.readString(history, StandardCharsets.UTF_8);

        Path blockedParent = tempDir.resolve("blocked-file");
        Files.writeString(blockedParent, "not-a-directory", StandardCharsets.UTF_8);
        Path blocked = blockedParent.resolve("history.json");
        MiningSessionHistoryStore.SaveResult failed =
                MiningSessionHistoryStore.saveStoppedSession(
                        blocked,
                        first.document(),
                        stoppedModel(4_000L, "Diamond", 3),
                        OptionalLong.of(3_500L),
                        "h4444444444444444");
        assertEquals(
                MiningSessionHistoryStore.SaveOutcome.WRITE_FAILED,
                failed.outcome());
        assertEquals(previous, Files.readString(history, StandardCharsets.UTF_8));
        assertEquals(trackerBefore, Files.readString(tracker, StandardCharsets.UTF_8));
    }

    @Test
    void utf8ByteLimitRejectsWithoutTouchingThePreviousHistoryFile()
            throws Exception {
        Path history = tempDir.resolve("utf8-limit-history.json");
        String previous = "previous-history-content";
        Files.writeString(history, previous, StandardCharsets.UTF_8);

        MiningSessionAnalyticsViewModel model = stoppedModel(
                6_000L,
                "Mithril \u2726",
                3L);
        String fingerprint = MiningSessionHistoryCodec.contentFingerprint(
                model,
                OptionalLong.of(5_500L));
        MiningSessionHistoryRecord record =
                MiningSessionHistoryRecord.fromStoppedViewModel(
                        "hutf8limit000001",
                        MiningSessionHistoryStore.SCHEMA_VERSION,
                        fingerprint,
                        model,
                        OptionalLong.of(5_500L));
        MiningSessionHistoryDocument document =
                MiningSessionHistoryDocument.of(
                        MiningSessionHistoryStore.SCHEMA_VERSION,
                        List.of(record));
        String json = MiningSessionHistoryCodec.toJsonString(document);
        long utf8Bytes = json.getBytes(StandardCharsets.UTF_8).length;
        assertTrue(utf8Bytes > json.length());

        assertFalse(MiningSessionHistoryStore.writeWithinLimit(
                history, document, utf8Bytes - 1L));
        assertEquals(previous,
                Files.readString(history, StandardCharsets.UTF_8));

        assertTrue(MiningSessionHistoryStore.writeWithinLimit(
                history, document, utf8Bytes));
        assertEquals(json, Files.readString(history, StandardCharsets.UTF_8));
        MiningSessionHistoryStore.LoadResult loaded =
                MiningSessionHistoryStore.load(history);
        assertTrue(loaded.available());
        assertEquals(1, loaded.document().size());
    }

    @Test
    void privacyGuaranteesForSerializedHistory() {
        MiningSessionHistoryRecord record = sampleRecord("hprivacysafe0001");
        String json = MiningSessionHistoryCodec.toJsonString(
                MiningSessionHistoryDocument.of(
                        MiningSessionHistoryStore.SCHEMA_VERSION,
                        List.of(record)));
        String lower = json.toLowerCase();
        assertFalse(lower.contains("chat"));
        assertFalse(lower.contains("coordinate"));
        assertFalse(lower.contains("username"));
        assertFalse(lower.contains("uuid"));
        assertFalse(lower.contains("sessionid"));
        assertFalse(lower.contains("eventid"));
        assertFalse(lower.contains("correlation"));
        assertFalse(lower.contains("http"));
        assertFalse(lower.contains("api"));
        assertFalse(lower.contains("c:\\\\"));
        assertFalse(lower.contains("/users/"));
        assertFalse(json.contains("products"));
        assertFalse(json.contains("sell_summary"));
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        assertTrue(root.has("schemaVersion"));
        assertTrue(root.has("sessions"));
        assertFalse(root.has("priceMap"));
    }

    @Test
    void sanitizedFixtureRoundTrip() {
        MiningSessionHistoryRecord fixture = sanitizedFixture();
        String json = MiningSessionHistoryCodec.toJsonString(
                MiningSessionHistoryDocument.of(
                        MiningSessionHistoryStore.SCHEMA_VERSION,
                        List.of(fixture)));
        MiningSessionHistoryDocument loaded =
                MiningSessionHistoryCodec.fromJsonString(json);
        MiningSessionHistoryRecord record = loaded.sessions().get(0);
        assertEquals("Gold", record.selectedTargetDisplayName());
        assertEquals(1, record.targetEntryCount());
        assertEquals(1, record.otherEntryCount());
        assertEquals(2, record.chestLootEntryCount());
        assertEquals(1, record.currencyEntryCount());
        assertTrue(record.targetQuantities().containsKey("GOLD_INGOT"));
        assertTrue(record.otherMinedQuantities().containsKey("HARD_STONE"));
        assertTrue(record.chestLootQuantities().containsKey("ROUGH_TOPAZ_GEM"));
        assertTrue(record.chestLootQuantities().containsKey("FLAWED_RUBY_GEM"));
        assertTrue(record.currencyQuantities().containsKey("GEMSTONE_POWDER"));
        assertEquals(0L, record.mismatchCount());
        assertTrue(record.unsupportedEntryCount() >= 1);
        assertFalse(json.toLowerCase().contains("chat"));
    }

    private static MiningSessionHistoryRecord sanitizedFixture() {
        Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity> target =
                Map.of("GOLD_INGOT", quantity("GOLD_INGOT", "Gold Ingot", 4L));
        Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity> other =
                Map.of("HARD_STONE", quantity("HARD_STONE", "Hard Stone", 12L));
        LinkedHashMap<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                chest = new LinkedHashMap<>();
        chest.put("ROUGH_TOPAZ_GEM",
                quantity("ROUGH_TOPAZ_GEM", "Rough Topaz Gemstone", 24L));
        chest.put("FLAWED_RUBY_GEM",
                quantity("FLAWED_RUBY_GEM", "Flawed Ruby Gemstone", 2L));
        Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity> currency =
                Map.of("GEMSTONE_POWDER",
                        quantity("GEMSTONE_POWDER", "Gemstone Powder", 291L));
        MiningSessionAnalyticsViewModel model =
                MiningSessionAnalyticsViewModel.create(
                        MiningSessionAnalyticsViewModel.SessionState.STOPPED,
                        "Gold",
                        true,
                        OptionalLong.of(1_000L),
                        OptionalLong.of(2_000L),
                        OptionalLong.of(2_000L),
                        5,
                        1,
                        1,
                        2,
                        1,
                        "MATCH",
                        0L,
                        MiningSessionValuation.PRICE_BASIS_LABEL,
                        true,
                        new BigDecimal("56.00"),
                        3,
                        0,
                        0,
                        0,
                        1,
                        1,
                        new BigDecimal("8.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("48.00"),
                        OptionalLong.of(100L),
                        true,
                        false,
                        target,
                        other,
                        chest,
                        currency);
        String fingerprint = MiningSessionHistoryCodec.contentFingerprint(
                model,
                OptionalLong.of(1_900L));
        return MiningSessionHistoryRecord.fromStoppedViewModel(
                "hfixture00000001",
                MiningSessionHistoryStore.SCHEMA_VERSION,
                fingerprint,
                model,
                OptionalLong.of(1_900L));
    }

    private static MiningSessionHistoryRecord sampleRecord(String id) {
        MiningSessionAnalyticsViewModel model = stoppedModel(5_000L, "Gold", 3);
        String fingerprint = MiningSessionHistoryCodec.contentFingerprint(
                model,
                OptionalLong.of(4_500L));
        return MiningSessionHistoryRecord.fromStoppedViewModel(
                id,
                MiningSessionHistoryStore.SCHEMA_VERSION,
                fingerprint,
                model,
                OptionalLong.of(4_500L));
    }

    private static MiningSessionAnalyticsViewModel stoppedModel(
            long stoppedAt,
            String target,
            long goldQuantity) {
        return MiningSessionAnalyticsViewModel.create(
                MiningSessionAnalyticsViewModel.SessionState.STOPPED,
                target,
                true,
                OptionalLong.of(stoppedAt - 1_000L),
                OptionalLong.of(stoppedAt),
                OptionalLong.of(stoppedAt),
                1,
                1,
                0,
                0,
                0,
                "MATCH",
                0L,
                MiningSessionValuation.PRICE_BASIS_LABEL,
                true,
                new BigDecimal("2.00").multiply(BigDecimal.valueOf(goldQuantity)),
                1,
                0,
                0,
                0,
                0,
                0,
                new BigDecimal("2.00").multiply(BigDecimal.valueOf(goldQuantity)),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                OptionalLong.of(50L),
                true,
                false,
                Map.of(
                        "GOLD_INGOT",
                        quantity("GOLD_INGOT", "Gold Ingot", goldQuantity)),
                Map.of(),
                Map.of(),
                Map.of());
    }

    private static MiningSessionAnalyticsViewModel.ResourceQuantity quantity(
            String id,
            String name,
            long quantity) {
        return new MiningSessionAnalyticsViewModel.ResourceQuantity(
                id,
                name,
                quantity);
    }
}
