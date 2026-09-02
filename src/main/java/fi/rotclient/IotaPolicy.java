package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Local chat, alert and utility decisions: party commands, limbo/kick
 * alerts, party-join sound, and arrow tracking. Minecraft send/receive
 * lives in the client runtime.
 */
public final class IotaPolicy {
    public static final String LIMBO_MESSAGE =
            "A kick occurred in your connection, so you were put in the SkyBlock lobby!";
    public static final String LIMBO_PARTY =
            "[Rot] Kicked, will be back in 60s probably.";
    public static final String ARROW_EMPTY_PARTY =
            "[Rot] Oh! I ran out of arrows mid run, unlucky :(";
    public static final String ARROW_EMPTY_TITLE = "§cYou run out of arrows";
    public static final String ARROW_LOW_TITLE = "§eYou are running out of arrows";
    public static final String VISIBILITY_ALWAYS = "Always";
    public static final String VISIBILITY_ONLY_SHOOTING = "Only Shooting";
    public static final List<String> ARROW_VISIBILITY_OPTIONS =
            List.of(VISIBILITY_ALWAYS, VISIBILITY_ONLY_SHOOTING);
    public static final float TOGGLE_CLICK_CPS = 5.0F;

    public enum PartyActionKind {
        COMMAND,
        PARTY_CHAT
    }

    public record PartyAction(PartyActionKind kind, String payload) {
        public static PartyAction command(String command) {
            return new PartyAction(PartyActionKind.COMMAND, command);
        }

        public static PartyAction partyChat(String text) {
            return new PartyAction(PartyActionKind.PARTY_CHAT, text);
        }
    }

    public record PartyCommandConfig(
            boolean enable,
            boolean warp,
            boolean transfer,
            boolean ping,
            boolean allInvite,
            boolean tps,
            boolean promote,
            boolean kick,
            boolean kuudra,
            boolean chests,
            boolean runs,
            boolean profit) {
    }

    public record ArrowSnapshot(
            String type,
            int count,
            boolean outOfArrows,
            boolean tracked) {
        public static ArrowSnapshot empty() {
            return new ArrowSnapshot("Unknown", 0, false, false);
        }
    }

    public record ArrowNotice(boolean outOfArrows, int count, String type) {
    }

    private static final Pattern CONTROL = Pattern.compile("§.");
    private static final Pattern PARTY_JOIN = Pattern.compile("^\\w+ joined the party[.!]?$");
    private static final Pattern TERMINATOR_COOLDOWN =
            Pattern.compile("^This ability is on cooldown for ");
    private static final Map<Integer, String> KUUDRA_TIERS = Map.of(
            1, "KUUDRA_NORMAL",
            2, "KUUDRA_HOT",
            3, "KUUDRA_BURNING",
            4, "KUUDRA_FIERY",
            5, "KUUDRA_INFERNAL");

    private IotaPolicy() {
    }

    public static String strip(String raw) {
        if (raw == null) {
            return "";
        }
        return CONTROL.matcher(raw).replaceAll("").trim();
    }

    public static boolean shouldActivate(boolean enabled, boolean active) {
        return enabled && !active;
    }

    public static boolean shouldDeactivate(boolean enabled, boolean active) {
        return !enabled && active;
    }

    public static boolean isLimboKick(String raw) {
        return strip(raw).contains(LIMBO_MESSAGE);
    }

    public static boolean isPartyJoin(String raw) {
        String text = strip(raw);
        if (text.contains("joined the party")) {
            return true;
        }
        return PARTY_JOIN.matcher(text).matches();
    }

    public static boolean isTerminatorCooldownChat(String raw) {
        return TERMINATOR_COOLDOWN.matcher(strip(raw)).find();
    }

    public static boolean shouldMuteTerminator(
            boolean moduleEnabled,
            boolean muteEnabled,
            boolean inSkyblock,
            boolean holdingTerminator) {
        return moduleEnabled && muteEnabled && inSkyblock && holdingTerminator;
    }

    public static boolean isTerminatorId(String skyBlockId) {
        if (skyBlockId == null || skyBlockId.isBlank()) {
            return false;
        }
        String id = skyBlockId.trim();
        return "TERMINATOR".equalsIgnoreCase(id)
                || "skyblock:TERMINATOR".equalsIgnoreCase(id);
    }

    public static boolean shouldMuteFishingCast(boolean moduleEnabled, boolean muteEnabled) {
        return moduleEnabled && muteEnabled;
    }

    public static boolean shouldFixFishingHook(boolean moduleEnabled, boolean fixEnabled) {
        return moduleEnabled && fixEnabled;
    }

