package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarmKeysPolicyTest {
    @Test
    void cameraLockAndRemapMatchTemple() {
        assertTrue(FarmKeysPolicy.shouldLockCamera(true, true));
        assertFalse(FarmKeysPolicy.shouldLockCamera(false, true));
        assertFalse(FarmKeysPolicy.shouldLockCamera(true, false));
        assertTrue(FarmKeysPolicy.shouldRemap(true, "R"));
        assertFalse(FarmKeysPolicy.shouldRemap(true, ""));
        assertFalse(FarmKeysPolicy.shouldRemap(false, "R"));
    }
}
