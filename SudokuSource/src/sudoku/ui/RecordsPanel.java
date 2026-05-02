package sudoku.ui;

import sudoku.Main;
import sudoku.core.*;
import sudoku.utils.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Displays high-score records stored by {@link ScoreManager}.
 *
 * Layout:
 *   • Filter row  — two button groups (mode / difficulty) + "Clear All" button
 *   • Table area  — scrollable list of records (rank, name, id, time, mode, diff)
 *   • Back button
 */
public class RecordsPanel extends JPanel {

    private static final int ROW_H   = 44;
    private static final int COL_PAD = 12;

    private final ScoreManager scoreManager;
    private final Runnable     onBack;

    // Filter state
    private GameMode   filterMode = null;
    private Difficulty filterDiff = null;

    // UI pieces that are rebuilt when filter changes
    private JPanel tablePanel;
    private JScrollPane scrollPane;

    // ── Constructor ───────────────────────────────────────────────────────────

    public RecordsPanel(ScoreManager scoreManager, Runnable onBack) {
        this.scoreManager = scoreManager;
        this.onBack       = onBack;
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        build();
    }

    // ── Icon loader ───────────────────────────────────────────────────────────

    private ImageIcon loadIcon(String name, int size) {
        String path = "/icons/" + name;
        java.net.URL url = getClass().getResource(path);
        if (url == null) return null;
        ImageIcon icon = new ImageIcon(url);
        return new ImageIcon(icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void build() {
        ThemeManager.Theme t = ThemeManager.get();

        // ── Top bar ───────────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

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
        back.setPreferredSize(new Dimension(110, 44));
        back.addActionListener(e -> onBack.run());
        topBar.add(back, BorderLayout.WEST);

        JLabel title = new JLabel("🏆  Leaderboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(t.accentLight());
        title.setHorizontalAlignment(SwingConstants.CENTER);
        topBar.add(title, BorderLayout.CENTER);

        StyledButton clearBtn = new StyledButton("Clear All") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme th = ThemeManager.get();
                g2.setColor(new Color(th.cellError().getRed(),
                        th.cellError().getGreen(),
                        th.cellError().getBlue(), 160));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                FontMetrics fm = g2.getFontMetrics(getFont());
                g2.setColor(th.text());
                g2.setFont(getFont());
                g2.drawString(getText(),
                        (getWidth()  - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        clearBtn.setPreferredSize(new Dimension(110, 44));
        clearBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    RecordsPanel.this,
                    "Clear ALL records?", "Confirm",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                scoreManager.clearAll();
                rebuildTable();
            }
        });
        topBar.add(clearBtn, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // ── Filter row ────────────────────────────────────────────────────────
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        filterRow.setOpaque(false);
        filterRow.setBorder(BorderFactory.createEmptyBorder(0, 20, 4, 20));

        filterRow.add(filterLabel("Mode:", t));
        filterRow.add(filterBtn("All",     () -> filterMode == null,
                () -> { filterMode = null;             rebuildTable(); }));
        for (GameMode m : new GameMode[]{GameMode.CLASSIC, GameMode.TIMED, GameMode.KILLER}) {
            filterRow.add(filterBtn(m.toString(), () -> filterMode == m,
                    () -> { filterMode = m;            rebuildTable(); }));
        }

        filterRow.add(Box.createHorizontalStrut(20));
        filterRow.add(filterLabel("Diff:", t));
        filterRow.add(filterBtn("All",    () -> filterDiff == null,
                () -> { filterDiff = null;             rebuildTable(); }));
        for (Difficulty d : Difficulty.values()) {
            filterRow.add(filterBtn(d.toString(), () -> filterDiff == d,
                    () -> { filterDiff = d;            rebuildTable(); }));
        }

        add(filterRow, BorderLayout.SOUTH);

        // ── Scrollable table ──────────────────────────────────────────────────
        tablePanel = new JPanel();
        tablePanel.setOpaque(false);
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane(tablePanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        scrollPane.getVerticalScrollBar().setUI(
                new javax.swing.plaf.basic.BasicScrollBarUI() {
                    @Override protected void configureScrollBarColors() {
                        thumbColor  = ThemeManager.get().accent();
                        trackColor  = new Color(0,0,0,0);
                    }
                    @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
                    @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
                    private JButton zeroBtn() {
                        JButton b = new JButton();
                        b.setPreferredSize(new Dimension(0, 0));
                        return b;
                    }
                });

        add(scrollPane, BorderLayout.CENTER);

        rebuildTable();
    }

    // ── Table building ────────────────────────────────────────────────────────

    private void rebuildTable() {
        tablePanel.removeAll();

        tablePanel.add(makeHeaderRow());

        List<ScoreRecord> list;
        if (filterMode != null && filterDiff != null) {
            list = scoreManager.getFiltered(filterMode, filterDiff);
        } else if (filterMode != null) {
            list = scoreManager.getAll().stream()
                    .filter(r -> r.getGameMode() == filterMode)
                    .sorted(java.util.Comparator.comparingInt(ScoreRecord::getTimeSeconds))
                    .collect(java.util.stream.Collectors.toList());
        } else if (filterDiff != null) {
            list = scoreManager.getAll().stream()
                    .filter(r -> r.getDifficulty() == filterDiff)
                    .sorted(java.util.Comparator.comparingInt(ScoreRecord::getTimeSeconds))
                    .collect(java.util.stream.Collectors.toList());
        } else {
            list = new java.util.ArrayList<>(scoreManager.getAll());
            list.sort(java.util.Comparator.comparingInt(ScoreRecord::getTimeSeconds));
        }

        if (list.isEmpty()) {
            tablePanel.add(emptyRow());
        } else {
            for (int i = 0; i < list.size(); i++) {
                tablePanel.add(makeRecordRow(i + 1, list.get(i), i % 2 == 0));
            }
        }

        tablePanel.revalidate();
        tablePanel.repaint();
    }

    // ── Row builders ──────────────────────────────────────────────────────────

    private static final int[] COL_W = { 44, 140, 90, 80, 80, 80 };
    private static final String[] HEADERS = { "#", "Name", "ID", "Time", "Mode", "Diff" };

    private JPanel makeHeaderRow() {
        ThemeManager.Theme t = ThemeManager.get();
        JPanel row = rowPanel(true);
        for (int i = 0; i < HEADERS.length; i++) {
            JLabel cell = cell(HEADERS[i], Font.BOLD, 12, t.accentLight());
            cell.setPreferredSize(new Dimension(COL_W[i], ROW_H));
            row.add(cell);
        }
        return row;
    }

    private JPanel makeRecordRow(int rank, ScoreRecord r, boolean even) {
        ThemeManager.Theme t = ThemeManager.get();
        JPanel row = rowPanel(false);

        Color stripeBg = even
                ? new Color(t.cellBg().getRed(), t.cellBg().getGreen(), t.cellBg().getBlue(), 60)
                : new Color(0, 0, 0, 0);
        row.setBackground(stripeBg);
        row.setOpaque(even);

        String rankStr = rank <= 3
                ? new String[]{"🥇", "🥈", "🥉"}[rank - 1]
                : String.valueOf(rank);

        Color rankColor = rank == 1 ? new Color(255, 215, 0)
                : rank == 2 ? new Color(192, 192, 192)
                : rank == 3 ? new Color(205, 127, 50)
                : t.textMuted();

        String timeStr = String.format("%02d:%02d",
                r.getTimeSeconds() / 60, r.getTimeSeconds() % 60);

        String name = truncate(r.getUserName(), 16);
        String id   = truncate(r.getUserId(),   10);

        String[] values = { rankStr, name, id, timeStr,
                r.getGameMode().toString(),
                r.getDifficulty().toString() };
        Color[]  colors = { rankColor, t.text(), t.textMuted(),
                t.accentLight(), t.textMuted(), t.textMuted() };

        for (int i = 0; i < values.length; i++) {
            JLabel c = cell(values[i], i == 0 ? Font.BOLD : Font.PLAIN, 13, colors[i]);
            c.setPreferredSize(new Dimension(COL_W[i], ROW_H));
            row.add(c);
        }
        return row;
    }

    private JPanel emptyRow() {
        ThemeManager.Theme t = ThemeManager.get();
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER));
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(0, 80));
        JLabel lbl = new JLabel("No records yet. Play a game to get started! 🎮");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lbl.setForeground(t.textMuted());
        row.add(lbl);
        return row;
    }

    private JPanel rowPanel(boolean isHeader) {
        ThemeManager.Theme t = ThemeManager.get();
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, COL_PAD, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (isHeader) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(ThemeManager.get().gridBold());
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                    g2.dispose();
                }
            }
        };
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H + (isHeader ? 4 : 0)));
        row.setOpaque(false);
        return row;
    }

    private JLabel cell(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", style, size));
        l.setForeground(color);
        return l;
    }

    // ── Filter button ─────────────────────────────────────────────────────────

    private JButton filterBtn(String label,
                              java.util.function.BooleanSupplier isActive,
                              Runnable onClick) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme th = ThemeManager.get();
                boolean active = isActive.getAsBoolean();
                int arc = getHeight();
                if (active) {
                    g2.setPaint(new GradientPaint(0, 0, th.accent(),
                            getWidth(), 0, th.accentDark()));
                } else {
                    g2.setColor(new Color(th.accent().getRed(),
                            th.accent().getGreen(),
                            th.accent().getBlue(), 40));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.setColor(th.accent());
                g2.setStroke(new BasicStroke(active ? 1.5f : 1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, arc, arc);
                FontMetrics fm = g2.getFontMetrics(getFont());
                g2.setColor(active ? th.buttonText() : th.textMuted());
                g2.setFont(getFont());
                g2.drawString(getText(),
                        (getWidth()  - fm.stringWidth(getText())) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(72, 30));
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> { onClick.run(); repaint(); });
        return btn;
    }

    private JLabel filterLabel(String text, ThemeManager.Theme t) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(t.textMuted());
        return l;
    }

    // ── Background painting ───────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        ThemeManager.Theme t = ThemeManager.get();
        g2.setPaint(new GradientPaint(0, 0, t.bg(),
                getWidth(), getHeight(), t.bgSecondary()));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}