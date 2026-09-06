package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

final class CustomScoreboardPolicyTest {
    @Test
    void rebuildsPurseAndBitsFromHypixelLines() {
        CustomScoreboardPolicy.ComposeResult result = compose(
                List.of("§6Purse: §61,234,567", "§bBits: §b12,000"),
                List.of());
        String joined = joined(result);
        assertTrue(joined.contains("Purse"));
        assertTrue(joined.contains("1,234,567"));
        assertTrue(joined.contains("Bits"));
        assertTrue(joined.contains("12,000"));
        assertTrue(result.unknownPlain().isEmpty());
    }

    @Test
    void appearanceOrderControlsRows() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Purse\nBits";
        CustomScoreboardPolicy.ComposeResult result = CustomScoreboardPolicy.compose(
                view(List.of("Purse: 10", "Bits: 4", "Objective: none"), List.of()),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        List<String> plains = result.rows().stream().map(CustomScoreboardPolicy.Row::plain).toList();
        assertEquals("Purse: 10", plains.get(0));
        assertEquals("Bits: 4", plains.get(1));
        assertFalse(plains.stream().anyMatch(line -> line.toLowerCase().contains("objective")));
    }

    @Test
    void hidesEmptyAndConsecutiveBlankRows() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Purse\nEmpty\nEmpty\nBits";
        settings.hideEmpty = true;
        settings.hideConsecutiveEmpty = true;
        CustomScoreboardPolicy.ComposeResult result = CustomScoreboardPolicy.compose(
                view(List.of("Purse: 8"), List.of()),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        long blanks = result.rows().stream().filter(CustomScoreboardPolicy.Row::blank).count();
        assertTrue(blanks <= 1);
        assertFalse(joined(result).toLowerCase().contains("bits:"));
    }

    @Test
    void compactNumbersAndLayouts() {
        assertEquals("1.2M", CustomScoreboardPolicy.formatNumber(
                1_200_000L, CustomScoreboardPolicy.NumberStyle.COMPACT));
        assertEquals("1,234", CustomScoreboardPolicy.formatNumber(
                1234L, CustomScoreboardPolicy.NumberStyle.COMMA));
        assertEquals(
                CustomScoreboardPolicy.NumberLayout.LABEL_WHITE,
                CustomScoreboardPolicy.parseNumberLayout("White label: color number"));
        assertEquals(
                CustomScoreboardPolicy.NumberLayout.LABEL_COLOR,
                CustomScoreboardPolicy.parseNumberLayout("Colored label: number"));
        assertEquals(
                CustomScoreboardPolicy.NumberLayout.NUMBER_LABEL,
                CustomScoreboardPolicy.parseNumberLayout("Color number then label"));
        assertEquals(
                CustomScoreboardPolicy.NumberLayout.NUMBER_WHITE_LABEL,
                CustomScoreboardPolicy.parseNumberLayout("Color number, white label"));
    }

    @Test
    void powderModesAndMarkup() {
        assertEquals(CustomScoreboardPolicy.PowderMode.AVAILABLE,
                CustomScoreboardPolicy.parsePowderMode("Available"));
        assertEquals(CustomScoreboardPolicy.PowderMode.TOTAL,
                CustomScoreboardPolicy.parsePowderMode("Total"));
        assertEquals(CustomScoreboardPolicy.PowderMode.BOTH,
                CustomScoreboardPolicy.parsePowderMode("Available / All"));
        assertEquals("§6§lSKYBLOCK", CustomScoreboardPolicy.decodeMarkup("&&6&&lSKYBLOCK"));
        assertEquals("§ewww.hypixel.net", CustomScoreboardPolicy.decodeMarkup("&&ewww.hypixel.net"));
    }

    @Test
    void eventPriorityPicksFirstMatchUnlessShowAll() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Events";
        settings.eventPriority = "Dark Auction\nSpooky";
        settings.showAllEvents = false;
        CustomScoreboardPolicy.ComposeResult first = CustomScoreboardPolicy.compose(
                view(List.of("Dark Auction in 5m", "Spooky Festival"), List.of()),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        String one = joined(first);
        assertTrue(one.contains("Dark Auction"));
        assertFalse(one.contains("Spooky"));
        settings.showAllEvents = true;
        CustomScoreboardPolicy.ComposeResult all = CustomScoreboardPolicy.compose(
                view(List.of("Dark Auction in 5m", "Spooky Festival"), List.of()),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        String both = joined(all);
        assertTrue(both.contains("Dark Auction"));
        assertTrue(both.contains("Spooky"));
    }

    @Test
    void numberDiffSuffixLastsFiveSeconds() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Purse";
        settings.showDiff = true;
        CustomScoreboardPolicy.DeltaBook deltas = new CustomScoreboardPolicy.DeltaBook();
        CustomScoreboardPolicy.compose(
                view(1_000L, List.of("Purse: 100"), List.of()),
                settings.options(),
                deltas);
        CustomScoreboardPolicy.ComposeResult next = CustomScoreboardPolicy.compose(
                view(1_100L, List.of("Purse: 200"), List.of()),
                settings.options(),
                deltas);
        assertTrue(joined(next).contains("+100") || joined(next).contains("(+100"));
    }

