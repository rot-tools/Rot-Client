package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class QolChatSlotWaypointWiringTest {
    @Test
    void catalogMarksChatSlotAndWaypointRuntimeReady() throws Exception {
        String catalog = Files.readString(Path.of(
                "src/main/java/fi/rotclient/QolUtilityCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains("\"qol.chat_commands\""));
        assertTrue(catalog.contains("\"qol.slot_binds\""));
        assertTrue(catalog.contains("\"qol.waypoints\""));
        assertTrue(catalog.contains("Look Target"));
    }

    @Test
    void clientTicksAndChatHooksAreRegistered() throws Exception {
        String source = Files.readString(Path.of(
                "src/client/java/fi/rotclient/RotClientClient.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("CHAT_COMMANDS"));
        assertTrue(source.contains("ChatCommandsRuntime.tick"));
        assertTrue(source.contains("WAYPOINTS"));
        assertTrue(source.contains("WaypointRuntime.tick"));
        assertTrue(source.contains("SLOT_BINDS"));
        assertTrue(source.contains("ChatCommandsRuntime.onGameMessage"));
        assertTrue(source.contains("WaypointRuntime.onGameMessage"));
    }

    @Test
    void mixinsWireSlotBindsEmotesAndWaypointGizmos() throws Exception {
        String json = Files.readString(Path.of(
                "src/client/resources/rotclient.client.mixins.json"),
                StandardCharsets.UTF_8);
        assertTrue(json.contains("AbstractContainerScreenSlotBindMixin"));

        String overlay = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/AbstractContainerScreenInventoryOverlayMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(overlay.contains("SlotBindsRuntime.render"));

        String chat = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/ClientPacketListenerMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(chat.contains("ChatCommandsRuntime.modifyOutgoing"));
        assertTrue(chat.contains("ChatCommandsRuntime.outgoingCommand"));
        assertTrue(chat.contains("ChatCommandsRuntime.applyEmotes"));
        assertTrue(chat.contains("sendChat"));
        assertTrue(chat.contains("MiningAssistRuntime.shouldIgnoreUpdate"));

        String drop = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/LocalPlayerMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(drop.contains("ItemProtectRuntime.shouldBlockDrop"));
        String slots = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/AbstractContainerScreenSlotBindMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(slots.contains("ItemProtectRuntime.shouldBlockSlotClick"));
        String mine = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/MultiPlayerGameModeMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(mine.contains("MiningAssistRuntime.startMining"));

        String gizmos = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/LevelRendererEtherwarpMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(gizmos.contains("renderWaypointGizmos"));
    }

    @Test
    void runtimesKeepMinecraftBridgesOutOfPolicy() throws Exception {
        String chat = Files.readString(Path.of(
                "src/client/java/fi/rotclient/ChatCommandsRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(chat.contains("sendCommand"));
        assertTrue(chat.contains("PROCESS_DELAY_TICKS"));

        String slots = Files.readString(Path.of(
                "src/client/java/fi/rotclient/SlotBindsRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(slots.contains("ContainerInput.SWAP"));
        assertTrue(slots.contains("ContainerInput.QUICK_MOVE"));

        String waypoints = Files.readString(Path.of(
                "src/client/java/fi/rotclient/WaypointRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(waypoints.contains("EtherwarpPredictor.predict"));
        assertTrue(waypoints.contains("Gizmos.cuboid"));
    }
}
