package fi.rotclient;

import java.util.EnumMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

final class TermSimPersonalBestPolicyTest {
    @Test
    void fasterTimesPersistAsPersonalBests() {
        Map<TermSimPolicy.Kind, Integer> pbs = new EnumMap<>(TermSimPolicy.Kind.class);
        assertTrue(TermSimPersonalBestPolicy.recordPersonalBest(pbs, TermSimPolicy.Kind.PANES, 3200));
        assertFalse(TermSimPersonalBestPolicy.recordPersonalBest(pbs, TermSimPolicy.Kind.PANES, 4000));
        assertTrue(TermSimPersonalBestPolicy.recordPersonalBest(pbs, TermSimPolicy.Kind.PANES, 2100));
        String stored = TermSimPersonalBestPolicy.writePersonalBests(pbs);
        assertEquals(2100, TermSimPersonalBestPolicy.parsePersonalBests(stored).get(TermSimPolicy.Kind.PANES));
        assertTrue(TermSimPersonalBestPolicy.hubSlotName(TermSimPolicy.Kind.PANES, pbs).contains("2.10s"));
    }
}
