package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Connects direct gemstone breaks, Pristine messages and Sack observations to
 * the diagnostic correlation and shadow-accounting pipeline.
 *
 * This class does not update gemstone ledgers or tracker state.
 */
final class GemstoneGainDetector {
    private static final long SACK_DUPLICATE_WINDOW_MILLIS =
            2_500L;

    private final GemstoneMiningCorrelationGate correlationGate =
            new GemstoneMiningCorrelationGate();

    private final GemstoneDirectBreakTracker directBreakTracker =
            new GemstoneDirectBreakTracker();

    private final GemstoneMiningBatchTracker batchTracker =
            new GemstoneMiningBatchTracker();

    private final Listener listener;

    private Object observedLevel;

    GemstoneGainDetector() {
        this(
                Listener.NOOP);
    }

    GemstoneGainDetector(
            Listener listener) {
        this.listener =
                listener == null
                        ? Listener.NOOP
                        : listener;
    }

    private String lastSackFingerprint =
            "";

    private long lastSackEpochMillis;

    void tick(
            Minecraft client) {
        tick(
                client,
                false);
    }

    void tick(
            Minecraft client,
            boolean liveTrackingActive) {
        boolean active =
                liveTrackingActive
                        || DiagnosticRecorder.isRecording();

        if (!active
                || client == null
                || client.level == null) {
            reset();
            return;
        }

        if (observedLevel != client.level) {
            resetSignals();
            observedLevel =
                    client.level;
        }

        long now =
                System.currentTimeMillis();

        directBreakTracker.clearExpired(
                now);

        batchTracker.clearExpired(
                now);

        observeAttackTarget(
                client,
                now);
    }

    void onInventoryPacket(
            Minecraft client,
            String packetType) {
        // Inventory changes remain diagnostic-only until exact transfer
        // deduplication is implemented.
    }

    void onServerBlockUpdate(
            Minecraft client,
            BlockPos position,
            BlockState newState) {
        onServerBlockUpdate(
                client,
                position,
                newState,
                false);
    }

    void onServerBlockUpdate(
            Minecraft client,
            BlockPos position,
            BlockState newState,
            boolean liveTrackingActive) {
        boolean active =
                liveTrackingActive
                        || DiagnosticRecorder.isRecording();

        if (!active
                || client == null
                || client.level == null
                || position == null
                || newState == null) {
            return;
        }

        long now =
                System.currentTimeMillis();

        BlockState previousState =
                client.level.getBlockState(
                        position);

        GemstoneType previousGemstone =
                GemstoneBlockClassifier.fromBlockState(
                        previousState);

        GemstoneDirectBreakTracker.Acceptance acceptance =
                acceptBlockRemoval(
                        trackerPosition(position),
                        previousGemstone,
                        newState.isAir(),
                        now,
                        liveTrackingActive);

        if (previousGemstone == null
                || !newState.isAir()) {
            return;
        }

        boolean accepted =
                acceptance != null;

        DiagnosticRecorder.record(
                "GEMSTONE_BLOCK_CHANGE",
                "gemstone="
                        + previousGemstone.id()
                        + " directTarget="
                        + (accepted
                        && acceptance.evidence()
                        == GemstoneDirectBreakTracker.Evidence.DIRECT_TARGET)
                        + " evidence="
                        + (accepted
                        ? acceptance.evidence().name()
                        : "REJECTED"));

    }

    void inspectMessage(
            Component message) {
        inspectMessage(
                message,
                false);
    }