    /**
     * Fishing-hook fix: ignore the armor stand whose entity id is hookId+1.
     */
    public static boolean shouldIgnoreHookOwner(
            boolean fixEnabled,
            int hookId,
            boolean ownerIsArmorStand,
            int ownerId) {
        return fixEnabled && ownerIsArmorStand && ownerId == hookId + 1;
    }

    public static Optional<PartyAction> partyCommand(
            String raw,
            boolean moduleEnabled,
            PartyCommandConfig config,
            String localName,
            int pingMs,
            float tps,
            int chests,
            int runs,
            int failedRuns,
            double averageRunSeconds,
            long profitCoins,
            long hourlyRateCoins) {
        if (!moduleEnabled || config == null || !config.enable()) {
            return Optional.empty();
        }
        String[] party = parsePartyChat(strip(raw));
        if (party == null) {
            return Optional.empty();
        }
        String sender = party[0];
        String body = party[1];
        if (!body.startsWith("!")) {
            return Optional.empty();
        }
        String[] parts = body.split("\\s+");
        String command = parts[0].toLowerCase(Locale.ROOT);
        return switch (command) {
            case "!warp", "!w", "!wp" -> config.warp()
                    ? Optional.of(PartyAction.command("party warp"))
                    : Optional.empty();
            case "!pt", "!ptme", "!transfer" -> config.transfer()
                    ? Optional.of(PartyAction.command("party transfer " + sender))
                    : Optional.empty();
            case "!ping" -> config.ping()
                    ? Optional.of(PartyAction.partyChat(
                            String.format(Locale.ROOT, "[Rot] %,dms", Math.max(0, pingMs))))
                    : Optional.empty();
            case "!allinvite", "!allinv", "!invites" -> config.allInvite()
                    ? Optional.of(PartyAction.command("party settings allinvite"))
                    : Optional.empty();
            case "!tps" -> config.tps()
                    ? Optional.of(PartyAction.partyChat(
                            String.format(Locale.ROOT, "[Rot] %.1f", tps)))
                    : Optional.empty();
            case "!promote" -> config.promote()
                    ? Optional.of(PartyAction.command("party promote " + sender))
                    : Optional.empty();
            case "!kick" -> kick(config, parts);
            case "!t1", "!t2", "!t3", "!t4", "!t5" -> kuudraTier(config, command);
            case "!chests" -> config.chests()
                    ? Optional.of(PartyAction.partyChat(
                            "[Rot] I am currently at " + Math.max(0, chests) + "/60 of my chest limit."))
                    : Optional.empty();
            case "!runs" -> runs(config, parts, localName, runs, failedRuns, averageRunSeconds);
            case "!profit" -> config.profit()
                    ? Optional.of(PartyAction.partyChat(formatProfitLine(
                            profitCoins, hourlyRateCoins, runs, averageRunSeconds)))
                    : Optional.empty();
            default -> Optional.empty();
        };
    }

