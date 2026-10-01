package fi.rotclient;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MiningHudRegressionTest {
    @Test void petWidgetReadsSplitTitleAndLiveXp() {
        var pet = PetHudPolicy.parseTabText("Players:\n[Lvl 60] Somebody\nPet:\n[Lvl 70] Rabbit\n25k/100k XP (25%)\nCommissions:\nGoblin Slayer: 20%").orElseThrow();
        assertEquals("Rabbit", pet.name());
        assertEquals(25, pet.progressPercent(), 0.001);
        assertEquals(71, pet.nextLevel());
    }
    @Test void petRefreshReplacesCachedProgressAndPetSwitchDropsOldDetails() {
        var cached = new PetHudPolicy.Snapshot(70, "Rabbit", "Textbook", 1234,
                ItemRarityPolicy.DEFAULT_EPIC, ItemRarityPolicy.DEFAULT_RARE, 10, 71, false);
        var fresh = PetHudPolicy.parseTabText("Pet: [Lvl 70] Rabbit\nEXP: 60/100").orElseThrow();
        assertEquals(60, PetHudPolicy.mergeTabSnapshot(cached, fresh).progressPercent(), 0.001);
        var other = PetHudPolicy.mergeTabSnapshot(cached, new PetHudPolicy.Snapshot(20, "Bee", ""));
        assertEquals("", other.heldItem());
        assertEquals(-1, other.experience());
        assertFalse(other.hasProgress());
    }
    @Test void unrelatedLevelRowsCannotInventAnEquippedPet() {
        assertTrue(PetHudPolicy.parseTabText("Players:\n[Lvl 90] Someone\nCommissions:\nMithril Miner: 5%").isEmpty());
    }
    @Test void splitCommissionRowsStayInsideTheirWidget() {
        var rows = CommissionDisplayPolicy.parseTabLines(List.of("Commissions:", "Mithril Miner", "25%", "Goblin Slayer", "COMPLETED", "Pet:", "Rabbit", "50%"));
        assertEquals(2, rows.size());
        assertEquals(25, rows.getFirst().progressPercent());
        assertTrue(rows.get(1).done());
    }
    @Test void highlightsRequireMatchingIncompleteCommission() {
        var active = List.of(new CommissionDisplayPolicy.Commission("Ice Walker Slayer", 20, false));
        assertTrue(MiningLeftoverPolicy.matchesActiveCommission("[Lv45] Glacite Walker 500❤", active));
        assertFalse(MiningLeftoverPolicy.matchesActiveCommission("Goblin", active));
        assertFalse(MiningLeftoverPolicy.matchesActiveCommission("Glacite Walker", List.of(new CommissionDisplayPolicy.Commission("Ice Walker Slayer", 100, true))));
        assertFalse(MiningLeftoverPolicy.matchesActiveCommission("Goblin", List.of()));
    }
    @Test void skyMallAcceptsChatAndSplitWidgetButNotThePossibleBuffList() {
        assertEquals("+50 Mining Fortune", MiningLeftoverPolicy.parseSkyMallChat("§eNew buff: §a+50 Mining Fortune").orElseThrow().perk());
        assertEquals("+100 Mining Speed", MiningLeftoverPolicy.parseSkyMall(List.of("SkyMall:", "+100 Mining Speed")).orElseThrow().perk());
        assertTrue(MiningLeftoverPolicy.parseSkyMallLore(List.of("Possible buffs:", "+50 Mining Fortune")).isEmpty());
        assertEquals("+15% Powder", MiningLeftoverPolicy.parseSkyMallLore(List.of("Current buff:", "+15% Powder")).orElseThrow().perk());
    }
    @Test void landmarksHaveUniqueNamesAndIncludeTheForge() {
        assertEquals(DwarvenWaypointPolicy.LANDMARKS.size(), DwarvenWaypointPolicy.LANDMARKS.stream().map(DwarvenWaypointPolicy.Landmark::name).distinct().count());
        assertTrue(DwarvenWaypointPolicy.LANDMARKS.stream().anyMatch(p -> p.name().equals("The Forge") && p.z() == -69));
    }
}
