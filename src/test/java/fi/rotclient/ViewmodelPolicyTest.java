package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ViewmodelPolicyTest {
    @Test
    void everyPoseAndSwingSliderUpdatesTheConfigurationReadByTheRenderer() {
        QolUtilityConfig config = new QolUtilityConfig();
        config.setModuleEnabled("qol.viewmodel", true);
        assertEquals(true, config.extras().viewmodelEnabled);
        for (String axis : java.util.List.of("x", "y", "z")) {
            for (String family : java.util.List.of("offset", "scale", "rot", "swing")) {
                String id = "qol.viewmodel." + family + "_" + axis;
                double next = family.equals("rot") ? 90 : 0.5;
                assertEquals(true, config.writeNumber(id, next), id);
                assertEquals(next, config.readNumber(id), id);
            }
        }
        assertEquals(0.5, config.extras().viewmodelOffsetX);
        assertEquals(0.5, config.extras().viewmodelScaleY);
        assertEquals(90.0, config.extras().viewmodelRotZ);
        assertEquals(0.5, config.extras().viewmodelSwingX);
    }

    @Test
    void finiteValuesAreClampedToTheirRange() {
        assertEquals(2.0D, ViewmodelPolicy.clampOffset(9.0D));
        assertEquals(-2.0D, ViewmodelPolicy.clampOffset(-9.0D));
        assertEquals(5.0D, ViewmodelPolicy.clampScale(50.0D));
        assertEquals(0.0D, ViewmodelPolicy.clampScale(-3.0D));
        assertEquals(180.0D, ViewmodelPolicy.clampRotation(400.0D));
        assertEquals(2.0D, ViewmodelPolicy.clampSwing(7.0D));
    }

    @Test
    void nonFiniteValuesFallBackToTheNeutralPoseNotTheMinimum() {
        // A NaN scale used to become 0 and make the held item vanish.
        assertEquals(1.0D, ViewmodelPolicy.clampScale(Double.NaN));
        assertEquals(1.0D, ViewmodelPolicy.clampScale(Double.POSITIVE_INFINITY));
        assertEquals(1.0D, ViewmodelPolicy.clampSwing(Double.NaN));
        assertEquals(0.0D, ViewmodelPolicy.clampOffset(Double.NaN));
        assertEquals(0.0D, ViewmodelPolicy.clampRotation(Double.NaN));
    }
}
