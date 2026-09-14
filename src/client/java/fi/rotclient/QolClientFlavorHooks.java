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

import java.util.List;

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

    default boolean ghostsShouldSuppress(Entity entity) {
        return false;
    }

    default void renderWorldGizmos() {
    }

    default void slayerAutomationTick(Minecraft client) {
    }

    default void slayerAutomationOnAttack(Minecraft client, Entity entity) {
    }

    default void slayerAutomationOnChat(String line) {
    }

    default void slayerAutomationScheduleAutoStart() {
    }

    default void slayerAutomationReset() {
    }

    default void dungeonRequeueTick(Minecraft client) {
    }

    default boolean dungeonRequeueOnChat(
            boolean dungeonRunStarted,
            boolean extraStatsSeen,
            int dungeonWorldTicks,
            String line) {
        return false;
    }

    default void dungeonRequeueReset() {
    }

    default void fishingCreatureAutoAttackTick(
            Minecraft client, boolean lookingAtTrackedCreature, boolean screenOpen) {
    }

    default void foragingAutomationTick(Minecraft client) {
    }

    default void foragingAutomationReset() {
    }

    default void dianaMaybeAutoWarp(
            Minecraft client,
            boolean moduleEnabled,
            boolean autoWarpEnabled,
            DianaPolicy.WarpPoint warp,
            long now,
            long lastSpadeUseAt) {
    }

    default void dianaMaybePartyShare(
            Minecraft client,
            boolean moduleEnabled,
            boolean partyShareEnabled,
            DianaPolicy.RareMob mob,
            BlockPos position,
            long now) {
    }

    default void dianaAutomationReset() {
    }

    default boolean wardrobeMenuHandleInput(
            net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen,
            int code) {
        return false;
    }

    default boolean wardrobeMenuShouldCancelRender(
            net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
        return false;
    }

    default void wardrobeMenuTick(Minecraft client) {
    }

    default boolean experimentShouldBlockWrongClick(boolean solverWouldBlock) {
        return false;
    }

    default void plusModuleKeybindTick(Minecraft client) {
    }

    default void escrowFixOnChat(Component message) {
    }

    default void dungeonPartyJoinTick(Minecraft client) {
    }

    default void dungeonPartyJoinMaybeKick(
            Minecraft client,
            String player,
            java.util.Optional<DungeonPartyFinderPolicy.Stats> stats,
            DungeonAthenSettings settings) {
    }

    default void fishingCreatureSpawned(
            Minecraft client, FishingCreaturesPolicy.Creature spawn) {
    }

    default void miningMineshaftPortal() {
    }

    default void miningCommissionComplete() {
    }

    default void miningCorpseCoordinates(MiningLeftoverPolicy.CorpseCoords coordinates) {
    }

    default void miningWormSeen(MiningLeftoverPolicy.WormKind kind) {
    }

    default void miningShaftEntered(String area) {
    }

    default List<RingPolicy.MacroDef> commandMacros(
            QolUtilityConfig config, List<RingPolicy.MacroDef> presets) {
        return presets == null ? List.of() : presets;
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

    /**
     * Loadouts are a Plus-only product feature.
     * The regular client has no Loadouts navigation or activation UI.
     */
    default boolean loadoutsEnabled() {
        return false;
    }

    default boolean loadoutPetAutoEquipBusy() {
        return false;
    }

    default boolean beginLoadoutPetEquip(String petUuid) {
        return false;
    }

    default boolean loadoutEquipmentAutoEquipBusy() {
        return false;
    }

    default boolean beginLoadoutEquipmentEquip(int equipmentSetNumber) {
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
