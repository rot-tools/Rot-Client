package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

final class RotClientChatCopyTest {
    @Test
    void normalizesActionsWithoutAddingSurpriseText() {
        assertEquals("Ready.", RotClientChatCopy.normalizedAction("  "));
        assertEquals("Storage page cached.",
                RotClientChatCopy.normalizedAction(" Storage page cached. "));
    }

    @Test
    void oneLinersAreStableAndCycleSafely() {
        String first = RotClientChatCopy.oneLiner(0);
        assertFalse(first.isBlank());
        assertEquals(first, RotClientChatCopy.oneLiner(RotClientChatCopy.oneLinerCount()));
    }
}
