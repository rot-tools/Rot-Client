package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class HudNumbersTest {
    @Test
    void compactCountStaysShortAcrossMagnitudes() {
        assertEquals("0", HudNumbers.compactCount(0));
        assertEquals("999", HudNumbers.compactCount(999));
        assertEquals("1k", HudNumbers.compactCount(1_000));
        assertEquals("1.2k", HudNumbers.compactCount(1_234));
        assertEquals("9.9k", HudNumbers.compactCount(9_949));
        assertEquals("12k", HudNumbers.compactCount(12_345));
        assertEquals("999k", HudNumbers.compactCount(999_999));
        assertEquals("1m", HudNumbers.compactCount(1_000_000));
        assertEquals("1.2m", HudNumbers.compactCount(1_234_567));
        assertEquals("12m", HudNumbers.compactCount(12_345_678));
        assertEquals("123m", HudNumbers.compactCount(123_456_789));
        assertEquals("1.2b", HudNumbers.compactCount(1_234_567_890L));
    }

    @Test
    void everyValueBelowABillionFitsInFourCharacters() {
        long[] samples = {1, 9, 99, 999, 1_000, 9_999, 10_000, 99_999,
                100_000, 999_999, 1_000_000, 9_999_999, 10_000_000,
                999_999_999};
        for (long sample : samples) {
            assertTrue(HudNumbers.compactCount(sample).length() <= 4,
                    sample + " -> " + HudNumbers.compactCount(sample));
        }
    }
}
