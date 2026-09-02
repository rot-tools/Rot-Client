package fi.rotclient;

import fi.rotclient.mixin.PlayerTabOverlayAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FortuneDetector {
    private static final Pattern MINING = Pattern.compile(
            "(?i)Mining\\s+Fortune\\s*:?\\s*\\+?\\s*([0-9][0-9,.]*)");
    private static final Pattern ORE = Pattern.compile(
            "(?i)Ore\\s+Fortune\\s*:?\\s*\\+?\\s*([0-9][0-9,.]*)");
    private static final Pattern BLOCK = Pattern.compile(
            "(?i)Block\\s+Fortune\\s*:?\\s*\\+?\\s*([0-9][0-9,.]*)");
    private static final Pattern DWARVEN_METAL = Pattern.compile(
            "(?i)Dwarven\\s+Metal\\s+Fortune\\s*:?\\s*\\+?\\s*([0-9][0-9,.]*)");
    private static final Pattern GEMSTONE = Pattern.compile(
            "(?i)Gemstone\\s+Fortune\\s*:?\\s*\\+?\\s*([0-9][0-9,.]*)");
    private final TrackerConfig config;
    private int ticks;

    FortuneDetector(TrackerConfig config) {
        this.config = config;
    }

    void tick(Minecraft client) {
        if (!config.fortuneAuto) return;
        ClientBoundaryGuard.run("FORTUNE_SCAN", () -> tickChecked(client));
    }

    private void tickChecked(Minecraft client) {
        boolean fullScan = ++ticks % 20 == 0;
        double[] best = {-1, -1, -1, -1, -1};

        if (fullScan && client.getConnection() != null) {
            for (PlayerInfo info : client.getConnection().getOnlinePlayers()) {
                Component display = info.getTabListDisplayName();
                if (display != null) inspect(display.getString(), best);
            }

            PlayerTabOverlayAccessor tab = (PlayerTabOverlayAccessor) client.gui.hud.getTabList();
            Component header = tab.rotclient$getHeader();
            Component footer = tab.rotclient$getFooter();
            if (header != null) inspect(header.getString(), best);
            if (footer != null) inspect(footer.getString(), best);
        }

        if (fullScan && client.level != null) {
            Scoreboard scoreboard = client.level.getScoreboard();
            Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
            if (sidebar != null) {
                inspect(sidebar.getDisplayName().getString(), best);
                for (PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
                    Component line = PlayerTeam.formatNameForTeam(
                            scoreboard.getPlayersTeam(entry.owner()), entry.ownerName());
                    inspect(line.getString(), best);
                }
            }
        }

        // The Player Stats menu may show base/category values without the held
        // mining tool and temporary buffs. Only live HUD sources are accepted.
        apply(best, "live Tab widget / scoreboard");
    }

    void inspectMessage(Component message) {
        if (!config.fortuneAuto) return;
        ClientBoundaryGuard.run("FORTUNE_MESSAGE", () -> {
            double[] best = {-1, -1, -1, -1, -1};
            inspect(message.getString(), best);
            apply(best, "game message");
        });
    }

    private void inspect(String text, double[] best) {
        Matcher mining = MINING.matcher(text);
        while (mining.find()) {
            best[0] = Math.max(best[0], parse(mining.group(1)));
        }
        Matcher ore = ORE.matcher(text);
        while (ore.find()) {
            best[1] = Math.max(best[1], parse(ore.group(1)));
        }
        Matcher block = BLOCK.matcher(text);
        while (block.find()) {
            best[2] = Math.max(best[2], parse(block.group(1)));
        }
        Matcher dwarvenMetal = DWARVEN_METAL.matcher(text);
        while (dwarvenMetal.find()) {
            best[3] = Math.max(best[3], parse(dwarvenMetal.group(1)));
        }
        Matcher gemstone = GEMSTONE.matcher(text);
        while (gemstone.find()) {
            best[4] = Math.max(best[4], parse(gemstone.group(1)));
        }
    }

    private void apply(double[] best, String source) {
        boolean changed = false;
        if (best[0] >= 0) {
            config.miningFortune = best[0];
            changed = true;
        }
        if (best[1] >= 0) {
            config.oreFortune = best[1];
            changed = true;
        }
        if (best[2] >= 0) {
            config.blockFortune = best[2];
            changed = true;
        }
        if (best[3] >= 0) {
            config.dwarvenMetalFortune = best[3];
            changed = true;
        }
        if (best[4] >= 0) {
            config.gemstoneFortune = best[4];
            changed = true;
        }
        if (changed) {
            config.fortuneLastDetectedEpochMillis = System.currentTimeMillis();
            config.fortuneSource = source;
            DiagnosticRecorder.record("FORTUNE",
                    "source=" + source + " mining=" + config.miningFortune
                            + " block=" + config.blockFortune
                            + " ore=" + config.oreFortune
                            + " dwarvenMetal=" + config.dwarvenMetalFortune
                            + " gemstone=" + config.gemstoneFortune);
        }
    }

    private static double parse(String number) {
        try {
            return Double.parseDouble(number.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
