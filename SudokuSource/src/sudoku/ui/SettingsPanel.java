package sudoku.ui;

import sudoku.Main;
import sudoku.settings.AppSettings;
import sudoku.utils.ThemeManager;
import sudoku.audio.MusicPlayer;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;

public class SettingsPanel extends JPanel {

    private final Runnable onBack;

    public SettingsPanel(Runnable onBack) {
        this.onBack = onBack;
        setLayout(new GridBagLayout());
        buildUI();
    }

    public SettingsPanel() {
        this(Main::showMenu);
    }

    private void buildUI() {
        ThemeManager.Theme t = ThemeManager.get();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 40, 6, 40);

        JLabel title = makeLabel("Settings", 28, Font.BOLD, t.text());
        title.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0; gbc.insets = new Insets(0, 40, 20, 40);
        add(title, gbc);

        AppSettings s = AppSettings.getInstance();

        // ── Sound Effects toggle + volume ─────────────────────────────────────
        gbc.gridy = 1; gbc.insets = new Insets(6, 40, 2, 40);
        add(makeRow("🔊  Sound Effects", makeToggle(s.isSoundEnabled(), val -> s.setSoundEnabled(val))), gbc);

        gbc.gridy = 2; gbc.insets = new Insets(0, 40, 8, 40);
        add(makeVolumeRow("Sound Volume", s.getSoundVolume(), val -> s.setSoundVolume(val)), gbc);

        // ── Background Music toggle + volume ──────────────────────────────────
        gbc.gridy = 3; gbc.insets = new Insets(6, 40, 2, 40);
        add(makeRow("🎵  Background Music",
                makeToggle(s.isMusicEnabled(), val -> {
                    s.setMusicEnabled(val);
                    if (val) {
                        MusicPlayer.stop();
                        MusicPlayer.playLoop("/resources/music/background.wav");
                    } else {
                        MusicPlayer.stop();
                    }
                })
        ), gbc);

        gbc.gridy = 4; gbc.insets = new Insets(0, 40, 8, 40);
        add(makeVolumeRow("Music Volume", s.getMusicVolume(), val -> s.setMusicVolume(val)), gbc);

        // ── Separator ─────────────────────────────────────────────────────────
        gbc.gridy = 5; gbc.insets = new Insets(8, 40, 8, 40);
        add(makeSeparator(), gbc);

        // ── Theme ─────────────────────────────────────────────────────────────
        gbc.gridy = 6; gbc.insets = new Insets(6, 40, 4, 40);
        add(makeLabel("🎨  Theme", 14, Font.BOLD, t.text()), gbc);

        JPanel themeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        themeRow.setOpaque(false);
        String[] themeNames = {"Default", "Light", "Dark"};
        String[] themeKeys  = {"default", "light", "dark"};
        ButtonGroup bg = new ButtonGroup();
        for (int i = 0; i < themeNames.length; i++) {
            final String key = themeKeys[i];
            JToggleButton btn = makeThemeBtn(themeNames[i], s.getTheme().equals(key));
            bg.add(btn);
            btn.addActionListener(e -> {
                s.setTheme(key);
                ThemeManager.applyTheme(key);
                Main.showPanel(new SettingsPanel(onBack));
            });
            themeRow.add(btn);
        }
        gbc.gridy = 7; gbc.insets = new Insets(4, 40, 16, 40);
        add(themeRow, gbc);

