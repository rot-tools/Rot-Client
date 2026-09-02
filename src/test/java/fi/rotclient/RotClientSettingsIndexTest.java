package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class RotClientSettingsIndexTest {
    @Test
    void catalogIsCachedAndIncludesHudToggles() {
        assertTrue(RotClientSettingsIndex.catalogSize() >= 23);
        assertTrue(RotClientSettingsIndex.catalog()
                == RotClientSettingsIndex.catalog());
        int hudToggles = 0;
        for (RotClientSettingsIndex.Entry entry
                : RotClientSettingsIndex.catalog()) {
            if (RotClientSettingsIndex.isHudVisibilityToggle(entry.id())) {
                hudToggles++;
            }
        }
        assertEquals(23, hudToggles);
    }

    @Test
    void blankQueryReturnsNoResults() {
        assertTrue(RotClientSettingsIndex.search("").isEmpty());
        assertTrue(RotClientSettingsIndex.search("   ").isEmpty());
        assertTrue(RotClientSettingsIndex.search(null).isEmpty());
    }

    @Test
    void whitespaceNormalizedAndCaseInsensitive() {
        List<RotClientSettingsIndex.Entry> a =
                RotClientSettingsIndex.search("Auto   Pause");
        List<RotClientSettingsIndex.Entry> b =
                RotClientSettingsIndex.search("auto pause");
        assertFalse(a.isEmpty());
        assertEquals(a.get(0).id(), b.get(0).id());
    }

    @Test
    void requiredSearchTermsResolve() {
        String[] terms = {
                "graph", "other", "background", "reset", "session", "hud",
                "analytics", "history", "appearance", "price", "bazaar",
                "tool", "fortune", "auto pause", "mob", "chest", "area",
                "location"
        };
        for (String term : terms) {
            List<RotClientSettingsIndex.Entry> results =
                    RotClientSettingsIndex.search(term);
            assertFalse(results.isEmpty(), "missing results for: " + term);
            Set<String> ids = new HashSet<>();
            for (RotClientSettingsIndex.Entry entry : results) {
                assertTrue(ids.add(entry.id()), "duplicate id for " + term);
                assertFalse(entry.description().isBlank());
                assertNotNull(entry.destination());
            }
        }
    }

    @Test
    void searchIsDeterministic() {
        List<RotClientSettingsIndex.Entry> first =
                RotClientSettingsIndex.search("hud");
        List<RotClientSettingsIndex.Entry> second =
                RotClientSettingsIndex.search("hud");
        assertEquals(first, second);
    }

    @Test
    void autoSprintIsSearchable() {
        List<RotClientSettingsIndex.Entry> results =
                RotClientSettingsIndex.search("auto sprint");
        assertFalse(results.isEmpty());
        assertEquals("qol.auto_sprint", results.get(0).id());
        assertEquals(
                RotClientSettingsIndex.Destination.QOL_SETTINGS,
                results.get(0).destination());
    }

    @Test
    void cameraIsSearchable() {
        List<RotClientSettingsIndex.Entry> results =
                RotClientSettingsIndex.search("third person");
        assertFalse(results.isEmpty());
        assertEquals("qol.camera", results.get(0).id());
        assertEquals(
                RotClientSettingsIndex.Destination.QOL_SETTINGS,
                results.get(0).destination());
    }

    @Test
    void utilityChildSettingsSurfaceParentModules() {
        assertEquals(
                "qol.render_optimizer",
                QolUtilityCatalog.findById(
                        RotClientSettingsIndex.search("lightning").get(0).id()).id());
        assertEquals(
                "qol.no_cursor_reset",
                QolUtilityCatalog.findById(
                        RotClientSettingsIndex.search("cursor").get(0).id()).id());
        assertEquals(
                "qol.pet_keybinds",
                QolUtilityCatalog.findById(
                        RotClientSettingsIndex.search("pet keybinds").get(0).id()).id());
        assertEquals(
                "qol.performance_hud",
                QolUtilityCatalog.findById(
                        RotClientSettingsIndex.search("show fps").get(0).id()).id());
        assertEquals(
                "qol.camera",
                QolUtilityCatalog.findById(
                        RotClientSettingsIndex.search("third person").get(0).id()).id());
    }

    @Test
    void editHudAliasDeepLinksToExistingEditor() {
        List<RotClientSettingsIndex.Entry> results =
                RotClientSettingsIndex.search("drag hud");
        assertFalse(results.isEmpty());
        assertEquals("hud.edit_position", results.get(0).id());
        assertEquals(
                RotClientSettingsIndex.Destination.HUD_EDITOR,
                results.get(0).destination());
    }

    @Test
    void findByIdIsStable() {
        RotClientSettingsIndex.Entry entry =
                RotClientSettingsIndex.findById("showRateGraph");
        assertNotNull(entry);
        assertEquals("Rate Graph", entry.label());
        assertTrue(entry.matches("graph"));
    }

    @Test
    void searchRespectsLimit() {
        List<RotClientSettingsIndex.Entry> results =
                RotClientSettingsIndex.search("a", 3);
        assertTrue(results.size() <= 3);
    }
}
