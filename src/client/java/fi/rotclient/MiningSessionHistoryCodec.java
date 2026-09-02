package fi.rotclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Deterministic Gson codec for Session History documents.
 * Reads legacy schema v1 and canonical Current Session schema v2. Rejects
 * unknown schema versions, partial records, and invalid values.
 */
final class MiningSessionHistoryCodec {
    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_DISPLAY_TEXT_LENGTH = 64;
    private static final int MAX_RESOURCE_ID_LENGTH = 64;
    private static final int FINGERPRINT_HEX_LENGTH = 64;
    static final int MAX_SESSION_SEGMENTS = 100_000;
    static final int MAX_SESSION_ITEM_ROWS = 50_000;
    private static final Set<String> ALLOWED_PARITY_LABELS = Set.of(
            "MATCH",
            "MISMATCH",
            "NOT_CHECKED",
            "INCOMPARABLE",
            "unavailable");

    private MiningSessionHistoryCodec() {
    }

    static String toJsonString(MiningSessionHistoryDocument document) {
        return GSON.toJson(toJson(document));
    }

    static JsonObject toJson(MiningSessionHistoryDocument document) {
        if (document == null) {
            throw new IllegalArgumentException(
                    "Document cannot be null");
        }
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", document.schemaVersion());
        JsonArray sessions = new JsonArray();
        for (MiningSessionHistoryRecord record : document.sessions()) {
            sessions.add(recordToJson(record));
        }
        root.add("sessions", sessions);
        return root;
    }

