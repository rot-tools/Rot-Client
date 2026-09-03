package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

final class AppearanceNavPolicyTest {
    @Test
    void appearanceSidebarOmitsMiningHud() {
        assertEquals(6, AppearanceNavPolicy.sidebarLabels().size());
        assertFalse(AppearanceNavPolicy.includesMiningHud());
        assertFalse(AppearanceNavPolicy.sidebarLabels().stream().anyMatch(
                label -> label.toLowerCase().contains("mining hud")));
        assertEquals("Colors", AppearanceNavPolicy.sidebarLabels().get(2));
    }
}
