package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class QolNumberSettingsTest {
    @Test
    void autoClickerCpsStaysANumericStepper() {
        assertFalse(QolNumberSettings.usesSlider("qol.auto_clicker.cps"));
        assertFalse(QolNumberSettings.usesSlider("qol.auto_clicker.left_cps"));
        assertFalse(QolNumberSettings.usesSlider("qol.auto_clicker.right_cps"));
    }

    @Test
    void playerSizeAndRarityUseSliders() {
        assertTrue(QolNumberSettings.usesSlider("qol.player_size.x"));
        assertTrue(QolNumberSettings.usesSlider("qol.item_rarity.fill_alpha"));
        assertTrue(QolNumberSettings.usesSlider("qol.item_rarity.outline_alpha"));
        QolNumberSettings.Spec spec = QolNumberSettings.spec("qol.player_size.x");
        assertEquals(0.1D, spec.fromFraction(0.0D), 0.0001D);
        assertEquals(2.0D, spec.fromFraction(1.0D), 0.0001D);
        assertEquals(0.5D, spec.fraction(1.05D), 0.0001D);
    }
}
