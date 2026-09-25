import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphCanvas extends JPanel {
   public static final class PlottedFunction {
        public final String text;
        public final Color color;
        public boolean error;
        public PlottedFunction(String text, Color color) {
            this.text = text;
            this.color = color;
        }
    }

    public String inputText = "";
    public boolean useDegrees = false;
     public final List<PlottedFunction> functions = new ArrayList<>();
    public Map<Character, Double> variables = new HashMap<>();

    private static final double DEFAULT_X_MIN = -2 * Math.PI, DEFAULT_X_MAX = 2 * Math.PI;
    private double xMin = DEFAULT_X_MIN, xMax = DEFAULT_X_MAX;
    private static final double MIN_SPAN = 0.02, MAX_SPAN = 2000;

    public GraphCanvas() {
        setBackground(Theme.DISPLAY_BG);
         MouseAdapter panAndReset = new MouseAdapter() {
            private int lastX;
            @Override public void mousePressed(MouseEvent e) { lastX = e.getX(); }
            @Override public void mouseDragged(MouseEvent e) {
                if (getWidth() <= 0) return;
                int dx = e.getX() - lastX;
                lastX = e.getX();
                double dataPerPixel = (xMax - xMin) / getWidth();
                double shift = -dx * dataPerPixel;
                xMin += shift;
                xMax += shift;
                repaint();
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) resetView(); 
            }
        };
        addMouseListener(panAndReset);
        addMouseMotionListener(panAndReset);
        addMouseWheelListener(e -> {
            if (getWidth() <= 0) return;
            double dataPerPixel = (xMax - xMin) / getWidth();
            double xAtCursor = xMin + e.getX() * dataPerPixel;
            double factor = e.getWheelRotation() < 0 ? 0.9 : 1.1; // scroll up/away = zoom in
            double newMin = xAtCursor - (xAtCursor - xMin) * factor;
            double newMax = xAtCursor + (xMax - xAtCursor) * factor;
            double newSpan = newMax - newMin;
            if (newSpan < MIN_SPAN || newSpan > MAX_SPAN) return;
            xMin = newMin;
            xMax = newMax;
            repaint();
        });
    }
    public void resetView() {
        xMin = DEFAULT_X_MIN;
        xMax = DEFAULT_X_MAX;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g2.setColor(Theme.DISPLAY_BG);
        g2.fillRect(0, 0, w, h);

        if (functions.isEmpty()) {
            g2.setFont(new Font("Serif", Font.BOLD, 24));
            g2.setColor(Theme.GOLD_BRIGHT);
            String text = inputText.isEmpty() ? "" : "f(X) = " + inputText;
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, w - fm.stringWidth(text) - 14, h / 2);
            g2.dispose();
            return;
        }
        double[][] allYs = new double[functions.size()][];
        double yMin = Double.POSITIVE_INFINITY, yMax = Double.NEGATIVE_INFINITY;
        boolean anyValid = false;
        for (int i = 0; i < functions.size(); i++) {
            PlottedFunction pf = functions.get(i);
            double[] ys = Evaluator.sampleFunction(pf.text, useDegrees, xMin, xMax, w, variables);
            allYs[i] = ys;
            boolean curveValid = false;
            for (double y : ys) {
                if (Double.isFinite(y)) {
                    yMin = Math.min(yMin, y);
                    yMax = Math.max(yMax, y);
                    curveValid = true;
                }
            }
            pf.error = !curveValid;
            anyValid |= curveValid;
        }

        if (!anyValid) {
            g2.setColor(new Color(230, 120, 110));
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString("Error plotting function", 14, h / 2);
            g2.dispose();
            return;
        }
        if (yMax - yMin < 1e-6) { yMin -= 1; yMax += 1; }
        double pad = (yMax - yMin) * 0.1;
        yMin -= pad; yMax += pad;

        // grid
        g2.setColor(new Color(50, 52, 60));
        for (int gx = 0; gx <= 10; gx++) g2.drawLine(gx * w / 10, 0, gx * w / 10, h);
        for (int gy = 0; gy <= 6; gy++) g2.drawLine(0, gy * h / 6, w, gy * h / 6);

        // axes
        g2.setColor(new Color(110, 105, 90));
        int zeroXpix = (int) ((0 - xMin) / (xMax - xMin) * w);
        int zeroYpix = (int) (h - (0 - yMin) / (yMax - yMin) * h);
        if (zeroXpix >= 0 && zeroXpix <= w) g2.drawLine(zeroXpix, 0, zeroXpix, h);
        if (zeroYpix >= 0 && zeroYpix <= h) g2.drawLine(0, zeroYpix, w, zeroYpix);
        g2.setStroke(new BasicStroke(2.2f));
        for (int i = 0; i < functions.size(); i++) {
            PlottedFunction pf = functions.get(i);
            if (pf.error) continue;
            double[] ys = allYs[i];
            g2.setColor(pf.color);
            Integer prevX = null, prevY = null;
            for (int px = 0; px < w; px++) {
                double y = ys[px];
                if (!Double.isFinite(y)) { prevX = null; continue; }
                int py = (int) (h - (y - yMin) / (yMax - yMin) * h);
                if (prevX != null) g2.drawLine(prevX, prevY, px, py);
                prevX = px; prevY = py;
            }
        }
        g2.setFont(new Font("Serif", Font.PLAIN, 12));
        int ly = 16;
        for (PlottedFunction pf : functions) {
            g2.setColor(pf.error ? Theme.TEXT_DIM : pf.color);
            String label = "f(x) = " + CalcUtils.prettyPrint(pf.text) + (pf.error ? "  (error)" : "");
            g2.drawString(label, 10, ly);
            ly += 15;
        }
        if (!inputText.isEmpty()) {
            g2.setFont(new Font("Serif", Font.BOLD, 16));
            g2.setColor(Theme.GOLD_BRIGHT);
            String typing = "f(X) = " + inputText;
            FontMetrics tfm = g2.getFontMetrics();
            g2.drawString(typing, w - tfm.stringWidth(typing) - 10, h - 10);
        }
        g2.dispose();
    }
}