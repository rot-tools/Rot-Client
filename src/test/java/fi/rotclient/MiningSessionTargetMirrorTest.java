package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MiningSessionTargetMirrorTest {
    private static final long SELECTION_EPOCH = 7L;
    private static final long OBSERVED_AT = 1_000L;

    @Test
    void goldUnderGoldAppendsTargetMined() {
        Fixture fixture = fixture();
        MiningSessionResource gold = material(fixture, TrackedMaterial.GOLD);

        MiningSessionTargetMirror.QuantityResult result =
                fixture.mirror.mirrorQuantity(event(
                        gold,
                        32L,
                        MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                        TrackerSelection.GOLD,
                        "gold-1"));

        assertAppended(fixture, result, gold, 32L);
    }

    @Test
    void diamondUnderDiamondAppendsTargetMined() {
        Fixture fixture = fixture();
        MiningSessionResource diamond = material(
                fixture,
                TrackedMaterial.DIAMOND);

        MiningSessionTargetMirror.QuantityResult result =
                fixture.mirror.mirrorQuantity(event(
                        diamond,
                        18L,
                        MiningSessionObservation.EvidenceType.SACK_CHANGE,
                        TrackerSelection.DIAMOND,
                        "diamond-1"));

        assertAppended(fixture, result, diamond, 18L);
    }

    @Test
    void mithrilUnderCombinedSelectionAppendsTargetMined() {
        Fixture fixture = fixture();
        MiningSessionResource mithril = material(
                fixture,
                TrackedMaterial.MITHRIL);

        MiningSessionTargetMirror.QuantityResult result =
                fixture.mirror.mirrorQuantity(event(
                        mithril,
                        160L,
                        MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                        TrackerSelection.MITHRIL_TITANIUM,
                        "mithril-1"));

        assertAppended(fixture, result, mithril, 160L);
    }

    @Test
    void titaniumUnderCombinedSelectionAppendsTargetMined() {
        Fixture fixture = fixture();
        MiningSessionResource titanium = material(
                fixture,
                TrackedMaterial.TITANIUM);

        MiningSessionTargetMirror.QuantityResult result =
                fixture.mirror.mirrorQuantity(event(
                        titanium,
                        24L,
                        MiningSessionObservation.EvidenceType.SACK_CHANGE,
                        TrackerSelection.MITHRIL_TITANIUM,
                        "titanium-1"));

        assertAppended(fixture, result, titanium, 24L);
    }

    @Test
    void roughRubyUnderRubyAppendsTargetMined() {
        Fixture fixture = fixture();
        MiningSessionResource ruby = gemstone(
                fixture,
                GemstoneType.RUBY,
                GemstoneTier.ROUGH);

        MiningSessionTargetMirror.QuantityResult result =
                fixture.mirror.mirrorQuantity(event(
                        ruby,
                        800L,
                        MiningSessionObservation.EvidenceType.SACK_CHANGE,
                        TrackerSelection.RUBY,
                        "ruby-rough-1"));

        assertAppended(fixture, result, ruby, 800L);
    }

    @Test
    void flawedRubyFromPristineUnderRubyAppendsTargetMined() {
        Fixture fixture = fixture();
        MiningSessionResource ruby = gemstone(
                fixture,
                GemstoneType.RUBY,
                GemstoneTier.FLAWED);

        MiningSessionTargetMirror.QuantityResult result =
                fixture.mirror.mirrorQuantity(event(
                        ruby,
                        3L,
                        MiningSessionObservation.EvidenceType.PRISTINE_MESSAGE,
                        TrackerSelection.RUBY,
                        "ruby-flawed-1"));

        assertAppended(fixture, result, ruby, 3L);
    }

    @Test
    void topazUnderRubyIsVisibleOtherCategoryInvariant() {
        Fixture fixture = fixture();
        MiningSessionResource topaz = gemstone(
                fixture,
                GemstoneType.TOPAZ,
                GemstoneTier.ROUGH);

        MiningSessionTargetMirror.QuantityResult result =
                fixture.mirror.mirrorQuantity(event(
                        topaz,
                        80L,
                        MiningSessionObservation.EvidenceType.SACK_CHANGE,
                        TrackerSelection.RUBY,
                        "topaz-under-ruby"));

        assertInvariantOther(fixture, result);
    }

    @Test
    void rubyUnderTopazIsVisibleOtherCategoryInvariant() {
        Fixture fixture = fixture();
        MiningSessionResource ruby = gemstone(
                fixture,
                GemstoneType.RUBY,
                GemstoneTier.ROUGH);

        MiningSessionTargetMirror.QuantityResult result =
                fixture.mirror.mirrorQuantity(event(
                        ruby,
                        80L,
                        MiningSessionObservation.EvidenceType.SACK_CHANGE,
                        TrackerSelection.TOPAZ,
                        "ruby-under-topaz"));

        assertInvariantOther(fixture, result);
    }

    @Test
    void goldUnderGemstoneSelectionIsRejected() {
        Fixture fixture = fixture();
        MiningSessionResource gold = material(fixture, TrackedMaterial.GOLD);

        MiningSessionTargetMirror.QuantityResult result =
                fixture.mirror.mirrorQuantity(event(
                        gold,
                        12L,
                        MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                        TrackerSelection.RUBY,
                        "gold-under-ruby"));

        assertInvariantOther(fixture, result);
    }

    @Test
    void rubyUnderMaterialSelectionIsRejected() {
        Fixture fixture = fixture();
        MiningSessionResource ruby = gemstone(
                fixture,
                GemstoneType.RUBY,
                GemstoneTier.ROUGH);

        MiningSessionTargetMirror.QuantityResult result =
                fixture.mirror.mirrorQuantity(event(
                        ruby,
                        80L,
                        MiningSessionObservation.EvidenceType.SACK_CHANGE,
                        TrackerSelection.GOLD,
                        "ruby-under-gold"));

        assertInvariantOther(fixture, result);
    }

    @Test
    void gemstoneTierDoesNotAlterTargetIdentity() {
        Fixture fixture = fixture();
        MiningSessionResource rough = gemstone(
                fixture,
                GemstoneType.RUBY,
                GemstoneTier.ROUGH);
        MiningSessionResource flawed = gemstone(
                fixture,
                GemstoneType.RUBY,
                GemstoneTier.FLAWED);

        MiningSessionTargetMirror.QuantityResult roughResult =
                fixture.mirror.mirrorQuantity(event(
                        rough,
                        80L,
                        MiningSessionObservation.EvidenceType.SACK_CHANGE,
                        TrackerSelection.RUBY,
                        "ruby-tier-rough"));
        MiningSessionTargetMirror.QuantityResult flawedResult =
                fixture.mirror.mirrorQuantity(event(
                        flawed,
                        1L,
                        MiningSessionObservation.EvidenceType.PRISTINE_MESSAGE,
                        TrackerSelection.RUBY,
                        "ruby-tier-flawed"));

        assertTrue(roughResult.appended());
        assertTrue(flawedResult.appended());
        assertEquals(
                MiningSessionCategory.TARGET_MINED,
                roughResult.classification().category());
        assertEquals(
                MiningSessionCategory.TARGET_MINED,
                flawedResult.classification().category());
        assertEquals(2, fixture.ledger.entryCount());
    }

    @Test
    void genericResourceIdTextCannotEstablishTargetIdentity() {
        Fixture fixture = fixture();
        MiningSessionResource canonical = material(
                fixture,
                TrackedMaterial.GOLD);
        MiningSessionResource generic = MiningSessionResource.genericItem(
                canonical.resourceId(),
                canonical.displayName());

        MiningSessionTargetMirror.QuantityResult result =
                fixture.mirror.mirrorQuantity(event(
                        generic,
                        32L,
                        MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                        TrackerSelection.GOLD,
                        "generic-gold"));

        assertEquals(
                MiningSessionTargetMirror.QuantityStatus.REJECTED,
                result.status());
        assertEquals(
                MiningSessionClassification.ReasonCode.UNKNOWN_RESOURCE,
                result.classification().reasonCode());
        assertEquals(0, fixture.ledger.entryCount());
    }

    @Test
    void directBreakCannotCreateQuantityButCanValidateParityIdentity() {
        Fixture fixture = fixture();
        MiningSessionResource ruby = gemstone(
                fixture,
                GemstoneType.RUBY,
                GemstoneTier.ROUGH);

        MiningSessionTargetMirror.QuantityResult quantityResult =
                fixture.mirror.mirrorQuantity(event(
                        ruby,
                        1L,
                        MiningSessionObservation.EvidenceType.DIRECT_BREAK,
                        TrackerSelection.RUBY,
                        "ruby-direct-quantity"));
        MiningSessionTargetMirror.BlockResult blockResult =
                fixture.mirror.observeBlock(block(
                        ruby,
                        TrackerSelection.RUBY,
                        "ruby-direct-block"));

        assertEquals(
                MiningSessionTargetMirror.QuantityStatus.REJECTED,
                quantityResult.status());
        assertEquals(
                MiningSessionClassification.ReasonCode
                        .MISSING_EXACT_QUANTITY_EVIDENCE,
                quantityResult.classification().reasonCode());
        assertTrue(blockResult.acceptedForParity());
        assertEquals(0, fixture.ledger.entryCount());
    }

    @Test
    void zeroQuantityIsRejected() {
        assertNonPositiveRejected(0L, "zero");
    }

    @Test
    void negativeQuantityIsRejected() {
        assertNonPositiveRejected(-1L, "negative");
    }

    @Test
    void manualTransferIsRejected() {
        assertExplicitTransferRejected(
                MiningSessionObservation.EvidenceType.MANUAL_TRANSFER,
                "manual-transfer");
    }

    @Test
    void bazaarPurchaseIsRejected() {
        assertExplicitTransferRejected(
                MiningSessionObservation.EvidenceType.BAZAAR_PURCHASE,
                "bazaar-purchase");
    }

    @Test
    void chestRewardEvidenceIsRejected() {
        Fixture fixture = fixture();
        MiningSessionResource ruby = gemstone(
                fixture,
                GemstoneType.RUBY,
                GemstoneTier.FLAWED);

        MiningSessionTargetMirror.QuantityResult result =
                fixture.mirror.mirrorQuantity(event(
                        ruby,
                        2L,
                        MiningSessionObservation.EvidenceType
                                .CHEST_REWARD_MESSAGE,
                        TrackerSelection.RUBY,
                        "chest-reward"));

        assertEquals(
                MiningSessionTargetMirror.QuantityStatus.REJECTED,
                result.status());
        assertEquals(
                MiningSessionClassification.ReasonCode.MISSING_CHEST_CONTEXT,
                result.classification().reasonCode());
        assertEquals(0, fixture.ledger.entryCount());
    }

    @Test
    void currencyIsRejected() {
        Fixture fixture = fixture();
        MiningSessionResource powder = MiningSessionResource.currency(
                "MITHRIL_POWDER",
                "Mithril Powder");

        MiningSessionTargetMirror.QuantityResult result =
                fixture.mirror.mirrorQuantity(event(
                        powder,
                        250L,
                        MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                        TrackerSelection.MITHRIL_TITANIUM,
                        "currency"));

        assertEquals(
                MiningSessionTargetMirror.QuantityStatus.REJECTED,
                result.status());
        assertEquals(0, fixture.ledger.entryCount());
    }

    @Test
    void exactDuplicateIsIdempotent() {
        Fixture fixture = fixture();
        MiningSessionResource gold = material(fixture, TrackedMaterial.GOLD);
        MiningSessionTargetMirror.QuantityEvent event = event(
                gold,
                32L,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                TrackerSelection.GOLD,
                "duplicate-event");

        MiningSessionTargetMirror.QuantityResult first =
                fixture.mirror.mirrorQuantity(event);
        MiningSessionTargetMirror.QuantityResult duplicate =
                fixture.mirror.mirrorQuantity(event);

        assertTrue(first.appended());
        assertEquals(
                MiningSessionTargetMirror.QuantityStatus.DUPLICATE,
                duplicate.status());
        assertEquals(
                MiningSessionTargetMirror.QuantityReason.EXACT_DUPLICATE,
                duplicate.reason());
        assertEquals(1, fixture.ledger.entryCount());
        assertEquals(
                32L,
                fixture.ledger.quantity(
                        MiningSessionCategory.TARGET_MINED,
                        gold));
    }

    @Test
    void deliveryIdentityConflictIsVisibleAndAtomic() {
        Fixture fixture = fixture();
        MiningSessionResource gold = material(fixture, TrackedMaterial.GOLD);

        MiningSessionTargetMirror.QuantityResult first =
                fixture.mirror.mirrorQuantity(event(
                        gold,
                        32L,
                        MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                        TrackerSelection.GOLD,
                        "conflict-event"));
        MiningSessionTargetMirror.QuantityResult conflict =
                fixture.mirror.mirrorQuantity(event(
                        gold,
                        33L,
                        MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                        TrackerSelection.GOLD,
                        "conflict-event"));

        assertTrue(first.appended());
        assertEquals(
                MiningSessionTargetMirror.QuantityStatus.IDENTITY_CONFLICT,
                conflict.status());
        assertEquals(
                MiningSessionTargetMirror.QuantityReason.IDENTITY_CONFLICT,
                conflict.reason());
        assertEquals(1, fixture.ledger.entryCount());
        assertEquals(
                32L,
                fixture.ledger.quantity(
                        MiningSessionCategory.TARGET_MINED,
                        gold));
    }

    @Test
    void liveStateFixturesRemainUnchanged() {
        MaterialTrackerState materialState = new MaterialTrackerState();
        materialState.sessionBlocks = 17L;
        materialState.sessionActualRawEquivalent = 640L;
        GemstoneTrackerState gemstoneState = new GemstoneTrackerState();
        gemstoneState.recordBlock(100L);
        gemstoneState.recordGain(GemstoneTier.ROUGH, 80L);
        Fixture fixture = fixture();

        fixture.mirror.mirrorQuantity(event(
                material(fixture, TrackedMaterial.GOLD),
                32L,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                TrackerSelection.GOLD,
                "live-isolation-material"));
        fixture.mirror.mirrorQuantity(event(
                gemstone(fixture, GemstoneType.RUBY, GemstoneTier.ROUGH),
                80L,
                MiningSessionObservation.EvidenceType.SACK_CHANGE,
                TrackerSelection.RUBY,
                "live-isolation-gemstone"));

        assertEquals(17L, materialState.sessionBlocks);
        assertEquals(640L, materialState.sessionActualRawEquivalent);
        assertEquals(1L, gemstoneState.sessionBlocks);
        assertEquals(
                80L,
                gemstoneState.sessionLedger().quantity(GemstoneTier.ROUGH));
        assertEquals(0L, gemstoneState.totalLedger().quantity(GemstoneTier.FLAWED));
    }

    @Test
    void nullSelectionCannotFallBackToGold() {
        Fixture fixture = fixture();
        MiningSessionResource gold = material(fixture, TrackedMaterial.GOLD);

        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningSessionTargetMirror.QuantityEvent(
                        gold,
                        32L,
                        MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                        null,
                        SELECTION_EPOCH,
                        OBSERVED_AT,
                        "no-selection",
                        null,
                        "live"));
        assertEquals(0, fixture.ledger.entryCount());
    }

    @Test
    void targetMinedIsOnlyAppendableCategory() {
        Fixture fixture = fixture();
        MiningSessionResource gold = material(fixture, TrackedMaterial.GOLD);
        MiningSessionResource diamond = material(
                fixture,
                TrackedMaterial.DIAMOND);

        fixture.mirror.mirrorQuantity(event(
                gold,
                32L,
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                TrackerSelection.GOLD,
                "only-target"));
        MiningSessionTargetMirror.QuantityResult other =
                fixture.mirror.mirrorQuantity(event(
                        diamond,
                        32L,
                        MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                        TrackerSelection.GOLD,
                        "never-other"));

        assertEquals(
                MiningSessionTargetMirror.QuantityStatus.INVARIANT_REJECTED,
                other.status());
        assertTrue(fixture.ledger.entries().stream().allMatch(entry ->
                entry.category() == MiningSessionCategory.TARGET_MINED));
        assertEquals(
                0L,
                fixture.ledger.totalItemQuantity(
                        MiningSessionCategory.OTHER_MINED));
        assertEquals(
                0L,
                fixture.ledger.totalItemQuantity(
                        MiningSessionCategory.CHEST_LOOT));
        assertEquals(1, fixture.ledger.entryCount());
    }

    @Test
    void correlationIdentityIsAcceptedWhenEventIdIsAbsent() {
        Fixture fixture = fixture();
        MiningSessionResource gold = material(fixture, TrackedMaterial.GOLD);
        MiningSessionTargetMirror.QuantityEvent event =
                new MiningSessionTargetMirror.QuantityEvent(
                        gold,
                        32L,
                        MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                        TrackerSelection.GOLD,
                        SELECTION_EPOCH,
                        OBSERVED_AT,
                        null,
                        "accepted-correlation",
                        "live");

        MiningSessionTargetMirror.QuantityResult result =
                fixture.mirror.mirrorQuantity(event);

        assertAppended(fixture, result, gold, 32L);
        assertNull(fixture.ledger.entries().getFirst().observation().eventId());
        assertEquals(
                "accepted-correlation",
                fixture.ledger.entries().getFirst()
                        .observation().correlationId());
    }

    @Test
    void acceptedClassificationWithoutDeliveryIdentityDoesNotAppend() {
        Fixture fixture = fixture();
        MiningSessionResource gold = material(fixture, TrackedMaterial.GOLD);
        MiningSessionTargetMirror.QuantityEvent event =
                new MiningSessionTargetMirror.QuantityEvent(
                        gold,
                        32L,
                        MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                        TrackerSelection.GOLD,
                        SELECTION_EPOCH,
                        OBSERVED_AT,
                        " ",
                        null,
                        "live");

        MiningSessionTargetMirror.QuantityResult result =
                fixture.mirror.mirrorQuantity(event);

        assertEquals(
                MiningSessionTargetMirror.QuantityStatus.INVARIANT_REJECTED,
                result.status());
        assertEquals(
                MiningSessionTargetMirror.QuantityReason
                        .MISSING_DELIVERY_IDENTITY,
                result.reason());
        assertEquals(0, fixture.ledger.entryCount());
    }

    @Test
    void optionalSourceNameIsSanitizedAndBounded() {
        Fixture fixture = fixture();
        MiningSessionResource gold = material(fixture, TrackedMaterial.GOLD);
        String unbounded = " live\naccounting\t" + "x".repeat(100);
        MiningSessionTargetMirror.QuantityEvent event =
                new MiningSessionTargetMirror.QuantityEvent(
                        gold,
                        32L,
                        MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                        TrackerSelection.GOLD,
                        SELECTION_EPOCH,
                        OBSERVED_AT,
                        "bounded-source",
                        null,
                        unbounded);

        fixture.mirror.mirrorQuantity(event);

        String source = fixture.ledger.entries().getFirst()
                .observation().sourceName();
        assertTrue(source.length()
                <= MiningSessionTargetMirror.MAX_SOURCE_NAME_LENGTH);
        assertFalse(source.contains("\n"));
        assertFalse(source.contains("\t"));
    }

    @Test
    void nonTargetBlockIsRejectedForParityAndNeverAppends() {
        Fixture fixture = fixture();
        MiningSessionResource topaz = gemstone(
                fixture,
                GemstoneType.TOPAZ,
                GemstoneTier.ROUGH);

        MiningSessionTargetMirror.BlockResult result =
                fixture.mirror.observeBlock(block(
                        topaz,
                        TrackerSelection.RUBY,
                        "non-target-block"));

        assertEquals(
                MiningSessionTargetMirror.BlockStatus.NON_TARGET,
                result.status());
        assertFalse(result.acceptedForParity());
        assertEquals(0, fixture.ledger.entryCount());
    }

    @Test
    void unsupportedGemstoneTierCannotCreateTargetQuantity() {
        Fixture fixture = fixture();
        MiningSessionResource fineRuby = gemstone(
                fixture,
                GemstoneType.RUBY,
                GemstoneTier.FINE);

        MiningSessionTargetMirror.QuantityResult result =
                fixture.mirror.mirrorQuantity(event(
                        fineRuby,
                        1L,
                        MiningSessionObservation.EvidenceType.SACK_CHANGE,
                        TrackerSelection.RUBY,
                        "fine-ruby"));

        assertEquals(
                MiningSessionTargetMirror.QuantityStatus.INVARIANT_REJECTED,
                result.status());
        assertEquals(
                MiningSessionTargetMirror.QuantityReason
                        .UNSUPPORTED_TARGET_EVIDENCE,
                result.reason());
        assertEquals(0, fixture.ledger.entryCount());
    }

    @Test
    void negativeEventTimestampsAndEpochsAreRejected() {
        Fixture fixture = fixture();
        MiningSessionResource gold = material(fixture, TrackedMaterial.GOLD);

        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningSessionTargetMirror.QuantityEvent(
                        gold,
                        1L,
                        MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                        TrackerSelection.GOLD,
                        -1L,
                        OBSERVED_AT,
                        "negative-epoch",
                        null,
                        null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningSessionTargetMirror.BlockObservation(
                        gold,
                        TrackerSelection.GOLD,
                        SELECTION_EPOCH,
                        -1L,
                        "negative-time",
                        null,
                        null));
        assertEquals(0, fixture.ledger.entryCount());
    }

    private static void assertNonPositiveRejected(
            long quantity,
            String eventId) {
        Fixture fixture = fixture();
        MiningSessionResource gold = material(fixture, TrackedMaterial.GOLD);

        MiningSessionTargetMirror.QuantityResult result =
                fixture.mirror.mirrorQuantity(event(
                        gold,
                        quantity,
                        MiningSessionObservation.EvidenceType.INVENTORY_CHANGE,
                        TrackerSelection.GOLD,
                        eventId));

        assertEquals(
                MiningSessionTargetMirror.QuantityStatus.REJECTED,
                result.status());
        assertEquals(
                MiningSessionClassification.ReasonCode.NON_POSITIVE_QUANTITY,
                result.classification().reasonCode());
        assertEquals(0, fixture.ledger.entryCount());
    }

    private static void assertExplicitTransferRejected(
            MiningSessionObservation.EvidenceType evidence,
            String eventId) {
        Fixture fixture = fixture();
        MiningSessionResource gold = material(fixture, TrackedMaterial.GOLD);

        MiningSessionTargetMirror.QuantityResult result =
                fixture.mirror.mirrorQuantity(event(
                        gold,
                        32L,
                        evidence,
                        TrackerSelection.GOLD,
                        eventId));

        assertEquals(
                MiningSessionTargetMirror.QuantityStatus.REJECTED,
                result.status());
        assertEquals(
                MiningSessionClassification.ReasonCode
                        .EXPLICIT_NON_MINING_SOURCE,
                result.classification().reasonCode());
        assertEquals(0, fixture.ledger.entryCount());
    }

    private static void assertAppended(
            Fixture fixture,
            MiningSessionTargetMirror.QuantityResult result,
            MiningSessionResource resource,
            long expectedQuantity) {
        assertEquals(
                MiningSessionTargetMirror.QuantityStatus.APPENDED,
                result.status());
        assertEquals(
                MiningSessionTargetMirror.QuantityReason.APPENDED,
                result.reason());
        assertEquals(
                MiningSessionCategory.TARGET_MINED,
                result.classification().category());
        assertEquals(1, fixture.ledger.entryCount());
        assertEquals(
                expectedQuantity,
                fixture.ledger.quantity(
                        MiningSessionCategory.TARGET_MINED,
                        resource));
        assertEquals(
                MiningSessionPriceResolution.PriceStatus.UNRESOLVED,
                fixture.ledger.entries().getFirst()
                        .priceResolution().status());
    }

    private static void assertInvariantOther(
            Fixture fixture,
            MiningSessionTargetMirror.QuantityResult result) {
        assertEquals(
                MiningSessionTargetMirror.QuantityStatus.INVARIANT_REJECTED,
                result.status());
        assertEquals(
                MiningSessionTargetMirror.QuantityReason.UNEXPECTED_CATEGORY,
                result.reason());
        assertEquals(
                MiningSessionClassification.Outcome.WOULD_CREDIT,
                result.classification().outcome());
        assertEquals(
                MiningSessionCategory.OTHER_MINED,
                result.classification().category());
        assertEquals(0, fixture.ledger.entryCount());
    }

    private static Fixture fixture() {
        MiningSessionLedger ledger = new MiningSessionLedger();
        MiningResourceCatalog catalog = new MiningResourceCatalog();
        MiningSessionTargetMirror mirror = new MiningSessionTargetMirror(
                ledger,
                catalog);
        return new Fixture(ledger, catalog, mirror);
    }

    private static MiningSessionResource material(
            Fixture fixture,
            TrackedMaterial material) {
        return fixture.catalog.fromMaterial(material).orElseThrow().resource();
    }

    private static MiningSessionResource gemstone(
            Fixture fixture,
            GemstoneType gemstone,
            GemstoneTier tier) {
        return fixture.catalog.fromGemstone(gemstone, tier)
                .orElseThrow()
                .resource();
    }

    private static MiningSessionTargetMirror.QuantityEvent event(
            MiningSessionResource resource,
            long quantity,
            MiningSessionObservation.EvidenceType evidence,
            TrackerSelection selection,
            String eventId) {
        return new MiningSessionTargetMirror.QuantityEvent(
                resource,
                quantity,
                evidence,
                selection,
                SELECTION_EPOCH,
                OBSERVED_AT,
                eventId,
                null,
                "live-accounting");
    }

    private static MiningSessionTargetMirror.BlockObservation block(
            MiningSessionResource resource,
            TrackerSelection selection,
            String eventId) {
        return new MiningSessionTargetMirror.BlockObservation(
                resource,
                selection,
                SELECTION_EPOCH,
                OBSERVED_AT,
                eventId,
                null,
                "live-accounting");
    }

    private record Fixture(
            MiningSessionLedger ledger,
            MiningResourceCatalog catalog,
            MiningSessionTargetMirror mirror) {
    }
}
