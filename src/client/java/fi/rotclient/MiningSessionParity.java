package fi.rotclient;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Read-only comparison state for the current shadow-session target context.
 * Live totals and mirrored shadow totals are supplied as immutable scalar
 * samples; this component never retains or mutates either accounting model.
 */
final class MiningSessionParity {
    private final MiningSessionShadowObserver.DiagnosticSink diagnostics;

    private Context context;
    private LiveBaseline baseline;
    private Status status = Status.NOT_CHECKED;
    private long mismatchCount;
    private MismatchDetail lastMismatch;
    private Sample lastSample;
    private ComparisonResult lastResult;
    private final Map<ComparisonKey, Status> domainStatuses =
            new HashMap<>();
    private final Map<ComparisonKey, MismatchDetail> currentMismatches =
            new LinkedHashMap<>();

    MiningSessionParity(
            MiningSessionShadowObserver.DiagnosticSink diagnostics) {
        this(diagnostics, 0L);
    }

    MiningSessionParity(
            MiningSessionShadowObserver.DiagnosticSink diagnostics,
            long initialMismatchCount) {
        if (diagnostics == null) {
            throw new IllegalArgumentException(
                    "Parity diagnostic sink cannot be null");
        }
        if (initialMismatchCount < 0L) {
            throw new IllegalArgumentException(
                    "Mismatch count cannot be negative");
        }
        this.diagnostics = diagnostics;
        this.mismatchCount = initialMismatchCount;
    }

    /**
     * Begins a new selection context. Historical mismatches remain counted,
     * while the previous baseline, status and idempotence key are invalidated.
     */
    void beginContext(
            Context nextContext,
            LiveBaseline nextBaseline) {
        if (nextContext == null || nextBaseline == null) {
            throw new IllegalArgumentException(
                    "Parity context and baseline cannot be null");
        }
        if (nextContext.selection() != nextBaseline.selection()) {
            throw new IllegalArgumentException(
                    "Parity context and baseline selections must match");
        }

        context = nextContext;
        baseline = nextBaseline;
        status = Status.NOT_CHECKED;
        lastMismatch = null;
        lastSample = null;
        lastResult = null;
        domainStatuses.clear();
        currentMismatches.clear();
    }

    /**
     * Compares one compatible domain. The live value is cumulative live state;
     * the shadow value is the current-epoch mirrored TARGET_MINED quantity.
     */
    ComparisonResult compare(Sample sample) {
        if (sample == null) {
            throw new IllegalArgumentException(
                    "Parity sample cannot be null");
        }
        if (context == null) {
            return ignored(sample, Status.NOT_CHECKED);
        }
        if (!sameContext(sample.context(), context)
                || sample.category() != MiningSessionCategory.TARGET_MINED) {
            return ignored(sample, Status.INCOMPARABLE);
        }
        if (sample.equals(lastSample)) {
            return lastResult;
        }
        if (!matchesSelectedTarget(sample)
                || sample.comparisonUnit()
                != expectedUnit(sample.domain())) {
            return incomparable(sample);
        }

        Long baselineQuantity = baselineQuantity(sample);
        if (baselineQuantity == null
                || sample.liveQuantity() < baselineQuantity) {
            return incomparable(sample);
        }

        long comparableLiveQuantity =
                sample.liveQuantity() - baselineQuantity;
        if (comparableLiveQuantity == sample.shadowQuantity()) {
            ComparisonResult result = new ComparisonResult(
                    Status.MATCH,
                    true,
                    sample.domain(),
                    sample.comparisonUnit(),
                    resourceId(sample),
                    comparableLiveQuantity,
                    sample.shadowQuantity(),
                    mismatchCount,
                    null);
            ComparisonKey key = ComparisonKey.from(sample);
            domainStatuses.put(key, Status.MATCH);
            currentMismatches.remove(key);
            refreshAggregateStatus();
            lastSample = sample;
            lastResult = result;
            emit(
                    "MINING_SESSION_PARITY_OK",
                    sample,
                    comparableLiveQuantity,
                    mismatchCount,
                    Status.MATCH);
            return result;
        }

        long nextMismatchCount = Math.addExact(mismatchCount, 1L);
        MismatchDetail detail = new MismatchDetail(
                context.sessionId(),
                context.sessionEpoch(),
                context.selectionEpoch(),
                context.selection(),
                sample.domain(),
                sample.comparisonUnit(),
                resourceId(sample),
                comparableLiveQuantity,
                sample.shadowQuantity(),
                sample.observedAtMillis());
        ComparisonResult result = new ComparisonResult(
                Status.MISMATCH,
                true,
                sample.domain(),
                sample.comparisonUnit(),
                detail.resourceId(),
                comparableLiveQuantity,
                sample.shadowQuantity(),
                nextMismatchCount,
                detail);

        ComparisonKey key = ComparisonKey.from(sample);
        mismatchCount = nextMismatchCount;
        domainStatuses.put(key, Status.MISMATCH);
        currentMismatches.remove(key);
        currentMismatches.put(key, detail);
        refreshAggregateStatus();
        lastMismatch = detail;
        lastSample = sample;
        lastResult = result;
        emit(
                "MINING_SESSION_PARITY_MISMATCH",
                sample,
                comparableLiveQuantity,
                nextMismatchCount,
                Status.MISMATCH);
        return result;
    }

