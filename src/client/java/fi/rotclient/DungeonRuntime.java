package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Lite compatibility boundary for dungeon entry points. The complete dungeon
 * runtime is compiled from the Plus client source set and replaces this class
 * in the Plus artifact. Lite deliberately omits its mixed solver and map code.
 */
public final class DungeonRuntime {
    static int dungeonWorldTicks;

    private DungeonRuntime() {
    }

    static DungeonPolicy.Sidebar sidebar() {
        return new DungeonPolicy.Sidebar("", "", 0, 0, -1, 0, null, 0, 0, false);
    }

    public static boolean shouldHideTerminalHeader(AbstractContainerScreen<?> screen) {
        return false;
    }

    static void tick(Minecraft client) {
    }

    static void onChat(Component message) {
    }

    static void onWorldChanged() {
    }

    static List<String> displayLines(boolean editorOpen) {
        return List.of();
    }

    static DungeonMapPolicy.Schematic mapSchematic() {
        return DungeonMapPolicy.Schematic.empty();
    }

    static List<DungeonAssistPolicy.MapChip> mapStatusChips() {
        return List.of();
    }

    public static boolean enqueueTerminalClick(int slot, int button) {
        return false;
    }

    static int overlayScore() {
        return -1;
    }

    static boolean shouldHideDioriteNow() {
        return false;
    }

    public static void renderGizmos() {
    }

    public static int highlightColor(AbstractContainerScreen<?> screen, int slot) {
        return 0;
    }

    static String overlayLabel(AbstractContainerScreen<?> screen, int slot) {
        return "";
    }

    public static void renderMenuExtras(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, int left, int top) {
    }

    static void renderPartyFinderSlot(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, Slot slot, int left, int top, int index) {
    }

    public static void onLeapEntityPacket(int entityId, double x, double y, double z) {
    }

    public static void onBlockUsed(BlockPos pos) {
    }

    public static void noteTerminalSlotClick(AbstractContainerScreen<?> screen, int slot) {
    }

    static String entityName(Entity entity) {
        return entity == null ? "" : entity.getName().getString();
    }

    static void resetSplitPersonalBests() {
    }

    static void resetKuudraPersonalBests() {
    }

    static void resetTerminalPersonalBests() {
    }

    static void resetPredevPersonalBest() {
    }

    public static void paintMaskOverlay(GuiGraphicsExtractor graphics, int x, int y, ItemStack stack) {
    }

    static void onBlockUpdate(BlockPos pos, BlockState oldState, BlockState newState) {
    }

    public static void onEntityMetadata(int entityId) {
    }

    public static boolean shouldHideHudTitle(Component title) {
        return false;
    }

    public static boolean shouldHideTeammate(Player player) {
        return false;
    }

    public static boolean handleContainerKey(AbstractContainerScreen<?> screen, int key) {
        return false;
    }

    public static boolean handleContainerMouse(AbstractContainerScreen<?> screen, int button) {
        return false;
    }
}
