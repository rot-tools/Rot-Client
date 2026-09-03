package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityLerpPolicyTest {
    @Test
    void renderOffsetSitsOnThePreviousTickAtZeroAndTheCurrentTickAtOne() {
        EntityLerpPolicy.Offset start = EntityLerpPolicy.renderOffset(
                10.0D, 20.0D, 30.0D,
                0.0D, 0.0D, 0.0D,
                0.0F);
        assertEquals(-10.0D, start.x(), 0.0001D);
        assertEquals(-20.0D, start.y(), 0.0001D);
        assertEquals(-30.0D, start.z(), 0.0001D);

        EntityLerpPolicy.Offset end = EntityLerpPolicy.renderOffset(
                10.0D, 20.0D, 30.0D,
                0.0D, 0.0D, 0.0D,
                1.0F);
        assertEquals(0.0D, end.x(), 0.0001D);
        assertEquals(0.0D, end.y(), 0.0001D);
        assertEquals(0.0D, end.z(), 0.0001D);

        EntityLerpPolicy.Offset mid = EntityLerpPolicy.renderOffset(
                10.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 0.0D,
                0.5F);
        assertEquals(-5.0D, mid.x(), 0.0001D);
    }
}
