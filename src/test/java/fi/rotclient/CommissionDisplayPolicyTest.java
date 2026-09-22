package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The Commission HUD reads one tab-list widget, so the parser has to survive its variations. */
final class CommissionDisplayPolicyTest {
    private static List<CommissionDisplayPolicy.Commission> parse(String... lines) {
        return CommissionDisplayPolicy.parseTabLines(List.of(lines));
    }

    private static List<String> names(List<CommissionDisplayPolicy.Commission> list) {
        return list.stream().map(CommissionDisplayPolicy.Commission::name).toList();
    }

    // ------------------------------------------------------------------ what the tab list looks like

    @Test
    void readsTheDwarvenMinesWidgetAndStopsAtTheNextOne() {
        List<CommissionDisplayPolicy.Commission> parsed = parse(
                "Area: Dwarven Mines",
                "Commissions:",
                "Mithril Miner: 40%",
                "Goblin Slayer: DONE",
                "Powders:",
                "Mithril: 12,345");
        assertEquals(List.of("Mithril Miner", "Goblin Slayer"), names(parsed));
        assertEquals(40.0F, parsed.get(0).progressPercent(), 0.001F);
        assertFalse(parsed.get(0).done());
        assertTrue(parsed.get(1).done());
        assertEquals(100.0F, parsed.get(1).progressPercent(), 0.001F);
    }

    @Test
    void readsGlaciteFractionsAsPercentages() {
        List<CommissionDisplayPolicy.Commission> parsed = parse(
                "Commissions:",
                "Soft Glacite: 12/500",
                "Umber: 1,234/2,500",
                "Frozen Corpses:",
                "Corpse: 1/3");
        assertEquals(List.of("Soft Glacite", "Umber"), names(parsed));
        assertEquals(2.4F, parsed.get(0).progressPercent(), 0.05F);
        assertEquals(49.36F, parsed.get(1).progressPercent(), 0.05F);
    }

    @Test
    void acceptsTheOlderCommissionProgressLayout() {
        List<CommissionDisplayPolicy.Commission> parsed = parse(
                "Commission Progress",
                "• Mithril Miner: 40 %",
                "Titanium Miner - COMPLETED",
                "Goblin Slayer 100%",
                "HOTM:",
                "Tier: 7");
        assertEquals(List.of("Mithril Miner", "Titanium Miner", "Goblin Slayer"), names(parsed));
        assertEquals(40.0F, parsed.get(0).progressPercent(), 0.001F);
        assertTrue(parsed.get(1).done());
        assertTrue(parsed.get(2).done());
    }

    @Test
    void ignoresColourCodesBoldAndPadding() {
        List<CommissionDisplayPolicy.Commission> parsed = parse(
                "§6§lCommissions:",
                "  §f§lMithril Miner§r§7: §a40%",
                "§b Goblin Slayer§7: §a§lDONE");
        assertEquals(List.of("Mithril Miner", "Goblin Slayer"), names(parsed));
        assertTrue(parsed.get(1).done());
    }

    @Test
    void nonBreakingAndZeroWidthCharactersDoNotBreakARow() {
        // Hypixel pads tab rows with characters that String.trim() and \s do not treat as spaces.
        List<CommissionDisplayPolicy.Commission> parsed = parse(
                "Commissions: ",
                "  Mithril Miner: 40% ",
                "Goblin Slayer:​  DONE​",
                "Titanium Miner: 5%﻿");
        assertEquals(List.of("Mithril Miner", "Goblin Slayer", "Titanium Miner"), names(parsed));
        assertEquals(40.0F, parsed.get(0).progressPercent(), 0.001F);
        assertTrue(parsed.get(1).done());
        assertEquals(5.0F, parsed.get(2).progressPercent(), 0.001F);
    }

    @Test
    void statusWordsAreCaseInsensitiveAndMayEndInPunctuation() {
        List<CommissionDisplayPolicy.Commission> parsed = parse(
                "Commissions:",
                "Mithril Miner: Done",
                "Goblin Slayer: COMPLETE!",
                "Titanium Miner: completed.");
        assertEquals(3, parsed.size());
        for (CommissionDisplayPolicy.Commission commission : parsed) {
            assertTrue(commission.done(), commission.name());
        }
    }

