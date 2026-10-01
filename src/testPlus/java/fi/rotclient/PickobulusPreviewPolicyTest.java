package fi.rotclient;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PickobulusPreviewPolicyTest {
    @Test void boundsAndGeometryStayExplicit() {
        assertTrue(PickobulusPreviewPolicy.inside(3, 3, 3, 3, false));
        assertFalse(PickobulusPreviewPolicy.inside(3, 3, 3, 3, true));
        assertTrue(PickobulusPreviewPolicy.inside(0, 0, 3, 3, true));
        assertFalse(PickobulusPreviewPolicy.inside(6, 0, 0, 100, false));
    }
}
