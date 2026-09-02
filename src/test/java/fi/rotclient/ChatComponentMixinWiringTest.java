package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ChatComponentMixinWiringTest {
    @Test
    void cancellableChatInjectionMatchesMinecraft26Descriptor() throws Exception {
        String source = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/ChatComponentNameHiderMixin.java"));
        assertTrue(source.contains("MessageSignature signature"));
        assertTrue(source.contains("GuiMessageSource source"));
        assertTrue(source.contains("GuiMessageTag tag"));
        assertTrue(source.contains("CallbackInfo ci"));
    }
}