    @Test
    void namesMayContainSeparatorsAndBullets() {
        List<CommissionDisplayPolicy.Commission> parsed = parse(
                "Commissions:",
                "• Rampart's Quarry - Mithril: 50%",
                "- Lava Springs Titanium: 12.5%",
                "► Star Sentry Puncher — 7%");
        assertEquals(
                List.of("Rampart's Quarry - Mithril", "Lava Springs Titanium", "Star Sentry Puncher"),
                names(parsed));
        assertEquals(12.5F, parsed.get(1).progressPercent(), 0.001F);
        assertEquals(7.0F, parsed.get(2).progressPercent(), 0.001F);
    }

    // ------------------------------------------------------------------ what must not become a commission

    @Test
    void rowsOutsideTheCommissionsWidgetAreIgnored() {
        assertTrue(parse("Fairy Souls: 50/250", "Minions: 3/10", "Mithril Miner: 40%").isEmpty());
        assertTrue(parse().isEmpty());
        assertTrue(CommissionDisplayPolicy.parseTabLines(null).isEmpty());
        assertEquals(List.of("Mithril Miner"), names(parse(
                "Mithril Miner: 10%", "Commissions:", "Mithril Miner: 40%")));
    }

    @Test
    void aWidgetHypixelAddsLaterStillEndsTheList() {
        // "Fossil Dust:" is in no header list. Its rows must not be read as commissions.
        List<CommissionDisplayPolicy.Commission> parsed = parse(
                "Commissions:",
                "Mithril Miner: 40%",
                "Fossil Dust:",
                "Dust: 45%",
                "Progress: 10/20");
        assertEquals(List.of("Mithril Miner"), names(parsed));
    }

    @Test
    void aLaterWidgetWithAHeaderOfItsOwnDoesNotHideALaterCommissionsWidget() {
        List<CommissionDisplayPolicy.Commission> parsed = parse(
                "Commissions:",
                "Mithril Miner: 40%",
                "Fossil Dust:",
                "Dust: 45%",
                "Commissions:",
                "Titanium Miner: 5%");
        assertEquals(List.of("Mithril Miner", "Titanium Miner"), names(parsed));
    }

    @Test
    void valueLinesInsideTheWidgetDoNotEndItOrCountAsRows() {
        List<CommissionDisplayPolicy.Commission> parsed = parse(
                "Commissions:",
                "Area: Dwarven Mines",
                "Mithril Miner: 40%",
                "Ping: 45%",
                "FPS: 90%");
        assertEquals(List.of("Mithril Miner"), names(parsed));
    }

    @Test
    void impossibleFractionsAndShortNamesAreDropped() {
        assertTrue(parse("Commissions:", "Umber: 5/0", "Ab: 40%", ": 40%", "40%").isEmpty());
        assertNull(CommissionDisplayPolicy.parseRow(null));
        assertNull(CommissionDisplayPolicy.parseRow("   "));
        assertNull(CommissionDisplayPolicy.parseRow("Mithril Miner"));
        assertNull(CommissionDisplayPolicy.parseRow("Mithril Miner: soon"));
    }

    @Test
    void percentagesAreClampedAndSeenTwiceKeepsOneRow() {
        List<CommissionDisplayPolicy.Commission> parsed = parse(
                "Commissions:",
                "Mithril Miner: 250%",
                "Goblin Slayer: 0%",
                "mithril miner: 60%");
        assertEquals(2, parsed.size());
        assertEquals("mithril miner", parsed.get(0).name().toLowerCase());
        assertEquals(60.0F, parsed.get(0).progressPercent(), 0.001F);
        assertEquals(0.0F, parsed.get(1).progressPercent(), 0.001F);
        assertFalse(parsed.get(1).done());
        assertTrue(new CommissionDisplayPolicy.Commission("x", 250.0F, false).progressPercent() <= 100.0F);
    }

    @Test
    void aRunawayListIsCapped() {
        List<String> lines = new ArrayList<>();
        lines.add("Commissions:");
        for (int i = 0; i < 40; i++) {
            lines.add("Commission number " + i + ": " + i + "%");
        }
        assertEquals(CommissionDisplayPolicy.MAX_COMMISSIONS,
                CommissionDisplayPolicy.parseTabLines(lines).size());
    }