    Snapshot snapshot() {
        return new Snapshot(
                status,
                mismatchCount,
                lastMismatch);
    }

    void reset() {
        context = null;
        baseline = null;
        status = Status.NOT_CHECKED;
        mismatchCount = 0L;
        lastMismatch = null;
        lastSample = null;
        lastResult = null;
        domainStatuses.clear();
        currentMismatches.clear();
    }

    private ComparisonResult incomparable(Sample sample) {
        ComparisonResult result = ignored(
                sample,
                Status.INCOMPARABLE);
        ComparisonKey key = ComparisonKey.from(sample);
        domainStatuses.put(key, Status.INCOMPARABLE);
        currentMismatches.remove(key);
        refreshAggregateStatus();
        lastSample = sample;
        lastResult = result;
        return result;
    }

    private void refreshAggregateStatus() {
        if (domainStatuses.containsValue(Status.MISMATCH)) {
            status = Status.MISMATCH;
            if (!currentMismatches.isEmpty()
                    && (lastMismatch == null
                    || !currentMismatches.containsValue(lastMismatch))) {
                lastMismatch = null;
                for (MismatchDetail detail : currentMismatches.values()) {
                    lastMismatch = detail;
                }
            }
            return;
        }
        if (domainStatuses.containsValue(Status.INCOMPARABLE)) {
            status = Status.INCOMPARABLE;
            return;
        }
        status = domainStatuses.containsValue(Status.MATCH)
                ? Status.MATCH
                : Status.NOT_CHECKED;
    }

    private ComparisonResult ignored(
            Sample sample,
            Status resultStatus) {
        return new ComparisonResult(
                resultStatus,
                false,
                sample.domain(),
                sample.comparisonUnit(),
                resourceId(sample),
                0L,
                sample.shadowQuantity(),
                mismatchCount,
                null);
    }

    private Long baselineQuantity(Sample sample) {
        return switch (sample.domain()) {
            case GEMSTONE_TIER ->
                    baseline.gemstoneTierQuantities()
                            .get(sample.gemstoneTier());
            case GEMSTONE_ROUGH_EQUIVALENT ->
                    baseline.gemstoneRoughEquivalent();
            case GEMSTONE_BLOCKS -> baseline.gemstoneBlocks();
            case MATERIAL_NORMALIZED ->
                    baseline.materialNormalizedQuantities()
                            .get(sample.material());
            case MATERIAL_BLOCKS ->
                    baseline.materialBlockQuantities()
                            .get(sample.material());
        };
    }

    private boolean matchesSelectedTarget(Sample sample) {
        TrackerSelection selection = context.selection();
        return switch (sample.domain()) {
            case GEMSTONE_TIER,
                    GEMSTONE_ROUGH_EQUIVALENT,
                    GEMSTONE_BLOCKS ->
                    selection.isGemstone()
                            && selection.gemstone() == sample.gemstone();
            case MATERIAL_NORMALIZED,
                    MATERIAL_BLOCKS ->
                    sample.material() != TrackedMaterial.TUNGSTEN
                            && selection.isMaterial()
                            && selection.materialTarget()
                            .includes(sample.material());
        };
    }

    private void emit(
            String marker,
            Sample sample,
            long comparableLiveQuantity,
            long currentMismatchCount,
            Status eventStatus) {
        if (!diagnostics.isActive()) {
            return;
        }
        diagnostics.record(
                marker,
                "sessionId=" + context.sessionId()
                        + " sessionEpoch=" + context.sessionEpoch()
                        + " selection=" + context.selection().id()
                        + " selectionEpoch=" + context.selectionEpoch()
                        + " resourceId=" + resourceId(sample)
                        + " category=TARGET_MINED"
                        + " parityStatus=" + eventStatus.name()
                        + " liveQuantity=" + comparableLiveQuantity
                        + " shadowQuantity=" + sample.shadowQuantity()
                        + " comparisonUnit="
                        + sample.comparisonUnit().name()
                        + " mismatchCount=" + currentMismatchCount
                        + " observedAt=" + sample.observedAtMillis());
    }

