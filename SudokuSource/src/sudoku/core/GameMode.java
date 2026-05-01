package sudoku.core;

/**
 * Enumerates all supported game modes.
 */
public enum GameMode {
    CLASSIC("Classic"),
    TIMED  ("Timed"),
    KILLER ("Killer");

    private final String displayName;

    GameMode(String displayName) { this.displayName = displayName; }

    @Override
    public String toString() { return displayName; }
}