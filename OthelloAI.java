import java.util.ArrayList;

public class OthelloAI implements IOthelloAI {

    /** A depth of 8 is a safe default for 8×8., this gives decent thinking time and strong plays */
    private static final int MAX_DEPTH = 8;

    /** Weight given to the mobility term relative to positional weights. */
    private static final int MOBILITY_WEIGHT = 10;


    /** Reward values per square */
    private static final int[][] WEIGHTS_8X8 = {
            { 120, -20,  20,   5,   5,  20, -20,  120},
            { -20, -40,  -5,  -5,  -5,  -5, -40,  -20},
            {  20,  -5,  15,   3,   3,  15,  -5,   20},
            {   5,  -5,   3,   3,   3,   3,  -5,    5},
            {   5,  -5,   3,   3,   3,   3,  -5,    5},
            {  20,  -5,  15,   3,   3,  15,  -5,   20},
            { -20, -40,  -5,  -5,  -5,  -5, -40,  -20},
            { 120, -20,  20,   5,   5,  20, -20,  120}
    };

    /** Player number for this AI (1 = black, 2 = white). */
    private int maxPlayer;

    /** Opponent's player number. */
    private int minPlayer;

    /** Total number of moves made by our AI. */
    private int moveCount = 0;

    /** Total time spent deciding moves in nanoseconds. */
    private long totalTimeNanos = 0;

    /** Flag to ensure statistics are only printed once. */
    private boolean statisticsPrinted = false;

    @Override
    public Position decideMove(GameState s) {
        long startTime = System.nanoTime();

        maxPlayer = s.getPlayerInTurn();
        minPlayer = (maxPlayer == 1) ? 2 : 1;

        ArrayList<Position> moves = s.legalMoves();
        if (moves.isEmpty()) return new Position(-1, -1);

        Position bestMove  = moves.get(0);
        int      bestValue = Integer.MIN_VALUE;

        for (Position move : moves) {
            // Create a child state and apply the move.
            GameState child = new GameState(s.getBoard(), maxPlayer);
            child.insertToken(move);

            int value = minimax(child, MAX_DEPTH - 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
            if (value > bestValue) {
                bestValue = value;
                bestMove  = move;
            }
        }

        long endTime = System.nanoTime();
        totalTimeNanos += (endTime - startTime);
        moveCount++;

        // Print current statistics after each move
        printCurrentStats();

        return bestMove;
    }


    private int minimax(GameState state, int depth, int alpha, int beta) {
        // Terminal check
        if (state.isFinished()) {
            return terminalScore(state);
        }

        ArrayList<Position> moves = state.legalMoves();

        // Pass: current player has no moves but the game is not over
        if (moves.isEmpty()) {
            int nextPlayer = (state.getPlayerInTurn() == 1) ? 2 : 1;
            GameState passState = new GameState(state.getBoard(), nextPlayer);
            // Depth is NOT decremented: no actual move was made.
            return minimax(passState, depth, alpha, beta);
        }

        // Depth cut-off: evaluate with heuristic
        if (depth == 0) {
            return evaluate(state);
        }

        // Recursive search
        if (state.getPlayerInTurn() == maxPlayer) {
            // Maximising node
            int maxVal = Integer.MIN_VALUE;
            for (Position move : moves) {
                GameState child = new GameState(state.getBoard(), state.getPlayerInTurn());
                child.insertToken(move);
                maxVal = Math.max(maxVal, minimax(child, depth - 1, alpha, beta));
                alpha  = Math.max(alpha, maxVal);
                if (beta <= alpha) break; // Beta cut-off
            }
            return maxVal;
        } else {
            // Minimising node
            int minVal = Integer.MAX_VALUE;
            for (Position move : moves) {
                GameState child = new GameState(state.getBoard(), state.getPlayerInTurn());
                child.insertToken(move);
                minVal = Math.min(minVal, minimax(child, depth - 1, alpha, beta));
                beta   = Math.min(beta, minVal);
                if (beta <= alpha) break; // Alpha cut-off
            }
            return minVal;
        }
    }


    /**
     * Returns a large positive value if the AI won, large negative if it lost,
     * or 0 for a draw.
     */
    private int terminalScore(GameState state) {
        int[] tokens   = state.countTokens();
        int myTokens   = (maxPlayer == 1) ? tokens[0] : tokens[1];
        int oppTokens  = (maxPlayer == 1) ? tokens[1] : tokens[0];
        if (myTokens > oppTokens) return  100_000;
        if (myTokens < oppTokens) return -100_000;
        return 0;
    }

    private int evaluate(GameState state) {
        int[][] board = state.getBoard();
        int size  = board.length;
        int score = 0;

        // 1. Positional weights
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int w = positionWeight(i, j, size);
                if      (board[i][j] == maxPlayer) score += w;
                else if (board[i][j] == minPlayer) score -= w;
            }
        }

        // 2. Mobility
        // Build temporary states to count each player's available moves
        // independently of whose turn it currently is in 'state'.
        int myMoves  = new GameState(board, maxPlayer).legalMoves().size();
        int oppMoves = new GameState(board, minPlayer).legalMoves().size();
        score += MOBILITY_WEIGHT * (myMoves - oppMoves);

        return score;
    }

    /**
     * Returns the strategic weight for board position (col,row)
     * on a board of the given size.
     */
    private int positionWeight(int col, int row, int size) {
        if (size == 8) {
            return WEIGHTS_8X8[col][row];
        }
        boolean isCorner  = (col == 0 || col == size - 1) && (row == 0 || row == size - 1);
        boolean isXSquare = (col == 1 || col == size - 2) && (row == 1 || row == size - 2);
        boolean isEdge    = col == 0 || col == size - 1 || row == 0 || row == size - 1;
        if (isCorner)  return  100;
        if (isXSquare) return  -30;
        if (isEdge)    return   10;
        return 3;
    }

    /**
     * Prints current statistics after each move.
     */
    private void printCurrentStats() {
        double averageTimeSeconds = (totalTimeNanos / (double) moveCount) / 1_000_000_000.0;
        System.out.printf("OthelloAI - Move %d | Avg time: %.2f seconds%n", moveCount, averageTimeSeconds);
    }

    /**
     * Prints final performance statistics for this AI to the console.
     * Shows total moves made and average time per move.
     */
    public void printStatistics() {
        if (statisticsPrinted) return;
        statisticsPrinted = true;

        if (moveCount == 0) {
            System.out.println("OthelloAI Statistics: No moves made.");
            return;
        }

        double averageTimeSeconds = (totalTimeNanos / (double) moveCount) / 1_000_000_000.0;
        System.out.println("\nOthelloAI Statistics:");
        System.out.println("Total moves: " + moveCount);
        System.out.printf("Average time per move: %.2f seconds%n", averageTimeSeconds);
    }
}
