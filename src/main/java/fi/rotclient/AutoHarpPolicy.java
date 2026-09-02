package fi.rotclient;

import java.util.List;
import java.util.OptionalInt;

/**
 * Auto Harp: Melody's Harp GUI is a 9x6 chest titled
 * {@code Harp - ...}. Notes light as quartz blocks in slots 37-43. When that
 * 7-bit mask changes, middle-click ({@code CLONE}) the first lit note.
 */
public final class AutoHarpPolicy {
    public static final int FIRST_NOTE_SLOT = 37;
    public static final int LAST_NOTE_SLOT = 43;
    public static final String TITLE_PREFIX = "Harp - ";
    public static final String QUARTZ_BLOCK = "quartz_block";

    public record TickResult(int hash, OptionalInt clickSlot) {
        public TickResult {
            clickSlot = clickSlot == null ? OptionalInt.empty() : clickSlot;
        }
    }

    private AutoHarpPolicy() {
    }

    public static boolean isHarpTitle(String title) {
        return title != null && title.startsWith(TITLE_PREFIX);
    }

    /**
     * @param quartzByNoteIndex seven booleans for slots 37-43, true when the
     *                          slot holds a quartz block
     * @param previousHash      last computed bitmask, or 0 before the first note
     */
    public static TickResult nextClick(List<Boolean> quartzByNoteIndex, int previousHash) {
        if (quartzByNoteIndex == null || quartzByNoteIndex.size() < 7) {
            return new TickResult(previousHash, OptionalInt.empty());
        }
        int hash = 0;
        int slot = -1;
        for (int i = 0; i < 7; i++) {
            boolean quartz = Boolean.TRUE.equals(quartzByNoteIndex.get(i));
            hash = (hash << 1) | (quartz ? 1 : 0);
            if (slot == -1 && quartz) {
                slot = FIRST_NOTE_SLOT + i;
            }
        }
        if (hash == previousHash) {
            return new TickResult(previousHash, OptionalInt.empty());
        }
        return new TickResult(
                hash, slot < 0 ? OptionalInt.empty() : OptionalInt.of(slot));
    }
}