    public static Optional<Integer> parseArrowCountFromLore(List<String> lore) {
        if (lore == null) {
            return Optional.empty();
        }
        for (String raw : lore) {
            String line = strip(raw);
            if (!line.contains("Arrows Remaining:")) {
                continue;
            }
            String[] parts = line.split(":");
            if (parts.length < 2) {
                continue;
            }
            String digits = parts[1].replaceAll("[^0-9]", "").trim();
            if (digits.isEmpty()) {
                continue;
            }
            try {
                return Optional.of(Integer.parseInt(digits));
            } catch (NumberFormatException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static boolean isQuiverSlot(String itemDescriptionId, String hoverName) {
        if (itemDescriptionId != null && itemDescriptionId.toLowerCase(Locale.ROOT).contains("feather")) {
            return hoverName != null && !strip(hoverName).isEmpty();
        }
        return false;
    }

    public static Optional<ArrowNotice> arrowChatNotice(String raw) {
        String text = strip(raw);
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("your quiver is now completely empty")) {
            return Optional.of(new ArrowNotice(true, 0, ""));
        }
        if (text.startsWith("QUIVER! You only have")) {
            String[] parts = text.split(" ");
            if (parts.length >= 6) {
                try {
                    int count = Integer.parseInt(parts[4]);
                    String type = String.join(" ", java.util.Arrays.copyOfRange(parts, 5, parts.length));
                    return Optional.of(new ArrowNotice(false, count, type));
                } catch (NumberFormatException ignored) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    public static boolean showArrowHud(
            boolean moduleEnabled,
            boolean trackerEnabled,
            boolean hasTrackedData,
            boolean holdingBow,
            String visibility) {
        if (!moduleEnabled || !trackerEnabled || !hasTrackedData) {
            return false;
        }
        if (VISIBILITY_ONLY_SHOOTING.equals(normalizeVisibility(visibility))) {
            return holdingBow;
        }
        return true;
    }

    public static List<String> arrowHudLines(ArrowSnapshot snapshot) {
        List<String> lines = new ArrayList<>();
        if (snapshot == null || !snapshot.tracked()) {
            return List.of();
        }
        if (snapshot.outOfArrows() || snapshot.count() <= 0) {
            lines.add("§cOut of arrows");
        } else {
            lines.add("§e" + snapshot.type() + " §f" + snapshot.count());
        }
        return List.copyOf(lines);
    }

    public static String normalizeVisibility(String value) {
        if (value == null) {
            return VISIBILITY_ALWAYS;
        }
        String trimmed = value.trim();
        if (trimmed.equalsIgnoreCase(VISIBILITY_ONLY_SHOOTING)
                || trimmed.equalsIgnoreCase("ONLY_SHOOTING")) {
            return VISIBILITY_ONLY_SHOOTING;
        }
        return VISIBILITY_ALWAYS;
    }

    public static boolean isKuudraDefeatChat(String raw) {
        String text = strip(raw).toLowerCase(Locale.ROOT);
        return text.contains("kuudra down")
                || text.contains("kuudra has been defeated");
    }

    public static String formatCoins(long coins) {
        long abs = Math.abs(coins);
        String sign = coins < 0L ? "-" : "";
        if (abs >= 1_000_000_000L) {
            return String.format(Locale.ROOT, "%s%.2fb", sign, abs / 1.0E9);
        }
        if (abs >= 1_000_000L) {
            return String.format(Locale.ROOT, "%s%.2fm", sign, abs / 1_000_000.0);
        }
        if (abs >= 1_000L) {
            return String.format(Locale.ROOT, "%s%.1fk", sign, abs / 1000.0);
        }
        return sign + abs;
    }

    private static Optional<PartyAction> kick(PartyCommandConfig config, String[] parts) {
        if (!config.kick() || parts.length < 2) {
            return Optional.empty();
        }
        return Optional.of(PartyAction.command("party kick " + parts[1]));
    }

    private static Optional<PartyAction> kuudraTier(PartyCommandConfig config, String command) {
        if (!config.kuudra()) {
            return Optional.empty();
        }
        int tier = Integer.parseInt(command.substring(2));
        String instance = KUUDRA_TIERS.get(tier);
        if (instance == null) {
            return Optional.empty();
        }
        return Optional.of(PartyAction.command("joininstance " + instance));
    }

    private static Optional<PartyAction> runs(
            PartyCommandConfig config,
            String[] parts,
            String localName,
            int runs,
            int failedRuns,
            double averageRunSeconds) {
        if (!config.runs()) {
            return Optional.empty();
        }
        if (parts.length > 1
                && localName != null
                && !parts[1].equalsIgnoreCase(localName)) {
            return Optional.empty();
        }
        return Optional.of(PartyAction.partyChat(String.format(
                Locale.ROOT,
                "[Rot] Runs: %d (F:%d) | Avg: %.2fs",
                Math.max(0, runs),
                Math.max(0, failedRuns),
                Math.max(0.0D, averageRunSeconds))));
    }

    private static String[] parsePartyChat(String text) {
        if (text == null || !text.startsWith("Party >")) {
            return null;
        }
        String rest = text.substring("Party >".length()).trim();
        if (rest.startsWith("[")) {
            int close = rest.indexOf(']');
            if (close < 0) {
                return null;
            }
            rest = rest.substring(close + 1).trim();
        }
        int colon = rest.indexOf(':');
        if (colon <= 0 || colon >= rest.length() - 1) {
            return null;
        }
        String sender = rest.substring(0, colon).trim();
        String body = rest.substring(colon + 1).trim();
        if (sender.isEmpty() || body.isEmpty()) {
            return null;
        }
        for (int i = 0; i < sender.length(); i++) {
            char c = sender.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_')) {
                return null;
            }
        }
        return new String[] {sender, body};
    }

    private static String formatProfitLine(
            long profit,
            long hourly,
            int runs,
            double averageRunSeconds) {
        return String.format(
                Locale.ROOT,
                "[Rot] Profit: %s | Rate: %s/h | Runs: %d | Avg: %.2fs",
                formatCoins(profit),
                formatCoins(Math.max(0L, hourly)),
                Math.max(0, runs),
                Math.max(0.0D, averageRunSeconds));
    }
}
