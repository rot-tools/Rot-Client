package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Regressions found while auditing the HUD and Display modules. */
class HudDisplayRegressionTest {
    @Test
    void nameIsFoundRightAfterALegacyColourCode() {
        // Nametags are written "§aSteve"; the code letter must not count as part of the name.
        assertTrue(NameHiderPolicy.containsUsername("§aSteve", "Steve"));
        assertTrue(NameHiderPolicy.containsUsername("§7[MVP+] §bSteve§r", "Steve"));
        assertEquals("§a###", NameHiderPolicy.replaceUsername("§aSteve", "Steve", "###"));
    }

    @Test
    void nameStillRespectsRealWordBoundaries() {
        assertFalse(NameHiderPolicy.containsUsername("Stevens", "Steve"));
        assertFalse(NameHiderPolicy.containsUsername("xSteve", "Steve"));
        assertFalse(NameHiderPolicy.containsUsername("Steve_2", "Steve"));
        // A colour code between two words must not merge them either way.
        assertFalse(NameHiderPolicy.containsUsername("§aSteveish", "Steve"));
        assertTrue(NameHiderPolicy.containsUsername("hi, steve!", "Steve"));
    }

    @Test
    void statBarStripFormattingKeepsItsBehaviour() {
        assertEquals("Hello World", SkyBlockStatBarParser.stripFormatting("§aHello\u00A0 §cWorld"));
        assertEquals("a b", SkyBlockStatBarParser.stripFormatting("a\uE001  b"));
        assertEquals("", SkyBlockStatBarParser.stripFormatting(null));
    }

    @Test
    void actionBarFilterStillRemovesTheHiddenStat() {
        var filtered = SkyBlockStatBarParser.filterActionBar(
                "§c1,234/1,234❤     §a567§a❈ Defense     §b123/123✎ Mana",
                true, false, false, false, false, false, false);
        assertTrue(filtered.isPresent());
        assertFalse(filtered.get().contains("1,234"));
        assertTrue(filtered.get().contains("567"));
    }

    @Test
    void skillNameDetectionIsCaseInsensitiveAndUnchanged() {
        assertTrue(SkillLevelOverlayPolicy.isSkillName("§aFarming Skill"));
        assertTrue(SkillLevelOverlayPolicy.isSkillName("MINING"));
        assertFalse(SkillLevelOverlayPolicy.isSkillName("Go Back"));
        assertEquals(
                60,
                SkillLevelOverlayPolicy.parse(
                        "Farming",
                        List.of("§7Current Level: §a60", "§aMaxed"),
                        1).orElseThrow().level());
    }
}
