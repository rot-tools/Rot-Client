package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class FishingAutomationPolicyTest {
    @Test
    void attacksOnlyAnObservedCreatureWithoutAnOpenScreen() {
        assertTrue(FishingAutomationPolicy.shouldAutoAttack(true, true, true, false));
        assertFalse(FishingAutomationPolicy.shouldAutoAttack(true, true, true, true));
        assertFalse(FishingAutomationPolicy.shouldAutoAttack(true, false, true, false));
    }
}
