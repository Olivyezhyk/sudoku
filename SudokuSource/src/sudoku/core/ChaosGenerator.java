package sudoku.core;

import java.util.*;

/**
 * Generates a valid Chaos Sudoku puzzle.
 *
 * Pipeline
 * ────────
 * 1. PARTITION  Divide the 9×9 grid into 9 connected regions of exactly 9
 *               cells each using Voronoi-style BFS growth:
 *               • One seed per classic 3×3 box (guaranteed even spread).
 *               • All 9 frontiers expand simultaneously in shuffled
 *                 round-robin until every cell is claimed.
 *               • If the BFS stalls (extremely rare), unassigned cells are
 *                 force-donated to an adjacent hungry region via BFS scan.
 *               • The result is validated; if any region ≠ 9 cells the
 *                 whole partition is discarded and regenerated (up to
 *                 MAX_RETRIES times).
 *
 * 2. SOLVE      Randomised backtracking solver that respects rows, columns,
 *               classic 3×3 boxes AND the irregular regions.
 *               Returns null on failure so the caller retries with a new
 *               partition (avoids exceptions during normal operation).
 *
 * 3. DIG HOLES  Remove clues based on difficulty.
 */
public class ChaosGenerator {

    private static final int SIZE        = GameState.SIZE;   // 9
    private static final int MAX_RETRIES = 40;

    private final Random rng;

