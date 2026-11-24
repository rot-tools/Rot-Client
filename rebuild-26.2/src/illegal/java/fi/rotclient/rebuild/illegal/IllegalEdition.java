package fi.rotclient.rebuild.illegal;

import fi.rotclient.rebuild.Category;
import fi.rotclient.rebuild.ClientEdition;
import fi.rotclient.rebuild.DashboardScreen;
import fi.rotclient.rebuild.ModuleCard;
import fi.rotclient.rebuild.ModuleCard.Action;
import fi.rotclient.rebuild.ModuleCard.Keybind;
import fi.rotclient.rebuild.ModuleCard.Toggle;
import fi.rotclient.rebuild.RebuildClient;
import fi.rotclient.rebuild.illegal.autocommands.AutoCommandsClient;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class IllegalEdition implements ClientEdition {
    private static IllegalSettings settings = new IllegalSettings();
    private boolean sprintWasDown;
    private boolean dashboardWasDown;

    @Override
    public void initialize() {
        settings = IllegalSettings.load();
        new AutoCommandsClient().register();
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    @Override
    public List<ModuleCard> modules() {
        return List.of(
                new ModuleCard("click_gui", "Click GUI",
                        "Open or close the dashboard with an additional keyboard shortcut.", Category.INTERFACE,
                        List.of(new Toggle("Use custom shortcut", () -> settings.clickGui, value -> {
                            settings.clickGui = value;
                            settings.save();
                        }), new Keybind("Dashboard key", () -> settings.clickGuiKey, value -> {
                            settings.clickGuiKey = configurableKey(value);
                            dashboardWasDown = true;
                            settings.save();
                        }))),
                new ModuleCard("fullbright_night", "Fullbright and Night",
                        "Change the client lightmap or sky for the current world.", Category.RENDER,
                        List.of(
                                new Toggle("Use Fullbright", IllegalEdition::fullbrightEnabled,
                                        value -> setLighting(FullbrightNightPolicy.setFullbright(lighting(), value))),
                                new Toggle("Always Night", IllegalEdition::alwaysNightEnabled,
                                        value -> setLighting(FullbrightNightPolicy.setAlwaysNight(lighting(), value))),
                                new Toggle("Allow both", () -> settings.forceBoth,
                                        value -> setLighting(FullbrightNightPolicy.setForceBoth(lighting(), value))))),
                new ModuleCard("auto_sprint", "Auto Sprint",
                        "Sustain vanilla sprint input while enabled on the test server.", Category.UTILITIES,
                        List.of(new Toggle("Auto Sprint", IllegalEdition::autoSprintEnabled, this::setAutoSprint),
                                new Keybind("Toggle key", () -> settings.autoSprintKey, value -> {
                                    settings.autoSprintKey = configurableKey(value);
                                    settings.save();
                                }))),
                new ModuleCard("autocommands", "AutoCommands",
                        "Send saved commands with one keypress and show optional warp feedback.", Category.UTILITIES,
                        List.of(new Toggle("AutoCommands", IllegalEdition::autoCommandsEnabled, value -> {
                            settings.autoCommands = value;
                            settings.save();
                        }), new Action("Edit shortcuts and popup style", parent -> {
                            Minecraft client = Minecraft.getInstance();
                            var screen = AutoCommandsClient.settingsScreen(parent);
                            if (screen != parent) client.gui.setScreen(screen);
                        }))));
    }

    public static boolean autoCommandsEnabled() {
        return settings.autoCommands;
    }

    public static boolean autoSprintEnabled() {
        return settings.autoSprint;
    }

    public static boolean fullbrightEnabled() {
        return settings.fullbright;
    }

    public static boolean alwaysNightEnabled() {
        return settings.alwaysNight;
    }

    private void setAutoSprint(boolean enabled) {
        settings.autoSprint = enabled;
        settings.save();
    }

    private static FullbrightNightPolicy.LightingState lighting() {
        return new FullbrightNightPolicy.LightingState(settings.fullbright, settings.alwaysNight, settings.forceBoth);
    }

    private static void setLighting(FullbrightNightPolicy.LightingState next) {
        boolean wasNight = settings.alwaysNight;
        settings.fullbright = next.fullbright();
        settings.alwaysNight = next.alwaysNight();
        settings.forceBoth = next.forceBoth();
        settings.save();
        if (wasNight != next.alwaysNight()) {
            FullbrightNightRuntime.onAlwaysNightChanged(wasNight, next.alwaysNight(), Minecraft.getInstance().level != null);
        }
    }

    private void tick(Minecraft client) {
        long handle = client.getWindow().handle();
        boolean sprintDown = keyDown(handle, settings.autoSprintKey);
        boolean dashboardDown = settings.clickGui && keyDown(handle, settings.clickGuiKey);
        boolean sprintPressed = sprintDown && !sprintWasDown;
        boolean dashboardPressed = dashboardDown && !dashboardWasDown;
        sprintWasDown = sprintDown;
        dashboardWasDown = dashboardDown;
        if (!client.isWindowActive() || client.gui.overlay() != null) return;
        var screen = client.gui.screen();
        if (dashboardPressed) {
            if (screen == null) {
                if (client.player != null) RebuildClient.openDashboard(null);
                return;
            }
            if (screen instanceof DashboardScreen dashboard && !dashboard.isEditing()) {
                screen.onClose();
                return;
            }
        }
        if (sprintPressed && client.player != null && screen == null) {
            setAutoSprint(!settings.autoSprint);
        }
    }

    private static boolean keyDown(long handle, int key) {
        return key >= GLFW.GLFW_KEY_SPACE && key <= GLFW.GLFW_KEY_LAST
                && key != GLFW.GLFW_KEY_RIGHT_SHIFT
                && GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
    }

    private static int configurableKey(int key) {
        return key == GLFW.GLFW_KEY_RIGHT_SHIFT ? GLFW.GLFW_KEY_UNKNOWN : key;
    }
}