    private static boolean sameContext(
            Context sampleContext,
            Context currentContext) {
        return sampleContext.sessionId() == currentContext.sessionId()
                && sampleContext.sessionEpoch()
                == currentContext.sessionEpoch()
                && sampleContext.selectionEpoch()
                == currentContext.selectionEpoch()
                && sampleContext.selection()
                == currentContext.selection();
    }

    private static ComparisonUnit expectedUnit(Domain domain) {
        return switch (domain) {
            case GEMSTONE_TIER -> ComparisonUnit.ITEM_QUANTITY;
            case GEMSTONE_ROUGH_EQUIVALENT ->
                    ComparisonUnit.ROUGH_EQUIVALENT;
            case GEMSTONE_BLOCKS,
                    MATERIAL_BLOCKS -> ComparisonUnit.BLOCK_COUNT;
            case MATERIAL_NORMALIZED ->
                    ComparisonUnit.RAW_EQUIVALENT;
        };
    }

    private static String resourceId(Sample sample) {
        return switch (sample.domain()) {
            case GEMSTONE_TIER -> sample.gemstone()
                    .bazaarId(sample.gemstoneTier());
            case GEMSTONE_ROUGH_EQUIVALENT ->
                    "GEMSTONE_" + sample.gemstone().id()
                            + "_ROUGH_EQUIVALENT";
            case GEMSTONE_BLOCKS ->
                    "GEMSTONE_" + sample.gemstone().id() + "_BLOCKS";
            case MATERIAL_NORMALIZED ->
                    sample.material().rawBazaarId();
            case MATERIAL_BLOCKS ->
                    "MATERIAL_" + sample.material().id() + "_BLOCKS";
        };
    }

    enum Status {
        NOT_CHECKED,
        MATCH,
        MISMATCH,
        INCOMPARABLE
    }

    enum Domain {
        GEMSTONE_TIER,
        GEMSTONE_ROUGH_EQUIVALENT,
        GEMSTONE_BLOCKS,
        MATERIAL_NORMALIZED,
        MATERIAL_BLOCKS
    }

    enum ComparisonUnit {
        ITEM_QUANTITY,
        ROUGH_EQUIVALENT,
        BLOCK_COUNT,
        RAW_EQUIVALENT
    }

    private record ComparisonKey(
            Domain domain,
            TrackedMaterial material,
            GemstoneType gemstone,
            GemstoneTier gemstoneTier) {
        private static ComparisonKey from(Sample sample) {
            return new ComparisonKey(
                    sample.domain(),
                    sample.material(),
                    sample.gemstone(),
                    sample.gemstoneTier());
        }
    }

    record Context(
            long sessionId,
            long sessionEpoch,
            long selectionEpoch,
            TrackerSelection selection,
            long startedAtMillis) {
        Context {
            if (sessionId <= 0L) {
                throw new IllegalArgumentException(
                        "Session ID must be positive");
            }
            if (sessionEpoch < 0L || selectionEpoch < 0L) {
                throw new IllegalArgumentException(
                        "Parity epochs cannot be negative");
            }
            if (selection == null) {
                throw new IllegalArgumentException(
                        "Parity selection cannot be null");
            }
            if (startedAtMillis < 0L) {
                throw new IllegalArgumentException(
                        "Parity context timestamp cannot be negative");
            }
        }
    }

