package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class TempleSkyblockAutomationPolicyTest {
    @Test
    void escrowFixMapsHypixelChatToAhAndBz() {
        assertEquals("ah", EscrowFixPolicy.commandForMessage(
                "§cThere was an error with the auction house! (AUCTION_EXPIRED_OR_NOT_FOUND)"));
        assertEquals("ah", EscrowFixPolicy.commandForMessage("Claiming BIN auction..."));
        assertEquals("ah", EscrowFixPolicy.commandForMessage(
                "Visit the Auction House to collect your item!"));
        assertEquals("bz", EscrowFixPolicy.commandForMessage(
                "Escrow refunded 12000 coins for Bazaar Instant Buy Submit!"));
        assertEquals(null, EscrowFixPolicy.commandForMessage("Welcome to Hypixel SkyBlock!"));
    }

    @Test
    void autoHarpClicksFirstQuartzWhenMaskChanges() {
        List<Boolean> first = List.of(false, true, false, false, false, false, false);
        AutoHarpPolicy.TickResult result = AutoHarpPolicy.nextClick(first, 0);
        assertEquals(OptionalInt.of(38), result.clickSlot());
        AutoHarpPolicy.TickResult same = AutoHarpPolicy.nextClick(first, result.hash());
        assertTrue(same.clickSlot().isEmpty());
        assertTrue(AutoHarpPolicy.isHarpTitle("Harp - Hymn of the Day"));
        assertFalse(AutoHarpPolicy.isHarpTitle("Enchanting Table"));
    }

    @Test
    void autoGfsMatchesSackFillAndChat() {
        assertTrue(AutoGfsPolicy.gfsCommand(true, 4, AutoGfsPolicy.PEARL)
                .orElseThrow()
                .equals("gfs ender_pearl 12"));
        assertTrue(AutoGfsPolicy.gfsCommand(false, 0, AutoGfsPolicy.PEARL).isEmpty());
        assertTrue(AutoGfsPolicy.gfsCommand(true, 16, AutoGfsPolicy.PEARL).isEmpty());
        assertTrue(AutoGfsPolicy.isDungeonStart(
                "[NPC] Mort: Here, I found this map when I first entered the dungeon."));
        assertTrue(AutoGfsPolicy.isLocalPuzzleFail("PUZZLE FAIL! Steve couldn't solve it", "Steve"));
        assertFalse(AutoGfsPolicy.isLocalPuzzleFail("PUZZLE FAIL! Alex couldn't solve it", "Steve"));
        assertTrue(AutoGfsPolicy.locationAllowsRefill(
                true, false, false, true, false, false));
        assertFalse(AutoGfsPolicy.locationAllowsRefill(
                true, true, true, false, true, true));
        assertTrue(AutoGfsPolicy.locationAllowsRefill(
                false, true, false, false, true, false));
        assertTrue(AutoGfsPolicy.isKuudraSidebar("Kuudra's Hollow\nWave 3"));
        assertTrue(AutoGfsPolicy.timerReady(100, 0, 5));
        assertFalse(AutoGfsPolicy.timerReady(99, 0, 5));
    }

    @Test
    void autoSellMatchesTradesMenuAndDefaultList() {
        assertTrue(AutoSellPolicy.isSellMenu("§8Trades"));
        assertTrue(AutoSellPolicy.isSellMenu("Booster Cookie"));
        assertFalse(AutoSellPolicy.isSellMenu("Chest"));
        assertEquals(AutoSellPolicy.ClickKind.QUICK_MOVE, AutoSellPolicy.clickKind("Shift"));
        assertEquals(AutoSellPolicy.ClickKind.CLONE, AutoSellPolicy.clickKind("Middle"));
        assertEquals(300L, AutoSellPolicy.waitMs(6, 0, 0.0D));
        List<AutoSellPolicy.SlotView> slots = List.of(
                new AutoSellPolicy.SlotView(0, "Spirit Leap", false),
                new AutoSellPolicy.SlotView(54, "Enchanted Ice", false),
                new AutoSellPolicy.SlotView(55, "Skeleton Master Chestplate", false));
        assertEquals(OptionalInt.of(54), AutoSellPolicy.nextSellSlot(slots, AutoSellPolicy.DEFAULT_ITEMS));
        assertFalse(AutoSellPolicy.shouldSell(
                "Skeleton Master Chestplate", Set.of("skeleton master")));
        assertTrue(AutoSellPolicy.withDefaults(List.of("foo")).contains("enchanted ice"));
    }

    @Test
    void ghostsHideInvisibleCreepersWhenBothLayersOff() {
        GhostsPolicy.Decision hidden = GhostsPolicy.decide(true, false, false, true, true);
        assertTrue(hidden.suppressEntity());
        GhostsPolicy.Decision body = GhostsPolicy.decide(true, true, false, true, true);
        assertTrue(body.forceVisible());
        assertTrue(body.hidePoweredLayer());
        GhostsPolicy.Decision vanillaOverlay = GhostsPolicy.decide(true, false, true, true, true);
        assertFalse(vanillaOverlay.suppressEntity());
        assertFalse(vanillaOverlay.forceVisible());
        assertFalse(vanillaOverlay.hidePoweredLayer());
        assertEquals(GhostsPolicy.Decision.NONE, GhostsPolicy.decide(false, true, true, true, true));
    }

    @Test
    void extrasRoundTripNewTempleModules() {
        QolUtilityConfig config = new QolUtilityConfig();
        config.setModuleEnabled("qol.cheater_wardrobe", true);
        assertTrue(config.isModuleEnabled("qol.cheater_wardrobe"));
        config.setModuleEnabled("qol.escrow_fix", true);
        config.setModuleEnabled("qol.auto_harp", true);
        config.setModuleEnabled("qol.auto_gfs", true);
        config.setModuleEnabled("qol.auto_sell", true);
        config.setModuleEnabled("qol.ghosts", true);
        config.writeBoolean("qol.auto_gfs.refill_pearl", false);
        assertFalse(config.readBoolean("qol.auto_gfs.refill_pearl"));
        assertTrue(config.writeNumber("qol.auto_experiments.serum_count", 3));
        assertEquals(3.0D, config.readNumber("qol.auto_experiments.serum_count"));
        assertTrue(config.writeEnum("qol.auto_sell.click_type", "Middle"));
        assertEquals("Middle", config.readEnum("qol.auto_sell.click_type"));
        assertTrue(config.writeText("qol.auto_sell.list", "enchanted ice, mimic fragment"));
        assertTrue(config.readText("qol.auto_sell.list").contains("enchanted ice"));
        assertTrue(config.extras().addAutoSellDefaults());
        assertTrue(config.readText("qol.auto_sell.list").contains("superboom tnt"));
        assertTrue(config.writeKeybind("qol.ghosts.keybind", "G"));
        assertEquals("G", config.readKeybind("qol.ghosts.keybind"));
        assertTrue(config.resetModuleToDefaults("qol.ghosts"));
        assertFalse(config.isModuleEnabled("qol.ghosts"));
        assertFalse(config.extras().ghostsShowPowered);
    }
}
