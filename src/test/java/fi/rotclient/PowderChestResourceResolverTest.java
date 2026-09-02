package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PowderChestResourceResolverTest {
    private final PowderChestResourceResolver resolver =
            new PowderChestResourceResolver(new MiningResourceCatalog());

    @Test
    void resolvesGemstonePowderAsCurrency() {
        PowderChestResourceResolver.ResolvedReward resolved =
                resolver.resolveCurrency("Gemstone Powder");

        assertTrue(resolved.known());
        assertEquals("GEMSTONE_POWDER", resolved.resource().resourceId());
        assertEquals("Gemstone Powder", resolved.resource().displayName());
        assertEquals(
                MiningSessionResource.ResourceKind.CURRENCY,
                resolved.resource().kind());
    }

    @Test
    void resolvesMithrilPowderAsCurrency() {
        PowderChestResourceResolver.ResolvedReward resolved =
                resolver.resolveCurrency("Mithril Powder");

        assertTrue(resolved.known());
        assertEquals("MITHRIL_POWDER", resolved.resource().resourceId());
        assertEquals("Mithril Powder", resolved.resource().displayName());
        assertEquals(
                MiningSessionResource.ResourceKind.CURRENCY,
                resolved.resource().kind());
    }

    @Test
    void unknownCurrencyNameIsUnresolved() {
        PowderChestResourceResolver.ResolvedReward resolved =
                resolver.resolveCurrency("Yoggie Powder");

        assertFalse(resolved.known());
        assertEquals(
                MiningSessionResource.ResourceKind.CURRENCY,
                resolved.resource().kind());
    }

    @Test
    void powderNamesDoNotResolveAsItems() {
        assertFalse(resolver.resolveItem("Gemstone Powder").known());
        assertFalse(resolver.resolveItem("Mithril Powder").known());
    }

    @Test
    void resolvesCompactedHardStoneAsAKnownChestItem() {
        PowderChestResourceResolver.ResolvedReward resolved =
                resolver.resolveItem("Compacted Hard Stone");

        assertTrue(resolved.known());
        assertEquals("COMPACTED_HARD_STONE", resolved.resource().resourceId());
        assertEquals("Compacted Hard Stone", resolved.resource().displayName());
    }
}