    record LiveBaseline(
            TrackerSelection selection,
            Map<TrackedMaterial, Long> materialNormalizedQuantities,
            Map<TrackedMaterial, Long> materialBlockQuantities,
            Map<GemstoneTier, Long> gemstoneTierQuantities,
            Long gemstoneRoughEquivalent,
            Long gemstoneBlocks,
            long capturedAtMillis) {
        LiveBaseline {
            if (selection == null
                    || materialNormalizedQuantities == null
                    || materialBlockQuantities == null
                    || gemstoneTierQuantities == null) {
                throw new IllegalArgumentException(
                        "Live baseline fields cannot be null");
            }
            if (capturedAtMillis < 0L) {
                throw new IllegalArgumentException(
                        "Baseline timestamp cannot be negative");
            }

            materialNormalizedQuantities = immutableNonNegative(
                    materialNormalizedQuantities,
                    TrackedMaterial.class,
                    "Material normalized baseline");
            materialBlockQuantities = immutableNonNegative(
                    materialBlockQuantities,
                    TrackedMaterial.class,
                    "Material block baseline");
            gemstoneTierQuantities = immutableNonNegative(
                    gemstoneTierQuantities,
                    GemstoneTier.class,
                    "Gemstone tier baseline");

            if (selection.isMaterial()) {
                if (!gemstoneTierQuantities.isEmpty()
                        || gemstoneRoughEquivalent != null
                        || gemstoneBlocks != null) {
                    throw new IllegalArgumentException(
                            "Material baseline cannot carry gemstone values");
                }
            } else {
                if (!materialNormalizedQuantities.isEmpty()
                        || !materialBlockQuantities.isEmpty()
                        || gemstoneRoughEquivalent == null
                        || gemstoneBlocks == null) {
                    throw new IllegalArgumentException(
                            "Gemstone baseline fields are incomplete");
                }
                requireNonNegative(
                        gemstoneRoughEquivalent,
                        "Gemstone Rough Equivalent baseline");
                requireNonNegative(
                        gemstoneBlocks,
                        "Gemstone block baseline");
            }
        }

        static LiveBaseline material(
                TrackerSelection selection,
                Map<TrackedMaterial, Long> normalizedQuantities,
                Map<TrackedMaterial, Long> blockQuantities,
                long capturedAtMillis) {
            if (selection == null || !selection.isMaterial()) {
                throw new IllegalArgumentException(
                        "Material baseline requires a material selection");
            }
            return new LiveBaseline(
                    selection,
                    normalizedQuantities,
                    blockQuantities,
                    Map.of(),
                    null,
                    null,
                    capturedAtMillis);
        }

        static LiveBaseline gemstone(
                TrackerSelection selection,
                Map<GemstoneTier, Long> tierQuantities,
                long roughEquivalent,
                long blocks,
                long capturedAtMillis) {
            if (selection == null || !selection.isGemstone()) {
                throw new IllegalArgumentException(
                        "Gemstone baseline requires a gemstone selection");
            }
            return new LiveBaseline(
                    selection,
                    Map.of(),
                    Map.of(),
                    tierQuantities,
                    roughEquivalent,
                    blocks,
                    capturedAtMillis);
        }

        private static <E extends Enum<E>> Map<E, Long>
        immutableNonNegative(
                Map<E, Long> values,
                Class<E> enumType,
                String label) {
            EnumMap<E, Long> copied = new EnumMap<>(enumType);
            for (Map.Entry<E, Long> entry : values.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    throw new IllegalArgumentException(
                            label + " cannot contain nulls");
                }
                requireNonNegative(entry.getValue(), label);
                copied.put(entry.getKey(), entry.getValue());
            }
            return Map.copyOf(copied);
        }

