package sudoku.utils;

import java.awt.*;

public class ThemeManager {
    public record Theme(
        Color bg, Color bgSecondary, Color text, Color textMuted,
        Color accent, Color accentLight, Color accentDark,
        Color cellBg, Color cellFixed, Color cellSelected,
        Color cellHighlight, Color cellError, Color cellSameNum,
        Color gridLine, Color gridBold,
        Color buttonBg, Color buttonText, Color buttonHover
    ) {}

    private static Theme current;

    public static void applyTheme(String name) {
        current = switch (name) {
            case "light" -> light();
            case "dark"  -> dark();
            default      -> defaultTheme();
        };
    }

    public static Theme get() {
        if (current == null) current = defaultTheme();
        return current;
    }

    private static Theme defaultTheme() {
        Color acc = new Color(150, 80, 220);
        Color accL = new Color(200, 140, 255);
        Color accD = new Color(100, 40, 180);
        return new Theme(
            new Color(30, 15, 50),
            new Color(45, 22, 72),
            new Color(245, 235, 255),
            new Color(180, 155, 210),
            acc, accL, accD,
            new Color(50, 28, 82),
            new Color(38, 20, 62),
            new Color(120, 60, 200),
            new Color(80, 40, 130),
            new Color(200, 60, 80),
            new Color(70, 35, 115),
            new Color(80, 50, 120),
            new Color(140, 80, 200),
            new Color(100, 50, 170),
            new Color(245, 235, 255),
            new Color(130, 70, 210)
        );
    }

    private static Theme light() {
        Color acc = new Color(100, 60, 180);
        return new Theme(
            new Color(245, 243, 255),
            new Color(230, 225, 250),
            new Color(30, 20, 60),
            new Color(100, 90, 130),
            acc, new Color(150, 110, 220), new Color(70, 30, 140),
            Color.WHITE,
            new Color(235, 232, 250),
            new Color(200, 185, 255),
            new Color(220, 210, 255),
            new Color(255, 220, 220),
            new Color(235, 228, 255),
            new Color(200, 190, 230),
            new Color(120, 80, 200),
            new Color(100, 60, 180),
            new Color(30, 20, 60),
            new Color(130, 90, 210)
        );
    }

    private static Theme dark() {
        Color acc = new Color(80, 160, 255);
        return new Theme(
            new Color(15, 18, 28),
            new Color(22, 26, 40),
            new Color(220, 230, 255),
            new Color(130, 145, 190),
            acc, new Color(120, 190, 255), new Color(50, 120, 210),
            new Color(28, 33, 50),
            new Color(20, 24, 38),
            new Color(50, 80, 160),
            new Color(35, 55, 110),
            new Color(200, 60, 80),
            new Color(40, 65, 130),
            new Color(45, 55, 85),
            new Color(70, 90, 150),
            new Color(50, 75, 160),
            new Color(220, 230, 255),
            new Color(70, 105, 190)
        );
    }
}
