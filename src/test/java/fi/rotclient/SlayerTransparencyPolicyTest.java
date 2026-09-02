package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerTransparencyPolicyTest {
    @Test
    void onlyFadesBackgroundMobsDuringAVerifiedFight() {
        SlayerTransparencyPolicy.Options options = new SlayerTransparencyPolicy.Options(true, 35, false);

        assertTrue(SlayerTransparencyPolicy.shouldFade(options, true, false, false, false));
        assertFalse(SlayerTransparencyPolicy.shouldFade(options, false, false, false, false));
        assertFalse(SlayerTransparencyPolicy.shouldFade(options, true, true, false, false));
        assertFalse(SlayerTransparencyPolicy.shouldFade(options, true, false, true, false));
    }

    @Test
    void onlyIncludesOtherPlayersWhenExplicitlyEnabled() {
        SlayerTransparencyPolicy.Options mobsOnly = new SlayerTransparencyPolicy.Options(true, 35, false);
        SlayerTransparencyPolicy.Options includePlayers = new SlayerTransparencyPolicy.Options(true, 35, true);

        assertFalse(SlayerTransparencyPolicy.shouldFade(mobsOnly, true, false, false, true));
        assertTrue(SlayerTransparencyPolicy.shouldFade(includePlayers, true, false, false, true));
    }

    @Test
    void clampsStrengthAndDerivesOpacity() {
        SlayerTransparencyPolicy.Options low = new SlayerTransparencyPolicy.Options(true, 0, false);
        SlayerTransparencyPolicy.Options high = new SlayerTransparencyPolicy.Options(true, 100, false);

        assertEquals(15, low.strengthPercent());
        assertEquals(70, high.strengthPercent());
        assertEquals(85, SlayerTransparencyPolicy.opacityPercent(low));
        assertEquals(30, SlayerTransparencyPolicy.opacityPercent(high));
    }
}
