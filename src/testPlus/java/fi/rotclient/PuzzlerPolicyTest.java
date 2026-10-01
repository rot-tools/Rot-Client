package fi.rotclient;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PuzzlerPolicyTest {
    @Test void arrowsUseTheNpcOrientation() {
        assertEquals(new PuzzlerPolicy.Target(180, 195, 136), PuzzlerPolicy.solve("§e[NPC] Puzzler: ▲▶").orElseThrow());
        assertEquals(new PuzzlerPolicy.Target(181, 195, 135), PuzzlerPolicy.solve("[NPC] Puzzler: ▲▶▼◀").orElseThrow());
    }
    @Test void rejectsOtherChatAndMixedSequences() {
        assertTrue(PuzzlerPolicy.solve("Someone: ▲▶").isEmpty());
        assertTrue(PuzzlerPolicy.solve("[NPC] Puzzler: Wrong!").isEmpty());
        assertTrue(PuzzlerPolicy.solve("[NPC] Puzzler: ▲oops▶").isEmpty());
    }
}
