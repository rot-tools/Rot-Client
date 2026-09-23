package fi.rotclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tic Tac Toe best-move solver. Board index 0 is top-left (Y=72 row, first
 * button along the wall), matching the hashed-room stone-button grid.
 */
public final class DungeonTicTacToePolicy {
    public static final char EMPTY = '\0';
    public static final char PLAYER = 'O';
    public static final char OPPONENT = 'X';
    public static final int MAP_COLOR_O = 33;
    public static final int MAP_COLOR_X = 114;

    public record BoardPos(int x, int y, int z) {
    }

    private static final int[][] WIN_SETS = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {6, 4, 2}
    };

    private DungeonTicTacToePolicy() {
    }

    public static char markFromMapColor(int colorByte) {
        int color = colorByte & 255;
        if (color == MAP_COLOR_O) {
            return PLAYER;
        }
        if (color == MAP_COLOR_X) {
            return OPPONENT;
        }
        return EMPTY;
    }

    public static boolean isWon(char[] board) {
        if (board == null || board.length < 9) {
            return false;
        }
        for (int[] line : WIN_SETS) {
            char a = board[line[0]];
            if (a != EMPTY && a == board[line[1]] && a == board[line[2]]) {
                return true;
            }
        }
        return false;
    }

    public static List<Integer> findBestMoves(char[] board, char player, char opponent) {
        if (board == null || board.length < 9) {
            return List.of();
        }
        List<Integer> moves = availableMoves(board);
        if (moves.isEmpty()) {
            return List.of();
        }
        Map<Integer, Double> scores = new LinkedHashMap<>();
        double best = Double.NEGATIVE_INFINITY;
        for (int move : moves) {
            char[] next = Arrays.copyOf(board, board.length);
            next[move] = player;
            double score = minimax(next, player, opponent, false, 1);
            scores.put(move, score);
            if (score > best) {
                best = score;
            }
        }
        List<Integer> out = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : scores.entrySet()) {
            if (entry.getValue() >= best - 1.0E-4D) {
                out.add(entry.getKey());
            }
        }
        return List.copyOf(out);
    }

    public static BoardPos indexToPos(int index, BoardPos leftmostRow, char facing, int sign) {
        if (leftmostRow == null) {
            return new BoardPos(0, 72, 0);
        }
        int row = Math.floorMod(index, 3);
        int col = index / 3;
        int x = facing == 'X' ? leftmostRow.x() - sign * row : leftmostRow.x();
        int z = facing == 'Z' ? leftmostRow.z() - sign * row : leftmostRow.z();
        return new BoardPos(x, 72 - col, z);
    }

    public static int boardIndex(int column, int row) {
        return column * 3 + row;
    }

    private static List<Integer> availableMoves(char[] board) {
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (board[i] == EMPTY) {
                out.add(i);
            }
        }
        return out;
    }

    private static double minimax(char[] board, char ai, char opp, boolean maximizing, int depth) {
        boolean won = isWon(board);
        List<Integer> moves = availableMoves(board);
        if (!won && !moves.isEmpty()) {
            if (maximizing) {
                double best = Double.NEGATIVE_INFINITY;
                for (int move : moves) {
                    char[] next = Arrays.copyOf(board, board.length);
                    next[move] = ai;
                    best = Math.max(best, minimax(next, ai, opp, false, depth + 1));
                }
                return best;
            }
            double best = Double.POSITIVE_INFINITY;
            for (int move : moves) {
                char[] next = Arrays.copyOf(board, board.length);
                next[move] = opp;
                best = Math.min(best, minimax(next, ai, opp, true, depth + 1));
            }
            return best;
        }
        if (won) {
            return maximizing ? -1.0D / depth : 1.0D / depth;
        }
        return 0.0D;
    }
}
