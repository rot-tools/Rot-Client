package fi.rotclient;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Manually enabled, bounded, privacy-safe tracking runtime trace sized for a
 * ~15–20 minute Hypixel session. Records meaningful tracking events only;
 * does not activate observers.
 */
final class TrackingRuntimeTrace {
    static final String DIAGNOSTICS_SCHEMA = "rotclient-tracking-runtime-v2";
    static final long MAX_BYTES = 4L * 1024L * 1024L;
    static final long CALLBACK_ALIVE_THROTTLE_MILLIS = 5_000L;
    static final int MAX_UNIQUE_ITEMS = 512;

    enum Reason {
        ACCEPTED_TARGET,
        ACCEPTED_OTHER_MINING,
        ACCEPTED_CHEST,
        ACCEPTED_MOB,
        REJECTED_DUPLICATE,
        REJECTED_NO_MINING_EVIDENCE,
        REJECTED_NO_SOURCE_EVIDENCE,
        REJECTED_TARGET_AUTHORITY,
        REJECTED_SESSION_PAUSED,
        REJECTED_NO_STABLE_ID,
        REJECTED_ZERO_DELTA,
        REJECTED_UNKNOWN_SOURCE,
        REJECTED_COLLECTION_INACTIVE,
        UNATTRIBUTED_NO_SOURCE_EVIDENCE,
        IGNORED_INVENTORY_MOVE,
        IGNORED_PURCHASE,
        OBSERVER_NOT_READY,
        CALLBACK_ALIVE,
        INFO
    }

    enum ObservationPath {
        TARGET_TRACKER,
        INVENTORY_DELTA,
        SACK_MESSAGE,
        ACTIONBAR,
        BLOCK_EVIDENCE,
        CHEST_REWARD,
        MOB_DROP,
        OTHER
    }

    record StopResult(
            Path runtimeLog,
            Path summaryFile,
            long bytesWritten,
            int uniqueItems,
            long truncatedCount) {
    }

    record RuntimeState(
            String targetId,
            String sessionState,
            String sessionId,
            String collectionState,
            String liveTrackerState) {
        RuntimeState {
            targetId = safeState(targetId, "?");
            sessionState = safeState(sessionState, "?");
            sessionId = safeState(sessionId, "NONE");
            collectionState = safeState(collectionState, "?");
            liveTrackerState = safeState(liveTrackerState, "?");
        }

        static RuntimeState unknown() {
            return new RuntimeState("?", "?", "NONE", "?", "?");
        }

        private static String safeState(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value;
        }
    }

    @FunctionalInterface
    interface RuntimeStateProvider {
        RuntimeState snapshot();
    }

    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter LINE_TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private static volatile boolean enabled;
    private static volatile RuntimeStateProvider runtimeStateProvider =
            RuntimeState::unknown;
    private static BufferedWriter writer;
    private static Path currentPath;
    private static Path summaryPath;
    private static String sessionStamp;
    private static long bytesWritten;
    private static long truncatedCount;
    private static final Map<String, Long> lastCallbackAlive =
            new LinkedHashMap<>();
    private static final Map<String, UniqueItemStats> uniqueItems =
            new LinkedHashMap<>();

    private static String lastMiningEvidence = "NONE";
    private static String lastItemGain = "NONE";
    private static String lastAcceptedOther = "NONE";
    private static long lastMiningEvidenceAt;
    private static long lastItemGainAt;
    private static long lastAcceptedOtherAt;

    private TrackingRuntimeTrace() {
    }

    static void installRuntimeStateProvider(RuntimeStateProvider provider) {
        runtimeStateProvider = provider == null
                ? RuntimeState::unknown
                : provider;
    }

    static synchronized boolean isEnabled() {
        return enabled && writer != null;
    }

    static synchronized Path currentPath() {
        return currentPath;
    }

    static synchronized Path summaryPath() {
        return summaryPath;
    }

    static synchronized Path enable() throws IOException {
        if (isEnabled()) {
            return currentPath;
        }
        Path dir = diagnosticsDir();
        Files.createDirectories(dir);
        sessionStamp = FILE_TIME.format(LocalDateTime.now());
        currentPath = dir.resolve("tracking-runtime-" + sessionStamp + ".log");
        summaryPath = dir.resolve("tracking-summary-" + sessionStamp + ".txt");
        writer = Files.newBufferedWriter(currentPath, StandardCharsets.UTF_8);
        bytesWritten = 0L;
        truncatedCount = 0L;
        enabled = true;
        lastCallbackAlive.clear();
        uniqueItems.clear();
        writeSessionHeader();
        return currentPath;
    }

