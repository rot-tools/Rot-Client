package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ColumnStyleQolPolicyTest {
    @Test
    void autoConversationSelectsGreenThenFirstCommand() {
        List<AutoConversationPolicy.ClickOption> options = List.of(
                new AutoConversationPolicy.ClickOption("/talk1", 0xFFFFFF, "[§fTalk]"),
                new AutoConversationPolicy.ClickOption("/next", AutoConversationPolicy.GREEN_RGB, "[§aNext]"));
        List<String> green = AutoConversationPolicy.selectCommands(options, true, true);
        assertEquals(List.of("next"), green);
        List<String> all = AutoConversationPolicy.selectCommands(options, false, true);
        assertEquals(2, all.size());
        assertTrue(AutoConversationPolicy.selectCommands(options, false, false).isEmpty());
        assertTrue(AutoConversationPolicy.isNpcPrompt("[NPC] Guide: Hello"));
        assertTrue(AutoConversationPolicy.isNpcPrompt("Select an option: "));
        assertEquals(7, AutoConversationPolicy.scheduledDelayTicks(4, 3));
    }

    @Test
    void fishingHelperAddsRecastPaddingAndDetectsBite() {
        assertTrue(FishingHelperPolicy.isBiteHologram("§c!!!"));
        assertEquals(3, FishingHelperPolicy.recastDelayTicks(1, 0, 0.0D));
        assertTrue(FishingHelperPolicy.shouldPull(true, true, true, true, true));
        assertTrue(FishingHelperPolicy.shouldRecastCheck(true, true, true, true, false));
        assertFalse(FishingHelperPolicy.shouldRecastCheck(true, true, true, true, true));
    }

    @Test
    void etherwarpRecognizesConduitAndShiftWindow() {
        assertTrue(EtherwarpHelperPolicy.isEtherwarpItem(true, ""));
        assertTrue(EtherwarpHelperPolicy.isEtherwarpItem(false, "ETHERWARP_CONDUIT"));
        assertFalse(EtherwarpHelperPolicy.isEtherwarpItem(false, "ASPECT_OF_THE_END"));
        assertTrue(EtherwarpHelperPolicy.needsAutoShift(false, true));
        assertTrue(EtherwarpHelperPolicy.shouldAssistLookTarget(true));
        assertFalse(EtherwarpHelperPolicy.shouldAssistLookTarget(false));
        assertTrue(EtherwarpHelperPolicy.forceSneakInput(true));
        assertFalse(EtherwarpHelperPolicy.forceSneakInput(false));
        assertTrue(EtherwarpHelperPolicy.shiftHoldTicks(0.0D) >= 2);
        assertTrue(EtherwarpHelperPolicy.shiftHoldTicks(1.0D) <= 4);
    }

    @Test
    void commissionParsesTabRowsAndFormatsPlaceholders() {
        List<CommissionDisplayPolicy.Commission> parsed = CommissionDisplayPolicy.parseTabLines(List.of(
                "Commissions",
                "Mithril Miner: 40%",
                "Goblin Slayer: DONE",
                "Area: Dwarven Mines"));
        assertEquals(2, parsed.size());
        assertEquals("Mithril Miner", parsed.get(0).name());
        assertTrue(parsed.get(1).done());
        String line = CommissionDisplayPolicy.formatLine(
                CommissionDisplayPolicy.DEFAULT_ROW, parsed.get(0), true);
        assertTrue(line.contains("Mithril Miner"));
        assertTrue(line.contains("%"));
    }

    @Test
    void commissionParsesDwarvenBlankLineAndGlaciteFractions() {
        List<CommissionDisplayPolicy.Commission> dwarven = CommissionDisplayPolicy.parseTabLines(List.of(
                "Area: Dwarven Mines",
                "Dwarven Village",
                "Commissions:",
                "",
                "Mithril Miner: 40%",
                "Goblin Slayer: DONE",
                "Powders:",
                "Mithril: 12,345"));
        assertEquals(2, dwarven.size());
        assertEquals("Mithril Miner", dwarven.get(0).name());
        assertTrue(dwarven.get(1).done());

        List<CommissionDisplayPolicy.Commission> glacite = CommissionDisplayPolicy.parseTabLines(List.of(
                "Commissions:",
                "Soft Glacite: 12/500",
                "Umber: 1,234/2,500",
                "Frozen Corpses:",
                "Corpse: 1/3"));
        assertEquals(2, glacite.size());
        assertEquals("Soft Glacite", glacite.get(0).name());
        assertEquals(2.4F, glacite.get(0).progressPercent(), 0.05F);
        assertEquals("Umber", glacite.get(1).name());
        assertFalse(glacite.get(1).done());
        assertTrue(CommissionDisplayPolicy.parseTabLines(List.of(
                "Fairy Souls: 50/250",
                "Minions: 3/10")).isEmpty());
    }

    @Test
    void itemRarityReadsLastLoreLine() {
        assertEquals(
                ItemRarityPolicy.Rarity.LEGENDARY,
                ItemRarityPolicy.parseRarity(List.of("Sharpness V", "LEGENDARY SWORD")));
        assertTrue(ItemRarityPolicy.drawFill(ItemRarityPolicy.STYLE_FILLED_OUTLINE));
        assertTrue(ItemRarityPolicy.drawOutline(ItemRarityPolicy.STYLE_FILLED_OUTLINE));
        int fill = ItemRarityPolicy.withAlpha(0x00FFAA00, 0.25F);
        assertEquals(64, (fill >>> 24) & 0xFF);
        int outline = ItemRarityPolicy.withAlpha(0x00FFAA00, 0.0F);
        assertEquals(0, (outline >>> 24) & 0xFF);
    }

    @Test
    void missingEnchantsFindsSwordPoolGaps() {
        List<String> lore = List.of("Sharpness V", "Critical VI", "LEGENDARY SWORD");
        List<String> missing = MissingEnchantsPolicy.missing(
                MissingEnchantsPolicy.itemType(lore),
                MissingEnchantsPolicy.presentEnchantNames(lore));
        assertTrue(missing.contains("Looting"));
        assertFalse(missing.contains("Sharpness"));
        assertTrue(MissingEnchantsPolicy.shouldShow(true, true, false));
        assertFalse(MissingEnchantsPolicy.shouldShow(true, false, false));
        List<String> arabic = List.of(
                "Sharpness 5, Critical 6, First Strike 4",
                "§6§l§kX§r §6§lLEGENDARY DUNGEON SWORD §6§l§kX");
        List<String> arabicMissing = MissingEnchantsPolicy.missing(
                MissingEnchantsPolicy.itemType(arabic),
                MissingEnchantsPolicy.presentEnchantNames(arabic));
        assertFalse(arabicMissing.contains("Sharpness"));
        assertTrue(arabicMissing.contains("Looting"));
        assertEquals(
                "KATANA",
                MissingEnchantsPolicy.itemType(List.of("SHINY MYTHIC DUNGEON KATANA")));
        assertTrue(MissingEnchantsPolicy.missing(
                MissingEnchantsPolicy.itemType(List.of(), "Hyperion", "HYPERION"),
                Set.of()).contains("Sharpness"));

        MissingEnchantsPolicy.TooltipPlan enchantPlan = MissingEnchantsPolicy.plan(
                "SWORD",
                java.util.Map.of("sharpness", 5, "critical", 7),
                true,
                false);
        assertTrue(enchantPlan.upgrades().contains("Sharpness V → VII"));
        assertFalse(enchantPlan.upgrades().stream().anyMatch(line -> line.startsWith("Critical")));
        assertFalse(enchantPlan.missing().contains("Smite"));
        assertFalse(enchantPlan.missing().contains("Bane of Arthropods"));

        MissingEnchantsPolicy.TooltipPlan withConflicts = MissingEnchantsPolicy.plan(
                "SWORD", java.util.Map.of("sharpness", 5), false, true);
        assertTrue(withConflicts.missing().contains("Smite"));
    }

    @Test
    void mobHighlightTogglesNames() {
        List<String> added = MobHighlightPolicy.addName(List.of(), "Goblin");
        assertTrue(MobHighlightPolicy.matches("[Lv5] Goblin", added));
        assertTrue(MobHighlightPolicy.removeName(added, "goblin").isEmpty());
    }

    @Test
    void worldScannerMapsHitNamesAndStyles() {
        assertEquals("corleone", WorldScannerEspSettings.idForHit("Corleone Dock"));
        assertEquals("key_guardian", WorldScannerEspSettings.idForHit("Key Guardian Tower"));
        assertEquals("fairy", WorldScannerEspSettings.idForHit("Fairy Grotto"));
        assertEquals("divan", WorldScannerEspSettings.idForHit("Mines of Divan"));
        assertEquals("Mines of Divan", WorldScannerEspSettings.labelForHit("Divan"));
        assertEquals("king", WorldScannerEspSettings.idForHit("Goblin King"));
        assertTrue(WorldScannerEspSettings.drawFill(WorldScannerEspSettings.STYLE_BOTH));
        assertTrue(WorldScannerEspSettings.masterAllows(
                "king", true, true, true, true, true));
        assertFalse(WorldScannerEspSettings.masterAllows(
                "king", false, true, true, true, true));
        String[] parts = WorldScannerEspSettings.splitSetting(
                "qol.world_scanner.target.key_guardian.color");
        assertEquals("key_guardian", parts[0]);
        assertEquals("color", parts[1]);
    }

    @Test
    void customTooltipOnlyNameRequiresBoundHeldKey() {
        assertFalse(CustomTooltipPolicy.showOnlyName(true, true, true));
        assertTrue(CustomTooltipPolicy.showOnlyName(true, false, true));
        assertEquals(8, CustomTooltipPolicy.nextOffset(0, 1, 8, true, false, 100));
        assertEquals(-8, CustomTooltipPolicy.nextOffset(0, -1, 8, true, false, 100));
        assertEquals(0, CustomTooltipPolicy.nextOffset(100, 1, 8, true, true, 100));
        assertEquals(12, CustomTooltipPolicy.panAfterClamp(20, 8));
        assertTrue(CustomTooltipPolicy.panTooltipVertically(false, false));
        assertFalse(CustomTooltipPolicy.panTooltipVertically(true, false));
        assertTrue(CustomTooltipPolicy.storageOverlayTakesWheel(true, false));
        assertEquals(1, CustomTooltipPolicy.verticalWheelDelta(-1.0D));
        assertEquals(-1, CustomTooltipPolicy.verticalWheelDelta(1.0D));
        assertEquals(1, CustomTooltipPolicy.verticalWheelDelta(-0.2D));
    }

    @Test
    void qolConfigRoundTripsNewSettings() {
        QolUtilityConfig config = new QolUtilityConfig();
        config.writeBoolean("qol.auto_conversation.green", false);
        assertFalse(config.readBoolean("qol.auto_conversation.green"));
        assertTrue(config.writeNumber("qol.fishing_helper.pull_delay", 6));
        assertEquals(6.0D, config.readNumber("qol.fishing_helper.pull_delay"));
        assertTrue(config.writeColor("qol.item_rarity.legendary", 0xFF112233));
        assertEquals(0xFF112233, config.readColor("qol.item_rarity.legendary"));
        assertTrue(config.writeKeybind("qol.missing_enchants.keybind", ""));
        assertEquals("", config.readKeybind("qol.missing_enchants.keybind"));
        config.writeBoolean("qol.world_scanner.target.fairy.enabled", false);
        assertFalse(config.worldScannerFairyGrottos);
        assertFalse(config.readBoolean("qol.world_scanner.target.fairy.enabled"));
        assertTrue(config.writeEnum("qol.world_scanner.target.divan.style", "Outline"));
        assertEquals("Outline", config.readEnum("qol.world_scanner.target.divan.style"));
        assertTrue(config.writeColor("qol.world_scanner.target.divan.color", 0xFF00FF00));
        assertEquals(0xFF00FF00, config.readColor("qol.world_scanner.target.divan.color"));
        assertEquals(80.0F, config.pose("commission")[1], 0.01F);
        assertTrue(config.resetModuleToDefaults("qol.auto_conversation"));
        assertTrue(config.autoConversationGreen);
    }
}
