package sudoku;

import sudoku.ui.MainMenuPanel;
import sudoku.settings.AppSettings;
import sudoku.utils.ThemeManager;
import sudoku.audio.MusicPlayer;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static JFrame frame;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        AppSettings.load();

        if (AppSettings.getInstance().isMusicEnabled()) {
            MusicPlayer.playLoop("/resources/music/background.wav");
        }

        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Sudoku");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setUndecorated(false);
            frame.setResizable(true);

            int scale = AppSettings.getInstance().getScale();
            int baseW = 900, baseH = 700;
            int w = baseW * scale / 100;
            int h = baseH * scale / 100;
            frame.setSize(w, h);
            frame.setMinimumSize(new Dimension(700, 550));
            frame.setLocationRelativeTo(null);

            ThemeManager.applyTheme(AppSettings.getInstance().getTheme());

            showMenu();

            frame.setVisible(true);
        });
    }

    public static void showMenu() {
        frame.setContentPane(new MainMenuPanel());
        frame.revalidate();
        frame.repaint();
    }

    public static void showPanel(JPanel panel) {
        frame.setContentPane(panel);
        frame.revalidate();
        frame.repaint();
    }
}
