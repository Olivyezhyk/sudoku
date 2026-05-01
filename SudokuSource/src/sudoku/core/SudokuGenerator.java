package sudoku.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SudokuGenerator {
    private static final int SIZE = 9;
    private static final int BOX = 3;
    private final Random random = new Random();

    public int[][] generateFull() {
        int[][] board = new int[SIZE][SIZE];
        fill(board, 0, 0);
        return board;
    }

    private boolean fill(int[][] board, int row, int col) {
        if (row == SIZE) return true;
        int nextRow = (col == SIZE - 1) ? row + 1 : row;
        int nextCol = (col == SIZE - 1) ? 0 : col + 1;

        List<Integer> nums = new ArrayList<>();
        for (int i = 1; i <= SIZE; i++) nums.add(i);
        Collections.shuffle(nums, random);

        for (int num : nums) {
            if (isValid(board, row, col, num)) {
                board[row][col] = num;
                if (fill(board, nextRow, nextCol)) return true;
                board[row][col] = 0;
            }
        }
        return false;
    }

    private boolean isValid(int[][] board, int row, int col, int num) {
        for (int c = 0; c < SIZE; c++)
            if (board[row][c] == num) return false;
        for (int r = 0; r < SIZE; r++)
            if (board[r][col] == num) return false;
        int br = (row / BOX) * BOX, bc = (col / BOX) * BOX;
        for (int r = br; r < br + BOX; r++)
            for (int c = bc; c < bc + BOX; c++)
                if (board[r][c] == num) return false;
        return true;
    }

    public int[][] generatePuzzle(Difficulty difficulty) {
        int[][] full = generateFull();
        int[][] puzzle = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++)
            System.arraycopy(full[r], 0, puzzle[r], 0, SIZE);

        int cellsToRemove = switch (difficulty) {
            case EASY   -> 36;
            case MEDIUM -> 46;
            case HARD   -> 54;
        };

        List<int[]> cells = new ArrayList<>();
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                cells.add(new int[]{r, c});
        Collections.shuffle(cells, random);

        int removed = 0;
        for (int[] cell : cells) {
            if (removed >= cellsToRemove) break;
            int backup = puzzle[cell[0]][cell[1]];
            puzzle[cell[0]][cell[1]] = 0;
            if (hasUniqueSolution(puzzle)) {
                removed++;
            } else {
                puzzle[cell[0]][cell[1]] = backup;
            }
        }
        return puzzle;
    }

    private boolean hasUniqueSolution(int[][] board) {
        int[][] copy = copyBoard(board);
        int[] count = {0};
        solveCount(copy, count);
        return count[0] == 1;
    }

    private void solveCount(int[][] board, int[] count) {
        if (count[0] > 1) return;
        int[] empty = findEmpty(board);
        if (empty == null) { count[0]++; return; }
        int r = empty[0], c = empty[1];
        for (int num = 1; num <= SIZE; num++) {
            if (isValid(board, r, c, num)) {
                board[r][c] = num;
                solveCount(board, count);
                board[r][c] = 0;
            }
        }
    }

    private int[] findEmpty(int[][] board) {
        for (int r = 0; r < SIZE; r++)
            for (int c = 0; c < SIZE; c++)
                if (board[r][c] == 0) return new int[]{r, c};
        return null;
    }

    private int[][] copyBoard(int[][] board) {
        int[][] copy = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++)
            System.arraycopy(board[r], 0, copy[r], 0, SIZE);
        return copy;
    }

    public int[][] solve(int[][] board) {
        int[][] copy = copyBoard(board);
        solveSingle(copy);
        return copy;
    }

    private boolean solveSingle(int[][] board) {
        int[] empty = findEmpty(board);
        if (empty == null) return true;
        int r = empty[0], c = empty[1];
        for (int num = 1; num <= SIZE; num++) {
            if (isValid(board, r, c, num)) {
                board[r][c] = num;
                if (solveSingle(board)) return true;
                board[r][c] = 0;
            }
        }
        return false;
    }
}
