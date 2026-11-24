package fi.rotclient.rebuild;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.screens.Screen;

public record ModuleCard(String id, String title, String description, Category category, List<Control> controls) {
    public ModuleCard {
        controls = List.copyOf(controls);
    }

    public sealed interface Control permits Toggle, Action, Keybind {
        String label();
    }

    public record Toggle(String label, BooleanSupplier read, Consumer<Boolean> write) implements Control {}

    public record Action(String label, Consumer<Screen> open) implements Control {}

    /** A GLFW keyboard key code, or -1 when unbound. */
    public record Keybind(String label, IntSupplier read, IntConsumer write) implements Control {}
}
