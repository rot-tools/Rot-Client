package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RotClientHeaderLinksPolicyTest {
    @Test
    void githubSitsLeftOfDiscordInsideOmniboxChrome() {
        RotClientHeaderLinksPolicy.Layout layout =
                RotClientHeaderLinksPolicy.layout(40, 20);
        assertTrue(layout.github().x() < layout.discord().x());
        assertEquals(layout.github().y(), layout.discord().y());
        assertEquals(RotClientHeaderLinksPolicy.HIT, layout.github().width());
        assertEquals(RotClientHeaderLinksPolicy.HIT, layout.discord().width());
        int chromeTop = RotClientDashboardLayout.omniboxY(20);
        int chromeBottom = 20 + RotClientDashboardLayout.chromeHeight();
        assertTrue(layout.github().y() >= chromeTop);
        assertTrue(layout.github().y() + layout.github().height() <= chromeBottom);
        assertTrue(layout.discord().x() + layout.discord().width()
                <= 40 + RotClientDashboardLayout.SIDEBAR_WIDTH);
        assertTrue(layout.github().x() > 40);
    }

    @Test
    void hitTestingSeparatesGithubDiscordAndMisses() {
        RotClientHeaderLinksPolicy.Layout layout =
                RotClientHeaderLinksPolicy.layout(10, 0);
        int githubX = layout.github().x() + layout.github().width() / 2;
        int githubY = layout.github().y() + layout.github().height() / 2;
        int discordX = layout.discord().x() + layout.discord().width() / 2;
        int discordY = layout.discord().y() + layout.discord().height() / 2;
        assertEquals(
                RotClientHeaderLinksPolicy.Kind.GITHUB,
                RotClientHeaderLinksPolicy.hit(githubX, githubY, layout));
        assertEquals(
                RotClientHeaderLinksPolicy.Kind.DISCORD,
                RotClientHeaderLinksPolicy.hit(discordX, discordY, layout));
        assertEquals(
                RotClientHeaderLinksPolicy.Kind.NONE,
                RotClientHeaderLinksPolicy.hit(10, layout.github().y(), layout));
        assertEquals(
                RotClientHeaderLinksPolicy.Kind.NONE,
                RotClientHeaderLinksPolicy.hit(githubX, 0, layout));
        assertEquals(
                RotClientHeaderLinksPolicy.Kind.NONE,
                RotClientHeaderLinksPolicy.hit(0, 0, null));
    }

    @Test
    void urlsMatchCanonicalProjectHosts() {
        assertEquals(
                "https://github.com/rot-tools/Rot-Client",
                RotClientHeaderLinksPolicy.url(RotClientHeaderLinksPolicy.Kind.GITHUB));
        assertEquals(
                "https://discord.gg/8UpMfvZugq",
                RotClientHeaderLinksPolicy.url(RotClientHeaderLinksPolicy.Kind.DISCORD));
        assertEquals("", RotClientHeaderLinksPolicy.url(RotClientHeaderLinksPolicy.Kind.NONE));
        assertEquals("GitHub", RotClientHeaderLinksPolicy.tip(RotClientHeaderLinksPolicy.Kind.GITHUB));
        assertEquals("Discord", RotClientHeaderLinksPolicy.tip(RotClientHeaderLinksPolicy.Kind.DISCORD));
    }

    @Test
    void glyphsAreDistinctFilledMarks() {
        String[] github = RotClientHeaderLinksPolicy.githubGlyph();
        String[] discord = RotClientHeaderLinksPolicy.discordGlyph();
        assertEquals(RotClientHeaderLinksPolicy.ICON, github.length);
        assertEquals(RotClientHeaderLinksPolicy.ICON, discord.length);
        assertEquals(RotClientHeaderLinksPolicy.ICON, github[0].length());
        assertEquals(RotClientHeaderLinksPolicy.ICON, discord[0].length());
        int githubCells = RotClientHeaderLinksPolicy.filledCells(github);
        int discordCells = RotClientHeaderLinksPolicy.filledCells(discord);
        assertTrue(githubCells >= 40, "GitHub mark should be a filled silhouette");
        assertTrue(discordCells >= 40, "Discord mark should be a filled silhouette");
        assertNotEquals(githubCells, discordCells);
        assertNotEquals(String.join("", github), String.join("", discord));
    }
}
