package fi.rotclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rot-native dungeon helpers inspired by Athen/Nebulune Dungeons screenshots.
 * Minecraft-free so unit tests can lock delays, walls, order, and kick math.
 */
public final class DungeonAthenPortPolicy {
    public static final List<String> SUPERBOOM_SWAP_TO = List.of("Original slot", "Custom slot");
    public static final List<String> TERM_CLICK_ORDERS = List.of("First", "Random", "Closest", "Furthest");
    public static final List<String> HIGHLIGHT_STYLES = List.of("Outline", "Filled", "Both");
    public static final List<String> DUNGEON_CLASSES =
            List.of("Tank", "Mage", "Archer", "Berserk", "Healer");
    public static final String DEFAULT_QUALITY_STYLE = "&7Item Quality: &c#cur&8/&c#max";

    public static final int MIN_SUPERBOOM_DELAY = 1;
    public static final int MAX_SUPERBOOM_DELAY = 5;
    public static final int MIN_TERM_DELAY_MS = 0;
    public static final int MAX_TERM_DELAY_MS = 500;
    public static final int MIN_HOVER_DELAY_MS = 0;
    public static final int MAX_HOVER_DELAY_MS = 400;
    public static final int MIN_RESYNC_MS = 400;
    public static final int MAX_RESYNC_MS = 1000;
    public static final int DEFAULT_RESYNC_MS = 800;
    public static final int MIN_FIRST_CLICK_MS = 0;
    public static final int MAX_FIRST_CLICK_MS = 800;
    public static final int DEFAULT_FIRST_CLICK_MS = 350;
    public static final int MIN_CHEST_DELAY = 0;
    public static final int MAX_CHEST_DELAY = 5;

    private DungeonAthenPortPolicy() {
    }

    public static int clampSuperboomDelay(int ticks) {
        return Math.max(MIN_SUPERBOOM_DELAY, Math.min(MAX_SUPERBOOM_DELAY, ticks));
    }

    public static int clampTermDelayMs(int ms) {
        return Math.max(MIN_TERM_DELAY_MS, Math.min(MAX_TERM_DELAY_MS, ms));
    }

    public static int clampHoverDelayMs(int ms) {
        return Math.max(MIN_HOVER_DELAY_MS, Math.min(MAX_HOVER_DELAY_MS, ms));
    }

    public static int clampResyncMs(int ms) {
        return Math.max(MIN_RESYNC_MS, Math.min(MAX_RESYNC_MS, ms));
    }

    public static int clampFirstClickMs(int ms) {
        return Math.max(MIN_FIRST_CLICK_MS, Math.min(MAX_FIRST_CLICK_MS, ms));
    }

    public static int clampChestDelay(int ticks) {
        return Math.max(MIN_CHEST_DELAY, Math.min(MAX_CHEST_DELAY, ticks));
    }

    public static int clampSlot(int slot) {
        return Math.max(1, Math.min(9, slot));
    }

    public static int randomBetween(int min, int max) {
        int lo = Math.min(min, max);
        int hi = Math.max(min, max);
        if (lo >= hi) {
            return lo;
        }
        return ThreadLocalRandom.current().nextInt(lo, hi + 1);
    }

    public static String normalizeSwapTo(String value) {
        if (value != null && value.trim().equalsIgnoreCase("Custom slot")) {
            return "Custom slot";
        }
        return "Original slot";
    }

    public static String normalizeTermOrder(String value) {
        if (value == null) {
            return "Closest";
        }
        String text = value.trim();
        for (String option : TERM_CLICK_ORDERS) {
            if (option.equalsIgnoreCase(text)) {
                return option;
            }
        }
        return "Closest";
    }

    public static String normalizeHighlightStyle(String value) {
        if (value != null) {
            for (String option : HIGHLIGHT_STYLES) {
                if (option.equalsIgnoreCase(value.trim())) {
                    return option;
                }
            }
        }
        return "Outline";
    }

    public static String normalizeDungeonClass(String value) {
        if (value != null) {
            for (String option : DUNGEON_CLASSES) {
                if (option.equalsIgnoreCase(value.trim())) {
                    return option;
                }
            }
        }
        return "Tank";
    }

    public static DungeonPolicy.DungeonClass parseClass(String value) {
        return DungeonPolicy.dungeonClass(normalizeDungeonClass(value));
    }

    public static boolean shouldFill(String highlightStyle) {
        String style = normalizeHighlightStyle(highlightStyle);
        return "Filled".equals(style) || "Both".equals(style);
    }

    public static boolean shouldOutline(String highlightStyle) {
        String style = normalizeHighlightStyle(highlightStyle);
        return "Outline".equals(style) || "Both".equals(style);
    }

    public static Set<String> extraBlocks(String csv) {
        Set<String> out = new HashSet<>();
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        for (String token : csv.split("[,\\s]+")) {
            String id = DungeonLeftoverPolicy.path(token);
            if (!id.isBlank()) {
                out.add(id);
            }
        }
        return Set.copyOf(out);
    }