    void inspectMessage(
            Component message,
            boolean liveTrackingActive) {
        boolean active =
                liveTrackingActive
                        || DiagnosticRecorder.isRecording();

        if (!active
                || message == null) {
            return;
        }

        long now =
                System.currentTimeMillis();

        String plainMessage =
                message.getString()
                        .trim();

        PristineMessageParser.Reward reward =
                inspectPlainMessage(
                        plainMessage,
                        now);

        if (reward != null) {
            DiagnosticRecorder.record(
                    "GEMSTONE_PRISTINE_OBSERVED",
                    "gemstone="
                            + reward.gemstone().id()
                            + " tier=FLAWED"
                            + " amount="
                            + reward.flawedAmount());

            DiagnosticRecorder.record(
                    "GEMSTONE_WOULD_CREDIT",
                    "gemstone="
                            + reward.gemstone().id()
                            + " tier=FLAWED"
                            + " amount="
                            + reward.flawedAmount()
                            + " source=PRISTINE");

            if (liveTrackingActive) {
                listener.onGain(
                        reward.gemstone(),
                        GemstoneTier.FLAWED,
                        reward.flawedAmount(),
                        "PRISTINE");
            }

            return;
        }

        if (!isSackMessage(
                plainMessage)) {
            return;
        }

        List<String> sackChangeTexts =
                sackChangeHoverTexts(
                        message);

        if (sackChangeTexts.isEmpty()) {
            DiagnosticRecorder.record(
                    "GEMSTONE_SACK_CORRELATION",
                    "result=no-sack-change-hover");

            return;
        }

        String fingerprint =
                plainMessage
                        + "\n"
                        + String.join(
                                "\n---\n",
                                sackChangeTexts);

        if (isDuplicateSackMessage(
                fingerprint,
                now)) {
            return;
        }

        List<SackObservation> observations =
                new ArrayList<>();

        for (String sackChangeText :
                sackChangeTexts) {
            observations.addAll(
                    inspectSackChangeText(
                            sackChangeText,
                            now));
        }

        if (observations.isEmpty()) {
            DiagnosticRecorder.record(
                    "GEMSTONE_SACK_CORRELATION",
                    "result=no-positive-gemstone-entry");

            return;
        }

        for (SackObservation observation :
                observations) {
            logSackObservation(
                    observation);

            if (liveTrackingActive
                    && observation.batchDecision()
                    .shouldCredit()) {
                listener.onGain(
                        observation.gemstone(),
                        observation.tier(),
                        observation.batchDecision()
                                .creditAmount(),
                        "SACK");
            }
        }
    }

    PristineMessageParser.Reward inspectPlainMessage(
            String message,
            long epochMillis) {
        requireTimestamp(
                epochMillis);

        PristineMessageParser.Reward reward =
                PristineMessageParser.parse(
                        message);

        if (reward == null) {
            return null;
        }

        correlationGate.recordPristine(
                reward.gemstone(),
                reward.flawedAmount(),
                epochMillis);

        batchTracker.recordPristine(
                reward.gemstone(),
                reward.flawedAmount(),
                epochMillis);

        return reward;
    }

    List<SackObservation> inspectSackAddedText(
            String addedText,
            long epochMillis) {
        return inspectSackChangeText(
                addedText,
                epochMillis);
    }

    List<SackObservation> inspectSackChangeText(
            String changeText,
            long epochMillis) {
        requireTimestamp(
                epochMillis);

        if (changeText == null
                || changeText.isBlank()) {
            return List.of();
        }

        List<SackChangeParser.Change> parsedChanges =
                SackChangeParser.parse(
                        changeText);

        if (parsedChanges.isEmpty()) {
            return List.of();
        }

        Map<SackKey, Long> aggregated =
                new LinkedHashMap<>();

        for (SackChangeParser.Change change :
                parsedChanges) {
            GemstoneDiagnosticObserver.GemstoneItem item =
                    GemstoneDiagnosticObserver.matchItemName(
                            change.itemName());

            if (item == null) {
                continue;
            }

            DiagnosticRecorder.record(
                    "GEMSTONE_SACK_CHANGE",
                    "gemstone="
                            + item.gemstone().id()
                            + " tier="
                            + item.tier().id()
                            + " delta="
                            + change.delta()
                            + " sacks="
                            + String.join(
                            ",",
                            change.sacks()));

            if (change.delta() <= 0L) {
                DiagnosticRecorder.record(
                        "GEMSTONE_SACK_CHANGE_REJECTED",
                        "gemstone="
                                + item.gemstone().id()
                                + " tier="
                                + item.tier().id()
                                + " delta="
                                + change.delta()
                                + " reason=non-positive");

                continue;
            }

            boolean gemstoneSack =
                    change.fromSack(
                            "Gemstone Sack")
                            || change.fromSack(
                            "Gemstones Sack");

            if (!gemstoneSack) {
                DiagnosticRecorder.record(
                        "GEMSTONE_SACK_CHANGE_REJECTED",
                        "gemstone="
                                + item.gemstone().id()
                                + " tier="
                                + item.tier().id()
                                + " delta="
                                + change.delta()
                                + " reason=unsupported-sack"
                                + " sacks="
                                + String.join(
                                ",",
                                change.sacks()));

                continue;
            }

            SackKey key =
                    new SackKey(
                            item.gemstone(),
                            item.tier());

            long previous =
                    aggregated.getOrDefault(
                            key,
                            0L);

            aggregated.put(
                    key,
                    Math.addExact(
                            previous,
                            change.delta()));
        }

        List<SackObservation> observations =
                new ArrayList<>(
                        aggregated.size());

        for (Map.Entry<SackKey, Long> aggregate :
                aggregated.entrySet()) {
            SackKey key =
                    aggregate.getKey();

            long amount =
                    aggregate.getValue();

            GemstoneMiningCorrelationGate.Correlation
                    correlation =
                    correlationGate.snapshot(
                            key.gemstone(),
                            epochMillis);

            GemstoneSackCorrelationEvaluator.Decision
                    correlationDecision =
                    GemstoneSackCorrelationEvaluator.evaluate(
                            key.tier(),
                            correlation);

            GemstoneMiningBatchTracker.SackDecision
                    batchDecision =
                    batchTracker.evaluateSack(
                            key.gemstone(),
                            key.tier(),
                            amount,
                            epochMillis);

            observations.add(
                    new SackObservation(
                            key.gemstone(),
                            key.tier(),
                            amount,
                            correlationDecision,
                            correlation,
                            batchDecision));
        }

        return List.copyOf(
                observations);
    }

