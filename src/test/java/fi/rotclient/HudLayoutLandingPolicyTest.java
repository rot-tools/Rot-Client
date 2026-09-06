package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class HudLayoutLandingPolicyTest {
    @Test
    void groupsVanillaAndRotOverlaysAndKeepsCatalogAt130() {
        assertEquals(131, QolUtilityCatalog.modules().size());
        assertFalse(HudLayoutLandingPolicy.sections().isEmpty());
        assertTrue(HudLayoutLandingPolicy.sections().stream()
                .anyMatch(section -> "Vanilla / Hypixel".equals(section.title())));
        assertTrue(HudLayoutLandingPolicy.sections().stream()
                .anyMatch(section -> "Mining".equals(section.title())));
        assertTrue(HudLayoutLandingPolicy.rows().stream()
                .anyMatch(row -> MiningTrackerCatalogPolicy.TRACKER.equals(row.settingId())));
        assertTrue(HudLayoutLandingPolicy.rows().stream()
                .anyMatch(row -> "qol.performance_hud".equals(row.settingId()) && row.hasSettings()));
        assertTrue(HudLayoutLandingPolicy.rows().stream()
                .anyMatch(row -> row.settingId().startsWith("qol.hud_layout.hide_")
                        && !row.hasSettings()));
        assertEquals("HUD Elements Editor", HudLayoutLandingPolicy.TITLE);
        assertTrue(HudLayoutLandingPolicy.rows().stream()
                .anyMatch(row -> "qol.wardrobe_keybinds".equals(row.settingId())));
        assertTrue(HudLayoutLandingPolicy.rows().stream()
                .anyMatch(row -> "qol.iota.arrow_tracker".equals(row.settingId())));
        assertTrue(HudLayoutLandingPolicy.rows().stream()
                .anyMatch(row -> "qol.custom_scoreboard".equals(row.settingId())));
        assertTrue(HudLayoutLandingPolicy.sections().stream()
                .anyMatch(section -> "GUI".equals(section.title())));
        assertTrue(HudLayoutLandingPolicy.contentHeight() > 200);
        assertEquals(
                HudLayoutLandingPolicy.Action.NONE,
                HudLayoutLandingPolicy.hit(
                        200,
                        HudLayoutLandingPolicy.headerHeight() + 2,
                        0,
                        0,
                        400,
                        0).action());
    }

    @Test
    void hitTestFindsBackEditorToggleAndSettings() {
        assertEquals(
                HudLayoutLandingPolicy.Action.BACK,
                HudLayoutLandingPolicy.hit(10, 10, 0, 0, 400, 0).action());
        HudLayoutLandingPolicy.Hit editor = HudLayoutLandingPolicy.hit(
                20, HudLayoutLandingPolicy.EDITOR_TOP + 4, 0, 0, 400, 0);
        assertEquals(HudLayoutLandingPolicy.Action.OPEN_EDITOR, editor.action());
        assertEquals(
                HudLayoutLandingPolicy.Action.OPEN_EDITOR,
                HudLayoutLandingPolicy.hit(
                        20, HudLayoutLandingPolicy.EDITOR_TOP + 4, 0, 0, 400, 80).action());
        int miningIndex = -1;
        java.util.List<HudLayoutLandingPolicy.Row> rows = HudLayoutLandingPolicy.rows();
        for (int i = 0; i < rows.size(); i++) {
            if (MiningTrackerCatalogPolicy.TRACKER.equals(rows.get(i).settingId())) {
                miningIndex = i;
                break;
            }
        }
        assertTrue(miningIndex >= 0);
        HudLayoutLandingPolicy.Hit settings = HudLayoutLandingPolicy.hit(
                400 - HudLayoutLandingPolicy.TOGGLE_WIDTH - 20
                        - HudLayoutLandingPolicy.SETTINGS_WIDTH,
                rowY(miningIndex) + 8,
                0,
                0,
                400,
                0);
        assertEquals(HudLayoutLandingPolicy.Action.SETTINGS, settings.action());
        assertEquals(miningIndex, settings.rowIndex());
    }

    @Test
    void poseDisableMapsOverlaysAndSkipsUnknown() {
        assertEquals(
                "qol.performance_hud",
                HudLayoutLandingPolicy.disableForPose("performance").settingId());
        assertTrue(HudLayoutLandingPolicy.disableForPose("performance").moduleToggle());
        assertEquals(
                "qol.player_display.health_hud",
                HudLayoutLandingPolicy.disableForPose("health").settingId());
        assertNotNull(HudLayoutLandingPolicy.disableForPose("mining_tracker"));
        assertEquals("qol.wardrobe_keybinds",
                HudLayoutLandingPolicy.disableForPose("wardrobe").settingId());
        assertTrue(HudLayoutLandingPolicy.disableForPose("wardrobe").moduleToggle());
        assertEquals("qol.iota.arrow_tracker",
                HudLayoutLandingPolicy.disableForPose("iota_arrows").settingId());
        assertEquals("qol.iota",
                HudLayoutLandingPolicy.disableForPose("kuudra_alerts").settingId());
        for (String poseId : new String[] {
                "performance", "health", "mana", "overflow", "defense", "vitality", "ehp",
                "speed", "pet", "commission", "wardrobe", "auto_clicker", "fishing", "mining",
                "diana", "foraging", "iota_arrows", "kuudra_alerts", "stall_bin", "slayer",
                "slayer_progress", "slayer_rng", "slayer_profit", "slayer_stats", "slayer_carry",
                "slayer_cocoon", "slayer_attunement", "slayer_vengeance", "dungeon",
                "dungeon_carry", "dungeon_watcher", "custom_scoreboard"
        }) {
            assertNotNull(HudLayoutLandingPolicy.disableForPose(poseId), poseId);
        }
        assertEquals(
                HudLayoutLandingPolicy.Action.NONE,
                HudLayoutLandingPolicy.hit(-4, 10, 0, 0, 400, 0).action());
    }

    private static int rowY(int index) {
        int y = HudLayoutLandingPolicy.headerHeight()
                + HudLayoutLandingPolicy.SECTION_GAP;
        int seen = 0;
        for (HudLayoutLandingPolicy.Section section : HudLayoutLandingPolicy.sections()) {
            y += HudLayoutLandingPolicy.SECTION_LABEL_HEIGHT;
            for (int i = 0; i < section.rows().size(); i++) {
                if (seen == index) {
                    return y;
                }
                y += HudLayoutLandingPolicy.ROW_HEIGHT + HudLayoutLandingPolicy.ROW_GAP;
                seen++;
            }
            y += 8;
        }
        return y;
    }
}
