package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PlayerDisplayHidePolicyTest {
    @Test
    void moduleOffIgnoresEveryPersistedActionHide() {
        QolUtilityConfig qol = new QolUtilityConfig();
        qol.setModuleEnabled("qol.player_display", false);
        qol.writeBoolean("qol.player_display.hide_action_health", true);
        qol.writeBoolean("qol.player_display.hide_action_defense", true);
        qol.writeBoolean("qol.player_display.hide_action_mana", true);
        qol.writeBoolean("qol.player_display.hide_action_overflow", true);
        qol.writeBoolean("qol.player_display.hide_action_speed", true);
        qol.writeBoolean("qol.player_display.hide_action_vitality", true);
        qol.writeBoolean("qol.player_display.hide_action_location", true);
        qol.writeBoolean("qol.player_display.health_hud", true);
        PlayerDisplayHidePolicy.ActionHides hides = PlayerDisplayHidePolicy.from(qol);
        assertFalse(hides.any());
        assertFalse(hides.health());
        assertFalse(hides.location());
    }

    @Test
    void defaultLocationHideWaitsForTheModule() {
        QolUtilityConfig qol = new QolUtilityConfig();
        assertTrue(qol.playerDisplayHideActionLocation);
        assertFalse(qol.playerDisplayEnabled);
        assertFalse(PlayerDisplayHidePolicy.from(qol).any());
        qol.setModuleEnabled("qol.player_display", true);
        assertTrue(PlayerDisplayHidePolicy.from(qol).location());
    }

    @Test
    void enabledHealthHudAutoHidesActionHealth() {
        QolUtilityConfig qol = new QolUtilityConfig();
        qol.setModuleEnabled("qol.player_display", true);
        qol.writeBoolean("qol.player_display.hide_action_health", false);
        qol.writeBoolean("qol.player_display.health_hud", true);
        assertTrue(PlayerDisplayHidePolicy.from(qol).health());
        qol.writeBoolean("qol.player_display.health_hud", false);
        assertFalse(PlayerDisplayHidePolicy.from(qol).health());
        qol.writeBoolean("qol.player_display.hide_action_health", true);
        assertTrue(PlayerDisplayHidePolicy.from(qol).health());
    }
}