    /**
     * Flushes, closes the runtime log, writes the unique-item summary, and
     * returns both paths. Safe to call when already off.
     */
    static synchronized StopResult disable() {
        Path runtime = currentPath;
        Path summary = summaryPath;
        int unique = uniqueItems.size();
        long truncated = truncatedCount;
        long bytes = bytesWritten;
        if (writer != null) {
            try {
                writeRaw("STOP",
                        "tracking runtime trace ended truncated="
                                + truncatedCount
                                + " uniqueItems=" + uniqueItems.size()
                                + " bytes=" + bytesWritten);
                writer.flush();
                writer.close();
            } catch (IOException ignored) {
            }
        }
        writer = null;
        enabled = false;
        if (summary != null) {
            try {
                writeSummaryFile(summary);
            } catch (IOException ignored) {
                summary = null;
            }
        }
        currentPath = null;
        summaryPath = null;
        sessionStamp = null;
        return new StopResult(runtime, summary, bytes, unique, truncated);
    }

    static synchronized void clearStatusMemory() {
        lastMiningEvidence = "NONE";
        lastItemGain = "NONE";
        lastAcceptedOther = "NONE";
        lastMiningEvidenceAt = 0L;
        lastItemGainAt = 0L;
        lastAcceptedOtherAt = 0L;
        uniqueItems.clear();
        lastCallbackAlive.clear();
    }

    static synchronized Path clearAndRestart() throws IOException {
        disable();
        clearStatusMemory();
        return enable();
    }

    static void callbackAlive(String callback) {
        if (!isEnabled() || callback == null || callback.isBlank()) {
            return;
        }
        long now = System.currentTimeMillis();
        synchronized (TrackingRuntimeTrace.class) {
            Long previous = lastCallbackAlive.get(callback);
            if (previous != null
                    && now - previous < CALLBACK_ALIVE_THROTTLE_MILLIS) {
                return;
            }
            lastCallbackAlive.put(callback, now);
        }
        event("CALLBACK_ALIVE",
                Map.of(
                        "name", callback,
                        "result", Reason.CALLBACK_ALIVE.name()));
    }

    static void stateTransition(String kind, String from, String to) {
        event("STATE_TRANSITION",
                Map.of(
                        "kind", nullToEmpty(kind),
                        "from", nullToEmpty(from),
                        "to", nullToEmpty(to),
                        "target", currentTargetId(),
                        "session", sessionState(),
                        "collection", collectionState(),
                        "area", currentAreaId(),
                        "sub_area", currentSubAreaId()));
    }

