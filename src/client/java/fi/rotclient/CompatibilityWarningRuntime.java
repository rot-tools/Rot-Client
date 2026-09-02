package fi.rotclient;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.util.Set;
import java.util.stream.Collectors;

/** Shows one local, non-blocking duplicate-feature warning per client run. */
final class CompatibilityWarningRuntime {
    private static boolean shown;

    private CompatibilityWarningRuntime() {
    }

    static void tick(Minecraft client) {
        if (shown || client == null || client.player == null) {
            return;
        }
        shown = true;
        Set<String> loaded = FabricLoader.getInstance()
                .getAllMods()
                .stream()
                .map(container -> container.getMetadata().getId())
                .collect(Collectors.toUnmodifiableSet());
        String warning = ModCompatibilityPolicy.warning(
                ModCompatibilityPolicy.overlappingSkyBlockMods(loaded));
        if (!warning.isBlank()) {
            client.player.sendSystemMessage(RotClientChat.message(warning));
        }
    }
}
