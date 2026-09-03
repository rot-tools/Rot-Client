package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

final class PlayerDisplayMathTest {
    @Test
    void independentHudToggles() {
        SkyBlockStatBarParser.Stats stats = parsed(
                "❤ 100/200  ❈ 50  ✎ 80/100  ✦ 120");
        List<PlayerDisplayMath.HudLine> onlyHealth = PlayerDisplayMath.visibleLines(
                stats, true, false, false, false, false, false, false);
        assertEquals(1, onlyHealth.size());
        assertEquals(PlayerDisplayMath.StatKind.HEALTH, onlyHealth.get(0).kind());

        List<PlayerDisplayMath.HudLine> none = PlayerDisplayMath.visibleLines(
                stats, false, false, false, false, false, false, false);
        assertTrue(none.isEmpty());
    }

    @Test
    void healthTextCanBeIconNameAndCurrentOnly() {
        assertEquals("❤ Health 1,234/2,000", PlayerDisplayMath.composeLine(
                PlayerDisplayMath.StatKind.HEALTH, "1,234/2,000", true, true));
        assertEquals("1,234/2,000", PlayerDisplayMath.composeLine(
                PlayerDisplayMath.StatKind.HEALTH, "1,234/2,000", false, false));
        assertEquals("1,234", PlayerDisplayMath.withMax("1,234/2,000", false));
        assertEquals("1,234/2,000", PlayerDisplayMath.withMax("1,234/2,000", true));
        QolUtilityConfig config = new QolUtilityConfig();
        assertEquals(Boolean.TRUE, config.readBoolean("qol.player_display.show_labels"));
        assertEquals(Boolean.TRUE, config.readBoolean("qol.player_display.show_max"));
        config.writeBoolean("qol.player_display.show_labels", false);
        config.writeBoolean("qol.player_display.show_max", false);
        assertEquals(Boolean.FALSE, config.readBoolean("qol.player_display.show_labels"));
        assertEquals(Boolean.FALSE, config.readBoolean("qol.player_display.show_max"));
    }

    @Test
    void healthProjection() {
        SkyBlockStatBarParser.Stats stats = parsed("❤ 1,234/2,000");
        List<PlayerDisplayMath.HudLine> lines = PlayerDisplayMath.visibleLines(
                stats, true, false, false, false, false, false, false);
        assertEquals("1,234/2,000", lines.get(0).value());
        assertTrue(lines.get(0).available());
    }

    @Test
    void classicActionBarPutsIconAfterTheNumbers() {
        SkyBlockStatBarParser.Stats stats = parsed(
                "1,234/2,000❤     500❈     400/500✎     40ʬ     120✦     12♨");
        assertEquals("1,234/2,000", PlayerDisplayMath.visibleLines(
                stats, true, false, false, false, false, false, false).get(0).value());
        assertEquals("400/500", PlayerDisplayMath.visibleLines(
                stats, false, true, false, false, false, false, false).get(0).value());
        assertEquals("500", PlayerDisplayMath.visibleLines(
                stats, false, false, false, true, false, false, false).get(0).value());
        assertEquals("40", PlayerDisplayMath.visibleLines(
                stats, false, false, true, false, false, false, false).get(0).value());
        assertEquals("120", PlayerDisplayMath.visibleLines(
                stats, false, false, false, false, false, false, true).get(0).value());
        assertEquals("12", PlayerDisplayMath.visibleLines(
                stats, false, false, false, false, true, false, false).get(0).value());
    }

    @Test
    void tabListBritishDefenceSpelling() {
        SkyBlockStatBarParser.Stats stats = parsed(
                "Health: 100/200\nDefence: 50\nMana: 80/100\nSpeed: 400");
        assertEquals("100/200", PlayerDisplayMath.visibleLines(
                stats, true, false, false, false, false, false, false).get(0).value());
        assertEquals("50", PlayerDisplayMath.visibleLines(
                stats, false, false, false, true, false, false, false).get(0).value());
        assertEquals("80/100", PlayerDisplayMath.visibleLines(
                stats, false, true, false, false, false, false, false).get(0).value());
        assertEquals("400", PlayerDisplayMath.visibleLines(
                stats, false, false, false, false, false, false, true).get(0).value());
    }

    @Test
    void manaProjection() {
        SkyBlockStatBarParser.Stats stats = parsed("✎ 400/500");
        List<PlayerDisplayMath.HudLine> lines = PlayerDisplayMath.visibleLines(
                stats, false, true, false, false, false, false, false);
        assertEquals("400/500", lines.get(0).value());
    }

