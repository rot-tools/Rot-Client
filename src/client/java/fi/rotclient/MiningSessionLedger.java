package fi.rotclient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Disconnected, ephemeral ledger for accepted shadow classifications. */
final class MiningSessionLedger {
    private final List<Entry> entries =
            new ArrayList<>();
    private final Map<DeliveryIdentity, DeliveryPayloadFingerprint>
            deliveries = new HashMap<>();
    private final Map<MiningSessionCategory,
            Map<MiningSessionResource, Long>> quantities =
            new EnumMap<>(MiningSessionCategory.class);
    private final Map<MiningSessionCategory, Long> itemTotals =
            new EnumMap<>(MiningSessionCategory.class);
    private final Map<MiningSessionResource, Long> currencyTotals =
            new HashMap<>();
    private final Map<MiningSessionCategory, BigDecimal> resolvedValues =
            new EnumMap<>(MiningSessionCategory.class);
    private BigDecimal resolvedTotal =
            BigDecimal.ZERO;

    boolean append(
            MiningSessionClassification classification,
            MiningSessionPriceResolution priceResolution) {
        if (classification == null) {
            throw new IllegalArgumentException(
                    "Classification cannot be null");
        }
        if (priceResolution == null) {
            throw new IllegalArgumentException(
                    "Price resolution cannot be null");
        }
        if (classification.outcome()
                != MiningSessionClassification.Outcome.WOULD_CREDIT) {
            return false;
        }

        MiningSessionObservation observation =
                classification.observation();
        MiningSessionCategory category =
                classification.category();
        MiningSessionResource resource =
                observation.resource();
        long amount = observation.quantity();

        if (amount <= 0L) {
            throw new IllegalArgumentException(
                    "Accepted quantity must be positive");
        }
        validateCategoryResource(category, resource);

        DeliveryIdentity deliveryIdentity =
                DeliveryIdentity.from(observation);
        DeliveryPayloadFingerprint payloadFingerprint =
                deliveryIdentity == null
                        ? null
                        : DeliveryPayloadFingerprint.from(
                                observation,
                                category);
        if (deliveryIdentity != null) {
            DeliveryPayloadFingerprint previousPayload =
                    deliveries.get(deliveryIdentity);
            if (previousPayload != null) {
                if (previousPayload.equals(payloadFingerprint)) {
                    return false;
                }
                throw new IllegalStateException(
                        "Conflicting "
                                + deliveryIdentity.type().label()
                                + " delivery identity");
            }
        }

        Map<MiningSessionResource, Long> categoryQuantities =
                quantities.get(category);
        long previousQuantity = categoryQuantities == null
                ? 0L
                : categoryQuantities.getOrDefault(resource, 0L);
        long nextQuantity = Math.addExact(
                previousQuantity,
                amount);

        Long nextItemTotal = null;
        Long nextCurrencyTotal = null;
        if (resource.kind()
                == MiningSessionResource.ResourceKind.ITEM) {
            nextItemTotal = Math.addExact(
                    itemTotals.getOrDefault(category, 0L),
                    amount);
        } else {
            nextCurrencyTotal = Math.addExact(
                    currencyTotals.getOrDefault(resource, 0L),
                    amount);
        }

        BigDecimal priceDelta = BigDecimal.ZERO;
        BigDecimal nextCategoryValue =
                resolvedValues.getOrDefault(
                        category,
                        BigDecimal.ZERO);
        BigDecimal nextResolvedTotal = resolvedTotal;

        if (category != MiningSessionCategory.CURRENCY
                && priceResolution.status()
                == MiningSessionPriceResolution.PriceStatus.RESOLVED) {
            priceDelta = priceResolution.estimatedTotal(amount)
                    .orElseThrow();
            nextCategoryValue = nextCategoryValue.add(priceDelta);
            nextResolvedTotal = resolvedTotal.add(priceDelta);
        }

        Entry entry = new Entry(
                observation,
                category,
                priceResolution,
                deliveryIdentity,
                payloadFingerprint,
                observation.observedAtMillis());

        Map<MiningSessionResource, Long> mutableCategoryQuantities =
                quantities.computeIfAbsent(
                        category,
                        ignored -> new HashMap<>());
        mutableCategoryQuantities.put(resource, nextQuantity);
        if (nextItemTotal != null) {
            itemTotals.put(category, nextItemTotal);
        }
        if (nextCurrencyTotal != null) {
            currencyTotals.put(resource, nextCurrencyTotal);
        }
        if (priceDelta.signum() != 0) {
            resolvedValues.put(category, nextCategoryValue);
            resolvedTotal = nextResolvedTotal;
        }
        entries.add(entry);
        if (deliveryIdentity != null) {
            deliveries.put(
                    deliveryIdentity,
                    payloadFingerprint);
        }
        return true;
    }

