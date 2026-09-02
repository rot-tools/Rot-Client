package fi.rotclient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Experimentation table solver state machine.
 *
 * <p>The class is deliberately Minecraft-free: the runtime feeds it one
 * {@link SlotUpdate} per server slot packet and asks back which slots to tint and
 * which clicks to swallow.
 */
public final class ExperimentSolverPolicy {
    public enum Experiment {
        CHRONOMATRON,
        ULTRASEQUENCER,
        SUPERPAIRS,
        NONE
    }

    /** Which highlight a slot should get; the runtime maps these to configured colors. */
    public enum Highlight {
        FIRST,
        SECOND,
        MATCHED,
        MATCH,
        POWERUP
    }

    /**
     * A single slot as pushed by the server.
     *
     * @param slotIndex container slot index
     * @param itemPath  registry path, e.g. {@code red_terracotta}
     * @param name      plain (formatting-stripped) hover name
     * @param count     stack size, used as the Ultrasequencer ordering key
     * @param lore      plain lore lines
     * @param textureId stable identity for player-head textures, or empty
     */
    public record SlotUpdate(
            int slotIndex,
            String itemPath,
            String name,
            int count,
            List<String> lore,
            String textureId) {
    }

    private static final String CLOCK = "clock";
    private static final String GLOWSTONE = "glowstone";
    private static final String BOOKSHELF = "bookshelf";
    private static final String CAULDRON = "cauldron";
    private static final Set<String> DYES = Set.of(
            "white_dye", "orange_dye", "magenta_dye", "light_blue_dye", "yellow_dye",
            "lime_dye", "pink_dye", "gray_dye", "light_gray_dye", "cyan_dye",
            "purple_dye", "blue_dye", "brown_dye", "green_dye", "red_dye", "black_dye",
            "ink_sac", "bone_meal", "lapis_lazuli", "cocoa_beans");

    private final List<List<Integer>> chronoSolution = new ArrayList<>();
    private final List<Ultra> ultraSolution = new ArrayList<>();
    private final Map<Integer, SlotUpdate> superRevealed = new LinkedHashMap<>();
    private final Map<Integer, Highlight> superHighlights = new LinkedHashMap<>();
    private int superLastSlot = -1;
    private boolean rememberPhase = true;

    private record Ultra(int slotIndex, int count) {
    }

    public static Experiment experimentFor(String title, boolean onPrivateIsland) {
        if (title == null || !onPrivateIsland) {
            return Experiment.NONE;
        }
        if (title.startsWith("Chronomatron (")) {
            return Experiment.CHRONOMATRON;
        }
        if (title.startsWith("Ultrasequencer (")) {
            return Experiment.ULTRASEQUENCER;
        }
        if (title.startsWith("Superpairs (")) {
            return Experiment.SUPERPAIRS;
        }
        return Experiment.NONE;
    }

    public static boolean isStatus(SlotUpdate slot) {
        String path = path(slot);
        return CLOCK.equals(path)
                || BOOKSHELF.equals(path)
                || (GLOWSTONE.equals(path) && !"Enchanted Book".equals(slot.name()))
                || CAULDRON.equals(path);
    }

    public static boolean isDye(SlotUpdate slot) {
        return DYES.contains(path(slot));
    }

    public static boolean isTerracotta(SlotUpdate slot) {
        return path(slot).endsWith("terracotta");
    }

    public static boolean isStainedGlass(SlotUpdate slot) {
        return path(slot).endsWith("stained_glass");
    }

    public static boolean isStainedGlassPane(SlotUpdate slot) {
        return path(slot).endsWith("stained_glass_pane");
    }

    public static boolean isPowerup(SlotUpdate slot) {
        if (slot.lore() == null) {
            return false;
        }
        for (String line : slot.lore()) {
            if (line != null && line.toLowerCase(Locale.ROOT).contains("powerup")) {
                return true;
            }
        }
        return false;
    }

    public static boolean sameStack(SlotUpdate first, SlotUpdate second) {
        return first != null
                && second != null
                && path(first).equals(path(second))
                && name(first).equals(name(second))
                && first.count() == second.count()
                && texture(first).equals(texture(second));
    }

    public void reset() {
        chronoSolution.clear();
        ultraSolution.clear();
        superRevealed.clear();
        superHighlights.clear();
        superLastSlot = -1;
        rememberPhase = true;
    }

    public boolean rememberPhase() {
        return rememberPhase;
    }

    /**
     * Chronomatron and Ultrasequencer switch between "watch the sequence" and
     * "repeat the sequence" whenever the status item flips between glowstone and a clock.
     */
    public void updatePhase(SlotUpdate slot) {
        String path = path(slot);
        if (!rememberPhase && GLOWSTONE.equals(path)) {
            rememberPhase = true;
        }
        if (rememberPhase && CLOCK.equals(path)) {
            rememberPhase = false;
        }
    }

    public void onChronomatronSlot(SlotUpdate slot) {
        if (!rememberPhase) {
            return;
        }
        if (isTerracotta(slot)) {
            if (chronoSolution.isEmpty()) {
                chronoSolution.add(new ArrayList<>());
            }
            List<Integer> current = chronoSolution.get(chronoSolution.size() - 1);
            if (!current.contains(slot.slotIndex())) {
                current.add(slot.slotIndex());
            }
            return;
        }
        if (isStainedGlass(slot)
                && !chronoSolution.isEmpty()
                && chronoSolution.get(chronoSolution.size() - 1).contains(slot.slotIndex())) {
            chronoSolution.add(new ArrayList<>());
        }
    }