    // ------------------------------------------------------------------ headers

    @Test
    void recognisesWidgetTitlesByShape() {
        assertTrue(CommissionDisplayPolicy.isWidgetHeader("Fossil Dust:"));
        assertTrue(CommissionDisplayPolicy.isWidgetHeader("  Pickaxe Ability:  "));
        assertFalse(CommissionDisplayPolicy.isWidgetHeader("Mithril Miner: 40%"));
        assertFalse(CommissionDisplayPolicy.isWidgetHeader("Area: Dwarven Mines"));
        assertFalse(CommissionDisplayPolicy.isWidgetHeader(":"));
        assertFalse(CommissionDisplayPolicy.isWidgetHeader("12:"));
        assertFalse(CommissionDisplayPolicy.isWidgetHeader(null));
        assertFalse(CommissionDisplayPolicy.isWidgetHeader("x".repeat(60) + ":"));
    }

    @Test
    void recognisesTheCommissionsHeadersAndTheMenu() {
        assertTrue(CommissionDisplayPolicy.isCommissionsHeader("Commissions:"));
        assertTrue(CommissionDisplayPolicy.isCommissionsHeader("Commissions"));
        assertTrue(CommissionDisplayPolicy.isCommissionsHeader("Commission Progress"));
        assertFalse(CommissionDisplayPolicy.isCommissionsHeader("Commissioned Art"));
        assertTrue(CommissionDisplayPolicy.isCommissionsMenu("Commissions"));
        assertTrue(CommissionDisplayPolicy.isCommissionsMenu("Commissions:"));
        assertTrue(CommissionDisplayPolicy.isCommissionsMenu("Commissions (Page 2)"));
        assertFalse(CommissionDisplayPolicy.isCommissionsMenu("Skyblock Menu"));
        assertFalse(CommissionDisplayPolicy.isCommissionsMenu(null));
    }

    @Test
    void menuLoreShowsCompletion() {
        assertTrue(CommissionDisplayPolicy.loreCompleted(List.of("§7Progress:", "§a§lCOMPLETED")));
        assertTrue(CommissionDisplayPolicy.loreCompleted(List.of("Status: completed")));
        assertFalse(CommissionDisplayPolicy.loreCompleted(List.of("§7Progress: §e40%")));
        assertFalse(CommissionDisplayPolicy.loreCompleted(List.of()));
        assertFalse(CommissionDisplayPolicy.loreCompleted(null));
        assertFalse(CommissionDisplayPolicy.loreCompleted(java.util.Arrays.asList("x", null)));
    }

    @Test
    void normalizeLineHandlesEmptyAndOddInput() {
        assertEquals("", CommissionDisplayPolicy.normalizeLine(null));
        assertEquals("", CommissionDisplayPolicy.normalizeLine(""));
        assertEquals("", CommissionDisplayPolicy.normalizeLine("§a§l  ​"));
        assertEquals("a b", CommissionDisplayPolicy.normalizeLine(" a  \t b  "));
    }

    // ------------------------------------------------------------------ the HUD text

    @Test
    void formatsRowsWithPlaceholders() {
        CommissionDisplayPolicy.Commission mithril =
                new CommissionDisplayPolicy.Commission("Mithril Miner", 40.0F, false);
        assertEquals("§7- §rMithril Miner: §640%§r",
                CommissionDisplayPolicy.formatLine(CommissionDisplayPolicy.DEFAULT_ROW, mithril, true));
        assertEquals("§7- §rMithril Miner: 40%",
                CommissionDisplayPolicy.formatLine(CommissionDisplayPolicy.DEFAULT_ROW, mithril, false));
        assertEquals("Mithril Miner=40%",
                CommissionDisplayPolicy.formatLine("#name=#progress", mithril, false));
        // A blank or missing template falls back to the default.
        assertEquals(
                CommissionDisplayPolicy.formatLine(CommissionDisplayPolicy.DEFAULT_ROW, mithril, false),
                CommissionDisplayPolicy.formatLine("  ", mithril, false));
        assertEquals(
                CommissionDisplayPolicy.formatLine(CommissionDisplayPolicy.DEFAULT_ROW, mithril, false),
                CommissionDisplayPolicy.formatLine(null, mithril, false));
    }

