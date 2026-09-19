package fi.rotclient;

import java.util.List;

/**
 * Which QoL parents toggle themselves from their own "Keybind" row.
 *
 * <p>A catalog row that says "Toggle this module" does nothing unless something
 * reads the key, so every such parent must be listed here (handled by
 * {@link QolModuleKeybindRuntime}) or in {@link #HANDLED_ELSEWHERE}. A test
 * enforces that.</p>
 */
public final class QolModuleKeybindCatalog {
    /** Parents toggled by the shared per-tick keybind runtime. */
    public static final List<String> TOGGLE_MODULE_IDS = List.of(
            CustomScoreboardPolicy.MODULE_ID,
            "qol.no_cursor_reset",
            "qol.player_display",
            "qol.pet_keybinds",
            "qol.performance_hud",
            "qol.render_optimizer",
            "qol.hide_players",
            "qol.player_size",
            "qol.etherwarp");

    /** Parents whose toggle key is read by their own runtime. */
    public static final List<String> HANDLED_ELSEWHERE = List.of(
            "qol.chat_commands");

    private QolModuleKeybindCatalog() {
    }

    public static String keybindSettingId(String moduleId) {
        return moduleId + ".keybind";
    }
}