    /**
     * Consumes the click if it matched the head of the remembered sequence.
     */
    public void onChronomatronClick(int slotIndex) {
        if (rememberPhase || chronoSolution.isEmpty()) {
            return;
        }
        if (chronoSolution.get(0).contains(slotIndex)) {
            chronoSolution.remove(0);
            pruneEmptySteps();
        }
    }

    private void pruneEmptySteps() {
        while (!chronoSolution.isEmpty() && chronoSolution.get(0).isEmpty()) {
            chronoSolution.remove(0);
        }
    }

    public void onUltrasequencerSlot(SlotUpdate slot, List<SlotUpdate> containerSlots) {
        String path = path(slot);
        if (GLOWSTONE.equals(path)) {
            ultraSolution.clear();
            for (SlotUpdate candidate : containerSlots) {
                if (isDye(candidate)) {
                    ultraSolution.add(new Ultra(candidate.slotIndex(), candidate.count()));
                }
            }
            return;
        }
        if (CLOCK.equals(path)) {
            ultraSolution.sort((a, b) -> Integer.compare(a.count(), b.count()));
        }
    }

    public void onUltrasequencerClick(int slotIndex) {
        if (rememberPhase || ultraSolution.isEmpty()) {
            return;
        }
        if (ultraSolution.get(0).slotIndex() == slotIndex) {
            ultraSolution.remove(0);
        }
    }

    public void onSuperpairsSlot(SlotUpdate slot) {
        if (isStatus(slot)) {
            return;
        }
        if (isPowerup(slot)) {
            superHighlights.put(slot.slotIndex(), Highlight.POWERUP);
            return;
        }
        if (isStainedGlass(slot) || isStainedGlassPane(slot) || path(slot).isEmpty() || "air".equals(path(slot))) {
            return;
        }
        SlotUpdate last = superRevealed.get(superLastSlot);
        if (last != null && superLastSlot != slot.slotIndex() && sameStack(slot, last)) {
            superHighlights.put(slot.slotIndex(), Highlight.MATCHED);
            superHighlights.put(superLastSlot, Highlight.MATCHED);
        }
        for (Map.Entry<Integer, SlotUpdate> known : superRevealed.entrySet()) {
            if (superHighlights.containsKey(slot.slotIndex())) {
                break;
            }
            if (known.getKey() != slot.slotIndex() && sameStack(slot, known.getValue())) {
                superHighlights.put(slot.slotIndex(), Highlight.MATCH);
                superHighlights.put(known.getKey(), Highlight.MATCH);
            }
        }
        superRevealed.put(slot.slotIndex(), slot);
        superLastSlot = slot.slotIndex();
    }

    /**
     * Slots to tint for the active experiment, keyed by container slot index.
     */
    public Map<Integer, Highlight> highlights(Experiment experiment) {
        Map<Integer, Highlight> out = new LinkedHashMap<>();
        switch (experiment) {
            case CHRONOMATRON -> {
                if (rememberPhase) {
                    return out;
                }
                if (!chronoSolution.isEmpty()) {
                    for (int slot : chronoSolution.get(0)) {
                        out.put(slot, Highlight.FIRST);
                    }
                }
                if (chronoSolution.size() > 1) {
                    for (int slot : chronoSolution.get(1)) {
                        out.putIfAbsent(slot, Highlight.SECOND);
                    }
                }
            }
            case ULTRASEQUENCER -> {
                if (rememberPhase) {
                    return out;
                }
                if (!ultraSolution.isEmpty()) {
                    out.put(ultraSolution.get(0).slotIndex(), Highlight.FIRST);
                }
                if (ultraSolution.size() > 1) {
                    out.putIfAbsent(ultraSolution.get(1).slotIndex(), Highlight.SECOND);
                }
            }
            case SUPERPAIRS -> out.putAll(superHighlights);
            default -> {
            }
        }
        return out;
    }

    /**
     * True when a click should be swallowed because it is not the next step of the
     * sequence. Superpairs never blocks; only the two sequence games block
     * clicks.
     */
    public boolean shouldBlockClick(Experiment experiment, int slotIndex) {
        return switch (experiment) {
            case CHRONOMATRON -> knownChronoSlot(slotIndex)
                    && (rememberPhase
                            || chronoSolution.isEmpty()
                            || !chronoSolution.get(0).contains(slotIndex));
            case ULTRASEQUENCER -> knownUltraSlot(slotIndex)
                    && (rememberPhase
                            || ultraSolution.isEmpty()
                            || ultraSolution.get(0).slotIndex() != slotIndex);
            default -> false;
        };
    }

    public Optional<Integer> nextChronomatronSlot() {
        if (chronoSolution.isEmpty() || chronoSolution.get(0).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(chronoSolution.get(0).get(0));
    }

    private boolean knownChronoSlot(int slotIndex) {
        Set<Integer> known = new LinkedHashSet<>();
        for (List<Integer> step : chronoSolution) {
            known.addAll(step);
        }
        return known.contains(slotIndex);
    }

    private boolean knownUltraSlot(int slotIndex) {
        for (Ultra entry : ultraSolution) {
            if (entry.slotIndex() == slotIndex) {
                return true;
            }
        }
        return false;
    }

    private static String path(SlotUpdate slot) {
        return slot == null || slot.itemPath() == null ? "" : slot.itemPath();
    }

    private static String name(SlotUpdate slot) {
        return slot.name() == null ? "" : slot.name();
    }

    private static String texture(SlotUpdate slot) {
        return slot.textureId() == null ? "" : slot.textureId();
    }
}
