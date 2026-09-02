package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Source-level foraging runtime wiring. Does not assert catalog counts. */
final class ForagingWiringTest {
    @Test
    void runtimeExposesFishingStyleHooksWithoutCatalogEdits() throws Exception {
        String runtime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/ForagingRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(runtime.contains("static void tick("));
        assertTrue(runtime.contains("static void onChat("));
        assertTrue(runtime.contains("static boolean allowGameMessage("));
        assertTrue(runtime.contains("static List<String> hudLines("));
        assertTrue(runtime.contains("static boolean hudVisible("));
        assertTrue(runtime.contains("static String overlayTitle("));
        assertTrue(runtime.contains("public static void renderGizmos("));
        assertTrue(runtime.contains("static void clear("));
        assertTrue(runtime.contains("shouldHideParticle"));
        assertTrue(runtime.contains("shouldMuteSound"));
        assertTrue(runtime.contains("shouldSuppressEntity"));
        assertTrue(runtime.contains("observeSound"));
        assertTrue(runtime.contains("qol.foraging_trees"));
        assertTrue(runtime.contains("qol.foraging_audio"));
        assertTrue(runtime.contains("qol.foraging_helpers"));
        assertTrue(runtime.contains("qol.foraging_cheats"));
        assertTrue(runtime.contains("qol().isModuleEnabled"));
        assertTrue(runtime.contains("ForagingGiftTracker"));
        assertTrue(runtime.contains("hunting_esp"));
        assertTrue(runtime.contains("qol.foraging_helpers.cinderbat"));
        assertTrue(runtime.contains("qol.foraging_helpers.shard_tracker"));
        assertTrue(runtime.contains("qol.foraging_helpers.lasso_alert"));
        assertTrue(runtime.contains("moonglade_beacon"));
        assertTrue(runtime.contains("isChopLog"));
        assertFalse(runtime.contains("QolUtilityCatalog"));
        assertFalse(runtime.contains("assertEquals(107"));
    }

    @Test
    void policyKeepsBeaconAndTempleDecisionsMinecraftFree() throws Exception {
        String policy = Files.readString(Path.of(
                "src/main/java/fi/rotclient/ForagingPolicy.java"),
                StandardCharsets.UTF_8);
        assertTrue(policy.contains("nextBeaconClick"));
        assertTrue(policy.contains("forestTempleTurns"));
        assertTrue(policy.contains("colorClicks"));
        assertTrue(policy.contains("speedFromMoveTicks"));
        assertTrue(policy.contains("isChopLog"));
        assertTrue(policy.contains("HuntGlow"));
        assertTrue(policy.contains("inferIslandFromLog"));
        assertFalse(policy.contains("net.minecraft"));
    }
}
