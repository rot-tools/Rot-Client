package fi.rotclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Left/right click trails inside terminal GUIs. */
public final class DungeonTerminalClickRuntime {
    private record Trail(int x, int y, boolean right, long until) {
    }

    private static final List<Trail> TRAILS = new ArrayList<>();

    private DungeonTerminalClickRuntime() {
    }

    public static void record(AbstractContainerScreen<?> screen, int mouseX, int mouseY, int button) {
        DungeonTerminalClickSettings settings = settings();
        if (!settings.enabled() || screen == null) {
            return;
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        if (DungeonPolicy.detectTerminal(title) == DungeonPolicy.Terminal.NONE) {
            return;
        }
        TRAILS.add(new Trail(mouseX, mouseY,
                button == InputConstants.MOUSE_BUTTON_RIGHT, System.currentTimeMillis() + 1_200L));
        if (TRAILS.size() > 40) {
            TRAILS.removeFirst();
        }
    }

    public static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics) {
        DungeonTerminalClickSettings settings = settings();
        if (!settings.enabled() || screen == null || graphics == null) {
            return;
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        if (DungeonPolicy.detectTerminal(title) == DungeonPolicy.Terminal.NONE) {
            TRAILS.clear();
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Trail> iterator = TRAILS.iterator();
        while (iterator.hasNext()) {
            Trail trail = iterator.next();
            if (trail.until <= now) {
                iterator.remove();
                continue;
            }
            int color = trail.right ? settings.rightColor() : settings.leftColor();
            int radius = Math.max(1, settings.radius());
            int thickness = Math.max(1, settings.thickness());
            graphics.fill(
                    trail.x - radius,
                    trail.y - thickness,
                    trail.x + radius,
                    trail.y + thickness,
                    color);
            graphics.fill(
                    trail.x - thickness,
                    trail.y - radius,
                    trail.x + thickness,
                    trail.y + radius,
                    color);
        }
    }

    private static DungeonTerminalClickSettings settings() {
        return DungeonTerminalClickSettings.from(RotClientClient.qolConfigPublic());
    }
}
