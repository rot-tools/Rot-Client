package fi.rotclient;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

import java.util.List;

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

    static Component legacy(String text) {
        List<LegacyMcText.Span> spans = LegacyMcText.parse(text);
        if (spans.isEmpty()) {
            return Component.empty();
        }
        MutableComponent root = Component.empty();
        for (LegacyMcText.Span span : spans) {
            if (span.text().isEmpty()) {
                continue;
            }
            Style style = Style.EMPTY;
            if (span.hasColor()) {
                style = style.withColor(TextColor.fromRgb(span.rgb()));
            }
            if (span.bold()) {
                style = style.withBold(true);
            }
            if (span.italic()) {
                style = style.withItalic(true);
            }
            if (span.underline()) {
                style = style.withUnderlined(true);
            }
            if (span.strike()) {
                style = style.withStrikethrough(true);
            }
            if (span.obfuscated()) {
                style = style.withObfuscated(true);
            }
            root.append(Component.literal(span.text()).withStyle(style));
        }
        return root;
    }

    static int vanillaWidth(Font font, String text) {
        if (font == null) {
            return text == null ? 0 : text.length() * 6;
        }
        return font.width(vanilla(text));
    }

    static int legacyWidth(Font font, String text) {
        if (font == null) {
            return LegacyMcText.strip(text).length() * 6;
        }
        return font.width(legacy(text));
    }

    static int width(Font font, String text) {
        if (font == null) {
            return text == null ? 0 : text.length() * 6;
        }
        return font.width(component(text));
    }
}
