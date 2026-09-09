package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Plus Minecraft bridges used by shared mixins and overlay code via
 * {@link QolClientFlavorSupport}.
 */
public final class RotClientPlusHooks implements QolClientFlavorHooks {
    @Override
    public void tickStart(Minecraft client) {
        ClientBoundaryGuard.run("AUTO_CLICKER", () -> AutoClickerRuntime.tick(client));
        ClientBoundaryGuard.run("AUTO_EXPERIMENTS", () -> AutoExperimentsRuntime.tick(client));
        ClientBoundaryGuard.run("AUTO_HARP", () -> AutoHarpRuntime.tick(client));
        ClientBoundaryGuard.run("AUTO_GFS", () -> AutoGfsRuntime.tick(client));
        ClientBoundaryGuard.run("AUTO_SELL", () -> AutoSellRuntime.tick(client));
    }

    @Override
    public void tickEnd(Minecraft client) {
        ClientBoundaryGuard.run("CAMERA_ENFORCE", this::enforceCameraPerspective);
        ClientBoundaryGuard.run("FREECAM", () -> FreecamRuntime.tick(client));
        ClientBoundaryGuard.run("INVENTORY_WALK", () -> InventoryWalkRuntime.tick(client));
        ClientBoundaryGuard.run("WARDROBE_AUTO_EQUIP", () -> WardrobeAutoEquipRuntime.tick(client));
        ClientBoundaryGuard.run("AUTO_CONVERSATION", () -> AutoConversationRuntime.tick(client));
        ClientBoundaryGuard.run("FARM_KEYS", () -> FarmKeysRuntime.tick(client));
        ClientBoundaryGuard.run("AUTO_DOJO", () -> AutoDojoRuntime.tick(client));
        ClientBoundaryGuard.run("DUNGEON_PLUS", () -> DungeonPlusRuntime.tick(client));
        ClientBoundaryGuard.run("FISHING_PLUS", () -> FishingPlusRuntime.tick(client));
        ClientBoundaryGuard.run("IOTA_PLUS", () -> IotaPlusRuntime.tick(client));
        ClientBoundaryGuard.run("ETHERWARP_PLUS", () -> EtherwarpPlusRuntime.tick(client));
    }

    @Override
    public void onChat(Component message) {
        AutoGfsRuntime.onChat(message);
        AutoConversationRuntime.onChat(message);
        AutoDojoRuntime.onChat(message == null ? "" : message.getString());
        IotaPlusRuntime.onChat(message);
        DungeonPlusRuntime.onChat(message);
    }

    @Override
    public void onScreenOpened(Screen screen) {
        WardrobeAutoEquipRuntime.onScreenOpened(screen);
        AutoExperimentsRuntime.onScreenOpened(screen);
        AutoHarpRuntime.onScreenOpened(screen);
        DungeonPlusRuntime.onScreenOpened(screen);
    }

    @Override
    public void onWorldChanged() {
        AutoDojoRuntime.onWorldChanged();
        FishingPlusRuntime.clear();
        EtherwarpPlusRuntime.clear();
    }

    @Override
    public boolean freecamActive() {
        return FreecamRuntime.active();
    }

    @Override
    public boolean freecamHideLocalBody(Entity entity) {
        return FreecamRuntime.shouldHideLocalBody(entity);
    }

    @Override
    public boolean inventoryWalkBlocksClick() {
        return InventoryWalkRuntime.shouldBlockContainerClick();
    }

    @Override
    public boolean autoExperimentsBlockMouse() {
        return AutoExperimentsRuntime.shouldBlockMouse();
    }

    @Override
    public boolean wardrobeAutoEquipKey(KeyEvent event, int action) {
        return WardrobeAutoEquipRuntime.onKeyPress(event, action);
    }

    @Override
    public boolean consumeHiddenOpenScreen(ClientboundOpenScreenPacket packet) {
        return WardrobeAutoEquipRuntime.consumeOpenScreen(packet);
    }

    @Override
    public void onContainerSlotUpdate() {
        AutoExperimentsRuntime.onSlotUpdate();
    }

    @Override
    public void onContainerClosed() {
        WardrobeAutoEquipRuntime.onContainerClosed();
        AutoExperimentsRuntime.reset();
        AutoHarpRuntime.reset();
    }

    @Override
    public void resetExperimentScreens() {
        AutoExperimentsRuntime.reset();
        AutoHarpRuntime.reset();
    }

    @Override
    public boolean wardrobeAutoEquipBusy() {
        return WardrobeAutoEquipRuntime.busy();
    }

    @Override
    public boolean beginWardrobeLoadoutEquip(int slot) {
        return WardrobeAutoEquipRuntime.beginLoadoutEquip(slot);
    }