    static MiningSessionHistoryDocument fromJsonString(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException(
                    "History JSON cannot be blank");
        }
        JsonElement root = JsonParser.parseString(json);
        if (root == null || !root.isJsonObject()) {
            throw new IllegalArgumentException(
                    "History root must be a JSON object");
        }
        return fromJson(root.getAsJsonObject());
    }

    static MiningSessionHistoryDocument fromJson(JsonObject root) {
        if (root == null) {
            throw new IllegalArgumentException(
                    "History root cannot be null");
        }
        if (!root.has("schemaVersion") || !root.has("sessions")) {
            throw new IllegalArgumentException(
                    "History document is incomplete");
        }
        int schemaVersion = requirePositiveInt(root, "schemaVersion");
        if (schemaVersion != MiningSessionHistoryStore.LEGACY_SCHEMA_VERSION
                && schemaVersion != MiningSessionHistoryStore.SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported history schema version: " + schemaVersion);
        }
        JsonElement sessionsElement = root.get("sessions");
        if (sessionsElement == null || !sessionsElement.isJsonArray()) {
            throw new IllegalArgumentException(
                    "History sessions must be an array");
        }
        JsonArray sessionsArray = sessionsElement.getAsJsonArray();
        if (sessionsArray.size() > MiningSessionHistoryStore.MAX_RECORDS) {
            throw new IllegalArgumentException(
                    "History exceeds maximum retained sessions");
        }

        List<MiningSessionHistoryRecord> sessions = new ArrayList<>();
        Set<String> recordIds = new HashSet<>();
        Set<String> fingerprints = new HashSet<>();
        for (JsonElement element : sessionsArray) {
            if (element == null || !element.isJsonObject()) {
                throw new IllegalArgumentException(
                        "History session must be an object");
            }
            MiningSessionHistoryRecord record =
                    recordFromJson(element.getAsJsonObject());
            if (schemaVersion == MiningSessionHistoryStore.LEGACY_SCHEMA_VERSION
                    && record.schemaVersion()
                    != MiningSessionHistoryStore.LEGACY_SCHEMA_VERSION) {
                throw new IllegalArgumentException(
                        "Legacy history cannot contain newer records");
            }
            if (!recordIds.add(record.recordId())) {
                throw new IllegalArgumentException(
                        "Duplicate history record ID");
            }
            if (!fingerprints.add(record.contentFingerprint())) {
                throw new IllegalArgumentException(
                        "Duplicate history content fingerprint");
            }
            sessions.add(record);
        }
        return MiningSessionHistoryDocument.of(schemaVersion, sessions);
    }

    static String contentFingerprint(
            MiningSessionAnalyticsViewModel model,
            OptionalLong priceBookObservedAtMillis) {
        MiningSessionHistoryRecord provisional =
                MiningSessionHistoryRecord.fromStoppedViewModel(
                        "fingerprint-placeholder",
                        MiningSessionHistoryStore.SCHEMA_VERSION,
                        "fingerprint-placeholder",
                        model,
                        priceBookObservedAtMillis);
        return fingerprintPayload(provisional);
    }

    static String contentFingerprint(RotClientSessionFreeze frozen) {
        MiningSessionHistoryRecord provisional =
                MiningSessionHistoryRecord.fromCurrentSessionFreeze(
                        "fingerprint-placeholder",
                        MiningSessionHistoryStore.SCHEMA_VERSION,
                        "fingerprint-placeholder",
                        frozen);
        return fingerprintPayload(provisional);
    }

    static String fingerprintPayload(MiningSessionHistoryRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("Record cannot be null");
        }
        String canonical = GSON.toJson(recordPayloadJson(record));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to fingerprint history record",
                    exception);
        }
    }

    private static JsonObject recordToJson(MiningSessionHistoryRecord record) {
        JsonObject json = recordPayloadJson(record);
        json.addProperty("recordId", record.recordId());
        json.addProperty("contentFingerprint", record.contentFingerprint());
        return json;
    }

    private static JsonObject recordPayloadJson(
            MiningSessionHistoryRecord record) {
        JsonObject json = new JsonObject();
        json.addProperty("schemaVersion", record.schemaVersion());
        json.addProperty("stoppedMillis", record.stoppedMillis());
        if (record.sessionStartedMillis().isPresent()) {
            json.addProperty(
                    "sessionStartedMillis",
                    record.sessionStartedMillis().getAsLong());
        } else {
            json.add("sessionStartedMillis", null);
        }
        json.addProperty(
                "selectedTargetDisplayName",
                record.selectedTargetDisplayName());
        json.addProperty("trackerEnabled", record.trackerEnabled());
        json.addProperty("entryCount", record.entryCount());
        json.addProperty("targetEntryCount", record.targetEntryCount());
        json.addProperty("otherEntryCount", record.otherEntryCount());
        json.addProperty("chestLootEntryCount", record.chestLootEntryCount());
        json.addProperty("currencyEntryCount", record.currencyEntryCount());
        json.addProperty("parityStatusLabel", record.parityStatusLabel());
        json.addProperty("mismatchCount", record.mismatchCount());
        json.addProperty("priceBasisLabel", record.priceBasisLabel());
        json.addProperty(
                "resolvedValueAvailable",
                record.resolvedValueAvailable());
        json.addProperty(
                "resolvedItemValue",
                record.resolvedItemValue().toPlainString());
        json.addProperty("resolvedEntryCount", record.resolvedEntryCount());
        json.addProperty(
                "unresolvedEntryCount",
                record.unresolvedEntryCount());
        json.addProperty("staleEntryCount", record.staleEntryCount());
        json.addProperty(
                "unavailableEntryCount",
                record.unavailableEntryCount());
        json.addProperty(
                "unsupportedEntryCount",
                record.unsupportedEntryCount());
        json.addProperty(
                "excludedCurrencyEntryCount",
                record.excludedCurrencyEntryCount());
        json.addProperty(
                "targetMinedValue",
                record.targetMinedValue().toPlainString());
        json.addProperty(
                "otherMinedValue",
                record.otherMinedValue().toPlainString());
        json.addProperty(
                "chestLootValue",
                record.chestLootValue().toPlainString());
        if (record.priceBookObservedAtMillis().isPresent()) {
            json.addProperty(
                    "priceBookObservedAtMillis",
                    record.priceBookObservedAtMillis().getAsLong());
        } else {
            json.add("priceBookObservedAtMillis", null);
        }
        json.add(
                "targetMinedQuantities",
                quantitiesToJson(record.targetQuantities()));
        json.add(
                "otherMinedQuantities",
                quantitiesToJson(record.otherMinedQuantities()));
        json.add(
                "chestLootQuantities",
                quantitiesToJson(record.chestLootQuantities()));
        json.add(
                "currencyQuantities",
                quantitiesToJson(record.currencyQuantities()));
        record.currentSessionFreeze().ifPresent(frozen ->
                json.add("currentSession", currentSessionToJson(frozen)));
        return json;
    }

    private static MiningSessionHistoryRecord recordFromJson(JsonObject json) {
        requireKeys(json,
                "recordId",
                "schemaVersion",
                "contentFingerprint",
                "stoppedMillis",
                "selectedTargetDisplayName",
                "trackerEnabled",
                "entryCount",
                "targetEntryCount",
                "otherEntryCount",
                "chestLootEntryCount",
                "currencyEntryCount",
                "parityStatusLabel",
                "mismatchCount",
                "priceBasisLabel",
                "resolvedValueAvailable",
                "resolvedItemValue",
                "resolvedEntryCount",
                "unresolvedEntryCount",
                "staleEntryCount",
                "unavailableEntryCount",
                "unsupportedEntryCount",
                "excludedCurrencyEntryCount",
                "targetMinedValue",
                "otherMinedValue",
                "chestLootValue",
                "targetMinedQuantities",
                "otherMinedQuantities",
                "chestLootQuantities",
                "currencyQuantities");

        String recordId = requireNonBlankString(json, "recordId");
        validateRecordId(recordId);
        int schemaVersion = requirePositiveInt(json, "schemaVersion");
        if (schemaVersion != MiningSessionHistoryStore.LEGACY_SCHEMA_VERSION
                && schemaVersion != MiningSessionHistoryStore.SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported record schema version: " + schemaVersion);
        }
        String fingerprint = requireNonBlankString(json, "contentFingerprint");
        validateFingerprint(fingerprint);
        long stoppedMillis = requirePositiveLong(json, "stoppedMillis");
        OptionalLong started = optionalNonNegativeLong(
                json,
                "sessionStartedMillis",
                true);
        String target = requireSafeDisplayText(
                json,
                "selectedTargetDisplayName");
        boolean trackerEnabled = requireBoolean(json, "trackerEnabled");
        int entryCount = requireNonNegativeInt(json, "entryCount");
        int targetEntryCount = requireNonNegativeInt(json, "targetEntryCount");
        int otherEntryCount = requireNonNegativeInt(json, "otherEntryCount");
        int chestLootEntryCount =
                requireNonNegativeInt(json, "chestLootEntryCount");
        int currencyEntryCount =
                requireNonNegativeInt(json, "currencyEntryCount");
        String parity = requireParityLabel(json, "parityStatusLabel");
        long mismatchCount = requireNonNegativeLong(json, "mismatchCount");
        String priceBasis = requireNonBlankString(json, "priceBasisLabel");
        if (!MiningSessionValuation.PRICE_BASIS_LABEL.equals(priceBasis)) {
            throw new IllegalArgumentException(
                    "Unsupported history price basis label");
        }
        boolean resolvedAvailable =
                requireBoolean(json, "resolvedValueAvailable");
        BigDecimal resolvedValue = requireDecimal(json, "resolvedItemValue");
        int resolvedEntryCount =
                requireNonNegativeInt(json, "resolvedEntryCount");
        int unresolvedEntryCount =
                requireNonNegativeInt(json, "unresolvedEntryCount");
        int staleEntryCount = requireNonNegativeInt(json, "staleEntryCount");
        int unavailableEntryCount =
                requireNonNegativeInt(json, "unavailableEntryCount");
        int unsupportedEntryCount =
                requireNonNegativeInt(json, "unsupportedEntryCount");
        int excludedCurrencyEntryCount =
                requireNonNegativeInt(json, "excludedCurrencyEntryCount");
        BigDecimal targetValue = requireDecimal(json, "targetMinedValue");
        BigDecimal otherValue = requireDecimal(json, "otherMinedValue");
        BigDecimal chestValue = requireDecimal(json, "chestLootValue");
        OptionalLong priceBookObserved =
                optionalNonNegativeLong(json, "priceBookObservedAtMillis", false);

        MiningSessionHistoryRecord record = MiningSessionHistoryRecord.create(
                recordId,
                schemaVersion,
                fingerprint,
                stoppedMillis,
                started,
                target,
                trackerEnabled,
                entryCount,
                targetEntryCount,
                otherEntryCount,
                chestLootEntryCount,
                currencyEntryCount,
                parity,
                mismatchCount,
                priceBasis,
                resolvedAvailable,
                resolvedValue,
                resolvedEntryCount,
                unresolvedEntryCount,
                staleEntryCount,
                unavailableEntryCount,
                unsupportedEntryCount,
                excludedCurrencyEntryCount,
                targetValue,
                otherValue,
                chestValue,
                priceBookObserved,
                quantitiesFromJson(json.get("targetMinedQuantities")),
                quantitiesFromJson(json.get("otherMinedQuantities")),
                quantitiesFromJson(json.get("chestLootQuantities")),
                quantitiesFromJson(json.get("currencyQuantities")));
        if (schemaVersion == MiningSessionHistoryStore.LEGACY_SCHEMA_VERSION
                && json.has("currentSession")) {
            throw new IllegalArgumentException(
                    "Legacy history record cannot contain Current Session freeze");
        }
        if (json.has("currentSession")) {
            JsonElement current = json.get("currentSession");
            if (current == null || !current.isJsonObject()) {
                throw new IllegalArgumentException(
                        "Current Session freeze must be an object");
            }
            record = record.withCurrentSessionFreeze(
                    currentSessionFromJson(current.getAsJsonObject()));
        }
        if (!record.summaryMatchesCurrentSessionFreeze()) {
            throw new IllegalArgumentException(
                    "Current Session freeze does not match stored summary");
        }
        String expectedFingerprint = fingerprintPayload(record);
        if (!expectedFingerprint.equals(fingerprint)) {
            throw new IllegalArgumentException(
                    "History content fingerprint mismatch");
        }
        return record;
    }

    private static JsonObject currentSessionToJson(
            RotClientSessionFreeze frozen) {
        if (frozen.targetSegments().size() > MAX_SESSION_SEGMENTS
                || frozen.areaSegments().size() > MAX_SESSION_SEGMENTS
                || frozen.itemRows().size() > MAX_SESSION_ITEM_ROWS) {
            throw new IllegalArgumentException(
                    "Current Session freeze exceeds archive bounds");
        }
        JsonObject json = new JsonObject();
        json.addProperty("displayNumber", frozen.displayNumber());
        json.addProperty("startedAtMillis", frozen.startedAtMillis());
        json.addProperty("stoppedAtMillis", frozen.stoppedAtMillis());
        json.addProperty("activeDurationMillis", frozen.activeDurationMillis());
        json.addProperty("pausedDurationMillis", frozen.pausedDurationMillis());
        json.addProperty("selectedTargetId", frozen.selectedTargetId());
        if (frozen.powderChestsOpened() > 0L) {
            json.addProperty(
                    "powderChestsOpened", frozen.powderChestsOpened());
        }
        if (frozen.priceBookObservedAtMillis().isPresent()) {
            json.addProperty(
                    "priceBookObservedAtMillis",
                    frozen.priceBookObservedAtMillis().getAsLong());
        } else {
            json.add("priceBookObservedAtMillis", null);
        }

        JsonArray targets = new JsonArray();
        for (RotClientCurrentSessionConfig.TargetSegment segment
                : frozen.targetSegments()) {
            JsonObject item = new JsonObject();
            item.addProperty("targetId", segment.targetId());
            item.addProperty("startedAtMillis", segment.startedAtMillis());
            item.addProperty("endedAtMillis", segment.endedAtMillis());
            targets.add(item);
        }
        json.add("targetSegments", targets);

        JsonArray areas = new JsonArray();
        for (RotClientCurrentSessionConfig.AreaSegment segment
                : frozen.areaSegments()) {
            JsonObject item = new JsonObject();
            item.addProperty("areaId", segment.areaId());
            item.addProperty("startedAtMillis", segment.startedAtMillis());
            item.addProperty("endedAtMillis", segment.endedAtMillis());
            areas.add(item);
        }
        json.add("areaSegments", areas);

        JsonArray rows = new JsonArray();
        for (RotClientCurrentSessionConfig.SessionItemRecord row
                : frozen.itemRows()) {
            JsonObject item = new JsonObject();
            item.addProperty("itemId", row.itemId());
            item.addProperty("displayName", row.displayName());
            item.addProperty("quantity", row.quantity());
            item.addProperty("sourceType", row.sourceType());
            if (row.miningClass() == null) {
                item.add("miningClass", null);
            } else {
                item.addProperty("miningClass", row.miningClass());
            }
            item.addProperty("areaId", row.areaId());
            item.addProperty("known", row.known());
            item.addProperty("priceStatus", row.priceStatus());
            BigDecimal gross = BigDecimal.valueOf(row.resolvedGrossValue())
                    .setScale(8, java.math.RoundingMode.HALF_UP)
                    .stripTrailingZeros();
            item.addProperty("resolvedGrossValue", gross.toPlainString());
            rows.add(item);
        }
        json.add("itemRows", rows);
        return json;
    }

    private static RotClientSessionFreeze currentSessionFromJson(
            JsonObject json) {
        requireKeys(
                json,
                "displayNumber",
                "startedAtMillis",
                "stoppedAtMillis",
                "activeDurationMillis",
                "pausedDurationMillis",
                "selectedTargetId",
                "targetSegments",
                "areaSegments",
                "itemRows");
        int displayNumber = requirePositiveInt(json, "displayNumber");
        long started = requirePositiveLong(json, "startedAtMillis");
        long stopped = requirePositiveLong(json, "stoppedAtMillis");
        long active = requireNonNegativeLong(json, "activeDurationMillis");
        long paused = requireNonNegativeLong(json, "pausedDurationMillis");
        long powderChestsOpened = json.has("powderChestsOpened")
                ? requireNonNegativeLong(json, "powderChestsOpened")
                : 0L;
        if (stopped < started
                || active > stopped - started
                || paused > stopped - started
                || Math.addExact(active, paused) != stopped - started) {
            throw new IllegalArgumentException(
                    "Invalid Current Session lifecycle durations");
        }
        String selectedTargetId = requireSafeOptionalId(
                json, "selectedTargetId");
        OptionalLong priceObserved = optionalNonNegativeLong(
                json, "priceBookObservedAtMillis", false);
        return RotClientSessionFreeze.fromStored(
                displayNumber,
                started,
                stopped,
                active,
                paused,
                selectedTargetId,
                targetSegmentsFromJson(
                        json.get("targetSegments"), started, stopped),
                areaSegmentsFromJson(
                        json.get("areaSegments"), started, stopped),
                itemRowsFromJson(json.get("itemRows")),
                powderChestsOpened,
                priceObserved);
    }

    private static List<RotClientCurrentSessionConfig.TargetSegment>
            targetSegmentsFromJson(
                    JsonElement element,
                    long sessionStarted,
                    long stopped) {
        if (element == null || !element.isJsonArray()
                || element.getAsJsonArray().size() > MAX_SESSION_SEGMENTS) {
            throw new IllegalArgumentException("Invalid target segments");
        }
        List<RotClientCurrentSessionConfig.TargetSegment> result =
                new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (entry == null || !entry.isJsonObject()) {
                throw new IllegalArgumentException("Invalid target segment");
            }
            JsonObject item = entry.getAsJsonObject();
            requireKeys(item, "targetId", "startedAtMillis", "endedAtMillis");
            String id = requireSafeOptionalId(item, "targetId");
            long start = requireNonNegativeLong(item, "startedAtMillis");
            long end = requirePositiveLong(item, "endedAtMillis");
            if (start < sessionStarted || end < start || end > stopped) {
                throw new IllegalArgumentException("Invalid target segment time");
            }
            result.add(new RotClientCurrentSessionConfig.TargetSegment(
                    id, start, end));
        }
        return result;
    }

    private static List<RotClientCurrentSessionConfig.AreaSegment>
            areaSegmentsFromJson(
                    JsonElement element,
                    long sessionStarted,
                    long stopped) {
        if (element == null || !element.isJsonArray()
                || element.getAsJsonArray().size() > MAX_SESSION_SEGMENTS) {
            throw new IllegalArgumentException("Invalid area segments");
        }
        List<RotClientCurrentSessionConfig.AreaSegment> result =
                new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (entry == null || !entry.isJsonObject()) {
                throw new IllegalArgumentException("Invalid area segment");
            }
            JsonObject item = entry.getAsJsonObject();
            requireKeys(item, "areaId", "startedAtMillis", "endedAtMillis");
            String rawArea = requireNonBlankString(item, "areaId");
            SkyBlockArea area = SkyBlockArea.fromId(rawArea);
            if (!area.id().equals(rawArea)) {
                throw new IllegalArgumentException("Invalid area ID");
            }
            long start = requireNonNegativeLong(item, "startedAtMillis");
            long end = requirePositiveLong(item, "endedAtMillis");
            if (start < sessionStarted || end < start || end > stopped) {
                throw new IllegalArgumentException("Invalid area segment time");
            }
            result.add(new RotClientCurrentSessionConfig.AreaSegment(
                    area.id(), start, end));
        }
        return result;
    }

    private static List<RotClientCurrentSessionConfig.SessionItemRecord>
            itemRowsFromJson(JsonElement element) {
        if (element == null || !element.isJsonArray()
                || element.getAsJsonArray().size() > MAX_SESSION_ITEM_ROWS) {
            throw new IllegalArgumentException("Invalid Current Session rows");
        }
        List<RotClientCurrentSessionConfig.SessionItemRecord> result =
                new ArrayList<>();
        Set<String> identities = new HashSet<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (entry == null || !entry.isJsonObject()) {
                throw new IllegalArgumentException("Invalid Current Session row");
            }
            JsonObject item = entry.getAsJsonObject();
            requireKeys(
                    item,
                    "itemId",
                    "displayName",
                    "quantity",
                    "sourceType",
                    "areaId",
                    "known",
                    "priceStatus",
                    "resolvedGrossValue");
            String itemId = requireResourceId(item, "itemId");
            String displayName = requireSafeDisplayText(item, "displayName");
            long quantity = requirePositiveLong(item, "quantity");
            String sourceName = requireNonBlankString(item, "sourceType")
                    .toUpperCase(Locale.ROOT);
            SessionSourceType source = SessionSourceType.fromName(sourceName);
            if (!source.name().equals(sourceName)) {
                throw new IllegalArgumentException("Invalid source type");
            }
            String miningClass = null;
            if (item.has("miningClass")
                    && !item.get("miningClass").isJsonNull()) {
                miningClass = requireNonBlankString(item, "miningClass")
                        .toUpperCase(Locale.ROOT);
                MiningClassification classification =
                        MiningClassification.fromName(miningClass);
                if (classification == null
                        || !classification.name().equals(miningClass)) {
                    throw new IllegalArgumentException(
                            "Invalid mining classification");
                }
            }
            String rawArea = requireNonBlankString(item, "areaId");
            SkyBlockArea area = SkyBlockArea.fromId(rawArea);
            if (!area.id().equals(rawArea)) {
                throw new IllegalArgumentException("Invalid row area ID");
            }
            boolean known = requireBoolean(item, "known");
            String priceName = requireNonBlankString(item, "priceStatus")
                    .toUpperCase(Locale.ROOT);
            RotClientCurrentSessionConfig.PriceStatus status =
                    RotClientCurrentSessionConfig.PriceStatus.fromName(
                            priceName);
            if (!status.name().equals(priceName)) {
                throw new IllegalArgumentException("Invalid price status");
            }
            BigDecimal gross = requireDecimal(item, "resolvedGrossValue");
            if (gross.signum() < 0) {
                throw new IllegalArgumentException("Negative resolved value");
            }
            String identity = itemId + '\n' + sourceName + '\n'
                    + (miningClass == null ? "" : miningClass) + '\n'
                    + area.id();
            if (!identities.add(identity)) {
                throw new IllegalArgumentException(
                        "Duplicate Current Session row identity");
            }
            result.add(new RotClientCurrentSessionConfig.SessionItemRecord(
                    itemId,
                    displayName,
                    quantity,
                    sourceName,
                    miningClass,
                    area.id(),
                    known,
                    priceName,
                    gross.doubleValue()));
        }
        return result;
    }

    private static String requireSafeOptionalId(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()
                || !element.isJsonPrimitive()) {
            throw new IllegalArgumentException("Invalid ID field: " + key);
        }
        String value = element.getAsString();
        if (value == null || value.length() > MAX_RESOURCE_ID_LENGTH) {
            throw new IllegalArgumentException("Invalid ID field: " + key);
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if ((ch < 'A' || ch > 'Z')
                    && (ch < 'a' || ch > 'z')
                    && (ch < '0' || ch > '9')
                    && ch != '_') {
                throw new IllegalArgumentException(
                        "Invalid ID field characters: " + key);
            }
        }
        return value;
    }

    private static JsonArray quantitiesToJson(
            Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                    quantities) {
        JsonArray array = new JsonArray();
        for (MiningSessionAnalyticsViewModel.ResourceQuantity quantity
                : quantities.values()) {
            JsonObject item = new JsonObject();
            item.addProperty("resourceId", quantity.resourceId());
            item.addProperty("displayName", quantity.displayName());
            item.addProperty("quantity", quantity.quantity());
            array.add(item);
        }
        return array;
    }

    private static Map<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
            quantitiesFromJson(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException(
                    "Quantity maps must be JSON arrays");
        }
        LinkedHashMap<String, MiningSessionAnalyticsViewModel.ResourceQuantity>
                result = new LinkedHashMap<>();
        for (JsonElement itemElement : element.getAsJsonArray()) {
            if (itemElement == null || !itemElement.isJsonObject()) {
                throw new IllegalArgumentException(
                        "Quantity entry must be an object");
            }
            JsonObject item = itemElement.getAsJsonObject();
            requireKeys(item, "resourceId", "displayName", "quantity");
            String resourceId = requireResourceId(item, "resourceId");
            String displayName = requireSafeDisplayText(item, "displayName");
            long quantity = requirePositiveLong(item, "quantity");
            if (result.containsKey(resourceId)) {
                throw new IllegalArgumentException(
                        "Duplicate resource ID in quantity map");
            }
            result.put(
                    resourceId,
                    new MiningSessionAnalyticsViewModel.ResourceQuantity(
                            resourceId,
                            displayName,
                            quantity));
        }
        return result;
    }

    private static void validateRecordId(String recordId) {
        String normalized = recordId.toLowerCase(Locale.ROOT);
        if (!recordId.startsWith("h")
                || recordId.length() < 8
                || recordId.length() > 40) {
            throw new IllegalArgumentException(
                    "Invalid history record ID");
        }
        for (int i = 0; i < recordId.length(); i++) {
            char ch = recordId.charAt(i);
            if ((ch < 'a' || ch > 'z')
                    && (ch < 'A' || ch > 'Z')
                    && (ch < '0' || ch > '9')) {
                throw new IllegalArgumentException(
                        "Invalid history record ID characters");
            }
        }
        if (normalized.contains("uuid")
                || normalized.contains("player")
                || normalized.contains("user")
                || normalized.contains("server")
                || normalized.contains("path")
                || normalized.contains(":")
                || normalized.contains("\\")
                || normalized.contains("/")) {
            throw new IllegalArgumentException(
                    "History record ID contains forbidden information");
        }
    }

    private static void validateFingerprint(String fingerprint) {
        if (fingerprint.length() != FINGERPRINT_HEX_LENGTH) {
            throw new IllegalArgumentException(
                    "Invalid history content fingerprint length");
        }
        for (int i = 0; i < fingerprint.length(); i++) {
            char ch = fingerprint.charAt(i);
            boolean hex = (ch >= '0' && ch <= '9')
                    || (ch >= 'a' && ch <= 'f');
            if (!hex) {
                throw new IllegalArgumentException(
                        "Invalid history content fingerprint characters");
            }
        }
    }

    private static String requireParityLabel(JsonObject json, String key) {
        String value = requireSafeDisplayText(json, key);
        if (!ALLOWED_PARITY_LABELS.contains(value)) {
            throw new IllegalArgumentException(
                    "Unsupported history parity label");
        }
        return value;
    }

    private static String requireSafeDisplayText(JsonObject json, String key) {
        String value = requireNonBlankString(json, key);
        if (value.length() > MAX_DISPLAY_TEXT_LENGTH) {
            throw new IllegalArgumentException(
                    "History text field too long: " + key);
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch < 0x20 || ch == 0x7F) {
                throw new IllegalArgumentException(
                        "History text field contains control characters: "
                                + key);
            }
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains("uuid")
                || normalized.contains("http://")
                || normalized.contains("https://")
                || normalized.contains("c:\\")
                || normalized.contains("/users/")
                || normalized.contains("/home/")) {
            throw new IllegalArgumentException(
                    "History text field contains forbidden information: "
                            + key);
        }
        return value;
    }

    private static String requireResourceId(JsonObject json, String key) {
        String value = requireNonBlankString(json, key);
        if (value.length() > MAX_RESOURCE_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "History resource ID too long");
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if ((ch < 'A' || ch > 'Z')
                    && (ch < 'a' || ch > 'z')
                    && (ch < '0' || ch > '9')
                    && ch != '_') {
                throw new IllegalArgumentException(
                        "Invalid history resource ID characters");
            }
        }
        return value;
    }

    private static void requireKeys(JsonObject json, String... keys) {
        for (String key : keys) {
            if (!json.has(key)) {
                throw new IllegalArgumentException(
                        "Missing required history field: " + key);
            }
        }
    }

    private static String requireNonBlankString(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            throw new IllegalArgumentException(
                    "Invalid string field: " + key);
        }
        String value = element.getAsString();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Blank string field: " + key);
        }
        return value;
    }

    private static boolean requireBoolean(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null
                || !element.isJsonPrimitive()
                || !((JsonPrimitive) element).isBoolean()) {
            throw new IllegalArgumentException(
                    "Invalid boolean field: " + key);
        }
        return element.getAsBoolean();
    }

    private static int requirePositiveInt(JsonObject json, String key) {
        int value = requireNonNegativeInt(json, key);
        if (value <= 0) {
            throw new IllegalArgumentException(
                    "Field must be positive: " + key);
        }
        return value;
    }

    private static int requireNonNegativeInt(JsonObject json, String key) {
        long value = requireNonNegativeLong(json, key);
        if (value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Integer overflow for field: " + key);
        }
        return (int) value;
    }

    private static long requirePositiveLong(JsonObject json, String key) {
        long value = requireNonNegativeLong(json, key);
        if (value <= 0L) {
            throw new IllegalArgumentException(
                    "Field must be positive: " + key);
        }
        return value;
    }

    private static long requireNonNegativeLong(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null
                || !element.isJsonPrimitive()
                || !((JsonPrimitive) element).isNumber()) {
            throw new IllegalArgumentException(
                    "Invalid number field: " + key);
        }
        long value;
        try {
            value = element.getAsLong();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(
                    "Invalid number field: " + key,
                    exception);
        }
        if (value < 0L) {
            throw new IllegalArgumentException(
                    "Negative number field: " + key);
        }
        return value;
    }

    private static OptionalLong optionalNonNegativeLong(
            JsonObject json,
            String key,
            boolean requirePositiveWhenPresent) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            return OptionalLong.empty();
        }
        long value = requireNonNegativeLong(json, key);
        if (requirePositiveWhenPresent && value <= 0L) {
            throw new IllegalArgumentException(
                    "Optional timestamp must be positive: " + key);
        }
        return OptionalLong.of(value);
    }

    private static BigDecimal requireDecimal(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null
                || element.isJsonNull()
                || !element.isJsonPrimitive()) {
            throw new IllegalArgumentException(
                    "Invalid decimal field: " + key);
        }
        try {
            BigDecimal value = new BigDecimal(element.getAsString());
            if (value.scale() > 8) {
                throw new IllegalArgumentException(
                        "Decimal scale too large: " + key);
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Invalid decimal field: " + key,
                    exception);
        }
    }
}
