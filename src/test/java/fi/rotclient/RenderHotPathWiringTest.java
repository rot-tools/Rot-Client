package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RenderHotPathWiringTest {
    @Test
    void nameHiderCachesContextPerTick() throws Exception {
        String runtime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/NameHiderRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(runtime.contains("static void beginTick()"));
        assertTrue(runtime.contains("cachedGeneration == tickGeneration"));
        String client = Files.readString(Path.of(
                "src/client/java/fi/rotclient/RotClientClient.java"),
                StandardCharsets.UTF_8);
        assertTrue(client.contains("NameHiderRuntime.beginTick()"));
        assertTrue(client.contains("QolVisualRuntime.beginTick()"));
    }

    @Test
    void entitySuppressSkipsDisabledModules() throws Exception {
        String runtime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/QolVisualRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(runtime.contains("if (!flags.any)"));
        assertTrue(runtime.contains("flags.fishing && FishingSuiteRuntime.shouldSuppressEntity"));
        assertTrue(runtime.contains("record SuppressFlags("));
    }

    @Test
    void playerDisplayDoesNotScanEveryTabName() throws Exception {
        String runtime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/InventoryChromeRuntime.java"),
                StandardCharsets.UTF_8);
        assertFalse(runtime.contains("getOnlinePlayers"));
        assertTrue(runtime.contains("rotclient$getHeader"));
        assertTrue(runtime.contains("rotclient$getFooter"));
        assertTrue(runtime.contains("DisplaySlot.SIDEBAR"));
    }

    @Test
    void dungeonEspUsesOneLivingPass() throws Exception {
        String runtime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/DungeonRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(runtime.contains("needsNamedDungeonEsp"));
        assertTrue(runtime.contains("considerNamedEsp"));
        assertFalse(runtime.contains("named.addAll(client.level.getEntitiesOfClass(ArmorStand.class"));
    }
}
