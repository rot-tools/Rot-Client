package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Structural safety contract for the optional local Discord project-feed bootstrap.
 */
final class RotClientDiscordFeedBootstrapTest {
    private static final Path SCRIPT = Path.of(
            "scripts/ensure-discord-project-feed.ps1");

    @Test
    void bootstrapIsIdempotentLocalOnlyAndSecretSafe() throws Exception {
        assertTrue(Files.isRegularFile(SCRIPT), "Discord feed bootstrap script is missing");

        String script = Files.readString(SCRIPT, StandardCharsets.UTF_8);
        assertTrue(script.contains("data/health.json")
                        || script.contains("data\\health.json"),
                "Bootstrap must use the bot health file to avoid duplicate processes");
        assertTrue(script.contains("Get-Process"),
                "Bootstrap must verify that the recorded process is alive");
        assertTrue(script.contains("-WindowStyle Hidden"),
                "Background bot startup must not open a visible console window");
        assertTrue(script.contains("ROT_CLIENT_DISCORD_BOT_PATH"),
                "Portable environment-variable configuration is required");
        assertTrue(script.contains("rotclient-dev.local.json"),
                "An ignored local configuration fallback is required");
        assertFalse(script.contains("DISCORD_TOKEN"),
                "The bootstrap must not read, print, or forward Discord credentials");
        assertFalse(script.matches("(?s).*\\b[A-Z]:\\\\Users\\\\[^\\s]+.*"),
                "The public bootstrap must not contain a personal machine path");
    }

    @Test
    void repositoryKeepsLocalBootstrapConfigurationPrivate() throws Exception {
        String ignore = Files.readString(Path.of(".gitignore"), StandardCharsets.UTF_8);
        assertTrue(ignore.lines().anyMatch("/rotclient-dev.local.json"::equals),
                "Local Discord bot path configuration must be ignored");

        String agents = Files.readString(Path.of("AGENTS.md"), StandardCharsets.UTF_8);
        assertTrue(agents.contains("ensure-discord-project-feed.ps1"));
        assertTrue(agents.contains("Do not commit credentials"));
        assertFalse(agents.matches("(?s).*\\b[A-Z]:\\\\Users\\\\[^\\s]+.*"));
    }
}
