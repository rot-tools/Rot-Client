package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class InventoryOverlayWiringTest {
    @Test
    void inventoryDrawsEquipmentColumnAndCtrlDragsPet() throws Exception {
        String runtime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/InventoryChromeRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(runtime.contains("InventoryOverlayPolicy.OPEN_STATS_COMMAND"));
        assertTrue(runtime.contains("InventoryOverlayPolicy.OPEN_WARDROBE_COMMAND"));
        assertTrue(runtime.contains("sendCommand(InventoryOverlayPolicy.OPEN_PETS_COMMAND)"));
        assertTrue(runtime.contains("renderPetSlot"));
        assertTrue(runtime.contains("renderEquipmentColumn"));
        assertTrue(runtime.contains("handleInventoryDrag"));
        assertTrue(runtime.contains("handleInventoryRelease"));
        assertTrue(runtime.contains("snapshotStatsMenuPet"));
        assertTrue(runtime.contains("drawClickHint"));
        assertTrue(runtime.contains("renderColorEditor"));
        assertTrue(runtime.contains("afterForeground"));
        assertTrue(runtime.contains("skillLevelsBackground"));
        assertTrue(runtime.contains("showSkyblockInventoryUi"));
        assertTrue(runtime.contains("hideInventoryStatusEffects"));
        assertTrue(runtime.contains("valueMarkRect(leftPos, topPos,"));
        assertTrue(runtime.contains("skyblockInventoryUi"));
        assertTrue(runtime.contains("drawPaintBucketIcon"));
        assertTrue(runtime.contains("snapshotEquipmentSets"));
        assertTrue(runtime.contains("paintChrome"));
        assertTrue(runtime.contains("InventoryOverlayPolicy.CHROME_CACHE_FILE"));
        assertTrue(runtime.contains("loadCache"));
        assertTrue(runtime.contains("scheduleSave"));
        assertTrue(runtime.contains("flushForShutdown"));
        assertTrue(runtime.contains("shouldKeepExistingCache"));
        assertTrue(runtime.contains("shouldReloadChromeCache"));
        assertTrue(runtime.contains("shouldSkipEmptyChromeSave"));
        assertTrue(runtime.contains("StorageOverlayRuntime.stackToCache"));
        assertTrue(runtime.contains("StorageOverlayRuntime.stackFromCache"));
        assertTrue(runtime.contains("addProperty(\"petKnownEmpty\""));
        assertTrue(!runtime.contains("EQUIPMENT[i] = ItemStack.EMPTY"));
        assertTrue(!runtime.contains("mascotCoverRect"));
        assertTrue(!runtime.contains("0xC008080C"));
        assertTrue(runtime.contains("editorResetRect"));
        assertTrue(runtime.contains("editorCloseRect"));
        assertTrue(runtime.contains("dismissColorEditor"));
        assertTrue(runtime.contains("dashboardButtonRect"));
        assertTrue(runtime.contains("drawRotLetterIcon"));
        assertTrue(runtime.contains("renderValueMark"));
        assertTrue(runtime.contains("RotClientClient.openClickGui"));
        assertTrue(runtime.contains("InventoryValuePolicy"));
        assertTrue(runtime.contains("defaultChromeColor"));
        assertTrue(runtime.contains("extractInventoryBackground"));
        assertTrue(runtime.contains("chromeFillRects"));
        assertTrue(runtime.contains("survivalSlotRects"));
        assertTrue(runtime.contains("drawSlotWell"));
        assertTrue(!runtime.contains("client.gui.setScreen(new InventoryChromeColorsScreen"));
        assertTrue(!runtime.contains("shouldShiftInventory"));
        assertTrue(!runtime.contains("renderPetInOffhand"));
        assertTrue(!runtime.contains("renderPetBesideBoots"));

        String mixin = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/AbstractContainerScreenInventoryOverlayMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(mixin.contains("mouseClicked"));
        assertTrue(mixin.contains("mouseDragged"));
        assertTrue(mixin.contains("mouseReleased"));
        assertTrue(mixin.contains("handleInventoryClick"));
        assertTrue(mixin.contains("hasControlDown"));
        assertTrue(mixin.contains("hasShiftDown"));
        assertTrue(mixin.contains("InventoryButtonsRuntime.drag"));
        assertTrue(mixin.contains("InventoryButtonsRuntime.release"));
        assertTrue(mixin.contains("renderColorEditor"));
        assertTrue(mixin.contains("extractSlot"));
        assertTrue(mixin.contains("shouldHideOffhandSlot"));
        assertTrue(mixin.contains("extractSlot"));
        assertTrue(!mixin.contains("shouldShiftInventory"));

        String buttons = Files.readString(Path.of(
                "src/client/java/fi/rotclient/InventoryButtonsRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(buttons.contains("RotClientTheme.BUTTON_HOVER"));
        assertTrue(buttons.contains("RotClientTheme.drawOutline"));
        assertTrue(buttons.contains("showSkyblockInventoryUi"));
        assertTrue(!buttons.contains("0xD0182A38"));

        String dashboard = Files.readString(Path.of(
                "src/client/java/fi/rotclient/QolUtilityDashboard.java"),
                StandardCharsets.UTF_8);
        assertTrue(dashboard.contains("qol.inventory_overlay.open_colors"));
        assertTrue(dashboard.contains("InventoryChromeColorsScreen"));

        String client = Files.readString(Path.of(
                "src/client/java/fi/rotclient/RotClientClient.java"),
                StandardCharsets.UTF_8);
        assertTrue(client.contains("InventoryChromeRuntime.flushForShutdown"));
        assertTrue(client.contains("InventoryChromeRuntime.loadCache"));

        String catalog = Files.readString(Path.of(
                "src/main/java/fi/rotclient/QolUtilityCatalog.java"),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains("Last seen equipment and the chosen pet are stored locally"));
    }
}
