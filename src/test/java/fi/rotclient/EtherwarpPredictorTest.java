package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

final class EtherwarpPredictorTest {
    @Test
    void validTargetPrediction() {
        Set<String> solids = Set.of("0,1,5");
        Optional<EtherwarpPredictor.Target> target = EtherwarpPredictor.predict(
                new EtherwarpPredictor.Vec3d(0.5, 1.5, 0.5),
                Optional.empty(),
                false,
                new EtherwarpPredictor.Vec3d(0, 0, 1),
                61,
                occupancy(solids));
        assertTrue(target.isPresent());
        assertEquals(EtherwarpPredictor.Validity.VALID, target.get().validity());
        assertEquals(0, target.get().blockX());
        assertEquals(2, target.get().blockY());
        assertEquals(5, target.get().blockZ());
        assertEquals(0, target.get().surfaceBlockX());
        assertEquals(1, target.get().surfaceBlockY());
        assertEquals(5, target.get().surfaceBlockZ());
        assertTrue(EtherwarpPredictor.canWarpTo(target));
        assertFalse(EtherwarpPredictor.canWarpTo(Optional.empty()));
    }

    @Test
    void invalidTargetPrediction() {
        Set<String> solids = Set.of("0,1,5", "0,2,5", "0,3,5");
        Optional<EtherwarpPredictor.Target> target = EtherwarpPredictor.predict(
                new EtherwarpPredictor.Vec3d(0.5, 1.5, 0.5),
                Optional.empty(),
                false,
                new EtherwarpPredictor.Vec3d(0, 0, 1),
                61,
                occupancy(solids));
        assertTrue(target.isPresent());
        assertEquals(EtherwarpPredictor.Validity.INVALID, target.get().validity());
        assertFalse(EtherwarpPredictor.canWarpTo(target));
    }

    @Test
    void failedTargetHiddenWhenDisabled() {
        Optional<EtherwarpPredictor.Target> target = Optional.of(
                new EtherwarpPredictor.Target(1, 2, 3, EtherwarpPredictor.Validity.INVALID, false));
        assertFalse(EtherwarpPredictor.shouldRenderTarget(target, true, false));
    }

    @Test
    void failedTargetRenderedWhenEnabled() {
        Optional<EtherwarpPredictor.Target> target = Optional.of(
                new EtherwarpPredictor.Target(1, 2, 3, EtherwarpPredictor.Validity.INVALID, false));
        assertTrue(EtherwarpPredictor.shouldRenderTarget(target, true, true));
    }

    @Test
    void fullBlockBounds() {
        // Full-block is a render choice; predictor returns integer block coords.
        Optional<EtherwarpPredictor.Target> target = EtherwarpPredictor.predict(
                new EtherwarpPredictor.Vec3d(0.5, 1.5, 0.5),
                Optional.empty(),
                false,
                new EtherwarpPredictor.Vec3d(0, 0, 1),
                10,
                occupancy(Set.of("0,1,4")));
        assertTrue(target.isPresent());
        assertEquals(0, target.get().blockX());
        assertEquals(4, target.get().blockZ());
    }

    @Test
    void highlightIsAThinPlaneOnTopOfTheHitSurface() {
        EtherwarpPredictor.Target target = EtherwarpPredictor.predict(
                new EtherwarpPredictor.Vec3d(0.5, 1.5, 0.5),
                Optional.empty(),
                false,
                new EtherwarpPredictor.Vec3d(0, 0, 1),
                10,
                occupancy(Set.of("0,1,4")))
                .orElseThrow();

        EtherwarpPredictor.HighlightPlane plane =
                EtherwarpPredictor.highlightPlane(target);
        assertEquals(0.03D, plane.minX(), 0.0001D);
        assertEquals(2.002D, plane.minY(), 0.0001D);
        assertEquals(4.03D, plane.minZ(), 0.0001D);
        assertEquals(0.97D, plane.maxX(), 0.0001D);
        assertEquals(2.012D, plane.maxY(), 0.0001D);
        assertEquals(4.97D, plane.maxZ(), 0.0001D);
        EtherwarpPredictor.HighlightPlane cube =
                EtherwarpPredictor.highlightBox(target, true);
        assertEquals(0.0D, cube.minX(), 0.0001D);
        assertEquals(1.0D, cube.maxX(), 0.0001D);
        assertEquals("Filled Outline", EtherwarpPredictor.normalizeRenderStyle("filled_outline"));
        assertTrue(EtherwarpPredictor.drawFilled("Filled"));
        assertFalse(EtherwarpPredictor.drawOutline("Filled"));
    }

    @Test
    void emptyRayDoesNotCreateAnAirBlockTarget() {
        Optional<EtherwarpPredictor.Target> target = EtherwarpPredictor.predict(
                new EtherwarpPredictor.Vec3d(0.5, 1.5, 0.5),
                Optional.empty(),
                false,
                new EtherwarpPredictor.Vec3d(0, 0, 1),
                10,
                occupancy(Set.of()));
        assertTrue(target.isEmpty());
    }

    @Test
    void depthSettingProjection() {
        assertTrue(EtherwarpPredictor.depthRespectsOcclusion(true));
        assertFalse(EtherwarpPredictor.depthRespectsOcclusion(false));
    }

    @Test
    void previewRequiresSneakingWithAnEtherwarpCapableItem() {
        assertTrue(EtherwarpPredictor.shouldPreviewWhileAiming(
                true, "Heroic Aspect of the Void"));
        assertTrue(EtherwarpPredictor.shouldPreviewWhileAiming(
                true, "Aspect of the End"));
        assertTrue(EtherwarpPredictor.shouldPreviewWhileAiming(
                true, "Etherwarp Conduit"));
        assertFalse(EtherwarpPredictor.shouldPreviewWhileAiming(
                false, "Aspect of the Void"));
        assertFalse(EtherwarpPredictor.shouldPreviewWhileAiming(
                true, "Titanium Drill DR-X655"));
    }

    @Test
    void serverPositionUnavailableFailsSafely() {
        Optional<EtherwarpPredictor.Target> target = EtherwarpPredictor.predict(
                new EtherwarpPredictor.Vec3d(0.5, 1.5, 0.5),
                Optional.empty(),
                true,
                new EtherwarpPredictor.Vec3d(0, 0, 1),
                61,
                occupancy(Set.of("0,1,5")));
        assertTrue(target.isPresent());
        assertFalse(target.get().usedServerPosition());
    }

    private static EtherwarpPredictor.BlockOccupancy occupancy(Set<String> solids) {
        Set<String> solidCopy = new HashSet<>(solids);
        return new EtherwarpPredictor.BlockOccupancy() {
            @Override
            public boolean isSolidSurface(int x, int y, int z) {
                return solidCopy.contains(x + "," + y + "," + z);
            }

            @Override
            public boolean isStandSpaceClear(int x, int y, int z) {
                return !solidCopy.contains(x + "," + y + "," + z);
            }
        };
    }
}
