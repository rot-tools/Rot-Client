package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class AppearanceLandingPolicyTest {
    @Test
    void submenuHasFiveCardsAndNoMiningHudPage() {
        assertEquals(5, AppearanceLandingPolicy.cards().size());
        assertEquals("qol.appearance.open_dashboard",
                AppearanceLandingPolicy.cards().get(0).actionId());
        assertEquals("colors", AppearanceLandingPolicy.sectionId(
                AppearanceLandingPolicy.OPEN_COLORS));
        assertFalse(AppearanceLandingPolicy.cards().stream().anyMatch(
                card -> card.title().toLowerCase().contains("mining hud")));
        assertTrue(AppearanceLandingPolicy.isAppearanceAction(
                AppearanceLandingPolicy.OPEN_RESET));
        assertEquals(0, AppearanceLandingPolicy.hitIndex(8, 40, 0, 0, 400));
        assertTrue(AppearanceLandingPolicy.hitBack(10, 10, 0, 0));
    }

    @Test
    void emptyContentAndOutsideClicksAreDistinctFromCards() {
        assertEquals(
                AppearanceLandingPolicy.LandingHit.BACK,
                AppearanceLandingPolicy.hitKind(10, 10, 0, 0, 400, 300, 0, 380));
        assertEquals(
                AppearanceLandingPolicy.LandingHit.CARD,
                AppearanceLandingPolicy.hitKind(8, 40, 0, 0, 400, 300, 0, 380));
        assertEquals(
                AppearanceLandingPolicy.LandingHit.EMPTY,
                AppearanceLandingPolicy.hitKind(390, 290, 0, 0, 400, 300, 0, 380));
        assertEquals(
                AppearanceLandingPolicy.LandingHit.OUTSIDE,
                AppearanceLandingPolicy.hitKind(-8, 40, 0, 0, 400, 300, 0, 380));
        assertEquals(
                AppearanceLandingPolicy.LandingHit.OUTSIDE,
                AppearanceLandingPolicy.hitKind(10, -4, 0, 0, 400, 300, 0, 380));
    }
}
