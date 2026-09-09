package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalDouble;
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

    @Test
    void powderUsesHeaderAndIslandTint() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Powder";
        settings.hideIrrelevant = false;
        CustomScoreboardPolicy.ComposeResult result = CustomScoreboardPolicy.compose(
                new CustomScoreboardPolicy.BoardView(
                        true,
                        false,
                        "SKYBLOCK",
                        List.of("§2᠅ §2Mithril Powder: §254,646"),
                        List.of(),
                        "Dwarven Mines",
                        "Dwarven Village",
                        "Banana",
                        "",
                        OptionalLong.empty(),
                        OptionalLong.empty(),
                        false,
                        1_000L),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        String text = joined(result);
        assertTrue(text.contains("§9§lPowder"));
        assertTrue(text.contains("Mithril"));
        assertTrue(text.contains("54,646"));
    }

    @Test
    void chunkedStatsAreCurrenciesNotHealth() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Chunked Stats";
        settings.chunkedStats = "Purse\nBits";
        settings.hideEmpty = false;
        CustomScoreboardPolicy.ComposeResult result = CustomScoreboardPolicy.compose(
                view(List.of("§6Purse: §61,000", "§bBits: §b20"), List.of()),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        String text = joined(result);
        assertTrue(text.contains("1,000"));
        assertTrue(text.contains("20"));
        assertTrue(text.contains("§f|"));
        assertFalse(text.toLowerCase().contains("health"));
    }

    @Test
    void locationSlotKeepsYourIsland() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Location";
        settings.hideEmpty = true;
        List<String> sidebar = List.of("Your Island");
        assertEquals(
                CustomScoreboardLines.Kind.LOCATION,
                CustomScoreboardLines.classify(sidebar.get(0)).kind());
        CustomScoreboardPolicy.ComposeResult result = CustomScoreboardPolicy.compose(
                view(sidebar, List.of()),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        assertEquals(
                List.of("Your Island"),
                result.rows().stream().map(CustomScoreboardPolicy.Row::plain).toList(),
                "unknown=" + result.unknownPlain());
    }

    @Test
    void chunkedHealthDefenseManaSpeedDoNotFallBackToBankBits() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Bank\nBits\nChunked Stats\nLocation\nFooter";
        settings.chunkedStats = "Health\nDefense\nMana\nSpeed";
        settings.hideEmpty = true;
        SkyBlockStatBarParser.Stats combat = new SkyBlockStatBarParser.Stats(
                OptionalDouble.of(12_345),
                OptionalDouble.of(15_000),
                OptionalDouble.of(800),
                OptionalDouble.of(400),
                OptionalDouble.of(500),
                OptionalDouble.empty(),
                OptionalDouble.of(117),
                OptionalDouble.empty());
        CustomScoreboardPolicy.ComposeResult result = CustomScoreboardPolicy.compose(
                new CustomScoreboardPolicy.BoardView(
                        true,
                        false,
                        "SKYBLOCK",
                        List.of("§6Bank: §6122", "§bBits: §b82,540", "Your Island", "www.hypixel.net"),
                        List.of(),
                        "",
                        "",
                        "",
                        "",
                        OptionalLong.empty(),
                        OptionalLong.empty(),
                        false,
                        1_000L,
                        combat),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        String text = joined(result);
        List<String> plains = result.rows().stream().map(CustomScoreboardPolicy.Row::plain).toList();
        assertTrue(text.contains("Bank"));
        assertTrue(text.contains("Bits"));
        assertTrue(text.contains("12,345"));
        assertTrue(text.contains("800"));
        assertTrue(text.contains("400"));
        assertTrue(text.contains("117"));
        assertFalse(plains.stream().anyMatch(
                line -> line.contains("|") && line.contains("122") && line.contains("82,540")));
        assertTrue(plains.stream().anyMatch(line -> line.contains("Your Island")), plains.toString());
        assertEquals(1, plains.stream().filter(line -> line.toLowerCase().contains("hypixel.net")).count(), plains.toString());
        assertTrue(result.unknownPlain().isEmpty());
    }

    @Test
    void classifiesIslandAndFooterLines() {
        assertEquals(
                CustomScoreboardLines.Kind.LOCATION,
                CustomScoreboardLines.classify("📍 Your Island").kind());
        assertEquals(
                CustomScoreboardLines.Kind.LOCATION,
                CustomScoreboardLines.classify("Your Island").kind());
        assertEquals(
                CustomScoreboardLines.Kind.SKIP,
                CustomScoreboardLines.classify("www.hypixel.net").kind());
    }

    @Test
    void unknownChunkedNamesStayEmptyInsteadOfDefaultCurrencies() {
        assertEquals(
                List.of(
                        CustomScoreboardPolicy.ChunkStat.HEALTH,
                        CustomScoreboardPolicy.ChunkStat.DEFENSE,
                        CustomScoreboardPolicy.ChunkStat.MANA,
                        CustomScoreboardPolicy.ChunkStat.SPEED),
                CustomScoreboardPolicy.parseChunked("Health\nDefense\nMana\nSpeed"));
        assertTrue(CustomScoreboardPolicy.parseChunked("Not A Stat").isEmpty());
    }

    @Test
    void hideEmptySkipsZeroPurse() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Purse";
        settings.hideEmpty = true;
        CustomScoreboardPolicy.ComposeResult hidden = CustomScoreboardPolicy.compose(
                view(List.of("§6Purse: §60"), List.of()),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        assertFalse(joined(hidden).contains("Purse"));
        settings.hideEmpty = false;
        CustomScoreboardPolicy.ComposeResult shown = CustomScoreboardPolicy.compose(
                view(List.of("§6Purse: §60"), List.of()),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        assertTrue(joined(shown).contains("Purse"));
    }

    @Test
    void jacobContestKeepsFollowLines() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Events";
        settings.eventPriority = "Jacob Contest";
        CustomScoreboardPolicy.ComposeResult result = CustomScoreboardPolicy.compose(
                view(List.of("§eJacob's Contest", "§aWheat", "§eEnds in 12m"), List.of()),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        String text = joined(result);
        assertTrue(text.contains("Jacob's Contest"));
        assertTrue(text.contains("Wheat"));
    }

    @Test
    void motesStayOffTheBoardOutsideTheRift() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Motes";
        settings.hideEmpty = false;
        CustomScoreboardPolicy.ComposeResult result = CustomScoreboardPolicy.compose(
                view(List.of("§dMotes: §d12,000"), List.of()),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        assertFalse(joined(result).contains("Motes"));
    }

    @Test
    void legacyCodesKeepGoldAndRgbWithoutLeavingX() {
        assertEquals("Purse: 10", LegacyMcText.strip("§6Purse: §610"));
        assertEquals("Purse: 10", LegacyMcText.strip("§x§f§f§a§a§0§0Purse: 10"));
        assertEquals("§6", LegacyMcText.encodeColor(0xFFAA00));
        assertEquals("§x§1§2§3§4§5§6", LegacyMcText.encodeColor(0x123456));
        List<LegacyMcText.Span> gold = LegacyMcText.parse("§6§lSKYBLOCK");
        assertEquals(1, gold.size());
        assertEquals("SKYBLOCK", gold.get(0).text());
        assertEquals(0xFFAA00, gold.get(0).rgb());
        assertTrue(gold.get(0).bold());
        List<LegacyMcText.Span> rgb = LegacyMcText.parse("§x§1§2§3§4§5§6Hi");
        assertEquals(0x123456, rgb.get(0).rgb());
        assertEquals("Hi", rgb.get(0).text());
        CustomScoreboardLines.Hit hit = CustomScoreboardLines.classify(
                "§x§f§f§a§a§0§0Purse: §x§f§f§a§a§0§01,000");
        assertEquals(CustomScoreboardLines.Kind.PURSE, hit.kind());
        assertEquals("1,000", hit.capture());
    }

    @Test
    void tabGemsFillTheGemsSlot() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Gems";
        settings.hideEmpty = true;
        CustomScoreboardPolicy.ComposeResult result = CustomScoreboardPolicy.compose(
                view(List.of("§6Purse: §61,000"), List.of("Community Shop", "Gems: 12")),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        assertTrue(joined(result).contains("12"), joined(result));
        assertTrue(joined(result).toLowerCase().contains("gems"), joined(result));
    }

    @Test
    void unclaimedBitsComeFromTabAliases() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Bits";
        settings.showUnclaimedBits = true;
        settings.hideEmpty = true;
        CustomScoreboardPolicy.ComposeResult result = CustomScoreboardPolicy.compose(
                view(List.of("§bBits: §b82,540"), List.of("Unclaimed: 400")),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        String text = joined(result);
        assertTrue(text.contains("82,540"), text);
        assertTrue(text.contains("400"), text);
    }

    @Test
    void mayorPerksMinisterAndElectionComeFromTabWidgets() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Mayor";
        settings.showMayorPerks = true;
        settings.showMayorTime = true;
        settings.showExtraMayor = true;
        settings.hideEmpty = true;
        List<String> tab = List.of(
                "Mayor",
                "Aatrox",
                "Slayer XP Buff",
                "Minister",
                "Cole",
                "Mining Fiesta",
                "Election",
                "3d 12h");
        CustomScoreboardPolicy.ComposeResult result = CustomScoreboardPolicy.compose(
                view(List.of("§6Purse: §610"), tab),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        String text = joined(result);
        assertTrue(text.contains("Aatrox"), text);
        assertTrue(text.contains("Slayer XP Buff"), text);
        assertTrue(text.contains("3d 12h"), text);
        assertTrue(text.contains("Cole"), text);
        assertTrue(text.contains("Mining Fiesta"), text);
    }

    @Test
    void magicalPowerAndCompactTuningsComeFromTabSection() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Tuning";
        settings.showMagicalPower = true;
        settings.compactTuning = true;
        settings.hideEmpty = true;
        List<String> tab = List.of(
                "Magical Power",
                "1,420",
                "Health: +50",
                "Critical Damage: +12");
        CustomScoreboardPolicy.ComposeResult result = CustomScoreboardPolicy.compose(
                view(List.of(), tab),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        String text = joined(result);
        assertTrue(text.contains("1,420") || text.contains("1420"), text);
        assertTrue(text.contains("+50"), text);
        assertTrue(text.contains("+12"), text);
        assertTrue(text.contains("§7,") || text.contains(", "), text);
    }

    @Test
    void tabEventsShowWhenShowAllActiveIsOn() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Events";
        settings.eventPriority = "Dark Auction\nSpooky";
        settings.showAllEvents = true;
        settings.hideEmpty = true;
        CustomScoreboardPolicy.ComposeResult result = CustomScoreboardPolicy.compose(
                view(
                        List.of("Dark Auction in 5m"),
                        List.of("Event", "Spooky Festival")),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        String text = joined(result);
        assertTrue(text.contains("Dark Auction"), text);
        assertTrue(text.contains("Spooky"), text);
    }

    @Test
    void exactMinutesUseComputedSkyBlockClock() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Time";
        settings.timeExact = true;
        settings.hideEmpty = true;
        long now = SkyBlockClock.EPOCH_MILLIS + 6_667L;
        CustomScoreboardPolicy.ComposeResult exact = CustomScoreboardPolicy.compose(
                view(now, List.of(), List.of()),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        assertTrue(joined(exact).contains(":08"), joined(exact));
        settings.timeExact = false;
        CustomScoreboardPolicy.ComposeResult rounded = CustomScoreboardPolicy.compose(
                view(now, List.of(), List.of()),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
        assertTrue(joined(rounded).contains(":00"), joined(rounded));
        assertFalse(joined(rounded).contains(":08"), joined(rounded));
    }

    @Test
    void bankNumberDiffSuffixLastsFiveSeconds() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Bank";
        settings.showDiff = true;
        settings.hideEmpty = false;
        CustomScoreboardPolicy.DeltaBook deltas = new CustomScoreboardPolicy.DeltaBook();
        CustomScoreboardPolicy.compose(
                view(1_000L, List.of("Bank: 100"), List.of()),
                settings.options(),
                deltas);
        CustomScoreboardPolicy.ComposeResult next = CustomScoreboardPolicy.compose(
                view(1_100L, List.of("Bank: 200"), List.of()),
                settings.options(),
                deltas);
        assertTrue(joined(next).contains("+100") || joined(next).contains("(+100"), joined(next));
    }

    @Test
    void listTextSettingsHaveChoicesAndJoinRoundTrip() {
        assertTrue(CustomScoreboardPolicy.isListTextSetting("qol.custom_scoreboard.appearance"));
        assertTrue(CustomScoreboardPolicy.listChoices("qol.custom_scoreboard.appearance")
                .contains("Purse"));
        assertEquals(
                "Purse\nBits",
                CustomScoreboardPolicy.joinListText(List.of("Purse", "Bits")));
        assertEquals(
                List.of("Health", "Mana"),
                CustomScoreboardPolicy.splitListText("Health\nMana"));
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
                false,
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
