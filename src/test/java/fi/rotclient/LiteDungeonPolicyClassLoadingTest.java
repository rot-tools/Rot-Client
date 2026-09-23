package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class LiteDungeonPolicyClassLoadingTest {
    @Test
    void liteClassesStillLoadWithoutPlusDungeonPolicies() throws Exception {
        Path jar;
        try (var files = Files.list(Path.of("build/libs"))) {
            jar = files.filter(path -> path.getFileName().toString().matches("RotClient-[^-]+\\.jar"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Playable Lite JAR missing"))
                    .toAbsolutePath();
        }
        try (URLClassLoader loader = new URLClassLoader(new URL[] {jar.toUri().toURL()}, getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("fi.rotclient.")) {
                    synchronized (getClassLoadingLock(name)) {
                        Class<?> found = findLoadedClass(name);
                        if (found == null) {
                            found = findClass(name);
                        }
                        if (resolve) {
                            resolveClass(found);
                        }
                        return found;
                    }
                }
                return super.loadClass(name, resolve);
            }
        }) {
            for (String name : new String[] {
                    "fi.rotclient.QolSkyblockExtras",
                    "fi.rotclient.QolNumberSettings",
                    "fi.rotclient.QolOverlayHud",
                    "fi.rotclient.DungeonRuntime",
                    "fi.rotclient.DungeonLeapOverlayRuntime",
                    "fi.rotclient.SkyBlockTooltipRuntime"}) {
                assertDoesNotThrow(() -> {
                    Class<?> type = Class.forName(name, true, loader);
                    type.getDeclaredMethods();
                }, name);
            }
            assertDoesNotThrow(() -> {
                Class<?> type = Class.forName("fi.rotclient.QolSkyblockExtras", true, loader);
                var constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                Object extras = constructor.newInstance();
                var writeNumber = type.getDeclaredMethod("writeNumber", String.class, double.class);
                writeNumber.setAccessible(true);
                for (String id : new String[] {
                        "qol.dungeon_terminals.protect_ms",
                        "qol.dungeon_f7.relic_look_time",
                        "qol.dungeon_f7.relic_spawn_ticks",
                        "qol.dungeon_f7.auto_i4_rotation",
                        "qol.dungeon_esp.trigger_delay",
                        "qol.dungeon_esp.opacity",
                        "qol.dungeon_announce.score_threshold"}) {
                    writeNumber.invoke(extras, id, 1.0D);
                }
                var writeEnum = type.getDeclaredMethod("writeEnum", String.class, String.class);
                writeEnum.setAccessible(true);
                writeEnum.invoke(extras, "qol.dungeon_menus.close_chest.mode", "Any Key");
                writeEnum.invoke(extras, "qol.dungeon_f7.dragon_solo_class", "Healer");
            });
        }
    }
}
