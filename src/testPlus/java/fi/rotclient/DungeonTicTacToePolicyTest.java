package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonTicTacToePolicyTest {
    @Test
    void bestMoveTakesTheOpenWinningCell() {
        char[] board = new char[9];
        board[0] = DungeonTicTacToePolicy.PLAYER;
        board[1] = DungeonTicTacToePolicy.PLAYER;
        board[3] = DungeonTicTacToePolicy.OPPONENT;
        board[4] = DungeonTicTacToePolicy.OPPONENT;
        List<Integer> moves = DungeonTicTacToePolicy.findBestMoves(
                board, DungeonTicTacToePolicy.PLAYER, DungeonTicTacToePolicy.OPPONENT);
        assertTrue(moves.contains(2));
    }

    @Test
    void indexToPosFollowsTheWallGrid() {
        DungeonTicTacToePolicy.BoardPos leftmost = new DungeonTicTacToePolicy.BoardPos(10, 72, 20);
        assertEquals(new DungeonTicTacToePolicy.BoardPos(10, 72, 20),
                DungeonTicTacToePolicy.indexToPos(0, leftmost, 'X', 1));
        assertEquals(new DungeonTicTacToePolicy.BoardPos(9, 71, 20),
                DungeonTicTacToePolicy.indexToPos(4, leftmost, 'X', 1));
        assertEquals(DungeonTicTacToePolicy.PLAYER, DungeonTicTacToePolicy.markFromMapColor(33));
        assertEquals(DungeonTicTacToePolicy.OPPONENT, DungeonTicTacToePolicy.markFromMapColor(114));
    }
}
