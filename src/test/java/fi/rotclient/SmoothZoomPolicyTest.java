package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SmoothZoomPolicyTest {
    @Test
    void easesInAndBackOut() {
        double progress = 0.0D;

        for (int i = 0; i < 60; i++) {
            progress =
                    SmoothZoomPolicy.advanceProgress(
                            progress,
                            true,
                            SmoothZoomPolicy.DEFAULT_SPEED,
                            1.0D / 60.0D);
        }

        assertTrue(progress > 0.99D);

        for (int i = 0; i < 60; i++) {
            progress =
                    SmoothZoomPolicy.advanceProgress(
                            progress,
                            false,
                            SmoothZoomPolicy.DEFAULT_SPEED,
                            1.0D / 60.0D);
        }

        assertTrue(progress < 0.01D);
    }

    @Test
    void magnifiesCurrentFov() {
        assertEquals(
                70,
                SmoothZoomPolicy.applyToFov(
                        70,
                        4.0D,
                        0.0D));

        assertEquals(
                18,
                SmoothZoomPolicy.applyToFov(
                        70,
                        4.0D,
                        1.0D));

        int middle =
                SmoothZoomPolicy.applyToFov(
                        70,
                        4.0D,
                        0.5D);

        assertTrue(
                middle > 18
                        && middle < 70);
    }

    @Test
    void preservesFractionalVanillaFovWhileZooming() {
        float result =
                SmoothZoomPolicy.applyToFov(
                        70.5F,
                        4.0D,
                        1.0D);

        assertTrue(
                result > 17.6F
                        && result < 17.7F);
    }

    @Test
    void configSupportsZoomAndReset() {
        QolUtilityConfig qol =
                new QolUtilityConfig();

        assertTrue(
                qol.isModuleEnabled(
                        "qol.zoom"));

        assertEquals(
                "C",
                qol.readKeybind(
                        "qol.zoom.keybind"));

        assertEquals(
                4.0D,
                qol.readNumber(
                        "qol.zoom.amount"),
                0.0001D);

        assertEquals(
                8.0D,
                qol.readNumber(
                        "qol.zoom.speed"),
                0.0001D);

        qol.writeKeybind(
                "qol.zoom.keybind",
                "V");

        qol.writeNumber(
                "qol.zoom.amount",
                100.0D);

        qol.writeNumber(
                "qol.zoom.speed",
                -100.0D);

        assertEquals(
                "V",
                qol.readKeybind(
                        "qol.zoom.keybind"));

        assertEquals(
                SmoothZoomPolicy.MAX_AMOUNT,
                qol.readNumber(
                        "qol.zoom.amount"),
                0.0001D);

        assertEquals(
                SmoothZoomPolicy.MIN_SPEED,
                qol.readNumber(
                        "qol.zoom.speed"),
                0.0001D);

        qol.setModuleEnabled(
                "qol.zoom",
                false);

        assertFalse(
                qol.isModuleEnabled(
                        "qol.zoom"));

        assertTrue(
                qol.resetModuleToDefaults(
                        "qol.zoom"));

        assertTrue(
                qol.isModuleEnabled(
                        "qol.zoom"));

        assertEquals(
                "C",
                qol.readKeybind(
                        "qol.zoom.keybind"));
    }

    @Test
    void catalogProvidesDedicatedCard() {
        QolUtilityCatalog.ModuleDef module =
                QolUtilityCatalog.findById(
                        "qol.zoom");

        assertNotNull(module);

        assertEquals(
                "Smooth Zoom",
                module.name());

        assertEquals(
                QolUtilityCatalog.Group.RENDER,
                module.group());

        assertFalse(
                QolUtilityCatalog.hasCheatTag(
                        module));

        assertEquals(
                List.of(
                        "qol.zoom.keybind",
                        "qol.zoom.amount",
                        "qol.zoom.speed",
                        "qol.zoom.scroll"),
                module.settings()
                        .stream()
                        .map(
                                QolUtilityCatalog.SettingDef::id)
                        .toList());

        assertTrue(
                QolNumberSettings.usesSlider(
                        "qol.zoom.amount"));

        assertTrue(
                QolNumberSettings.usesSlider(
                        "qol.zoom.speed"));
    }

    @Test
    void wheelUpZoomsInAndWheelDownZoomsOut() {
        double in = SmoothZoomPolicy.scrollAmount(4.0D, 1.0D);
        double out = SmoothZoomPolicy.scrollAmount(4.0D, -1.0D);
        assertTrue(in > 4.0D);
        assertTrue(out < 4.0D);
        // Multiplicative, so a step in and a step back out return to the same level.
        assertEquals(4.0D, SmoothZoomPolicy.scrollAmount(in, -1.0D), 1.0e-9D);
        // Fractional deltas from smooth-scroll wheels move it less than a full notch.
        double half = SmoothZoomPolicy.scrollAmount(4.0D, 0.5D);
        assertTrue(half > 4.0D && half < in);
    }

    @Test
    void scrollingIsClampedAndIgnoresGarbage() {
        assertEquals(SmoothZoomPolicy.MAX_LIVE_AMOUNT, SmoothZoomPolicy.scrollAmount(29.0D, 5.0D), 1.0e-9D);
        assertEquals(SmoothZoomPolicy.MIN_LIVE_AMOUNT, SmoothZoomPolicy.scrollAmount(1.2D, -5.0D), 1.0e-9D);
        assertEquals(4.0D, SmoothZoomPolicy.scrollAmount(4.0D, Double.NaN), 1.0e-9D);
        // A huge burst of wheel events in one call cannot fling it across the whole range.
        assertTrue(SmoothZoomPolicy.scrollAmount(4.0D, 1000.0D) < 4.0D * Math.pow(SmoothZoomPolicy.SCROLL_STEP, 5.1D));
    }

    @Test
    void liveAmountEasesTowardTheScrolledTarget() {
        double amount = 4.0D;
        double previous = amount;
        for (int i = 0; i < 12; i++) {
            amount = SmoothZoomPolicy.advanceAmount(amount, 8.0D, SmoothZoomPolicy.DEFAULT_SPEED, 1.0D / 60.0D);
            assertTrue(amount >= previous && amount <= 8.0D);
            previous = amount;
        }
        assertTrue(amount > 4.0D && amount < 8.0D, "still easing after a fifth of a second");
        for (int i = 0; i < 120; i++) {
            amount = SmoothZoomPolicy.advanceAmount(amount, 8.0D, SmoothZoomPolicy.DEFAULT_SPEED, 1.0D / 60.0D);
        }
        assertEquals(8.0D, amount, 1.0e-9D);
        assertEquals(8.0D, SmoothZoomPolicy.advanceAmount(8.0D, 2.0D, 8.0D, 0.0D), 1.0e-9D, "no time, no movement");
    }

    @Test
    void scrolledAmountCanExceedTheConfiguredRange() {
        // The setting tops out at 10x, but the wheel may go further; the 5 degree floor still holds.
        float fov = SmoothZoomPolicy.applyToFov(70.0F, 12.0D, 1.0D);
        assertTrue(fov > 5.5F && fov < 6.0F);
        assertEquals(5.0F, SmoothZoomPolicy.applyToFov(70.0F, 30.0D, 1.0D), 1.0e-4F);
    }

    @Test
    void scrollToZoomIsOnByDefaultAndResets() {
        QolUtilityConfig qol = new QolUtilityConfig();
        assertTrue(qol.readBoolean("qol.zoom.scroll"));
        qol.writeBoolean("qol.zoom.scroll", false);
        assertFalse(qol.readBoolean("qol.zoom.scroll"));
        assertTrue(qol.resetModuleToDefaults("qol.zoom"));
        assertTrue(qol.readBoolean("qol.zoom.scroll"));
    }

    @Test
    void wheelIsOnlyCapturedWhileTheZoomKeyIsHeld() throws Exception {
        String mixin = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/client/java/fi/rotclient/mixin/MouseHandlerZoomMixin.java"),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(mixin.contains("cancellable = true"));
        assertTrue(mixin.contains("SmoothZoomRuntime.onScroll(vertical)"));
        String json = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/client/resources/rotclient.client.mixins.json"),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(json.contains("MouseHandlerZoomMixin"));
        String runtime = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/client/java/fi/rotclient/SmoothZoomRuntime.java"),
                java.nio.charset.StandardCharsets.UTF_8);
        int scroll = runtime.indexOf("public static boolean onScroll");
        String body = runtime.substring(scroll, runtime.indexOf("private static boolean zoomHeld"));
        assertTrue(body.contains("zoomHeld(client, qol)"));
        assertTrue(body.contains("!qol.zoomScroll"));
    }
}
