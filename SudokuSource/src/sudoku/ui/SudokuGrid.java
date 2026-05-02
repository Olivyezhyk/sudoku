package sudoku.ui;

import sudoku.core.*;
import sudoku.utils.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Extends the original SudokuGrid to render:
 *   – Timed Mode  : semi-transparent region tints + bold region borders
 *   – Killer Mode : dashed cage outlines + sum labels in top-left corner
 *
 * The original Classic rendering is unchanged.
 */
public class SudokuGrid extends JPanel {

    private GameState state;
    private int       highlightNum = 0;
    private Runnable  onChange;

    public SudokuGrid(GameState state, Runnable onChange) {
        this.state    = state;
        this.onChange = onChange;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int cellW = getWidth()  / 9;
                int cellH = getHeight() / 9;
                int c = e.getX() / cellW;
                int r = e.getY() / cellH;
                if (r >= 0 && r < 9 && c >= 0 && c < 9) {
                    state.selectCell(r, c);
                    int val = state.getValue(r, c);
                    highlightNum = val;
                    onChange.run();
                    repaint();
                }
            }
        });
    }

    public void setHighlightNum(int num) { this.highlightNum = num; repaint(); }

    // ── painting ─────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        ThemeManager.Theme t = ThemeManager.get();
        int W = getWidth(), H = getHeight();
        int cellW = W / 9,  cellH = H / 9;
        int selR  = state.getSelectedRow(), selC = state.getSelectedCol();

        // ── 1. Mode-specific background tints (drawn before cells) ────────────
        GameMode mode = state.getGameMode();
        if (mode == GameMode.TIMED);
        else if (mode == GameMode.KILLER) drawKillerCageTints(g2, cellW, cellH, t);

        // ── 2. Cell backgrounds and content ──────────────────────────────────
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                int x = c * cellW, y = r * cellH;
                int val = state.getValue(r, c);

                Color cellColor = chooseCellBg(r, c, selR, selC, val, t);
                g2.setColor(cellColor);
                g2.fillRect(x + 1, y + 1, cellW - 2, cellH - 2);

                if (val != 0) {
                    drawCellValue(g2, r, c, x, y, cellW, cellH, val, t);
                } else {
                    List<Integer> notesHere = state.getNotes(r, c);
                    if (!notesHere.isEmpty()) drawNotes(g2, notesHere, x, y, cellW, cellH, t);
                }
            }
        }



        // ── 4. Standard grid lines ────────────────────────────────────────────
        drawGridLines(g2, W, H, cellW, cellH, t);

        // ── 5. Killer cage outlines — drawn LAST so they sit on top of grid ───
        if (mode == GameMode.KILLER) drawKillerCageOutlines(g2, cellW, cellH, t);

        g2.dispose();
    }

    // ── Classic cell background ───────────────────────────────────────────────

    private Color chooseCellBg(int r, int c, int selR, int selC, int val,
                               ThemeManager.Theme t) {
        Color base;
        if (r == selR && c == selC) {
            base = t.cellSelected();
        } else if (state.isError(r, c)) {
            base = new Color(t.cellError().getRed(), t.cellError().getGreen(),
                    t.cellError().getBlue(), 120);
        } else if (selR >= 0 && selC >= 0 &&
                (r == selR || c == selC || sameBox(r, c, selR, selC))) {
            base = t.cellHighlight();
        } else if (state.isFixed(r, c)) {
            base = t.cellFixed();
        } else {
            base = t.cellBg();
        }

        if (highlightNum > 0 && val == highlightNum && val != 0)
            base = blend(base, t.accent(), 0.35f);
        return base;
    }

    private void drawCellValue(Graphics2D g2, int r, int c, int x, int y,
                               int cellW, int cellH, int val,
                               ThemeManager.Theme t) {
        Color numColor;
        if (state.isError(r, c))  numColor = t.cellError();
        else if (state.isFixed(r, c)) numColor = t.text();
        else                          numColor = t.accentLight();

        g2.setColor(numColor);
        Font f = new Font("SansSerif", Font.BOLD, Math.min(cellW, cellH) * 55 / 100);
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();
        String s  = String.valueOf(val);
        int tx = x + (cellW - fm.stringWidth(s)) / 2;
        int ty = y + (cellH + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(s, tx, ty);
    }

    // ── Notes ─────────────────────────────────────────────────────────────────

    private void drawNotes(Graphics2D g2, List<Integer> notes, int x, int y,
                           int cw, int ch, ThemeManager.Theme t) {
        int noteSize = cw / 3;
        Font f = new Font("SansSerif", Font.PLAIN, Math.max(8, noteSize * 50 / 100));
        g2.setFont(f);
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(t.textMuted());
        for (int n : notes) {
            int nr = (n - 1) / 3, nc = (n - 1) % 3;
            int nx = x + nc * noteSize + (noteSize - fm.stringWidth(String.valueOf(n))) / 3;
            int ny = y + nr * noteSize + (noteSize + fm.getAscent() - fm.getDescent()) / 3;
            g2.drawString(String.valueOf(n), nx, ny);
        }
    }

    // ── Grid lines ────────────────────────────────────────────────────────────

    private void drawGridLines(Graphics2D g2, int W, int H, int cellW, int cellH,
                               ThemeManager.Theme t) {
        g2.setColor(t.gridLine());
        g2.setStroke(new BasicStroke(0.5f));
        for (int i = 1; i < 9; i++) {
            if (i % 3 != 0) {
                g2.drawLine(i * cellW, 0, i * cellW, H);
                g2.drawLine(0, i * cellH, W, i * cellH);
            }
        }
        g2.setColor(t.gridBold());
        g2.setStroke(new BasicStroke(2.5f));
        for (int i = 0; i <= 9; i += 3) {
            g2.drawLine(i * cellW, 0, i * cellW, H);
            g2.drawLine(0, i * cellH, W, i * cellH);
        }
    }



    // ── Killer Mode rendering ─────────────────────────────────────────────────

    private void drawKillerCageTints(Graphics2D g2, int cellW, int cellH,
                                     ThemeManager.Theme t) {
        for (Cage cage : state.getCages()) {
            Color base = cage.getColor(); // vivid full-brightness colour

            // Fill: same hue as the border but very transparent so digits stay readable
            Color fill = new Color(base.getRed(), base.getGreen(), base.getBlue(), 40);

            g2.setColor(fill);
            for (Cage.Cell cell : cage.getCells())
                g2.fillRect(cell.col() * cellW + 1,
                        cell.row() * cellH + 1,
                        cellW - 2, cellH - 2);
        }
    }

    /**
     * Draws cage outlines and sum labels for Killer Sudoku.
     *
     * Each cage gets a solid border in its own colour (derived from the cage's
     * pastel fill but fully opaque and darkened for contrast).  Border width is
     * 2.5 px so it is clearly visible against the dark background.
     * Sum labels are drawn in the cage's colour on a dark pill background.
     */
    private void drawKillerCageOutlines(Graphics2D g2, int cellW, int cellH,
                                        ThemeManager.Theme t) {
        List<Cage> cages = state.getCages();

        // Build cage-index map (cell → cage index, -1 if none)
        int[][] cageMap = new int[9][9];
        for (int[] row : cageMap) java.util.Arrays.fill(row, -1);
        for (int i = 0; i < cages.size(); i++)
            for (Cage.Cell cell : cages.get(i).getCells())
                cageMap[cell.row()][cell.col()] = i;

        // ── Draw borders — dashed, one pass per cage so colour switches cleanly ─
        float[] dashPattern = {5f, 3f};   // 5 px on, 3 px off
        Stroke borderStroke = new BasicStroke(2.2f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 0f,
                dashPattern, 0f);
        g2.setStroke(borderStroke);

        for (int i = 0; i < cages.size(); i++) {
            // Vivid colour at full opacity for the border
            Color base = cages.get(i).getColor();
            Color border = new Color(base.getRed(), base.getGreen(), base.getBlue(), 255);
            g2.setColor(border);

            for (Cage.Cell cell : cages.get(i).getCells()) {
                int r = cell.row(), c = cell.col();
                int x = c * cellW,  y = r * cellH;

                // Draw only the edges that face a different cage (or the grid edge)
                int above = (r > 0) ? cageMap[r - 1][c] : -2;
                if (above != i)
                    g2.drawLine(x + 2, y, x + cellW - 2, y);

                int left = (c > 0) ? cageMap[r][c - 1] : -2;
                if (left != i)
                    g2.drawLine(x, y + 2, x, y + cellH - 2);

                int right = (c < 8) ? cageMap[r][c + 1] : -2;
                if (right != i)
                    g2.drawLine(x + cellW, y + 2, x + cellW, y + cellH - 2);

                int below = (r < 8) ? cageMap[r + 1][c] : -2;
                if (below != i)
                    g2.drawLine(x + 2, y + cellH, x + cellW - 2, y + cellH);
            }
        }

        // ── Draw sum labels ───────────────────────────────────────────────────
        g2.setStroke(new BasicStroke(1f));
        Font sumFont = new Font("SansSerif", Font.BOLD,
                Math.max(9, Math.min(cellW, cellH) * 28 / 100));
        g2.setFont(sumFont);
        FontMetrics fm = g2.getFontMetrics();

        for (int i = 0; i < cages.size(); i++) {
            Cage cage = cages.get(i);
            Cage.Cell tl = cage.getTopLeftCell();
            int x = tl.col() * cellW;
            int y = tl.row() * cellH;
            String label = String.valueOf(cage.getTargetSum());

            int lw = fm.stringWidth(label) + 6;
            int lh = fm.getAscent() + 4;
            int px = x + 3;
            int py = y + 3;

            // Dark pill background so label is readable on any cage colour
            g2.setColor(new Color(15, 20, 35, 210));
            g2.fillRoundRect(px, py, lw, lh, 4, 4);

            // Label in the cage's own vivid colour — immediately readable
            Color base = cage.getColor();
            Color labelColor = new Color(base.getRed(), base.getGreen(), base.getBlue(), 255);
            g2.setColor(labelColor);
            g2.drawString(label, px + 3, py + fm.getAscent());
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private boolean sameBox(int r1, int c1, int r2, int c2) {
        return (r1 / 3 == r2 / 3) && (c1 / 3 == c2 / 3);
    }

    private Color blend(Color a, Color b, float ratio) {
        int r  = (int)(a.getRed()   * (1 - ratio) + b.getRed()   * ratio);
        int g  = (int)(a.getGreen() * (1 - ratio) + b.getGreen() * ratio);
        int bl = (int)(a.getBlue()  * (1 - ratio) + b.getBlue()  * ratio);
        int al = Math.max(a.getAlpha(), 180);
        return new Color(Math.min(r,255), Math.min(g,255), Math.min(bl,255), al);
    }
}