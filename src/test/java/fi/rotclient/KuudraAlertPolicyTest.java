package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class KuudraAlertPolicyTest {
    @Test
    void ownFreshMatchesPerkChatAndStartsATenSecondHud() {
        assertTrue(KuudraAlertPolicy.isOwnFresh(
                "§aYour Fresh Tools Perk bonus doubles your building speed for the next 10 seconds!"));
        assertFalse(KuudraAlertPolicy.isOwnFresh("Kuudra grows tired!"));
        long now = 1_000_000L;
        KuudraAlertPolicy.Snapshot next = KuudraAlertPolicy.applyOwnFresh(
                KuudraAlertPolicy.Snapshot.idle(), now);
        List<String> lines = KuudraAlertPolicy.hudLines(next, now, true, false, false);
        assertEquals(List.of("Fresh 10.0s"), lines);
        assertEquals(List.of("Fresh 4.5s"), KuudraAlertPolicy.hudLines(next, now + 5_500L, true, false, false));
        assertTrue(KuudraAlertPolicy.hudLines(next, now + 10_001L, true, false, false).isEmpty());
    }

    @Test
    void partyFreshTracksTeammatesAndBuildProgress() {
        KuudraAlertPolicy.Snapshot state = KuudraAlertPolicy.applyPartyFresh(
                KuudraAlertPolicy.Snapshot.idle(),
                KuudraAlertPolicy.partyFreshName("Party > [MVP+] Alice: FRESH").orElse(""),
                2_000L);
        assertEquals("Alice", KuudraAlertPolicy.partyEntries(state, 2_000L).get(0).name());
        assertTrue(KuudraAlertPolicy.partyFreshName("Party > Bob: hello").isEmpty());

        state = KuudraAlertPolicy.applyBuild(state, KuudraAlertPolicy.parseBuild(
                "Building Progress 47% (3 Players Helping)").orElseThrow());
        assertEquals(List.of("Alice 10.0s", "Build 47% · 3"),
                KuudraAlertPolicy.hudLines(state, 2_000L, false, true, true));
        assertEquals(23, KuudraAlertPolicy.parseBuild("PROGRESS: 23%").orElseThrow().percent());
        assertEquals(81, KuudraAlertPolicy.parseBuild("Protect Elle (81%)").orElseThrow().percent());
    }

    @Test
    void phaseTitlesAndNoPreStayLocal() {
        assertEquals("Supplies", KuudraAlertPolicy.titleForPhase(IotaKuudraPolicy.Phase.SUPPLIES));
        assertEquals("Build", KuudraAlertPolicy.titleForPhase(IotaKuudraPolicy.Phase.BUILD));
        assertEquals("Stun", KuudraAlertPolicy.titleForPhase(IotaKuudraPolicy.Phase.STUN));
        assertEquals("Tired", KuudraAlertPolicy.titleForPhase(IotaKuudraPolicy.Phase.SKIP));
        assertEquals("Kuudra Down", KuudraAlertPolicy.titleForPhase(IotaKuudraPolicy.Phase.COMPLETED));
        assertEquals("", KuudraAlertPolicy.titleForPhase(IotaKuudraPolicy.Phase.NONE));
        assertEquals("No Pre · Square", KuudraAlertPolicy.noPreTitle("Square"));
        assertTrue(KuudraAlertPolicy.hudEnabled(true, true, false, false));
        assertFalse(KuudraAlertPolicy.hudEnabled(true, false, false, false));
        assertFalse(KuudraAlertPolicy.hudEnabled(false, true, true, true));
        assertEquals("FRESH", KuudraAlertPolicy.PARTY_FRESH);
    }
}
