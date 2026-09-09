package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Minecraft bridge for {@link AutoHarpPolicy}. Applies the quartz-slot bitmask
 * and clicks the first lit note with {@code CLONE}.
 */
public final class AutoHarpRuntime {
    private static boolean inHarp;
    private static int hash;

    private AutoHarpRuntime() {
    }

    public static void onScreenOpened(Screen screen) {
        if (!enabled()) {
            reset();
            return;
        }
        String title = screen == null || screen.getTitle() == null
                ? ""
                : screen.getTitle().getString();
        inHarp = AutoHarpPolicy.isHarpTitle(title) && screen instanceof AbstractContainerScreen<?>;
        if (!inHarp) {
            hash = 0;
        }
    }

    public static void tick(Minecraft client) {
        if (!enabled() || !inHarp || client == null || client.gameMode == null || client.player == null) {
            if (!enabled()) {
                reset();
            }
            return;
        }
        AbstractContainerScreen<?> screen = containerScreen();
        if (screen == null) {
            inHarp = false;
            hash = 0;
            return;
        }
        List<Boolean> quartz = quartzMask(screen);
        AutoHarpPolicy.TickResult result = AutoHarpPolicy.nextClick(quartz, hash);
        hash = result.hash();
        OptionalInt slot = result.clickSlot();
        if (slot.isEmpty()) {
            return;
        }
        client.gameMode.handleContainerInput(
                screen.getMenu().containerId,
                slot.getAsInt(),
                0,
                ContainerInput.CLONE,
                client.player);
    }

    public static void reset() {
        inHarp = false;
        hash = 0;
    }

    private static boolean enabled() {
        return RotClientClient.qolConfigPublic().extras().autoHarpEnabled;
    }

    private static AbstractContainerScreen<?> containerScreen() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null) {
            return null;
        }
        return client.gui.screen() instanceof AbstractContainerScreen<?> screen ? screen : null;
    }

    private static List<Boolean> quartzMask(AbstractContainerScreen<?> screen) {
        List<Boolean> out = new ArrayList<>(7);
        List<Slot> slots = screen.getMenu().slots;
        for (int i = AutoHarpPolicy.FIRST_NOTE_SLOT; i <= AutoHarpPolicy.LAST_NOTE_SLOT; i++) {
            if (i >= slots.size()) {
                out.add(false);
                continue;
            }
            ItemStack stack = slots.get(i).getItem();
            out.add(AutoHarpPolicy.QUARTZ_BLOCK.equals(itemPath(stack)));
        }
        return out;
    }

    private static String itemPath(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? "" : key.getPath();
    }
}
