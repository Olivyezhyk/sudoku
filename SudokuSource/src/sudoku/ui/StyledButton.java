package sudoku.ui;

import sudoku.audio.SoundManager;
import sudoku.utils.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

public class StyledButton extends JButton {
    private float hoverAlpha = 0f;
    private Timer hoverTimer;
    private boolean hovering = false;
    private boolean pill = false;

    public StyledButton(String text) {
        this(text, false);
    }

    public StyledButton(String text, boolean pill) {
        super(text);
        this.pill = pill;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFont(new Font("SansSerif", Font.BOLD, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setForeground(ThemeManager.get().buttonText());

        hoverTimer = new Timer(16, e -> {
            if (hovering) hoverAlpha = Math.min(1f, hoverAlpha + 0.1f);
            else          hoverAlpha = Math.max(0f, hoverAlpha - 0.1f);
            if ((!hovering && hoverAlpha <= 0f) || (hovering && hoverAlpha >= 1f)) hoverTimer.stop();
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { hovering = true;  hoverTimer.start(); }
            public void mouseExited(MouseEvent e)  { hovering = false; hoverTimer.start(); }
            public void mousePressed(MouseEvent e) { SoundManager.playAsync(SoundManager.SoundType.CLICK); }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        ThemeManager.Theme t = ThemeManager.get();
        int arc = pill ? getHeight() : 12;
        RoundRectangle2D rect = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc);

        // Base gradient
        GradientPaint gp = new GradientPaint(0, 0, t.accent(), getWidth(), getHeight(), t.accentDark());
        g2.setPaint(gp);
        g2.fill(rect);

        // Hover overlay
        if (hoverAlpha > 0) {
            g2.setColor(new Color(255, 255, 255, (int)(hoverAlpha * 40)));
            g2.fill(rect);
        }

        // Press overlay
        ButtonModel m = getModel();
        if (m.isPressed()) {
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fill(rect);
        }

        // Text
        FontMetrics fm = g2.getFontMetrics(getFont());
        int tx = (getWidth()  - fm.stringWidth(getText())) / 2;
        int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        g2.setFont(getFont());
        g2.setColor(getForeground());
        g2.drawString(getText(), tx, ty);

        g2.dispose();
    }
}