    public ChaosGenerator()           { this(new Random()); }
    public ChaosGenerator(Random rng) { this.rng = rng; }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    public GameState generate(Difficulty difficulty) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            List<ChaosRegion> regions = buildRegions();
            if (regions == null) continue;
            int[][] solution = trySolve(regions);
            if (solution == null) continue;
            int[][] puzzle = digHoles(solution, difficulty);
            return new GameState(puzzle, solution, GameMode.TIMED,
                    regions, Collections.emptyList());
        }
        throw new IllegalStateException(
                "ChaosGenerator: no valid layout found after " + MAX_RETRIES + " attempts");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step 1 — Voronoi BFS partition
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns 9 connected regions of exactly 9 cells each, or null on failure.
     */
    private List<ChaosRegion> buildRegions() {
        int[][] regionOf = new int[SIZE][SIZE];
        for (int[] row : regionOf) Arrays.fill(row, -1);

        @SuppressWarnings("unchecked")
        List<int[]>[] owned = new List[SIZE];
        for (int i = 0; i < SIZE; i++) owned[i] = new ArrayList<>();

        @SuppressWarnings("unchecked")
        ArrayDeque<int[]>[] frontier = new ArrayDeque[SIZE];

        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};

        // ── 1a: one seed per classic 3×3 box ─────────────────────────────────
        for (int bi : shuffledRange(SIZE)) {
            int br = (bi / 3) * 3, bc = (bi % 3) * 3;
            List<int[]> free = new ArrayList<>();
            for (int r = br; r < br + 3; r++)
                for (int c = bc; c < bc + 3; c++)
                    if (regionOf[r][c] < 0) free.add(new int[]{r, c});
            int[] seed = free.get(rng.nextInt(free.size()));
            regionOf[seed[0]][seed[1]] = bi;
            owned[bi].add(seed);
            frontier[bi] = new ArrayDeque<>();
            frontier[bi].add(seed);
        }

        int unclaimed = SIZE * SIZE - SIZE;   // 72 cells after seeding

        // ── 1b: parallel BFS growth ───────────────────────────────────────────
        while (unclaimed > 0) {
            boolean progress = false;

            for (int ri : shuffledRange(SIZE)) {
                if (owned[ri].size() >= SIZE || frontier[ri].isEmpty()) continue;

                int attempts = frontier[ri].size() + 1;
                while (!frontier[ri].isEmpty() && attempts-- > 0) {
                    int[] cur = frontier[ri].poll();

                    List<int[]> freeNbr = new ArrayList<>();
                    for (int[] d : dirs) {
                        int nr = cur[0]+d[0], nc = cur[1]+d[1];
                        if (inGrid(nr, nc) && regionOf[nr][nc] < 0)
                            freeNbr.add(new int[]{nr, nc});
                    }

                    if (!freeNbr.isEmpty()) {
                        Collections.shuffle(freeNbr, rng);
                        int[] next = freeNbr.get(0);
                        regionOf[next[0]][next[1]] = ri;
                        owned[ri].add(next);
                        frontier[ri].add(next);
                        frontier[ri].add(cur);
                        unclaimed--;
                        progress = true;
                        break;
                    }
                }
            }

            // ── 1c: stall — force-assign orphans via BFS ──────────────────────
            if (!progress) {
                for (int r = 0; r < SIZE && unclaimed > 0; r++) {
                    for (int c = 0; c < SIZE && unclaimed > 0; c++) {
                        if (regionOf[r][c] >= 0) continue;
                        int target = nearestHungry(r, c, regionOf, owned, dirs);
                        if (target >= 0) {
                            regionOf[r][c] = target;
                            owned[target].add(new int[]{r, c});
                            frontier[target].add(new int[]{r, c});
                            unclaimed--;
                        }
                    }
                }
                break;
            }
        }

        // ── 1d: validate ──────────────────────────────────────────────────────
        List<ChaosRegion> result = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) {
            if (owned[i].size() != SIZE) return null;
            List<ChaosRegion.Cell> cells = new ArrayList<>();
            for (int[] rc : owned[i])
                cells.add(new ChaosRegion.Cell(rc[0], rc[1]));
            result.add(new ChaosRegion(i, cells, ChaosRegion.paletteColor(i)));
        }
        return result;
    }

    /** BFS to find nearest region that still needs more cells. */
    private int nearestHungry(int sr, int sc, int[][] regionOf,
                              List<int[]>[] owned, int[][] dirs) {
        boolean[][] visited = new boolean[SIZE][SIZE];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{sr, sc});
        visited[sr][sc] = true;
        while (!q.isEmpty()) {
            int[] cur = q.poll();
            int id = regionOf[cur[0]][cur[1]];
            if (id >= 0 && owned[id].size() < SIZE) return id;
            for (int[] d : dirs) {
                int nr = cur[0]+d[0], nc = cur[1]+d[1];
                if (inGrid(nr, nc) && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    q.add(new int[]{nr, nc});
                }
            }
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step 2 — backtracking solver
    // ─────────────────────────────────────────────────────────────────────────

    private int[][] trySolve(List<ChaosRegion> regions) {
        int[][] regionOf = new int[SIZE][SIZE];
        for (ChaosRegion region : regions)
            for (ChaosRegion.Cell cell : region.getCells())
                regionOf[cell.row()][cell.col()] = region.getRegionIndex();

        int[][] board = new int[SIZE][SIZE];
        return backtrack(board, regions, regionOf, 0) ? board : null;
    }

    private boolean backtrack(int[][] board, List<ChaosRegion> regions,
                              int[][] regionOf, int pos) {
        if (pos == SIZE * SIZE) return true;
        int r = pos / SIZE, c = pos % SIZE;
        for (int num : shuffled1to9()) {
            if (isValid(board, regions, regionOf, r, c, num)) {
                board[r][c] = num;
                if (backtrack(board, regions, regionOf, pos + 1)) return true;
                board[r][c] = 0;
            }
        }
        return false;
    }

    private boolean isValid(int[][] board, List<ChaosRegion> regions,
                            int[][] regionOf, int row, int col, int val) {
        for (int i = 0; i < SIZE; i++) {
            if (i != col && board[row][i] == val) return false;
            if (i != row && board[i][col] == val) return false;
        }
        int br = (row/3)*3, bc = (col/3)*3;
        for (int r = br; r < br+3; r++)
            for (int c = bc; c < bc+3; c++)
                if ((r!=row || c!=col) && board[r][c]==val) return false;
        return !regions.get(regionOf[row][col]).wouldConflict(board, row, col, val);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Step 3 — hole digging
    // ─────────────────────────────────────────────────────────────────────────

    private int[][] digHoles(int[][] solution, Difficulty difficulty) {
        int clues = switch (difficulty) {
            case EASY   -> 36;
            case MEDIUM -> 30;
            case HARD   -> 25;
        };

        int[][] puzzle = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) puzzle[r] = solution[r].clone();

        List<int[]> cells = new ArrayList<>();
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                cells.add(new int[]{r, c});
        Collections.shuffle(cells, rng);

        int filled = SIZE * SIZE;
        for (int[] cell : cells) {
            if (filled <= clues) break;
            puzzle[cell[0]][cell[1]] = 0;
            filled--;
        }
        return puzzle;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────

    private boolean inGrid(int r, int c) {
        return r >= 0 && r < SIZE && c >= 0 && c < SIZE;
    }

    private List<Integer> shuffledRange(int n) {
        List<Integer> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(i);
        Collections.shuffle(list, rng);
        return list;
    }

    private List<Integer> shuffled1to9() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1,2,3,4,5,6,7,8,9));
        Collections.shuffle(list, rng);
        return list;
    }
}