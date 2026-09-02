package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;

import java.awt.Desktop;
import java.net.URI;

/**
 * Opens external Rot Client links through Minecraft's confirmed URI flow.
 */
final class RotClientLinkOpener {
    private RotClientLinkOpener() {
    }

    static void openConfirmed(Screen parent, String url) {
        if (parent == null || url == null || url.isBlank()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        client.gui.setScreen(new ConfirmLinkScreen(
                accepted -> {
                    if (accepted) {
                        openDirect(url);
                    }
                    client.gui.setScreen(parent);
                },
                url,
                true));
    }

    static void openDirect(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException ignored) {
            return;
        }
        if (!isAllowedExternalUri(uri)) {
            return;
        }
        String safeUrl = uri.toString();
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
                return;
            }
        } catch (Exception ignored) {
            // Fall through to OS-specific launchers.
        }
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", "", safeUrl).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", safeUrl).start();
            } else {
                new ProcessBuilder("xdg-open", safeUrl).start();
            }
        } catch (Exception ignored) {
            // Keep UI responsive if no URI opener is available.
        }
    }

    /** Only HTTPS links to explicitly approved project hosts are opened. */
    private static boolean isAllowedExternalUri(URI uri) {
        if (uri == null || uri.getScheme() == null || uri.getHost() == null) {
            return false;
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }
        String host = uri.getHost().toLowerCase();
        return "github.com".equals(host)
                || host.endsWith(".github.com")
                || "discord.gg".equals(host);
    }
}