    BatchAppendResult appendAllAtomically(List<BatchEntry> batchEntries) {
        if (batchEntries == null) {
            throw new IllegalArgumentException(
                    "Batch entries cannot be null");
        }
        if (batchEntries.isEmpty()) {
            return BatchAppendResult.empty();
        }

        List<PreparedEntry> prepared = new ArrayList<>(batchEntries.size());
        Map<DeliveryIdentity, DeliveryPayloadFingerprint>
                batchIdentities = new HashMap<>();

        for (BatchEntry batchEntry : batchEntries) {
            if (batchEntry == null) {
                return BatchAppendResult.rejected(
                        BatchAppendOutcome.REJECTED_INVALID);
            }
            MiningSessionClassification classification =
                    batchEntry.classification();
            MiningSessionPriceResolution priceResolution =
                    batchEntry.priceResolution();
            if (classification == null || priceResolution == null) {
                return BatchAppendResult.rejected(
                        BatchAppendOutcome.REJECTED_INVALID);
            }
            if (classification.outcome()
                    != MiningSessionClassification.Outcome.WOULD_CREDIT) {
                return BatchAppendResult.rejected(
                        BatchAppendOutcome.REJECTED_INVALID);
            }

            MiningSessionObservation observation =
                    classification.observation();
            MiningSessionCategory category =
                    classification.category();
            MiningSessionResource resource =
                    observation.resource();
            long amount = observation.quantity();

            if (amount <= 0L) {
                return BatchAppendResult.rejected(
                        BatchAppendOutcome.REJECTED_INVALID);
            }
            validateCategoryResource(category, resource);

            DeliveryIdentity deliveryIdentity =
                    DeliveryIdentity.from(observation);
            DeliveryPayloadFingerprint payloadFingerprint =
                    deliveryIdentity == null
                            ? null
                            : DeliveryPayloadFingerprint.from(
                                    observation,
                                    category);
            if (deliveryIdentity != null) {
                DeliveryPayloadFingerprint duplicateInBatch =
                        batchIdentities.put(
                                deliveryIdentity,
                                payloadFingerprint);
                if (duplicateInBatch != null) {
                    if (duplicateInBatch.equals(payloadFingerprint)) {
                        return BatchAppendResult.rejected(
                                BatchAppendOutcome.REJECTED_DUPLICATE_IN_BATCH);
                    }
                    return BatchAppendResult.rejected(
                            BatchAppendOutcome.REJECTED_CONFLICT);
                }

                DeliveryPayloadFingerprint existingPayload =
                        deliveries.get(deliveryIdentity);
                if (existingPayload != null) {
                    if (existingPayload.equals(payloadFingerprint)) {
                        return BatchAppendResult.rejected(
                                BatchAppendOutcome.REJECTED_DUPLICATE_EXISTING);
                    }
                    return BatchAppendResult.rejected(
                            BatchAppendOutcome.REJECTED_CONFLICT);
                }
            }

            prepared.add(new PreparedEntry(
                    classification,
                    priceResolution,
                    deliveryIdentity,
                    payloadFingerprint));
        }

        for (PreparedEntry entry : prepared) {
            applyPreparedEntry(entry);
        }
        return BatchAppendResult.applied(prepared.size());
    }

