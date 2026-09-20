package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

/** Regressions found while auditing the Custom Scoreboard module. */
final class CustomScoreboardRegressionTest {
    @Test
    void sidebarEntriesAreOrderedLikeVanillaNotByArrivalOrder() {
        List<CustomScoreboardLines.SidebarEntry> arrived = List.of(
                new CustomScoreboardLines.SidebarEntry("b", 3, "third"),
                new CustomScoreboardLines.SidebarEntry("z", 4, "second-z"),
                new CustomScoreboardLines.SidebarEntry("a", 5, "first"),
                new CustomScoreboardLines.SidebarEntry("C", 4, "second-c"));

        assertEquals(
                List.of("first", "second-c", "second-z", "third"),
                CustomScoreboardLines.orderSidebar(arrived));
    }

    @Test
    void sidebarKeepsOnlyTheFifteenHighestScores() {
        List<CustomScoreboardLines.SidebarEntry> entries = new ArrayList<>();
        for (int score = 1; score <= 20; score++) {
            entries.add(new CustomScoreboardLines.SidebarEntry("o" + score, score, "line" + score));
        }

        List<String> ordered = CustomScoreboardLines.orderSidebar(entries);

        assertEquals(15, ordered.size());
        assertEquals("line20", ordered.get(0));
        assertEquals("line6", ordered.get(14));
    }

    @Test
    void slayerQuestLinesFillTheSlayerSlot() {
        assertEquals(
                CustomScoreboardLines.Kind.SLAYER,
                CustomScoreboardLines.classify("§eSlayer Quest").kind());

        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Slayer";
        CustomScoreboardPolicy.ComposeResult result = compose(
                settings,
                "Hub",
                "",
                List.of("§eSlayer Quest", "§5Voidgloom Seraph III", "§eSlay the boss!"));

        assertEquals(
                List.of("Slayer Quest", "Voidgloom Seraph III", "Slay the boss!"),
                plains(result));
    }

    @Test
    void objectiveLinesStillFillTheObjectiveSlot() {
        assertEquals(
                CustomScoreboardLines.Kind.OBJECTIVE,
                CustomScoreboardLines.classify("§eObjective").kind());
        assertEquals(
                CustomScoreboardLines.Kind.OBJECTIVE,
                CustomScoreboardLines.classify("§eQuest: Foo").kind());

        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Objective";
        CustomScoreboardPolicy.ComposeResult result = compose(
                settings,
                "Hub",
                "",
                List.of("§eObjective", "§cKill 10 zombies"));

        assertEquals(List.of("Objective", "Kill 10 zombies"), plains(result));
    }

    @Test
    void heatReadingsThatEndInZeroAreNotTreatedAsEmpty() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Heat";

        for (String reading : List.of("§cHeat: §c♨ 10", "§cHeat: §c♨ 20", "§cHeat: §c♨ 100")) {
            assertEquals(
                    1,
                    compose(settings, "Crystal Hollows", "", List.of(reading)).rows().size(),
                    reading);
        }
        assertTrue(
                compose(settings, "Crystal Hollows", "", List.of("§cHeat: §c♨ 0")).rows().isEmpty());
    }

    @Test
    void coldReadingsThatEndInZeroAreNotTreatedAsEmpty() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Cold";

        for (String reading : List.of("§bCold: §b-10❄", "§bCold: §b-100❄", "§bCold: §b-5❄")) {
            assertEquals(
                    1,
                    compose(settings, "Glacite Mineshaft", "", List.of(reading)).rows().size(),
                    reading);
        }
        assertTrue(
                compose(settings, "Glacite Mineshaft", "", List.of("§bCold: §b0❄")).rows().isEmpty());
    }

    @Test
    void dungeonHubStillShowsBitsButRealDungeonsDoNot() {
        CustomScoreboardSettings settings = new CustomScoreboardSettings();
        settings.appearance = "Bits";
        List<String> sidebar = List.of("§bBits: §b500");

        assertTrue(plains(compose(settings, "Dungeon Hub", "⏣ Dungeon Hub", sidebar)).stream()
                .anyMatch(line -> line.contains("Bits")));
        assertTrue(
                compose(settings, "Dungeon", "⏣ The Catacombs (F7)", sidebar).rows().isEmpty());
    }

    @Test
    void soloDungeonLineIsADungeonEvent() {
        assertEquals(
                CustomScoreboardPolicy.EventKind.DUNGEONS,
                CustomScoreboardLines.eventKind("§3§lSolo", "Solo"));
        assertNotEquals(
                CustomScoreboardPolicy.EventKind.DUNGEONS,
                CustomScoreboardLines.eventKind("§7Solo mission", "Solo mission"));
    }

    @Test
    void compactNumbersRoundUpAcrossUnitBoundaries() {
        assertEquals("999", compact(999));
        assertEquals("1.5k", compact(1_500));
        assertEquals("999.9k", compact(999_949));
        assertEquals("1M", compact(999_960));
        assertEquals("1M", compact(1_000_000));
        assertEquals("1B", compact(999_999_999));
        assertEquals("-1.5k", compact(-1_500));
    }

    private static String compact(long value) {
        return CustomScoreboardPolicy.formatNumber(value, CustomScoreboardPolicy.NumberStyle.COMPACT);
    }

    private static List<String> plains(CustomScoreboardPolicy.ComposeResult result) {
        return result.rows().stream().map(CustomScoreboardPolicy.Row::plain).toList();
    }

    private static CustomScoreboardPolicy.ComposeResult compose(
            CustomScoreboardSettings settings,
            String island,
            String location,
            List<String> sidebar) {
        return CustomScoreboardPolicy.compose(
                new CustomScoreboardPolicy.BoardView(
                        true,
                        false,
                        "SKYBLOCK",
                        sidebar,
                        List.of(),
                        island,
                        location,
                        "Banana",
                        "",
                        OptionalLong.empty(),
                        OptionalLong.empty(),
                        false,
                        1_000L),
                settings.options(),
                new CustomScoreboardPolicy.DeltaBook());
    }
}
