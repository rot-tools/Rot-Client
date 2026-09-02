package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Minecraft bridge for {@link AutoSellPolicy}. Clicks player-inventory slots
 * in Trades / Booster Cookie chests with a delay * 50ms wait.
 */
public final class AutoSellRuntime {
    private static long lastClickMs;
    private static long nextWaitMs;

    private AutoSellRuntime() {
    }

    public static void tick(Minecraft client) {
        if (!enabled()) {
            lastClickMs = 0L;
            nextWaitMs = 0L;
            return;
        }
        if (client == null || client.gameMode == null || client.player == null) {
            return;
        }
        AbstractContainerScreen<?> screen = containerScreen();
        if (screen == null) {
            return;
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        if (!AutoSellPolicy.isSellMenu(title)) {
            return;
        }
        QolSkyblockExtras extras = extras();
        long now = System.currentTimeMillis();
        if (nextWaitMs <= 0L) {
            nextWaitMs = AutoSellPolicy.waitMs(
                    extras.autoSellDelay, extras.autoSellRandomization, Math.random());
        }
        if (!AutoSellPolicy.delayElapsed(now, lastClickMs, nextWaitMs)) {
            return;
        }
        OptionalInt slot = AutoSellPolicy.nextSellSlot(snapshot(screen), extras.autoSellItems);
        if (slot.isEmpty()) {
            return;
        }
        client.gameMode.handleContainerInput(
                screen.getMenu().containerId,
                slot.getAsInt(),
                0,
                containerInput(extras.autoSellClickType),
                client.player);
        lastClickMs = now;
        nextWaitMs = AutoSellPolicy.waitMs(
                extras.autoSellDelay, extras.autoSellRandomization, Math.random());
    }

    static boolean addDefaults() {
        return extras().addAutoSellDefaults();
    }

    private static ContainerInput containerInput(String clickType) {
        return switch (AutoSellPolicy.clickKind(clickType)) {
            case CLONE -> ContainerInput.CLONE;
            case PICKUP -> ContainerInput.PICKUP;
            default -> ContainerInput.QUICK_MOVE;
        };
    }

    private static List<AutoSellPolicy.SlotView> snapshot(AbstractContainerScreen<?> screen) {
        List<Slot> slots = screen.getMenu().slots;
        List<AutoSellPolicy.SlotView> out = new ArrayList<>(slots.size());
        for (Slot slot : slots) {
            ItemStack stack = slot.getItem();
            out.add(new AutoSellPolicy.SlotView(
                    slot.index,
                    stack.getHoverName().getString(),
                    stack.isEmpty()));
        }
        return out;
    }

    private static AbstractContainerScreen<?> containerScreen() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null) {
            return null;
        }
        return client.gui.screen() instanceof AbstractContainerScreen<?> screen ? screen : null;
    }

    private static boolean enabled() {
        return extras().autoSellEnabled;
    }

    private static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }
}
