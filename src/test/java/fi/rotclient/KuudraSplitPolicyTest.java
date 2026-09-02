package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KuudraSplitPolicyTest {
    @Test
    void chatAdvancesSupplyThenKillWithLocalPb() {
        long t0 = 2_000_000L;
        var start = KuudraSplitPolicy.apply(
                null, KuudraSplitPolicy.eventFromChat("[NPC] Elle: I will go and fish up Kuudra!"), t0);
        assertEquals(KuudraSplitPolicy.Phase.SUPPLY, start.active());
        var build = KuudraSplitPolicy.apply(
                start, KuudraSplitPolicy.eventFromChat("Building Progress 40% (2 Players Helping)"), t0 + 30_000L);
        assertEquals(KuudraSplitPolicy.Phase.BUILD, build.active());
        assertEquals(30_000L, build.times().get(KuudraSplitPolicy.Phase.SUPPLY));
        var kill = KuudraSplitPolicy.apply(
                build, KuudraSplitPolicy.eventFromChat("  KUUDRA DOWN!"), t0 + 90_000L);
        assertTrue(kill.finished());
        Map<String, Long> pbs = new LinkedHashMap<>();
        assertTrue(KuudraSplitPolicy.recordPersonalBest(pbs, KuudraSplitPolicy.Phase.KILL, 90_000L));
        assertFalse(KuudraSplitPolicy.recordPersonalBest(pbs, KuudraSplitPolicy.Phase.KILL, 95_000L));
        List<String> hud = KuudraSplitPolicy.hudLines(kill, t0 + 90_000L, pbs);
        assertTrue(hud.stream().anyMatch(line -> line.startsWith("Kuudra")));
        assertTrue(hud.stream().anyMatch(line -> line.contains("Kill") && line.contains("PB")));
    }
}
