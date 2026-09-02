package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientThreadGuardTest {
    @Test
    void ignoresInitialNetworkThreadPacketPass() {
        assertFalse(ClientThreadGuard.shouldHandle(false));
    }

    @Test
    void handlesVanillaClientThreadReplay() {
        assertTrue(ClientThreadGuard.shouldHandle(true));
    }
}
