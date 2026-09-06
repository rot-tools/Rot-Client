package fi.rotclient;

import fi.rotclient.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

/** Hover-click solved terminal slots. Skips Melody and Auto Click's tick. */
final class DungeonHoverTermsRuntime {
    private static long nextClickAt;

    private DungeonHoverTermsRuntime() {
    }

    static void tick(Minecraft client) {
        DungeonAthenSettings athen = extras().athen();
        if (!athen.hoverEnabled || DungeonRuntime.autoClickedThisTick()) {
            return;
        }
        if (client == null || client.player == null || client.gameMode == null
                || !(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        DungeonPolicy.Terminal terminal = DungeonPolicy.detectTerminal(title);
        if (terminal == DungeonPolicy.Terminal.NONE || terminal == DungeonPolicy.Terminal.MELODY) {
            return;
        }
        Slot hovered = screen instanceof AbstractContainerScreenAccessor accessor
                ? accessor.rotclient$hoveredSlot() : null;
        if (hovered == null) {
            return;
        }
        var click = DungeonAthenPortPolicy.hoverClick(
                DungeonPolicy.solveTerminalClicks(terminal, title, DungeonRuntime.terminalSnapshot(screen)),
                hovered.index,
                terminal);
        if (click.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextClickAt) {
            return;
        }
        DungeonRuntime.clickSolved(screen, click.get());
        nextClickAt = now + DungeonAthenPortPolicy.randomBetween(
                DungeonAthenPortPolicy.clampHoverDelayMs(athen.hoverMinDelay),
                DungeonAthenPortPolicy.clampHoverDelayMs(athen.hoverMaxDelay));
    }

    private static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }
}
