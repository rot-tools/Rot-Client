package fi.rotclient;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Canonical Sack → classification → Current Session OTHERS ingest chain.
 * Extracted so unit tests can exercise the real service wiring without
 * Minecraft packet objects. Diagnostic tracing is optional and must never
 * gate domain collection.
 */
final class SackItemGainPipeline {
    private SackItemGainPipeline() {
    }

    record Context(
            MiningSessionEngine engine,
            RotClientCurrentSession session,
            MiningResourceCatalog catalog,
            Supplier<TrackerSelection> selectionSupplier,
            Supplier<SkyBlockArea> areaSupplier,
            BooleanSupplier collectionActive,
            LongSupplier clockMillis) {
        Context {
            if (engine == null
                    || session == null
                    || catalog == null
                    || selectionSupplier == null
                    || areaSupplier == null
                    || collectionActive == null
                    || clockMillis == null) {
                throw new IllegalArgumentException(
                        "SackItemGainPipeline context cannot contain nulls");
            }
        }
    }

    record Outcome(
            SackItemGainHandoff.Decision decision,
            MiningSessionShadowObserver.ObservationResult observation,
            boolean creditedOthers,
            String creditedItemId) {
    }

    /**
     * Submit one already-parsed Sack change through the live domain chain.
     */
    static Outcome submit(
            Context context,
            SackChangeParser.Change change) {
        return submit(context, change, 0L);
    }

    /**
     * @param coveredBatchMillis Hypixel {@code Last Ns} covered duration for
     *        this Sack batch; 0 uses the default correlation window
     */
    static Outcome submit(
            Context context,
            SackChangeParser.Change change,
            long coveredBatchMillis) {
        return submit(context, change, coveredBatchMillis, "");
    }

