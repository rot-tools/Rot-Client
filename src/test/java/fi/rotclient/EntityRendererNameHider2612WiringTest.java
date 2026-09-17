package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class EntityRendererNameHider2612WiringTest {
    @Test
    void uses2612ExtractRenderStateHookForNameAndScoreText() throws Exception {
        String source = Files.readString(
                Path.of("src/client/java/fi/rotclient/mixin/EntityRendererNameHiderMixin.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains(
                "extractRenderState(Lnet/minecraft/world/entity/Entity;"
                        + "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V"));

        assertTrue(source.contains("state.nameTag != null"));
        assertTrue(source.contains("state.scoreText != null"));

        assertTrue(source.contains(
                "state.nameTag = SkyBlockUtilityRuntime.rewriteNameTag("
                        + "NameHiderRuntime.apply(state.nameTag))"));

        assertTrue(source.contains(
                "state.scoreText = SkyBlockUtilityRuntime.rewriteNameTag("
                        + "NameHiderRuntime.apply(state.scoreText))"));

        assertFalse(source.contains(
                "extractNameTags(Lnet/minecraft/world/entity/Entity;"
                        + "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V"));
    }
}