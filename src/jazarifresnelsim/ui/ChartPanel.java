package jazarifresnelsim.ui;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.List;

/**
 * Lightweight, dependency-free XY chart panel (line / scatter / marker
 * series, optional log-x axis, legend). Draws with plain Java2D so no
 * external charting library is required.
 *
 * Intended for reproducing the manuscript's figures inline in the
 * Manuscript Validation panel, next to the numeric console output.
 *
 * @author JazariFresnelSim validation tooling
 */
public class ChartPanel extends javax.swing.JPanel {

    public enum Style { LINE, SCATTER, LINE_MARKER, STAR }

    public static class Series {
        public final String name;
        public final Color color;
        public final double[] xs;
        public final double[] ys;
        public final Style style;

        public Series(String name, Color color, double[] xs, double[] ys, Style style) {
            this.name = name;
            this.color = color;
            this.xs = xs;
            this.ys = ys;
            this.style = style;
        }
    }

    public static class Spec {
        public String title = "";
        public String xLabel = "";
        public String yLabel = "";
        public boolean logX = false;
        public double yMin = Double.NaN, yMax = Double.NaN;   // auto if NaN
        public double xMin = Double.NaN, xMax = Double.NaN;   // auto if NaN
        public List<Series> series;
        public boolean showLegend = true;
    }

    private final Spec spec;
    private static final Color BG = new Color(11, 14, 20);
    private static final Color AXIS = new Color(160, 170, 180);
    private static final Color GRID = new Color(35, 42, 54);
    private static final Color TEXT = new Color(220, 224, 230);

    public ChartPanel(Spec spec) {
        this.spec = spec;
        setBackground(BG);
        setPreferredSize(new Dimension(420, 320));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int left = 58, right = 16, top = spec.title.isEmpty() ? 10 : 30, bottom = 44;
        if (spec.showLegend && spec.series != null) {
            bottom += 18;
        }
        int plotW = Math.max(10, w - left - right);
        int plotH = Math.max(10, h - top - bottom);

        // --- data bounds ---
        double xMin = spec.xMin, xMax = spec.xMax, yMin = spec.yMin, yMax = spec.yMax;
        if (spec.series != null && !spec.series.isEmpty()) {
            if (Double.isNaN(xMin) || Double.isNaN(xMax)) {
                double lo = Double.POSITIVE_INFINITY, hi = Double.NEGATIVE_INFINITY;
                for (Series s : spec.series) {
                    for (double x : s.xs) {
                        if (spec.logX && x <= 0) continue;
                        lo = Math.min(lo, x);
                        hi = Math.max(hi, x);
                    }
                }
                if (Double.isFinite(lo)) { xMin = lo; xMax = hi; }
            }
            if (Double.isNaN(yMin) || Double.isNaN(yMax)) {
                double lo = Double.POSITIVE_INFINITY, hi = Double.NEGATIVE_INFINITY;
                for (Series s : spec.series) {
                    for (double y : s.ys) { lo = Math.min(lo, y); hi = Math.max(hi, y); }
                }
                if (Double.isFinite(lo)) {
                    double pad = (hi - lo) * 0.08;
                    if (pad == 0) pad = Math.abs(hi) * 0.1 + 1;
                    yMin = lo - pad; yMax = hi + pad;
                }
            }
        }
        if (Double.isNaN(xMin)) { xMin = 0; xMax = 1; }
        if (Double.isNaN(yMin)) { yMin = 0; yMax = 1; }
        final double fxMin = xMin, fxMax = xMax, fyMin = yMin, fyMax = yMax;

        double logXMin = spec.logX ? Math.log10(Math.max(fxMin, 1e-12)) : 0;
        double logXMax = spec.logX ? Math.log10(Math.max(fxMax, 1e-12)) : 0;

        // --- transforms ---
        java.util.function.DoubleUnaryOperator toPx = x -> {
            double t;
            if (spec.logX) {
                double lx = Math.log10(Math.max(x, 1e-12));
                t = (lx - logXMin) / Math.max(1e-12, (logXMax - logXMin));
            } else {
                t = (x - fxMin) / Math.max(1e-12, (fxMax - fxMin));
            }
            return left + t * plotW;
        };
        java.util.function.DoubleUnaryOperator toPy = y -> {
            double t = (y - fyMin) / Math.max(1e-12, (fyMax - fyMin));
            return top + plotH - t * plotH;
        };

        // --- gridlines + axes ---
        g2.setColor(GRID);
        int nGrid = 5;
        for (int i = 0; i <= nGrid; i++) {
            double gy = top + plotH * i / (double) nGrid;
            g2.draw(new Line2D.Double(left, gy, left + plotW, gy));
        }
        g2.setColor(AXIS);
        g2.draw(new Line2D.Double(left, top, left, top + plotH));
        g2.draw(new Line2D.Double(left, top + plotH, left + plotW, top + plotH));

        // --- y tick labels ---
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i <= nGrid; i++) {
            double val = fyMax - (fyMax - fyMin) * i / (double) nGrid;
            String lbl = fmt(val);
            double gy = top + plotH * i / (double) nGrid;
            g2.setColor(TEXT);
            g2.drawString(lbl, left - fm.stringWidth(lbl) - 6, (float) (gy + 3));
        }