    @Test
    void defenseProjection() {
        SkyBlockStatBarParser.Stats stats = parsed("❈ 1,250");
        List<PlayerDisplayMath.HudLine> lines = PlayerDisplayMath.visibleLines(
                stats, false, false, false, true, false, false, false);
        assertEquals("1,250", lines.get(0).value());
    }

    @Test
    void speedProjection() {
        SkyBlockStatBarParser.Stats stats = parsed("✦ 400");
        List<PlayerDisplayMath.HudLine> lines = PlayerDisplayMath.visibleLines(
                stats, false, false, false, false, false, false, true);
        assertEquals("400", lines.get(0).value());
    }

    @Test
    void unavailableStatDoesNotInventValue() {
        SkyBlockStatBarParser.Stats empty = SkyBlockStatBarParser.Stats.empty();
        List<PlayerDisplayMath.HudLine> lines = PlayerDisplayMath.visibleLines(
                empty, true, true, true, true, true, true, true);
        for (PlayerDisplayMath.HudLine line : lines) {
            if (line.kind() == PlayerDisplayMath.StatKind.EHP) {
                assertEquals("--", line.value());
                assertFalse(line.available());
            } else {
                assertEquals("--", line.value());
                assertFalse(line.available());
            }
        }
        assertTrue(PlayerDisplayMath.ehp(
                OptionalDouble.empty(), OptionalDouble.of(100)).isEmpty());
    }

    @Test
    void ehpCalculationWithValidInputs() {
        OptionalDouble ehp = PlayerDisplayMath.ehp(
                OptionalDouble.of(100),
                OptionalDouble.of(50));
        assertTrue(ehp.isPresent());
        assertEquals(150.0D, ehp.getAsDouble(), 0.0001D);
    }

    @Test
    void playerDisplayConfigRoundTrip() {
        QolUtilityConfig config = new QolUtilityConfig();
        assertTrue(config.playerDisplayHealthHud);
        assertTrue(config.toggleBooleanSetting("qol.player_display.health_hud"));
        assertFalse(config.playerDisplayHealthHud);
        config.setPose("health", 40.0F, 80.0F);
        float[] pose = config.pose("health");
        assertEquals(40.0F, pose[0], 0.01F);
        assertEquals(80.0F, pose[1], 0.01F);
        assertTrue(config.toggleBooleanSetting(
                "qol.player_display.hide_action_mana"));
        assertTrue(config.playerDisplayHideActionMana);
    }

    @Test
    void actionBarFilterPreservesUnrelatedText() {
        OptionalDouble unused = OptionalDouble.empty();
        var filtered = SkyBlockStatBarParser.filterActionBar(
                "❤ 100/200   Quest complete!   ✎ 50/100",
                true,
                false,
                true,
                false,
                false,
                false);
        assertTrue(filtered.isPresent());
        assertTrue(filtered.get().contains("Quest complete!"));
        assertFalse(filtered.get().contains("❤"));
        assertFalse(filtered.get().contains("✎"));
        Optional<String> classic = SkyBlockStatBarParser.filterActionBar(
                "100/200❤   Quest complete!   50/100✎",
                true,
                false,
                true,
                false,
                false,
                false);
        assertTrue(classic.isPresent());
        assertTrue(classic.get().contains("Quest complete!"));
        assertFalse(classic.get().contains("❤"));
        assertFalse(classic.get().contains("✎"));
        Optional<String> colored = SkyBlockStatBarParser.filterActionBar(
                "§c100/200❤   Quest complete!   §b50/100✎",
                true,
                false,
                true,
                false,
                false,
                false);
        assertTrue(colored.isPresent());
        assertTrue(colored.get().contains("Quest complete!"));
        assertFalse(colored.get().contains("❤"));
        assertFalse(colored.get().contains("✎"));
        Optional<String> leftover = SkyBlockStatBarParser.filterActionBar(
                "5,224/4,849  998  2,782/2,782  115/115",
                true,
                true,
                true,
                false,
                false,
                false);
        assertTrue(leftover.isEmpty());
        Optional<String> combatTick = SkyBlockStatBarParser.filterActionBar(
                "3,898/3,898 +408.2 Combat (19,896,487/0) 1,817/1,869 115/115",
                true,
                true,
                true,
                false,
                false,
                false);
        assertTrue(combatTick.isEmpty());
        assertTrue(unused.isEmpty());
    }

