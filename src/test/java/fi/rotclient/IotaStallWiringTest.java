package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class IotaStallWiringTest {
    @Test
    void catalogListsRuntimeReadyIotaAndStall() {
        QolUtilityCatalog.ModuleDef iota = QolUtilityCatalog.findById("qol.iota");
        assertNotNull(iota);
        assertEquals(QolUtilityCatalog.Group.KUUDRA, iota.group());
        assertTrue(iota.runtimeReady());
        assertTrue(iota.toggleable());
        assertTrue(iota.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.iota.supply_waypoints")));
        assertTrue(iota.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.iota.etherwarp_helper")));
        assertTrue(iota.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.iota.fresh_tools")));
        assertTrue(iota.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.iota.fresh_announce")));
        assertTrue(iota.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.iota.kuudra_titles")));
        QolUtilityCatalog.ModuleDef market = QolUtilityCatalog.findById("qol.stall_market");
        assertNotNull(market);
        assertTrue(market.runtimeReady());
        assertTrue(market.settings().stream().anyMatch(setting ->
                setting.id().equals("qol.stall_market.bazaar_search")));
    }

    @Test
    void clientWiresIotaAndStallRuntimes() throws Exception {
        String source = Files.readString(Path.of(
                "src/client/java/fi/rotclient/RotClientClient.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("IOTA"));
        assertTrue(source.contains("IotaRuntime.tick"));
        assertTrue(source.contains("IotaRuntime.onGameMessage"));
        assertTrue(source.contains("IotaRuntime.consumedPartyCommand"));
        assertTrue(source.contains("IotaRuntime.shouldMuteTerminatorChat"));
        assertTrue(source.contains("IotaKuudraRuntime.onTitle"));
        String kuudra = Files.readString(Path.of(
                "src/client/java/fi/rotclient/IotaKuudraRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(kuudra.contains("KuudraAlertPolicy"));
        assertTrue(kuudra.contains("iotaFreshTools"));
        assertTrue(kuudra.contains("iotaKuudraTitles"));
        assertTrue(source.contains("STALL_MARKET"));
        assertTrue(source.contains("StallMarketRuntime.tick"));
        assertTrue(source.contains("StallMarketRuntime.onScreenOpened"));
        assertTrue(source.contains("StallMarketRuntime.appendTooltip"));
        assertTrue(source.contains("literal(\"bazaarsearch\")"));
        String clicker = Files.readString(Path.of(
                "src/client/java/fi/rotclient/AutoClickerRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(clicker.contains("IotaRuntime.leftClickLatched"));
        assertTrue(clicker.contains("IotaRuntime.rightClickLatched"));
    }

    @Test
    void mixinsRegisterIotaAndStallHooks() throws Exception {
        String json = Files.readString(Path.of(
                "src/client/resources/rotclient.client.mixins.json"),
                StandardCharsets.UTF_8);
        assertTrue(json.contains("FishingHookIotaMixin"));
        assertTrue(json.contains("AbstractContainerScreenAccessor"));
        String packets = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/ClientPacketListenerMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(packets.contains("handleSoundEvent"));
        assertTrue(packets.contains("IotaRuntime.shouldMuteFishingCast"));
        assertTrue(packets.contains("IotaRuntime.shouldMuteTerminatorSound"));
        String hook = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/FishingHookIotaMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(hook.contains("onSyncedDataUpdated"));
        assertTrue(hook.contains("shouldFixFishingHook"));
        String mouse = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/AbstractContainerScreenMenuKeybindMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(mouse.contains("StallMarketRuntime.shouldBlockClick"));
        String player = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/LocalPlayerMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(player.contains("openTextEdit"));
        assertTrue(player.contains("fillPendingBazaarSign"));
        String overlay = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/AbstractContainerScreenInventoryOverlayMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(overlay.contains("StallMarketRuntime.shouldBlockClick"));
        String sounds = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/SoundEngineSlayerMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(sounds.contains("IotaRuntime.shouldMuteSound"));
        assertTrue(sounds.contains("sound.getSound() == null"));
        assertTrue(sounds.contains("SoundInstance;resolve"));
        String gizmos = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/LevelRendererEtherwarpMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(gizmos.contains("IotaKuudraRuntime.renderGizmos"));
        String titles = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/HudActionBarFilterMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(titles.contains("IotaKuudraRuntime.onTitle"));
        assertTrue(titles.contains("rotclient$hideNameInSubtitle"));
        String sidebar = Files.readString(Path.of(
                "src/client/java/fi/rotclient/SkyBlockSidebar.java"),
                StandardCharsets.UTF_8);
        assertTrue(sidebar.contains("getDisplayName()"));
        String slotClick = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/AbstractContainerScreenInventoryOverlayMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(slotClick.contains("StallMarketRuntime.controlHeld()"));
        String kuudra = Files.readString(Path.of(
                "src/client/java/fi/rotclient/IotaKuudraRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(kuudra.contains("recordKuudraRun"));
        assertTrue(kuudra.contains("onChestSlotClicked"));
        assertTrue(kuudra.contains("confirmChest"));
        assertTrue(kuudra.contains("applyReroll"));
        assertTrue(kuudra.contains("expireProfitSession"));
        assertTrue(kuudra.contains("iotaSessionDurationMs"));
        assertTrue(kuudra.contains("IotaKuudraProfitPolicy"));
        assertTrue(kuudra.contains("livePrices"));
        assertTrue(kuudra.contains("KISMET_FEATHER"));
        assertTrue(slotClick.contains("IotaKuudraRuntime.onChestSlotClicked"));
    }

    @Test
    void runtimesKeepMinecraftBridgesOutOfPolicy() throws Exception {
        String iota = Files.readString(Path.of(
                "src/client/java/fi/rotclient/IotaRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(iota.contains("IotaPolicy.partyCommand"));
        assertTrue(iota.contains("IotaKuudraRuntime.onGameMessage"));
        assertTrue(iota.contains("TOGGLE_CLICK_CPS"));
        assertTrue(iota.contains("SkyBlockAreaDetector.isInSkyblock()"));
        String market = Files.readString(Path.of(
                "src/client/java/fi/rotclient/StallMarketRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(market.contains("fillPendingBazaarSign"));
        assertTrue(market.contains("StallMarketPolicy.sellProtection"));
        assertTrue(market.contains("sendCommand(\"bz\")"));
        assertTrue(market.contains("rotclient$hoveredSlot"));
        assertTrue(market.contains("controlHeld()"));
        assertFalse(market.contains("getDeclaredField(\"hoveredSlot\")"));
        String policy = Files.readString(Path.of(
                "src/main/java/fi/rotclient/IotaPolicy.java"),
                StandardCharsets.UTF_8);
        assertTrue(policy.contains("Party >"));
        assertTrue(!policy.contains("net.minecraft"));
        String kuudraPolicy = Files.readString(Path.of(
                "src/main/java/fi/rotclient/IotaKuudraPolicy.java"),
                StandardCharsets.UTF_8);
        assertTrue(!kuudraPolicy.contains("net.minecraft"));
    }
}
