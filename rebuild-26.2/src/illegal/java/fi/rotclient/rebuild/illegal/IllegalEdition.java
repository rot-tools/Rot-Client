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
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public final class IllegalEdition implements ClientEdition {
    public static final int AUTO_ATTACK = 1;
    public static final int AUTO_USE = 2;
    public static final int AUTO_BREAK = 4;
    public static final int REPLACE_VANILLA_ATTACK = 8;
    public static final int REPLACE_VANILLA_USE = 16;
    private static IllegalSettings settings = new IllegalSettings();
    private static AutoLoadout autoLoadout;
    private static AutoClicker autoClicker;
    private static RewardClaim rewardClaim;
    private boolean sprintWasDown;
    private boolean dashboardWasDown;

    @Override
    public void initialize() {
        settings = IllegalSettings.load();
        autoLoadout = new AutoLoadout(settings.autoLoadout);
        autoClicker = new AutoClicker(settings.autoClicker);
        new AutoCommandsClient().register();
        rewardClaim = new RewardClaim(settings);
        rewardClaim.register();
        ClientTickEvents.START_CLIENT_TICK.register(autoClicker::tick);
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
                        }))),
                new ModuleCard("auto_loadout", "Auto Loadout",
                        "Equip a saved loadout with a hotkey on the designated test server.", Category.UTILITIES,
                        List.of(new Toggle("Auto Loadout", () -> settings.autoLoadout.enabled, enabled -> {
                            settings.autoLoadout.enabled = enabled;
                            settings.save();
                        }), new Action("Edit timing and keybinds", parent ->
                                Minecraft.getInstance().gui.setScreen(new AutoLoadoutScreen(parent, settings))))),
                new ModuleCard("reward_claim", "Daily Reward Claim",
                        "Open Hypixel daily rewards with item icons and streak details.", Category.INTERFACE,
                        List.of(new Toggle("Daily Reward Claim", () -> settings.rewardClaim, value -> {
                            settings.rewardClaim = value;
                            settings.save();
                        }))),
                new ModuleCard("auto_clicker", "Auto Clicker",
                        "Repeat attack or use while holding an input in a test world.", Category.COMBAT,
                        List.of(new Toggle("Auto Clicker", () -> settings.autoClicker.enabled, value -> {
                                    settings.autoClicker.enabled = value;
                                    settings.save();
                                }),
                                new Action("Clicker settings", parent ->
                                        Minecraft.getInstance().gui.setScreen(new AutoClickerScreen(parent, settings))))));
    }

    public static int autoClickerActions(Minecraft client) {
        return autoClicker == null ? 0 : autoClicker.takeActions(client);
    }

    public static boolean openRewardBook(BookViewScreen book, BookViewScreen.BookAccess access) {
        return rewardClaim != null && rewardClaim.openBook(book, access);
    }

    public static boolean autoClickerKeyAssigned(int binding) {
        return settings.autoClicker.hasBinding(binding);
    }

    public static boolean autoCommandsEnabled() {
        return settings.autoCommands;
    }

    public static boolean handleAutoLoadoutKey(long window, int action, KeyEvent event) {
        return autoLoadout != null && autoLoadout.onKey(window, action, event);
    }

    public static boolean loadoutKeyAssigned(int key) {
        return settings.autoLoadout.slotForKey(key) >= 0;
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
        autoLoadout.tick(client);
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
        return key == GLFW.GLFW_KEY_RIGHT_SHIFT || loadoutKeyAssigned(key) || autoClickerKeyAssigned(key)
                ? GLFW.GLFW_KEY_UNKNOWN : key;
    }
}
