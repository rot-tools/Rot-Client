package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ColumnStyleQolWiringTest {
    @Test
    void clientTicksConversationFishingAndSlayerRuntimes() throws Exception {
        String source = Files.readString(Path.of(
                "src/client/java/fi/rotclient/RotClientClient.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("AutoConversationRuntime.onChat"));
        assertTrue(source.contains("AutoConversationRuntime.tick"));
        assertTrue(source.contains("FishingHelperRuntime.tick"));
        assertTrue(source.contains("FishingSuiteRuntime.tick"));
        assertTrue(source.contains("FishingSuiteRuntime.onChat"));
        assertTrue(source.contains("EtherwarpHelperRuntime.tick"));
        String etherwarpHelper = Files.readString(Path.of(
                "src/client/java/fi/rotclient/EtherwarpHelperRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(etherwarpHelper.contains("shouldAssistLookTarget"));
        assertTrue(etherwarpHelper.contains("EtherwarpPredictor.canWarpTo"));
        assertTrue(etherwarpHelper.contains("hasValidLookTarget"));
        assertTrue(source.contains("CommissionDisplayRuntime.tick"));
        assertTrue(source.contains("MiningLeftoverRuntime.tick"));
        assertTrue(source.contains("MiningLeftoverRuntime.onChat"));
        String commissionRuntime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/CommissionDisplayRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(commissionRuntime.contains("rotclient$getNameForDisplay"));
        assertTrue(source.contains("MobHighlightRuntime.tick"));
        assertTrue(source.contains("MenuKeybindRuntime.tick"));
        assertTrue(source.contains("WardrobeAutoEquipRuntime.tick"));
        assertTrue(source.contains("WardrobeAutoEquipRuntime.onScreenOpened"));
        assertTrue(source.contains("AutoHarpRuntime.tick"));
        assertTrue(source.contains("AutoGfsRuntime.tick"));
        assertTrue(source.contains("AutoSellRuntime.tick"));
        assertTrue(source.contains("EscrowFixRuntime.onChat"));
        assertTrue(source.contains("QolModuleKeybindRuntime.tick"));
        assertTrue(source.contains("SlayerRuntime.tick"));
        assertTrue(source.contains("SlayerRuntime.onChat"));
        assertTrue(source.contains("SlayerRuntime.onEntityEvent"));
        assertTrue(source.contains("SlayerRuntime.onAttack"));
        assertTrue(source.contains("literal(\"slayer\")"));
        assertTrue(source.contains("RotClientClient::slayerStatus"));
        assertTrue(source.contains("NameHiderRuntime.beginTick()"));
        assertTrue(source.contains("QolVisualRuntime.beginTick()"));
        String slayerRuntime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/SlayerRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(slayerRuntime.contains("AutoClickerRuntime.pulseUse"));
        assertTrue(slayerRuntime.contains("attachedAttunementLine"));
        assertTrue(slayerRuntime.contains("inflate(1.5D, 3.0D, 1.5D)"));
        assertTrue(slayerRuntime.contains("isVengeanceStartTag"));
        assertFalse(slayerRuntime.contains("descriptor.role() != SlayerPolicy.EntityRole.DEMON"));
    }

    @Test
    void mixinsRegisterEtherwarpAndTooltipHooks() throws Exception {
        String json = Files.readString(Path.of(
                "src/client/resources/rotclient.client.mixins.json"),
                StandardCharsets.UTF_8);
        assertTrue(json.contains("AbstractContainerScreenCustomCursorMixin"));
        assertTrue(json.contains("GuiGraphicsExtractorCustomTooltipMixin"));
        assertTrue(json.contains("MouseHandlerCustomTooltipMixin"));
        assertTrue(!json.contains("ScreenCustomTooltipScrollMixin"));
        String tooltipMixin = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/GuiGraphicsExtractorCustomTooltipMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(tooltipMixin.contains("method = \"tooltip("));
        assertTrue(tooltipMixin.contains("WrapOperation"));
        assertTrue(tooltipMixin.contains("positionTooltip(IIIIII)Lorg/joml/Vector2ic;"));
        assertTrue(!tooltipMixin.contains("setTooltipForNextFrame"));
        assertTrue(!tooltipMixin.contains("ModifyVariable"));
        assertTrue(json.contains("MouseHandlerEtherwarpMixin"));
        assertTrue(json.contains("KeyboardInputEtherwarpMixin"));
        assertTrue(json.contains("KeyboardHandlerWardrobeMixin"));
        assertTrue(json.contains("CreeperRendererGhostsMixin"));
        String mouse = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/MouseHandlerEtherwarpMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(mouse.contains("method = \"onButton\""));
        assertTrue(mouse.contains("MouseButtonInfo"));
        String keyboard = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/KeyboardHandlerWardrobeMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(keyboard.contains("method = \"keyPress\""));
        String overlay = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/AbstractContainerScreenInventoryOverlayMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(overlay.contains("ItemRarityRuntime.afterContainerContents"));
        assertTrue(!overlay.contains("method = \"mouseScrolled\""));
        String client = Files.readString(Path.of(
                "src/client/java/fi/rotclient/RotClientClient.java"),
                StandardCharsets.UTF_8);
        assertTrue(client.contains("registerTooltipAndStorageScroll"));
        assertTrue(client.contains("ScreenMouseEvents.allowMouseScroll"));
        assertTrue(client.contains("CustomTooltipRuntime.shouldStealWheel"));
        assertTrue(!client.contains("CustomTooltipRuntime.mouseScrolled"));
        assertTrue(client.contains("StorageOverlayRuntime.scroll"));
        String tooltipRuntime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/CustomTooltipRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(tooltipRuntime.contains("afterScreenClamp"));
        assertTrue(!tooltipRuntime.contains("wrapPositioner"));
        String mouseTooltip = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/MouseHandlerCustomTooltipMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(mouseTooltip.contains("method = \"onScroll\""));
        assertTrue(mouseTooltip.contains("CustomTooltipRuntime.mouseScrolled"));
        assertTrue(overlay.contains("MissingEnchantsRuntime.afterTooltip"));
        assertTrue(overlay.contains("MenuKeybindRuntime.shouldCancelContainerRender"));
        String packets = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/ClientPacketListenerMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(packets.contains("WardrobeAutoEquipRuntime.consumeOpenScreen"));
        String autoEquip = Files.readString(Path.of(
                "src/client/java/fi/rotclient/WardrobeAutoEquipRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(autoEquip.contains("sendCommand(WardrobeKeybindPolicy.OPEN_COMMAND)"));
        assertTrue(autoEquip.contains("packet.getType().create"));
        assertTrue(autoEquip.contains("cheaterWardrobeSlotBinds"));
        assertTrue(autoEquip.contains("canStartHiddenEquip"));
        assertTrue(autoEquip.contains("cancelActiveSwap"));
        String levelRenderer = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/LevelRendererEtherwarpMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(levelRenderer.contains("SlayerRuntime.renderGizmos"));
        String entitySuppress = Files.readString(Path.of(
                "src/client/java/fi/rotclient/QolVisualRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(entitySuppress.contains("FishingSuiteRuntime.shouldSuppressEntity"));
    }

    @Test
    void worldScannerUsesPerTargetEsp() throws Exception {
        String source = Files.readString(Path.of(
                "src/client/java/fi/rotclient/WorldScannerRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("WorldScannerEspSettings.idForHit"));
        assertTrue(source.contains("target.tracer"));
        assertTrue(source.contains("target.chatCoords"));
        assertTrue(source.contains("billboardTextOverBlock"));
        assertTrue(source.contains("maybeHas"));
    }
}
