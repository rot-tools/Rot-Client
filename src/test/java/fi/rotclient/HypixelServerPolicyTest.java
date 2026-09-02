package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class HypixelServerPolicyTest {
    @Test
    void blankIsNotHypixel() {
        assertFalse(HypixelServerPolicy.isHypixelAddress(null));
        assertFalse(HypixelServerPolicy.isHypixelAddress(""));
        assertFalse(HypixelServerPolicy.isHypixelAddress("   "));
    }

    @Test
    void hypixelHostsAreDetected() {
        assertTrue(HypixelServerPolicy.isHypixelAddress("mc.hypixel.net"));
        assertTrue(HypixelServerPolicy.isHypixelAddress("hypixel.net"));
        assertTrue(HypixelServerPolicy.isHypixelAddress("play.hypixel.net:25565"));
        assertTrue(HypixelServerPolicy.isHypixelAddress("HYPIXEL.NET"));
    }

    @Test
    void unrelatedHostsAreNotHypixel() {
        assertFalse(HypixelServerPolicy.isHypixelAddress("localhost"));
        assertFalse(HypixelServerPolicy.isHypixelAddress("example.com"));
        assertFalse(HypixelServerPolicy.isHypixelAddress("not-hypixel.example"));
    }
}
