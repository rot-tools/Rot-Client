package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class SlayerHighlightPolicyTest {
    private static final SlayerHighlightPolicy.Options OPTIONS =
            new SlayerHighlightPolicy.Options(true, true, true, false, true, 32.0D);

    @Test
    void targetLinesUseTheSameRoleAndOwnershipRulesAsHighlights() {
        assertTrue(SlayerHighlightPolicy.shouldDrawTargetLine(
                SlayerPolicy.EntityRole.BOSS, true, 20.0D, OPTIONS));
        assertTrue(SlayerHighlightPolicy.shouldDrawTargetLine(
                SlayerPolicy.EntityRole.MINIBOSS, true, 20.0D, OPTIONS));
        assertFalse(SlayerHighlightPolicy.shouldDrawTargetLine(
                SlayerPolicy.EntityRole.DEMON, true, 20.0D, OPTIONS));
        assertFalse(SlayerHighlightPolicy.shouldDrawTargetLine(
                SlayerPolicy.EntityRole.BOSS, false, 20.0D, OPTIONS));
    }

    @Test
    void voidgloomHighlightsStayOnTheLocalPlayersFight() {
        assertLocalFightOnly(SlayerPolicy.SlayerType.VOIDGLOOM);
        assertTrue(SlayerHighlightPolicy.shouldDrawLocalFightMarker(true, 12.0D));
        assertFalse(SlayerHighlightPolicy.shouldDrawLocalFightMarker(true, 28.1D));
        assertFalse(SlayerHighlightPolicy.shouldDrawLocalFightMarker(false, 4.0D));
        assertTrue(SlayerHighlightPolicy.shouldDrawLocalFightMarker(false, Double.NaN, true, 12.0D));
        assertFalse(SlayerHighlightPolicy.shouldDrawLocalFightMarker(false, Double.NaN, true, 40.0D));
        assertFalse(SlayerHighlightPolicy.shouldDrawLocalFightMarker(false, Double.NaN, false, 4.0D));
        assertTrue(SlayerHighlightPolicy.shouldDrawLocalFightMarker(true, 40.0D, true, 8.0D));
        assertFalse(SlayerHighlightPolicy.shouldDrawLocalFightMarker(true, 40.0D, true, 40.0D));
    }

    @Test
    void yangGlyphDoesNotLatchAnotherPlayersNearbyBeacon() {
        assertFalse(SlayerHighlightPolicy.shouldTrackYangGlyph(new SlayerHighlightPolicy.YangGlyphTrack(
                false, false, false, true, 4.0D, Double.NaN, 8.0D)),
                "quest alone is not a throw");
        assertFalse(SlayerHighlightPolicy.shouldTrackYangGlyph(new SlayerHighlightPolicy.YangGlyphTrack(
                false, false, true, true, 22.0D, 4.0D, 8.0D)),
                "foreign Voidgloom is closer");
        assertFalse(SlayerHighlightPolicy.shouldTrackYangGlyph(new SlayerHighlightPolicy.YangGlyphTrack(
                false, true, true, true, 4.0D, 30.0D, 8.0D)),
                "this throw already claimed a glyph");
        assertFalse(SlayerHighlightPolicy.shouldTrackYangGlyph(new SlayerHighlightPolicy.YangGlyphTrack(
                false, false, true, true, 18.0D, Double.NaN, 8.0D)),
                "first sight must be at the owned throw origin");
        assertTrue(SlayerHighlightPolicy.shouldTrackYangGlyph(new SlayerHighlightPolicy.YangGlyphTrack(
                false, false, true, true, 4.0D, 22.0D, 8.0D)));
        assertTrue(SlayerHighlightPolicy.shouldTrackYangGlyph(new SlayerHighlightPolicy.YangGlyphTrack(
                true, true, true, true, 40.0D, 4.0D, 35.0D)),
                "keep the latched stand after it flies");
        assertFalse(SlayerHighlightPolicy.shouldTrackYangGlyph(new SlayerHighlightPolicy.YangGlyphTrack(
                true, true, true, true, 40.0D, 4.0D, 49.0D)));
        assertTrue(SlayerHighlightPolicy.shouldAdoptSittingYangGlyph(
                true,
                new SlayerHighlightPolicy.YangGlyphTrack(
                        false, true, true, true, 40.0D, 4.0D, 35.0D)));
        assertFalse(SlayerHighlightPolicy.shouldAdoptSittingYangGlyph(
                false,
                new SlayerHighlightPolicy.YangGlyphTrack(
                        false, true, true, true, 4.0D, 30.0D, 8.0D)));
        assertTrue(SlayerHighlightPolicy.closerToLocalBoss(4.0D, 22.0D));
        assertFalse(SlayerHighlightPolicy.closerToLocalBoss(22.0D, 4.0D));
        assertTrue(SlayerHighlightPolicy.closerToLocalBoss(4.0D, Double.NaN));
    }

    @Test
    void everySlayerFamilyHighlightStaysOnTheLocalPlayersFight() {
        for (SlayerPolicy.SlayerType type : SlayerPolicy.SlayerType.values()) {
            assertLocalFightOnly(type);
        }
    }

    private static void assertLocalFightOnly(SlayerPolicy.SlayerType type) {
        SlayerHighlightPolicy.Options showOthers =
                new SlayerHighlightPolicy.Options(false, true, true, true, true, 32.0D);
        assertFalse(SlayerHighlightPolicy.shouldHighlight(
                SlayerPolicy.EntityRole.BOSS, type, false, false, showOthers), type.name());
        assertTrue(SlayerHighlightPolicy.shouldHighlight(
                SlayerPolicy.EntityRole.BOSS, type, true, true, showOthers), type.name());
        assertTrue(SlayerHighlightPolicy.shouldHighlight(
                SlayerPolicy.EntityRole.MINIBOSS, type, false, true, OPTIONS), type.name());
        assertFalse(SlayerHighlightPolicy.shouldHighlight(
                SlayerPolicy.EntityRole.MINIBOSS, type, false, false, OPTIONS), type.name());
    }

    @Test
    void targetLinesRespectTheirOptInAndConfiguredRange() {
        assertFalse(SlayerHighlightPolicy.shouldDrawTargetLine(
                SlayerPolicy.EntityRole.BOSS, true, 32.1D, OPTIONS));
        assertFalse(SlayerHighlightPolicy.shouldDrawTargetLine(
                SlayerPolicy.EntityRole.BOSS, true, Double.NaN, OPTIONS));
        assertFalse(SlayerHighlightPolicy.shouldDrawTargetLine(
                SlayerPolicy.EntityRole.BOSS, true, 20.0D,
                new SlayerHighlightPolicy.Options(true, true, true, true, false, 32.0D)));
    }
}
