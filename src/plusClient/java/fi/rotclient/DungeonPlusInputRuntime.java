package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/** Input cancellation and client-side block rewriting used only by Rot Client+. */
public final class DungeonPlusInputRuntime {
    private DungeonPlusInputRuntime() { }

    public static boolean shouldCancelBlockUse(BlockPos pos, boolean sneaking) {
        if (pos == null) {
            return false;
        }
        QolSkyblockExtras extras = DungeonRuntime.extras();
        if (!extras.dungeonF7Enabled) {
            return false;
        }
        if (DungeonF7Policy.blockWrongArrow(
                pos.getX(), pos.getY(), pos.getZ(),
                extras.dungeonF7ArrowAlign && extras.dungeonF7ArrowBlockWrong,
                sneaking,
                false,
                DungeonRuntime.arrowClicks)) {
            return true;
        }
        EmberDungeonPolicy.IntVec next = DungeonRuntime.simon.nextButton();
        if (DungeonF7Policy.blockWrongSimon(
                pos.getX(), pos.getY(), pos.getZ(),
                extras.dungeonF7Simon && extras.dungeonF7SimonBlockWrong,
                sneaking,
                next)) {
            return true;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        boolean holdingRelicOrMenu = player != null && (
                DungeonF7Policy.holdingRelicOrMenu(player.getMainHandItem().getHoverName().getString())
                        || DungeonF7Policy.holdingRelicOrMenu(player.getOffhandItem().getHoverName().getString()));
        if (EmberDungeonPolicy.blockRelicClick(
                extras.dungeonF7RelicBlockWrong,
                DungeonRuntime.lastRelic,
                holdingRelicOrMenu,
                pos.getX(),
                pos.getY(),
                pos.getZ())) {
            return true;
        }
        if (!DungeonRuntime.lastRelic.isBlank()
                && EmberDungeonPolicy.correctRelicCauldron(
                        DungeonRuntime.lastRelic, pos.getX(), pos.getY(), pos.getZ())) {
            DungeonRuntime.lastRelic = "";
            DungeonRuntime.relicPickupAt = 0L;
        }
        return false;
    }

    public static boolean shouldCancelEntityUse(Entity entity, boolean sneaking) {
        if (!(entity instanceof ItemFrame frame)) {
            return false;
        }
        return shouldCancelBlockUse(frame.blockPosition(), sneaking);
    }

    public static boolean tryBreakerInstamine(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.level == null || pos == null) {
            return false;
        }
        QolSkyblockExtras extras = DungeonRuntime.extras();
        var stack = client.player.getMainHandItem();
        String name = stack.getHoverName().getString();
        String id = SkyBlockItemIdentity.skyBlockId(stack);
        boolean holding = TempleDungeonPolicy.isDungeonBreakerItem(name, id);
        int charges = DungeonPolicy.breakerCharges(InventoryChromeRuntime.loreLines(stack)).orElse(0);
        boolean fatigue = client.player.hasEffect(MobEffects.MINING_FATIGUE);
        if (!DungeonAthenPortPolicy.shouldInstamineBreaker(
                extras.dungeonF7Enabled,
                extras.athen().breakerInstamine,
                holding,
                fatigue,
                charges,
                DungeonRuntime.fullBlockId(client, pos))) {
            return false;
        }
        client.level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        return true;
    }

    public static boolean shouldSkipBreakerSecretMine(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.level == null || pos == null) {
            return false;
        }
        QolSkyblockExtras extras = DungeonRuntime.extras();
        if (!extras.dungeonF7Enabled || !extras.dungeonF7BreakerPreventSecrets) {
            return false;
        }
        var stack = client.player.getMainHandItem();
        String name = stack.getHoverName().getString();
        String id = SkyBlockItemIdentity.skyBlockId(stack);
        if (!TempleDungeonPolicy.isDungeonBreakerItem(name, id)) {
            return false;
        }
        return TempleDungeonPolicy.shouldBlockBreakerOnSecret(
                true, true, DungeonRuntime.fullBlockId(client, pos));
    }

    public static boolean shouldCancelTerminalSlot(AbstractContainerScreen<?> screen, int slot) {
        if (screen == null || !DungeonRuntime.extras().dungeonTerminalsEnabled) {
            return false;
        }
        DungeonPolicy.Terminal terminal = DungeonPolicy.detectTerminal(DungeonRuntime.titleOf(screen));
        if (terminal == DungeonPolicy.Terminal.NONE) {
            return false;
        }
        if (DungeonF7Policy.chestTerminalSlot(slot) && DungeonRuntime.terminalFirstClickPending()) {
            return true;
        }
        QolSkyblockExtras extras = DungeonRuntime.extras();
        if (!extras.dungeonTerminalsBlockWrongSlots) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        boolean sneaking = client != null && client.player != null && client.player.isShiftKeyDown();
        List<DungeonPolicy.TerminalClick> live = DungeonPolicy.solveTerminalClicks(
                terminal, DungeonRuntime.titleOf(screen), DungeonRuntime.snapshot(screen));
        if (live.isEmpty()) {
            return false;
        }
        List<DungeonPolicy.TerminalClick> clicks = DungeonRuntime.pinglessRemaining(terminal, live);
        return DungeonF7Policy.shouldBlockWrongTerminalSlot(
                true,
                sneaking,
                DungeonF7Policy.chestTerminalSlot(slot),
                DungeonF7Policy.slotInSolution(clicks, slot));
    }

    public static boolean shouldHideTerminalTooltip(AbstractContainerScreen<?> screen) {
        return DungeonRuntime.terminalOverlayActive(screen)
                && DungeonRuntime.extras().dungeonTerminalsStopTooltips;
    }

    public static boolean shouldHideTerminalSlot(AbstractContainerScreen<?> screen, Slot slot) {
        if (slot == null || !DungeonRuntime.terminalOverlayActive(screen)
                || !DungeonRuntime.extras().dungeonTerminalsHideClicked) {
            return false;
        }
        DungeonPolicy.Terminal terminal = DungeonPolicy.detectTerminal(DungeonRuntime.titleOf(screen));
        if (terminal == DungeonPolicy.Terminal.NONE || terminal == DungeonPolicy.Terminal.MELODY) {
            return false;
        }
        List<DungeonPolicy.TerminalClick> live = DungeonPolicy.solveTerminalClicks(
                terminal, DungeonRuntime.titleOf(screen), DungeonRuntime.snapshot(screen));
        if (live.isEmpty()) {
            return false;
        }
        List<DungeonPolicy.TerminalClick> clicks = DungeonRuntime.pinglessRemaining(terminal, live);
        return DungeonF7Policy.shouldHideClickedSlot(
                true,
                DungeonF7Policy.chestTerminalSlot(slot.index),
                slot.getItem() == null || slot.getItem().isEmpty(),
                DungeonF7Policy.slotInSolution(clicks, slot.index));
    }
}
