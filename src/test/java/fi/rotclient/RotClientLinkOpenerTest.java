package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RotClientLinkOpenerTest {
    @Test
    void onlyApprovedHttpsHostsAreAllowed() throws Exception {
        Method method = RotClientLinkOpener.class.getDeclaredMethod(
                "isAllowedExternalUri", URI.class);
        method.setAccessible(true);

        assertTrue((boolean) method.invoke(null,
                URI.create("https://github.com/rot-tools/Rot-Client")));
        assertTrue((boolean) method.invoke(null,
                URI.create("https://github.com/rot-tools/Rot-Client/issues")));
        assertTrue((boolean) method.invoke(null,
                URI.create("https://discord.gg/8UpMfvZugq")));
        assertFalse((boolean) method.invoke(null,
                URI.create("http://github.com/rot-tools/Rot-Client")));
        assertFalse((boolean) method.invoke(null,
                URI.create("https://evil.example/phish")));
        assertFalse((boolean) method.invoke(null,
                URI.create("https://discord.gg.evil.example/8UpMfvZugq")));
        assertFalse((boolean) method.invoke(null,
                URI.create("file:///C:/Windows/System32")));
        assertFalse((boolean) method.invoke(null,
                URI.create("javascript:alert(1)")));
    }
}
