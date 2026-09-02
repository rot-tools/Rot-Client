package fi.rotclient;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * In-memory TREE GIFT HUD state. Not a durable Current Session ledger.
 */
public final class ForagingGiftTracker {
    private static final Pattern REWARDS = Pattern.compile(
            "\\+\\s*([\\d,]+)\\s+rewards gained", Pattern.CASE_INSENSITIVE);
    private static final Pattern BONUS = Pattern.compile("BONUS GIFT", Pattern.CASE_INSENSITIVE);

    private boolean inGift;
    private ForagingPolicy.TreeType lastType = ForagingPolicy.TreeType.UNKNOWN;
    private double lastPercent;
    private int lastRewards;
    private int giftsThisSession;
    private boolean lastHadBonus;

    public void resetSession() {
        inGift = false;
        lastType = ForagingPolicy.TreeType.UNKNOWN;
        lastPercent = 0.0D;
        lastRewards = 0;
        giftsThisSession = 0;
        lastHadBonus = false;
    }

    public boolean ingest(String line) {
        if (line == null) {
            return false;
        }
        if (ForagingPolicy.isTreeGiftHeader(line)) {
            inGift = true;
            lastHadBonus = false;
            lastRewards = 0;
            lastPercent = 0.0D;
            lastType = ForagingPolicy.TreeType.UNKNOWN;
            return true;
        }
        if (!inGift) {
            return false;
        }
        ForagingPolicy.GiftContribution gift = ForagingPolicy.parseGiftContribution(line);
        if (gift != null) {
            lastType = gift.type();
            lastPercent = gift.percent();
            return true;
        }
        Matcher rewards = REWARDS.matcher(ForagingPolicy.strip(line));
        if (rewards.find()) {
            lastRewards = Integer.parseInt(rewards.group(1).replace(",", ""));
            giftsThisSession++;
            inGift = false;
            return true;
        }
        if (BONUS.matcher(ForagingPolicy.strip(line)).find()) {
            lastHadBonus = true;
            return true;
        }
        String stripped = ForagingPolicy.strip(line);
        if (stripped.chars().filter(ch -> ch == '▬').count() >= 20) {
            inGift = false;
            return true;
        }
        return false;
    }

    public String hudLine() {
        if (giftsThisSession <= 0 && lastType == ForagingPolicy.TreeType.UNKNOWN) {
            return "";
        }
        String bonus = lastHadBonus ? " bonus" : "";
        return lastType.name()
                + " "
                + formatPercent(lastPercent)
                + "% +"
                + lastRewards
                + bonus
                + " ("
                + giftsThisSession
                + ")";
    }

    public boolean inGift() {
        return inGift;
    }

    public int giftsThisSession() {
        return giftsThisSession;
    }

    public ForagingPolicy.TreeType lastType() {
        return lastType;
    }

    public int lastRewards() {
        return lastRewards;
    }

    private static String formatPercent(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.05D) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
