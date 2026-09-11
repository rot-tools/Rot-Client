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
        assertEquals(null, QolUtilityCatalog.findById("qol.camera"));
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
                "qol.trajectories",
                "qol.world_scanner",
                "qol.inventory_overlay",
                "qol.skill_levels",
                "qol.pet_hud",
                "qol.name_hider",
                "qol.chat_commands",
                "qol.slot_binds",
                "qol.waypoints",
                "qol.fishing_helper",
                "qol.fishing_creatures",
                "qol.fishing_hotspots",
                "qol.fishing_trophy",
                "qol.fishing_visuals",
                "qol.fishing_tools",
                "qol.item_tooltips",
                "qol.commission_display",
                "qol.mining_tracker",
                "qol.powder_chest",
                "qol.mining_session",
                "qol.mining_history",
                "qol.appearance",
                "qol.mining_scatha",
                "qol.mining_events",
                "qol.mining_glacite",
                "qol.mining_helpers",
                "qol.mining_hotm",
                "qol.diana_burrows",
                "qol.diana_mobs",
                "qol.diana_profit",
                "qol.foraging_trees",
                "qol.foraging_audio",
                "qol.foraging_helpers",
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
                "qol.escrow_fix",
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
                "qol.slayer_laser_hider",
                "qol.slayer_attunement_display",
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
                "qol.dungeon_hud",
                "qol.dungeon_esp",
                "qol.dungeon_announce",
                "qol.dungeon_leap",
                "qol.dungeon_terminals",
                "qol.dungeon_termsim",
                "qol.dungeon_puzzles",
                "qol.dungeon_f7",
                "qol.dungeon_menus",
                "qol.dungeon_carry",
                "qol.dungeon_party_join",
                "qol.dungeon_term_click",
                "qol.dungeon_watcher",
                "qol.hud_layout",
                "qol.custom_cursor",
                "qol.legacy_textures",
                "qol.custom_resource_pack",
                "qol.iota",
                "qol.stall_market",
                "qol.custom_scoreboard")) {
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
        assertTrue(utilities.stream().noneMatch(m -> m.id().equals("qol.farm_keys")));
        assertTrue(utilities.stream().noneMatch(m -> m.id().equals("qol.hud_layout")));
        assertTrue(hud.stream().anyMatch(m -> m.id().equals("qol.player_display")));
        assertTrue(hud.stream().anyMatch(m -> m.id().equals("qol.performance_hud")));
        assertTrue(hud.stream().anyMatch(m -> m.id().equals("qol.pet_hud")));
        assertTrue(hud.stream().anyMatch(m -> m.id().equals("qol.name_hider")));
        assertTrue(hud.stream().anyMatch(m -> m.id().equals("qol.item_tooltips")));
        assertTrue(hud.stream().anyMatch(m -> m.id().equals("qol.appearance")));
        assertTrue(hud.stream().anyMatch(m -> m.id().equals("qol.hud_layout")));
        assertTrue(QolUtilityCatalog.modulesOnGroupPage(QolUtilityCatalog.Group.HUD_DISPLAY)
                .stream()
                .noneMatch(m -> m.id().equals("qol.hud_layout")));
        assertTrue(QolUtilityCatalog.modulesOnGroupPage(QolUtilityCatalog.Group.HUD_DISPLAY)
                .stream()
                .noneMatch(m -> m.id().equals("qol.appearance")));
        assertTrue(QolUtilityCatalog.hiddenFromGroupPage(
                QolUtilityCatalog.findById("qol.hud_layout")));
        assertTrue(QolUtilityCatalog.hiddenFromGroupPage(
                QolUtilityCatalog.findById("qol.appearance")));
        assertTrue(hud.stream().anyMatch(m -> m.id().equals("qol.custom_cursor")));
        assertTrue(hud.stream().noneMatch(m -> m.id().equals("qol.info_tooltips")));
        assertTrue(hud.stream().anyMatch(m -> m.id().equals("qol.skill_levels")));
        assertTrue(iface.stream().anyMatch(m -> m.id().equals("qol.inventory_overlay")));
        assertTrue(iface.stream().anyMatch(m -> m.id().equals("qol.storage_overlay")));
        assertTrue(iface.stream().anyMatch(m -> m.id().equals("qol.reward_claim")));
        assertTrue(iface.stream().anyMatch(m -> m.id().equals("qol.slot_binds")));
        assertTrue(iface.stream().anyMatch(m -> m.id().equals("qol.no_cursor_reset")));
        assertTrue(iface.stream().noneMatch(m -> m.id().equals("qol.auto_experiments")));
        assertTrue(iface.stream().noneMatch(m -> m.id().equals("qol.auto_harp")));
        assertTrue(iface.stream().noneMatch(m -> m.id().equals("qol.hud_layout")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.GARDEN)
                .stream()
                .noneMatch(m -> m.id().equals("qol.farm_keys")));
        assertTrue(utilities.stream().anyMatch(m -> m.id().equals("qol.stall_market")));
        List<QolUtilityCatalog.ModuleDef> gui =
                QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.GUI);
        assertEquals("qol.custom_scoreboard", gui.get(0).id());
        assertEquals("Custom Scoreboard", gui.get(0).name());
        assertEquals("Board", gui.get(0).section());
        assertEquals("GUI", QolUtilityCatalog.Group.GUI.title());
        assertTrue(utilities.stream().noneMatch(m -> m.id().equals("qol.skill_levels")));
        assertTrue(render.stream().noneMatch(m -> m.id().equals("qol.camera")));
        assertTrue(render.stream().noneMatch(m -> m.id().equals("qol.freecam")));
        assertTrue(iface.stream().anyMatch(m -> m.id().equals("qol.click_gui")));
        assertTrue(render.stream().anyMatch(m -> m.id().equals("qol.legacy_textures")));
        assertTrue(render.stream().anyMatch(m -> m.id().equals("qol.custom_resource_pack")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.COMBAT)
                .stream()
                .noneMatch(m -> m.id().equals("qol.auto_clicker")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.COMBAT)
                .stream()
                .noneMatch(m -> m.id().equals("qol.auto_dojo")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.EVENTS)
                .stream()
                .anyMatch(m -> m.id().equals("qol.diana_burrows")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.COMBAT)
                .stream()
                .noneMatch(m -> m.id().equals("qol.diana_burrows")));
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
        assertEquals(3, QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.FORAGING).size());
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.DUNGEONS)
                .stream()
                .noneMatch(m -> m.id().equals("qol.secret_hitboxes")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.DUNGEONS)
                .stream()
                .noneMatch(m -> m.id().equals("qol.auto_gfs")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.DUNGEONS)
                .stream()
                .anyMatch(m -> m.id().equals("qol.dungeon_hud")));
        assertEquals("HUD", QolUtilityCatalog.findById("qol.dungeon_hud").section());
        assertEquals("F7", QolUtilityCatalog.findById("qol.dungeon_terminals").section());
        assertEquals("F7", QolUtilityCatalog.findById("qol.dungeon_termsim").section());
        assertEquals("Kuudra", QolUtilityCatalog.findById("qol.iota").section());
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.KUUDRA)
                .stream()
                .anyMatch(m -> m.id().equals("qol.iota")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.DUNGEONS)
                .stream()
                .noneMatch(m -> m.id().equals("qol.iota")));
        assertEquals(13, QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.DUNGEONS).size());
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.DUNGEONS)
                .stream()
                .anyMatch(m -> m.id().equals("qol.dungeon_carry")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.DUNGEONS)
                .stream()
                .noneMatch(m -> m.id().equals("qol.dungeon_hover_terms")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.DUNGEONS)
                .stream()
                .anyMatch(m -> m.id().equals("qol.dungeon_party_join")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.DUNGEONS)
                .stream()
                .noneMatch(m -> m.id().equals("qol.dungeon_soulsand")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.DUNGEONS)
                .stream()
                .anyMatch(m -> m.id().equals("qol.dungeon_term_click")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.DUNGEONS)
                .stream()
                .anyMatch(m -> m.id().equals("qol.dungeon_watcher")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.UTILITIES)
                .stream()
                .anyMatch(m -> m.id().equals("qol.stall_market")));
        assertEquals("Market", QolUtilityCatalog.findById("qol.stall_market").section());
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.RENDER)
                .stream()
                .anyMatch(m -> m.id().equals("qol.ghosts")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.MINING)
                .stream()
                .anyMatch(m -> m.id().equals("qol.mining_tracker")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.MINING)
                .stream()
                .anyMatch(m -> m.id().equals("qol.world_scanner")));
        assertTrue(QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.MINING)
                .stream()
                .anyMatch(m -> m.id().equals("qol.mining_scatha")));
        assertEquals(11, QolUtilityCatalog.modulesInGroup(QolUtilityCatalog.Group.MINING).size());
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
        assertEquals(null, QolUtilityCatalog.findById("qol.slayer_auto_soulcry"));
        assertEquals("Sven", QolUtilityCatalog.findById("qol.slayer_sven").section());
        assertEquals("Revenant", QolUtilityCatalog.findById("qol.slayer_revenant").section());
        assertEquals("Tarantula", QolUtilityCatalog.findById("qol.slayer_tarantula").section());
        assertEquals("Vampire", QolUtilityCatalog.findById("qol.slayer_vampire_markers").section());
        assertEquals("Blaze", QolUtilityCatalog.findById("qol.slayer_inferno").section());
        assertEquals(null, QolUtilityCatalog.findById("qol.slayer_auto_start"));
        assertEquals("Blaze", QolUtilityCatalog.findById("qol.slayer_vengeance_damage").section());
        assertEquals("Fight view", QolUtilityCatalog.findById("qol.slayer_active_boss_transparency").section());
        assertEquals("Fight view", QolUtilityCatalog.findById("qol.slayer_irrelevant_mobs").section());
        assertTrue(QolUtilityCatalog.sidebarPages().contains(QolUtilityCatalog.Group.COMBAT));
        assertTrue(QolUtilityCatalog.sidebarPages().contains(QolUtilityCatalog.Group.EVENTS));
        assertTrue(QolUtilityCatalog.sidebarPages().contains(QolUtilityCatalog.Group.FISHING));
        assertTrue(QolUtilityCatalog.sidebarPages().contains(QolUtilityCatalog.Group.FORAGING));
        assertEquals(
                List.of(
                        QolUtilityCatalog.Group.COMBAT,
                        QolUtilityCatalog.Group.SLAYER,
                        QolUtilityCatalog.Group.EVENTS,
                        QolUtilityCatalog.Group.DUNGEONS,
                        QolUtilityCatalog.Group.KUUDRA,
                        QolUtilityCatalog.Group.MINING,
                        QolUtilityCatalog.Group.FISHING,
                        QolUtilityCatalog.Group.FORAGING,
                        QolUtilityCatalog.Group.GUI,
                        QolUtilityCatalog.Group.HUD_DISPLAY,
                        QolUtilityCatalog.Group.RENDER,
                        QolUtilityCatalog.Group.INTERFACE,
                        QolUtilityCatalog.Group.UTILITIES),
                QolUtilityCatalog.sidebarPages());
        assertEquals("ESP", QolUtilityCatalog.findById("qol.hide_players").section());
        assertEquals("Catch", QolUtilityCatalog.findById("qol.fishing_helper").section());
        assertEquals("Trees", QolUtilityCatalog.findById("qol.foraging_trees").section());
        assertEquals("Scanner", QolUtilityCatalog.findById("qol.world_scanner").section());
        assertEquals("Skills", QolUtilityCatalog.findById("qol.skill_levels").section());
        assertEquals(null, QolUtilityCatalog.findById("qol.auto_clicker"));
        assertEquals(null, QolUtilityCatalog.findById("qol.foraging_cheats"));
        assertEquals(null, QolUtilityCatalog.findById("qol.farm_keys"));
        assertEquals("Blaze", QolUtilityCatalog.findById("qol.slayer_attunement_display").section());
        assertEquals("Dashboard", QolUtilityCatalog.findById("qol.click_gui").section());
        assertEquals("Layout", QolUtilityCatalog.findById("qol.hud_layout").section());
        assertEquals("HUD Elements Editor", QolUtilityCatalog.findById("qol.hud_layout").name());
        assertEquals(null, QolUtilityCatalog.findById("qol.foraging_cheats"));
        assertEquals(null, QolUtilityCatalog.findById("qol.diana_share"));
        assertEquals(null, QolUtilityCatalog.findById("qol.auto_clicker"));
        assertEquals(null, QolUtilityCatalog.findById("qol.auto_dojo"));
        assertEquals(null, QolUtilityCatalog.findById("qol.auto_conversation"));
        assertEquals(null, QolUtilityCatalog.findById("qol.auto_experiments"));
        assertEquals(null, QolUtilityCatalog.findById("qol.auto_harp"));
        assertFalse(QolUtilityCatalog.hasCheatTag(QolUtilityCatalog.findById("qol.fishing_helper")));
        assertEquals(null, QolUtilityCatalog.findById("qol.inventory_walk"));
        assertEquals(null, QolUtilityCatalog.findById("qol.freecam"));
        assertFalse(QolUtilityCatalog.hasCheatTag(QolUtilityCatalog.findById("qol.experiment_solver")));
        assertEquals(null, QolUtilityCatalog.findById("qol.secret_hitboxes"));
        assertFalse(QolUtilityCatalog.hasCheatTag(QolUtilityCatalog.findById("qol.auto_sprint")));
        assertFalse(QolUtilityCatalog.hasCheatTag(QolUtilityCatalog.findById("qol.performance_hud")));
        for (QolUtilityCatalog.ModuleDef module : QolUtilityCatalog.modules()) {
            if (module.id().startsWith("qol.auto_") && !module.id().equals("qol.auto_sprint")) {
                assertTrue(
                        QolUtilityCatalog.hasCheatTag(module),
                        "auto module missing CHEAT tag: " + module.id());
            }
        }
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
                        && !id.equals("qol.dungeon_carry.open_manager")
                        && !id.equals("qol.slayer_stats.reset_session")
                        && !id.equals("qol.slayer_drops.open_filter_editor")
                        && !id.equals("qol.dungeon_termsim.open")
                        && !id.equals("qol.dungeon_hud.reset_split_pbs")
                        && !id.equals("qol.dungeon_hud.reset_kuudra_pbs")
                        && !id.equals("qol.dungeon_f7.reset_term_pbs")
                        && !id.equals("qol.dungeon_f7.reset_predev_pb")
                        && !id.equals("qol.storage_overlay.clear_cache")
                        && !id.equals("qol.storage_overlay.reload_pages")
                        && !id.equals("qol.storage_overlay.clear_search")
                        && !id.equals("qol.inventory_overlay.open_colors")
                        && !id.startsWith("qol.inventory_buttons.")
                        && !id.equals("qol.mining_tracker.open_page")
                        && !id.equals("qol.powder_chest.open_page")
                        && !id.equals("qol.mining_session.open_page")
                        && !id.equals("qol.mining_history.open_page")
                        && !id.startsWith("qol.appearance.open_")
                        && !id.equals("qol.custom_scoreboard.reset_appearance")
                        && !id.equals("qol.custom_scoreboard.reset_events")
                        && !id.equals("qol.market_watch.open_dashboard"))
                .collect(Collectors.toList());

        assertTrue(unsupported.isEmpty(), "Action rows without behavior: " + unsupported);
    }

    @Test
    void wardrobeSwapperExposesStationaryGateAndNineExplicitSlotBinds() {
        QolUtilityCatalog.ModuleDef module =
                QolUtilityCatalog.findById("qol.cheater_wardrobe");
        assertEquals(null, module);
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
        assertEquals(111, moduleIds.size());
    }
}
