package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;

/**
 * Minecraft bridge for {@link AutoExperimentsPolicy}, wired to four points: screen
 * open, slot-update packet, client tick start, and screen mouse input. Clicks go
 * out as {@code ContainerInput.CLONE} with button 0.
 */
public final class AutoExperimentsRuntime {
    private static final Random RANDOM = new Random();

    private static AutoExperimentsPolicy.Handler handler;
    private static long lastClick;

    private AutoExperimentsRuntime() {
    }

    public static void onScreenOpened(Screen screen) {
        if (!enabled()) {
            handler = null;
            return;
        }
        String title = screen == null || screen.getTitle() == null
                ? ""
                : screen.getTitle().getString();
        handler = AutoExperimentsPolicy.handlerFor(AutoExperimentsPolicy.forTitle(title));
    }

    /**
     * Called at the tail of every container slot packet, after the menu already
     * holds the new stack. The whole container is re-read on each update.
     */
    public static void onSlotUpdate() {
        if (!enabled()) {
            handler = null;
            return;
        }
        AbstractContainerScreen<?> screen = containerScreen();
        if (screen == null) {
            return;
        }
        if (handler == null) {
            String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
            handler = AutoExperimentsPolicy.handlerFor(AutoExperimentsPolicy.forTitle(title));
        }
        AutoExperimentsPolicy.Handler active = handler;
        if (active == null) {
            return;
        }
        active.onSlotUpdate(snapshot(screen), options());
    }

    /**
     * On each client tick: click when the delay has elapsed, then ask the handler
     * whether the GUI should close. Close is independent of the delay so a finished
     * Chronomatron round is not held open waiting for the next click.
     */
    public static void tick(Minecraft client) {
        if (!enabled()) {
            handler = null;
            return;
        }
        AutoExperimentsPolicy.Handler active = handler;
        if (active == null || client == null) {
            return;
        }
        AbstractContainerScreen<?> screen = containerScreen();
        if (screen == null) {
            return;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        long now = System.currentTimeMillis();
        if (now - lastClick >= AutoExperimentsPolicy.delay(
                extras.autoExperimentsClickDelay, extras.autoExperimentsDelayVariety, RANDOM)) {
            OptionalInt next = active.nextClick();
            if (next.isPresent() && client.gameMode != null && client.player != null) {
                client.gameMode.handleContainerInput(
                        screen.getMenu().containerId,
                        next.getAsInt(),
                        0,
                        ContainerInput.CLONE,
                        client.player);
                lastClick = now;
            }
        }
        if (!active.shouldClose(extras.autoExperimentsAutoClose, options())) {
            return;
        }
        if (client.player != null) {
            client.player.closeContainer();
        }
        handler = null;
    }

    /**
     * Every mouse press and release is swallowed while a handler is live so a human
     * click cannot desync the remembered sequence.
     */
    public static boolean shouldBlockMouse() {
        return enabled() && handler != null && containerScreen() != null;
    }

    public static void reset() {
        handler = null;
        lastClick = 0L;
    }

    private static boolean enabled() {
        return RotClientClient.qolConfigPublic().extras().autoExperimentsEnabled;
    }

    private static AutoExperimentsPolicy.Options options() {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        return new AutoExperimentsPolicy.Options(
                extras.autoExperimentsGetMaxXp, extras.autoExperimentsSerumCount);
    }

    private static AbstractContainerScreen<?> containerScreen() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null) {
            return null;
        }
        return client.gui.screen() instanceof AbstractContainerScreen<?> screen ? screen : null;
    }

    private static List<AutoExperimentsPolicy.SlotView> snapshot(AbstractContainerScreen<?> screen) {
        List<Slot> slots = screen.getMenu().slots;
        List<AutoExperimentsPolicy.SlotView> out = new ArrayList<>(slots.size());
        for (Slot slot : slots) {
            ItemStack stack = slot.getItem();
            out.add(new AutoExperimentsPolicy.SlotView(
                    slot.index,
                    stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) != null || stack.hasFoil(),
                    itemPath(stack),
                    stack.getHoverName().getString(),
                    stack.getCount()));
        }
        return out;
    }

    private static String itemPath(ItemStack stack) {
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? "" : key.getPath();
    }
}
