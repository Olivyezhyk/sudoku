package sudoku.core;

public enum Difficulty {
    EASY, MEDIUM, HARD;

    @Override
    public String toString() {
        return switch (this) {
            case EASY   -> "Easy";
            case MEDIUM -> "Medium";
            case HARD   -> "Hard";
        };
    }
}
