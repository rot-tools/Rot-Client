package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class RotClientUiMotionWiringTest {
    @Test
    void dashboardScreensUseFractionalPoseScrollAndRealtimeClock() throws Exception {
        String mining = Files.readString(
                Path.of("src/client/java/fi/rotclient/MiningUiScreen.java"),
                StandardCharsets.UTF_8);
        String qol = Files.readString(
                Path.of("src/client/java/fi/rotclient/QolUtilityDashboard.java"),
                StandardCharsets.UTF_8);
        String appearance = Files.readString(
                Path.of("src/client/java/fi/rotclient/RotClientAppearanceScreen.java"),
                StandardCharsets.UTF_8);
        assertTrue(mining.contains("RotClientUiClock.beginFrame"));
        assertTrue(mining.contains("pushFractionalScroll"));
        assertTrue(qol.contains("pushFractionalScroll"));
        assertTrue(appearance.contains("pushFractionalScroll"));
        assertTrue(qol.contains("advanceSeconds(RotClientUiClock.seconds())"));
        assertTrue(mining.contains("fractionalPixel(displayedPanelX())"));
        assertTrue(mining.contains("applyLivePanelDrag"));
        assertEquals(0.4F, RotClientUiMotion.fractionalPixel(10.4D), 1.0E-5F);
        assertEquals(-0.4F, RotClientUiMotion.fractionalPixel(10.6D), 1.0E-5F);
    }

    @Test
    void fpsMixinUncapsRotClientScreens() throws Exception {
        String mixins = Files.readString(
                Path.of("src/client/resources/rotclient.client.mixins.json"),
                StandardCharsets.UTF_8);
        assertTrue(mixins.contains("FramerateLimitTrackerMixin"));
        assertTrue(mixins.contains("FramerateLimiterMixin"));
        String mixin = Files.readString(
                Path.of("src/client/java/fi/rotclient/mixin/FramerateLimitTrackerMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(mixin.contains("getFramerateLimit"));
        assertTrue(mixin.contains("getThrottleReason"));
        assertTrue(mixin.contains("ModifyReturnValue"));
        assertTrue(mixin.contains("wantsMonitorRefreshUi"));
        assertTrue(!mixin.contains("@At(\"HEAD\")"));
        String limiter = Files.readString(
                Path.of("src/client/java/fi/rotclient/mixin/FramerateLimiterMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(limiter.contains("limitDisplayFPS"));
        assertTrue(limiter.contains("wantsMonitorRefreshUi"));
        String clock = Files.readString(
                Path.of("src/client/java/fi/rotclient/RotClientUiClock.java"),
                StandardCharsets.UTF_8);
        assertTrue(clock.contains("keepInputFresh"));
        String client = Files.readString(
                Path.of("src/client/java/fi/rotclient/RotClientClient.java"),
                StandardCharsets.UTF_8);
        assertTrue(client.contains("wantsMonitorRefreshUi"));
        assertTrue(client.contains("RotClientAppearanceScreen"));
        assertTrue(client.contains("UI_FRAME_PACER"));
        String fabric = Files.readString(
                Path.of("src/main/resources/fabric.mod.json"),
                StandardCharsets.UTF_8);
        assertTrue(fabric.contains("rotclient.compat.mixins.json"));
        String compat = Files.readString(
                Path.of("src/client/resources/rotclient.compat.mixins.json"),
                StandardCharsets.UTF_8);
        assertTrue(compat.contains("DynamicFpsCheckForRenderMixin"));
        assertTrue(compat.contains("RotClientMixinPlugin"));
    }
}
