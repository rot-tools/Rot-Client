package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Optional;

/**
 * Applies {@link NameHiderPolicy} to on-screen Components and strings.
 * Never sends a rewritten name to the server.
 */
public final class NameHiderRuntime {
    private static int tickGeneration;
    private static int cachedGeneration = Integer.MIN_VALUE;
    private static Context cachedContext;

    private NameHiderRuntime() {
    }

    static void beginTick() {
        tickGeneration++;
    }

    public static String apply(String text) {
        Context context = context();
        if (context == null || text == null || text.isEmpty()) {
            return text;
        }
        if (!NameHiderPolicy.containsUsername(text, context.username())) {
            return text;
        }
        return NameHiderPolicy.replaceUsername(text, context.username(), context.display());
    }

    public static Component apply(Component component) {
        Context context = context();
        if (context == null || component == null) {
            return component;
        }
        String plain = component.getString();
        if (!NameHiderPolicy.containsUsername(plain, context.username())) {
            return component;
        }
        MutableComponent rebuilt = Component.empty();
        component.visit((style, text) -> {
            rebuilt.append(rewriteSpan(text, style, context));
            return Optional.empty();
        }, Style.EMPTY);
        if (NameHiderPolicy.containsUsername(rebuilt.getString(), context.username())) {
            return Component.literal(
                    NameHiderPolicy.replaceUsername(plain, context.username(), context.display()));
        }
        return rebuilt;
    }

    private static Component rewriteSpan(String text, Style style, Context context) {
        if (!NameHiderPolicy.containsUsername(text, context.username())) {
            return Component.literal(text).setStyle(style);
        }
        MutableComponent out = Component.empty();
        int cursor = 0;
        int match;
        while ((match = NameHiderPolicy.indexOfUsername(text, context.username(), cursor)) >= 0) {
            if (match > cursor) {
                out.append(Component.literal(text.substring(cursor, match)).setStyle(style));
            }
            Style nameStyle = context.obfuscate()
                    ? style.withObfuscated(true)
                    : style;
            out.append(Component.literal(context.display()).setStyle(nameStyle));
            cursor = match + context.username().length();
        }
        if (cursor < text.length()) {
            out.append(Component.literal(text.substring(cursor)).setStyle(style));
        }
        return out;
    }

    private static Context context() {
        if (cachedGeneration == tickGeneration) {
            return cachedContext;
        }
        cachedGeneration = tickGeneration;
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null) {
            cachedContext = null;
            return null;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (qol == null || !qol.nameHiderEnabled) {
            cachedContext = null;
            return null;
        }
        String username = player.getScoreboardName();
        if (username == null || username.length() < 3) {
            cachedContext = null;
            return null;
        }
        NameHiderPolicy.Mode mode = NameHiderPolicy.Mode.fromConfig(qol.nameHiderMode);
        String display = NameHiderPolicy.displayName(mode, username, qol.nameHiderCustomName);
        if (display.isEmpty()) {
            cachedContext = null;
            return null;
        }
        cachedContext = new Context(username, display, mode == NameHiderPolicy.Mode.SCRAMBLE);
        return cachedContext;
    }

    private record Context(String username, String display, boolean obfuscate) {
    }
}
