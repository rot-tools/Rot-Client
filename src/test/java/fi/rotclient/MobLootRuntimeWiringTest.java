package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class MobLootRuntimeWiringTest {
    @Test
    void combatAndInventoryHooksFeedCanonicalMobRowsWithoutAreaGate()
            throws Exception {
        String client = Files.readString(Path.of(
                "src/client/java/fi/rotclient/RotClientClient.java"),
                StandardCharsets.UTF_8);
        String packetMixin = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/ClientPacketListenerMixin.java"),
                StandardCharsets.UTF_8);
        String attackMixin = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/MultiPlayerGameModeMixin.java"),
                StandardCharsets.UTF_8);

        assertTrue(attackMixin.contains("onPlayerAttack"));
        assertTrue(packetMixin.contains("handleDamageEvent"));
        assertTrue(packetMixin.contains("sourceCauseId"));
        assertTrue(packetMixin.contains("handleAddEntity"));
        assertTrue(client.contains("onSpawnedCombatProjectile"));
        assertTrue(client.contains("isLocalPlayerCombatCause"));
        assertTrue(client.contains("offerActionBarGain"));
        assertTrue(client.contains("observeMobLootOverlay"));
        assertTrue(client.contains("MobLootRareDropParser"));
        assertTrue(client.contains("MagicFindParser"));
        assertTrue(client.contains("MAGIC_FIND_DETECTOR"));
        assertTrue(client.contains("SessionSourceType.MOB"));
        assertTrue(client.contains("SkyBlockAreaDetector.detect()"));
        assertTrue(client.contains("offerSackGain"));
        assertTrue(client.contains("offerCoinGain"));
        assertTrue(client.contains("MobLootCoinParser"));
        assertFalse(client.contains("MOB_LOOT_ALLOWED_AREAS"));
        assertFalse(client.contains("DIANA_IN_SCOPE = false"));
    }
}
