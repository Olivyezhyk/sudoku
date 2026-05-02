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
    private JTextField nameField;
    private JTextField idField;

    public SettingsPanel(Runnable onBack) {
        this.onBack = onBack;
        setLayout(new GridBagLayout());
        buildUI();
    }

    public SettingsPanel() {
        this(Main::showMenu);
    }

    private ImageIcon loadIcon(String name, int size) {
        String path = "/icons/" + name;
        java.net.URL url = getClass().getResource(path);
        if (url == null) return null;
        ImageIcon icon = new ImageIcon(url);
        return new ImageIcon(icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
    }

    private void buildUI() {
        ThemeManager.Theme t = ThemeManager.get();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 40, 6, 40);

        JLabel title = makeLabel("Settings", 28, Font.BOLD, t.text());
        title.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 40, 20, 40);
        add(title, gbc);

        AppSettings s = AppSettings.getInstance();

        // ── Sound Effects toggle + volume ─────────────────────────────────────
        gbc.gridy = 1;
        gbc.insets = new Insets(6, 40, 2, 40);
        add(makeRow("🔊  Sound Effects",
                makeToggle(s.isSoundEnabled(), val -> s.setSoundEnabled(val))), gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 40, 8, 40);
        add(makeVolumeRow("Sound Volume", s.getSoundVolume(),
                val -> s.setSoundVolume(val)), gbc);

        // ── Background Music toggle + volume ──────────────────────────────────
        gbc.gridy = 3;
        gbc.insets = new Insets(6, 40, 2, 40);
        add(makeRow("🎵  Background Music",
                makeToggle(s.isMusicEnabled(), val -> {
                    s.setMusicEnabled(val);
                    if (val) {
                        MusicPlayer.stop();
                        MusicPlayer.playLoop("/resources/music/background.wav");
                    } else {
                        MusicPlayer.stop();
                    }
                })), gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 40, 8, 40);
        add(makeVolumeRow("Music Volume", s.getMusicVolume(),
                val -> s.setMusicVolume(val)), gbc);

        // ── Separator ─────────────────────────────────────────────────────────
        gbc.gridy = 5;
        gbc.insets = new Insets(8, 40, 8, 40);
        add(makeSeparator(), gbc);

        // ── Theme ─────────────────────────────────────────────────────────────
        gbc.gridy = 6;
        gbc.insets = new Insets(6, 40, 4, 40);
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
        gbc.gridy = 7;
        gbc.insets = new Insets(4, 40, 16, 40);
        add(themeRow, gbc);

        // ── Separator ─────────────────────────────────────────────────────────
        gbc.gridy = 8;
        gbc.insets = new Insets(8, 40, 8, 40);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(makeSeparator(), gbc);

        // ── Player Info ───────────────────────────────────────────────────────
        gbc.gridy = 9;
        gbc.insets = new Insets(6, 40, 4, 40);
        add(makeLabel("👤  Player Info", 14, Font.BOLD, t.text()), gbc);

        // ── Name field ────────────────────────────────────────────────────────
        nameField = makeStyledField(s.getUserName(), 20);
        gbc.gridy = 10;
        gbc.insets = new Insets(4, 40, 4, 40);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(makeLabeledRow("Name:", nameField), gbc);

        // ── ID field ──────────────────────────────────────────────────────────
        idField = makeStyledField(s.getUserId(), 20);
        gbc.gridy = 11;
        gbc.insets = new Insets(4, 40, 12, 40);
        add(makeLabeledRow("ID:", idField), gbc);

        // ── Separator ─────────────────────────────────────────────────────────
        gbc.gridy = 12;
        gbc.insets = new Insets(4, 40, 12, 40);
        add(makeSeparator(), gbc);

        // ── Save + Back buttons ───────────────────────────────────────────────
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setOpaque(false);

        final ImageIcon saveIcon = loadIcon("save.png", 20);
        StyledButton saveBtn = new StyledButton(saveIcon != null ? "  Save" : "💾  Save") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme th = ThemeManager.get();
                GradientPaint gp = new GradientPaint(0, 0, th.accent(), getWidth(), getHeight(), th.accentDark());
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                int iconW  = saveIcon != null ? saveIcon.getIconWidth() : 0;
                int textW  = g2.getFontMetrics(getFont()).stringWidth(getText());
                int totalW = iconW + (iconW > 0 ? 8 : 0) + textW;
                int startX = (getWidth() - totalW) / 2;
                int centerY = getHeight() / 2;
                if (saveIcon != null)
                    g2.drawImage(saveIcon.getImage(), startX,
                            centerY - saveIcon.getIconHeight() / 2,
                            saveIcon.getIconWidth(), saveIcon.getIconHeight(), null);
                FontMetrics fm = g2.getFontMetrics(getFont());
                g2.setColor(th.buttonText());
                g2.setFont(getFont());
                g2.drawString(getText(), startX + iconW + (iconW > 0 ? 8 : 0),
                        centerY + (fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        saveBtn.setPreferredSize(new Dimension(160, 44));
        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String id   = idField.getText().trim();

            if (name.isEmpty() || id.isEmpty()) {
                JOptionPane.showMessageDialog(
                        SettingsPanel.this,
                        "Please fill in both Name and ID fields.",
                        "Missing Info",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            s.setUserName(name);
            s.setUserId(id);
            s.save();

            JOptionPane.showMessageDialog(
                    SettingsPanel.this,
                    "Settings saved!  ✅",
                    "Saved",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        final ImageIcon backIcon = loadIcon("back.png", 20);
        StyledButton back = new StyledButton(backIcon != null ? "  Back" : "← Back") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme th = ThemeManager.get();
                g2.setColor(new Color(th.accent().getRed(), th.accent().getGreen(),
                        th.accent().getBlue(), 50));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(th.accent());
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                int iconW  = backIcon != null ? backIcon.getIconWidth() : 0;
                int textW  = g2.getFontMetrics(getFont()).stringWidth(getText());
                int totalW = iconW + (iconW > 0 ? 8 : 0) + textW;
                int startX = (getWidth() - totalW) / 2;
                int centerY = getHeight() / 2;
                if (backIcon != null)
                    g2.drawImage(backIcon.getImage(), startX,
                            centerY - backIcon.getIconHeight() / 2,
                            backIcon.getIconWidth(), backIcon.getIconHeight(), null);
                FontMetrics fm = g2.getFontMetrics(getFont());
                g2.setColor(th.textMuted());
                g2.setFont(getFont());
                g2.drawString(getText(), startX + iconW + (iconW > 0 ? 8 : 0),
                        centerY + (fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        back.setPreferredSize(new Dimension(160, 44));
        back.addActionListener(e -> onBack.run());

        btnRow.add(saveBtn);
        btnRow.add(back);

        gbc.gridy = 13;
        gbc.insets = new Insets(0, 40, 0, 40);
        gbc.fill   = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        add(btnRow, gbc);
    }

    // ── Styled text field ─────────────────────────────────────────────────────

    private JTextField makeStyledField(String initial, int maxChars) {
        ThemeManager.Theme t = ThemeManager.get();

        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme th = ThemeManager.get();
                g2.setColor(new Color(th.cellBg().getRed(),
                        th.cellBg().getGreen(),
                        th.cellBg().getBlue(), 220));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(th.gridBold());
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setForeground(t.text());
        field.setCaretColor(t.accentLight());
        field.setPreferredSize(new Dimension(0, 32));

        field.setDocument(new javax.swing.text.PlainDocument() {
            @Override
            public void insertString(int offs, String str,
                                     javax.swing.text.AttributeSet a)
                    throws javax.swing.text.BadLocationException {
                if (str == null) return;
                if ((getLength() + str.length()) <= maxChars)
                    super.insertString(offs, str, a);
            }
        });

        field.setText(initial);

        return field;
    }

    private JPanel makeLabeledRow(String labelText, JTextField field) {
        ThemeManager.Theme t = ThemeManager.get();
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lbl.setForeground(t.text());
        lbl.setPreferredSize(new Dimension(50, 32));

        row.add(lbl,   BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
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
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Rectangle r  = trackRect;
                int cy = r.y + r.height / 2;
                int h  = 4;
                g2.setColor(new Color(t.gridLine().getRed(), t.gridLine().getGreen(),
                        t.gridLine().getBlue(), 120));
                g2.fillRoundRect(r.x, cy - h / 2, r.width, h, h, h);
                int filled = thumbRect.x + thumbRect.width / 2 - r.x;
                g2.setColor(t.accent());
                g2.fillRoundRect(r.x, cy - h / 2, filled, h, h, h);
                g2.dispose();
            }

            @Override
            public void paintThumb(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
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

            @Override
            public void paintFocus(Graphics g) {}
        });
        slider.setPreferredSize(new Dimension(0, 28));
        return slider;
    }

    // ── Misc helpers ──────────────────────────────────────────────────────────

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
        p.add(lbl,  BorderLayout.WEST);
        p.add(comp, BorderLayout.EAST);
        return p;
    }

    private JToggleButton makeToggle(boolean initial,
                                     java.util.function.Consumer<Boolean> onChange) {
        JToggleButton btn = new JToggleButton(initial ? "ON" : "OFF") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme th = ThemeManager.get();
                boolean on = isSelected();
                if (on) g2.setPaint(new GradientPaint(0, 0, th.accent(),
                        getWidth(), 0, th.accentDark()));
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
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme th = ThemeManager.get();
                boolean sel = isSelected();
                if (sel) g2.setPaint(new GradientPaint(0, 0, th.accent(),
                        getWidth(), getHeight(), th.accentDark()));
                else     g2.setColor(new Color(th.accent().getRed(),
                        th.accent().getGreen(),
                        th.accent().getBlue(), 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(th.accent());
                g2.setStroke(new BasicStroke(sel ? 2 : 1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
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
                g.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
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
        g2.setPaint(new GradientPaint(0, 0, t.bg(),
                getWidth(), getHeight(), t.bgSecondary()));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }
}