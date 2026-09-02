package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class InfoTooltipsPolicyTest {
    @Test
    void qualityAndIdLinesMatchPlainFormat() {
        InfoTooltipsPolicy.Snapshot snapshot = new InfoTooltipsPolicy.Snapshot(
                "HYPERION", 50, 7, 1_700_000_000_000L, true, true, "#ff00aa");
        List<String> lines = InfoTooltipsPolicy.lines(
                true, true, true, true, true, true, snapshot);
        assertEquals(5, lines.size());
        assertTrue(lines.get(0).contains("Quality:"));
        assertTrue(lines.get(0).contains("50/50"));
        assertTrue(lines.get(1).contains("Created:"));
        assertTrue(lines.get(2).contains("#ff00aa"));
        assertTrue(lines.get(3).contains("Donated"));
        assertTrue(lines.get(4).contains("HYPERION"));
    }

    @Test
    void disabledModuleProducesNothing() {
        assertTrue(InfoTooltipsPolicy.lines(
                false, true, true, true, true, true,
                new InfoTooltipsPolicy.Snapshot("X", 10, 1, 1L, false, true, "#fff")).isEmpty());
    }
}
