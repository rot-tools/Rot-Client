package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class MobLootSackEvaluatorTest {
    private final MiningResourceCatalog catalog = new MiningResourceCatalog();

    @Test
    void creditsCombatSackRottenFleshAfterTheInventoryWindow() {
        MobLootAttribution attribution = new MobLootAttribution();
        attribution.recordAttack(1, 1_000L);
        assertTrue(attribution.recordDeath(1, 1_200L));

        Optional<MobLootSackEvaluator.Credit> credit =
                MobLootSackEvaluator.evaluate(
                        attribution,
                        catalog,
                        TrackerSelection.GOLD,
                        sack("Rotten Flesh", 2L, "Combat Sack"),
                        1_200L + 5_471L);

        assertTrue(credit.isPresent());
        assertEquals("ROTTEN_FLESH", credit.get().itemId());
        assertEquals(2L, credit.get().quantity());
        assertEquals("Rotten Flesh", credit.get().displayName());
    }

    @Test
    void ignoresCombatSackWithoutAKillWindow() {
        Optional<MobLootSackEvaluator.Credit> credit =
                MobLootSackEvaluator.evaluate(
                        new MobLootAttribution(),
                        catalog,
                        TrackerSelection.GOLD,
                        sack("Rotten Flesh", 2L, "Combat Sack"),
                        5_000L);
        assertTrue(credit.isEmpty());
    }

    @Test
    void ignoresMiningSackEvenDuringAKillWindow() {
        MobLootAttribution attribution = new MobLootAttribution();
        attribution.recordAttack(2, 1_000L);
        assertTrue(attribution.recordDeath(2, 1_100L));

        assertTrue(MobLootSackEvaluator.evaluate(
                attribution,
                catalog,
                TrackerSelection.GOLD,
                sack("Cobblestone", 40L, "Mining Sack"),
                1_500L).isEmpty());
    }

    @Test
    void doesNotStealTheSelectedMiningTargetFamily() {
        MobLootAttribution attribution = new MobLootAttribution();
        attribution.recordAttack(3, 1_000L);
        assertTrue(attribution.recordDeath(3, 1_100L));

        assertTrue(MobLootSackEvaluator.evaluate(
                attribution,
                catalog,
                TrackerSelection.GOLD,
                sack("Enchanted Gold", 1L, "Combat Sack"),
                1_500L).isEmpty());
    }

    @Test
    void detectorCreditsCanonicalMobRowsFromCombatSack() {
        List<String> credits = new ArrayList<>();
        MobLootInventoryDetector detector = new MobLootInventoryDetector(
                (itemId, displayName, quantity, nowMillis) ->
                        credits.add(itemId + ":" + quantity));
        assertTrue(detector.recordAttributedKill(4, 10_000L, 10_500L));
        assertTrue(detector.offerSackGain(
                catalog,
                TrackerSelection.GOLD,
                sack("Rotten Flesh", 2L, "Combat Sack"),
                16_000L));
        assertEquals(List.of("ROTTEN_FLESH:2"), credits);
    }

    @Test
    void creditsSlayerAndDungeonSacksDuringTheKillWindow() {
        MobLootAttribution attribution = new MobLootAttribution();
        attribution.recordAttack(5, 1_000L);
        assertTrue(attribution.recordDeath(5, 1_100L));

        assertTrue(MobLootSackEvaluator.evaluate(
                attribution,
                catalog,
                TrackerSelection.GOLD,
                sack("Tarantula Web", 3L, "Slayer Sack"),
                2_000L).isPresent());
        assertTrue(MobLootSackEvaluator.evaluate(
                attribution,
                catalog,
                TrackerSelection.GOLD,
                sack("Poisonous Potato", 1L, "Agronomy Sack"),
                2_000L).isPresent());
        assertTrue(MobLootSackEvaluator.evaluate(
                attribution,
                catalog,
                TrackerSelection.GOLD,
                sack("Cobblestone", 40L, "Mining Sack"),
                2_000L).isEmpty());
    }

    @Test
    void detectorCreditsCoinsFromChatDuringTheKillWindow() {
        List<String> credits = new ArrayList<>();
        MobLootInventoryDetector detector = new MobLootInventoryDetector(
                (itemId, displayName, quantity, nowMillis) ->
                        credits.add(itemId + ":" + quantity));
        assertTrue(detector.recordAttributedKill(6, 10_000L, 10_500L));
        assertTrue(detector.offerCoinGain(12L, 16_000L));
        assertFalse(detector.offerCoinGain(12L, 30_000L));
        assertEquals(List.of("COINS:12"), credits);
    }

    @Test
    void actionBarCreditsCoinsAndNonFleshDropsDuringTheKillWindow() {
        List<String> credits = new ArrayList<>();
        MobLootInventoryDetector detector = new MobLootInventoryDetector(
                (itemId, displayName, quantity, nowMillis) ->
                        credits.add(itemId + ":" + quantity));
        assertTrue(detector.recordAttributedKill(7, 10_000L, 10_500L));
        assertTrue(detector.offerActionBarGain(
                catalog,
                TrackerSelection.GOLD,
                "+12 coins",
                11_000L));
        assertTrue(detector.offerActionBarGain(
                catalog,
                TrackerSelection.GOLD,
                "+1 Poisonous Potato",
                11_200L));
        assertFalse(detector.offerActionBarGain(
                catalog,
                TrackerSelection.GOLD,
                "+1 Poisonous Potato",
                11_400L),
                "same action-bar/sack quantity must not double-count");
        assertTrue(detector.offerActionBarGain(
                catalog,
                TrackerSelection.GOLD,
                "+2 Rotten Flesh +3% ★ Magic Find",
                11_500L));
        assertTrue(detector.offerActionBarGain(
                catalog,
                TrackerSelection.GOLD,
                "+3 Rotten Flesh (+3 ✯ Magic Find)",
                12_000L));
        assertFalse(detector.offerActionBarGain(
                catalog,
                TrackerSelection.GOLD,
                "+40 Kill Combo +3% ★ Magic Find",
                12_100L));
        assertFalse(detector.offerActionBarGain(
                catalog,
                TrackerSelection.GOLD,
                "+5 Crypt Ghoul ✯ Magic Find",
                12_200L));
        assertEquals(
                List.of(
                        "COINS:12",
                        "POISONOUS_POTATO:1",
                        "ROTTEN_FLESH:2",
                        "ROTTEN_FLESH:3"),
                credits);
        assertFalse(detector.offerActionBarGain(
                catalog,
                TrackerSelection.GOLD,
                "+128 Hard Stone",
                11_600L),
                "mining-catalog action-bar lines must stay off the MOB path");
    }

    @Test
    void namedRareDropCreditsDuringTheKillWindow() {
        MobLootAttribution attribution = new MobLootAttribution();
        attribution.recordAttack(8, 1_000L);
        assertTrue(attribution.recordDeath(8, 1_100L));
        Optional<MobLootSackEvaluator.Credit> credit =
                MobLootSackEvaluator.evaluateNamed(
                        attribution,
                        catalog,
                        TrackerSelection.GOLD,
                        "Poisonous Potato",
                        1L,
                        2_000L);
        assertTrue(credit.isPresent());
        assertEquals("POISONOUS_POTATO", credit.get().itemId());
    }

    private static SackChangeParser.Change sack(
            String itemName,
            long delta,
            String sackName) {
        return new SackChangeParser.Change(
                delta, itemName, List.of(sackName));
    }
}
