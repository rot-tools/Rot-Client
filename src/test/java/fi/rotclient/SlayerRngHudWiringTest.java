package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerRngHudWiringTest {
    @Test
    void rngChanceIsWithheldUntilMagicFindWasActuallyObserved() throws Exception {
        String source = Files.readString(Path.of("src/client/java/fi/rotclient/SlayerRuntime.java"));

        assertTrue(source.contains("private static Integer lastMagicFind"));
        assertTrue(source.contains("lastMagicFind = null"));
        assertTrue(source.contains("Magic Find pending"));
        assertTrue(source.contains("if (lastMagicFind == null)"));
        assertTrue(source.contains("observeContainer("));
        assertTrue(source.contains("SlayerRngCatalog.resolve"));
        assertTrue(source.contains("SlayerRngMeterPolicy.chatSelection"));
        assertTrue(source.contains("activateStoredRngDrop"));
        assertTrue(source.contains("slayerRngMeterSelectedByFamily"));
        assertTrue(source.contains("currentRngFamily"));
        assertTrue(source.contains("ClientBoundaryGuard.run(\"SLAYER_CHAT\""));
        assertTrue(source.contains("ClientBoundaryGuard.call(\"SLAYER_RNG_CHAT_HIDE\""));
        assertTrue(source.contains("ClientBoundaryGuard.run(\"SLAYER_GIZMOS\""));
        assertTrue(source.contains("renderGizmosInternal"));
    }

    @Test
    void dashboardTogglesDoNotReenterTheGameMessageFilter() throws Exception {
        String client = Files.readString(Path.of("src/client/java/fi/rotclient/RotClientClient.java"));
        int notify = client.indexOf("static void notifyQolModuleToggled");
        int next = client.indexOf("private static int resetLayout", notify);
        assertTrue(notify >= 0);
        assertTrue(next > notify);
        String section = client.substring(notify, next);
        assertTrue(section.contains("addClientSystemMessage"));
        assertTrue(!section.contains("player.sendSystemMessage"));
        assertTrue(client.contains("ClientBoundaryGuard.call(\"SLAYER_ALLOW_GAME\""));
    }
}
