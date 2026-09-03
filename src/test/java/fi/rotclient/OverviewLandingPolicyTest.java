package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class OverviewLandingPolicyTest {
    @Test
    void idleSessionUsesPlaceholderAndNoNotice() {
        OverviewLandingPolicy.Model model = OverviewLandingPolicy.from(
                false, false, 0, false, "", false, 0L, false);
        assertEquals("—", model.session().value());
        assertEquals("Not collecting", model.session().hint());
        assertEquals(OverviewLandingPolicy.Tone.MUTED, model.session().tone());
        assertEquals("—", model.tracker().value());
        assertEquals("Off", model.tracker().hint());
        assertEquals("empty", model.powder().value());
        assertEquals("Off", model.powder().hint());
        assertNull(model.notice());
        assertFalse(model.miningCardActive());
    }

    @Test
    void runningTrackerAndPowderCopy() {
        OverviewLandingPolicy.Model model = OverviewLandingPolicy.from(
                true, false, 12, true, "Mithril", true, 3L, false);
        assertEquals("RUNNING", model.session().value());
        assertEquals("", model.session().hint());
        assertEquals(OverviewLandingPolicy.Tone.GOOD, model.session().tone());
        assertEquals("Mithril", model.tracker().value());
        assertEquals("On", model.tracker().hint());
        assertEquals("3 chests", model.powder().value());
        assertEquals("On", model.powder().hint());
        assertNull(model.notice());
        assertTrue(model.miningCardActive());
    }

    @Test
    void truncatesLongTrackerTarget() {
        String longName = "Really Very Long Gemstone Target Name";
        OverviewLandingPolicy.Model model = OverviewLandingPolicy.from(
                false, false, 0, true, longName, false, 0L, false);
        assertEquals(
                OverviewLandingPolicy.TARGET_MAX_CHARS,
                model.tracker().value().length());
        assertTrue(model.tracker().value().endsWith("…"));
        assertEquals(
                longName.substring(0, OverviewLandingPolicy.TARGET_MAX_CHARS - 1) + "…",
                model.tracker().value());
    }

    @Test
    void noticeRanksPausedThenEmptyThenStale() {
        assertEquals(
                OverviewLandingPolicy.PAUSED_NOTICE,
                OverviewLandingPolicy.from(
                        false, true, 0, false, "Mithril", false, 0L, true)
                        .notice());
        assertEquals(
                OverviewLandingPolicy.ACTIVE_EMPTY_NOTICE,
                OverviewLandingPolicy.from(
                        true, false, 0, false, "Mithril", false, 0L, true)
                        .notice());
        assertEquals(
                OverviewLandingPolicy.STALE_NOTICE,
                OverviewLandingPolicy.from(
                        true, false, 4, false, "Mithril", false, 0L, true)
                        .notice());
        assertNull(OverviewLandingPolicy.from(
                true, false, 4, false, "Mithril", false, 0L, false)
                .notice());
    }

    @Test
    void layoutUsesSixteenPixelSectionGapsAndSharedHitRects() {
        OverviewLandingPolicy.Layout quiet =
                OverviewLandingPolicy.layout(10, 100, 400, false);
        assertEquals(100 + OverviewLandingPolicy.CHIP_Y_OFFSET, quiet.chipY());
        assertEquals(
                quiet.chipY() + OverviewLandingPolicy.CHIP_HEIGHT
                        + OverviewLandingPolicy.SECTION_GAP,
                quiet.gotoY());
        assertEquals(0, quiet.notice().height());
        assertEquals(
                OverviewLandingPolicy.SECTION_GAP,
                quiet.communityY()
                        - (quiet.events().y() + OverviewLandingPolicy.CARD_HEIGHT));

        OverviewLandingPolicy.Layout noticed =
                OverviewLandingPolicy.layout(10, 100, 400, true);
        int expectedNoticeY = noticed.chipY()
                + OverviewLandingPolicy.CHIP_HEIGHT
                + OverviewLandingPolicy.SECTION_GAP;
        assertEquals(expectedNoticeY, noticed.notice().y());
        assertEquals(
                expectedNoticeY
                        + OverviewLandingPolicy.NOTICE_HEIGHT
                        + OverviewLandingPolicy.SECTION_GAP,
                noticed.gotoY());

        assertEquals(
                OverviewLandingPolicy.Hit.SESSION,
                OverviewLandingPolicy.hit(quiet.session().x() + 2, quiet.chipY() + 2, quiet));
        assertEquals(
                OverviewLandingPolicy.Hit.TRACKER,
                OverviewLandingPolicy.hit(quiet.tracker().x() + 2, quiet.chipY() + 2, quiet));
        assertEquals(
                OverviewLandingPolicy.Hit.POWDER,
                OverviewLandingPolicy.hit(quiet.powder().x() + 2, quiet.chipY() + 2, quiet));
        assertEquals(
                OverviewLandingPolicy.Hit.MODULES,
                OverviewLandingPolicy.hit(quiet.modules().x() + 2, quiet.modules().y() + 2, quiet));
        assertEquals(
                OverviewLandingPolicy.Hit.LOOK,
                OverviewLandingPolicy.hit(quiet.look().x() + 2, quiet.look().y() + 2, quiet));
        assertEquals(
                OverviewLandingPolicy.Hit.MINING,
                OverviewLandingPolicy.hit(quiet.mining().x() + 2, quiet.mining().y() + 2, quiet));
        assertEquals(
                OverviewLandingPolicy.Hit.EVENTS,
                OverviewLandingPolicy.hit(quiet.events().x() + 2, quiet.events().y() + 2, quiet));
        assertEquals(
                OverviewLandingPolicy.Hit.NONE,
                OverviewLandingPolicy.hit(0, 0, quiet));
    }
}
