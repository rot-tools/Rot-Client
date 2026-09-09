package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Plus-only Minecraft hooks. The legit JAR has no ServiceLoader provider, so
 * every default is a no-op and shared client code never names Plus classes.
 */
public interface QolClientFlavorHooks {
    QolClientFlavorHooks NONE = new QolClientFlavorHooks() {};

    default void enforceCameraPerspective() {
    }

    default void tickStart(Minecraft client) {
    }

    default void tickEnd(Minecraft client) {
    }

    default void onChat(Component message) {
    }

    default void onScreenOpened(Screen screen) {
    }

    default void onWorldChanged() {
    }

    default boolean freecamActive() {
        return false;
    }

    default boolean freecamHideLocalBody(Entity entity) {
        return false;
    }

    default boolean inventoryWalkBlocksClick() {
        return false;
    }

    default boolean autoExperimentsBlockMouse() {
        return false;
    }

    default boolean wardrobeAutoEquipKey(KeyEvent event, int action) {
        return false;
    }

    default boolean consumeHiddenOpenScreen(ClientboundOpenScreenPacket packet) {
        return false;
    }

    default void onContainerSlotUpdate() {
    }

    default void onContainerClosed() {
    }

    default void resetExperimentScreens() {
    }

    default boolean wardrobeAutoEquipBusy() {
        return false;
    }

    default boolean beginWardrobeLoadoutEquip(int slot) {
        return false;
    }

    default String wardrobeHudText(boolean editorOpen) {
        return "";
    }

    default boolean handleDashboardAction(String settingId) {
        return false;
    }

    default int autoClickerAdd(boolean left, java.util.function.Consumer<String> feedback) {
        return 0;
    }

    default int autoClickerRemove(boolean left, java.util.function.Consumer<String> feedback) {
        return 0;
    }

    default int autoClickerList(java.util.function.Consumer<String> feedback) {
        return 0;
    }

    default CpsHud autoClickerHud() {
        return null;
    }

    default void renderExtraOverlays(
            GuiGraphicsExtractor graphics, Font font, QolUtilityConfig qol, boolean editorOpen) {
    }

    default boolean shouldCancelTerminalSlot(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen, int slot) {
        return false;
    }

    default boolean tryBreakerInstamine(BlockPos pos) {
        return false;
    }

    default boolean shouldCancelBlockUse(BlockPos pos, boolean sneaking) {
        return false;
    }

    default boolean shouldCancelEntityUse(Entity entity, boolean sneaking) {
        return false;
    }

    default boolean shouldBlockWrongItemUse(ItemStack stack) {
        return false;
    }

    record CpsHud(String text, boolean blockHold) {
    }
}
