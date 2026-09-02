package fi.rotclient;

/**
 * Context gate for ambiguous physical block states.
 *
 * <p>A missing mechanics row does not turn this registry into an allow-list;
 * only resources explicitly marked as context-dependent are gated.</p>
 */
final class MiningBlockEvidencePolicy {
    private MiningBlockEvidencePolicy() {
    }

    static boolean allows(String canonicalResourceId, SkyBlockLocation location) {
        return SkyBlockMiningResourceRegistry.bundled()
                .lookup(canonicalResourceId)
                .map(resource -> resource.allowsArea(
                        location == null
                                ? SkyBlockArea.UNKNOWN_SKYBLOCK_AREA
                                : location.parentArea()))
                .orElse(true);
    }

    /**
     * A user-selected live target is explicit intent and must keep working
     * while the scoreboard location is missing, stale, or newly introduced by
     * Hypixel. Ambiguous background candidates for OTHERS still use the
     * reviewed location gate to avoid treating decorative vanilla blocks as
     * mining resources.
     */
    static boolean allows(
            String canonicalResourceId,
            SkyBlockLocation location,
            boolean explicitlySelectedTarget) {
        return explicitlySelectedTarget
                || allows(canonicalResourceId, location);
    }
}
