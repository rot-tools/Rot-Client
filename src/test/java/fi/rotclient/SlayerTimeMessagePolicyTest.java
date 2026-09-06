package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class SlayerTimeMessagePolicyTest {
    @Test
    void announcesOnlyARealOwnedBossKillOnce() {
        assertTrue(SlayerTimeMessagePolicy.shouldAnnounce(
                true, true, true, true,
                SlayerPolicy.EntityRole.BOSS, 10_000L, 20_000L, 0L));
        assertFalse(SlayerTimeMessagePolicy.shouldAnnounce(
                true, true, true, true,
                SlayerPolicy.EntityRole.MINIBOSS, 10_000L, 20_000L, 0L));
        assertFalse(SlayerTimeMessagePolicy.shouldAnnounce(
                true, true, true, true,
                SlayerPolicy.EntityRole.BOSS, 0L, 20_000L, 0L));
        assertFalse(SlayerTimeMessagePolicy.shouldAnnounce(
                true, true, true, true,
                SlayerPolicy.EntityRole.BOSS, 10_000L, 20_500L, 20_000L));
        SlayerPolicy.EntityDescriptor t5PhaseOne = SlayerPolicy.classifyTag(
                "☠ Tarantula Broodfather 10M❤", "Owner: LocalPlayer").orElseThrow();
        SlayerPolicy.EntityDescriptor t5PhaseTwo = SlayerPolicy.classifyTag(
                "☠ Conjoined Brood 20M❤", "Owner: LocalPlayer").orElseThrow();
        assertFalse(SlayerTimeMessagePolicy.shouldAnnounce(
                true, true, true, true,
                SlayerPolicy.EntityRole.BOSS, 10_000L, 20_000L, 0L, t5PhaseOne));
        assertTrue(SlayerTimeMessagePolicy.shouldAnnounce(
                true, true, true, true,
                SlayerPolicy.EntityRole.BOSS, 10_000L, 20_000L, 0L, t5PhaseTwo));
    }
}
