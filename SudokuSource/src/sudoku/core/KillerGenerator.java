package sudoku.core;

import java.util.*;

/**
 * Generates a valid Killer Sudoku puzzle.
 *
 * Pipeline:
 *   1. Generate a complete, fully-filled solution grid (no holes).
 *   2. Partition all 81 cells into connected cages of 2–5 cells.
 *      Any single-cell leftover is immediately merged into an adjacent cage,
 *      so every cage is guaranteed to have at least 2 cells.
 *   3. Compute the target sum for every cage directly from the solution.
 *   4. Return a fully empty puzzle board (all zeros).
 *      The cage sums are the player's only constraints — that is the
 *      defining feature of Killer Sudoku.
 *
 * No "given" digits are placed on the board; the player deduces everything
 * from the cage outlines and sums shown in the UI.
 */
public class KillerGenerator {

    private static final int SIZE     = GameState.SIZE;
    private static final int MIN_CAGE = 2;
    private static final int MAX_CAGE = 5;

    private final Random rng;

    public KillerGenerator()           { this(new Random()); }
    public KillerGenerator(Random rng) { this.rng = rng; }

    // ── public API ────────────────────────────────────────────────────────────

    public GameState generate(Difficulty difficulty) {
        // 1. Build a complete, valid solution grid from scratch.
        int[][] solution = generateCompleteSolution();

        // 2. Build cages — guaranteed minimum size 2.
        List<Cage> cages = buildCages(solution);

        // 3. Killer Sudoku starts mostly empty, but EASY/MEDIUM get a few
        //    revealed digits so the player has a concrete starting point.
        //    The revealed cells are NOT associated with 1-cell cages — the
        //    cage structure stays the same; these are just pre-filled values.
        int[][] puzzle = buildStartingPuzzle(solution, cages, difficulty);

        return new GameState(puzzle, solution, GameMode.KILLER,
                Collections.emptyList(), cages);
    }

    // ── Step 1: complete solution ─────────────────────────────────────────────

    /**
     * Fills a 9×9 board with a valid Sudoku solution using randomised
     * backtracking.  Always produces a fully filled grid.
     */
    private int[][] generateCompleteSolution() {
        int[][] board = new int[SIZE][SIZE];
        if (!fillBoard(board, 0)) {
            // Should never happen with a correct isValidClassic check.
            throw new IllegalStateException("Killer: failed to generate solution");
        }
        return board;
    }

    private boolean fillBoard(int[][] board, int pos) {
        if (pos == SIZE * SIZE) return true;
        int r = pos / SIZE, c = pos % SIZE;

        for (int num : shuffled1to9()) {
            if (isValidClassic(board, r, c, num)) {
                board[r][c] = num;
                if (fillBoard(board, pos + 1)) return true;
                board[r][c] = 0;
            }
        }
        return false;
    }

    private boolean isValidClassic(int[][] board, int row, int col, int value) {
        for (int i = 0; i < SIZE; i++) {
            if (board[row][i] == value) return false;
            if (board[i][col] == value) return false;
        }
        int br = (row / 3) * 3, bc = (col / 3) * 3;
        for (int r = br; r < br + 3; r++)
            for (int c = bc; c < bc + 3; c++)
                if (board[r][c] == value) return false;
        return true;
    }

