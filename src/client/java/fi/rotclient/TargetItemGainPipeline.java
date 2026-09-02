package fi.rotclient;

/**
 * Terminal Current Session handoff for quantities already accepted by the
 * authoritative live target tracker. The transient analytics engine may
 * mirror these events, but this canonical row survives pause and restart.
 */
final class TargetItemGainPipeline {
    private TargetItemGainPipeline() {
    }

    static boolean creditMaterial(
            RotClientCurrentSession currentSession,
            TrackedMaterial material,
            long quantity,
            SkyBlockArea area,
            long observedAtMillis) {
        if (currentSession == null
                || material == null
                || quantity <= 0L
                || !currentSession.isActive()
                || !currentSession.acceptsTargetMaterial(material)) {
            return false;
        }
        String itemId = CurrentSessionCanonicalIds.canonicalItemId(
                material.id(), material.rawItemName());
        currentSession.creditItem(
                itemId,
                material.rawItemName(),
                quantity,
                SessionSourceType.MINING,
                MiningClassification.TARGET,
                area,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0,
                observedAtMillis);
        return true;
    }

    static boolean creditGemstone(
            RotClientCurrentSession currentSession,
            GemstoneType gemstone,
            GemstoneTier tier,
            long quantity,
            SkyBlockArea area,
            long observedAtMillis) {
        if (currentSession == null
                || gemstone == null
                || tier == null
                || quantity <= 0L
                || !currentSession.isActive()
                || !currentSession.acceptsTargetGemstone(gemstone)) {
            return false;
        }
        currentSession.creditItem(
                gemstone.bazaarId(tier),
                gemstone.itemName(tier),
                quantity,
                SessionSourceType.MINING,
                MiningClassification.TARGET,
                area,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0,
                observedAtMillis);
        return true;
    }
}
