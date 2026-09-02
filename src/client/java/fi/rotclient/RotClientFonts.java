package fi.rotclient;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Clear sans-serif UI type for Rot Client screens and HUD overlays.
 * Vanilla {@code minecraft:default} stays on gameplay GUIs; Rot Client
 * strings are drawn as components with this font description.
 */
final class RotClientFonts {
    static final Identifier ID = Identifier.fromNamespaceAndPath("rotclient", "ui");
    static final FontDescription DESCRIPTION = new FontDescription.Resource(ID);
    static final Style STYLE = Style.EMPTY.withFont(DESCRIPTION);

    private RotClientFonts() {
    }

    static Component component(String text) {
        return Component.literal(text == null ? "" : text).withStyle(STYLE);
    }

    static Component vanilla(String text) {
        return Component.literal(text == null ? "" : text);
    }

    static int vanillaWidth(Font font, String text) {
        if (font == null) {
            return text == null ? 0 : text.length() * 6;
        }
        return font.width(vanilla(text));
    }

    static int width(Font font, String text) {
        if (font == null) {
            return text == null ? 0 : text.length() * 6;
        }
        return font.width(component(text));
    }
}
