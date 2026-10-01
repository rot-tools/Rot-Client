package fi.rotclient;

import java.util.Optional;

/** Solves only the NPC's explicit arrow sequence. No block/entity scanning. */
final class PuzzlerPolicy {
    record Target(int x, int y, int z) {}
    static Optional<Target> solve(String message) {
        String text = CommissionDisplayPolicy.normalizeLine(message);
        if (!text.startsWith("[NPC] Puzzler:")) return Optional.empty();
        String arrows = text.substring("[NPC] Puzzler:".length()).replace(" ", "");
        if (arrows.isEmpty() || arrows.length() > 64 || !arrows.matches("[▲▶▼◀]+")) return Optional.empty();
        int x = 181, z = 135;
        for (char arrow : arrows.toCharArray()) {
            switch (arrow) {
                case '▲' -> z++;
                case '▼' -> z--;
                case '▶' -> x--;
                case '◀' -> x++;
                default -> throw new IllegalStateException();
            }
        }
        return Optional.of(new Target(x, 195, z));
    }
    private PuzzlerPolicy() {}
}