        // --- x tick labels ---
        int nXTicks = spec.logX ? (int) Math.round(logXMax - logXMin) + 1 : 5;
        nXTicks = Math.max(2, Math.min(nXTicks, 8));
        for (int i = 0; i <= nXTicks; i++) {
            double val;
            if (spec.logX) {
                double lx = logXMin + (logXMax - logXMin) * i / (double) nXTicks;
                val = Math.pow(10, lx);
            } else {
                val = fxMin + (fxMax - fxMin) * i / (double) nXTicks;
            }
            double gx = toPx.applyAsDouble(val);
            String lbl = fmt(val);
            g2.setColor(TEXT);
            g2.drawString(lbl, (float) (gx - fm.stringWidth(lbl) / 2.0), top + plotH + 16);
        }

        // --- axis labels ---
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.setColor(TEXT);
        FontMetrics fm2 = g2.getFontMetrics();
        g2.drawString(spec.xLabel, left + plotW / 2 - fm2.stringWidth(spec.xLabel) / 2,
                h - (spec.showLegend ? 24 : 8));
        Graphics2D gy2 = (Graphics2D) g2.create();
        gy2.rotate(-Math.PI / 2);
        gy2.drawString(spec.yLabel, -(top + plotH / 2 + fm2.stringWidth(spec.yLabel) / 2), 14);
        gy2.dispose();

        // --- title ---
        if (!spec.title.isEmpty()) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.setColor(TEXT);
            FontMetrics fmt = g2.getFontMetrics();
            g2.drawString(spec.title, left + plotW / 2 - fmt.stringWidth(spec.title) / 2, 18);
        }

        // --- series ---
        if (spec.series != null) {
            for (Series s : spec.series) {
                g2.setColor(s.color);
                if (s.style == Style.LINE || s.style == Style.LINE_MARKER) {
                    Path2D path = new Path2D.Double();
                    boolean first = true;
                    for (int i = 0; i < s.xs.length; i++) {
                        double px = toPx.applyAsDouble(s.xs[i]);
                        double py = toPy.applyAsDouble(s.ys[i]);
                        if (first) { path.moveTo(px, py); first = false; }
                        else path.lineTo(px, py);
                    }
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.draw(path);
                }
                if (s.style == Style.SCATTER || s.style == Style.LINE_MARKER) {
                    for (int i = 0; i < s.xs.length; i++) {
                        double px = toPx.applyAsDouble(s.xs[i]);
                        double py = toPy.applyAsDouble(s.ys[i]);
                        g2.fill(new Ellipse2D.Double(px - 3, py - 3, 6, 6));
                    }
                }
                if (s.style == Style.STAR) {
                    for (int i = 0; i < s.xs.length; i++) {
                        double px = toPx.applyAsDouble(s.xs[i]);
                        double py = toPy.applyAsDouble(s.ys[i]);
                        drawStar(g2, px, py, 7, s.color);
                    }
                }
            }
        }

        // --- legend (skip star markers -- they duplicate a line series) ---
        if (spec.showLegend && spec.series != null && !spec.series.isEmpty()) {
            int lx = left, ly = h - 16;
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            for (Series s : spec.series) {
                if (s.style == Style.STAR) continue;
                g2.setColor(s.color);
                g2.fillRect(lx, ly - 7, 10, 10);
                g2.setColor(TEXT);
                String lbl = s.name;
                g2.drawString(lbl, lx + 14, ly + 2);
                lx += 18 + g2.getFontMetrics().stringWidth(lbl) + 14;
            }
        }
    }

    private void drawStar(Graphics2D g2, double cx, double cy, double r, Color c) {
        Path2D star = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double ang = Math.PI / 2 + i * Math.PI / 5;
            double rr = (i % 2 == 0) ? r : r * 0.42;
            double x = cx + rr * Math.cos(ang);
            double y = cy - rr * Math.sin(ang);
            if (i == 0) star.moveTo(x, y); else star.lineTo(x, y);
        }
        star.closePath();
        g2.setColor(c);
        g2.fill(star);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(0.8f));
        g2.draw(star);
    }

    private static String fmt(double v) {
        if (Math.abs(v) >= 1000) return String.format("%.0f", v);
        if (Math.abs(v) >= 10) return String.format("%.1f", v);
        if (Math.abs(v) >= 0.01 || v == 0) return String.format("%.2f", v);
        return String.format("%.1e", v);
    }
}
