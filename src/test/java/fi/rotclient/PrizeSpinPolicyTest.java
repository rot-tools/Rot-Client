package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class PrizeSpinPolicyTest {
    @Test
    void dungeonRewardChestTitlesAndLootFilter() {
        assertEquals("Gold", PrizeSpinPolicy.rewardChestType("Golden Chest").orElseThrow());
        assertEquals("Obsidian", PrizeSpinPolicy.rewardChestType("Obsidian Chest").orElseThrow());
        assertTrue(PrizeSpinPolicy.rewardChestType("Chest").isEmpty());
        List<String> loot = PrizeSpinPolicy.lootNames(List.of(
                "Black Stained Glass Pane",
                "§6Spirit Wing",
                "Barrier",
                "Shadow Assassin Chestplate"));
        assertEquals(List.of("Spirit Wing", "Shadow Assassin Chestplate"), loot);
        List<String> reel = PrizeSpinPolicy.spinReel(loot, 0, 8);
        assertEquals(8, reel.size());
        assertEquals("Spirit Wing", reel.get(7));
        assertEquals(7, PrizeSpinPolicy.focusedIndex(1.0D, 8));
        assertTrue(PrizeSpinPolicy.easeOutCubic(1.0D) > PrizeSpinPolicy.easeOutCubic(0.2D));
    }

    @Test
    void vanguardChatCollectsThenEnds() {
        assertEquals(
                PrizeSpinPolicy.ChatKind.START,
                PrizeSpinPolicy.classifyVanguardLine(false, "VANGUARD! You found:"));
        assertEquals(
                PrizeSpinPolicy.ChatKind.ITEM,
                PrizeSpinPolicy.classifyVanguardLine(true, "+ Heroic Hyperion"));
        assertEquals("Heroic Hyperion", PrizeSpinPolicy.vanguardItemName("+ Heroic Hyperion"));
        assertEquals(
                PrizeSpinPolicy.ChatKind.END,
                PrizeSpinPolicy.classifyVanguardLine(true, "Click to collect your rewards"));
        assertEquals(
                PrizeSpinPolicy.ChatKind.IGNORE,
                PrizeSpinPolicy.classifyVanguardLine(false, "Welcome to Hypixel"));
    }
}