    private void applyPreparedEntry(PreparedEntry prepared) {
        MiningSessionClassification classification =
                prepared.classification();
        MiningSessionPriceResolution priceResolution =
                prepared.priceResolution();
        MiningSessionObservation observation =
                classification.observation();
        MiningSessionCategory category =
                classification.category();
        MiningSessionResource resource =
                observation.resource();
        long amount = observation.quantity();

        Map<MiningSessionResource, Long> categoryQuantities =
                quantities.get(category);
        long previousQuantity = categoryQuantities == null
                ? 0L
                : categoryQuantities.getOrDefault(resource, 0L);
        long nextQuantity = Math.addExact(
                previousQuantity,
                amount);

        Long nextItemTotal = null;
        Long nextCurrencyTotal = null;
        if (resource.kind()
                == MiningSessionResource.ResourceKind.ITEM) {
            nextItemTotal = Math.addExact(
                    itemTotals.getOrDefault(category, 0L),
                    amount);
        } else {
            nextCurrencyTotal = Math.addExact(
                    currencyTotals.getOrDefault(resource, 0L),
                    amount);
        }

        BigDecimal priceDelta = BigDecimal.ZERO;
        BigDecimal nextCategoryValue =
                resolvedValues.getOrDefault(
                        category,
                        BigDecimal.ZERO);
        BigDecimal nextResolvedTotal = resolvedTotal;

        if (category != MiningSessionCategory.CURRENCY
                && priceResolution.status()
                == MiningSessionPriceResolution.PriceStatus.RESOLVED) {
            priceDelta = priceResolution.estimatedTotal(amount)
                    .orElseThrow();
            nextCategoryValue = nextCategoryValue.add(priceDelta);
            nextResolvedTotal = resolvedTotal.add(priceDelta);
        }

        Entry entry = new Entry(
                observation,
                category,
                priceResolution,
                prepared.deliveryIdentity(),
                prepared.payloadFingerprint(),
                observation.observedAtMillis());

        Map<MiningSessionResource, Long> mutableCategoryQuantities =
                quantities.computeIfAbsent(
                        category,
                        ignored -> new HashMap<>());
        mutableCategoryQuantities.put(resource, nextQuantity);
        if (nextItemTotal != null) {
            itemTotals.put(category, nextItemTotal);
        }
        if (nextCurrencyTotal != null) {
            currencyTotals.put(resource, nextCurrencyTotal);
        }
        if (priceDelta.signum() != 0) {
            resolvedValues.put(category, nextCategoryValue);
            resolvedTotal = nextResolvedTotal;
        }
        entries.add(entry);
        if (prepared.deliveryIdentity() != null) {
            deliveries.put(
                    prepared.deliveryIdentity(),
                    prepared.payloadFingerprint());
        }
    }

    enum BatchAppendOutcome {
        APPLIED,
        EMPTY,
        REJECTED_INVALID,
        REJECTED_CONFLICT,
        REJECTED_DUPLICATE_IN_BATCH,
        REJECTED_DUPLICATE_EXISTING
    }

    record BatchEntry(
            MiningSessionClassification classification,
            MiningSessionPriceResolution priceResolution) {
        BatchEntry {
            if (classification == null || priceResolution == null) {
                throw new IllegalArgumentException(
                        "Batch entry fields cannot be null");
            }
        }
    }

    record BatchAppendResult(
            BatchAppendOutcome outcome,
            int appliedCount) {
        static BatchAppendResult empty() {
            return new BatchAppendResult(
                    BatchAppendOutcome.EMPTY,
                    0);
        }

        static BatchAppendResult applied(int appliedCount) {
            return new BatchAppendResult(
                    BatchAppendOutcome.APPLIED,
                    appliedCount);
        }

        static BatchAppendResult rejected(BatchAppendOutcome outcome) {
            return new BatchAppendResult(outcome, 0);
        }

        boolean applied() {
            return outcome == BatchAppendOutcome.APPLIED;
        }
    }

    private record PreparedEntry(
            MiningSessionClassification classification,
            MiningSessionPriceResolution priceResolution,
            DeliveryIdentity deliveryIdentity,
            DeliveryPayloadFingerprint payloadFingerprint) {
    }

    List<Entry> entries() {
        return List.copyOf(entries);
    }

    long quantity(
            MiningSessionCategory category,
            MiningSessionResource resource) {
        requireCategory(category);
        requireResource(resource);
        Map<MiningSessionResource, Long> categoryQuantities =
                quantities.get(category);
        return categoryQuantities == null
                ? 0L
                : categoryQuantities.getOrDefault(resource, 0L);
    }

    long totalItemQuantity(
            MiningSessionCategory category) {
        requireCategory(category);
        return itemTotals.getOrDefault(category, 0L);
    }

    long totalCurrencyQuantity(
            MiningSessionResource resource) {
        requireResource(resource);
        if (resource.kind()
                != MiningSessionResource.ResourceKind.CURRENCY) {
            throw new IllegalArgumentException(
                    "Currency total requires a currency resource");
        }
        return currencyTotals.getOrDefault(resource, 0L);
    }

    BigDecimal resolvedCoinValue(
            MiningSessionCategory category) {
        requireCategory(category);
        return resolvedValues.getOrDefault(
                category,
                BigDecimal.ZERO);
    }

    BigDecimal resolvedTotalCoinValue() {
        return resolvedTotal;
    }

    int entryCount() {
        return entries.size();
    }

