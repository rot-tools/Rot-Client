package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProfileScopedStateTest {
    @Test
    void detectsAndCanonicalizesProfileWidgetWithoutFormatting() {
        assertEquals(
                "sweet_apple",
                SkyBlockProfileIdentity.detect(List.of(
                        "Area: Dwarven Mines",
                        "§bProfile: §aSweet Apple"))
                        .orElseThrow());
        assertTrue(SkyBlockProfileIdentity.detect(
                List.of("Profile Viewer: Alice")).isEmpty());
    }

    @Test
    void keepsKnownProfilesIndependentAndUnknownTransient() {
        ProfileScopedState<List<String>> state =
                new ProfileScopedState<>(ArrayList::new);
        List<String> apple = state.forProfile("Apple");
        List<String> appleAgain = state.forProfile(" apple ");
        List<String> banana = state.forProfile("Banana");
        List<String> unknown = state.forProfile(null);

        apple.add("run");
        unknown.add("temporary");
        assertSame(apple, appleAgain);
        assertNotSame(apple, banana);
        assertEquals(List.of("run"), state.snapshotKnownProfiles().get("apple"));
        assertEquals(2, state.snapshotKnownProfiles().size());

        state.clearUnknown();
        assertNotSame(unknown, state.forProfile(""));
        assertEquals(List.of("run"), state.snapshotKnownProfiles().get("apple"));
    }
}
