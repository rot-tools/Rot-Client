package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ForagingAutomationPolicyTest {
    @Test
    void chopAndTossRequireEveryGate() {
        assertFalse(ForagingAutomationPolicy.shouldAutoChop(
                true, false, ForagingPolicy.Island.GALATEA, true, true));
        assertTrue(ForagingAutomationPolicy.shouldAutoChop(
                true, true, ForagingPolicy.Island.HUB, true, true));
        assertFalse(ForagingAutomationPolicy.shouldAutoChop(
                true, true, ForagingPolicy.Island.NONE, true, true));
        assertTrue(ForagingAutomationPolicy.shouldAxeToss(true, true, true, 8, 5, true));
        assertFalse(ForagingAutomationPolicy.shouldAxeToss(true, true, true, 3, 5, true));
    }

    @Test
    void beaconClickAdvancesOnlyWhenEnabled() {
        ForagingPolicy.BeaconHint hint = new ForagingPolicy.BeaconHint(2, 0, 0);
        ForagingAutomationPolicy.AutoBeaconClick click =
                ForagingAutomationPolicy.nextBeaconClick(
                        true, true, "Tune Frequency", hint);
        assertNotNull(click);
        assertEquals(ForagingPolicy.BEACON_COLOR_SLOT, click.slot());
        assertTrue(click.rightClick());
        assertNull(ForagingAutomationPolicy.nextBeaconClick(
                true, false, "Tune Frequency", hint));
        assertEquals(
                1,
                ForagingAutomationPolicy.applyPress(
                        hint, ForagingPolicy.BEACON_COLOR_SLOT, true, "Tune Frequency")
                        .colorClicks());
        assertEquals(0, ForagingAutomationPolicy.remainingAfterDirectedClick(1, 13, true));
    }
}
