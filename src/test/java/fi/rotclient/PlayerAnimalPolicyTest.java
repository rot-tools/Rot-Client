package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PlayerAnimalPolicyTest {
    @Test
    void scopeSeparatesSelfRemoteAndEveryone() {
        assertTrue(PlayerAnimalPolicy.applies(true, "Self", 7, 7));
        assertFalse(PlayerAnimalPolicy.applies(true, "Self", 8, 7));
        assertTrue(PlayerAnimalPolicy.applies(true, "Players", 8, 7));
        assertFalse(PlayerAnimalPolicy.applies(true, "Players", 7, 7));
        assertTrue(PlayerAnimalPolicy.applies(true, "Everyone", 7, 7));
        assertTrue(PlayerAnimalPolicy.applies(true, "Everyone", 8, 7));
    }

    @Test
    void selectedTexturesHaveAdultAndBabyVariants() {
        for (String species : PlayerAnimalPolicy.SPECIES) {
            assertTrue(PlayerAnimalPolicy.texture(species, false).endsWith(".png"));
            assertTrue(PlayerAnimalPolicy.texture(species, true).contains("_baby"));
        }
    }
}
