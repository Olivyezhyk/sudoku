package sudoku.core;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single "cage" in Killer Sudoku.
 * A cage is a group of 2-5 cells with a target sum; digits inside must be unique.
 *
 * Each cage also carries a pastel background {@link Color} assigned by the
 * generator so that neighbouring cages always get visually distinct tints.
 */
public class Cage {

    public record Cell(int row, int col) {}

    private final List<Cell> cells;
    private final int        targetSum;
    private final Color      color;      // pastel tint for UI rendering

    /**
     * Full constructor used by the generator (includes colour).
     */
    public Cage(int targetSum, List<Cell> cells, Color color) {
        if (cells == null || cells.isEmpty())
            throw new IllegalArgumentException("Cage must have at least one cell");
        this.targetSum = targetSum;
        this.cells     = Collections.unmodifiableList(new ArrayList<>(cells));
        this.color     = color != null ? color : new Color(200, 200, 200, 70);
    }

    /**
     * Legacy constructor — assigns a neutral grey tint.
     * Kept so any existing call sites that don't pass a colour still compile.
     */
    public Cage(int targetSum, List<Cell> cells) {
        this(targetSum, cells, new Color(200, 200, 200, 70));
    }

    public int        getTargetSum() { return targetSum; }
    public List<Cell> getCells()     { return cells;     }
    public Color      getColor()     { return color;     }

    // ── Colour palette ────────────────────────────────────────────────────────

    /**
     * Returns one of 12 vivid, evenly-spaced colours (every 30° on the HSB
     * colour wheel, saturation=90%, brightness=95%).
     *
     * Stored as full-brightness RGB — the alpha and darkening are applied by
     * the caller ({@code drawKillerCageTints} for fills,
     * {@code drawKillerCageOutlines} for borders).
     *
     * @param index  0-based index — wraps with modulo if > 11
     */
    public static Color paletteColor(int index) {
        // 12 colours chosen to be as visually distinct as possible on a dark
        // background. Warm/cool/neutral hues are interleaved so adjacent palette
        // indices never look similar (red is NOT next to orange, etc.).
        // Order: red, cyan, yellow, blue, lime, magenta,
        //        orange, sky, pink, teal, white, violet
        int[][] rgb = {
                {230,  50,  50},   //  0 — red
                { 50, 220, 220},   //  1 — cyan
                {230, 210,  40},   //  2 — yellow
                { 70,  90, 230},   //  3 — blue
                {100, 220,  50},   //  4 — lime
                {210,  50, 200},   //  5 — magenta
                {230, 130,  40},   //  6 — orange
                { 50, 160, 230},   //  7 — sky
                {230,  80, 140},   //  8 — pink
                { 40, 190, 140},   //  9 — teal
                {200, 200, 200},   // 10 — white/silver
                {140,  60, 220},   // 11 — violet
        };
        int[] c = rgb[index % rgb.length];
        return new Color(c[0], c[1], c[2], 255);
    }

    /**
     * Returns the top-left cell of the cage — used to draw the sum label.
     */
    public Cell getTopLeftCell() {
        return cells.stream()
                .min((a, b) -> a.row() != b.row() ? a.row() - b.row() : a.col() - b.col())
                .orElse(cells.get(0));
    }

    /**
     * Returns true when all cells are filled, no digit is repeated, and the
     * sum equals targetSum.
     */
    public boolean isSatisfied(int[][] board) {
        int sum = 0;
        boolean[] seen = new boolean[10];
        for (Cell c : cells) {
            int v = board[c.row()][c.col()];
            if (v == 0)  return false;
            if (seen[v]) return false;
            seen[v] = true;
            sum += v;
        }
        return sum == targetSum;
    }

    /**
     * Returns true if placing {@code value} at (row, col) would duplicate a
     * digit already present in this cage.
     */
    public boolean wouldConflict(int[][] board, int row, int col, int value) {
        boolean cellInCage = cells.stream()
                .anyMatch(c -> c.row() == row && c.col() == col);
        if (!cellInCage) return false;

        for (Cell c : cells) {
            if (c.row() == row && c.col() == col) continue;
            if (board[c.row()][c.col()] == value)  return true;
        }
        return false;
    }

    /** True if the running partial sum already exceeds targetSum. */
    public boolean isSumExceeded(int[][] board) {
        int sum = 0;
        for (Cell c : cells) {
            int v = board[c.row()][c.col()];
            if (v != 0) sum += v;
        }
        return sum > targetSum;
    }
}