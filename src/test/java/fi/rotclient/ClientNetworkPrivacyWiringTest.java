package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ClientNetworkPrivacyWiringTest {
    @Test
    void privacyMixinsScrubBrandAndRotClientChannels() throws Exception {
        String hooks = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/FabricHooksPrivacyMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(hooks.contains("insertBranding"));
        assertTrue(hooks.contains("ClientNetworkPrivacyRuntime"));

        String connection = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/ConnectionOutboundPrivacyMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(connection.contains("sanitizeOutbound"));
        assertTrue(connection.contains("shouldBlockOutbound"));

        String mixins = Files.readString(Path.of(
                "src/client/resources/rotclient.client.mixins.json"),
                StandardCharsets.UTF_8);
        assertTrue(mixins.contains("FabricHooksPrivacyMixin"));
        assertTrue(mixins.contains("ConnectionOutboundPrivacyMixin"));

        String runtime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/ClientNetworkPrivacyRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(runtime.contains("catch (LinkageError"));
        assertTrue(runtime.contains("isLocalServer()"));
        assertFalse(runtime.contains("getCurrentServer()"));
        assertFalse(runtime.contains("getConnection()"));
    }

    @Test
    void slayerTransparencyMixinPinsLivingExtractRenderState() throws Exception {
        String source = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/LivingEntityRendererSlayerTransparencyMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains(
                "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;"));
        assertTrue(source.contains("LivingEntityRenderState;F)V"));
    }
}
