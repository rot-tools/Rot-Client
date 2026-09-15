package fi.rotclient;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class FishingHotspotRadarPolicyTest {
    @Test
    void stillParticlesProduceBoundedDirectionalGuess() {
        assertTrue(FishingHotspotRadarPolicy.isRadarFlame("minecraft:flame", 0, 0, 0));
        assertFalse(FishingHotspotRadarPolicy.isRadarFlame("minecraft:flame", 1, 0, 0));
        var trail = List.<FishingHotspotPolicy.Point>of();
        for (int i = 0; i < 30; i++) {
            trail = FishingHotspotRadarPolicy.pushRadar(trail,
                    new FishingHotspotPolicy.Point(i, 64, 0));
        }
        assertEquals(FishingHotspotRadarPolicy.MAX_RADAR_POINTS, trail.size());
        var guess = FishingHotspotRadarPolicy.guess(trail);
        assertNotNull(guess);
        assertTrue(guess.dx() > 0);
    }
}
