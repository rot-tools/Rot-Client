package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerMinibossAlertPolicyTest {
    @Test
    void readsTheLocalPlayersHypixelSpawnChatOnce() {
        assertTrue(SlayerMinibossAlertPolicy.isSpawnChat(
                "§cSLAYER MINI-BOSS §5Voidling Devotee §chas spawned!"));
        SlayerPolicy.EntityDescriptor mini = SlayerMinibossAlertPolicy.spawnFromChat(
                "SLAYER MINI-BOSS Voidling Devotee has spawned!").orElseThrow();
        assertEquals(SlayerPolicy.EntityRole.MINIBOSS, mini.role());
        assertEquals(SlayerPolicy.SlayerType.VOIDGLOOM, mini.type());
        assertEquals("Voidling Devotee", mini.displayName());
        assertFalse(mini.bigMiniboss());

        SlayerPolicy.EntityDescriptor big = SlayerMinibossAlertPolicy.spawnFromChat(
                "SLAYER MINI-BOSS Voidcrazed Maniac has spawned!").orElseThrow();
        assertTrue(big.bigMiniboss());

        assertTrue(SlayerMinibossAlertPolicy.shouldAnnounce(true, true, 2_000L, 0L));
        assertFalse(SlayerMinibossAlertPolicy.shouldAnnounce(
                true, true, 3_500L, 2_000L));
        assertFalse(SlayerMinibossAlertPolicy.shouldAnnounce(true, false, 2_000L, 0L));
        assertFalse(SlayerMinibossAlertPolicy.isSpawnChat("SLAYER QUEST STARTED!"));
        assertFalse(SlayerMinibossAlertPolicy.isSpawnChat("Voidling Devotee"));
    }

    @Test
    void nearbyForeignMinibossHostsNeverAlert() {
        assertFalse(SlayerMinibossAlertPolicy.shouldAnnounceNearbyHost(false, true));
        assertFalse(SlayerMinibossAlertPolicy.shouldAnnounceNearbyHost(true, true));
        assertFalse(SlayerMinibossAlertPolicy.shouldAnnounceNearbyHost(false, false));
    }
}