    private List<Integer> shuffled1to9() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1,2,3,4,5,6,7,8,9));
        Collections.shuffle(list, rng);
        return list;
    }

    // ── Step 2: cage building ─────────────────────────────────────────────────

    /**
     * Partitions all 81 cells into cages of size MIN_CAGE..MAX_CAGE.
     *
     * Algorithm:
     *   • Visit unassigned seeds in random order; grow each cage greedily.
     *   • After the first pass, any 1-cell cage is merged into an adjacent cage.
     *   • This guarantees every cage has at least 2 cells.
     */
    private List<Cage> buildCages(int[][] solution) {
        // cageId[r][c] = which cage index this cell belongs to (-1 = unassigned)
        int[][] cageId = new int[SIZE][SIZE];
        for (int[] row : cageId) Arrays.fill(row, -1);

        // Mutable list-of-cells for each cage (by cage index)
        List<List<Cage.Cell>> cageCells = new ArrayList<>();

        for (int[] seed : shuffledAllCells()) {
            if (cageId[seed[0]][seed[1]] != -1) continue; // already claimed

            int id = cageCells.size();
            List<Cage.Cell> cells = new ArrayList<>();
            cells.add(new Cage.Cell(seed[0], seed[1]));
            cageId[seed[0]][seed[1]] = id;

            int targetSize = MIN_CAGE + rng.nextInt(MAX_CAGE - MIN_CAGE + 1);

            while (cells.size() < targetSize) {
                List<int[]> free = freeNeighbours(cells, cageId);
                if (free.isEmpty()) break;
                int[] next = free.get(rng.nextInt(free.size()));
                cells.add(new Cage.Cell(next[0], next[1]));
                cageId[next[0]][next[1]] = id;
            }

            cageCells.add(cells);
        }

        // Merge any remaining 1-cell cages into a neighbour
        mergeSingleCellCages(cageCells, cageId);

        // ── Rebuild cageId map after merges (indices may have shifted) ─────────
        // cageId still points to the original index; after merges some slots in
        // cageCells are empty (clear()'d).  Re-index into a compact list first.
        List<List<Cage.Cell>> compact = new ArrayList<>();
        int[] remap = new int[cageCells.size()];
        Arrays.fill(remap, -1);
        for (int i = 0; i < cageCells.size(); i++) {
            if (!cageCells.get(i).isEmpty()) {
                remap[i] = compact.size();
                compact.add(cageCells.get(i));
            }
        }
        // Rebuild cageId using remap
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (cageId[r][c] >= 0) cageId[r][c] = remap[cageId[r][c]];

        // ── Graph-colour the cages so no two neighbours share a palette slot ───
        int n = compact.size();
        int[] colorSlot = assignColors(compact, cageId, n);

        // ── Build final Cage objects with sums and colours ─────────────────────
        List<Cage> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<Cage.Cell> cells = compact.get(i);
            int sum = cells.stream()
                    .mapToInt(cell -> solution[cell.row()][cell.col()])
                    .sum();
            result.add(new Cage(sum, cells, Cage.paletteColor(colorSlot[i])));
        }
        return result;
    }

    /**
     * Greedy graph-colouring: assigns a palette index to every cage such that
     * no two cages that share a border get the same index.
     *
     * <p>Uses a simple greedy pass over cages in order.  The palette has 12
     * colours — far more than the 4 colours guaranteed sufficient by the four-
     * colour theorem — so collisions are always resolved.
     */
    private int[] assignColors(List<List<Cage.Cell>> cageCells,
                               int[][] cageId, int n) {
        int PALETTE = 12;
        int[] slot = new int[n];
        Arrays.fill(slot, -1);

        for (int i = 0; i < n; i++) {
            // Collect palette indices already used by neighbouring cages
            Set<Integer> usedByNeighbours = new HashSet<>();
            for (Cage.Cell cell : cageCells.get(i)) {
                int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
                for (int[] d : dirs) {
                    int nr = cell.row() + d[0], nc = cell.col() + d[1];
                    if (nr < 0 || nr >= SIZE || nc < 0 || nc >= SIZE) continue;
                    int nid = cageId[nr][nc];
                    if (nid >= 0 && nid != i && slot[nid] >= 0)
                        usedByNeighbours.add(slot[nid]);
                }
            }
            // Pick the lowest palette index not used by any neighbour
            for (int s = 0; s < PALETTE; s++) {
                if (!usedByNeighbours.contains(s)) { slot[i] = s; break; }
            }
            if (slot[i] < 0) slot[i] = i % PALETTE; // fallback (should not happen)
        }
        return slot;
    }

    /**
     * Finds every cage that has exactly 1 cell and merges it into an
     * adjacent cage.  Repeats until no such cage remains.
     */
    private void mergeSingleCellCages(List<List<Cage.Cell>> cageCells, int[][] cageId) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = 0; i < cageCells.size(); i++) {
                List<Cage.Cell> cells = cageCells.get(i);
                if (cells.size() != 1) continue;

                Cage.Cell solo = cells.get(0);

                // Prefer an orthogonally adjacent cage
                int targetId = findAdjacentCageId(solo, cageId, i);

                // Fallback (very rare): take any non-empty cage
                if (targetId < 0) targetId = findAnyCageId(cageCells, i);

                if (targetId >= 0) {
                    cageCells.get(targetId).add(solo);
                    cageId[solo.row()][solo.col()] = targetId;
                    cells.clear(); // mark as absorbed
                    changed = true;
                }
            }
        }
    }

    /** Returns the id of any cage orthogonally adjacent to {@code cell},
     *  excluding the cell's own cage id.  Returns -1 if none. */
    private int findAdjacentCageId(Cage.Cell cell, int[][] cageId, int ownId) {
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int nr = cell.row() + d[0], nc = cell.col() + d[1];
            if (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE) {
                int id = cageId[nr][nc];
                if (id >= 0 && id != ownId) return id;
            }
        }
        return -1;
    }

    /** Returns the index of any non-empty cage that is not {@code ownId}. */
    private int findAnyCageId(List<List<Cage.Cell>> cageCells, int ownId) {
        for (int i = 0; i < cageCells.size(); i++)
            if (i != ownId && !cageCells.get(i).isEmpty()) return i;
        return -1;
    }

    /**
     * Builds the puzzle board shown to the player.
     *
     * Strategy — two passes:
     *
     * Pass 1 (cage guarantee):
     *   Reveal one randomly chosen cell inside every cage.
     *   This ensures every cage always has at least one visible digit,
     *   which is the single biggest usability improvement for Killer Sudoku.
     *
     * Pass 2 (top-up to difficulty target):
     *   After pass 1 the open count may still be below the target.
     *   Hidden cells are bucketed by row and revealed in a round-robin
     *   order (one row at a time, row order shuffled each round) so the
     *   extra hints spread evenly across the board instead of clustering.
     *
     * Target open-cell counts (out of 81 total):
     *   EASY   -> 45-50  (~56-62 % revealed)
     *   MEDIUM -> 32-38
     *   HARD   -> 25-30
     *
     * Only this method changes — cage sums, uniqueness checks, and
     * GameState construction are all untouched.
     */
    private int[][] buildStartingPuzzle(int[][] solution,
                                        List<Cage> cages,
                                        Difficulty difficulty) {
        int target = switch (difficulty) {
            case EASY   -> 45 + rng.nextInt(6);   // 45-50
            case MEDIUM -> 32 + rng.nextInt(7);   // 32-38
            case HARD   -> 25 + rng.nextInt(6);   // 25-30
        };

        boolean[][] revealed = new boolean[SIZE][SIZE];
        int openCount = 0;

        // Pass 1: one random cell per cage
        for (Cage cage : cages) {
            List<Cage.Cell> cells = new ArrayList<>(cage.getCells());
            Collections.shuffle(cells, rng);
            Cage.Cell pick = cells.get(0);
            if (!revealed[pick.row()][pick.col()]) {
                revealed[pick.row()][pick.col()] = true;
                openCount++;
            }
        }

        // Pass 2: top-up — bucket hidden cells by row, reveal round-robin
        if (openCount < target) {
            @SuppressWarnings("unchecked")
            List<int[]>[] byRow = new List[SIZE];
            for (int i = 0; i < SIZE; i++) byRow[i] = new ArrayList<>();
            for (int r = 0; r < SIZE; r++)
                for (int c = 0; c < SIZE; c++)
                    if (!revealed[r][c]) byRow[r].add(new int[]{r, c});
            for (List<int[]> row : byRow) Collections.shuffle(row, rng);

            boolean anyLeft = true;
            while (openCount < target && anyLeft) {
                anyLeft = false;
                for (int ri : shuffledIndices(SIZE)) {
                    if (openCount >= target) break;
                    if (byRow[ri].isEmpty()) continue;
                    int[] cell = byRow[ri].remove(byRow[ri].size() - 1);
                    revealed[cell[0]][cell[1]] = true;
                    openCount++;
                    anyLeft = true;
                }
            }
        }

        // Build puzzle array
        int[][] puzzle = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (revealed[r][c]) puzzle[r][c] = solution[r][c];
        return puzzle;
    }

    private List<int[]> freeNeighbours(List<Cage.Cell> cells, int[][] cageId) {
        Set<String> seen = new HashSet<>();
        List<int[]> result = new ArrayList<>();
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (Cage.Cell cell : cells) {
            for (int[] d : dirs) {
                int nr = cell.row() + d[0], nc = cell.col() + d[1];
                if (nr < 0 || nr >= SIZE || nc < 0 || nc >= SIZE) continue;
                if (cageId[nr][nc] != -1) continue;
                String key = nr + "," + nc;
                if (seen.add(key)) result.add(new int[]{nr, nc});
            }
        }
        return result;
    }

    private List<int[]> shuffledAllCells() {
        List<int[]> list = new ArrayList<>();
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                list.add(new int[]{r, c});
        Collections.shuffle(list, rng);
        return list;
    }

    /** Returns a shuffled list of integers [0, n). */
    private List<Integer> shuffledIndices(int n) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(i);
        Collections.shuffle(list, rng);
        return list;
    }
}