        // ── Back button ───────────────────────────────────────────────────────
        StyledButton back = new StyledButton("← Back");
        back.setPreferredSize(new Dimension(200, 44));
        back.addActionListener(e -> onBack.run());
        gbc.gridy = 8; gbc.insets = new Insets(4, 40, 0, 40);
        gbc.fill   = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        add(back, gbc);
    }

    // ── Volume slider row ─────────────────────────────────────────────────────

    private JPanel makeVolumeRow(String label, int initial,
                                 java.util.function.Consumer<Integer> onChange) {
        ThemeManager.Theme t = ThemeManager.get();
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setForeground(t.textMuted());
        lbl.setPreferredSize(new Dimension(110, 24));

        JSlider slider = makeSlider(initial);

        JLabel valLbl = new JLabel(initial + "%");
        valLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        valLbl.setForeground(t.accentLight());
        valLbl.setPreferredSize(new Dimension(40, 24));
        valLbl.setHorizontalAlignment(SwingConstants.RIGHT);

        slider.addChangeListener(e -> {
            int v = slider.getValue();
            valLbl.setText(v + "%");
            if (!slider.getValueIsAdjusting()) onChange.accept(v);
        });

        row.add(lbl,    BorderLayout.WEST);
        row.add(slider, BorderLayout.CENTER);
        row.add(valLbl, BorderLayout.EAST);
        return row;
    }

    private JSlider makeSlider(int initial) {
        ThemeManager.Theme t = ThemeManager.get();
        JSlider slider = new JSlider(0, 100, initial);
        slider.setOpaque(false);
        slider.setFocusable(false);
        slider.setUI(new BasicSliderUI(slider) {

            @Override
            public void paintTrack(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Rectangle r = trackRect;
                int cy = r.y + r.height / 2;
                int h  = 4;
                // Background track
                g2.setColor(new Color(t.gridLine().getRed(), t.gridLine().getGreen(),
                        t.gridLine().getBlue(), 120));
                g2.fillRoundRect(r.x, cy - h/2, r.width, h, h, h);
                // Filled portion
                int filled = thumbRect.x + thumbRect.width / 2 - r.x;
                g2.setColor(t.accent());
                g2.fillRoundRect(r.x, cy - h/2, filled, h, h, h);
                g2.dispose();
            }

            @Override
            public void paintThumb(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = thumbRect.x + thumbRect.width  / 2;
                int cy = thumbRect.y + thumbRect.height / 2;
                int r  = 8;
                g2.setColor(t.accent());
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                g2.setColor(t.buttonText());
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                g2.dispose();
            }

            @Override public void paintFocus(Graphics g) {}
        });
        slider.setPreferredSize(new Dimension(0, 28));
        return slider;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JLabel makeLabel(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(color);
        return l;
    }

    private JPanel makeRow(String label, Component comp) {
        ThemeManager.Theme t = ThemeManager.get();
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lbl.setForeground(t.text());
        p.add(lbl, BorderLayout.WEST);
        p.add(comp, BorderLayout.EAST);
        return p;
    }

    private JToggleButton makeToggle(boolean initial,
                                     java.util.function.Consumer<Boolean> onChange) {
        JToggleButton btn = new JToggleButton(initial ? "ON" : "OFF") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme th = ThemeManager.get();
                boolean on = isSelected();
                if (on) g2.setPaint(new GradientPaint(0, 0, th.accent(), getWidth(), 0, th.accentDark()));
                else    g2.setColor(new Color(128, 128, 128, 80));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                FontMetrics fm = g2.getFontMetrics(getFont());
                g2.setColor(on ? th.buttonText() : th.textMuted());
                g2.setFont(getFont());
                g2.drawString(getText(),
                        (getWidth()  - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setSelected(initial);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setPreferredSize(new Dimension(64, 30));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            onChange.accept(btn.isSelected());
            btn.setText(btn.isSelected() ? "ON" : "OFF");
            btn.repaint();
        });
        return btn;
    }

    private JToggleButton makeThemeBtn(String label, boolean selected) {
        JToggleButton btn = new JToggleButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme th = ThemeManager.get();
                boolean sel = isSelected();
                if (sel) g2.setPaint(new GradientPaint(0, 0, th.accent(), getWidth(), getHeight(), th.accentDark()));
                else     g2.setColor(new Color(th.accent().getRed(), th.accent().getGreen(), th.accent().getBlue(), 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(th.accent());
                g2.setStroke(new BasicStroke(sel ? 2 : 1));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                FontMetrics fm = g2.getFontMetrics(getFont());
                g2.setColor(sel ? th.buttonText() : th.textMuted());
                g2.setFont(getFont());
                g2.drawString(getText(),
                        (getWidth()  - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setSelected(selected);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setPreferredSize(new Dimension(90, 36));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JSeparator makeSeparator() {
        JSeparator sep = new JSeparator() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(ThemeManager.get().gridLine());
                g.drawLine(0, getHeight()/2, getWidth(), getHeight()/2);
            }
        };
        sep.setPreferredSize(new Dimension(0, 1));
        sep.setOpaque(false);
        return sep;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        ThemeManager.Theme t = ThemeManager.get();
        g2.setPaint(new GradientPaint(0, 0, t.bg(), getWidth(), getHeight(), t.bgSecondary()));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}