package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ModCompatibilityPolicyTest {
    @Test
    void optionalAndExactCollisionMixinsAreGated() {
        assertFalse(ModCompatibilityPolicy.shouldApplyMixin(
                "fi.rotclient.mixin.DynamicFpsCheckForRenderMixin",
                Set.of()));
        assertTrue(ModCompatibilityPolicy.shouldApplyMixin(
                "fi.rotclient.mixin.DynamicFpsCheckForRenderMixin",
                Set.of("dynamic_fps")));
        assertFalse(ModCompatibilityPolicy.shouldApplyMixin(
                "fi.rotclient.mixin.LivingEntityPlayerAnimalsMixin",
                Set.of("SkyOcean")));
        assertFalse(ModCompatibilityPolicy.shouldApplyMixin(
                "fi.rotclient.mixin.ItemModelResolverAnimationMixin",
                Set.of("skyocean")));
        assertTrue(ModCompatibilityPolicy.shouldApplyMixin(
                "fi.rotclient.mixin.SoundEngineSlayerMixin",
                Set.of("skyhanni")));
    }

    @Test
    void overlapWarningIsStableAndExcludesLibraries() {
        List<String> overlaps =
                ModCompatibilityPolicy.overlappingSkyBlockMods(Set.of(
                        "fabric-api", "owo", "SkyHanni", "nofrills"));

        assertEquals(List.of("nofrills", "skyhanni"), overlaps);
        assertTrue(ModCompatibilityPolicy.warning(overlaps)
                .contains("nofrills, skyhanni"));
        assertEquals("", ModCompatibilityPolicy.warning(List.of()));
    }
}
