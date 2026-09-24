import javax.swing.JButton;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RoundedButton extends JButton {
    private static final int HOVER_ANIM_MS = 140;
    private static final int TICK_MS = 12;
 
    private final int arc;
    private final Color baseBg, baseFg, borderColor;
    private Color gradientTo = null;
    private boolean active = false;
 
    private float hoverProgress = 0f; // 0 = resting, 1 = fully hovered
    private Timer hoverTimer;

    private String cornerLeftText, cornerRightText;
    private Color cornerLeftColor, cornerRightColor;

    public RoundedButton(String text, int arc, Color bg, Color fg, Color borderColor) {
        super(text);
        this.arc = arc;
        this.baseBg = bg;
        this.baseFg = fg;
        this.borderColor = borderColor;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setForeground(fg);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
 
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (isEnabled()) animateHoverTo(1f); }
            @Override public void mouseExited(MouseEvent e) { animateHoverTo(0f); }
        });
    }

    public void setGradientTo(Color c) { this.gradientTo = c; }
    public void setActive(boolean active) { this.active = active; repaint(); }
    
    public void setCornerLabels(String leftText, Color leftColor, String rightText, Color rightColor) {
        this.cornerLeftText = leftText;
        this.cornerLeftColor = leftColor;
        this.cornerRightText = rightText;
        this.cornerRightColor = rightColor;
        repaint();
    }

    private void animateHoverTo(float target) {
        if (hoverTimer != null && hoverTimer.isRunning()) hoverTimer.stop();
        float start = hoverProgress;
        int totalTicks = Math.max(1, HOVER_ANIM_MS / TICK_MS);
        int[] tick = {0};
        hoverTimer = new Timer(TICK_MS, e -> {
            tick[0]++;
            float t = Math.min(1f, tick[0] / (float) totalTicks);
            hoverProgress = start + (target - start) * t;
            repaint();
            if (t >= 1f) ((Timer) e.getSource()).stop();
        });
        hoverTimer.start();
    }
 
   private static Color lighten(Color c, int amount) {
        return new Color(
            Math.min(255, c.getRed() + amount),
            Math.min(255, c.getGreen() + amount),
            Math.min(255, c.getBlue() + amount)
        );
    }
    
    private static Color blend(Color a, Color b, float t) {
        int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, bl);
    }
      @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int arcUse = Math.min(arc, getHeight());
        boolean enabled = isEnabled();
 
        if (gradientTo != null) {
            // Gradient (EXE) button: nudge both stops brighter as hoverProgress rises.
            Color from = blend(baseBg, lighten(baseBg, 30), hoverProgress);
            Color to = blend(gradientTo, lighten(gradientTo, 20), hoverProgress);
            g2.setPaint(new GradientPaint(0, 0, from, getWidth(), 0, to));
        } else {
            Color fill = active ? Theme.GOLD_BRIGHT : baseBg;
            if (enabled && hoverProgress > 0f) {
                fill = blend(fill, lighten(fill, 34), hoverProgress);
            }
            g2.setColor(enabled ? fill : new Color(30, 31, 36));
        }
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arcUse, arcUse);
 
        if (borderColor != null) {
            Color border = isEnabled() ? borderColor : new Color(50, 50, 55);
            if (enabled && hoverProgress > 0f) border = blend(border, Theme.GOLD_BRIGHT, hoverProgress * 0.6f);
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arcUse, arcUse);
        }
        if (cornerLeftText != null || cornerRightText != null) {
            Font cornerFont = new Font("SansSerif", Font.BOLD, Math.max(8, getHeight() / 5));
            g2.setFont(cornerFont);
            FontMetrics fm = g2.getFontMetrics();
            int pad = 4;
            int baseline = fm.getAscent() + 1;
            if (cornerLeftText != null) {
                g2.setColor(cornerLeftColor);
                g2.drawString(cornerLeftText, pad, baseline);
            }
            if (cornerRightText != null) {
                g2.setColor(cornerRightColor);
                int textWidth = fm.stringWidth(cornerRightText);
                g2.drawString(cornerRightText, getWidth() - textWidth - pad, baseline);
            }
        }
        g2.dispose();
        setForeground(!isEnabled() ? Theme.TEXT_DIM : (active ? Color.BLACK : baseFg));
        super.paintComponent(g);
    }
}