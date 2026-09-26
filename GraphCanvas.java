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
    public static final class TracePoint {
        public final double x, y;
        public final Color color;
        public TracePoint(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }

    public String inputText = "";
    public TracePoint tracePoint = null;
    public boolean useDegrees = false;
    public final List<PlottedFunction> functions = new ArrayList<>();
    public Map<Character, Double> variables = new HashMap<>();

    private static final double DEFAULT_X_MIN = -2 * Math.PI, DEFAULT_X_MAX = 2 * Math.PI;
    private double xMin = DEFAULT_X_MIN, xMax = DEFAULT_X_MAX;
   private static final double MIN_SPAN = 0.02, MAX_SPAN = 2000;
    private double lastYMin = Double.NaN, lastYMax = Double.NaN;

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
                if (e.getClickCount() == 2) resetView(); // double-click: back to the default window
                else if (e.getClickCount() == 1) handleTraceClick(e.getX(), e.getY());
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
        tracePoint = null;
        repaint();
    }
    private void handleTraceClick(int px, int py) {
        if (functions.isEmpty() || getWidth() <= 0 || !Double.isFinite(lastYMin) || !Double.isFinite(lastYMax)) {
            tracePoint = null;
            repaint();
            return;
        }
        int w = getWidth(), h = getHeight();
        double dataX = xMin + px * (xMax - xMin) / w;

        PlottedFunction nearest = null;
        double nearestY = 0, nearestPixelDist = Double.MAX_VALUE;
        for (PlottedFunction pf : functions) {
            double y;
            try {
                y = new Evaluator(pf.text, useDegrees, dataX, variables).evaluate();
            } catch (Exception ex) {
                continue;
            }
            if (!Double.isFinite(y)) continue;
            int curvePy = (int) (h - (y - lastYMin) / (lastYMax - lastYMin) * h);
            double dist = Math.abs(curvePy - py);
            if (dist < nearestPixelDist) {
                nearestPixelDist = dist;
                nearest = pf;
                nearestY = y;
            }
        }
        tracePoint = (nearest != null && nearestPixelDist <= 40)
            ? new TracePoint(dataX, nearestY, nearest.color)
            : null;
        repaint();
    }
    private static String formatAxisValue(double v) {
        double av = Math.abs(v);
        if (av < 1e-9) return "0";
        int decimals = av < 1 ? 2 : (av < 10 ? 1 : 0);
        String s = String.format("%." + decimals + "f", v);
        return s.equals("-0") ? "0" : s;
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
        lastYMin = yMin;
        lastYMax = yMax;

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
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        g2.setColor(Theme.TEXT_DIM);
        FontMetrics afm = g2.getFontMetrics();
        for (int gx = 0; gx <= 10; gx += 2) {
            String label = formatAxisValue(xMin + gx / 10.0 * (xMax - xMin));
            int lx = gx * w / 10;
            int drawX = Math.max(2, Math.min(w - afm.stringWidth(label) - 2, lx - afm.stringWidth(label) / 2));
            g2.drawString(label, drawX, h - 3);
        }
        for (int gy = 2; gy <= 6; gy += 2) {
            String label = formatAxisValue(yMax - gy / 6.0 * (yMax - yMin));
            int ly0 = gy * h / 6;
            g2.drawString(label, 2, Math.min(h - 2, ly0 + 4));
        }

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
        if (tracePoint != null) {
            int tx = (int) ((tracePoint.x - xMin) / (xMax - xMin) * w);
            int ty = (int) (h - (tracePoint.y - yMin) / (yMax - yMin) * h);
            g2.setColor(Color.WHITE);
            g2.fillOval(tx - 4, ty - 4, 8, 8);
            g2.setColor(tracePoint.color);
            g2.drawOval(tx - 4, ty - 4, 8, 8);

            String coord = "(" + formatAxisValue(tracePoint.x) + ", " + formatAxisValue(tracePoint.y) + ")";
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            FontMetrics cfm = g2.getFontMetrics();
            int cx = tx + 8;
            if (cx + cfm.stringWidth(coord) > w) cx = tx - cfm.stringWidth(coord) - 8;
            int cy = (ty - 8 < 12) ? ty + 20 : ty - 8;
            g2.setColor(Color.WHITE);
            g2.drawString(coord, cx, cy);
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