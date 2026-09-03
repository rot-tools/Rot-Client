package fi.rotclient;

import java.util.ArrayDeque;

/**
 * Visibility-only undo for the world HUD editor. Right-click hide pushes;
 * Ctrl+Z restores. Does not record moves or scale.
 */
public final class HudEditorVisibilityUndo {
    public static final int MAX_STEPS = 16;

    public enum Kind {
        QOL,
        MINING_TRACKER,
        POWDER_CHEST
    }

    public record Entry(Kind kind, String settingId, boolean moduleToggle) {
        public Entry {
            kind = kind == null ? Kind.QOL : kind;
            settingId = settingId == null ? "" : settingId;
        }

        public static Entry qol(String settingId, boolean moduleToggle) {
            return new Entry(Kind.QOL, settingId, moduleToggle);
        }

        public static Entry miningTracker() {
            return new Entry(Kind.MINING_TRACKER, MiningTrackerCatalogPolicy.TRACKER, true);
        }

        public static Entry powderChest() {
            return new Entry(Kind.POWDER_CHEST, MiningTrackerCatalogPolicy.POWDER_HUD, false);
        }
    }

    private final ArrayDeque<Entry> undo = new ArrayDeque<>();
    private final ArrayDeque<Entry> redo = new ArrayDeque<>();

    public void pushHide(Entry entry) {
        if (entry == null) {
            return;
        }
        undo.addFirst(entry);
        while (undo.size() > MAX_STEPS) {
            undo.removeLast();
        }
        redo.clear();
    }

    public Entry undo() {
        Entry entry = undo.pollFirst();
        if (entry != null) {
            redo.addFirst(entry);
        }
        return entry;
    }

    public Entry redo() {
        Entry entry = redo.pollFirst();
        if (entry != null) {
            undo.addFirst(entry);
        }
        return entry;
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }
}