    @Test
    void leftoverLinesAreUnknownUntilExtraSlot() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Purse";
        CustomScoreboardPolicy.ComposeResult withoutExtra = CustomScoreboardPolicy.compose(
                view(List.of("Purse: 3", "Weird Widget: 9"), List.of()),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        assertTrue(withoutExtra.unknownPlain().stream().anyMatch(line -> line.contains("Weird Widget")));
        settings.appearance = "Purse\nExtra";
        CustomScoreboardPolicy.ComposeResult withExtra = CustomScoreboardPolicy.compose(
                view(List.of("Purse: 3", "Weird Widget: 9"), List.of()),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        assertTrue(joined(withExtra).contains("Weird Widget"));
    }

    @Test
    void resetAppearanceAndEventsRestoreDefaults() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Purse";
        settings.eventPriority = "Spooky";
        settings.resetAppearance();
        settings.resetEvents();
        assertEquals(CustomScoreboardPolicy.defaultAppearanceText(), settings.appearance);
        assertEquals(CustomScoreboardPolicy.defaultEventText(), settings.eventPriority);
    }

    @Test
    void panelAlignUsesMarginOrPose() {
        assertEquals(10, CustomScoreboardPolicy.panelX(400, 80, CustomScoreboardPolicy.Align.LEFT, 10, 50));
        assertEquals(310, CustomScoreboardPolicy.panelX(400, 80, CustomScoreboardPolicy.Align.RIGHT, 10, 50));
        assertEquals(160, CustomScoreboardPolicy.panelX(400, 80, CustomScoreboardPolicy.Align.CENTER, 10, 50));
        assertEquals(50, CustomScoreboardPolicy.panelX(400, 80, CustomScoreboardPolicy.Align.DONT_ALIGN, 10, 50));
        assertEquals(12, CustomScoreboardPolicy.panelY(300, 40, CustomScoreboardPolicy.VAlign.DONT_ALIGN, 8, 12));
    }

    @Test
    void configRoundTripAndVanillaHide() {
        QolUtilityConfig qol = new QolUtilityConfig();
        assertFalse(qol.isModuleEnabled(CustomScoreboardPolicy.MODULE_ID));
        qol.setModuleEnabled(CustomScoreboardPolicy.MODULE_ID, true);
        assertTrue(qol.isModuleEnabled(CustomScoreboardPolicy.MODULE_ID));
        assertTrue(Boolean.TRUE.equals(qol.readBoolean("qol.custom_scoreboard.hide_vanilla")));
        assertTrue(qol.writeText("qol.custom_scoreboard.custom_title", "&&6Board"));
        assertEquals("&&6Board", qol.readText("qol.custom_scoreboard.custom_title"));
        assertTrue(qol.writeEnum("qol.custom_scoreboard.align_h", "Don't Align"));
        assertEquals("Don't Align", qol.readEnum("qol.custom_scoreboard.align_h"));
        qol.setPose(CustomScoreboardPolicy.POSE_ID, 40.0F, 90.0F);
        float[] pose = qol.pose(CustomScoreboardPolicy.POSE_ID);
        assertEquals(40.0F, pose[0], 0.01F);
        assertEquals(90.0F, pose[1], 0.01F);
        try {
            SkyBlockAreaDetector.updateSkyblockPresence(List.of("SKYBLOCK"));
            assertTrue(HudLayerHidePolicy.shouldHide(
                    HudLayerHidePolicy.Layer.SCOREBOARD, HudLayerHidePolicy.flags(qol)));
        } finally {
            SkyBlockAreaDetector.clearSkyblockPresence();
        }
        qol.resetModuleToDefaults(CustomScoreboardPolicy.MODULE_ID);
        assertFalse(qol.isModuleEnabled(CustomScoreboardPolicy.MODULE_ID));
    }

    private static CustomScoreboardPolicy.ComposeResult compose(List<String> sidebar, List<String> tab) {
        return CustomScoreboardPolicy.compose(
                view(sidebar, tab),
                new CustomScoreboardSettings().options(),
                new CustomScoreboardPolicy.DeltaBook());
    }

    private static CustomScoreboardPolicy.BoardView view(List<String> sidebar, List<String> tab) {
        return view(1_000L, sidebar, tab);
    }

    private static CustomScoreboardPolicy.BoardView view(long now, List<String> sidebar, List<String> tab) {
        return new CustomScoreboardPolicy.BoardView(
                true,
                false,
                "SKYBLOCK",
                sidebar,
                tab,
                "Hub",
                "",
                "Banana",
                "",
                OptionalLong.empty(),
                OptionalLong.empty(),
                Map.of(),
                now);
    }

    private static String joined(CustomScoreboardPolicy.ComposeResult result) {
        StringBuilder out = new StringBuilder();
        for (CustomScoreboardPolicy.Row row : result.rows()) {
            if (!out.isEmpty()) {
                out.append('\n');
            }
            out.append(row.text());
        }
        return out.toString();
    }
}
