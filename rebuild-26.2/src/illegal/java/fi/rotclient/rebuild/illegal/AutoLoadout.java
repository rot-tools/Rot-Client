package fi.rotclient.rebuild.illegal;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

/** One user-triggered switch in a recognized menu. Never retries a click. */
public final class AutoLoadout {
    private static final int MENU_TIMEOUT = 100;
    private final AutoLoadoutSettings settings;
    private Attempt pending;
    private long tick;

    AutoLoadout(AutoLoadoutSettings settings) {
        this.settings = settings;
    }

    public boolean onKey(long window, int action, KeyEvent event) {
        if (!settings.enabled || action != GLFW.GLFW_PRESS || event.modifiers() != 0) return false;
        int binding = settings.slotForKey(event.key());
        if (binding < 0) return false;
        Minecraft client = Minecraft.getInstance();
        if ((pending != null && pending.close == null) || !client.isSameThread() || !client.isWindowActive() || client.getWindow().handle() != window
                || client.gui.screen() != null || client.gui.overlay() != null || client.player == null
                || client.level == null || !client.player.isAlive() || client.getConnection() == null) return false;
        pending = new Attempt(binding, tick + MENU_TIMEOUT, client.getConnection(), client.level);
        client.getConnection().sendCommand("loadout");
        return true;
    }

    void tick(Minecraft client) {
        tick++;
        Attempt current = pending;
        if (current == null) return;
        if (!settings.enabled || client.player == null || !client.player.isAlive() || client.gameMode == null
                || client.getConnection() != current.connection || client.level != current.world
                || !client.isWindowActive() || client.gui.overlay() != null
                || (current.close != null ? current.close.expired(tick) : tick > current.deadline)) {
            pending = null;
            return;
        }
        var screen = client.gui.screen();
        if (current.close != null) {
            if (screen == null) return;
            if (!(screen instanceof AbstractContainerScreen<?> container) || !(container.getMenu() instanceof ChestMenu menu)
                    || menu != client.player.containerMenu || menu.getRowCount() < 5
                    || AutoLoadoutPolicy.menuCapacity(container.getTitle().getString()) == 0) {
                pending = null;
            } else if (current.close.shouldClose(tick, menu, menu.getCarried().isEmpty())) {
                client.player.closeContainer();
            }
            return;
        }
        if (screen == null && current.screen == null) return;
        if (!(screen instanceof AbstractContainerScreen<?> container) || !(container.getMenu() instanceof ChestMenu menu)
                || menu != client.player.containerMenu || menu.getRowCount() < 5
                || AutoLoadoutPolicy.menuCapacity(container.getTitle().getString()) <= current.binding
                || !menu.getCarried().isEmpty()) {
            pending = null;
            return;
        }
        if (current.screen == null) {
            current.screen = container;
            current.menu = menu;
            current.actionAt = tick + delay(settings.equipDelay);
        } else if (container != current.screen || menu != current.menu) {
            pending = null;
            return;
        }
        if (tick < current.actionAt) return;
        int slot = AutoLoadoutPolicy.containerSlot(current.binding);
        var entry = menu.getSlot(slot).getItem();
        if (entry.isEmpty()) return;
        var lore = entry.get(DataComponents.LORE);
        List<String> lines = lore == null ? List.of() : lore.lines().stream().map(line -> line.getString()).toList();
        if (entry.is(Items.BARRIER) || AutoLoadoutPolicy.unavailable(entry.getHoverName().getString(), lines)) {
            pending = null;
            return;
        }
        boolean selected = AutoLoadoutPolicy.equipped(lines);
        if (!selected) {
            client.gameMode.handleContainerInput(menu.containerId, slot, 0, ContainerInput.PICKUP, client.player);
        }
        current.close = new AutoLoadoutPolicy.CloseWindow(tick + delay(settings.closeDelay));
    }

    private int delay(int base) {
        return base + ThreadLocalRandom.current().nextInt(settings.variety + 1);
    }

    private static final class Attempt {
        final int binding;
        final ClientPacketListener connection;
        final ClientLevel world;
        final long deadline;
        long actionAt;
        AbstractContainerScreen<?> screen;
        ChestMenu menu;
        AutoLoadoutPolicy.CloseWindow close;

        Attempt(int binding, long deadline, ClientPacketListener connection, ClientLevel world) {
            this.binding = binding;
            this.deadline = deadline;
            this.connection = connection;
            this.world = world;
        }
    }
}
