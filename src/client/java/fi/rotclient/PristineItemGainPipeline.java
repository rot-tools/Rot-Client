package fi.rotclient;

/**
 * Single production handoff for an accepted non-target Pristine reward.
 * The transient classifier owns correlation/dedupe; Current Session receives
 * exactly one terminal MINING/OTHER mutation only after that acceptance.
 */
final class PristineItemGainPipeline {
    record Outcome(
            MiningSessionShadowObserver.ObservationResult observation,
            boolean creditedOthers,
            String creditedItemId) {
        Outcome {
            creditedItemId = creditedItemId == null ? "" : creditedItemId;
        }
    }

    private PristineItemGainPipeline() {
    }

    static Outcome submit(
            MiningSessionEngine engine,
            RotClientCurrentSession currentSession,
            PristineMessageParser.Reward reward,
            SkyBlockArea area,
            long observedAtMillis) {
        return submit(
                engine,
                currentSession,
                reward,
                area,
                observedAtMillis,
                "");
    }

    static Outcome submit(
            MiningSessionEngine engine,
            RotClientCurrentSession currentSession,
            PristineMessageParser.Reward reward,
            SkyBlockArea area,
            long observedAtMillis,
            String deliveryIdentity) {
        if (engine == null
                || currentSession == null
                || reward == null
                || !currentSession.isActive()) {
            return new Outcome(null, false, "");
        }

        MiningSessionShadowObserver.ObservationResult result =
                engine.observePristine(
                        reward,
                        observedAtMillis,
                        deliveryIdentity);
        if (result == null || !result.appended()) {
            return new Outcome(result, false, "");
        }

        String itemId = reward.gemstone().bazaarId(GemstoneTier.FLAWED);
        currentSession.creditItem(
                itemId,
                reward.gemstone().itemName(GemstoneTier.FLAWED),
                reward.flawedAmount(),
                SessionSourceType.MINING,
                MiningClassification.OTHER,
                area,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0,
                observedAtMillis);
        return new Outcome(result, true, itemId);
    }
}
