package sudoku.ui;

import sudoku.core.ScoreManager;
import sudoku.ui.RecordsPanel;
import sudoku.Main;
import sudoku.core.Difficulty;
import sudoku.core.GameMode;
import sudoku.utils.ThemeManager;

import javax.swing.*;
import java.awt.*;

/**
 * Main menu panel.
 *
 * Adds a "Game Mode" selector row (Classic / Chaos / Killer) between the
 * title and the difficulty selector. All other visuals are unchanged.
 */
public class MainMenuPanel extends JPanel {

    private Difficulty selectedDiff = Difficulty.EASY;
    private GameMode   selectedMode = GameMode.CLASSIC;

    private JPanel diffPanel;
    private JPanel modePanel;
    private Timer  animTimer;
    private float  starPhase = 0f;

    private ImageIcon loadIcon(String name, int size) {
        String path = "/icons/" + name;
        java.net.URL url = getClass().getResource(path);
        if (url == null) { System.out.println("❌ NOT FOUND: " + path); return null; }
        ImageIcon icon = new ImageIcon(url);
        return new ImageIcon(icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
    }

    public MainMenuPanel() {
        setLayout(new GridBagLayout());
        setOpaque(true);
        animTimer = new Timer(50, e -> { starPhase += 0.02f; repaint(); });
        animTimer.start();
        buildUI();
    }

    private void buildUI() {
        ThemeManager.Theme t = ThemeManager.get();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill   = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx  = 0;

        // ── Title ─────────────────────────────────────────────────────────────
        JLabel title = new JLabel("SUDOKU") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, ThemeManager.get().accentLight(),
                        getWidth(), getHeight(), new Color(255, 160, 220));
                g2.setPaint(gp);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, fm.getAscent());
                g2.dispose();
            }
        };
        title.setFont(new Font("SansSerif", Font.BOLD, 52));
        title.setForeground(t.accentLight());
        title.setPreferredSize(new Dimension(400, 70));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0; gbc.insets = new Insets(0, 0, 4, 0);
        add(title, gbc);

        // ── Subtitle ──────────────────────────────────────────────────────────
        JLabel sub = new JLabel("Classic · Timed · Killer Sudoku");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 14));
        sub.setForeground(t.textMuted());
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 1; gbc.insets = new Insets(0, 0, 16, 0);
        add(sub, gbc);

        // ── Game Mode selector ────────────────────────────────────────────────
        JLabel modeTitle = sectionLabel("Game Mode", t);
        gbc.gridy = 2; gbc.insets = new Insets(0, 0, 6, 0);
        add(modeTitle, gbc);

        modePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        modePanel.setOpaque(false);

        GameMode[] visibleModes = { GameMode.CLASSIC, GameMode.TIMED, GameMode.KILLER };
        String[]   modeLabels   = { "Classic", "Timed", "Killer" };

        for (int mi = 0; mi < visibleModes.length; mi++) {
            GameMode mode  = visibleModes[mi];
            String   label = modeLabels[mi];
            StyledButton btn = buildToggleBtn(label, () -> mode == selectedMode);
            btn.addActionListener(e -> { selectedMode = mode; modePanel.repaint(); diffPanel.repaint(); });
            modePanel.add(btn);
        }
        gbc.gridy = 3; gbc.insets = new Insets(0, 0, 4, 0);
        add(modePanel, gbc);

        // Hint line: shows time limit for the selected difficulty when TIMED is active
        JLabel timeLimitHint = new JLabel(" ");
        timeLimitHint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        timeLimitHint.setForeground(t.textMuted());
        timeLimitHint.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 4; gbc.insets = new Insets(0, 0, 14, 0);
        add(timeLimitHint, gbc);

        // ── Difficulty selector ───────────────────────────────────────────────
        JLabel diffTitle = sectionLabel("Difficulty", t);
        gbc.gridy = 5; gbc.insets = new Insets(0, 0, 6, 0);
        add(diffTitle, gbc);

        diffPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        diffPanel.setOpaque(false);

        for (Difficulty d : Difficulty.values()) {
            StyledButton btn = buildToggleBtn(d.toString(), () -> d == selectedDiff);
            btn.addActionListener(e -> {
                selectedDiff = d;
                diffPanel.repaint();
                updateTimeLimitHint(timeLimitHint);
            });
            diffPanel.add(btn);
        }
        gbc.gridy = 6; gbc.insets = new Insets(0, 0, 20, 0);
        add(diffPanel, gbc);

        // Update hint when mode changes too
        for (java.awt.Component comp : modePanel.getComponents()) {
            comp.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    updateTimeLimitHint(timeLimitHint);
                }
            });
        }
        updateTimeLimitHint(timeLimitHint); // initial state

        // ── Play button ───────────────────────────────────────────────────────
        ImageIcon playIcon = loadIcon("play.png", 26);
        StyledButton play = new StyledButton("  PLAY") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme th = ThemeManager.get();
                GradientPaint gp = new GradientPaint(0, 0, th.accent(), getWidth(), getHeight(), th.accentDark());
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                int textW  = g2.getFontMetrics(getFont()).stringWidth(getText());
                int iconW  = playIcon != null ? playIcon.getIconWidth() : 0;
                int totalW = iconW + 8 + textW;
                int startX = (getWidth() - totalW) / 2;
                int centerY = getHeight() / 2;
                if (playIcon != null) {
                    g2.drawImage(playIcon.getImage(), startX,
                            centerY - playIcon.getIconHeight() / 2,
                            playIcon.getIconWidth(), playIcon.getIconHeight(), null);
                }
                FontMetrics fm = g2.getFontMetrics(getFont());
                g2.setColor(th.buttonText());
                g2.setFont(getFont());
                g2.drawString(getText(), startX + iconW + 8,
                        centerY + (fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        play.setPreferredSize(new Dimension(260, 64));
        play.setFont(new Font("SansSerif", Font.BOLD, 22));
        play.addActionListener(e -> Main.showPanel(new GamePanel(selectedDiff, selectedMode)));
        gbc.gridy = 7; gbc.insets = new Insets(4, 0, 12, 0);
        add(play, gbc);

        // ── Settings button ───────────────────────────────────────────────────
        ImageIcon settingsIcon = loadIcon("settings.png", 22);
        StyledButton settings = outlineBtn("  Settings", settingsIcon, t);
        settings.setPreferredSize(new Dimension(260, 56));
        settings.setFont(new Font("SansSerif", Font.BOLD, 18));
        settings.addActionListener(e -> Main.showPanel(new SettingsPanel()));
        gbc.gridy = 8; gbc.insets = new Insets(4, 0, 10, 0);
        add(settings, gbc);

        // ── Records button ────────────────────────────────────────────────────────
        ImageIcon recordsIcon = loadIcon("records.png", 22);   // або будь-яка наявна іконка
        StyledButton records  = outlineBtn("  Records", recordsIcon, t);
        records.setPreferredSize(new Dimension(260, 56));
        records.setFont(new Font("SansSerif", Font.BOLD, 18));
        records.addActionListener(e ->
                Main.showPanel(new RecordsPanel(new ScoreManager(), Main::showMenu)));
        gbc.gridy = 9; gbc.insets = new Insets(4, 0, 10, 0);
        add(records, gbc);

        // ── Exit button ───────────────────────────────────────────────────────
        ImageIcon exitIcon = loadIcon("exit.png", 20);
        StyledButton exit = iconOnlyBtn("  Exit", exitIcon);
        exit.setPreferredSize(new Dimension(260, 48));
        exit.setFont(new Font("SansSerif", Font.BOLD, 16));
        exit.addActionListener(e -> System.exit(0));
        gbc.gridy = 10; gbc.insets = new Insets(4, 0, 0, 0);
        add(exit, gbc);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private JLabel sectionLabel(String text, ThemeManager.Theme t) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(t.textMuted());
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    /** Updates the time-limit hint below the mode buttons. Visible only in TIMED mode. */
    private void updateTimeLimitHint(JLabel hint) {
        if (selectedMode == GameMode.TIMED) {
            int minutes = switch (selectedDiff) {
                case EASY   -> 15;
                case MEDIUM -> 10;
                case HARD   ->  7;
            };
            hint.setText("⏱  " + minutes + " minutes to solve");
        } else {
            hint.setText(" ");
        }
    }

    /**
     * Pill-shaped toggle button whose selected state is determined by a supplier
     * so repaints pick up the latest selection automatically.
     */
    private StyledButton buildToggleBtn(String label, java.util.function.BooleanSupplier isSelected) {
        StyledButton btn = new StyledButton(label, true) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme th = ThemeManager.get();
                boolean sel = isSelected.getAsBoolean();
                int arc = getHeight();
                if (sel) {
                    g2.setPaint(new GradientPaint(0, 0, th.accent(), getWidth(), 0, th.accentDark()));
                } else {
                    g2.setColor(new Color(th.accent().getRed(), th.accent().getGreen(),
                            th.accent().getBlue(), 60));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.setColor(th.accent());
                g2.setStroke(new BasicStroke(sel ? 2 : 1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                FontMetrics fm = g2.getFontMetrics(getFont());
                g2.setColor(sel ? th.buttonText() : th.textMuted());
                g2.setFont(getFont());
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(100, 40));
        return btn;
    }

    /** Semi-transparent outlined button (Settings style). */
    private StyledButton outlineBtn(String text, ImageIcon icon, ThemeManager.Theme t) {
        return new StyledButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme th = ThemeManager.get();
                g2.setColor(new Color(th.accent().getRed(), th.accent().getGreen(),
                        th.accent().getBlue(), 50));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(th.accent());
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                int iconW = icon != null ? icon.getIconWidth() : 0;
                int textW = g2.getFontMetrics(getFont()).stringWidth(getText());
                int totalW = iconW + 8 + textW, startX = (getWidth() - totalW) / 2;
                int centerY = getHeight() / 2;
                if (icon != null)
                    g2.drawImage(icon.getImage(), startX, centerY - icon.getIconHeight() / 2,
                            iconW, icon.getIconHeight(), null);
                FontMetrics fm = g2.getFontMetrics(getFont());
                g2.setColor(th.textMuted());
                g2.setFont(getFont());
                g2.drawString(getText(), startX + iconW + 8,
                        centerY + (fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
    }

    /** Icon-only plain button (Exit style). */
    private StyledButton iconOnlyBtn(String text, ImageIcon icon) {
        return new StyledButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme th = ThemeManager.get();
                int iconW = icon != null ? icon.getIconWidth() : 0;
                int textW = g2.getFontMetrics(getFont()).stringWidth(getText());
                int totalW = iconW + 8 + textW, startX = (getWidth() - totalW) / 2;
                int centerY = getHeight() / 2;
                if (icon != null)
                    g2.drawImage(icon.getImage(), startX, centerY - icon.getIconHeight() / 2,
                            iconW, icon.getIconHeight(), null);
                FontMetrics fm = g2.getFontMetrics(getFont());
                g2.setColor(th.textMuted());
                g2.setFont(getFont());
                g2.drawString(getText(), startX + iconW + 8,
                        centerY + (fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
    }

    // ── Background painting ───────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        ThemeManager.Theme t = ThemeManager.get();
        g2.setPaint(new GradientPaint(0, 0, t.bg(), getWidth(), getHeight(), t.bgSecondary()));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawDecoCircles(g2, t);
        g2.dispose();
    }

    private void drawDecoCircles(Graphics2D g2, ThemeManager.Theme t) {
        float[][] circles = {{0.08f,0.15f,80,0.0f},{0.92f,0.1f,60,1.5f},
                {0.05f,0.85f,100,0.8f},{0.88f,0.82f,70,2.3f},{0.5f,0.05f,45,1.1f}};
        for (float[] c : circles) {
            float x = c[0] * getWidth(), y = c[1] * getHeight();
            float r = c[2] + (float)(Math.sin(starPhase + c[3]) * 8);
            g2.setColor(new Color(t.accent().getRed(), t.accent().getGreen(), t.accent().getBlue(), 35));
            g2.fillOval((int)(x-r),(int)(y-r),(int)(r*2),(int)(r*2));
            g2.setColor(new Color(t.accentLight().getRed(),t.accentLight().getGreen(),t.accentLight().getBlue(),20));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval((int)(x-r*1.3f),(int)(y-r*1.3f),(int)(r*2.6f),(int)(r*2.6f));
        }
    }

    @Override public void removeNotify() { super.removeNotify(); animTimer.stop(); }
}