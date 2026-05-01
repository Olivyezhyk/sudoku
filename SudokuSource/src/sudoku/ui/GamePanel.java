package sudoku.ui;

import sudoku.Main;
import sudoku.audio.SoundManager;
import sudoku.core.*;
import sudoku.utils.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Game panel updated to:
 *   – Accept a {@link GameMode} parameter alongside {@link Difficulty}.
 *   – Initialise the correct generator (Classic / Chaos / Killer).
 *   – Show the game-mode name in the top bar.
 *
 * All other logic is preserved verbatim.
 */
public class GamePanel extends JPanel {

    private final Difficulty difficulty;
    private final GameMode   gameMode;

    private GameState   state;
    private SudokuGrid  grid;

    private JLabel     timerLabel;
    private JPanel     heartsPanel;
    private JLabel     hintsLabel;
    private Timer      gameTimer;
    private int        elapsedSeconds  = 0;
    private int        remainingSeconds = 0;   // used only in TIMED mode
    private JButton[]  numBtns = new JButton[9];

    private JToggleButton penBtn, pencilBtn;

    // ── Time limits per difficulty in TIMED mode ──────────────────────────────

    private int getTimeLimit() {
        return switch (difficulty) {
            case EASY   -> 15 * 60;   // 15 min
            case MEDIUM -> 10 * 60;   // 10 min
            case HARD   ->  7 * 60;   //  7 min
        };
    }

    // ── Constructor overloads ─────────────────────────────────────────────────

    /** Backwards-compatible: Classic mode. */
    public GamePanel(Difficulty difficulty) {
        this(difficulty, GameMode.CLASSIC);
    }

    public GamePanel(Difficulty difficulty, GameMode gameMode) {
        this.difficulty = difficulty;
        this.gameMode   = gameMode;
        initGame();
        if (gameMode == GameMode.TIMED) remainingSeconds = getTimeLimit();
        buildUI();
        updateStats();
        startTimer();
    }

    // ── Game initialisation ───────────────────────────────────────────────────

    private void initGame() {
        int attempts = 0;
        while (true) {
            try {
                state = switch (gameMode) {
                    case KILLER -> new KillerGenerator().generate(difficulty);
                    // TIMED uses a standard classic puzzle — the only difference
                    // is the countdown timer enforced in startTimer().
                    default -> {
                        SudokuGenerator gen = new SudokuGenerator();
                        int[][] puzzle   = gen.generatePuzzle(difficulty);
                        int[][] solution = gen.solve(puzzle);
                        yield new GameState(puzzle, solution);
                    }
                };
                return;
            } catch (IllegalStateException e) {
                attempts++;
                if (attempts >= 3) {
                    System.err.println("GamePanel: generator failed, falling back to Classic. " + e.getMessage());
                    SudokuGenerator gen = new SudokuGenerator();
                    int[][] puzzle   = gen.generatePuzzle(difficulty);
                    int[][] solution = gen.solve(puzzle);
                    state = new GameState(puzzle, solution);
                    return;
                }
            }
        }
    }

    // ── UI Building ───────────────────────────────────────────────────────────

