package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class CustomScoreboardSettingsCacheTest {
    @Test
    void optionsAreReusedUntilASettingChanges() {
        CustomScoreboardSettings settings =
                new CustomScoreboardSettings();

        CustomScoreboardPolicy.Options first =
                settings.options();

        CustomScoreboardPolicy.Options second =
                settings.options();

        assertSame(first, second);

        assertTrue(settings.writeBoolean(
                "qol.custom_scoreboard.show_diff",
                true));

        CustomScoreboardPolicy.Options third =
                settings.options();

        assertNotSame(first, third);
        assertTrue(third.showDiff());
    }

    @Test
    void textMutationInvalidatesParsedLists() {
        CustomScoreboardSettings settings =
                new CustomScoreboardSettings();

        CustomScoreboardPolicy.Options first =
                settings.options();

        assertTrue(settings.writeText(
                "qol.custom_scoreboard.appearance",
                "Title\nPurse"));

        CustomScoreboardPolicy.Options second =
                settings.options();

        assertNotSame(first, second);
        assertFalse(second.appearance().isEmpty());
    }

    @Test
    void directFieldMutationIsDetectedAutomatically() {
        CustomScoreboardSettings settings =
                new CustomScoreboardSettings();

        CustomScoreboardPolicy.Options first =
                settings.options();

        settings.alignH = "Left";

        CustomScoreboardPolicy.Options second =
                settings.options();

        assertNotSame(first, second);
        assertSame(
                CustomScoreboardPolicy.Align.LEFT,
                second.alignH());
    }

    @Test
    void directBooleanMutationIsDetectedAutomatically() {
        CustomScoreboardSettings settings =
                new CustomScoreboardSettings();

        CustomScoreboardPolicy.Options first =
                settings.options();

        settings.hideEmpty = !settings.hideEmpty;

        CustomScoreboardPolicy.Options second =
                settings.options();

        assertNotSame(first, second);
    }
}