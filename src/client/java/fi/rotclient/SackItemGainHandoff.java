package fi.rotclient;

import java.util.Optional;

/**
 * Maps a parsed Sack change through domain classification into Current Session
 * ingest decisions. Diagnostic tracing only records the outcome — it is not the
 * terminal consumer of the parsed gain.
 */
final class SackItemGainHandoff {
    private SackItemGainHandoff() {
    }

    record Decision(
            String classification,
            String result,
            String reason,
            boolean ingestOthers,
            boolean targetFamilyProtected,
            boolean ingestUnattributed) {
        Decision(
                String classification,
                String result,
                String reason,
                boolean ingestOthers,
                boolean targetFamilyProtected) {
            this(
                    classification,
                    result,
                    reason,
                    ingestOthers,
                    targetFamilyProtected,
                    false);
        }
    }

    /**
     * True when the resolved sack resource belongs to the currently selected
     * tracker family (raw/enchanted forms included). Those gains must not enter
     * OTHERS and must not duplicate authoritative target accounting.
     */
    static boolean belongsToCurrentTargetFamily(
            Optional<MiningResourceCatalog.ResourceDefinition> definition,
            TrackerSelection selection) {
        if (definition == null
                || definition.isEmpty()
                || selection == null) {
            return false;
        }
        MiningResourceCatalog.ResourceDefinition resource = definition.get();
        if (resource.material() != null) {
            return selection.isMaterial()
                    && selection.materialTarget() != null
                    && selection.materialTarget().includes(resource.material());
        }
        if (resource.gemstone() != null) {
            return selection.isGemstone()
                    && selection.gemstone() == resource.gemstone();
        }
        return false;
    }

    static Decision fromObservation(
            MiningSessionShadowObserver.ObservationResult result) {
        if (result == null || result.reason() == null) {
            return new Decision(
                    "REJECTED",
                    "REJECTED",
                    TrackingRuntimeTrace.Reason.REJECTED_UNKNOWN_SOURCE.name(),
                    false,
                    false);
        }
        if (result.appended()) {
            return new Decision(
                    "OTHER_MINED",
                    "ACCEPTED_OTHER_MINING",
                    TrackingRuntimeTrace.Reason.ACCEPTED_OTHER_MINING.name(),
                    true,
                    false);
        }
        if (result.reason()
                == MiningSessionClassification.ReasonCode
                .TARGET_EXCLUDED_FROM_OTHERS) {
            return new Decision(
                    "TARGET",
                    "IGNORED",
                    TrackingRuntimeTrace.Reason.REJECTED_TARGET_AUTHORITY.name(),
                    false,
                    true);
        }
        if (result.reason()
                == MiningSessionClassification.ReasonCode
                .MISSING_MINING_CORRELATION
                || result.reason()
                == MiningSessionClassification.ReasonCode
                .INSUFFICIENT_EVIDENCE
                || result.reason()
                == MiningSessionClassification.ReasonCode
                .INACTIVE_MINING_CONTEXT) {
            return new Decision(
                    "UNATTRIBUTED",
                    "UNATTRIBUTED",
                    TrackingRuntimeTrace.Reason
                            .UNATTRIBUTED_NO_SOURCE_EVIDENCE.name(),
                    false,
                    false);
        }
        if (result.reason()
                == MiningSessionClassification.ReasonCode.DUPLICATE_DELIVERY) {
            return new Decision(
                    "REJECTED",
                    "REJECTED",
                    TrackingRuntimeTrace.Reason.REJECTED_DUPLICATE.name(),
                    false,
                    false);
        }
        if (result.reason()
                == MiningSessionClassification.ReasonCode.UNKNOWN_RESOURCE) {
            // Identity retained — catalog absence is not an allow-list reject.
            // Without mining-family attribution, credit as UNATTRIBUTED only.
            return new Decision(
                    "UNATTRIBUTED",
                    "UNATTRIBUTED",
                    TrackingRuntimeTrace.Reason
                            .UNATTRIBUTED_NO_SOURCE_EVIDENCE.name(),
                    false,
                    false,
                    true);
        }
        return new Decision(
                "REJECTED",
                "REJECTED",
                result.reason().name(),
                false,
                false);
    }
}
