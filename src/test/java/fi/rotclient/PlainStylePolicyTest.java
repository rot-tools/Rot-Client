package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PlainStylePolicyTest {
    @Test
    void viewmodelClampsAndSwingRules() {
        assertEquals(-2.0D, ViewmodelPolicy.clampOffset(-9));
        assertEquals(2.0D, ViewmodelPolicy.clampOffset(9));
        assertEquals(6, ViewmodelPolicy.swingDuration(6, false, false, 0));
        assertEquals(0, ViewmodelPolicy.swingDuration(6, true, true, 20));
        assertEquals(12, ViewmodelPolicy.swingDuration(6, false, false, 12));
        assertFalse(ViewmodelPolicy.applyToStack(false, true));
        assertTrue(ViewmodelPolicy.applyToStack(true, true));
    }

    @Test
    void anvilAndPetMatchers() {
        assertTrue(AnvilHelperPolicy.isAnvilTitle("Anvil"));
        assertTrue(AnvilHelperPolicy.isAnvilMarker("Anvil", true));
        assertTrue(AnvilHelperPolicy.highlightBook("ENCHANTMENT_SHARPNESS", "ENCHANTMENT_SHARPNESS", true));
        assertFalse(AnvilHelperPolicy.highlightBook("ENCHANTMENT_UNKNOWN", "ENCHANTMENT_UNKNOWN", true));
        assertTrue(ActivePetHighlightPolicy.isPetsMenu("(1/2) Pets"));
        assertTrue(ActivePetHighlightPolicy.isActivePet("PET", java.util.List.of("Click to despawn!")));
        assertFalse(ActivePetHighlightPolicy.isActivePet("PET", java.util.List.of("Click to summon!")));
    }

    @Test
    void doubleUseKinds() {
        assertEquals(DoubleUseFixPolicy.Kind.ROD, DoubleUseFixPolicy.kind(true, ""));
        assertEquals(DoubleUseFixPolicy.Kind.DAGGER, DoubleUseFixPolicy.kind(false, "Ability: Attunement"));
        assertTrue(DoubleUseFixPolicy.cancelItemUseOnBlock(true, DoubleUseFixPolicy.Kind.DAGGER, true));
        assertTrue(DoubleUseFixPolicy.replaceBlockUseWithItemUse(true, DoubleUseFixPolicy.Kind.ROD));
    }

    @Test
    void extrasRoundTripThroughConfig() {
        QolUtilityConfig config = new QolUtilityConfig();
        config.setModuleEnabled("qol.info_tooltips", true);
        assertTrue(config.isModuleEnabled("qol.info_tooltips"));
        config.writeBoolean("qol.info_tooltips.item_id", false);
        assertEquals(Boolean.FALSE, config.readBoolean("qol.info_tooltips.item_id"));
        assertTrue(config.writeNumber("qol.viewmodel.offset_x", 0.5D));
        assertEquals(0.5D, config.readNumber("qol.viewmodel.offset_x"));
        assertTrue(config.writeEnum("qol.render_optimizer.vignette", "Both"));
        assertEquals("Both", config.readEnum("qol.render_optimizer.vignette"));
        assertTrue(config.resetModuleToDefaults("qol.info_tooltips"));
        assertFalse(config.isModuleEnabled("qol.info_tooltips"));
    }

    @Test
    void wardrobeStyleAndCustomSlots() {
        QolUtilityConfig config = new QolUtilityConfig();
        assertTrue(config.writeEnum("qol.wardrobe_keybinds.style", "Custom"));
        assertEquals("Custom", config.readEnum("qol.wardrobe_keybinds.style"));
        assertTrue(config.writeKeybind("qol.wardrobe_keybinds.custom_3", "H"));
        assertEquals("H", config.readKeybind("qol.wardrobe_keybinds.custom_3"));
        assertEquals(
                QolSkyblockExtras.STYLE_CUSTOM,
                WardrobeKeybindPolicy.effectiveStyle("Custom", true));
        assertEquals(
                QolSkyblockExtras.STYLE_HOTBAR,
                WardrobeKeybindPolicy.effectiveStyle("Simple", true));
    }

    @Test
    void extraParticlesAndEmptyTooltips() {
        assertTrue(RenderOptimizerPolicy.shouldSuppressExtraParticle(
                true, true, false, false, false,
                RenderOptimizerPolicy.ParticleKind.POOF, false));
        assertTrue(RenderOptimizerPolicy.shouldSuppressExtraParticle(
                true, false, true, false, false,
                RenderOptimizerPolicy.ParticleKind.FIREWORK, true));
        assertFalse(RenderOptimizerPolicy.shouldSuppressExtraParticle(
                true, false, true, false, false,
                RenderOptimizerPolicy.ParticleKind.FIREWORK, false));
        assertTrue(RenderOptimizerPolicy.shouldHideEmptyTooltip(true, true, "  ", "Chest"));
        assertFalse(RenderOptimizerPolicy.shouldHideEmptyTooltip(
                true, true, "  ", "Ultrasequencer (Super)"));
        assertTrue(RenderOptimizerPolicy.shouldHideVignette(true, "Both", false));
        assertFalse(RenderOptimizerPolicy.shouldHideVignette(true, "None", true));
    }
}