    static void event(String type, Map<String, String> fields) {
        if (!isEnabled()) {
            return;
        }
        StringBuilder details = new StringBuilder();
        if (fields != null) {
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) {
                    continue;
                }
                if (!details.isEmpty()) {
                    details.append('\n');
                }
                details.append(sanitizeKey(entry.getKey()))
                        .append('=')
                        .append(sanitizeValue(entry.getValue()));
            }
        }
        record(type, details.toString());
    }

    static void miningEvidence(
            String materialId,
            String source,
            int count,
            String blockId) {
        String summary = (materialId == null ? "?" : materialId)
                + " x" + Math.max(0, count)
                + (source == null ? "" : " via " + source);
        synchronized (TrackingRuntimeTrace.class) {
            lastMiningEvidence = summary;
            lastMiningEvidenceAt = System.currentTimeMillis();
        }
        SkyBlockItemIdentityResolver.Resolved resolved =
                SkyBlockItemIdentityResolver.fromKnownId(
                        materialId, materialId);
        event("BLOCK_BREAK_EVIDENCE",
                identityFields(
                        ObservationPath.BLOCK_EVIDENCE.name(),
                        resolved,
                        "",
                        nullToEmpty(blockId),
                        Math.max(0, count),
                        "MINING",
                        "EVIDENCE",
                        "ACCEPTED",
                        Reason.INFO.name(),
                        "",
                        nullToEmpty(source)));
    }

    static void inventoryScan(
            String materialId,
            long before,
            long after,
            long delta,
            String source,
            String result) {
        if (delta == 0L) {
            return;
        }
        synchronized (TrackingRuntimeTrace.class) {
            lastItemGain = nullToEmpty(materialId) + " " + signed(delta);
            lastItemGainAt = System.currentTimeMillis();
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("path", ObservationPath.INVENTORY_DELTA.name());
        fields.put("item", nullToEmpty(materialId));
        fields.put("before", Long.toString(before));
        fields.put("after", Long.toString(after));
        fields.put("delta", Long.toString(delta));
        fields.put("source", nullToEmpty(source));
        fields.put("target", currentTargetId());
        fields.put("session", sessionState());
        fields.put("collection", collectionState());
        fields.put("area", currentAreaId());
        fields.put("sub_area", currentSubAreaId());
        fields.put("result", nullToEmpty(result));
        event("INVENTORY_SCAN", fields);
    }

    /**
     * Canonical item-gain decision with raw/resolved identity fields.
     */
    static void observedItemGain(ObservedGain gain) {
        if (gain == null || !isEnabled()) {
            return;
        }
        SkyBlockItemIdentityResolver.Resolved resolved = gain.resolved() == null
                ? SkyBlockItemIdentityResolver.fromTextToken(gain.displayName())
                : gain.resolved();
        String result = nullToEmpty(gain.result());
        String reason = nullToEmpty(gain.reason());
        String classification = nullToEmpty(gain.classification());

        synchronized (TrackingRuntimeTrace.class) {
            lastItemGain = resolved.stableId() + " " + signed(gain.quantity());
            lastItemGainAt = System.currentTimeMillis();
            if (result.startsWith("ACCEPTED")
                    && classification.toUpperCase(Locale.ROOT).contains("OTHER")) {
                lastAcceptedOther = resolved.stableId()
                        + " +" + Math.max(0L, gain.quantity());
                lastAcceptedOtherAt = System.currentTimeMillis();
            }
            accumulateUnique(gain, resolved);
        }

        Map<String, String> fields = identityFields(
                gain.path() == null
                        ? ObservationPath.OTHER.name()
                        : gain.path().name(),
                resolved,
                nullToEmpty(gain.rawMinecraftId()),
                nullToEmpty(gain.rawBlockId()),
                gain.quantity(),
                nullToEmpty(gain.sourceClassification()),
                classification,
                result,
                reason,
                nullToEmpty(gain.duplicateOf()),
                nullToEmpty(gain.parserSource()));
        if (gain.rawTextToken() != null && !gain.rawTextToken().isBlank()) {
            fields = new LinkedHashMap<>(fields);
            fields.put("raw_text_item", gain.rawTextToken());
        }
        event(nullToEmpty(gain.eventType()).isEmpty()
                        ? "ITEM_GAIN"
                        : gain.eventType(),
                fields);
    }

    /** Test-only: seed unique-item memory without opening a trace file. */
    static synchronized void seedUniqueItemForTest(ObservedGain gain) {
        if (gain == null) {
            return;
        }
        SkyBlockItemIdentityResolver.Resolved resolved = gain.resolved() == null
                ? SkyBlockItemIdentityResolver.fromTextToken(gain.displayName())
                : gain.resolved();
        accumulateUnique(gain, resolved);
    }

    /** Backward-compatible decision logger used by existing call sites. */
    static void itemGainDecision(
            String eventType,
            String itemId,
            long qty,
            String evidence,
            String result,
            String reason) {
        itemGainDecision(
                eventType,
                itemId,
                qty,
                evidence,
                result,
                reason,
                "",
                pathFromEvidence(evidence));
    }

    static void itemGainDecision(
            String eventType,
            String itemId,
            long qty,
            String evidence,
            String result,
            String reason,
            String duplicateOf,
            ObservationPath path) {
        SkyBlockItemIdentityResolver.Resolved resolved =
                SkyBlockItemIdentityResolver.fromKnownId(itemId, itemId);
        String classification = classificationFromResult(result, reason);
        observedItemGain(new ObservedGain(
                eventType,
                path,
                resolved.displayName(),
                itemId,
                "",
                "",
                qty,
                "MINING",
                classification,
                result,
                reason,
                duplicateOf,
                evidence,
                resolved));
    }

    static void actionBarParsed(String itemName, long qty, String result) {
        SkyBlockItemIdentityResolver.Resolved resolved =
                SkyBlockItemIdentityResolver.fromTextToken(itemName);
        observedItemGain(new ObservedGain(
                "ACTIONBAR_ITEM_GAIN",
                ObservationPath.ACTIONBAR,
                itemName,
                resolved.stableId(),
                "",
                "",
                qty,
                "MINING",
                "PARSED",
                result,
                Reason.INFO.name(),
                "",
                "actionbar",
                resolved));
    }

    static void textSignal(
            ObservationPath path,
            String rawTextItem,
            long qty,
            SkyBlockItemIdentityResolver.Resolved resolved,
            String result,
            String reason,
            String duplicateOf) {
        observedItemGain(new ObservedGain(
                path == ObservationPath.SACK_MESSAGE
                        ? "SACK_ITEM_GAIN"
                        : "TEXT_ITEM_GAIN",
                path,
                rawTextItem,
                resolved == null ? "" : resolved.stableId(),
                "",
                "",
                qty,
                "MINING",
                classificationFromResult(result, reason),
                result,
                reason,
                duplicateOf,
                path == null ? "text" : path.name().toLowerCase(Locale.ROOT),
                resolved));
    }

    /**
     * Domain classification outcome for a previously observed item gain.
     * Emitted after the canonical pipeline decides OTHER / TARGET / REJECT.
     */
    static void itemGainClassified(
            ObservationPath path,
            SkyBlockItemIdentityResolver.Resolved resolved,
            long qty,
            String sourceClassification,
            String classification,
            String result,
            String reason) {
        observedItemGain(new ObservedGain(
                "ITEM_GAIN_CLASSIFIED",
                path,
                resolved == null ? "" : resolved.displayName(),
                resolved == null ? "" : resolved.stableId(),
                "",
                "",
                qty,
                nullToEmpty(sourceClassification),
                nullToEmpty(classification),
                nullToEmpty(result),
                nullToEmpty(reason),
                "",
                path == null ? "classified" : path.name().toLowerCase(Locale.ROOT),
                resolved));
    }

    /** Current Session acceptance of a classified non-target gain. */
    static void currentSessionIngest(
            String itemId,
            long qty,
            String source,
            String classification,
            String result) {
        event("CURRENT_SESSION_INGEST",
                Map.of(
                        "source", nullToEmpty(source),
                        "classification", nullToEmpty(classification),
                        "item", nullToEmpty(itemId),
                        "qty", Long.toString(qty),
                        "result", nullToEmpty(result),
                        "target", currentTargetId(),
                        "session", sessionState(),
                        "collection", collectionState(),
                        "area", currentAreaId()));
    }

    static void gameMessage(boolean overlay, String prefix) {
        if (!isEnabled()) {
            return;
        }
        String safe = nullToEmpty(prefix);
        if (!safe.startsWith("+")) {
            String key = overlay ? "ActionBarMsg" : "GameMsg";
            long now = System.currentTimeMillis();
            synchronized (TrackingRuntimeTrace.class) {
                Long previous = lastCallbackAlive.get(key);
                if (previous != null
                        && now - previous < CALLBACK_ALIVE_THROTTLE_MILLIS) {
                    return;
                }
                lastCallbackAlive.put(key, now);
            }
        }
        event(overlay ? "ACTIONBAR_MESSAGE" : "GAME_MESSAGE",
                Map.of(
                        "path", overlay
                                ? ObservationPath.ACTIONBAR.name()
                                : ObservationPath.OTHER.name(),
                        "overlay", Boolean.toString(overlay),
                        "prefix", safe,
                        "target", currentTargetId(),
                        "session", sessionState(),
                        "collection", collectionState(),
                        "area", currentAreaId()));
    }

    static synchronized String statusReport() {
        long now = System.currentTimeMillis();
        StringBuilder out = new StringBuilder();
        out.append("Tracking trace: ").append(isEnabled() ? "ON" : "OFF");
        if (currentPath != null) {
            out.append('\n').append("Runtime file: ").append(currentPath);
            out.append('\n').append("Summary file: ").append(summaryPath);
            out.append('\n').append("Bytes: ").append(bytesWritten)
                    .append('/').append(MAX_BYTES);
            out.append('\n').append("Unique items seen: ")
                    .append(uniqueItems.size());
        }
        out.append('\n').append("Schema: ").append(DIAGNOSTICS_SCHEMA);
        out.append('\n').append("Current Session: ").append(sessionState());
        out.append('\n').append("Session ID: ").append(sessionId());
        out.append('\n').append("Target: ").append(currentTargetId());
        out.append('\n').append("Area: ").append(currentAreaId());
        out.append('\n').append("Parent Area: ").append(currentParentAreaDisplay());
        out.append('\n').append("Sub-area: ").append(currentSubAreaDisplay());
        out.append('\n').append("Live tracker: ")
                .append(runtimeState().liveTrackerState());
        out.append('\n').append("Collection: ").append(collectionState());
        out.append('\n').append("Shared aim pending: ")
                .append(SharedMiningAimEvidence.pendingAimCount());
        out.append('\n').append("Last mining evidence: ")
                .append(formatAge(lastMiningEvidence, lastMiningEvidenceAt, now));
        out.append('\n').append("Last item gain: ")
                .append(formatAge(lastItemGain, lastItemGainAt, now));
        out.append('\n').append("Last accepted OTHER: ")
                .append(formatAge(lastAcceptedOther, lastAcceptedOtherAt, now));
        out.append('\n').append("Observers:");
        out.append("\n  Inventory       ACTIVE");
        out.append("\n  ActionBar       ACTIVE");
        out.append("\n  Chat/System     ACTIVE");
        out.append("\n  Block evidence  ACTIVE");
        out.append("\n  Shared aim      ACTIVE");
        out.append("\n  Sack parser     ACTIVE");
        return out.toString();
    }

    /** Test helper: format unique-item summary from in-memory stats. */
    static synchronized String formatUniqueSummaryForTest() {
        return formatUniqueSummaryBody();
    }

    record ObservedGain(
            String eventType,
            ObservationPath path,
            String displayName,
            String rawTextToken,
            String rawMinecraftId,
            String rawBlockId,
            long quantity,
            String sourceClassification,
            String classification,
            String result,
            String reason,
            String duplicateOf,
            String parserSource,
            SkyBlockItemIdentityResolver.Resolved resolved) {
    }

    private static void writeSessionHeader() {
        writeRaw("SESSION_HEADER",
                "schema=" + DIAGNOSTICS_SCHEMA
                        + "\nidentity_schema="
                        + SkyBlockItemIdentityResolver.SCHEMA_VERSION
                        + "\nrotclient_version="
                        + sanitizeValue(RotClientVersionLabel.resolveInstalledVersion())
                        + "\nminecraft_version="
                        + sanitizeValue(minecraftVersion())
                        + "\ncurrent_target=" + sanitizeValue(currentTargetId())
                        + "\ncurrent_session_id=" + sanitizeValue(sessionId())
                        + "\ncurrent_session_state=" + sanitizeValue(sessionState())
                        + "\ncollection=" + sanitizeValue(collectionState())
                        + "\narea=" + sanitizeValue(currentAreaId())
                        + "\nsub_area=" + sanitizeValue(currentSubAreaId())
                        + "\nprivacy=no_chat_body_no_tokens_no_account_ids"
                        + "\nmax_bytes=" + MAX_BYTES);
    }

    private static void writeSummaryFile(Path summary) throws IOException {
        String body = formatUniqueSummaryBody();
        Files.writeString(summary, body, StandardCharsets.UTF_8);
    }

    private static String formatUniqueSummaryBody() {
        StringBuilder out = new StringBuilder();
        out.append("Rot Client tracking unique-item summary\n");
        out.append("schema=").append(DIAGNOSTICS_SCHEMA).append('\n');
        out.append("generated=")
                .append(FILE_TIME.format(LocalDateTime.now())).append('\n');
        out.append("target=").append(currentTargetId()).append('\n');
        out.append("session_id=").append(sessionId()).append('\n');
        out.append("session_state=").append(sessionState()).append('\n');
        out.append("area=").append(currentAreaId()).append('\n');
        out.append("sub_area=").append(currentSubAreaId()).append('\n');
        out.append("unique_items=").append(uniqueItems.size()).append('\n');
        out.append("truncated_events=").append(truncatedCount).append('\n');
        out.append("privacy=no_chat_body_no_tokens_no_account_ids\n");
        out.append('\n');

        List<UniqueItemStats> ordered = new ArrayList<>(uniqueItems.values());
        ordered.sort(Comparator
                .comparing((UniqueItemStats s) -> !s.catalogMatch)
                .thenComparing(s -> s.resolvedId, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(s -> s.displayName, String.CASE_INSENSITIVE_ORDER));

        if (ordered.isEmpty()) {
            out.append("(no item gains observed)\n");
            return out.toString();
        }

        int index = 1;
        for (UniqueItemStats stats : ordered) {
            out.append("ITEM #").append(index++).append('\n');
            out.append("DISPLAY NAME=").append(stats.displayName).append('\n');
            out.append("RAW MINECRAFT ID=")
                    .append(emptyOr(stats.rawMinecraftId, "UNKNOWN")).append('\n');
            out.append("HYPIXEL ID=")
                    .append(emptyOr(stats.hypixelId, "UNKNOWN")).append('\n');
            out.append("RESOLVED ID=")
                    .append(emptyOr(stats.resolvedId, "UNKNOWN")).append('\n');
            out.append("AREAS SEEN=")
                    .append(joinSet(stats.areas)).append('\n');
            out.append("SIGNAL SOURCES=")
                    .append(joinSet(stats.sources)).append('\n');
            out.append("TOTAL OBSERVED QUANTITY=")
                    .append(stats.totalQuantity).append('\n');
            out.append("OBSERVATION EVENTS=")
                    .append(stats.observationEvents).append('\n');
            out.append("ACCEPTED TARGET QTY=")
                    .append(stats.targetQuantity).append('\n');
            out.append("ACCEPTED OTHERS QTY=")
                    .append(stats.othersQuantity).append('\n');
            out.append("UNATTRIBUTED QTY=")
                    .append(stats.unattributedQuantity).append('\n');
            out.append("REJECTED QTY=")
                    .append(stats.rejectedQuantity).append('\n');
            out.append("TARGET OBSERVATIONS=")
                    .append(stats.targetCount).append('\n');
            out.append("OTHERS OBSERVATIONS=")
                    .append(stats.othersCount).append('\n');
            out.append("REJECTED OBSERVATIONS=")
                    .append(stats.rejectedCount).append('\n');
            out.append("LAST TERMINAL REASON=")
                    .append(emptyOr(stats.lastReason, "NONE")).append('\n');
            out.append("CATALOG MATCH=")
                    .append(stats.catalogMatch ? "YES" : "NO").append('\n');
            out.append("BAZAAR MAPPING=")
                    .append(emptyOr(stats.bazaarId, "NONE")).append('\n');
            out.append("NOTES=").append(stats.notes()).append('\n');
            out.append('\n');
        }
        return out.toString();
    }

    private static void accumulateUnique(
            ObservedGain gain,
            SkyBlockItemIdentityResolver.Resolved resolved) {
        if (uniqueItems.size() >= MAX_UNIQUE_ITEMS
                && !uniqueItems.containsKey(uniqueKey(resolved, gain))) {
            return;
        }
        String key = uniqueKey(resolved, gain);
        UniqueItemStats stats = uniqueItems.computeIfAbsent(
                key, ignored -> UniqueItemStats.create(resolved, gain));
        stats.observe(gain, resolved, currentAreaId());
    }

    private static String uniqueKey(
            SkyBlockItemIdentityResolver.Resolved resolved,
            ObservedGain gain) {
        return SkyBlockItemIdentityResolver.normalizeKey(resolved.stableId())
                + "|"
                + SkyBlockItemIdentityResolver.normalizeKey(resolved.displayName())
                + "|"
                + SkyBlockItemIdentityResolver.normalizeKey(gain.rawMinecraftId());
    }

    private static Map<String, String> identityFields(
            String path,
            SkyBlockItemIdentityResolver.Resolved resolved,
            String rawMinecraftId,
            String rawBlockId,
            long quantity,
            String sourceClassification,
            String classification,
            String result,
            String reason,
            String duplicateOf,
            String parserSource) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("path", path);
        fields.put("raw_minecraft_id", emptyOr(rawMinecraftId, "UNKNOWN"));
        fields.put("raw_block_id", emptyOr(rawBlockId, ""));
        fields.put("hypixel_id", emptyOr(resolved.hypixelId(), "UNKNOWN"));
        fields.put("resolved_id", emptyOr(resolved.stableId(), "UNKNOWN"));
        fields.put("display", emptyOr(resolved.displayName(), "UNKNOWN"));
        fields.put("qty", Long.toString(quantity));
        fields.put("area", currentAreaId());
        fields.put("target", currentTargetId());
        fields.put("source_classification", sourceClassification);
        fields.put("classification", classification);
        fields.put("result", result);
        fields.put("reason", reason);
        if (duplicateOf != null && !duplicateOf.isBlank()) {
            fields.put("duplicate_of", duplicateOf);
        }
        fields.put("price_lookup_id", emptyOr(resolved.bazaarId(), "NONE"));
        fields.put("catalog_match",
                SkyBlockItemIdentityResolver.catalogMatchLabel(
                        resolved.catalogMatch()));
        fields.put("identity_confidence", resolved.confidence());
        fields.put("session", sessionState());
        fields.put("collection", collectionState());
        if (parserSource != null && !parserSource.isBlank()) {
            fields.put("parser_source", parserSource);
        }
        return fields;
    }

    private static synchronized void record(String type, String details) {
        if (!enabled || writer == null) {
            return;
        }
        if (bytesWritten >= MAX_BYTES) {
            truncatedCount++;
            return;
        }
        writeRaw(type, details);
    }

    private static void writeRaw(String type, String details) {
        if (writer == null) {
            return;
        }
        try {
            String line = "[" + LINE_TIME.format(LocalDateTime.now()) + "]\n"
                    + "EVENT=" + sanitizeKey(type)
                    + (details == null || details.isBlank()
                    ? ""
                    : "\n" + details)
                    + "\n";
            writer.write(line);
            writer.flush();
            bytesWritten += line.length();
            if (bytesWritten >= MAX_BYTES) {
                writer.write("EVENT=TRACE_TRUNCATED reason=MAX_BYTES\n");
                writer.flush();
            }
        } catch (IOException ignored) {
        }
    }

    private static Path diagnosticsDir() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("rotclient")
                .resolve("diagnostics");
    }

    private static ObservationPath pathFromEvidence(String evidence) {
        if (evidence == null) {
            return ObservationPath.OTHER;
        }
        String lower = evidence.toLowerCase(Locale.ROOT);
        if (lower.contains("inventory")) {
            return ObservationPath.INVENTORY_DELTA;
        }
        if (lower.contains("actionbar") || lower.contains("action-bar")) {
            return ObservationPath.ACTIONBAR;
        }
        if (lower.contains("sack")) {
            return ObservationPath.SACK_MESSAGE;
        }
        if (lower.contains("break") || lower.contains("block")) {
            return ObservationPath.BLOCK_EVIDENCE;
        }
        if (lower.contains("target")) {
            return ObservationPath.TARGET_TRACKER;
        }
        return ObservationPath.OTHER;
    }

    private static String classificationFromResult(String result, String reason) {
        String r = nullToEmpty(reason).toUpperCase(Locale.ROOT);
        String res = nullToEmpty(result).toUpperCase(Locale.ROOT);
        if (r.contains("ACCEPTED_TARGET") || res.equals("TARGET")
                || (res.contains("TARGET") && !res.contains("AUTHORITY"))) {
            return "TARGET";
        }
        if (r.contains("OTHER") || res.contains("OTHER")) {
            return "OTHER";
        }
        if (r.contains("UNATTRIBUTED") || res.contains("UNATTRIBUTED")) {
            return "UNATTRIBUTED";
        }
        if (r.contains("CHEST")) {
            return "CHEST";
        }
        if (r.contains("MOB")) {
            return "MOB";
        }
        if (res.contains("REJECT") || res.contains("IGNORE")) {
            return "REJECTED";
        }
        return "UNKNOWN";
    }

    private static String currentTargetId() {
        return runtimeState().targetId();
    }

    private static String sessionState() {
        return runtimeState().sessionState();
    }

    private static String sessionId() {
        return runtimeState().sessionId();
    }

    private static String collectionState() {
        return runtimeState().collectionState();
    }

    private static RuntimeState runtimeState() {
        try {
            RuntimeState state = runtimeStateProvider.snapshot();
            return state == null ? RuntimeState.unknown() : state;
        } catch (RuntimeException ignored) {
            return RuntimeState.unknown();
        }
    }

    private static String currentAreaId() {
        try {
            return SkyBlockAreaDetector.detect().id();
        } catch (RuntimeException ignored) {
            return "UNKNOWN";
        }
    }

    private static String currentParentAreaDisplay() {
        try {
            SkyBlockLocation location = SkyBlockAreaDetector.detectLocation();
            return location.displayParent();
        } catch (RuntimeException ignored) {
            return "Unknown";
        }
    }

    private static String currentSubAreaDisplay() {
        try {
            SkyBlockLocation location = SkyBlockAreaDetector.detectLocation();
            if (!location.hasSubArea()) {
                return "(none)";
            }
            return location.displaySubArea();
        } catch (RuntimeException ignored) {
            return "(none)";
        }
    }

    private static String currentSubAreaId() {
        try {
            SkyBlockLocation location = SkyBlockAreaDetector.detectLocation();
            return location.hasSubArea()
                    ? location.subArea().id()
                    : "";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static String minecraftVersion() {
        try {
            SharedConstants.tryDetectVersion();
            return SharedConstants.getCurrentVersion().name();
        } catch (RuntimeException ignored) {
            try {
                return SharedConstants.getCurrentVersion().id();
            } catch (RuntimeException ignoredAgain) {
                return "26.2";
            }
        }
    }

    private static String formatAge(String label, long at, long now) {
        if (label == null || "NONE".equals(label) || at <= 0L) {
            return "NONE";
        }
        double seconds = Math.max(0L, now - at) / 1000.0;
        return String.format(Locale.ROOT, "%s (%.1fs ago)", label, seconds);
    }

    private static String signed(long delta) {
        return delta >= 0 ? "+" + delta : Long.toString(delta);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String emptyOr(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static String joinSet(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return "NONE";
        }
        return String.join(",", values);
    }

    private static String sanitizeKey(String key) {
        return sanitizeValue(key).replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private static String sanitizeValue(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .trim();
        if (cleaned.length() > 160) {
            return cleaned.substring(0, 160);
        }
        return cleaned;
    }

    private static final class UniqueItemStats {
        private String displayName;
        private String rawMinecraftId;
        private String hypixelId;
        private String resolvedId;
        private String bazaarId;
        private boolean catalogMatch;
        private long totalQuantity;
        private long targetQuantity;
        private long othersQuantity;
        private long unattributedQuantity;
        private long rejectedQuantity;
        private long observationEvents;
        private long targetCount;
        private long othersCount;
        private long rejectedCount;
        private String lastReason = "NONE";
        private final Set<String> areas = new LinkedHashSet<>();
        private final Set<String> sources = new LinkedHashSet<>();

        private static UniqueItemStats create(
                SkyBlockItemIdentityResolver.Resolved resolved,
                ObservedGain gain) {
            UniqueItemStats stats = new UniqueItemStats();
            stats.displayName = emptyOr(resolved.displayName(), "UNKNOWN");
            stats.rawMinecraftId = emptyOr(gain.rawMinecraftId(), "UNKNOWN");
            stats.hypixelId = emptyOr(resolved.hypixelId(), "UNKNOWN");
            stats.resolvedId = emptyOr(resolved.stableId(), "UNKNOWN");
            stats.bazaarId = emptyOr(resolved.bazaarId(), "NONE");
            stats.catalogMatch = resolved.catalogMatch();
            return stats;
        }

        private void observe(
                ObservedGain gain,
                SkyBlockItemIdentityResolver.Resolved resolved,
                String area) {
            observationEvents++;
            long qty = Math.max(0L, gain.quantity());
            String eventType = nullToEmpty(gain.eventType()).toUpperCase(Locale.ROOT);
            // Observed quantity comes from raw signal events only — classified
            // / ingest follow-ups must not double-count the same physical gain.
            boolean rawObservation = eventType.equals("SACK_ITEM_GAIN")
                    || eventType.equals("TEXT_ITEM_GAIN")
                    || eventType.equals("ACTIONBAR_ITEM_GAIN")
                    || eventType.equals("INVENTORY_ITEM_GAIN")
                    || eventType.isEmpty();
            boolean terminal = eventType.equals("ITEM_GAIN_CLASSIFIED")
                    || eventType.equals("CURRENT_SESSION_INGEST")
                    || resultIsTerminal(gain);
            if (rawObservation && qty > 0L) {
                totalQuantity = safeAdd(totalQuantity, qty);
            }
            if (area != null && !area.isBlank()) {
                areas.add(area);
            }
            if (gain.path() != null) {
                sources.add(gain.path().name());
            }
            String classification = nullToEmpty(gain.classification())
                    .toUpperCase(Locale.ROOT);
            String result = nullToEmpty(gain.result()).toUpperCase(Locale.ROOT);
            String reason = nullToEmpty(gain.reason());
            if (terminal && !reason.isBlank() && !reason.equals("INFO")) {
                lastReason = reason;
            } else if (!terminal && !reason.isBlank() && lastReason.equals("NONE")) {
                lastReason = reason;
            }
            if (!resolved.catalogMatch()) {
                catalogMatch = false;
            }
            if ((rawMinecraftId.equals("UNKNOWN") || rawMinecraftId.isBlank())
                    && gain.rawMinecraftId() != null
                    && !gain.rawMinecraftId().isBlank()) {
                rawMinecraftId = gain.rawMinecraftId();
            }
            if (!terminal) {
                return;
            }
            if (result.startsWith("ACCEPTED")
                    && classification.contains("TARGET")) {
                targetCount++;
                targetQuantity = safeAdd(targetQuantity, qty);
            } else if (result.startsWith("ACCEPTED")
                    && classification.contains("OTHER")) {
                othersCount++;
                othersQuantity = safeAdd(othersQuantity, qty);
            } else if (classification.contains("UNATTRIBUTED")
                    || result.contains("UNATTRIBUTED")) {
                unattributedQuantity = safeAdd(unattributedQuantity, qty);
            } else if (result.contains("REJECT") || result.contains("IGNORE")) {
                rejectedCount++;
                rejectedQuantity = safeAdd(rejectedQuantity, qty);
            }
        }

        private static boolean resultIsTerminal(ObservedGain gain) {
            String result = nullToEmpty(gain.result()).toUpperCase(Locale.ROOT);
            String classification = nullToEmpty(gain.classification())
                    .toUpperCase(Locale.ROOT);
            if (result.equals("OBSERVED") || result.equals("PARSED")
                    || result.equals("INFO")) {
                return false;
            }
            return result.startsWith("ACCEPTED")
                    || result.contains("REJECT")
                    || result.contains("IGNORE")
                    || result.contains("UNATTRIBUTED")
                    || classification.contains("UNATTRIBUTED")
                    || classification.equals("OTHER_MINED")
                    || classification.equals("TARGET");
        }

        private String notes() {
            List<String> notes = new ArrayList<>();
            if (!catalogMatch) {
                notes.add("unknown_or_unverified_catalog_identity");
            }
            if (rejectedCount > 0 && othersCount == 0 && targetCount == 0) {
                notes.add("only_rejected_observations");
            }
            if (sources.contains(ObservationPath.ACTIONBAR.name())
                    && !sources.contains(ObservationPath.INVENTORY_DELTA.name())) {
                notes.add("text_signal_without_inventory_delta");
            }
            if (sources.contains(ObservationPath.BLOCK_EVIDENCE.name())
                    && othersQuantity == 0L
                    && targetQuantity == 0L) {
                notes.add("block_evidence_without_quantity_signal");
            }
            if ("HARD_STONE".equals(resolvedId)
                    && !sources.contains(ObservationPath.SACK_MESSAGE.name())
                    && !sources.contains(ObservationPath.ACTIONBAR.name())
                    && !sources.contains(ObservationPath.INVENTORY_DELTA.name())) {
                notes.add("hard_stone_quantity_signal_missing");
            }
            return notes.isEmpty() ? "NONE" : String.join(";", notes);
        }

        private static long safeAdd(long left, long right) {
            try {
                return Math.addExact(left, right);
            } catch (ArithmeticException ex) {
                return Long.MAX_VALUE;
            }
        }
    }
}
