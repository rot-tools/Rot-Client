package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class CustomScoreboardRefreshPolicyTest {
    @Test
    void emptyCacheAlwaysRecomposes() {
        assertTrue(CustomScoreboardRefreshPolicy.shouldRecompose(
                1_000L,
                990L,
                false));
    }

    @Test
    void rowsAreReusedInsideFiftyMillisecondWindow() {
        assertFalse(CustomScoreboardRefreshPolicy.shouldRecompose(
                1_049L,
                1_000L,
                true));
    }

    @Test
    void rowsRecomposeAtFiftyMilliseconds() {
        assertTrue(CustomScoreboardRefreshPolicy.shouldRecompose(
                1_050L,
                1_000L,
                true));
    }

    @Test
    void clockMovingBackwardsForcesRefresh() {
        assertTrue(CustomScoreboardRefreshPolicy.shouldRecompose(
                900L,
                1_000L,
                true));
    }
}