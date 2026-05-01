package sudoku.core;

import java.util.*;

/**
 * Central model for a Sudoku game.
 *
 * Extends the original Classic logic to support:
 *   – Chaos Mode  : irregular 9-cell regions (in addition to rows/cols/boxes)
 *   – Killer Mode : cages with a target sum and no-repeat constraint
 *
 * The original public API is preserved so existing UI classes need no changes.
 */
public class GameState {

    public static final int SIZE         = 9;
    public static final int MAX_MISTAKES = 3;
    public static final int MAX_HINTS    = 3;

    // ── board data ──────────────────────────────────────────────────────────
    private final int[][]           board;
    private final int[][]           solution;
    private final boolean[][]       fixed;
    private final boolean[][]       errorCells;
    private final List<Integer>[][] notes;

    // ── selection & game state ───────────────────────────────────────────────
    private int  selectedRow   = -1;
    private int  selectedCol   = -1;
    private int  mistakeCount  = 0;
    private int  hintsLeft     = MAX_HINTS;
    private boolean gameOver   = false;
    private boolean won        = false;
    private boolean pencilMode = false;

    // ── mode-specific data ───────────────────────────────────────────────────
    private final GameMode         gameMode;
    private final List<ChaosRegion> chaosRegions;   // non-null in CHAOS mode
    private final List<Cage>        cages;           // non-null in KILLER mode

    // ── constructors ─────────────────────────────────────────────────────────

    /** Classic mode constructor (original signature). */
    @SuppressWarnings("unchecked")
    public GameState(int[][] puzzle, int[][] solution) {
        this(puzzle, solution, GameMode.CLASSIC, Collections.emptyList(), Collections.emptyList());
    }

    /** Full constructor used by the new modes. */
    @SuppressWarnings("unchecked")
    public GameState(int[][] puzzle, int[][] solution,
                     GameMode gameMode,
                     List<ChaosRegion> chaosRegions,
                     List<Cage> cages) {
        this.gameMode     = gameMode;
        this.chaosRegions = chaosRegions != null ? chaosRegions : Collections.emptyList();
        this.cages        = cages        != null ? cages        : Collections.emptyList();

        board      = new int[SIZE][SIZE];
        this.solution = new int[SIZE][SIZE];
        fixed      = new boolean[SIZE][SIZE];
        errorCells = new boolean[SIZE][SIZE];
        notes      = new List[SIZE][SIZE];

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                board[r][c]      = puzzle[r][c];
                this.solution[r][c] = solution[r][c];
                fixed[r][c]      = (puzzle[r][c] != 0);
                notes[r][c]      = new ArrayList<>();
            }
        }
    }

    // ── selection ────────────────────────────────────────────────────────────

    public void selectCell(int row, int col) { selectedRow = row; selectedCol = col; }
    public int  getSelectedRow()             { return selectedRow; }
    public int  getSelectedCol()             { return selectedCol; }

    // ── queries ──────────────────────────────────────────────────────────────

    public int          getValue(int r, int c)  { return board[r][c];       }
    public boolean      isFixed(int r, int c)   { return fixed[r][c];       }
    public boolean      isError(int r, int c)   { return errorCells[r][c];  }
    public List<Integer> getNotes(int r, int c) { return notes[r][c];       }
    public int          getMistakeCount()       { return mistakeCount;       }
    public int          getMaxMistakes()        { return MAX_MISTAKES;       }
    public int          getHintsLeft()          { return hintsLeft;          }
    public boolean      isGameOver()            { return gameOver;           }
    public boolean      isWon()                 { return won;                }
    public boolean      isPencilMode()          { return pencilMode;         }
    public void         setPencilMode(boolean v){ pencilMode = v;            }
    public int[][]      getSolution()           { return solution;           }

    public GameMode         getGameMode()    { return gameMode;     }
    public List<ChaosRegion> getChaosRegions(){ return chaosRegions; }
    public List<Cage>        getCages()      { return cages;         }

    // ── entering a number ────────────────────────────────────────────────────

    public void enterNumber(int num) {
        if (gameOver || won || selectedRow < 0 || selectedCol < 0) return;
        int r = selectedRow, c = selectedCol;
        if (fixed[r][c]) return;

        if (pencilMode) {
            if (board[r][c] == 0) {
                if (notes[r][c].contains(num)) notes[r][c].remove((Integer) num);
                else                           notes[r][c].add(num);
            }
            return;
        }

        board[r][c] = num;
        notes[r][c].clear();

        if (num != solution[r][c]) {
            errorCells[r][c] = true;
            mistakeCount++;
            if (mistakeCount >= MAX_MISTAKES) gameOver = true;
        } else {
            errorCells[r][c] = false;
            clearRelatedErrors(r, c);
            checkWin();
        }
    }

    public boolean isGiven(int row, int col) {
        return fixed[row][col];
    }

    public void deleteSelected() {
        if (selectedRow < 0 || selectedCol < 0) return;
        int r = selectedRow, c = selectedCol;
        if (fixed[r][c]) return;
        board[r][c] = 0;
        errorCells[r][c] = false;
        notes[r][c].clear();
    }

    public boolean useHint() {
        if (hintsLeft <= 0 || selectedRow < 0 || selectedCol < 0) return false;
        int r = selectedRow, c = selectedCol;
        if (fixed[r][c] || board[r][c] == solution[r][c]) return false;

        board[r][c]      = solution[r][c];
        errorCells[r][c] = false;
        fixed[r][c]      = true;
        notes[r][c].clear();
        hintsLeft--;
        checkWin();
        return true;
    }

    // ── internal helpers ─────────────────────────────────────────────────────

    private void clearRelatedErrors(int row, int col) {
        int val = board[row][col];
        for (int i = 0; i < SIZE; i++) {
            if (errorCells[row][i] && board[row][i] == val) errorCells[row][i] = false;
            if (errorCells[i][col] && board[i][col] == val) errorCells[i][col] = false;
        }
        int br = (row / 3) * 3, bc = (col / 3) * 3;
        for (int r = br; r < br + 3; r++)
            for (int c = bc; c < bc + 3; c++)
                if (errorCells[r][c] && board[r][c] == val) errorCells[r][c] = false;
    }

    private void checkWin() {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (board[r][c] != solution[r][c]) return;
        won = true;
    }

    /**
     * Checks whether placing {@code value} at (row,col) violates ANY constraint
     * for the current game mode. Used by the generator/validator.
     */
    public boolean isValidPlacement(int row, int col, int value) {
        // Standard Sudoku checks
        for (int i = 0; i < SIZE; i++) {
            if (i != col && board[row][i] == value) return false;
            if (i != row && board[i][col] == value) return false;
        }
        int br = (row / 3) * 3, bc = (col / 3) * 3;
        for (int r = br; r < br + 3; r++)
            for (int c = bc; c < bc + 3; c++)
                if ((r != row || c != col) && board[r][c] == value) return false;

        // Chaos Mode: check irregular regions
        for (ChaosRegion region : chaosRegions)
            if (region.wouldConflict(board, row, col, value)) return false;

        // Killer Mode: check cage duplicates and sum overflow
        for (Cage cage : cages) {
            if (cage.wouldConflict(board, row, col, value)) return false;
            // Temporarily place to test sum
            board[row][col] = value;
            boolean exceeded = cage.isSumExceeded(board);
            board[row][col] = 0;
            if (exceeded) return false;
        }

        return true;
    }
}
