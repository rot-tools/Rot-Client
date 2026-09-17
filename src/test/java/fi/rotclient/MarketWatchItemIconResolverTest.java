package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

final class MarketWatchItemIconResolverTest {
    @Test
    void componentsNotBoundFallsBackToEmptyIcon() {
        ItemStack result =
                MarketWatchItemIconResolver.recoverComponentsNotBound(
                        new NullPointerException(
                                "Components not bound yet"));

        assertTrue(result.isEmpty());
    }

    @Test
    void unrelatedNullPointerIsNotHidden() {
        NullPointerException original =
                new NullPointerException(
                        "different failure");

        NullPointerException thrown =
                assertThrows(
                        NullPointerException.class,
                        () -> MarketWatchItemIconResolver
                                .recoverComponentsNotBound(
                                        original));

        assertSame(original, thrown);
    }
}