    private void buildUI() {
        setLayout(new BorderLayout(0, 0));
        add(createTopBar(),    BorderLayout.NORTH);

        grid = new SudokuGrid(state, this::onGridChange);
        JPanel gridWrapper = new JPanel(new GridBagLayout());
        gridWrapper.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 1; gc.weighty = 1; gc.fill = GridBagConstraints.BOTH;
        gc.insets  = new Insets(10, 16, 10, 16);
        gridWrapper.add(grid, gc);
        add(gridWrapper, BorderLayout.CENTER);
        add(createRightPanel(), BorderLayout.EAST);

        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (k >= KeyEvent.VK_1 && k <= KeyEvent.VK_9)              enterNum(k - KeyEvent.VK_0);
                else if (k >= KeyEvent.VK_NUMPAD1 && k <= KeyEvent.VK_NUMPAD9) enterNum(k - KeyEvent.VK_NUMPAD0);
                else if (k == KeyEvent.VK_DELETE || k == KeyEvent.VK_BACK_SPACE) deleteSelected();
                else if (k == KeyEvent.VK_UP    && state.getSelectedRow() > 0) nav(state.getSelectedRow()-1, state.getSelectedCol());
                else if (k == KeyEvent.VK_DOWN  && state.getSelectedRow() < 8) nav(state.getSelectedRow()+1, state.getSelectedCol());
                else if (k == KeyEvent.VK_LEFT  && state.getSelectedCol() > 0) nav(state.getSelectedRow(), state.getSelectedCol()-1);
                else if (k == KeyEvent.VK_RIGHT && state.getSelectedCol() < 8) nav(state.getSelectedRow(), state.getSelectedCol()+1);
            }
        });
        SwingUtilities.invokeLater(this::requestFocus);
    }

    private void nav(int r, int c) { state.selectCell(r, c); grid.repaint(); }

    // ── Top bar ───────────────────────────────────────────────────────────────

    private JPanel createTopBar() {
        ThemeManager.Theme t = ThemeManager.get();
        JPanel bar = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                ThemeManager.Theme th = ThemeManager.get();
                g2.setColor(th.bgSecondary());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(th.gridLine());
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        // Back button
        StyledButton back = iconBtn("", 32, loadIcon("back.png", 25));
        back.setPreferredSize(new Dimension(72, 60));
        back.addActionListener(e -> { gameTimer.stop(); Main.showMenu(); });

        // Centre info
        JPanel centre = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        centre.setOpaque(false);

        String modeText = switch (gameMode) {
            case TIMED  -> difficulty.toString().toUpperCase() + " · "
                    + String.format("%d:%02d", getTimeLimit()/60, getTimeLimit()%60);
            case KILLER -> "KILLER · " + difficulty.toString().toUpperCase();
            default     -> difficulty.toString().toUpperCase();
        };
        JLabel diffLbl = new JLabel(modeText);
        diffLbl.setFont(new Font("SansSerif", Font.BOLD,
                gameMode == GameMode.CLASSIC ? 32 : 22));
        diffLbl.setForeground(t.accentLight());

        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 32));
        sep.setForeground(t.gridLine());

        heartsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 32, 0));
        heartsPanel.setOpaque(false);

        timerLabel = new JLabel();
        timerLabel.setFont(new Font("SansSerif", Font.PLAIN, 32));
        timerLabel.setForeground(t.textMuted());
        timerLabel.setIconTextGap(6);
        timerLabel.setIcon(loadIcon("timer.png", 32));
        if (gameMode == GameMode.TIMED) {
            int lim = getTimeLimit();
            timerLabel.setText(String.format(" %02d:%02d", lim/60, lim%60));
        } else {
            timerLabel.setText(" 00:00");
        }

        hintsLabel = new JLabel();
        hintsLabel.setFont(new Font("SansSerif", Font.PLAIN, 32));
        hintsLabel.setForeground(t.textMuted());
        hintsLabel.setIconTextGap(6);

        centre.add(diffLbl); centre.add(sep);
        centre.add(heartsPanel); centre.add(timerLabel); centre.add(hintsLabel);

        // Settings button
        StyledButton settingsBtn = iconBtn("", 28, loadIcon("settings.png", 25));
        settingsBtn.setPreferredSize(new Dimension(72, 60));
        settingsBtn.addActionListener(e -> {
            gameTimer.stop();
            Main.showPanel(new SettingsPanel(() -> { Main.showPanel(GamePanel.this); gameTimer.start(); }));
        });

        bar.add(back,        BorderLayout.WEST);
        bar.add(centre,      BorderLayout.CENTER);
        bar.add(settingsBtn, BorderLayout.EAST);
        return bar;
    }

    // ── Right panel ───────────────────────────────────────────────────────────

    private JPanel createRightPanel() {
        ThemeManager.Theme t = ThemeManager.get();
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                ThemeManager.Theme th = ThemeManager.get();
                g2.setColor(th.bgSecondary()); g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(th.gridLine());    g2.drawLine(0,0,0,getHeight());
                g2.dispose();
            }
        };
        panel.setLayout(new GridBagLayout());
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(280, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(6,0,6,0);

        panel.add(sectionLabel("Numbers", t), rowOf(gbc, 0, new Insets(0,0,6,0)));

        JPanel numGrid = new JPanel(new GridLayout(3, 3, 8, 8));
        numGrid.setOpaque(false);
        for (int i = 1; i <= 9; i++) {
            final int num = i;
            // FIX: createNumBtn no longer adds its own listener, so we add only ONE here
            JButton btn = createNumBtn(String.valueOf(i));
            numBtns[i-1] = btn;
            btn.addActionListener(e -> { enterNum(num); requestFocus(); });
            numGrid.add(btn);
        }
        panel.add(numGrid, rowOf(gbc, 1, new Insets(8,0,24,0)));

        panel.add(sectionLabel("Mode", t), rowOf(gbc, 2, new Insets(0,0,8,0)));
        JPanel modeRow = new JPanel(new GridLayout(1, 2, 8, 0));
        modeRow.setOpaque(false);
        penBtn    = createModeBtn(loadIcon("pen.png",    32), true);
        pencilBtn = createModeBtn(loadIcon("pencil.png", 32), false);
        penBtn.addActionListener(e  -> { state.setPencilMode(false); penBtn.setSelected(true);    pencilBtn.setSelected(false); modeRow.repaint(); updateStats(); requestFocus(); });
        pencilBtn.addActionListener(e -> { state.setPencilMode(true);  pencilBtn.setSelected(true); penBtn.setSelected(false);    modeRow.repaint(); updateStats(); requestFocus(); });
        modeRow.add(penBtn); modeRow.add(pencilBtn);
        panel.add(modeRow, rowOf(gbc, 3, new Insets(0,0,24,0)));

        panel.add(sectionLabel("Actions", t), rowOf(gbc, 4, new Insets(0,0,8,0)));
        JButton eraseBtn = createActionBtn(loadIcon("erase.png", 32));
        eraseBtn.addActionListener(e -> { deleteSelected(); requestFocus(); });
        panel.add(eraseBtn, rowOf(gbc, 5, new Insets(0,0,8,0)));

        JButton hintBtn = createActionBtn(loadIcon("hint.png", 32));
        hintBtn.addActionListener(e -> { useHint(); requestFocus(); });
        panel.add(hintBtn, rowOf(gbc, 6, new Insets(0,0,24,0)));

        gbc.gridy = 7; gbc.weighty = 1; panel.add(Box.createVerticalGlue(), gbc); gbc.weighty = 0;

        StyledButton newGame = new StyledButton("New Game");
        newGame.setPreferredSize(new Dimension(256, 60));
        newGame.setFont(new Font("SansSerif", Font.BOLD, 18));
        newGame.addActionListener(e -> restartGame());
        panel.add(newGame, rowOf(gbc, 8, new Insets(0,0,0,0)));

        return panel;
    }

    // ── Number buttons ────────────────────────────────────────────────────────

    // FIX: removed the num parameter and the internal addActionListener —
    //      the listener is now added exactly once in createRightPanel().
    private JButton createNumBtn(String label) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme t = ThemeManager.get();
                boolean exhausted = !isEnabled();
                ButtonModel m = getModel();
                if (exhausted) {
                    g2.setColor(new Color(t.textMuted().getRed(),t.textMuted().getGreen(),t.textMuted().getBlue(),30));
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                    g2.setColor(new Color(t.textMuted().getRed(),t.textMuted().getGreen(),t.textMuted().getBlue(),60));
                    g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                    FontMetrics fm = g2.getFontMetrics(getFont());
                    g2.setColor(new Color(t.textMuted().getRed(),t.textMuted().getGreen(),t.textMuted().getBlue(),100));
                    g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                } else {
                    Color bg = m.isPressed() ? t.accent()
                            : m.isRollover() ? new Color(t.accent().getRed(),t.accent().getGreen(),t.accent().getBlue(),100)
                            : new Color(t.accent().getRed(),t.accent().getGreen(),t.accent().getBlue(),50);
                    g2.setColor(bg); g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                    g2.setColor(t.accent()); g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                    FontMetrics fm = g2.getFontMetrics(getFont());
                    g2.setColor(t.text());
                    g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                }
                g2.dispose();
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 28));
        btn.setPreferredSize(new Dimension(76, 76));
        btn.setContentAreaFilled(false); btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // NO addActionListener here — added once in createRightPanel()
        return btn;
    }

    private JToggleButton createModeBtn(ImageIcon icon, boolean selected) {
        JToggleButton btn = new JToggleButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme t = ThemeManager.get();
                if (isSelected()) g2.setPaint(new GradientPaint(0,0,t.accent(),getWidth(),0,t.accentDark()));
                else              g2.setColor(new Color(t.accent().getRed(),t.accent().getGreen(),t.accent().getBlue(),40));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(t.accent()); g2.setStroke(new BasicStroke(isSelected()?2:1));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                if (icon != null) g2.drawImage(icon.getImage(),(getWidth()-icon.getIconWidth())/2,(getHeight()-icon.getIconHeight())/2,icon.getIconWidth(),icon.getIconHeight(),null);
                g2.dispose();
            }
        };
        btn.setSelected(selected);
        btn.setPreferredSize(new Dimension(0, 60));
        btn.setContentAreaFilled(false); btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createActionBtn(ImageIcon icon) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme t = ThemeManager.get();
                ButtonModel m = getModel();
                g2.setColor(m.isRollover()
                        ? new Color(t.accent().getRed(),t.accent().getGreen(),t.accent().getBlue(),70)
                        : new Color(t.accent().getRed(),t.accent().getGreen(),t.accent().getBlue(),30));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(t.gridLine()); g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                if (icon != null) g2.drawImage(icon.getImage(),(getWidth()-icon.getIconWidth())/2,(getHeight()-icon.getIconHeight())/2,icon.getIconWidth(),icon.getIconHeight(),null);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(256, 60));
        btn.setContentAreaFilled(false); btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Styled icon button (top bar) ──────────────────────────────────────────

    private StyledButton iconBtn(String text, int fontSize) {
        return iconBtn(text, fontSize, null);
    }

    private StyledButton iconBtn(String text, int fontSize, ImageIcon icon) {
        return new StyledButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme th = ThemeManager.get();
                g2.setColor(new Color(th.accent().getRed(),th.accent().getGreen(),th.accent().getBlue(),60));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                if (icon != null) {
                    int ix = (getWidth()  - icon.getIconWidth())  / 2;
                    int iy = (getHeight() - icon.getIconHeight()) / 2;
                    g2.drawImage(icon.getImage(), ix, iy,
                            icon.getIconWidth(), icon.getIconHeight(), null);
                } else {
                    FontMetrics fm = g2.getFontMetrics(getFont());
                    g2.setColor(th.text()); g2.setFont(getFont());
                    g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                }
                g2.dispose();
            }
        };
    }

    // ── Game logic ────────────────────────────────────────────────────────────

    private void startTimer() {
        gameTimer = new Timer(1000, e -> {
            if (state.isGameOver() || state.isWon()) return;

            if (gameMode == GameMode.TIMED) {
                remainingSeconds--;
                updateTimerLabel();
                if (remainingSeconds <= 0) {
                    remainingSeconds = 0;
                    gameTimer.stop();
                    showEndDialog(false);
                }
            } else {
                elapsedSeconds++;
                updateTimerLabel();
            }
        });
        gameTimer.start();
    }

    private void updateTimerLabel() {
        if (gameMode == GameMode.TIMED) {
            int m = remainingSeconds / 60, s = remainingSeconds % 60;
            timerLabel.setText(String.format(" %02d:%02d", m, s));
            ThemeManager.Theme t = ThemeManager.get();
            timerLabel.setForeground(remainingSeconds <= 60 ? t.cellError() : t.textMuted());
        } else {
            int m = elapsedSeconds / 60, s = elapsedSeconds % 60;
            timerLabel.setText(String.format(" %02d:%02d", m, s));
        }
    }

    private void enterNum(int num) {
        if (state.isGameOver() || state.isWon()) return;

        boolean wasPencil = state.isPencilMode();

        // In pen mode, do not allow overwriting a correctly placed digit
        if (!wasPencil) {
            int selR = state.getSelectedRow(), selC = state.getSelectedCol();
            if (selR >= 0 && selC >= 0) {
                int current = state.getValue(selR, selC);
                int correct = state.getSolution()[selR][selC];
                if (current != 0 && current == correct) return;
            }
        }

        state.enterNumber(num);

        if (!wasPencil) {
            // Pen mode: highlight all cells with this digit
            grid.setHighlightNum(num);
        } else {
            // Pencil mode: just repaint notes, do NOT change the number highlight
            grid.repaint();
        }

        updateStats();

        int selR = state.getSelectedRow(), selC = state.getSelectedCol();
        if (selR >= 0 && selC >= 0 && state.isError(selR, selC))
            SoundManager.playAsync(SoundManager.SoundType.ERROR);
        else if (!wasPencil)
            SoundManager.playAsync(SoundManager.SoundType.SUCCESS);

        if (state.isWon())           { gameTimer.stop(); showEndDialog(true);  }
        else if (state.isGameOver()) { gameTimer.stop(); showEndDialog(false); }
    }

    private void deleteSelected() {
        int r = state.getSelectedRow(), c = state.getSelectedCol();
        if (r < 0 || c < 0) return;

        // Не стираємо, якщо:
        // 1. Це початково задана цифра (givens)
        // 2. Режим пера + цифра правильна
        if (state.isGiven(r, c)) return;

        int current = state.getValue(r, c);
        int correct  = state.getSolution()[r][c];
        boolean isCorrectPen = !state.isPencilMode() && current != 0 && current == correct;
        if (isCorrectPen) return;

        state.deleteSelected();
        grid.repaint();
    }

    private void useHint() {
        if (state.useHint()) {
            SoundManager.playAsync(SoundManager.SoundType.SUCCESS);
            updateStats(); grid.repaint();
            if (state.isWon()) { gameTimer.stop(); showEndDialog(true); }
        }
    }

    private void onGridChange() { updateStats(); }

    private void updateStats() {
        if (numBtns != null) {
            for (int i = 0; i < numBtns.length; i++) {
                if (numBtns[i] == null) continue;
                int num = i + 1;
                boolean exhausted = !state.isPencilMode() && countCorrectlyPlaced(num) >= 9;
                numBtns[i].setEnabled(!exhausted);
                numBtns[i].repaint();
            }
        }
        heartsPanel.removeAll();
        int mistakes = state.getMistakeCount(), max = state.getMaxMistakes();
        for (int i = 0; i < max; i++) {
            JLabel heart = new JLabel();
            heart.setIcon(loadIcon(i < mistakes ? "heartbroken.png" : "heart.png", 32));
            heartsPanel.add(heart);
        }
        heartsPanel.revalidate(); heartsPanel.repaint();
        hintsLabel.setIcon(loadIcon("hint.png", 32));
        hintsLabel.setText(" " + state.getHintsLeft());
        grid.repaint();
    }

    private int countCorrectlyPlaced(int num) {
        int count = 0;
        int[][] sol = state.getSolution();
        for (int r = 0; r < GameState.SIZE; r++)
            for (int c = 0; c < GameState.SIZE; c++)
                if (state.getValue(r,c) == num && state.getValue(r,c) == sol[r][c]) count++;
        return count;
    }

    private void restartGame() {
        gameTimer.stop();
        elapsedSeconds   = 0;
        remainingSeconds = 0;
        initGame();
        if (gameMode == GameMode.TIMED) remainingSeconds = getTimeLimit();
        Component gridWrapper = ((BorderLayout) getLayout()).getLayoutComponent(BorderLayout.CENTER);
        remove(gridWrapper);
        grid = new SudokuGrid(state, this::onGridChange);
        JPanel newWrapper = new JPanel(new GridBagLayout());
        newWrapper.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx=1; gc.weighty=1; gc.fill=GridBagConstraints.BOTH;
        gc.insets=new Insets(10,16,10,16);
        newWrapper.add(grid, gc);
        add(newWrapper, BorderLayout.CENTER);
        revalidate(); repaint(); updateStats();
        updateTimerLabel();
        startTimer(); requestFocus();
    }

    private void showEndDialog(boolean won) {
        SoundManager.playAsync(won ? SoundManager.SoundType.WIN : SoundManager.SoundType.LOSE);
        JDialog dialog = new JDialog(Main.frame, true);
        dialog.setUndecorated(true); dialog.setSize(340, 260);
        dialog.setLocationRelativeTo(Main.frame);
        JPanel content = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme t = ThemeManager.get();
                g2.setColor(t.bgSecondary()); g2.fillRoundRect(0,0,getWidth(),getHeight(),20,20);
                g2.setColor(t.gridBold());    g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1,1,getWidth()-2,getHeight()-2,20,20);
                g2.dispose();
            }
        };
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx=0; gbc.fill=GridBagConstraints.HORIZONTAL;
        ThemeManager.Theme t = ThemeManager.get();

        boolean timeUp = !won && gameMode == GameMode.TIMED && remainingSeconds <= 0;

        String emojiStr = won ? "🎉" : (timeUp ? "⏰" : "💀");
        JLabel emoji = new JLabel(emojiStr);
        emoji.setFont(new Font("SansSerif", Font.PLAIN, 48)); emoji.setHorizontalAlignment(SwingConstants.CENTER);
        content.add(emoji, rowOf(gbc, 0, new Insets(20,30,4,30)));

        String titleStr = won ? "Victory!" : (timeUp ? "Time's Up!" : "Game Over");
        JLabel titleLbl = new JLabel(titleStr);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLbl.setForeground(won ? t.accentLight() : t.cellError());
        titleLbl.setHorizontalAlignment(SwingConstants.CENTER);
        content.add(titleLbl, rowOf(gbc, 1, new Insets(0,30,4,30)));

        String subStr;
        if (won && gameMode == GameMode.TIMED) {
            int used = getTimeLimit() - remainingSeconds;
            subStr = String.format("Solved in %02d:%02d ✨", used/60, used%60);
        } else if (won) {
            int m = elapsedSeconds/60, s = elapsedSeconds%60;
            subStr = String.format("Solved in %02d:%02d ✨", m, s);
        } else if (timeUp) {
            subStr = "You ran out of time!";
        } else {
            subStr = "Better luck next time!";
        }
        JLabel subLbl = new JLabel(subStr);
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 13)); subLbl.setForeground(t.textMuted());
        subLbl.setHorizontalAlignment(SwingConstants.CENTER);
        content.add(subLbl, rowOf(gbc, 2, new Insets(0,30,14,30)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0)); btnRow.setOpaque(false);
        StyledButton restart = new StyledButton("Restart");
        restart.setPreferredSize(new Dimension(120,40));
        restart.addActionListener(e -> { dialog.dispose(); restartGame(); });
        StyledButton menuBtn = new StyledButton("Menu") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeManager.Theme th = ThemeManager.get();
                g2.setColor(new Color(th.accent().getRed(),th.accent().getGreen(),th.accent().getBlue(),50));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.setColor(th.accent()); g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,12,12);
                FontMetrics fm = g2.getFontMetrics(getFont());
                g2.setColor(th.textMuted()); g2.setFont(getFont());
                g2.drawString(getText(),(getWidth()-fm.stringWidth(getText()))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
                g2.dispose();
            }
        };
        menuBtn.setPreferredSize(new Dimension(120,40));
        menuBtn.addActionListener(e -> { dialog.dispose(); gameTimer.stop(); Main.showMenu(); });
        btnRow.add(restart); btnRow.add(menuBtn);
        content.add(btnRow, rowOf(gbc, 3, new Insets(0,30,20,30)));
        dialog.setContentPane(content); dialog.setVisible(true);
    }

    // ── Painting ──────────────────────────────────────────────────────────────

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        ThemeManager.Theme t = ThemeManager.get();
        g2.setPaint(new GradientPaint(0,0,t.bg(),getWidth(),getHeight(),t.bgSecondary()));
        g2.fillRect(0,0,getWidth(),getHeight()); g2.dispose();
    }

    @Override public void removeNotify() { super.removeNotify(); if (gameTimer!=null) gameTimer.stop(); }

    // ── Layout helpers ────────────────────────────────────────────────────────

    private static GridBagConstraints rowOf(GridBagConstraints gbc, int y, Insets insets) {
        gbc.gridy = y; gbc.insets = insets; return gbc;
    }

    private JLabel sectionLabel(String text, ThemeManager.Theme t) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 14)); l.setForeground(t.textMuted());
        l.setHorizontalAlignment(SwingConstants.CENTER); return l;
    }

    // ── Icon loader ───────────────────────────────────────────────────────────

    private ImageIcon loadIcon(String name, int size) {
        String path = "/icons/" + name;
        java.net.URL url = getClass().getResource(path);
        if (url == null) { System.out.println("❌ NOT FOUND: " + path); return null; }
        ImageIcon icon = new ImageIcon(url);
        return new ImageIcon(icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
    }
}