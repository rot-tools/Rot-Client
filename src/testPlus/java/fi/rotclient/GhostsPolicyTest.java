package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GhostsPolicyTest {
    @Test
    void mistCreepersSitBelowTheDwarvenCeiling() {
        assertTrue(GhostsPolicy.isMistCreeper(true, true, 42.0D));
        assertTrue(GhostsPolicy.isMistCreeper(true, true, 99.9D));
        assertFalse(GhostsPolicy.isMistCreeper(true, true, 100.0D));
        assertFalse(GhostsPolicy.isMistCreeper(true, false, 20.0D));
        assertFalse(GhostsPolicy.isMistCreeper(false, true, 20.0D));
        assertFalse(GhostsPolicy.isMistCreeper(true, true, Double.NaN));
        assertTrue(GhostsPolicy.boardLooksLikeGhostIsland("Area: Dwarven Mines"));
        assertTrue(GhostsPolicy.boardLooksLikeGhostIsland("⏣ The Mist"));
        assertFalse(GhostsPolicy.boardLooksLikeGhostIsland("Area: Hub"));
    }

    @Test
    void visibilityOnlyAppliesInsideTheMist() {
        GhostsPolicy.Decision outside = GhostsPolicy.decide(
                true, true, false, true, true, false, 20.0D);
        assertEquals(GhostsPolicy.Decision.NONE, outside);
        GhostsPolicy.Decision high = GhostsPolicy.decide(
                true, true, false, true, true, true, 140.0D);
        assertEquals(GhostsPolicy.Decision.NONE, high);
        GhostsPolicy.Decision mist = GhostsPolicy.decide(
                true, true, false, true, true, true, 40.0D);
        assertTrue(mist.forceVisible());
        assertTrue(mist.hidePoweredLayer());
    }

    @Test
    void highlightStylesMapToFillAndStroke() {
        assertEquals("Both", GhostsPolicy.normalizeHighlight(null));
        assertEquals("Both", GhostsPolicy.normalizeHighlight("filled outline"));
        assertEquals("Filled", GhostsPolicy.normalizeHighlight("fill"));
        assertEquals("Outline", GhostsPolicy.normalizeHighlight("STROKE"));
        GhostsPolicy.BoxPaint both = GhostsPolicy.boxPaint("Both");
        assertTrue(both.fill());
        assertTrue(both.stroke());
        GhostsPolicy.BoxPaint filled = GhostsPolicy.boxPaint("Filled");
        assertTrue(filled.fill());
        assertFalse(filled.stroke());
        assertEquals(0.01F, GhostsPolicy.strokeWidth(filled), 0.0001F);
        GhostsPolicy.BoxPaint outline = GhostsPolicy.boxPaint("Outline");
        assertFalse(outline.fill());
        assertTrue(outline.stroke());
        assertEquals(3.0F, GhostsPolicy.strokeWidth(outline), 0.0001F);
        assertEquals(0xFF00C8C8, GhostsPolicy.strokeArgb(outline, 0xFF00C8C8, 0x7F00C8C8));
        assertEquals(0x00000000, GhostsPolicy.fillArgb(outline, 0x7F00C8C8));
        assertEquals(0x7F00C8C8, GhostsPolicy.fillArgb(filled, 0x7F00C8C8));
    }

    @Test
    void extrasPersistHighlightStyleAndColors() {
        QolUtilityConfig config = new QolUtilityConfig();
        config.setModuleEnabled("qol.ghosts", true);
        assertEquals("Both", config.readEnum("qol.ghosts.highlight_style"));
        assertEquals(GhostsPolicy.DEFAULT_FILL, config.readColor("qol.ghosts.fill_color"));
        assertEquals(GhostsPolicy.DEFAULT_OUTLINE, config.readColor("qol.ghosts.outline_color"));
        assertTrue(config.writeEnum("qol.ghosts.highlight_style", "Outline"));
        assertEquals("Outline", config.readEnum("qol.ghosts.highlight_style"));
        assertTrue(config.writeColor("qol.ghosts.fill_color", 0x80112233));
        assertTrue(config.writeColor("qol.ghosts.outline_color", 0xFFAABBCC));
        assertEquals(0x80112233, config.readColor("qol.ghosts.fill_color"));
        assertEquals(0xFFAABBCC, config.readColor("qol.ghosts.outline_color"));
        assertTrue(config.resetModuleToDefaults("qol.ghosts"));
        assertFalse(config.isModuleEnabled("qol.ghosts"));
        assertFalse(config.extras().ghostsShowPowered);
        assertEquals("Both", config.readEnum("qol.ghosts.highlight_style"));
        assertEquals(GhostsPolicy.DEFAULT_FILL, config.readColor("qol.ghosts.fill_color"));
        assertEquals(GhostsPolicy.DEFAULT_OUTLINE, config.readColor("qol.ghosts.outline_color"));
    }

    @Test
    void catalogExposesHighlightSettingsOnGhosts() {
        QolUtilityCatalog.ModuleDef ghosts = QolUtilityCatalog.findById("qol.ghosts");
        assertNotNull(ghosts);
        java.util.Set<String> ids = ghosts.settings().stream()
                .map(QolUtilityCatalog.SettingDef::id)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(ids.contains("qol.ghosts.highlight_style"));
        assertTrue(ids.contains("qol.ghosts.fill_color"));
        assertTrue(ids.contains("qol.ghosts.outline_color"));
        assertTrue(ids.contains("qol.ghosts.show_ghosts"));
        assertEquals(QolUtilityCatalog.SettingType.ENUM,
                ghosts.settings().stream()
                        .filter(setting -> setting.id().equals("qol.ghosts.highlight_style"))
                        .findFirst()
                        .orElseThrow()
                        .type());
        assertEquals(GhostsPolicy.HIGHLIGHT_STYLES,
                ghosts.settings().stream()
                        .filter(setting -> setting.id().equals("qol.ghosts.highlight_style"))
                        .findFirst()
                        .orElseThrow()
                        .enumOptions());
    }
}
