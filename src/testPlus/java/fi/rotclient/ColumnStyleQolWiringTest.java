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
        String client = Files.readString(Path.of(
                "src/client/java/fi/rotclient/RotClientClient.java"),
                StandardCharsets.UTF_8);
        String plus = Files.readString(Path.of(
                "src/plusClient/java/fi/rotclient/RotClientPlusHooks.java"),
                StandardCharsets.UTF_8);
        assertTrue(plus.contains("AutoConversationRuntime.onChat"));
        assertTrue(plus.contains("AutoConversationRuntime.tick"));
        assertTrue(client.contains("FishingHelperRuntime.tick"));
        assertTrue(client.contains("FishingSuiteRuntime.tick"));
        assertTrue(client.contains("FishingSuiteRuntime.onChat"));
        assertTrue(plus.contains("EtherwarpPlusRuntime"));
        String etherwarpHelper = Files.readString(Path.of(
                "src/client/java/fi/rotclient/EtherwarpHelperRuntime.java"),
                StandardCharsets.UTF_8);
        String etherwarpPlus = Files.readString(Path.of(
                "src/plusClient/java/fi/rotclient/EtherwarpPlusRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(etherwarpPlus.contains("shouldAssistLookTarget"));
        assertTrue(etherwarpHelper.contains("EtherwarpPredictor.canWarpTo"));
        assertTrue(etherwarpHelper.contains("hasValidLookTarget"));
        assertTrue(client.contains("CommissionDisplayRuntime.tick"));
        assertTrue(client.contains("MiningLeftoverRuntime.tick"));
        assertTrue(client.contains("MiningLeftoverRuntime.onChat"));
        String commissionRuntime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/CommissionDisplayRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(commissionRuntime.contains("rotclient$getNameForDisplay"));
        assertTrue(client.contains("MobHighlightRuntime.tick"));
        assertTrue(client.contains("MenuKeybindRuntime.tick"));
        assertTrue(plus.contains("WardrobeAutoEquipRuntime.tick"));
        assertTrue(plus.contains("WardrobeAutoEquipRuntime.onScreenOpened"));
        assertTrue(plus.contains("AutoHarpRuntime.tick"));
        assertTrue(plus.contains("AutoGfsRuntime.tick"));
        assertTrue(plus.contains("AutoSellRuntime.tick"));
        assertTrue(client.contains("EscrowFixRuntime.onChat"));
        assertTrue(client.contains("QolModuleKeybindRuntime.tick"));
        assertTrue(client.contains("SlayerRuntime.tick"));
        assertTrue(client.contains("SlayerRuntime.onChat"));
        assertTrue(client.contains("SlayerRuntime.onBlockUpdate"));
        assertTrue(client.contains("SlayerRuntime.onEntityEvent"));
        assertTrue(client.contains("SlayerRuntime.onAttack"));
        assertTrue(client.contains("literal(\"slayer\")"));
        assertTrue(client.contains("RotClientClient::slayerStatus"));
        assertTrue(client.contains("NameHiderRuntime.beginTick()"));
        assertTrue(client.contains("QolVisualRuntime.beginTick()"));
        String slayerRuntime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/SlayerRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(slayerRuntime.contains("ClickPulseHelper.pulseUse"));
        assertTrue(slayerRuntime.contains("SoulcryAbilityGate"));
        assertTrue(slayerRuntime.contains("heldItemOnCooldown"));
        assertTrue(!slayerRuntime.contains("soulcryUseCooldown"));
        assertTrue(slayerRuntime.contains("attachedAttunementLine"));
        assertTrue(slayerRuntime.contains("inflate(0.9D, 2.8D, 0.9D)"));
        assertTrue(slayerRuntime.contains("familyForMarker"));
        assertTrue(slayerRuntime.contains("nearOwnedBoss"));
        assertTrue(slayerRuntime.contains("primaryOwnedBoss"));
        assertTrue(slayerRuntime.contains("SlayerMinibossAlertPolicy"));
        assertTrue(slayerRuntime.contains("interpolatedBox"));
        assertTrue(slayerRuntime.contains("addTrackedMarker"));
        assertTrue(slayerRuntime.contains("isVengeanceStartTag"));
        assertFalse(slayerRuntime.contains("descriptor.role() != SlayerPolicy.EntityRole.DEMON"));
    }

    @Test
    void mixinsRegisterEtherwarpAndTooltipHooks() throws Exception {
        String json = Files.readString(Path.of(
                "src/client/resources/rotclient.client.mixins.json"),
                StandardCharsets.UTF_8);
        String plusJson = Files.readString(Path.of(
                "src/plusClient/resources/rotclient.plus.mixins.json"),
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
        assertTrue(json.contains("CreeperRendererGhostsMixin"));
        assertTrue(json.contains("CreeperPoweredGhostsMixin"));
        assertTrue(json.contains("KeyboardHandlerWardrobeMixin"));
        assertTrue(plusJson.contains("MouseHandlerEtherwarpMixin"));
        assertTrue(plusJson.contains("KeyboardInputEtherwarpMixin"));
        String mouse = Files.readString(Path.of(
                "src/plusClient/java/fi/rotclient/mixin/MouseHandlerEtherwarpMixin.java"),
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
        assertTrue(!overlay.contains("ItemRarityRuntime.afterContainerContents"));
        assertTrue(overlay.contains("paintSlotBackground"));
        assertTrue(!overlay.contains("method = \"mouseScrolled\""));
        String rarityHost = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/AbstractContainerScreenCustomCursorMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(rarityHost.contains("ItemRarityRuntime.afterContainerContents"));
        String client = Files.readString(Path.of(
                "src/client/java/fi/rotclient/RotClientClient.java"),
                StandardCharsets.UTF_8);
        assertTrue(client.contains("registerTooltipAndStorageScroll"));
        assertTrue(client.contains("ScreenMouseEvents.allowMouseScroll"));
        assertTrue(client.contains("shouldStealOverlayWheel"));
        assertTrue(client.contains("CustomTooltipPolicy.storageOverlayTakesWheel"));
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
        assertTrue(mouseTooltip.contains("StorageOverlayRuntime.shouldReplaceVanilla"));
        assertTrue(overlay.contains("MissingEnchantsRuntime.noteCtrlClick"));
        assertTrue(overlay.contains("MenuKeybindRuntime.shouldCancelContainerRender"));
        String packets = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/ClientPacketListenerMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(packets.contains("QolClientFlavorSupport.hooks().consumeHiddenOpenScreen"));
        String autoEquip = Files.readString(Path.of(
                "src/plusClient/java/fi/rotclient/WardrobeAutoEquipRuntime.java"),
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
        assertTrue(levelRenderer.contains("GhostsRuntime.renderGizmos"));
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
