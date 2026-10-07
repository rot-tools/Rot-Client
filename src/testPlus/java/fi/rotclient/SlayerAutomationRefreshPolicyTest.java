package fi.rotclient;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SlayerAutomationRefreshPolicyTest {
    @Test
    void togglesHeldDaggerOnceEvenIfServerHasNotUpdatedItsModeYet() {
        var state = new SlayerAutomationPolicy.DaggerSwapState();
        assertTrue(state.observe("ASHEN ♨: 12M❤", 0, 0));
        assertTrue(state.finishHeldDagger(1));
        for (int tick = 0; tick < 20; tick++) {
            state.tick();
            assertFalse(state.finishHeldDagger(1));
            assertTrue(state.ready().isEmpty());
        }
        assertFalse(state.observe("ASHEN ♨: 11M❤", 0, 0));
        assertTrue(state.observe("AURIC ♨: 11M❤", 0, 0));
        assertTrue(state.finishHeldDagger(0));
    }

    @Test
    void matchingOrMissingModeDoesNotIssueUse() {
        var state = new SlayerAutomationPolicy.DaggerSwapState();
        state.observe("ASHEN ♨: 12M❤", 0, 0);
        assertFalse(state.finishHeldDagger(0));
        assertTrue(state.ready().isEmpty());

        state.reset();
        state.observe("ASHEN ♨: 12M❤", 0, 0);
        assertFalse(state.finishHeldDagger(-1));
        assertTrue(state.ready().isEmpty());
    }

    @Test
    void singleUseCannotBypassDelayAndStalePendingExpires() {
        var state = new SlayerAutomationPolicy.DaggerSwapState();
        state.observe("SPIRIT ♨: 12M❤", 2, 0);
        assertFalse(state.finishHeldDagger(3));
        state.tick();
        assertFalse(state.finishHeldDagger(3));
        state.tick();
        assertTrue(state.finishHeldDagger(3));

        state.reset();
        state.observe("SPIRIT ♨: 12M❤", 10, 10);
        for (int tick = 0; tick <= SlayerAutomationPolicy.MAX_DAGGER_PENDING_TICKS; tick++) {
            state.tick();
        }
        assertTrue(state.ready().isEmpty());
        assertFalse(state.finishHeldDagger(3));
        assertTrue(state.observe("SPIRIT ♨: 12M❤", 0, 0));
    }
}
