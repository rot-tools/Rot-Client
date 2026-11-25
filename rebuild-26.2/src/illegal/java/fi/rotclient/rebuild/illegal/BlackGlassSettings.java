package fi.rotclient.rebuild.illegal;

import java.util.Set;

record BlackGlassSettings(boolean enabled, Set<Color> colors) {
    static final BlackGlassSettings OFF = new BlackGlassSettings(false, Set.of());

    BlackGlassSettings {
        colors = colors == null ? Set.of() : Set.copyOf(colors);
    }

    Set<Color> activeColors() {
        return enabled ? colors : Set.of();
    }

    enum Color {
        CLEAR("Clear", "glass"),
        TINTED("Tinted", "tinted_glass"),
        WHITE("White", "white"),
        ORANGE("Orange", "orange"),
        MAGENTA("Magenta", "magenta"),
        LIGHT_BLUE("Light Blue", "light_blue"),
        YELLOW("Yellow", "yellow"),
        LIME("Lime", "lime"),
        PINK("Pink", "pink"),
        GRAY("Gray", "gray"),
        LIGHT_GRAY("Light Gray", "light_gray"),
        CYAN("Cyan", "cyan"),
        PURPLE("Purple", "purple"),
        BLUE("Blue", "blue"),
        BROWN("Brown", "brown"),
        GREEN("Green", "green"),
        RED("Red", "red"),
        BLACK("Black", "black");

        final String title;
        final String blockPath;

        Color(String title, String path) {
            this.title = title;
            blockPath = path.endsWith("glass") ? path : path + "_stained_glass";
        }
    }
}
