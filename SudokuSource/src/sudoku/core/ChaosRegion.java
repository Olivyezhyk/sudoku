package sudoku.core;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One irregular region in Chaos Sudoku.
 *
 * Contains exactly 9 cells; digits 1–9 must not repeat within it
 * (same rule as a classic 3×3 box, but with an arbitrary shape).
 *
 * Internally uses a fast boolean[9][9] membership lookup so that
 * conflict checks run in O(9) without any stream overhead.
 */
public class ChaosRegion {

    public record Cell(int row, int col) {}

    private final int         regionIndex;
    private final List<Cell>  cells;       // unmodifiable, always size 9
    private final Color       color;
    private final boolean[][] member;      // member[r][c] = true if cell belongs here

    public ChaosRegion(int regionIndex, List<Cell> cells, Color color) {
        if (cells == null || cells.size() != 9)
            throw new IllegalArgumentException(
                    "ChaosRegion " + regionIndex + " must have exactly 9 cells, got "
                            + (cells == null ? "null" : cells.size()));

        this.regionIndex = regionIndex;
        this.cells       = Collections.unmodifiableList(new ArrayList<>(cells));
        this.color       = color;

        this.member = new boolean[9][9];
        for (Cell c : cells) member[c.row()][c.col()] = true;
    }

    // ── getters ───────────────────────────────────────────────────────────────

    public int        getRegionIndex() { return regionIndex; }
    public List<Cell> getCells()       { return cells;       }
    public Color      getColor()       { return color;       }

    // ── constraint checks ─────────────────────────────────────────────────────

    /**
     * Returns true if placing {@code value} at (row, col) would duplicate a
     * digit already in this region.
     */
    public boolean wouldConflict(int[][] board, int row, int col, int value) {
        if (!member[row][col]) return false;
        for (Cell c : cells) {
            if (c.row() == row && c.col() == col) continue;
            if (board[c.row()][c.col()] == value) return true;
        }
        return false;
    }

    /**
     * Returns true if the digit currently at (row, col) conflicts with any
     * other cell in the region (used for error highlighting).
     */
    public boolean hasConflict(int[][] board, int row, int col) {
        if (!member[row][col]) return false;
        int value = board[row][col];
        if (value == 0) return false;
        return wouldConflict(board, row, col, value);
    }

    // ── colour palette ────────────────────────────────────────────────────────

    /**
     * Returns one of 9 visually distinct semi-transparent tints.
     *
     * Colours are spread evenly around the HSB wheel but interleaved so
     * adjacent region indices always look different (warm/cool alternating).
     */
    public static Color paletteColor(int index) {
        // 9 hues chosen so adjacent indices contrast well; alpha = 55
        int[][] rgb = {
                {220,  60,  60},   // 0 — red
                { 60, 180, 220},   // 1 — sky blue
                {210, 190,  40},   // 2 — amber
                { 80, 200,  90},   // 3 — green
                {180,  60, 210},   // 4 — violet
                { 50, 190, 160},   // 5 — teal
                {220, 120,  40},   // 6 — orange
                {100, 100, 220},   // 7 — blue
                {210,  60, 140},   // 8 — rose
        };
        int[] c = rgb[index % rgb.length];
        return new Color(c[0], c[1], c[2], 55);
    }
}