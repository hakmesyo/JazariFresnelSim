package jazarifresnelsim.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Side-by-side (a)/(b) chart layout, matching the two-panel figures used
 * throughout the manuscript (Fig. 2-5).
 */
public class DualChartPanel extends JPanel {

    public DualChartPanel(ChartPanel.Spec left, ChartPanel.Spec right) {
        setLayout(new GridLayout(1, 2, 8, 0));
        setOpaque(false);
        add(new ChartPanel(left));
        add(new ChartPanel(right));
    }

    public DualChartPanel(ChartPanel.Spec single) {
        setLayout(new GridLayout(1, 1));
        setOpaque(false);
        add(new ChartPanel(single));
    }
}