    void recordDirectBreakSignal(
            GemstoneType gemstone,
            long epochMillis) {
        requireTimestamp(
                epochMillis);

        if (gemstone == null) {
            throw new IllegalArgumentException(
                    "Gemstone type cannot be null");
        }

        correlationGate.recordDirectBreak(
                gemstone,
                epochMillis);

        batchTracker.recordDirectBreak(
                gemstone,
                epochMillis);
    }

    GemstoneMiningCorrelationGate.Correlation correlationSnapshot(
            GemstoneType gemstone,
            long epochMillis) {
        return correlationGate.snapshot(
                gemstone,
                epochMillis);
    }

    GemstoneMiningBatchTracker.Snapshot batchSnapshot(
            GemstoneType gemstone,
            long epochMillis) {
        return batchTracker.snapshot(
                gemstone,
                epochMillis);
    }

    void reset() {
        resetSignals();
        observedLevel =
                null;
    }

    private void resetSignals() {
        correlationGate.reset();
        directBreakTracker.clear();
        batchTracker.reset();
        clearSackDuplicateState();
    }

    private void logSackObservation(
            SackObservation observation) {
        GemstoneMiningCorrelationGate.Correlation correlation =
                observation.correlation();

        GemstoneSackCorrelationEvaluator.Decision
                correlationDecision =
                observation.decision();

        GemstoneMiningBatchTracker.SackDecision
                batchDecision =
                observation.batchDecision();

        DiagnosticRecorder.record(
                "GEMSTONE_SACK_CORRELATION",
                "gemstone="
                        + observation.gemstone().id()
                        + " tier="
                        + observation.tier().id()
                        + " amount="
                        + observation.amount()
                        + " legacyCandidate="
                        + correlationDecision.acceptedCandidate()
                        + " finalCandidate="
                        + batchDecision.shouldCredit()
                        + " legacyDecision="
                        + correlationDecision.id()
                        + " batchOutcome="
                        + batchDecision.outcome().id()
                        + " directBreaks="
                        + correlation.directBreaks()
                        + " latestBreakAgeMillis="
                        + correlation
                        .latestDirectBreakAgeMillis()
                        + " pristineObserved="
                        + correlation
                        .pristineObserved()
                        + " pristineAmount="
                        + correlation
                        .pristineAmount());

        if (batchDecision.shouldCredit()) {
            DiagnosticRecorder.record(
                    "GEMSTONE_WOULD_CREDIT",
                    "gemstone="
                            + observation.gemstone().id()
                            + " tier="
                            + observation.tier().id()
                            + " amount="
                            + batchDecision.creditAmount()
                            + " source=SACK");

            DiagnosticRecorder.record(
                    "GEMSTONE_SIGNAL_CONSUMED",
                    "gemstone="
                            + observation.gemstone().id()
                            + " directBreaks="
                            + batchDecision
                            .consumedDirectBreaks());

            return;
        }

        if (batchDecision.matchedPristineAmount() > 0L) {
            DiagnosticRecorder.record(
                    "GEMSTONE_SACK_CONFIRMATION",
                    "gemstone="
                            + observation.gemstone().id()
                            + " tier="
                            + observation.tier().id()
                            + " sackAmount="
                            + observation.amount()
                            + " matchedPristineAmount="
                            + batchDecision
                            .matchedPristineAmount()
                            + " remainingPristineAmount="
                            + batchDecision
                            .remainingPristineAmount()
                            + " outcome="
                            + batchDecision.outcome().id());
        }
    }

    private void observeAttackTarget(
            Minecraft client,
            long epochMillis) {
        if (client == null
                || client.level == null
                || client.player == null
                || !client.options.keyAttack.isDown()
                || !(client.hitResult
                instanceof BlockHitResult blockHit)) {
            return;
        }

        BlockPos position =
                blockHit.getBlockPos();

        BlockState state =
                client.level.getBlockState(
                        position);

        GemstoneType gemstone =
                GemstoneBlockClassifier.fromBlockState(
                        state);

        if (gemstone == null) {
            return;
        }

        recordAttackTarget(
                trackerPosition(position),
                gemstone,
                epochMillis);
    }