    @Test
    void onlyAFinishedCommissionReadsOneHundredPercent() {
        CommissionDisplayPolicy.Commission almost =
                new CommissionDisplayPolicy.Commission("Mithril Miner", 99.6F, false);
        CommissionDisplayPolicy.Commission done =
                new CommissionDisplayPolicy.Commission("Mithril Miner", 100.0F, true);
        String almostLine = CommissionDisplayPolicy.formatLine("#progress", almost, false);
        assertEquals("99%", almostLine);
        assertEquals("100%", CommissionDisplayPolicy.formatLine("#progress", done, false));
        assertTrue(CommissionDisplayPolicy.formatLine("#progress", done, true).startsWith("§a"));
        assertFalse(CommissionDisplayPolicy.formatLine("#progress", almost, true).startsWith("§a"));
    }

    @Test
    void percentColoursChangeAtTheDocumentedThresholds() {
        assertEquals("§c", CommissionDisplayPolicy.percentColorTag(0.0F));
        assertEquals("§c", CommissionDisplayPolicy.percentColorTag(24.9F));
        assertEquals("§6", CommissionDisplayPolicy.percentColorTag(25.0F));
        assertEquals("§e", CommissionDisplayPolicy.percentColorTag(50.0F));
        assertEquals("§3", CommissionDisplayPolicy.percentColorTag(75.0F));
        assertEquals("§a", CommissionDisplayPolicy.percentColorTag(100.0F));
        assertEquals("§a", CommissionDisplayPolicy.percentColorTag(400.0F));
        assertEquals("§c", CommissionDisplayPolicy.percentColorTag(-5.0F));
    }

    @Test
    void titleAndEmptyTextsUseDefaultsAndLegacyTags() {
        assertEquals("§cCommissions:", CommissionDisplayPolicy.formatTitle(null));
        assertEquals("§cNo commissions available!", CommissionDisplayPolicy.formatNone(""));
        assertEquals("§6Quests§r", CommissionDisplayPolicy.formatTitle("<gold>Quests<reset>"));
        assertEquals("", CommissionDisplayPolicy.applyLegacyTags(null));
    }

    // ------------------------------------------------------------------ keeping the list steady

    @Test
    void aBriefEmptyRefreshKeepsTheLastList() {
        List<CommissionDisplayPolicy.Commission> held = parse("Commissions:", "Mithril Miner: 40%");
        List<CommissionDisplayPolicy.Commission> none = List.of();
        assertSame(held, CommissionDisplayPolicy.retain(none, held, 1_000L, 1_200L));
        assertSame(held, CommissionDisplayPolicy.retain(null, held, 1_000L, 5_999L));
        // Once the hold runs out the widget really is gone.
        assertTrue(CommissionDisplayPolicy.retain(none, held, 1_000L,
                1_000L + CommissionDisplayPolicy.HOLD_MILLIS).isEmpty());
        // Nothing to hold, or a clock that went backwards, shows nothing.
        assertTrue(CommissionDisplayPolicy.retain(none, List.of(), 1_000L, 1_100L).isEmpty());
        assertTrue(CommissionDisplayPolicy.retain(none, held, 5_000L, 1_000L).isEmpty());
    }

    @Test
    void aRealChangeWinsImmediately() {
        List<CommissionDisplayPolicy.Commission> held = parse("Commissions:", "Mithril Miner: 40%");
        List<CommissionDisplayPolicy.Commission> fresh = parse("Commissions:", "Goblin Slayer: 5%");
        assertSame(fresh, CommissionDisplayPolicy.retain(fresh, held, 1_000L, 1_001L));
    }

    // ------------------------------------------------------------------ wiring

    @Test
    void theRuntimeParsesOncePerSnapshotAndUsesTheHold() throws Exception {
        String runtime = Files.readString(
                Path.of("src/client/java/fi/rotclient/CommissionDisplayRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(runtime.contains("lines != parsedLines"));
        assertTrue(runtime.contains("CommissionDisplayPolicy.retain("));
        assertTrue(runtime.contains("hudCacheFor"));
        assertFalse(runtime.contains("text.split(\"\\\\R\")"));
        assertNotNull(CommissionDisplayPolicy.DEFAULT_ROW);
    }
}
