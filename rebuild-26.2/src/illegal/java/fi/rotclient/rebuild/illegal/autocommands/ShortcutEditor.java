// SPDX-License-Identifier: MIT
// AutoCommands 2.2.0, copyright 2026 Pickaxe Popup contributors.
package fi.rotclient.rebuild.illegal.autocommands;

import com.mojang.blaze3d.platform.InputConstants;
import fi.rotclient.rebuild.RotChrome;
import fi.rotclient.rebuild.illegal.IllegalEdition;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

final class ShortcutEditor extends Screen {
    private final CommandDashboard dashboard;
    private final PopupConfig config;
    private final int index;
    private EditBox commandInput;
    private EditBox labelInput;
    private Button bindButton;
    private String command;
    private String label;
    private String key;
    private boolean capturing;
    private String status = "";
    private int left;
    private int top;
    private int panelWidth;

    ShortcutEditor(CommandDashboard dashboard, PopupConfig config, int index, String preset) {
        super(Component.literal(index < 0 ? "Add command" : "Edit command"));
        this.dashboard = dashboard;
        this.config = config;
        this.index = index;
        var initial = index < 0 ? new CommandShortcut(preset, "", "") : config.shortcuts.get(index);
        command = initial.command();
        label = initial.label();
        key = initial.key();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(370, width - 16);
        left = (width - panelWidth) / 2;
        top = (height - 204) / 2;
        int x = left + 12;
        int usable = panelWidth - 24;
        commandInput = addRenderableWidget(RotChrome.field(font, x, top + 42, usable, 20, "Command"));
        commandInput.setMaxLength(CommandShortcut.MAX_COMMAND_LENGTH);
        commandInput.setHint(Component.literal("/warp mines"));
        commandInput.setValue(command);
        commandInput.setResponder(value -> { command = value; status = ""; });
        labelInput = addRenderableWidget(RotChrome.field(font, x, top + 81, usable, 20, "Custom label"));
        labelInput.setMaxLength(CommandShortcut.MAX_LABEL_LENGTH);
        labelInput.setHint(Component.literal("Automatic destination name"));
        labelInput.setValue(label);
        labelInput.setResponder(value -> { label = value; status = ""; });

        bindButton = addRenderableWidget(RotChrome.button(font, bindLabel(), x, top + 109, usable - 64, 20, false, button -> {
            capturing = !capturing;
            button.setMessage(bindLabel());
            status = capturing ? "Esc cancels. Choose a single keyboard key." : "";
        }));
        addRenderableWidget(RotChrome.button(font, Component.literal("Clear key"), x + usable - 60, top + 109, 60, 20, false, button -> {
            key = "";
            capturing = false;
            status = "";
            bindButton.setMessage(bindLabel());
        }));

        Button remove = addRenderableWidget(RotChrome.button(font, Component.literal("Remove"),
                x, top + 174, 63, 20, false, button -> commit(true)));
        remove.active = index >= 0;
        addRenderableWidget(RotChrome.button(font, Component.literal("Cancel"),
                x + usable - 125, top + 174, 59, 20, false, button -> onClose()));
        addRenderableWidget(RotChrome.button(font, Component.literal("Save"),
                x + usable - 61, top + 174, 61, 20, true, button -> commit(false)));
    }

    private Component bindLabel() {
        return Component.literal(capturing ? "Press a key..." : "Key: " + keyLabel(key));
    }

    static String keyLabel(String key) {
        if (!key.isEmpty()) {
            try {
                return InputConstants.getKey(key).getDisplayName().getString();
            } catch (IllegalArgumentException ignored) {
                // A manually edited settings file can contain a key unknown to Minecraft.
            }
        }
        return "Unassigned";
    }

    private boolean keyInUse(String candidate) {
        if (candidate.isEmpty()) return false;
        for (int i = 0; i < config.shortcuts.size(); i++) {
            if (i != index && candidate.equals(config.shortcuts.get(i).key())) return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!capturing) return super.keyPressed(event);
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            status = "";
        } else if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            status = "Assign Right Shift through the Click GUI settings.";
            return true;
        } else {
            String candidate = CommandShortcut.cleanKey(InputConstants.getKey(event).getName());
            if (candidate.isEmpty() || (event.modifiers() & AutoCommandsClient.MODIFIERS) != 0) {
                status = "Choose a key without modifiers.";
                return true;
            }
            if (keyInUse(candidate) || IllegalEdition.loadoutKeyAssigned(event.key())
                    || IllegalEdition.autoClickerKeyAssigned(event.key())) {
                status = "That key is already assigned to a command or loadout.";
                return true;
            } else {
                key = candidate;
                status = "";
                for (var mapping : minecraft.options.keyMappings) {
                    if (mapping.matches(event)) {
                        status = "This key is also used by a Minecraft control.";
                        break;
                    }
                }
            }
        }
        capturing = false;
        bindButton.setMessage(bindLabel());
        return true;
    }

    private void commit(boolean remove) {
        String cleaned = CommandShortcut.cleanCommand(command);
        if (!remove && (cleaned.isEmpty() || keyInUse(key))) {
            status = cleaned.isEmpty() ? "Enter one valid slash command." : "Choose a different key.";
            return;
        }
        var previous = config.shortcuts;
        var updated = new ArrayList<>(previous);
        if (remove) updated.remove(index);
        else {
            var shortcut = new CommandShortcut(cleaned, CommandShortcut.cleanLabel(label), key);
            if (index < 0) updated.add(shortcut);
            else updated.set(index, shortcut);
        }
        config.shortcuts = updated;
        if (!config.save()) {
            config.shortcuts = previous;
            status = "Could not save changes. Please try again.";
            return;
        }
        minecraft.gui.setScreen(dashboard);
        dashboard.showBindings();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        RotChrome.panel(graphics, left, top, panelWidth, 204);
        graphics.fill(left + 12, top + 1, left + 78, top + 3, RotChrome.accent());
        RotChrome.centered(graphics, font, title.getString(), width / 2, top + 11, RotChrome.TEXT);
        RotChrome.text(graphics, font, "Command", left + 12, top + 28, panelWidth - 24, RotChrome.TEXT);
        RotChrome.text(graphics, font, "Custom label (optional)", left + 12, top + 68, panelWidth - 24, RotChrome.TEXT);
        var shortcut = new CommandShortcut(CommandShortcut.cleanCommand(command), CommandShortcut.cleanLabel(label), key);
        String line = (shortcut.isTravelCommand() ? "Shortcut label: " : "Popup: ") + shortcut.notice().title();
        RotChrome.text(graphics, font, line, left + 12, top + 138, panelWidth - 24, RotChrome.MUTED);
        RotChrome.text(graphics, font, status, left + 12, top + 154, panelWidth - 24, 0xFFFFA0B0);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override public void onClose() { minecraft.gui.setScreen(dashboard); }

    @Override public boolean isPauseScreen() { return false; }
}
