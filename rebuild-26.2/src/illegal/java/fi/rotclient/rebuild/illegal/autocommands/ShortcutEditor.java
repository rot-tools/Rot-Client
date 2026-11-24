// SPDX-License-Identifier: MIT
// AutoCommands 2.2.0, copyright 2026 Pickaxe Popup contributors.
package fi.rotclient.rebuild.illegal.autocommands;

import com.mojang.blaze3d.platform.InputConstants;
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
        commandInput = addRenderableWidget(new EditBox(font, x, top + 42, usable, 20, Component.literal("Command")));
        commandInput.setMaxLength(CommandShortcut.MAX_COMMAND_LENGTH);
        commandInput.setHint(Component.literal("/warp mines"));
        commandInput.setValue(command);
        commandInput.setResponder(value -> { command = value; status = ""; });
        labelInput = addRenderableWidget(new EditBox(font, x, top + 81, usable, 20, Component.literal("Custom label")));
        labelInput.setMaxLength(CommandShortcut.MAX_LABEL_LENGTH);
        labelInput.setHint(Component.literal("Automatic destination name"));
        labelInput.setValue(label);
        labelInput.setResponder(value -> { label = value; status = ""; });

        bindButton = addRenderableWidget(Button.builder(bindLabel(), button -> {
            capturing = !capturing;
            button.setMessage(bindLabel());
            status = capturing ? "Esc cancels. Choose a single keyboard key." : "";
        }).bounds(x, top + 109, usable - 64, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Clear key"), button -> {
            key = "";
            capturing = false;
            status = "";
            bindButton.setMessage(bindLabel());
        }).bounds(x + usable - 60, top + 109, 60, 20).build());

        Button remove = addRenderableWidget(Button.builder(Component.literal("Remove"), button -> commit(true))
                .bounds(x, top + 174, 63, 20).build());
        remove.active = index >= 0;
        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(x + usable - 125, top + 174, 59, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save"), button -> commit(false))
                .bounds(x + usable - 61, top + 174, 61, 20).build());
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
            status = "Right Shift opens the Rot Client dashboard.";
            return true;
        } else {
            String candidate = CommandShortcut.cleanKey(InputConstants.getKey(event).getName());
            if (candidate.isEmpty() || (event.modifiers() & AutoCommandsClient.MODIFIERS) != 0) {
                status = "Choose a key without modifiers.";
                return true;
            }
            if (keyInUse(candidate)) {
                status = "That key is assigned to another command.";
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
        graphics.fill(0, 0, width, height, 0x99060910);
        graphics.fill(left, top, left + panelWidth, top + 204, 0xF2192234);
        graphics.fill(left, top, left + panelWidth, top + 2, 0xFFFFCC65);
        graphics.centeredText(font, title, width / 2, top + 11, 0xFFFFCC65);
        graphics.text(font, "Command", left + 12, top + 28, 0xFFEDF3FF);
        graphics.text(font, "Custom label (optional)", left + 12, top + 68, 0xFFEDF3FF);
        var shortcut = new CommandShortcut(CommandShortcut.cleanCommand(command), CommandShortcut.cleanLabel(label), key);
        String line = (shortcut.isTravelCommand() ? "Shortcut label: " : "Popup: ") + shortcut.notice().title();
        graphics.text(font, font.plainSubstrByWidth(line, panelWidth - 24), left + 12, top + 138, 0xFFFFCC65);
        graphics.text(font, font.plainSubstrByWidth(status, panelWidth - 24), left + 12, top + 154, 0xFFFFC48D);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override public void onClose() { minecraft.gui.setScreen(dashboard); }

    @Override public boolean isPauseScreen() { return false; }
}
