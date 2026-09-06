package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class SkillLevelOverlayPolicyTest {
    @Test
    void inProgressSkillUsesLevelLine() {
        Optional<SkillLevelOverlayPolicy.Overlay> overlay = SkillLevelOverlayPolicy.parse(List.of(
                "Farming",
                "Level: 52",
                "Progress to Level 53: 12.4%"));
        assertTrue(overlay.isPresent());
        assertEquals(52, overlay.get().level());
        assertFalse(overlay.get().max());
    }

    @Test
    void maxLevelUsesSpecialColorFlag() {
        Optional<SkillLevelOverlayPolicy.Overlay> overlay = SkillLevelOverlayPolicy.parse(List.of(
                "Combat",
                "§7Level: §e60",
                "§6MAX LEVEL"));
        assertTrue(overlay.isPresent());
        assertEquals(60, overlay.get().level());
        assertTrue(overlay.get().max());
        assertEquals(
                SkillLevelOverlayPolicy.DEFAULT_MAX_COLOR,
                SkillLevelOverlayPolicy.colorFor(
                        true,
                        SkillLevelOverlayPolicy.DEFAULT_LEVEL_COLOR,
                        SkillLevelOverlayPolicy.DEFAULT_MAX_COLOR));
    }

    @Test
    void unlockRequirementLinesAreNotTheCurrentLevel() {
        Optional<SkillLevelOverlayPolicy.Overlay> overlay = SkillLevelOverlayPolicy.parse(List.of(
                "Alchemy",
                "Level: 50",
                "Unlocks at Combat Level 15",
                "MAXED OUT!"));
        assertTrue(overlay.isPresent());
        assertEquals(50, overlay.get().level());
        assertTrue(overlay.get().max());
    }

    @Test
    void navigationIconsWithoutLevelsAreSkipped() {
        assertTrue(SkillLevelOverlayPolicy.parse(List.of("Go Back", "Click to return")).isEmpty());
        assertTrue(SkillLevelOverlayPolicy.parse(List.of()).isEmpty());
    }

    @Test
    void hoverNameAndProgressLineAreFallbacks() {
        Optional<SkillLevelOverlayPolicy.Overlay> fromName =
                SkillLevelOverlayPolicy.parse("Combat 60", List.of("Click to view!"), 1);
        assertTrue(fromName.isPresent());
        assertEquals(60, fromName.get().level());
        assertTrue(fromName.get().max());

        Optional<SkillLevelOverlayPolicy.Overlay> fromProgress =
                SkillLevelOverlayPolicy.parse("Farming", List.of("Progress to Level 53: 12.4%"), 1);
        assertTrue(fromProgress.isPresent());
        assertEquals(52, fromProgress.get().level());
        assertFalse(fromProgress.get().max());

        Optional<SkillLevelOverlayPolicy.Overlay> fromCount =
                SkillLevelOverlayPolicy.parse("Mining", List.of(), 41);
        assertTrue(fromCount.isPresent());
        assertEquals(41, fromCount.get().level());
        assertFalse(fromCount.get().max());

        Optional<SkillLevelOverlayPolicy.Overlay> skillLevel =
                SkillLevelOverlayPolicy.parse("Farming", List.of("Skill Level: 47"), 1);
        assertTrue(skillLevel.isPresent());
        assertEquals(47, skillLevel.get().level());
        assertFalse(skillLevel.get().max());
    }

    @Test
    void maxLevelReachedAndCapUseMaxColor() {
        Optional<SkillLevelOverlayPolicy.Overlay> reached =
                SkillLevelOverlayPolicy.parse(
                        "Combat LX",
                        List.of("Click to view!", "§c§lMAX LEVEL REACHED"),
                        1);
        assertTrue(reached.isPresent());
        assertEquals(60, reached.get().level());
        assertTrue(reached.get().max());

        Optional<SkillLevelOverlayPolicy.Overlay> romanCap =
                SkillLevelOverlayPolicy.parse("Farming LX", List.of("Click to view!"), 1);
        assertTrue(romanCap.isPresent());
        assertEquals(60, romanCap.get().level());
        assertTrue(romanCap.get().max());

        Optional<SkillLevelOverlayPolicy.Overlay> foragingCap =
                SkillLevelOverlayPolicy.parse("Foraging LVII", List.of("Click to view!"), 1);
        assertTrue(foragingCap.isPresent());
        assertEquals(57, foragingCap.get().level());
        assertTrue(foragingCap.get().max());

        Optional<SkillLevelOverlayPolicy.Overlay> stillProgressing =
                SkillLevelOverlayPolicy.parse(
                        "Enchanting 60",
                        List.of("Progress to Level 61: 12.4%"),
                        1);
        assertTrue(stillProgressing.isPresent());
        assertEquals(60, stillProgressing.get().level());
        assertFalse(stillProgressing.get().max());

        Optional<SkillLevelOverlayPolicy.Overlay> progressMaxed =
                SkillLevelOverlayPolicy.parse(
                        "Mining",
                        List.of("Progress to Level 61: MAXED!"),
                        1);
        assertTrue(progressMaxed.isPresent());
        assertEquals(60, progressMaxed.get().level());
        assertTrue(progressMaxed.get().max());
    }

    @Test
    void colorForKeepsDigitsOpaque() {
        assertEquals(0xFFFF00AA, SkillLevelOverlayPolicy.colorFor(true, 0xFFFFFFFF, 0x00FF00AA));
        assertEquals(0xFFFFFFFF, SkillLevelOverlayPolicy.colorFor(false, 0xFFFFFFFF, 0xFF55FFFF));
    }

    @Test
    void levelDigitsUseATightBackgroundOnTheItem() {
        assertEquals(9, SkillLevelOverlayPolicy.labelX(0, 8));
        assertEquals(8, SkillLevelOverlayPolicy.labelY(0));
        SkillLevelOverlayPolicy.LabelBox box = SkillLevelOverlayPolicy.labelBackground(0, 0, 8);
        assertEquals(8, box.left());
        assertEquals(7, box.top());
        assertEquals(18, box.right());
        assertEquals(17, box.bottom());
    }
}
