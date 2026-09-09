package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class AutoClickerWiringTest {
    @Test
    void catalogListsRuntimeReadyAutoClicker() throws Exception {
        String catalog = Files.readString(Path.of(
                "src/plus/java/fi/rotclient/QolPlusCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains("qol.auto_clicker"));
        assertTrue(catalog.contains("Auto Clicker"));
    }

    @Test
    void clientTickRegistersAutoClickerRuntime() throws Exception {
        String source = Files.readString(Path.of(
                "src/plusClient/java/fi/rotclient/RotClientPlusHooks.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("AUTO_CLICKER"));
        assertTrue(source.contains("AutoClickerRuntime.tick"));
    }

    @Test
    void rotclientCommandTreeIncludesAutoClickerWhitelist() throws Exception {
        String source = Files.readString(Path.of(
                "src/client/java/fi/rotclient/RotClientClient.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("literal(\"autoclicker\")"));
        assertTrue(source.contains("autoClickerAdd"));
        assertTrue(source.contains("autoClickerList"));
    }

    @Test
    void catalogExposesClickerStyleUtilities() throws Exception {
        String catalog = Files.readString(Path.of(
                "src/plus/java/fi/rotclient/QolPlusCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains("qol.inventory_walk"));
        assertTrue(catalog.contains("qol.secret_hitboxes"));
        String shared = Files.readString(Path.of(
                "src/main/java/fi/rotclient/QolUtilityCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(shared.contains("qol.trajectories"));
        assertTrue(shared.contains("qol.world_scanner"));
        assertTrue(shared.contains("\"qol.command_keybinds\""));
    }

    @Test
    void mixinsRegisterSecretHitboxesAndInventoryWalk() throws Exception {
        String json = Files.readString(Path.of(
                "src/plusClient/resources/rotclient.plus.mixins.json"),
                StandardCharsets.UTF_8);
        assertTrue(json.contains("ClipContextSecretHitboxMixin"));
        assertTrue(json.contains("BlockStateSecretHitboxMixin"));
        assertTrue(json.contains("ConnectionInventoryWalkMixin"));
        assertTrue(json.contains("MouseHandlerInventoryWalkMixin"));
        String sharedMixins = Files.readString(Path.of(
                "src/client/resources/rotclient.client.mixins.json"),
                StandardCharsets.UTF_8);
        assertTrue(sharedMixins.contains("KeyMappingAccessor"));
        assertTrue(sharedMixins.contains("AbstractContainerScreenMenuKeybindMixin"));
        assertTrue(sharedMixins.contains("PackSelectionModelMixin"));
    }

    @Test
    void commandKeybindsUseHypixelArmorAndLoadoutCommands() throws Exception {
        assertEquals("armor", RingPolicy.ARMOR_COMMAND);
        assertEquals("loadout", RingPolicy.LOADOUT_COMMAND);
        assertFalse("wardrobe".equals(RingPolicy.ARMOR_COMMAND));
        assertFalse("equipment".equals(RingPolicy.LOADOUT_COMMAND));
        String source = Files.readString(Path.of(
                "src/client/java/fi/rotclient/RingKeybindsRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("commandArmorWardrobeKey"));
        assertTrue(source.contains("commandLoadoutsKey"));
        assertTrue(source.contains("SkyBlockAreaDetector.isInSkyblock()"));
        String json = Files.readString(Path.of(
                "src/client/resources/rotclient.client.mixins.json"),
                StandardCharsets.UTF_8);
        assertTrue(json.contains("KeyboardHandlerRingMixin"));
        assertTrue(json.contains("MouseHandlerRingMixin"));
    }

    @Test
    void inventoryWalkHooksClickKeepaliveAndSlotAck() throws Exception {
        String mixin = Files.readString(Path.of(
                "src/plusClient/java/fi/rotclient/mixin/ConnectionInventoryWalkMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(mixin.contains("ChannelFutureListener;Z)V"));
        assertTrue(mixin.contains("channelRead0"));
        assertTrue(mixin.contains("ClientboundKeepAlivePacket"));
        assertTrue(mixin.contains("ClientboundPingPacket"));
        assertTrue(mixin.contains("ServerboundContainerClickPacket"));
        assertFalse(mixin.contains("ClientboundContainerSetSlotPacket"));
        assertTrue(mixin.contains("ServerboundKeepAlivePacket"));
        assertTrue(mixin.contains("CustomResourcePackRuntime.isReloading"));
    }

    @Test
    void autoClickerPulsesAttackAndUseKeyMappings() throws Exception {
        String source = Files.readString(Path.of(
                "src/plusClient/java/fi/rotclient/AutoClickerRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("KeyMapping.click"));
        assertTrue(source.contains("keyAttack"));
        assertTrue(source.contains("keyUse"));
        assertTrue(source.contains("pulseClick"));
        assertTrue(source.contains("syntheticAttackHeld"));
        assertTrue(source.contains("holdAttackForBlockBreaking"));
    }

    @Test
    void autoClickerTerminatorOnlyLeftClicks() throws Exception {
        String source = Files.readString(Path.of(
                "src/plusClient/java/fi/rotclient/AutoClickerRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("shouldTerminatorLeftClick"));
        assertTrue(source.contains("performLeftClick"));
        assertTrue(source.contains("autoClickerTerminatorOnly"));
    }

    @Test
    void autoClickerTicksOnStartClientTick() throws Exception {
        String source = Files.readString(Path.of(
                "src/plusClient/java/fi/rotclient/RotClientPlusHooks.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("START_CLIENT_TICK")
                || source.contains("AutoClickerRuntime.tick"));
        assertTrue(source.contains("AutoClickerRuntime.tick"));
    }

    @Test
    void worldScannerQueuesLoadedChunksInsteadOfFullRescan() throws Exception {
        String policy = Files.readString(Path.of(
                "src/main/java/fi/rotclient/WorldScannerPolicy.java"),
                StandardCharsets.UTF_8);
        assertTrue(policy.contains("shouldRescanLoadedChunks"));
        assertTrue(policy.contains("SCAN_CHUNKS_PER_TICK"));
        String runtime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/WorldScannerRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(runtime.contains("drainQueue"));
        assertTrue(runtime.contains("billboardTextOverBlock"));
        assertTrue(runtime.contains("formatEspLabel"));
        assertTrue(runtime.contains("Gizmos.arrow"));
        assertTrue(runtime.contains("maybeHas"));
    }

    @Test
    void trajectoriesDrawLinesNotSegmentCuboids() throws Exception {
        String source = Files.readString(Path.of(
                "src/client/java/fi/rotclient/TrajectoryRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("Gizmos.line"));
        assertTrue(source.contains("getGameTimeDeltaPartialTick"));
        assertTrue(source.contains("ClipContext"));
        assertTrue(source.contains("interpolatedEye"));
        assertFalse(source.contains("GizmoStyle.stroke("));
        assertFalse(source.contains("Math.min(a.x(), b.x())"));
    }
}
