package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PlayerSizePolicyTest {
    @Test
    void defaultScaleIsOne() {
        PlayerSizePolicy.Scale scale = PlayerSizePolicy.resolve(false, 2, 2, 2);
        assertEquals(1.0F, scale.x(), 0.0001F);
        assertEquals(1.0F, scale.y(), 0.0001F);
        assertEquals(1.0F, scale.z(), 0.0001F);
        assertTrue(scale.isIdentity());
    }

    @Test
    void independentAxisScaling() {
        PlayerSizePolicy.Scale scale = PlayerSizePolicy.resolve(
                true, 0.75F, 1.25F, 1.0F);
        assertEquals(0.75F, scale.x(), 0.0001F);
        assertEquals(1.25F, scale.y(), 0.0001F);
        assertEquals(1.0F, scale.z(), 0.0001F);
    }

    @Test
    void boundsClampSafely() {
        assertEquals(0.1F, PlayerSizePolicy.clamp(0.01F), 0.0001F);
        assertEquals(0.1F, PlayerSizePolicy.clamp(0.1F), 0.0001F);
        assertEquals(2.0F, PlayerSizePolicy.clamp(9.0F), 0.0001F);
        assertEquals(1.0F, PlayerSizePolicy.clamp(Float.NaN), 0.0001F);
    }

    @Test
    void renderOnlyPolicyDoesNotExposeGameplayGeometryChanges() {
        assertFalse(PlayerSizePolicy.exposesGameplayGeometryChanges());
    }
}
