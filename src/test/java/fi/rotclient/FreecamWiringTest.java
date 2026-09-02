package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class FreecamWiringTest {
    @Test
    void catalogListsRuntimeReadyFreecam() throws Exception {
        String catalog = Files.readString(Path.of(
                "src/main/java/fi/rotclient/QolUtilityCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains("qol.freecam"));
        assertTrue(catalog.contains("Free Camera"));
        QolUtilityCatalog.ModuleDef module = QolUtilityCatalog.findById("qol.freecam");
        assertNotNull(module);
        assertTrue(module.runtimeReady());
        assertTrue(module.toggleable());
        assertTrue(module.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.freecam.speed")));
        assertTrue(module.searchAliases().contains("cheat"));
    }

    @Test
    void clientTickRegistersFreecamRuntime() throws Exception {
        String source = Files.readString(Path.of(
                "src/client/java/fi/rotclient/RotClientClient.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("FREECAM"));
        assertTrue(source.contains("FreecamRuntime.tick"));
    }

    @Test
    void mixinsRegisterFreecamHooks() throws Exception {
        String json = Files.readString(Path.of(
                "src/client/resources/rotclient.client.mixins.json"),
                StandardCharsets.UTF_8);
        assertTrue(json.contains("CameraFreecamMixin"));
        assertTrue(json.contains("KeyboardInputFreecamMixin"));
        assertTrue(json.contains("MouseHandlerFreecamMixin"));
        assertTrue(json.contains("ConnectionFreecamMixin"));
        String camera = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/CameraFreecamMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(camera.contains("method = \"alignWithEntity\""));
        assertTrue(camera.contains("setPosition"));
        assertTrue(camera.contains("detached = true"));
        String packets = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/ConnectionFreecamMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(packets.contains("ChannelFutureListener;Z)V"));
        assertTrue(packets.contains("shouldBlockOutbound"));
        String input = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/KeyboardInputFreecamMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(input.contains("moveVector"));
        assertTrue(input.contains("extends ClientInput"));
        assertFalse(input.contains("@Shadow"));
        assertFalse(input.contains("PlayerMoveC2S"));
    }
}
