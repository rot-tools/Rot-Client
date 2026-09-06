package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerQolCatalogTest {
    @Test
    void slayerDropsExposesAnOptInLivePriceChatSetting() {
        QolUtilityCatalog.ModuleDef drops = QolUtilityCatalog.findById("qol.slayer_drops");

        assertNotNull(drops);
        assertTrue(drops.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.slayer_drops.price_in_chat")
                        && setting.type() == QolUtilityCatalog.SettingType.TOGGLE));
    }

    @Test
    void slayerDropsExposesVerifiedChanceVisibility() {
        QolUtilityCatalog.ModuleDef drops = QolUtilityCatalog.findById("qol.slayer_drops");

        assertNotNull(drops);
        assertTrue(drops.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.slayer_drops.show_chance")
                        && setting.type() == QolUtilityCatalog.SettingType.TOGGLE));
    }

    @Test
    void slayerDropsExposesRecentHudHighlightSetting() {
        QolUtilityCatalog.ModuleDef drops = QolUtilityCatalog.findById("qol.slayer_drops");

        assertNotNull(drops);
        assertTrue(drops.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.slayer_drops.recent_highlight")
                        && setting.type() == QolUtilityCatalog.SettingType.TOGGLE));
    }

    @Test
    void slayerDropsExposesOptionalGroundLabels() {
        QolUtilityCatalog.ModuleDef drops = QolUtilityCatalog.findById("qol.slayer_drops");

        assertNotNull(drops);
        assertTrue(drops.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.slayer_drops.ground_labels")
                        && setting.type() == QolUtilityCatalog.SettingType.TOGGLE));
    }

    @Test
    void slayerDropsExposesAConfigurableHighValueTitle() {
        QolUtilityCatalog.ModuleDef drops = QolUtilityCatalog.findById("qol.slayer_drops");

        assertNotNull(drops);
        assertTrue(drops.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.slayer_drops.price_title")
                        && setting.type() == QolUtilityCatalog.SettingType.TOGGLE));
        assertTrue(drops.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.slayer_drops.price_title_minimum")
                        && setting.type() == QolUtilityCatalog.SettingType.NUMBER));
        assertTrue(drops.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.slayer_drops.price_title_sound")
                        && setting.type() == QolUtilityCatalog.SettingType.TOGGLE));
    }

    @Test
    void slayerProfitHudRowsAndRateUseTheNormalPersistedContract() {
        QolUtilityConfig config = new QolUtilityConfig();
        config.setModuleEnabled("qol.slayer_drops", true);
        config.writeBoolean("qol.slayer_drops.profit_hud", true);
        config.writeBoolean("qol.slayer_drops.profit_table", false);
        config.writeBoolean("qol.slayer_drops.profit_per_hour", false);
        assertTrue(config.writeNumber("qol.slayer_drops.profit_items_shown", 7.0D));

        assertFalse(config.readBoolean("qol.slayer_drops.profit_table"));
        assertFalse(config.readBoolean("qol.slayer_drops.profit_per_hour"));
        assertEquals(7.0D, config.readNumber("qol.slayer_drops.profit_items_shown"));

        assertTrue(config.resetModuleToDefaults("qol.slayer_drops"));
        assertTrue(config.readBoolean("qol.slayer_drops.profit_table"));
        assertTrue(config.readBoolean("qol.slayer_drops.profit_per_hour"));
        assertEquals(5.0D, config.readNumber("qol.slayer_drops.profit_items_shown"));
    }

    @Test
    void slayerStatsExposesALocalSessionResetAction() {
        QolUtilityCatalog.ModuleDef stats = QolUtilityCatalog.findById("qol.slayer_stats");

        assertNotNull(stats);
        assertTrue(stats.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.slayer_stats.reset_session")
                        && setting.type() == QolUtilityCatalog.SettingType.ACTION));
    }

    @Test
    void exposesOneSharedSlayerFeatureFamilyWithoutPreviewCards() {
        Set<String> ids = QolUtilityCatalog.modules().stream()
                .filter(module -> module.group() == QolUtilityCatalog.Group.SLAYER)
                .map(QolUtilityCatalog.ModuleDef::id)
                .collect(Collectors.toSet());

        assertTrue(ids.contains("qol.slayer_display"));
        assertTrue(ids.contains("qol.slayer_time_messages"));
        assertTrue(ids.contains("qol.slayer_progress"));
        assertTrue(ids.contains("qol.slayer_stats"));
        assertTrue(ids.contains("qol.slayer_highlights"));
        assertTrue(ids.contains("qol.slayer_active_boss_transparency"));
        assertTrue(ids.contains("qol.slayer_irrelevant_mobs"));
        assertTrue(ids.contains("qol.slayer_miniboss_alert"));
        assertTrue(ids.contains("qol.slayer_drops"));
        assertTrue(ids.contains("qol.slayer_carry"));
        assertTrue(ids.contains("qol.slayer_cocoon_alert"));
        assertTrue(ids.contains("qol.slayer_dagger_swap"));
        assertTrue(ids.contains("qol.slayer_laser_hider"));
        assertTrue(ids.contains("qol.slayer_attunement_display"));
        assertTrue(ids.contains("qol.slayer_auto_soulcry"));
        assertTrue(ids.contains("qol.slayer_sounds"));
        assertTrue(ids.contains("qol.slayer_vengeance"));
        assertTrue(ids.contains("qol.slayer_vengeance_damage"));
        assertTrue(ids.contains("qol.slayer_big_drops"));
        assertTrue(ids.contains("qol.slayer_voidgloom"));
        assertTrue(ids.contains("qol.slayer_revenant"));
        assertTrue(ids.contains("qol.slayer_tarantula"));
        assertTrue(ids.contains("qol.slayer_sven"));
        assertTrue(ids.contains("qol.slayer_vampire_markers"));
        assertTrue(ids.contains("qol.slayer_inferno"));
        assertTrue(ids.contains("qol.slayer_quest_warning"));
        assertTrue(ids.contains("qol.slayer_auto_start"));
        for (String id : ids) {
            assertNotNull(QolUtilityCatalog.findById(id));
        }
    }

    @Test
    void slayerFightCopyKeepsEveryFamilyOnTheLocalPlayersBoss() {
        QolUtilityCatalog.ModuleDef highlights = QolUtilityCatalog.findById("qol.slayer_highlights");
        assertNotNull(highlights);
        assertTrue(highlights.description().contains("your own fight"));
        assertFalse(highlights.description().contains("Enderman Slayer boxes"));
        assertTrue(highlights.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.slayer_highlights.only_mine")
                        && setting.description().contains("Every Slayer family")));
        assertTrue(QolUtilityCatalog.findById("qol.slayer_voidgloom").description()
                .contains("your own Voidgloom fight"));
        assertTrue(QolUtilityCatalog.findById("qol.slayer_revenant").description()
                .contains("your own Revenant Horror fight"));
        assertTrue(QolUtilityCatalog.findById("qol.slayer_tarantula").description()
                .contains("your own Tarantula Broodfather fight"));
        assertTrue(QolUtilityCatalog.findById("qol.slayer_sven").description()
                .contains("your own Sven Packmaster fight"));
        assertTrue(QolUtilityCatalog.findById("qol.slayer_vampire_markers").description()
                .contains("your own Riftstalker Bloodfiend fight"));
        assertTrue(QolUtilityCatalog.findById("qol.slayer_inferno").description()
                .contains("your own Inferno Demonlord fight"));
        assertTrue(QolUtilityCatalog.findById("qol.slayer_miniboss_alert").description()
                .contains("your Slayer miniboss spawned"));
    }

    @Test
    void everySlayerDashboardValueControlHasAPersistedConfigBinding() {
        QolUtilityConfig config = new QolUtilityConfig();

        for (QolUtilityCatalog.ModuleDef module : QolUtilityCatalog.modules()) {
            if (module.group() != QolUtilityCatalog.Group.SLAYER) {
                continue;
            }
            for (QolUtilityCatalog.SettingDef setting : module.settings()) {
                switch (setting.type()) {
                    case TOGGLE -> assertNotNull(config.readBoolean(setting.id()), setting.id());
                    case NUMBER -> assertNotNull(config.readNumber(setting.id()), setting.id());
                    case TEXT -> assertNotNull(config.readText(setting.id()), setting.id());
                    case COLOR -> assertNotNull(config.readColor(setting.id()), setting.id());
                    case ACTION, SECTION, ENUM, KEYBIND, SQUARE -> {
                        // Actions are covered by the catalog-wide dashboard-action test.
                    }
                }
            }
        }
    }

    @Test
    void slayerSettingsPersistAndResetThroughTheNormalQolContract() {
        QolUtilityConfig config = new QolUtilityConfig();
        config.setModuleEnabled("qol.slayer_highlights", true);
        config.toggleBooleanSetting("qol.slayer_highlights.only_mine");
        config.writeBoolean("qol.slayer_highlights.target_lines", true);
        config.writeNumber("qol.slayer_highlights.boss_width", 4.5D);
        config.writeNumber("qol.slayer_highlights.target_line_distance", 40.0D);
        config.writeColor("qol.slayer_highlights.boss_color", 0xFF123456);
        config.writeText("qol.slayer_miniboss_alert.text", "A real miniboss appeared");

        assertTrue(config.isModuleEnabled("qol.slayer_highlights"));
        assertEquals(Boolean.FALSE, config.readBoolean("qol.slayer_highlights.only_mine"));
        assertEquals(Boolean.TRUE, config.readBoolean("qol.slayer_highlights.target_lines"));
        assertEquals(4.5D, config.readNumber("qol.slayer_highlights.boss_width"));
        assertEquals(40.0D, config.readNumber("qol.slayer_highlights.target_line_distance"));
        assertEquals(0xFF123456, config.readColor("qol.slayer_highlights.boss_color"));
        assertEquals("A real miniboss appeared", config.readText("qol.slayer_miniboss_alert.text"));

        assertTrue(config.resetModuleToDefaults("qol.slayer_highlights"));
        assertFalse(config.isModuleEnabled("qol.slayer_highlights"));
        assertEquals(Boolean.TRUE, config.readBoolean("qol.slayer_highlights.only_mine"));
        assertEquals(Boolean.FALSE, config.readBoolean("qol.slayer_highlights.target_lines"));
        assertEquals(2.0D, config.readNumber("qol.slayer_highlights.boss_width"));
        assertEquals(32.0D, config.readNumber("qol.slayer_highlights.target_line_distance"));
    }

    @Test
    void timeMessagesUseTheSamePersistedModuleContract() {
        QolUtilityConfig config = new QolUtilityConfig();
        config.setModuleEnabled("qol.slayer_time_messages", true);
        config.writeBoolean("qol.slayer_time_messages.personal_best", true);
        config.writeBoolean("qol.slayer_time_messages.compact", true);

        assertTrue(config.isModuleEnabled("qol.slayer_time_messages"));
        assertTrue(config.readBoolean("qol.slayer_time_messages.personal_best"));
        assertTrue(config.readBoolean("qol.slayer_time_messages.compact"));

        assertTrue(config.resetModuleToDefaults("qol.slayer_time_messages"));
        assertFalse(config.isModuleEnabled("qol.slayer_time_messages"));
        assertFalse(config.readBoolean("qol.slayer_time_messages.personal_best"));
        assertFalse(config.readBoolean("qol.slayer_time_messages.compact"));
    }

    @Test
    void progressWarningUsesTheNormalPersistedQolContract() {
        QolUtilityConfig config = new QolUtilityConfig();
        config.setModuleEnabled("qol.slayer_progress", true);
        config.writeBoolean("qol.slayer_progress.show_remaining", false);
        config.writeBoolean("qol.slayer_progress.boss_warning", true);
        config.writeBoolean("qol.slayer_progress.warning_repeat", true);
        assertTrue(config.writeNumber("qol.slayer_progress.warning_percent", 83.0D));

        assertTrue(config.isModuleEnabled("qol.slayer_progress"));
        assertFalse(config.readBoolean("qol.slayer_progress.show_remaining"));
        assertTrue(config.readBoolean("qol.slayer_progress.boss_warning"));
        assertTrue(config.readBoolean("qol.slayer_progress.warning_repeat"));
        assertEquals(83.0D, config.readNumber("qol.slayer_progress.warning_percent"));

        assertTrue(config.resetModuleToDefaults("qol.slayer_progress"));
        assertFalse(config.isModuleEnabled("qol.slayer_progress"));
        assertTrue(config.readBoolean("qol.slayer_progress.show_remaining"));
        assertTrue(config.readBoolean("qol.slayer_progress.boss_warning"));
        assertFalse(config.readBoolean("qol.slayer_progress.warning_repeat"));
        assertEquals(80.0D, config.readNumber("qol.slayer_progress.warning_percent"));
    }

    @Test
    void rngMeterHudUsesTheExistingSlayerDropsModuleContract() {
        QolUtilityConfig config = new QolUtilityConfig();
        config.setModuleEnabled("qol.slayer_drops", true);
        config.writeBoolean("qol.slayer_drops.rng_hud", true);
        assertTrue(config.readBoolean("qol.slayer_drops.rng_hud"));
        assertTrue(config.resetModuleToDefaults("qol.slayer_drops"));
        assertFalse(config.readBoolean("qol.slayer_drops.rng_hud"));
    }

    @Test
    void everyNewSlayerNumberHasAnExplicitSliderSpec() {
        assertNotNull(QolNumberSettings.spec("qol.slayer_highlights.boss_width"));
        assertNotNull(QolNumberSettings.spec("qol.slayer_highlights.miniboss_width"));
        assertNotNull(QolNumberSettings.spec("qol.slayer_highlights.demon_width"));
        assertNotNull(QolNumberSettings.spec("qol.slayer_highlights.target_line_width"));
        assertNotNull(QolNumberSettings.spec("qol.slayer_highlights.target_line_distance"));
        assertNotNull(QolNumberSettings.spec("qol.slayer_active_boss_transparency.strength"));
        assertNotNull(QolNumberSettings.spec("qol.slayer_irrelevant_mobs.strength"));
        assertNotNull(QolNumberSettings.spec("qol.slayer_miniboss_alert.distance"));
        assertNotNull(QolNumberSettings.spec("qol.slayer_cocoon_alert.pitch"));
        assertNotNull(QolNumberSettings.spec("qol.slayer_cocoon_alert.volume"));
        assertNotNull(QolNumberSettings.spec("qol.slayer_progress.warning_percent"));
        assertNotNull(QolNumberSettings.spec("qol.slayer_drops.profit_items_shown"));
        assertNotNull(QolNumberSettings.spec("qol.slayer_dagger_swap.delay"));
        assertNotNull(QolNumberSettings.spec("qol.slayer_dagger_swap.variance"));
        assertNotNull(QolNumberSettings.spec("qol.slayer_auto_soulcry.min_delay"));
        assertNotNull(QolNumberSettings.spec("qol.slayer_auto_soulcry.max_delay"));
        assertNotNull(QolNumberSettings.spec("qol.slayer_big_drops.scale"));
        assertNotNull(QolNumberSettings.spec("qol.slayer_big_drops.range"));
        assertNotNull(QolNumberSettings.spec("qol.slayer_big_drops.unscale_seconds"));
    }

    @Test
    void secondSlayerSliceSettingsPersistAndReset() {
        QolUtilityConfig config = new QolUtilityConfig();

        config.setModuleEnabled("qol.slayer_cocoon_alert", true);
        assertTrue(config.writeText(
                "qol.slayer_cocoon_alert.message", "<red>Wrapped!"));
        assertTrue(config.writeText(
                "qol.slayer_cocoon_alert.sound", "block.note_block.pling"));
        assertTrue(config.writeNumber("qol.slayer_cocoon_alert.pitch", 1.4D));
        assertTrue(config.writeNumber("qol.slayer_cocoon_alert.volume", 0.6D));
        config.setModuleEnabled("qol.slayer_dagger_swap", true);
        assertTrue(config.writeNumber("qol.slayer_dagger_swap.delay", 3.0D));
        assertTrue(config.writeNumber("qol.slayer_dagger_swap.variance", 4.0D));
        config.setModuleEnabled("qol.slayer_laser_hider", true);
        config.setModuleEnabled("qol.slayer_attunement_display", true);
        config.setModuleEnabled("qol.slayer_auto_soulcry", true);
        config.setModuleEnabled("qol.slayer_sounds", true);
        config.setModuleEnabled("qol.slayer_vengeance", true);
        config.setModuleEnabled("qol.slayer_vengeance_damage", true);
        config.setModuleEnabled("qol.slayer_big_drops", true);
        config.writeBoolean("qol.slayer_attunement_display.count", true);
        config.writeBoolean("qol.slayer_auto_soulcry.check_mana", false);
        config.writeBoolean("qol.slayer_auto_soulcry.attack_based", true);
        config.writeBoolean("qol.slayer_sounds.disable_voidgloom", true);
        config.writeBoolean("qol.slayer_vengeance.use_ticks", false);
        config.writeBoolean("qol.slayer_vengeance_damage.abbreviate", false);
        assertTrue(config.writeNumber("qol.slayer_auto_soulcry.min_delay", 2.0D));
        assertTrue(config.writeNumber("qol.slayer_auto_soulcry.max_delay", 5.0D));
        assertTrue(config.writeNumber("qol.slayer_big_drops.scale", 4.0D));

        assertTrue(config.isModuleEnabled("qol.slayer_cocoon_alert"));
        assertEquals("<red>Wrapped!", config.readText("qol.slayer_cocoon_alert.message"));
        assertEquals("block.note_block.pling", config.readText("qol.slayer_cocoon_alert.sound"));
        assertEquals(1.4D, config.readNumber("qol.slayer_cocoon_alert.pitch"));
        assertEquals(0.6D, config.readNumber("qol.slayer_cocoon_alert.volume"));
        assertEquals(3.0D, config.readNumber("qol.slayer_dagger_swap.delay"));
        assertEquals(4.0D, config.readNumber("qol.slayer_dagger_swap.variance"));
        assertTrue(config.isModuleEnabled("qol.slayer_laser_hider"));
        assertTrue(config.isModuleEnabled("qol.slayer_attunement_display"));
        assertTrue(config.isModuleEnabled("qol.slayer_auto_soulcry"));
        assertTrue(config.isModuleEnabled("qol.slayer_sounds"));
        assertTrue(config.isModuleEnabled("qol.slayer_vengeance"));
        assertTrue(config.isModuleEnabled("qol.slayer_vengeance_damage"));
        assertTrue(config.isModuleEnabled("qol.slayer_big_drops"));
        assertTrue(config.readBoolean("qol.slayer_attunement_display.count"));
        assertFalse(config.readBoolean("qol.slayer_auto_soulcry.check_mana"));
        assertTrue(config.readBoolean("qol.slayer_auto_soulcry.attack_based"));
        assertTrue(config.readBoolean("qol.slayer_sounds.disable_voidgloom"));
        assertFalse(config.readBoolean("qol.slayer_vengeance.use_ticks"));
        assertFalse(config.readBoolean("qol.slayer_vengeance_damage.abbreviate"));
        assertEquals(2.0D, config.readNumber("qol.slayer_auto_soulcry.min_delay"));
        assertEquals(5.0D, config.readNumber("qol.slayer_auto_soulcry.max_delay"));
        assertEquals(4.0D, config.readNumber("qol.slayer_big_drops.scale"));

        assertTrue(config.resetModuleToDefaults("qol.slayer_cocoon_alert"));
        assertFalse(config.isModuleEnabled("qol.slayer_cocoon_alert"));
        assertEquals("<red>Boss cocooned!", config.readText("qol.slayer_cocoon_alert.message"));
        assertEquals(1.0D, config.readNumber("qol.slayer_cocoon_alert.pitch"));
        assertEquals(1.0D, config.readNumber("qol.slayer_cocoon_alert.volume"));
    }
}
