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
