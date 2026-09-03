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

        Optional<SkillLevelOverlayPolicy.Overlay> fromProgress =
                SkillLevelOverlayPolicy.parse("Farming", List.of("Progress to Level 53: 12.4%"), 1);
        assertTrue(fromProgress.isPresent());
        assertEquals(52, fromProgress.get().level());

        Optional<SkillLevelOverlayPolicy.Overlay> fromCount =
                SkillLevelOverlayPolicy.parse("Mining", List.of(), 41);
        assertTrue(fromCount.isPresent());
        assertEquals(41, fromCount.get().level());

        Optional<SkillLevelOverlayPolicy.Overlay> skillLevel =
                SkillLevelOverlayPolicy.parse("Farming", List.of("Skill Level: 47"), 1);
        assertTrue(skillLevel.isPresent());
        assertEquals(47, skillLevel.get().level());
    }

    @Test
    void levelDigitsSitOnTheItemWithoutAFillBox() {
        assertEquals(9, SkillLevelOverlayPolicy.labelX(0, 8));
        assertEquals(8, SkillLevelOverlayPolicy.labelY(0));
    }
}
