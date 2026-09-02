package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class RotClientColorPickerPerfTest {
    @Test
    void hueStripIsBuiltOnceAndSvCacheInvalidatesOnHueChange() {
        assertTrue(RotClientColorPickerScreen.hueStripReadyForTests());
        RotClientColorPickerScreen.ensureSvCacheForTests(10.0F);
        int keyA = RotClientColorPickerScreen.svCacheHueKeyForTests();
        RotClientColorPickerScreen.ensureSvCacheForTests(10.4F);
        assertEquals(keyA, RotClientColorPickerScreen.svCacheHueKeyForTests());
        RotClientColorPickerScreen.ensureSvCacheForTests(25.0F);
        assertEquals(25, RotClientColorPickerScreen.svCacheHueKeyForTests());
    }

    @Test
    void hsvRgbRoundTripAndClampsRemainStable() {
        int color = RotClientColorMath.toArgb(12.0F, 0.8F, 0.6F, 0xFF);
        RotClientColorMath.Hsv hsv = RotClientColorMath.fromArgb(color);
        int rebuilt = RotClientColorMath.toArgb(hsv.h(), hsv.s(), hsv.v(), 0xFF);
        assertEquals((color >> 16) & 0xFF, (rebuilt >> 16) & 0xFF, 3);
        assertEquals(0, RotClientAppearanceConfig.clamp(-5, 0, 255));
        assertEquals(255, RotClientAppearanceConfig.clamp(999, 0, 255));
        assertEquals(
                0x80E33B3B,
                RotClientAppearanceConfig.withAlpha(0xFFE33B3B, 0x80));
    }

    @Test
    void liveCallbackFiresWithoutPersistenceHook() {
        AtomicInteger liveCount = new AtomicInteger();
        AtomicInteger applyCount = new AtomicInteger();
        IntLive live = ignored -> liveCount.incrementAndGet();
        IntLive apply = ignored -> applyCount.incrementAndGet();
        live.accept(1);
        apply.accept(1);
        assertEquals(1, liveCount.get());
        assertEquals(1, applyCount.get());
        assertFalse(RotClientAppearanceStore.FILE_NAME.isBlank());
    }

    @FunctionalInterface
    private interface IntLive {
        void accept(int value);
    }
}
