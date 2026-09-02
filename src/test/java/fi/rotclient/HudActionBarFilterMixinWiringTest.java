package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Structural wiring guard for the MC 26.2 action-bar overlay ingress.
 * Catches a wrong mixin target class (e.g. Gui vs Hud) before runtime.
 */
final class HudActionBarFilterMixinWiringTest {
    private static final String HUD_INTERNAL = "net/minecraft/client/gui/Hud.class";
    private static final String GUI_INTERNAL = "net/minecraft/client/gui/Gui.class";
    private static final String OVERLAY_METHOD = "setOverlayMessage";
    private static final String OVERLAY_DESCRIPTOR =
            "(Lnet/minecraft/network/chat/Component;Z)V";

    @Test
    void minecraft26HudOwnsSetOverlayMessage() throws Exception {
        Path clientJar = requireClientOnlyJar();
        try (JarFile jar = new JarFile(clientJar.toFile())) {
            JarEntry hud = jar.getJarEntry(HUD_INTERNAL);
            JarEntry gui = jar.getJarEntry(GUI_INTERNAL);
            assertNotNull(hud, "Hud.class missing from " + clientJar);
            assertNotNull(gui, "Gui.class missing from " + clientJar);

            assertTrue(
                    classBytesContainMethodUtf8(jar, hud, OVERLAY_METHOD),
                    "Hud must declare setOverlayMessage in MC 26.2");
            assertTrue(
                    classBytesContainUtf8(jar, hud, OVERLAY_DESCRIPTOR)
                            || classBytesContainMethodUtf8(jar, hud, OVERLAY_METHOD),
                    "Hud setOverlayMessage descriptor expected "
                            + OVERLAY_DESCRIPTOR);

            assertFalse(
                    classBytesContainMethodUtf8(jar, gui, OVERLAY_METHOD),
                    "Gui must NOT own setOverlayMessage in MC 26.2 "
                            + "(Batch 1 crash target was wrong class)");

            // Constant-pool evidence for the exact descriptor used by the mixin.
            assertTrue(
                    classBytesContainUtf8(jar, hud, "Component"),
                    "Hud class constant pool should reference Component");
        }
    }

    @Test
    void mixinJsonAndSourceTargetHudNotGui() throws Exception {
        String json = Files.readString(Path.of(
                "src/client/resources/rotclient.client.mixins.json"),
                StandardCharsets.UTF_8);
        assertTrue(json.contains("HudActionBarFilterMixin"));
        assertFalse(json.contains("GuiActionBarFilterMixin"));

        Path mixin = Path.of(
                "src/client/java/fi/rotclient/mixin/HudActionBarFilterMixin.java");
        assertTrue(Files.exists(mixin));
        String source = Files.readString(mixin, StandardCharsets.UTF_8);
        assertTrue(source.contains("@Mixin(Hud.class)"));
        assertTrue(source.contains("setOverlayMessage"));
        assertTrue(source.contains(
                "setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V"));
        assertTrue(source.contains("hideActionLocation()"));
        assertFalse(source.contains("@Mixin(Gui.class)"));
        assertFalse(Files.exists(Path.of(
                "src/client/java/fi/rotclient/mixin/GuiActionBarFilterMixin.java")));
    }

    @Test
    void actionBarFilterHelperStillFragmentScoped() {
        Optional<String> filtered = SkyBlockStatBarParser.filterActionBar(
                "❤ 100/200   Quest complete!   ✎ 50/100",
                true,
                false,
                true,
                false,
                false,
                false);
        assertTrue(filtered.isPresent());
        assertTrue(filtered.get().contains("Quest complete!"));
        assertFalse(filtered.get().contains("❤"));
        assertFalse(filtered.get().contains("✎"));
    }

    private static Path requireClientOnlyJar() throws IOException {
        return requireMinecraftJar("minecraft-clientOnly");
    }

    private static Path requireMinecraftJar(String nameFragment) throws IOException {
        Path root = Path.of(".").toAbsolutePath().normalize();
        Path loom = root.resolve(".gradle/loom-cache/minecraftMaven/net/minecraft");
        if (!Files.isDirectory(loom)) {
            fail("Missing loom minecraftMaven cache at " + loom);
        }
        try (Stream<Path> stream = Files.walk(loom)) {
            Optional<Path> jar = stream
                    .filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .filter(p -> !p.getFileName().toString().contains("-sources"))
                    .filter(p -> p.toString().contains(nameFragment))
                    .filter(p -> p.toString().contains("26.2"))
                    .findFirst();
            return jar.orElseThrow(() -> new AssertionError(
                    "No " + nameFragment + " 26.2 jar under " + loom));
        }
    }

    private static boolean classBytesContainMethodUtf8(
            JarFile jar,
            JarEntry entry,
            String utf8) throws IOException {
        return classBytesContainUtf8(jar, entry, utf8);
    }

    private static boolean classBytesContainUtf8(
            JarFile jar,
            JarEntry entry,
            String utf8) throws IOException {
        try (InputStream in = jar.getInputStream(entry)) {
            byte[] bytes = in.readAllBytes();
            byte[] needle = utf8.getBytes(StandardCharsets.UTF_8);
            outer:
            for (int i = 0; i <= bytes.length - needle.length; i++) {
                for (int j = 0; j < needle.length; j++) {
                    if (bytes[i + j] != needle[j]) {
                        continue outer;
                    }
                }
                return true;
            }
            return false;
        }
    }
}