    void clear() {
        entries.clear();
        deliveries.clear();
        quantities.clear();
        itemTotals.clear();
        currencyTotals.clear();
        resolvedValues.clear();
        resolvedTotal = BigDecimal.ZERO;
    }

    private static void validateCategoryResource(
            MiningSessionCategory category,
            MiningSessionResource resource) {
        requireCategory(category);
        requireResource(resource);
        boolean currency = resource.kind()
                == MiningSessionResource.ResourceKind.CURRENCY;
        if ((category == MiningSessionCategory.CURRENCY) != currency) {
            throw new IllegalArgumentException(
                    "Currency category and resource kind must match");
        }
    }

    private static void requireCategory(
            MiningSessionCategory category) {
        if (category == null) {
            throw new IllegalArgumentException(
                    "Category cannot be null");
        }
    }

    private static void requireResource(
            MiningSessionResource resource) {
        if (resource == null) {
            throw new IllegalArgumentException(
                    "Resource cannot be null");
        }
    }

    record Entry(
            MiningSessionObservation observation,
            MiningSessionCategory category,
            MiningSessionPriceResolution priceResolution,
            DeliveryIdentity deliveryIdentity,
            DeliveryPayloadFingerprint payloadFingerprint,
            long acceptedAtMillis) {
        Entry {
            if (observation == null
                    || category == null
                    || priceResolution == null) {
                throw new IllegalArgumentException(
                        "Ledger entry fields cannot be null");
            }
            if (observation.quantity() <= 0L) {
                throw new IllegalArgumentException(
                        "Ledger entry quantity must be positive");
            }
            if (acceptedAtMillis != observation.observedAtMillis()) {
                throw new IllegalArgumentException(
                        "Accepted timestamp must come from the observation");
            }
            validateCategoryResource(
                    category,
                    observation.resource());
            if ((deliveryIdentity == null)
                    != (payloadFingerprint == null)) {
                throw new IllegalArgumentException(
                        "Delivery identity and payload must both be present or absent");
            }
            DeliveryIdentity expectedIdentity =
                    DeliveryIdentity.from(observation);
            DeliveryPayloadFingerprint expectedPayload =
                    expectedIdentity == null
                            ? null
                            : DeliveryPayloadFingerprint.from(
                                    observation,
                                    category);
            if (!java.util.Objects.equals(
                    deliveryIdentity,
                    expectedIdentity)
                    || !java.util.Objects.equals(
                    payloadFingerprint,
                    expectedPayload)) {
                throw new IllegalArgumentException(
                        "Entry dedupe data does not match its observation");
            }
        }

        boolean hasDeliveryIdentity() {
            return deliveryIdentity != null;
        }
    }

    private enum DeliveryIdentityType {
        EVENT("event"),
        CORRELATION("correlation");

        private final String label;

        DeliveryIdentityType(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    private record DeliveryIdentity(
            DeliveryIdentityType type,
            String value) {
        DeliveryIdentity {
            if (type == null
                    || value == null
                    || value.isBlank()) {
                throw new IllegalArgumentException(
                        "Delivery identity must be complete");
            }
        }

        static DeliveryIdentity from(
                MiningSessionObservation observation) {
            if (observation.eventId() != null) {
                return new DeliveryIdentity(
                        DeliveryIdentityType.EVENT,
                        observation.eventId());
            }
            if (observation.correlationId() != null) {
                return new DeliveryIdentity(
                        DeliveryIdentityType.CORRELATION,
                        observation.correlationId());
            }
            return null;
        }
    }

    private record DeliveryPayloadFingerprint(
            MiningSessionCategory category,
            MiningSessionResource resource,
            long quantity,
            MiningSessionObservation.EvidenceType evidenceType,
            TrackerSelection selectedTracker,
            long selectionEpoch,
            String correlationId,
            String sourceName) {
        DeliveryPayloadFingerprint {
            if (category == null
                    || resource == null
                    || evidenceType == null
                    || selectedTracker == null
                    || quantity <= 0L
                    || selectionEpoch < 0L) {
                throw new IllegalArgumentException(
                        "Delivery payload fingerprint is invalid");
            }
        }

        static DeliveryPayloadFingerprint from(
                MiningSessionObservation observation,
                MiningSessionCategory category) {
            return new DeliveryPayloadFingerprint(
                    category,
                    observation.resource(),
                    observation.quantity(),
                    observation.evidenceType(),
                    observation.selectedTracker(),
                    observation.selectionEpoch(),
                    observation.correlationId(),
                    observation.sourceName());
        }
    }
}
