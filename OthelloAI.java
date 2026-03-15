import java.util.ArrayList;

/**
 * Othello AI using Minimax search with Alpha-Beta pruning.
 *
 * Evaluation combines:
 *   1. Positional weights  – corners are highly valuable; X-squares (diagonal
 *      to corners) are dangerous and penalised.
 *   2. Mobility            – having more legal moves than the opponent is good.
 *
 * Rename this class to match your group (e.g. OthelloAI13) and update the
 * file name accordingly before handing in.
 */
public class OthelloAI implements IOthelloAI {

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    /** How many plies (half-moves) to search ahead. Increase for stronger play
     *  at the cost of longer thinking time. 7 is a safe default for 8×8. */
    private static final int MAX_DEPTH = 7;

    /** Weight given to the mobility term relative to positional weights. */
    private static final int MOBILITY_WEIGHT = 10;

    // -------------------------------------------------------------------------
    // Positional weight table for the standard 8×8 board
    // -------------------------------------------------------------------------

    /**
     * Strategic values per square for 8×8 Othello.
     *  Corners (120) : can never be flipped – extremely stable.
     *  X-squares (-40): diagonal to a corner; dangerous if the corner is empty.
     *  C-squares (-20): edge squares adjacent to a corner; also risky.
     *  Edges (20/5)   : generally stable once surrounded.
     *  Inner squares  : modest positive values.
     */
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

    // -------------------------------------------------------------------------
    // Instance state (set once per decideMove call)
    // -------------------------------------------------------------------------

    /** Player number for this AI (1 = black, 2 = white). */
    private int maxPlayer;

    /** Opponent's player number. */
    private int minPlayer;

    // -------------------------------------------------------------------------
    // IOthelloAI interface
    // -------------------------------------------------------------------------

    /**
     * Selects the best move for the current player using Minimax with
     * Alpha-Beta pruning.
     *
     * @param s The current game state. It is the AI's turn.
     * @return The position chosen by the AI, or (-1,-1) if no moves exist.
     */
    @Override
    public Position decideMove(GameState s) {
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

        return bestMove;
    }

    // -------------------------------------------------------------------------
    // Minimax with Alpha-Beta pruning
    // -------------------------------------------------------------------------

    /**
     * Recursive Minimax search with Alpha-Beta pruning.
     *
     * The maximising/minimising role is determined by whose turn it is in
     * {@code state}: if it is {@code maxPlayer}'s turn we maximise, otherwise
     * we minimise.
     *
     * @param state Current game state.
     * @param depth Remaining search depth.
     * @param alpha Best value found so far for the maximiser (lower bound).
     * @param beta  Best value found so far for the minimiser (upper bound).
     * @return Heuristic (or terminal) value of {@code state}.
     */
    private int minimax(GameState state, int depth, int alpha, int beta) {
        // --- Terminal check ---
        if (state.isFinished()) {
            return terminalScore(state);
        }

        ArrayList<Position> moves = state.legalMoves();

        // --- Pass: current player has no moves but the game is not over ---
        if (moves.isEmpty()) {
            int nextPlayer = (state.getPlayerInTurn() == 1) ? 2 : 1;
            GameState passState = new GameState(state.getBoard(), nextPlayer);
            // Depth is NOT decremented: no actual move was made.
            return minimax(passState, depth, alpha, beta);
        }

        // --- Depth cut-off: evaluate with heuristic ---
        if (depth == 0) {
            return evaluate(state);
        }

        // --- Recursive search ---
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

    // -------------------------------------------------------------------------
    // Scoring helpers
    // -------------------------------------------------------------------------

    /**
     * Returns a large positive value if the AI won, large negative if it lost,
     * or 0 for a draw. Only called on genuinely finished games.
     */
    private int terminalScore(GameState state) {
        int[] tokens   = state.countTokens();
        int myTokens   = (maxPlayer == 1) ? tokens[0] : tokens[1];
        int oppTokens  = (maxPlayer == 1) ? tokens[1] : tokens[0];
        if (myTokens > oppTokens) return  100_000;
        if (myTokens < oppTokens) return -100_000;
        return 0;
    }

    /**
     * Heuristic evaluation of a non-terminal state.
     *
     * <p>Two components are combined:
     * <ol>
     *   <li><b>Positional score</b>: sum of strategic weights for all tokens
     *       owned by maxPlayer minus the sum for minPlayer.  Encoding stable
     *       corners as highly positive and risky X-squares as negative guides
     *       the AI toward strategically strong moves.</li>
     *   <li><b>Mobility score</b>: the difference in the number of legal moves
     *       available to each player.  Greater mobility means more options and
     *       more control over the game; limiting the opponent's choices is just
     *       as important.</li>
     * </ol>
     *
     * @param state The state to evaluate.
     * @return A score that is higher the better the state is for maxPlayer.
     */
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
     * Returns the strategic weight for board position ({@code col}, {@code row})
     * on a board of the given {@code size}.
     *
     * <p>For the standard 8×8 board the pre-computed {@link #WEIGHTS_8X8}
     * table is used directly.  For other sizes a generalised rule is applied:
     * corners are most valuable, X-squares (one step diagonal from a corner)
     * are penalised, edges carry a moderate bonus, and all other squares have
     * a small positive value.</p>
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
}