        private static void requireNonNegative(
                long value,
                String label) {
            if (value < 0L) {
                throw new IllegalArgumentException(
                        label + " cannot be negative");
            }
        }
    }

    record Sample(
            Context context,
            MiningSessionCategory category,
            Domain domain,
            ComparisonUnit comparisonUnit,
            TrackedMaterial material,
            GemstoneType gemstone,
            GemstoneTier gemstoneTier,
            long shadowQuantity,
            long liveQuantity,
            long observedAtMillis) {
        Sample {
            if (context == null
                    || category == null
                    || domain == null
                    || comparisonUnit == null) {
                throw new IllegalArgumentException(
                        "Parity sample fields cannot be null");
            }
            if (shadowQuantity < 0L
                    || liveQuantity < 0L
                    || observedAtMillis < 0L) {
                throw new IllegalArgumentException(
                        "Parity sample quantities and timestamp cannot be negative");
            }

            boolean materialDomain = domain == Domain.MATERIAL_NORMALIZED
                    || domain == Domain.MATERIAL_BLOCKS;
            boolean tierDomain = domain == Domain.GEMSTONE_TIER;
            if (materialDomain) {
                if (material == null
                        || gemstone != null
                        || gemstoneTier != null) {
                    throw new IllegalArgumentException(
                            "Material parity sample identity is invalid");
                }
            } else if (gemstone == null
                    || material != null
                    || (tierDomain != (gemstoneTier != null))) {
                throw new IllegalArgumentException(
                        "Gemstone parity sample identity is invalid");
            }
        }

        static Sample gemstoneTier(
                Context context,
                MiningSessionCategory category,
                GemstoneType gemstone,
                GemstoneTier tier,
                long shadowQuantity,
                long liveQuantity,
                long observedAtMillis) {
            return new Sample(
                    context,
                    category,
                    Domain.GEMSTONE_TIER,
                    ComparisonUnit.ITEM_QUANTITY,
                    null,
                    gemstone,
                    tier,
                    shadowQuantity,
                    liveQuantity,
                    observedAtMillis);
        }

        static Sample gemstoneRoughEquivalent(
                Context context,
                MiningSessionCategory category,
                GemstoneType gemstone,
                long shadowQuantity,
                long liveQuantity,
                long observedAtMillis) {
            return new Sample(
                    context,
                    category,
                    Domain.GEMSTONE_ROUGH_EQUIVALENT,
                    ComparisonUnit.ROUGH_EQUIVALENT,
                    null,
                    gemstone,
                    null,
                    shadowQuantity,
                    liveQuantity,
                    observedAtMillis);
        }

        static Sample gemstoneBlocks(
                Context context,
                MiningSessionCategory category,
                GemstoneType gemstone,
                long shadowQuantity,
                long liveQuantity,
                long observedAtMillis) {
            return new Sample(
                    context,
                    category,
                    Domain.GEMSTONE_BLOCKS,
                    ComparisonUnit.BLOCK_COUNT,
                    null,
                    gemstone,
                    null,
                    shadowQuantity,
                    liveQuantity,
                    observedAtMillis);
        }

        static Sample materialNormalized(
                Context context,
                MiningSessionCategory category,
                TrackedMaterial material,
                long shadowQuantity,
                long liveQuantity,
                long observedAtMillis) {
            return new Sample(
                    context,
                    category,
                    Domain.MATERIAL_NORMALIZED,
                    ComparisonUnit.RAW_EQUIVALENT,
                    material,
                    null,
                    null,
                    shadowQuantity,
                    liveQuantity,
                    observedAtMillis);
        }

        static Sample materialBlocks(
                Context context,
                MiningSessionCategory category,
                TrackedMaterial material,
                long shadowQuantity,
                long liveQuantity,
                long observedAtMillis) {
            return new Sample(
                    context,
                    category,
                    Domain.MATERIAL_BLOCKS,
                    ComparisonUnit.BLOCK_COUNT,
                    material,
                    null,
                    null,
                    shadowQuantity,
                    liveQuantity,
                    observedAtMillis);
        }
    }

    record MismatchDetail(
            long sessionId,
            long sessionEpoch,
            long selectionEpoch,
            TrackerSelection selection,
            Domain domain,
            ComparisonUnit comparisonUnit,
            String resourceId,
            long liveQuantity,
            long shadowQuantity,
            long observedAtMillis) {
        MismatchDetail {
            if (sessionId <= 0L
                    || sessionEpoch < 0L
                    || selectionEpoch < 0L
                    || liveQuantity < 0L
                    || shadowQuantity < 0L
                    || observedAtMillis < 0L) {
                throw new IllegalArgumentException(
                        "Mismatch detail numeric fields are invalid");
            }
            if (selection == null
                    || domain == null
                    || comparisonUnit == null
                    || resourceId == null
                    || resourceId.isBlank()) {
                throw new IllegalArgumentException(
                        "Mismatch detail fields cannot be null or blank");
            }
        }
    }

    record ComparisonResult(
            Status status,
            boolean compared,
            Domain domain,
            ComparisonUnit comparisonUnit,
            String resourceId,
            long liveQuantity,
            long shadowQuantity,
            long mismatchCount,
            MismatchDetail mismatchDetail) {
        ComparisonResult {
            if (status == null
                    || domain == null
                    || comparisonUnit == null
                    || resourceId == null
                    || resourceId.isBlank()) {
                throw new IllegalArgumentException(
                        "Parity result fields cannot be null or blank");
            }
            if (liveQuantity < 0L
                    || shadowQuantity < 0L
                    || mismatchCount < 0L) {
                throw new IllegalArgumentException(
                        "Parity result quantities cannot be negative");
            }
            if (status == Status.MISMATCH
                    && mismatchDetail == null) {
                throw new IllegalArgumentException(
                        "Mismatch result requires immutable detail");
            }
        }
    }

    record Snapshot(
            Status status,
            long mismatchCount,
            MismatchDetail lastMismatch) {
        Snapshot {
            if (status == null || mismatchCount < 0L) {
                throw new IllegalArgumentException(
                        "Parity snapshot fields are invalid");
            }
            if (status == Status.MISMATCH && lastMismatch == null) {
                throw new IllegalArgumentException(
                        "Mismatch snapshot requires immutable detail");
            }
            if (mismatchCount == 0L && lastMismatch != null) {
                throw new IllegalArgumentException(
                        "Zero-mismatch snapshot cannot carry mismatch detail");
            }
        }
    }
}