    @Override
    public String wardrobeHudText(boolean editorOpen) {
        return WardrobeAutoEquipRuntime.hudText(editorOpen);
    }

    @Override
    public boolean handleDashboardAction(String settingId) {
        if ("qol.auto_sell.add_defaults".equals(settingId)) {
            return AutoSellRuntime.addDefaults();
        }
        return false;
    }

    @Override
    public int autoClickerAdd(boolean left, java.util.function.Consumer<String> feedback) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            feedback.accept("Auto Clicker: no local player.");
            return 0;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        String identity = AutoClickerItemIdentity.identify(client.player.getMainHandItem());
        if (identity.isBlank()) {
            feedback.accept("Auto Clicker: hold an item to whitelist first.");
            return 0;
        }
        java.util.List<String> list = left
                ? AutoClickerWhitelist.ensureMutable(qol.autoClickerLeftWhitelist)
                : AutoClickerWhitelist.ensureMutable(qol.autoClickerRightWhitelist);
        if (left) {
            qol.autoClickerLeftWhitelist = list;
        } else {
            qol.autoClickerRightWhitelist = list;
        }
        if (!AutoClickerWhitelist.add(list, identity)) {
            feedback.accept("Auto Clicker: already whitelisted on "
                    + (left ? "left" : "right") + ": " + identity);
            return 1;
        }
        TrackerStore.save(RotClientClient.trackerConfig());
        feedback.accept("Auto Clicker: added to "
                + (left ? "left" : "right") + " whitelist: " + identity);
        return 1;
    }

    @Override
    public int autoClickerRemove(boolean left, java.util.function.Consumer<String> feedback) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            feedback.accept("Auto Clicker: no local player.");
            return 0;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        String identity = AutoClickerItemIdentity.identify(client.player.getMainHandItem());
        java.util.List<String> list = left
                ? qol.autoClickerLeftWhitelist
                : qol.autoClickerRightWhitelist;
        if (!AutoClickerWhitelist.remove(list, identity)) {
            feedback.accept("Auto Clicker: not on "
                    + (left ? "left" : "right") + " whitelist: " + identity);
            return 0;
        }
        TrackerStore.save(RotClientClient.trackerConfig());
        feedback.accept("Auto Clicker: removed from "
                + (left ? "left" : "right") + " whitelist: " + identity);
        return 1;
    }

    @Override
    public int autoClickerList(java.util.function.Consumer<String> feedback) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        feedback.accept("Auto Clicker left: " + formatWhitelist(qol.autoClickerLeftWhitelist));
        feedback.accept("Auto Clicker right: " + formatWhitelist(qol.autoClickerRightWhitelist));
        return 1;
    }

    private static String formatWhitelist(java.util.List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return "(empty)";
        }
        return String.join(", ", entries);
    }

    @Override
    public CpsHud autoClickerHud() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.autoClickerCpsHudEnabled) {
            return null;
        }
        AutoClickerCpsMeter.Snapshot snapshot = AutoClickerRuntime.cpsSnapshot();
        boolean blockHold = AutoClickerRuntime.isHoldingBlockBreak();
        String state = blockHold ? "BLOCK HOLD" : "PULSING";
        String text = "Auto Clicker  " + state + "  L " + snapshot.leftCps()
                + "  R " + snapshot.rightCps() + "  Total " + snapshot.totalCps() + " CPS";
        return new CpsHud(text, blockHold);
    }

    @Override
    public void renderExtraOverlays(
            GuiGraphicsExtractor graphics, Font font, QolUtilityConfig qol, boolean editorOpen) {
    }

    @Override
    public boolean shouldCancelTerminalSlot(AbstractContainerScreen<?> screen, int slot) {
        return DungeonRuntime.shouldCancelTerminalSlot(screen, slot);
    }

    @Override
    public boolean tryBreakerInstamine(BlockPos pos) {
        return DungeonRuntime.tryBreakerInstamine(pos);
    }

    @Override
    public boolean shouldCancelBlockUse(BlockPos pos, boolean sneaking) {
        return DungeonRuntime.shouldCancelBlockUse(pos, sneaking);
    }

    @Override
    public boolean shouldCancelEntityUse(Entity entity, boolean sneaking) {
        return DungeonRuntime.shouldCancelEntityUse(entity, sneaking);
    }

    @Override
    public boolean shouldBlockWrongItemUse(ItemStack stack) {
        return false;
    }

    @Override
    public void enforceCameraPerspective() {
        if (!RotClientClient.trackerConfig().cameraEnabled) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) {
            return;
        }
        if (client.options.getCameraType() == net.minecraft.client.CameraType.THIRD_PERSON_FRONT) {
            client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        }
    }
}