    void recordAttackTarget(
            GemstoneDirectBreakTracker.Position position,
            GemstoneType gemstone,
            long epochMillis) {
        directBreakTracker.recordTarget(
                position,
                gemstone,
                epochMillis);
    }

    GemstoneDirectBreakTracker.Acceptance acceptBlockRemoval(
            GemstoneDirectBreakTracker.Position position,
            GemstoneType previousGemstone,
            boolean becameAir,
            long epochMillis,
            boolean liveTrackingActive) {
        GemstoneDirectBreakTracker.Acceptance acceptance =
                directBreakTracker.acceptRemoval(
                        position,
                        previousGemstone,
                        becameAir,
                        epochMillis);

        if (acceptance == null) {
            return null;
        }

        recordDirectBreakSignal(
                acceptance.gemstone(),
                epochMillis);

        GemstoneMiningBatchTracker.Snapshot snapshot =
                batchTracker.snapshot(
                        acceptance.gemstone(),
                        epochMillis);

        DiagnosticRecorder.record(
                "GEMSTONE_DIRECT_BREAK",
                "gemstone="
                        + acceptance.gemstone().id()
                        + " pendingBreaks="
                        + snapshot.directBreaks()
                        + " evidence="
                        + acceptance.evidence().name());

        if (liveTrackingActive) {
            listener.onBlock(
                    acceptance.gemstone(),
                    epochMillis);
        }

        return acceptance;
    }

    private static GemstoneDirectBreakTracker.Position trackerPosition(
            BlockPos position) {
        return new GemstoneDirectBreakTracker.Position(
                position.getX(),
                position.getY(),
                position.getZ());
    }

    private boolean isDuplicateSackMessage(
            String fingerprint,
            long epochMillis) {
        boolean duplicate =
                fingerprint.equals(
                        lastSackFingerprint)
                        && epochMillis
                        - lastSackEpochMillis
                        <= SACK_DUPLICATE_WINDOW_MILLIS;

        lastSackFingerprint =
                fingerprint;

        lastSackEpochMillis =
                epochMillis;

        return duplicate;
    }

    private void clearSackDuplicateState() {
        lastSackFingerprint =
                "";

        lastSackEpochMillis =
                0L;
    }

    private static boolean isSackMessage(
            String plainMessage) {
        return plainMessage != null
                && plainMessage.regionMatches(
                true,
                0,
                "[Sacks]",
                0,
                7);
    }

    private static List<String> sackChangeHoverTexts(
            Component component) {
        List<String> texts =
                new ArrayList<>();

        collectSackChangeHoverTexts(
                component,
                texts);

        return List.copyOf(
                texts);
    }

    private static void collectSackChangeHoverTexts(
            Component component,
            List<String> texts) {
        if (component == null) {
            return;
        }

        HoverEvent hover =
                component.getStyle()
                        .getHoverEvent();

        if (hover instanceof
                HoverEvent.ShowText showText) {
            String text =
                    showText.value()
                            .getString();

            String trimmed =
                    text.trim();

            boolean sackChange =
                    trimmed.regionMatches(
                            true,
                            0,
                            "Added",
                            0,
                            5)
                            || trimmed.regionMatches(
                            true,
                            0,
                            "Removed",
                            0,
                            7);

            if (sackChange
                    && !texts.contains(
                    text)) {
                texts.add(
                        text);
            }
        }

        for (Component sibling :
                component.getSiblings()) {
            collectSackChangeHoverTexts(
                    sibling,
                    texts);
        }
    }

    private static void requireTimestamp(
            long epochMillis) {
        if (epochMillis < 0L) {
            throw new IllegalArgumentException(
                    "Timestamp cannot be negative");
        }
    }

    interface Listener {
        Listener NOOP =
                new Listener() {
                    @Override
                    public void onBlock(
                            GemstoneType gemstone,
                            long epochMillis) {
                    }

                    @Override
                    public void onGain(
                            GemstoneType gemstone,
                            GemstoneTier tier,
                            long amount,
                            String source) {
                    }
                };

        void onBlock(
                GemstoneType gemstone,
                long epochMillis);

        void onGain(
                GemstoneType gemstone,
                GemstoneTier tier,
                long amount,
                String source);
    }

    private record SackKey(
            GemstoneType gemstone,
            GemstoneTier tier) {
    }

    record SackObservation(
            GemstoneType gemstone,
            GemstoneTier tier,
            long amount,
            GemstoneSackCorrelationEvaluator.Decision decision,
            GemstoneMiningCorrelationGate.Correlation correlation,
            GemstoneMiningBatchTracker.SackDecision batchDecision) {
    }
}