    static Outcome submit(
            Context context,
            SackChangeParser.Change change,
            long coveredBatchMillis,
            String deliveryIdentity) {
        if (context == null || change == null || change.delta() == 0L) {
            return new Outcome(
                    new SackItemGainHandoff.Decision(
                            "REJECTED",
                            "REJECTED",
                            TrackingRuntimeTrace.Reason.REJECTED_ZERO_DELTA
                                    .name(),
                            false,
                            false),
                    null,
                    false,
                    "");
        }
        long now = context.clockMillis().getAsLong();
        SkyBlockItemIdentityResolver.Resolved resolved =
                SkyBlockItemIdentityResolver.fromTextToken(change.itemName());
        String itemDeliveryIdentity = itemScopedDeliveryIdentity(
                deliveryIdentity,
                resolved);
        // Observation-only trace: domain classification follows separately.
        TrackingRuntimeTrace.textSignal(
                TrackingRuntimeTrace.ObservationPath.SACK_MESSAGE,
                change.itemName(),
                change.delta(),
                resolved,
                "OBSERVED",
                TrackingRuntimeTrace.Reason.INFO.name(),
                "");

        if (change.delta() <= 0L) {
            return new Outcome(
                    new SackItemGainHandoff.Decision(
                            "REJECTED",
                            "REJECTED",
                            TrackingRuntimeTrace.Reason.REJECTED_ZERO_DELTA
                                    .name(),
                            false,
                            false),
                    null,
                    false,
                    "");
        }

        if (!context.collectionActive().getAsBoolean()) {
            TrackingRuntimeTrace.itemGainClassified(
                    TrackingRuntimeTrace.ObservationPath.SACK_MESSAGE,
                    resolved,
                    change.delta(),
                    "UNATTRIBUTED",
                    "REJECTED",
                    "REJECTED",
                    TrackingRuntimeTrace.Reason.REJECTED_COLLECTION_INACTIVE
                            .name());
            return new Outcome(
                    new SackItemGainHandoff.Decision(
                            "REJECTED",
                            "REJECTED",
                            TrackingRuntimeTrace.Reason
                                    .REJECTED_COLLECTION_INACTIVE.name(),
                            false,
                            false),
                    null,
                    false,
                    "");
        }

        Optional<MiningResourceCatalog.ResourceDefinition> definition =
                context.catalog().fromExactSackItem(change.itemName());
        TrackerSelection selection = context.selectionSupplier().get();
        if (SackItemGainHandoff.belongsToCurrentTargetFamily(
                definition, selection)) {
            MiningSessionShadowObserver.ObservationResult excluded =
                    context.engine().observeSackChange(
                            change,
                            now,
                            coveredBatchMillis,
                            itemDeliveryIdentity);
            SackItemGainHandoff.Decision decision =
                    SackItemGainHandoff.fromObservation(excluded);
            TrackingRuntimeTrace.itemGainClassified(
                    TrackingRuntimeTrace.ObservationPath.SACK_MESSAGE,
                    resolved,
                    change.delta(),
                    "MINING",
                    decision.classification(),
                    decision.result(),
                    decision.reason());
            return new Outcome(decision, excluded, false, "");
        }

        MiningSessionShadowObserver.ObservationResult result =
                context.engine().observeSackChange(
                        change,
                        now,
                        coveredBatchMillis,
                        itemDeliveryIdentity);
        SackItemGainHandoff.Decision decision =
                SackItemGainHandoff.fromObservation(result);
        String sourceClassification = decision.ingestOthers()
                ? "MINING"
                : (decision.classification().equals("UNATTRIBUTED")
                        ? "UNATTRIBUTED"
                        : "MINING");
        TrackingRuntimeTrace.itemGainClassified(
                TrackingRuntimeTrace.ObservationPath.SACK_MESSAGE,
                resolved,
                change.delta(),
                sourceClassification,
                decision.classification(),
                decision.result(),
                decision.reason());

        String itemId;
        if (definition.isPresent()) {
            itemId = CurrentSessionCanonicalIds.canonicalItemId(
                    definition.get().resource());
        } else {
            itemId = CurrentSessionCanonicalIds.canonicalItemId(
                    resolved.stableId(),
                    resolved.displayName());
        }

        if (decision.ingestOthers()) {
            context.session().creditItem(
                    itemId,
                    resolved.displayName(),
                    change.delta(),
                    SessionSourceType.MINING,
                    MiningClassification.OTHER,
                    context.areaSupplier().get(),
                    RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                    0.0,
                    now);
            TrackingRuntimeTrace.currentSessionIngest(
                    itemId,
                    change.delta(),
                    SessionSourceType.MINING.name(),
                    MiningClassification.OTHER.name(),
                    "ACCEPTED");
            return new Outcome(decision, result, true, itemId);
        }

        if (decision.ingestUnattributed()
                && !SkyBlockItemIdentityResolver.UNKNOWN.equals(itemId)
                && !itemId.isBlank()) {
            // Catalog absence must not erase a retained stable identity.
            context.session().creditItem(
                    itemId,
                    resolved.displayName(),
                    change.delta(),
                    SessionSourceType.UNATTRIBUTED,
                    null,
                    context.areaSupplier().get(),
                    RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                    0.0,
                    now);
            TrackingRuntimeTrace.currentSessionIngest(
                    itemId,
                    change.delta(),
                    SessionSourceType.UNATTRIBUTED.name(),
                    "",
                    "UNATTRIBUTED");
            return new Outcome(decision, result, false, itemId);
        }

        return new Outcome(decision, result, false, "");
    }

    /**
     * One Hypixel Sack component can contain several independent item rows.
     * Scope the component occurrence to the stable item identity so replaying
     * one row remains deduplicated without suppressing its sibling rows.
     */
    private static String itemScopedDeliveryIdentity(
            String deliveryIdentity,
            SkyBlockItemIdentityResolver.Resolved resolved) {
        if (deliveryIdentity == null || deliveryIdentity.isBlank()) {
            return "";
        }
        String stableId = resolved == null
                ? SkyBlockItemIdentityResolver.UNKNOWN
                : resolved.stableId();
        return deliveryIdentity + ":item:" + stableId;
    }
}
