package fi.rotclient;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PickobulusPolicyTest {
    @Test void parsesOnlyAbilityEvidenceAndValidCooldowns() {
        assertTrue(PickobulusPolicy.used("§aYou used your Pickobulus Pickaxe Ability!"));
        assertFalse(PickobulusPolicy.used("[Party] Somebody: You used your Pickobulus Pickaxe Ability!"));
        assertTrue(PickobulusPolicy.ready("Your Pickobulus is now available!"));
        assertEquals(60, PickobulusPolicy.seconds("Cooldown: 1m").orElseThrow());
        assertTrue(PickobulusPolicy.seconds("Cooldown: 0s").isEmpty());
        assertTrue(PickobulusPolicy.seconds("Cooldown: 900s").isEmpty());
    }
    @Test void timerNeverNotifiesTwiceAndServerReadyOverridesEstimate() {
        var timer = new PickobulusPolicy.Timer();
        assertEquals(-1, timer.remaining(0));
        assertFalse(timer.takeReadyNotification(0));
        timer.start(1_000, 60, false);
        assertEquals(59_000, timer.remaining(2_000));
        assertTrue(timer.label(2_000).contains("estimated"));
        assertFalse(timer.takeReadyNotification(2_000));
        timer.confirmReady(3_000);
        assertTrue(timer.takeReadyNotification(3_000));
        timer.confirmReady(4_000);
        assertFalse(timer.takeReadyNotification(4_000));
        assertFalse(timer.label(4_000).contains("estimated"));
    }
    @Test void configRoundTripsAndResetPreservesOrdinaryMiningSettings() {
        var config = new QolUtilityConfig();
        config.setModuleEnabled("qol.pickobulus", true);
        assertTrue(config.isModuleEnabled("qol.pickobulus"));
        assertTrue(config.writeNumber("qol.pickobulus.cooldown_seconds", 120));
        assertEquals(120, config.readNumber("qol.pickobulus.cooldown_seconds"));
        config.setPose("pickobulus", 100, 180);
        assertEquals(100, config.pose("pickobulus")[0]);
        assertEquals("pickobulus", HudElementCatalog.focusIdForHudEditorSetting("qol.pickobulus.open_hud_editor"));
        assertEquals("qol.pickobulus.timer_hud", HudLayoutLandingPolicy.disableForPose("pickobulus").settingId());
    }
}