    @Test
    void actionBarFilterStripsZoneNameBetweenStats() {
        Optional<String> palace = SkyBlockStatBarParser.filterActionBar(
                "5,224/4,849 Royal Palace 2,782/2,782 115/115",
                false,
                false,
                false,
                false,
                false,
                false,
                true);
        assertTrue(palace.isPresent());
        assertEquals("5,224/4,849 2,782/2,782 115/115", palace.get());
        Optional<String> bridge = SkyBlockStatBarParser.filterActionBar(
                "5,224/4,849 Palace Bridge 2,782/2,782 115/115",
                false,
                false,
                false,
                false,
                false,
                false,
                true);
        assertTrue(bridge.isPresent());
        assertEquals("5,224/4,849 2,782/2,782 115/115", bridge.get());
        assertTrue(SkyBlockStatBarParser.filterActionBar(
                "Royal Palace",
                false,
                false,
                false,
                false,
                false,
                false,
                true).isEmpty());
        Optional<String> quest = SkyBlockStatBarParser.filterActionBar(
                "❤ 100/200   Quest complete!   ✎ 50/100",
                true,
                false,
                true,
                false,
                false,
                false,
                true);
        assertTrue(quest.isPresent());
        assertTrue(quest.get().contains("Quest complete!"));
    }

    @Test
    void hideActionLocationDefaultsOn() {
        QolUtilityConfig config = new QolUtilityConfig();
        assertTrue(config.playerDisplayHideActionLocation);
        assertEquals(Boolean.TRUE, config.readBoolean(
                "qol.player_display.hide_action_location"));
        config.writeBoolean("qol.player_display.hide_action_location", false);
        assertFalse(config.playerDisplayHideActionLocation);
    }

    @Test
    void vanillaHeartPairIsNotSkyBlockHealth() {
        SkyBlockStatBarParser.Stats fromHearts = parsed("15/15");
        assertTrue(fromHearts.health().isEmpty());
        assertTrue(fromHearts.maxHealth().isEmpty());

        SkyBlockStatBarParser.Stats manaAndVanilla = parsed("✎ 73/100\n15/15");
        assertEquals(73.0D, manaAndVanilla.mana().orElse(-1), 0.001D);
        assertEquals(100.0D, manaAndVanilla.maxMana().orElse(-1), 0.001D);
        assertTrue(manaAndVanilla.health().isEmpty());
    }

    @Test
    void labeledLowHealthStillParses() {
        SkyBlockStatBarParser.Stats stats = parsed("❤ 15/15");
        assertEquals(15.0D, stats.health().orElse(-1), 0.001D);
        assertEquals(15.0D, stats.maxHealth().orElse(-1), 0.001D);
    }

    @Test
    void privateUseIconsStillParseHealthAndManaPairs() {
        SkyBlockStatBarParser.Stats stats = parsed(
                "\uE000 1,234/2,000     \uE001 400/500");
        assertEquals("1,234/2,000", PlayerDisplayMath.visibleLines(
                stats, true, false, false, false, false, false, false).get(0).value());
        assertEquals("400/500", PlayerDisplayMath.visibleLines(
                stats, false, true, false, false, false, false, false).get(0).value());
    }

    @Test
    void strippedActionBarKeepsDefenseBetweenHealthAndMana() {
        SkyBlockStatBarParser.Stats stats = parsed(
                "8,123/23,456     500     2,782/2,782     40     120");
        assertEquals(8123.0D, stats.health().orElse(-1), 0.001D);
        assertEquals(23456.0D, stats.maxHealth().orElse(-1), 0.001D);
        assertEquals(500.0D, stats.defense().orElse(-1), 0.001D);
        assertEquals(2782.0D, stats.mana().orElse(-1), 0.001D);
        assertEquals(2782.0D, stats.maxMana().orElse(-1), 0.001D);
        assertEquals(40.0D, stats.overflowMana().orElse(-1), 0.001D);
        assertEquals(120.0D, stats.speed().orElse(-1), 0.001D);
        assertEquals("48,738", PlayerDisplayMath.visibleLines(
                stats, false, false, false, false, false, true, false).get(0).value());
    }

    @Test
    void tabPlayerCountDoesNotBecomeHealth() {
        SkyBlockStatBarParser.Stats stats = SkyBlockStatBarParser.parseHudSources(
                "Players 8/23\nMana: 2,782/2,782\nSpeed: 400");
        assertTrue(stats.health().isEmpty());
        assertEquals(2782.0D, stats.mana().orElse(-1), 0.001D);
        assertEquals(400.0D, stats.speed().orElse(-1), 0.001D);
    }

    @Test
    void lobbyHeartPair23IsNotSkyBlockHealth() {
        SkyBlockStatBarParser.Stats stats = parsed("8/23");
        assertTrue(stats.health().isEmpty());
        assertTrue(stats.maxHealth().isEmpty());
    }

    private static SkyBlockStatBarParser.Stats parsed(String raw) {
        return SkyBlockStatBarParser.parse(raw);
    }
}
