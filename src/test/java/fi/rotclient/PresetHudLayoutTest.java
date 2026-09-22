package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fi.rotclient.PresetHudLayout.Anchor;
import fi.rotclient.PresetHudLayout.Placement;
import fi.rotclient.PresetHudLayout.Size;
import fi.rotclient.PresetHudLayout.Stack;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class PresetHudLayoutTest {

    private static final Map<String, Size> SIZES = Map.of(
            "a", new Size(100, 40),
            "b", new Size(60, 20),
            "c", new Size(80, 30));

    private static Placement at(List<Placement> placements, String id) {
        return placements.stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static List<Placement> resolve(
            int w, int h, Stack... stacks) {
        return PresetHudLayout.resolve(List.of(stacks), w, h, SIZES::get);
    }

    @Test
    void topLeftStackGoesDownFromTheCornerWithAGap() {
        List<Placement> p = resolve(400, 300,
                new Stack(Anchor.TOP_LEFT, List.of("a", "b")));

        assertEquals(12, at(p, "a").x());
        assertEquals(12, at(p, "a").y());
        assertEquals(12, at(p, "b").x());
        assertEquals(12 + 40 + PresetHudLayout.GAP, at(p, "b").y());
    }

    @Test
    void topRightStackIsRightAlignedToTheMargin() {
        List<Placement> p = resolve(400, 300,
                new Stack(Anchor.TOP_RIGHT, List.of("a", "b")));

        assertEquals(400 - 12 - 100, at(p, "a").x());
        // Narrower elements hug the right edge, not the stack's left edge.
        assertEquals(400 - 12 - 60, at(p, "b").x());
        assertEquals(12, at(p, "a").y());
    }

    @Test
    void bottomStackSitsOnTheBottomMarginAndKeepsListOrder() {
        List<Placement> p = resolve(400, 300,
                new Stack(Anchor.BOTTOM_LEFT, List.of("a", "b")));

        int total = 40 + PresetHudLayout.GAP + 20;
        assertEquals(300 - 12 - total, at(p, "a").y());
        assertEquals(300 - 12 - 20, at(p, "b").y());
    }

    @Test
    void middleStackIsVerticallyCentred() {
        List<Placement> p = resolve(400, 300,
                new Stack(Anchor.MIDDLE_LEFT, List.of("a")));

        assertEquals((300 - 40) / 2, at(p, "a").y());
    }

    @Test
    void centreStackIsHorizontallyCentredPerElement() {
        List<Placement> p = resolve(400, 300,
                new Stack(Anchor.TOP_CENTER, List.of("a", "b")));

        assertEquals((400 - 100) / 2, at(p, "a").x());
        assertEquals((400 - 100) / 2 + (100 - 60) / 2, at(p, "b").x());
    }

    @Test
    void stacksOnTheSameLeftAnchorSitSideBySideNeverOverlapping() {
        List<Placement> p = resolve(800, 400,
                new Stack(Anchor.TOP_LEFT, List.of("a")),
                new Stack(Anchor.TOP_LEFT, List.of("c")));

        assertEquals(12, at(p, "a").x());
        assertEquals(12 + 100 + PresetHudLayout.COLUMN_GAP, at(p, "c").x());
        assertFalse(at(p, "a").overlaps(at(p, "c")));
    }

    @Test
    void stacksOnTheSameRightAnchorGrowLeftwards() {
        List<Placement> p = resolve(800, 400,
                new Stack(Anchor.TOP_RIGHT, List.of("a")),
                new Stack(Anchor.TOP_RIGHT, List.of("c")));

        assertEquals(800 - 12 - 100, at(p, "a").x());
        assertEquals(800 - 12 - 100 - PresetHudLayout.COLUMN_GAP - 80, at(p, "c").x());
        assertFalse(at(p, "a").overlaps(at(p, "c")));
    }

    @Test
    void differentAnchorsDoNotShareAColumn() {
        List<Placement> p = resolve(800, 400,
                new Stack(Anchor.TOP_LEFT, List.of("a")),
                new Stack(Anchor.BOTTOM_LEFT, List.of("c")));

        assertEquals(12, at(p, "c").x());
    }

    @Test
    void everythingIsClampedOntoAScreenTooSmallForIt() {
        List<Placement> p = resolve(90, 50,
                new Stack(Anchor.TOP_RIGHT, List.of("a", "b", "c")));

        for (Placement placement : p) {
            assertTrue(placement.x() >= 0 && placement.y() >= 0);
            assertTrue(placement.x() + placement.width() <= Math.max(90, placement.width()));
        }
        // Wider than the screen: pinned to the left edge, never negative.
        assertEquals(0, at(p, "a").x());
    }

    @Test
    void unknownElementsAreSkippedNotGuessed() {
        List<Placement> p = resolve(400, 300,
                new Stack(Anchor.TOP_LEFT, List.of("nope", "a")));

        assertEquals(1, p.size());
        assertEquals(12, at(p, "a").y(),
                "an unknown element must not leave a gap above the next one");
    }

    @Test
    void emptyAndNullStacksAreIgnored() {
        List<Placement> p = PresetHudLayout.resolve(
                java.util.Arrays.asList(
                        null,
                        new Stack(null, List.of("a")),
                        new Stack(Anchor.TOP_LEFT, List.of())),
                400, 300, SIZES::get);

        assertTrue(p.isEmpty());
    }

    private static boolean fits(int w, int h, Stack... stacks) {
        return PresetHudLayout.fits(List.of(stacks), w, h, SIZES::get);
    }

    @Test
    void aLayoutThatFitsIsReportedAsFitting() {
        assertTrue(fits(800, 400,
                new Stack(Anchor.TOP_LEFT, List.of("a", "b")),
                new Stack(Anchor.TOP_RIGHT, List.of("c"))));
    }

    @Test
    void aLayoutTooTallForTheScreenDoesNotFit() {
        assertFalse(fits(400, 60,
                new Stack(Anchor.TOP_LEFT, List.of("a", "b", "c"))));
    }

    @Test
    void columnsFromOppositeEdgesThatMeetDoNotFit() {
        // Each column is on screen, but a 100 + 100 wide pair cannot share a
        // 200 wide screen once the margins are counted. Clamping would hide
        // this, which is why fits() looks at the unclamped positions.
        assertFalse(fits(200, 300,
                new Stack(Anchor.TOP_LEFT, List.of("a")),
                new Stack(Anchor.TOP_RIGHT, List.of("a"))));
    }

    @Test
    void leftAndRightColumnsThatJustClearEachOtherFit() {
        int needed = 12 + 100 + 80 + 12;

        assertTrue(fits(needed, 300,
                new Stack(Anchor.TOP_LEFT, List.of("a")),
                new Stack(Anchor.TOP_RIGHT, List.of("c"))));
        assertFalse(fits(needed - 1, 300,
                new Stack(Anchor.TOP_LEFT, List.of("a")),
                new Stack(Anchor.TOP_RIGHT, List.of("c"))));
    }

    @Test
    void anchorParsingIsForgiving() {
        assertEquals(Anchor.TOP_LEFT, Anchor.parse("top_left"));
        assertEquals(Anchor.TOP_LEFT, Anchor.parse(" TOP-LEFT "));
        assertEquals(Anchor.BOTTOM_RIGHT, Anchor.parse("bottom_right"));
        assertNull(Anchor.parse("left"));
        assertNull(Anchor.parse(null));
    }
}
