package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class QolUtilityCatalogTest {
    @Test
    void catalogContainsRequiredGroupsAndSkipsBannedModules() {
        assertFalse(QolUtilityCatalog.modules().isEmpty());
        assertNotNull(QolUtilityCatalog.findById("qol.auto_sprint"));
        assertNotNull(QolUtilityCatalog.findById("qol.camera"));
        assertNotNull(QolUtilityCatalog.findById("qol.render_optimizer"));
        assertNotNull(QolUtilityCatalog.findById("qol.waypoints"));
        assertFalse(QolUtilityCatalog.findById("qol.waypoints").wip());
        assertTrue(QolUtilityCatalog.findById("qol.waypoints").toggleable());
        assertTrue(QolUtilityCatalog.findById("qol.waypoints").runtimeReady());

        String joined = QolUtilityCatalog.modules().toString().toLowerCase();
        assertFalse(joined.contains("gyro"));
        assertFalse(joined.contains("ragnarok"));
        assertFalse(joined.contains("spring boots"));
    }

    @Test
    void childSettingResolvesParentModule() {
        QolUtilityCatalog.ModuleDef module =
                QolUtilityCatalog.findById("qol.render_optimizer.hide_lightning");
        assertNotNull(module);
        assertEquals("qol.render_optimizer", module.id());
    }

    @Test
    void committedAndBatchTwoRuntimeModulesAreMarkedReady() {
        for (String id : List.of(
                "qol.no_cursor_reset",
                "qol.player_display",
                "qol.performance_hud",
                "qol.render_optimizer",
                "qol.hide_players",
                "qol.player_size",
                "qol.etherwarp",
                "qol.command_keybinds",
                "qol.wardrobe_keybinds",
                "qol.loadout_keybinds",
                "qol.pet_keybinds",
                "qol.auto_clicker",
                "qol.inventory_walk",
                "qol.trajectories",
                "qol.secret_hitboxes",
                "qol.world_scanner",
                "qol.inventory_overlay",
                "qol.skill_levels",
                "qol.pet_hud",
                "qol.name_hider",
                "qol.chat_commands",
                "qol.slot_binds",
                "qol.waypoints",
                "qol.auto_conversation",
                "qol.fishing_helper",
                "qol.fishing_creatures",
                "qol.fishing_hotspots",
                "qol.fishing_trophy",
                "qol.fishing_visuals",
                "qol.fishing_tools",
                "qol.item_tooltips",
                "qol.commission_display",
                "qol.mining_scatha",
                "qol.mining_events",
                "qol.mining_glacite",
                "qol.mining_helpers",
                "qol.mining_hotm",
                "qol.diana_burrows",
                "qol.diana_mobs",
                "qol.diana_profit",
                "qol.diana_share",
                "qol.foraging_trees",
                "qol.foraging_audio",
                "qol.foraging_helpers",
                "qol.foraging_cheats",
                "qol.item_rarity",
                "qol.mob_highlight",
                "qol.viewmodel",
                "qol.item_scale",
                "qol.animation_fix",
                "qol.disconnect_fix",
                "qol.double_use_fix",
                "qol.eye_height_fix",
                "qol.instant_sneak",
                "qol.item_count_fix",
                "qol.active_pet_highlight",
                "qol.anvil_helper",
                "qol.calendar_date",
                "qol.experiment_solver",
                "qol.auto_experiments",
                "qol.cheater_wardrobe",
                "qol.escrow_fix",
                "qol.auto_harp",
                "qol.auto_gfs",
                "qol.auto_sell",
                "qol.ghosts",
                "qol.slayer_display",
                "qol.slayer_time_messages",
                "qol.slayer_progress",
                "qol.slayer_stats",
                "qol.slayer_highlights",
                "qol.slayer_miniboss_alert",
                "qol.slayer_drops",
                "qol.slayer_carry",
                "qol.slayer_cocoon_alert",
                "qol.slayer_dagger_swap",
                "qol.slayer_laser_hider",
                "qol.slayer_attunement_display",
                "qol.slayer_auto_soulcry",
                "qol.slayer_sounds",
                "qol.slayer_vengeance",
                "qol.slayer_vengeance_damage",
                "qol.slayer_big_drops",
                "qol.slayer_voidgloom",
                "qol.slayer_revenant",
                "qol.slayer_tarantula",
                "qol.slayer_sven",
                "qol.slayer_vampire_markers",
                "qol.slayer_inferno",
                "qol.slayer_quest_warning",
                "qol.slayer_auto_start",
                "qol.dungeon_hud",
                "qol.dungeon_esp",
                "qol.dungeon_announce",
                "qol.dungeon_leap",
                "qol.dungeon_terminals",
                "qol.dungeon_termsim",
                "qol.dungeon_requeue",
                "qol.dungeon_puzzles",
                "qol.dungeon_f7",
                "qol.dungeon_menus",
                "qol.farm_keys",
                "qol.auto_dojo",
                "qol.freecam",
                "qol.hud_layout",
                "qol.custom_cursor",
                "qol.legacy_textures",
                "qol.custom_resource_pack",
                "qol.iota",
                "qol.stall_market")) {
            QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById(id);
            assertNotNull(module, id);
            assertTrue(module.runtimeReady(), id);
        }
    }

    @Test
    void groupsContainExpectedModules() {
        List<QolUtilityCatalog.ModuleDef> utilities =
                QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.UTILITIES);
        List<QolUtilityCatalog.ModuleDef> hud =
                QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.HUD_DISPLAY);
        List<QolUtilityCatalog.ModuleDef> render =
                QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.RENDER);
        List<QolUtilityCatalog.ModuleDef> iface =
                QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.INTERFACE);
        assertTrue(utilities.stream().anyMatch(m -> m.id().equals("qol.auto_sprint")));
        assertTrue(utilities.stream().noneMatch(m -> m.id().equals("qol.auto_clicker")));
        assertTrue(hud.stream().anyMatch(m -> m.id().equals("qol.player_display")));
        assertTrue(hud.stream().anyMatch(m -> m.id().equals("qol.performance_hud")));
        assertTrue(hud.stream().anyMatch(m -> m.id().equals("qol.pet_hud")));
        assertTrue(hud.stream().anyMatch(m -> m.id().equals("qol.name_hider")));
        assertTrue(hud.stream().anyMatch(m -> m.id().equals("qol.item_tooltips")));
        assertTrue(hud.stream().noneMatch(m -> m.id().equals("qol.info_tooltips")));
        assertTrue(hud.stream().anyMatch(m -> m.id().equals("qol.skill_levels")));
        assertTrue(iface.stream().anyMatch(m -> m.id().equals("qol.inventory_overlay")));
        assertTrue(iface.stream().anyMatch(m -> m.id().equals("qol.storage_overlay")));
        assertTrue(iface.stream().anyMatch(m -> m.id().equals("qol.reward_claim")));
        assertTrue(iface.stream().anyMatch(m -> m.id().equals("qol.slot_binds")));
        assertTrue(utilities.stream().anyMatch(m -> m.id().equals("qol.farm_keys")));
        assertTrue(utilities.stream().noneMatch(m -> m.id().equals("qol.skill_levels")));
        assertTrue(render.stream().anyMatch(m -> m.id().equals("qol.camera")));
        assertTrue(render.stream().anyMatch(m -> m.id().equals("qol.freecam")));
        assertTrue(iface.stream().anyMatch(m -> m.id().equals("qol.click_gui")));
        assertTrue(iface.stream().anyMatch(m -> m.id().equals("qol.hud_layout")));
        assertTrue(iface.stream().anyMatch(m -> m.id().equals("qol.custom_cursor")));
        assertTrue(render.stream().anyMatch(m -> m.id().equals("qol.legacy_textures")));
        assertTrue(render.stream().anyMatch(m -> m.id().equals("qol.custom_resource_pack")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.COMBAT)
                .stream()
                .anyMatch(m -> m.id().equals("qol.auto_clicker")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.COMBAT)
                .stream()
                .anyMatch(m -> m.id().equals("qol.auto_dojo")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.COMBAT)
                .stream()
                .anyMatch(m -> m.id().equals("qol.diana_burrows")));
        assertEquals("Diana", QolUtilityCatalog.findById("qol.diana_burrows").section());
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.FISHING)
                .stream()
                .anyMatch(m -> m.id().equals("qol.fishing_helper")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.FISHING)
                .stream()
                .anyMatch(m -> m.id().equals("qol.fishing_creatures")));
        assertEquals(6, QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.FISHING).size());
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.FORAGING)
                .stream()
                .anyMatch(m -> m.id().equals("qol.foraging_trees")));
        assertEquals(4, QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.FORAGING).size());
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.DUNGEONS)
                .stream()
                .anyMatch(m -> m.id().equals("qol.secret_hitboxes")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.DUNGEONS)
                .stream()
                .anyMatch(m -> m.id().equals("qol.auto_gfs")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.DUNGEONS)
                .stream()
                .anyMatch(m -> m.id().equals("qol.dungeon_hud")));
        assertEquals("HUD", QolUtilityCatalog.findById("qol.dungeon_hud").section());
        assertEquals("F7", QolUtilityCatalog.findById("qol.dungeon_terminals").section());
        assertEquals("F7", QolUtilityCatalog.findById("qol.dungeon_termsim").section());
        assertEquals("Kuudra", QolUtilityCatalog.findById("qol.iota").section());
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.DUNGEONS)
                .stream()
                .anyMatch(m -> m.id().equals("qol.iota")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.UTILITIES)
                .stream()
                .anyMatch(m -> m.id().equals("qol.stall_market")));
        assertEquals("Market", QolUtilityCatalog.findById("qol.stall_market").section());
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.RENDER)
                .stream()
                .anyMatch(m -> m.id().equals("qol.ghosts")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.MINING)
                .stream()
                .anyMatch(m -> m.id().equals("qol.world_scanner")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.MINING)
                .stream()
                .anyMatch(m -> m.id().equals("qol.mining_scatha")));
        assertEquals(7, QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.MINING).size());
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.COMBAT)
                .stream()
                .anyMatch(m -> m.id().equals("qol.mob_highlight")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.SLAYER)
                .stream()
                .anyMatch(m -> m.id().equals("qol.slayer_display")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.SLAYER)
                .stream()
                .noneMatch(m -> m.id().equals("qol.mob_highlight")));
        assertEquals("Blaze", QolUtilityCatalog.findById("qol.slayer_vengeance").section());
        assertEquals("Voidgloom", QolUtilityCatalog.findById("qol.slayer_voidgloom").section());
        assertEquals("Voidgloom", QolUtilityCatalog.findById("qol.slayer_laser_hider").section());
        assertEquals("Voidgloom", QolUtilityCatalog.findById("qol.slayer_auto_soulcry").section());
        assertEquals("Sven", QolUtilityCatalog.findById("qol.slayer_sven").section());
        assertEquals("Revenant", QolUtilityCatalog.findById("qol.slayer_revenant").section());
        assertEquals("Tarantula", QolUtilityCatalog.findById("qol.slayer_tarantula").section());
        assertEquals("Vampire", QolUtilityCatalog.findById("qol.slayer_vampire_markers").section());
        assertEquals("Blaze", QolUtilityCatalog.findById("qol.slayer_inferno").section());
        assertEquals("Automation", QolUtilityCatalog.findById("qol.slayer_auto_start").section());
        assertEquals("Blaze", QolUtilityCatalog.findById("qol.slayer_vengeance_damage").section());
        assertEquals("Fight view", QolUtilityCatalog.findById("qol.slayer_active_boss_transparency").section());
        assertEquals("Fight view", QolUtilityCatalog.findById("qol.slayer_irrelevant_mobs").section());
        assertTrue(QolUtilityCatalog.sidebarPages().contains(QolUtilityCatalog.Group.COMBAT));
        assertTrue(QolUtilityCatalog.sidebarPages().contains(QolUtilityCatalog.Group.FISHING));
        assertTrue(QolUtilityCatalog.sidebarPages().contains(QolUtilityCatalog.Group.FORAGING));
        assertEquals(
                List.of(
                        QolUtilityCatalog.Group.COMBAT,
                        QolUtilityCatalog.Group.SLAYER,
                        QolUtilityCatalog.Group.DUNGEONS,
                        QolUtilityCatalog.Group.MINING,
                        QolUtilityCatalog.Group.FISHING,
                        QolUtilityCatalog.Group.FORAGING,
                        QolUtilityCatalog.Group.HUD_DISPLAY,
                        QolUtilityCatalog.Group.RENDER,
                        QolUtilityCatalog.Group.INTERFACE,
                        QolUtilityCatalog.Group.UTILITIES),
                QolUtilityCatalog.sidebarPages());
        assertEquals("Clicker", QolUtilityCatalog.findById("qol.auto_clicker").section());
        assertEquals("ESP", QolUtilityCatalog.findById("qol.hide_players").section());
        assertEquals("Catch", QolUtilityCatalog.findById("qol.fishing_helper").section());
        assertEquals("Trees", QolUtilityCatalog.findById("qol.foraging_trees").section());
        assertEquals("Cheats", QolUtilityCatalog.findById("qol.foraging_cheats").section());
        assertEquals("Scanner", QolUtilityCatalog.findById("qol.world_scanner").section());
        assertEquals("Skills", QolUtilityCatalog.findById("qol.skill_levels").section());
        assertTrue(QolUtilityCatalog.hasCheatTag(QolUtilityCatalog.findById("qol.foraging_cheats")));
        assertTrue(QolUtilityCatalog.hasCheatTag(QolUtilityCatalog.findById("qol.diana_share")));
        assertFalse(QolUtilityCatalog.hasCheatTag(QolUtilityCatalog.findById("qol.performance_hud")));
    }

    @Test
    void everyActionRowHasAnImplementedDashboardAction() {
        List<String> unsupported = QolUtilityCatalog.modules().stream()
                .flatMap(module -> module.settings().stream())
                .filter(setting -> setting.type() == QolUtilityCatalog.SettingType.ACTION)
                .map(QolUtilityCatalog.SettingDef::id)
                .filter(id -> !id.endsWith("_hud_editor")
                        && !id.equals("qol.auto_sell.add_defaults")
                        && !id.equals("qol.command_keybinds.open_sequence_editor")
                        && !id.equals("qol.storage_overlay.open_item_search")
                        && !id.equals("qol.slayer_carry.open_manager")
                        && !id.equals("qol.slayer_stats.reset_session")
                        && !id.equals("qol.slayer_drops.open_filter_editor")
                        && !id.equals("qol.dungeon_termsim.open")
                        && !id.equals("qol.dungeon_hud.reset_split_pbs")
                        && !id.equals("qol.dungeon_hud.reset_kuudra_pbs")
                        && !id.equals("qol.storage_overlay.clear_cache")
                        && !id.equals("qol.storage_overlay.clear_search")
                        && !id.equals("qol.inventory_overlay.open_colors")
                        && !id.startsWith("qol.inventory_buttons."))
                .collect(Collectors.toList());

        assertTrue(unsupported.isEmpty(), "Action rows without behavior: " + unsupported);
    }

    @Test
    void wardrobeSwapperExposesStationaryGateAndNineExplicitSlotBinds() {
        QolUtilityCatalog.ModuleDef module =
                QolUtilityCatalog.findById("qol.cheater_wardrobe");
        assertNotNull(module);
        assertEquals("Wardrobe Swapper", module.name());
        assertTrue(module.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.cheater_wardrobe.stationary_only")
                        && setting.type() == QolUtilityCatalog.SettingType.TOGGLE));
        assertEquals(9L, module.settings().stream().filter(setting ->
                setting.id().matches("qol\\.cheater_wardrobe\\.slot_[1-9]")
                        && setting.type() == QolUtilityCatalog.SettingType.KEYBIND).count());
    }

    @Test
    void moduleAndSettingIdsAreGloballyUnique() {
        Set<String> moduleIds = new HashSet<>();
        Set<String> settingIds = new HashSet<>();
        List<String> duplicates = new java.util.ArrayList<>();

        for (QolUtilityCatalog.ModuleDef module : QolUtilityCatalog.modules()) {
            if (!moduleIds.add(module.id())) {
                duplicates.add("module " + module.id());
            }
            for (QolUtilityCatalog.SettingDef setting : module.settings()) {
                if (!settingIds.add(setting.id())) {
                    duplicates.add("setting " + setting.id());
                }
            }
        }

        assertTrue(duplicates.isEmpty(), "Duplicate QoL identifiers: " + duplicates);
        assertEquals(119, moduleIds.size());
    }
}