    public static String formatExtraBlocks(Set<String> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return "";
        }
        List<String> sorted = new ArrayList<>(blocks);
        Collections.sort(sorted);
        return String.join(",", sorted);
    }

    public static String addExtraBlock(String csv, String blockId) {
        Set<String> blocks = new HashSet<>(extraBlocks(csv));
        String id = DungeonLeftoverPolicy.path(blockId);
        if (!id.isBlank()) {
            blocks.add(id);
        }
        return formatExtraBlocks(blocks);
    }

    public static String removeExtraBlock(String csv, String blockId) {
        Set<String> blocks = new HashSet<>(extraBlocks(csv));
        blocks.remove(DungeonLeftoverPolicy.path(blockId));
        return formatExtraBlocks(blocks);
    }

    public static boolean isSuperboomWall(String blockId, String extraCsv) {
        return DungeonLeftoverPolicy.isSuperboomWall(blockId)
                || extraBlocks(extraCsv).contains(DungeonLeftoverPolicy.path(blockId));
    }

    public static int superboomTargetSlot(String swapTo, int originalSlot, int customSlot) {
        if ("Custom slot".equals(normalizeSwapTo(swapTo))) {
            return clampSlot(customSlot) - 1;
        }
        return Math.max(0, Math.min(8, originalSlot));
    }

    public static List<DungeonPolicy.TerminalClick> orderClicks(
            List<DungeonPolicy.TerminalClick> clicks,
            int lastSlot,
            String order,
            boolean humanOrderCompat) {
        if (clicks == null || clicks.isEmpty()) {
            return List.of();
        }
        String mode = normalizeTermOrder(order);
        if (humanOrderCompat && "First".equals(mode)) {
            mode = "Closest";
        }
        return switch (mode) {
            case "Random" -> shuffleFirst(clicks);
            case "Furthest" -> preferFurthest(clicks, lastSlot);
            case "Closest" -> DungeonPolicy.preferHumanClick(clicks, lastSlot);
            default -> List.copyOf(clicks);
        };
    }

    private static List<DungeonPolicy.TerminalClick> shuffleFirst(List<DungeonPolicy.TerminalClick> clicks) {
        if (clicks.size() < 2) {
            return List.copyOf(clicks);
        }
        int pick = ThreadLocalRandom.current().nextInt(clicks.size());
        List<DungeonPolicy.TerminalClick> ordered = new ArrayList<>();
        ordered.add(clicks.get(pick));
        for (int i = 0; i < clicks.size(); i++) {
            if (i != pick) {
                ordered.add(clicks.get(i));
            }
        }
        return List.copyOf(ordered);
    }

    private static List<DungeonPolicy.TerminalClick> preferFurthest(
            List<DungeonPolicy.TerminalClick> clicks, int lastSlot) {
        int last = lastSlot < 0 ? 22 : lastSlot;
        DungeonPolicy.TerminalClick best = clicks.getFirst();
        int bestDist = -1;
        for (DungeonPolicy.TerminalClick click : clicks) {
            int dx = (click.slot() % 9) - (last % 9);
            int dy = (click.slot() / 9) - (last / 9);
            int dist = dx * dx + dy * dy;
            if (dist > bestDist) {
                best = click;
                bestDist = dist;
            }
        }
        List<DungeonPolicy.TerminalClick> ordered = new ArrayList<>();
        ordered.add(best);
        for (DungeonPolicy.TerminalClick click : clicks) {
            if (click.slot() != best.slot() || click.button() != best.button()) {
                ordered.add(click);
            }
        }
        return List.copyOf(ordered);
    }

    public static boolean queueNeedsResync(long lastUpdateMs, long nowMs, int timeoutMs) {
        if (lastUpdateMs <= 0L) {
            return false;
        }
        return nowMs - lastUpdateMs >= clampResyncMs(timeoutMs);
    }

    public static boolean firstClickPending(long openedAtMs, long nowMs, int firstClickMs) {
        if (openedAtMs <= 0L) {
            return false;
        }
        return nowMs - openedAtMs < clampFirstClickMs(firstClickMs);
    }

    public static int chestCloseDelayTicks(int min, int max) {
        return randomBetween(clampChestDelay(min), clampChestDelay(max));
    }

    public static boolean shouldInstamineBreaker(
            boolean moduleOn,
            boolean cheatOn,
            boolean holdingBreaker,
            boolean hasFatigue,
            int charges,
            String blockId) {
        if (!moduleOn || !cheatOn || !holdingBreaker || !hasFatigue || charges <= 0) {
            return false;
        }
        return !TempleDungeonPolicy.isSecretBlockId(blockId)
                && !DungeonLeftoverPolicy.isGhostBlacklisted(blockId);
    }

    public static boolean soulsandPlaceTarget(
            boolean inDungeon,
            boolean f7Boss,
            int lookY,
            String blockId,
            String heldItemId) {
        if (!inDungeon || !f7Boss || lookY != 105) {
            return false;
        }
        String block = DungeonLeftoverPolicy.path(blockId);
        if (!block.contains("stone_brick") && !block.equals("stone_bricks")) {
            return false;
        }
        String held = heldItemId == null ? "" : heldItemId.toLowerCase(Locale.ROOT);
        return held.contains("soul_sand")
                || held.equals("chest")
                || held.equals("ender_chest")
                || held.contains("soulsand");
    }

    public static boolean usesPinglessPredict(DungeonPolicy.Terminal terminal) {
        return terminal == DungeonPolicy.Terminal.PANES
                || terminal == DungeonPolicy.Terminal.STARTS_WITH
                || terminal == DungeonPolicy.Terminal.SELECT_ALL
                || terminal == DungeonPolicy.Terminal.COLORS
                || terminal == DungeonPolicy.Terminal.NUMBERS;
    }

    public static List<Integer> keepPredictedSlots(
            List<DungeonPolicy.TerminalClick> live,
            List<Integer> predicted) {
        if (predicted == null || predicted.isEmpty()) {
            return List.of();
        }
        Set<Integer> liveSlots = new HashSet<>();
        if (live != null) {
            for (DungeonPolicy.TerminalClick click : live) {
                liveSlots.add(click.slot());
            }
        }
        List<Integer> kept = new ArrayList<>();
        for (int slot : predicted) {
            if (liveSlots.contains(slot) && !kept.contains(slot)) {
                kept.add(slot);
            }
        }
        return List.copyOf(kept);
    }

    public static List<DungeonPolicy.TerminalClick> withoutPredictedSlots(
            List<DungeonPolicy.TerminalClick> live,
            List<Integer> predicted) {
        if (live == null || live.isEmpty()) {
            return List.of();
        }
        if (predicted == null || predicted.isEmpty()) {
            return List.copyOf(live);
        }
        Set<Integer> skip = new HashSet<>(predicted);
        List<DungeonPolicy.TerminalClick> remaining = new ArrayList<>();
        for (DungeonPolicy.TerminalClick click : live) {
            if (!skip.contains(click.slot())) {
                remaining.add(click);
            }
        }
        return List.copyOf(remaining);
    }

    public static List<Integer> withPredictedSlot(List<Integer> predicted, int slot) {
        List<Integer> next = new ArrayList<>();
        if (predicted != null) {
            for (int existing : predicted) {
                if (!next.contains(existing)) {
                    next.add(existing);
                }
            }
        }
        if (slot >= 0 && !next.contains(slot)) {
            next.add(slot);
        }
        return List.copyOf(next);
    }

    public static Optional<DungeonPolicy.TerminalClick> hoverClick(
            List<DungeonPolicy.TerminalClick> solution,
            int hoveredSlot,
            DungeonPolicy.Terminal terminal) {
        if (solution == null || hoveredSlot < 0 || terminal == DungeonPolicy.Terminal.MELODY
                || terminal == DungeonPolicy.Terminal.NONE) {
            return Optional.empty();
        }
        for (DungeonPolicy.TerminalClick click : solution) {
            if (click.slot() == hoveredSlot) {
                return Optional.of(click);
            }
        }
        return Optional.empty();
    }

    public static String formatQuality(String style, int cur, int max, int floor) {
        if (style == null || style.isBlank()) {
            String color = cur >= max && max > 0 ? "§c§l" : "§6";
            return "§bQuality: " + color + cur + "/" + max + "§r§b, Tier " + floor;
        }
        return applyMcCodes(style)
                .replace("#cur", String.valueOf(cur))
                .replace("#max", String.valueOf(max))
                .replace("#floor", floor <= 0 ? "?" : "F" + floor);
    }

    public static String applyMcCodes(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String text = raw
                .replace("<red>", "§c")
                .replace("<aqua>", "§b")
                .replace("<gray>", "§7")
                .replace("<white>", "§f")
                .replace("<yellow>", "§e")
                .replace("<green>", "§a")
                .replace("<gold>", "§6")
                .replace("<r>", "§r");
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '&' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                if (isMcCode(code)) {
                    out.append('§').append(code);
                    i++;
                    continue;
                }
            }
            out.append(ch);
        }
        return out.toString();
    }

    private static boolean isMcCode(char code) {
        return (code >= '0' && code <= '9')
                || (code >= 'a' && code <= 'f')
                || (code >= 'A' && code <= 'F')
                || "klmnorKLMNOR".indexOf(code) >= 0;
    }

    public static boolean waypointVisible(
            boolean checkClass,
            DungeonPolicy.DungeonClass playerClass,
            DungeonPolicy.DungeonClass assigned) {
        if (!checkClass || playerClass == DungeonPolicy.DungeonClass.UNKNOWN) {
            return true;
        }
        return assigned == DungeonPolicy.DungeonClass.UNKNOWN || assigned == playerClass;
    }

    public static int overlayPad(double padding) {
        if (!Double.isFinite(padding)) {
            return 5;
        }
        return (int) Math.round(Math.max(0.0D, Math.min(20.0D, padding)));
    }

    public static int overlayGap(double gap) {
        if (!Double.isFinite(gap)) {
            return 2;
        }
        return (int) Math.round(Math.max(0.0D, Math.min(12.0D, gap)));
    }
}
