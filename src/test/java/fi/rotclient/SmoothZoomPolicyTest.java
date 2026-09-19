package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SmoothZoomPolicyTest {
    @Test
    void zoomKeyIsIgnoredWhileAScreenIsOpen() {
        assertTrue(SmoothZoomPolicy.canZoom(true, false, true));
        // Typing "c" in chat or an inventory must not zoom the camera behind the screen.
        assertFalse(SmoothZoomPolicy.canZoom(true, true, true));
        assertFalse(SmoothZoomPolicy.canZoom(false, false, true));
        assertFalse(SmoothZoomPolicy.canZoom(true, false, false));
    }

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
                        "qol.zoom.speed"),
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
}