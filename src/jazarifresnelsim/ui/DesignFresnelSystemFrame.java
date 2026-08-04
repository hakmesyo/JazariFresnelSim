package jazarifresnelsim.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import jazarifresnelsim.domain.ConfigManager;
import jazarifresnelsim.optimization.TestOptimization;
import jazarifresnelsim.optimization.problem.DesignParameters;
import jazarifresnelsim.domain.Constants;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Design Fresnel System window — 1200 × 900.
 *
 * Left panel (900 px): tabbed workspace with manuscript validation tests. Right
 * panel (300 px): simulation parameter preset editor.
 *
 * Each validation button shows the corresponding manuscript Table / Figure
 * reference so reviewers can quickly cross-check outputs.
 *
 * Thermal and economic parameters have been removed from this class in v4.1;
 * JFS is an optical pre-screening tool and those quantities lie outside its
 * scope (see manuscript Section 1.2).
 *
 * @author Yunus Demirtas, Musa Atas — Siirt University
 * @version 4.1
 */
public class DesignFresnelSystemFrame extends JFrame {

    // ----------------------------------------------------------------
    // Color palette
    // ----------------------------------------------------------------
    private static final Color BG_DARK = new Color(11, 14, 20);
    private static final Color BG_PANEL = new Color(21, 27, 38);
    private static final Color ACCENT_ORANGE = new Color(255, 106, 0);
    private static final Color TEXT_PRIMARY = new Color(240, 240, 240);
    private static final Color TEXT_SECONDARY = new Color(160, 170, 180);
    private static final Color BORDER_COLOR = new Color(45, 55, 72);
    private static final Color BTN_DEFAULT = new Color(26, 37, 53);
    private static final Color BTN_HOVER = new Color(45, 63, 85);

    private JTextArea logArea;
    private JProgressBar progressBar;
    private JPanel csvLinkPanel;             // clickable CSV file links
    private JScrollPane presetScrollPane;          // right panel reference
    private JPanel chartContainer;                 // holds the manuscript figure, when applicable
    private final java.util.List<JComponent> presetFields = new java.util.ArrayList<>();

    // ================================================================
    // CONSTRUCTOR
    // ================================================================
    public DesignFresnelSystemFrame() {
        applyFlatLaf();
        buildFrame();
        buildComponents();
    }

    // ================================================================
    // FRAME SETUP
    // ================================================================
    private void applyFlatLaf() {
        FlatDarkLaf.setup();
        UIManager.put("Component.accentColor", ACCENT_ORANGE);
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 6);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Panel.background", BG_PANEL);
        UIManager.put("TabbedPane.background", BG_DARK);
        UIManager.put("TabbedPane.selectedBackground", BG_PANEL);
        UIManager.put("TabbedPane.contentAreaColor", BG_PANEL);
    }

    private void buildFrame() {
        setTitle("Design Fresnel System");
        setSize(1200, 900);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(10, 0));
        ((JComponent) getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    private void buildComponents() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setPreferredSize(new Dimension(900, 860));
        tabs.setBackground(BG_DARK);
        tabs.setForeground(TEXT_PRIMARY);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabs.addTab("Manuscript Validation", buildValidationPanel());

        JPanel designTab = new JPanel(new BorderLayout());
        designTab.setBackground(BG_PANEL);
        JLabel ph = new JLabel("Coming soon: parametric design tools & 2-D preview",
                SwingConstants.CENTER);
        ph.setForeground(Color.GRAY);
        ph.setFont(new Font("Segoe UI", Font.ITALIC, 18));
        designTab.add(ph, BorderLayout.CENTER);
        tabs.addTab("Design New Fresnel System", designTab);

        // Lock preset panel when Manuscript Validation tab is active,
        // unlock when Design tab is selected — prevents accidental edits
        // that would invalidate paper reproducibility.
        tabs.addChangeListener(e -> setPresetEditable(tabs.getSelectedIndex() != 0));

        add(tabs, BorderLayout.CENTER);
        add(buildPresetPanel(), BorderLayout.EAST);

        // Start locked (Manuscript Validation is selected by default)
        setPresetEditable(false);
    }

    // ================================================================
    // VALIDATION PANEL
    // ================================================================
    /**
     * Builds the left panel containing test buttons and log output.
     *
     * Button → manuscript reference mapping (verified against paper_v5,
     * see repository validation log). Tables first in ascending order,
     * then figures in ascending order:
     *   Test 1  Solar Position        → Table 1, Fig. 2
     *   Test 2  Tracking Solver       → Table 2
     *   Test 3  G1-G5 vs SolTrace     → Table 3-4
     *   Test 4  Mirror Count Scaling  → Table 6
     *   Test 5  p/w Sweep             → Table 7, Fig. 3
     *   Test 6  Metaheuristic Opt.    → Table 10 (Cg >= 20 hard constraint)
     *   Test 7  Temporal Discret.     → Table 11
     *   Test 8  Hr/Wf Scaling         → Table 8, Fig. 4
     *   Test 9  Well-posedness        → Fig. 5
     *   Test 10 Sensitivity Analysis  → supplementary (rho_m, sigma_opt), not tied
     *                                    to a manuscript table/figure -- reviewer
     *                                    response material
     *   Test 11 Low-Sun-Angle Val.    → supplementary (G2, six hours), reviewer
     *                                    response material
     *   Test 0  Run All Tests         → runs Tests 1-9, in order (Tests 10-11 are
     *                                    separate; run them individually)
     *
     * The manuscript has five figures total. Fig. 1 (field geometry and
     * nomenclature) is a static schematic, not a computed result, and has
     * no corresponding button.
     *
     * Table 5 and Table 9 have no dedicated button because neither is an
     * independent computation: Table 5 ("Three-layer verification
     * hierarchy") is a summary assembled from the results of Tests 1-3;
     * Table 9 ("Metaheuristic hyperparameters") documents the fixed PSO/
     * GA/SA settings already visible in buildGA()/buildPSO()/buildSA()
     * below, not a result to be reproduced.
     *
     * Four earlier tests were withdrawn: they did not correspond to any
     * table or figure in the manuscript (exploratory extreme-angle
     * analysis; exploratory daily-profile / convergence exports from an
     * earlier draft; a superseded ranking test with a circular metric).
     * Their underlying methods remain in TestOptimization for reference
     * but are no longer wired to a button or a test-ID case, to avoid
     * reviewer confusion when cross-checking this tool against the
     * manuscript.
     */
    private JPanel buildValidationPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(BG_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 2-column grid — 9 test buttons (last row has one button, left cell)
        JPanel grid = new JPanel(new GridLayout(0, 2, 10, 10));
        grid.setOpaque(false);

        Object[][] defs = {
            {"Test 1 · Solar Position", "Table 1, Fig. 2 · vs NREL SPA (pvlib)", 1},
            {"Test 2 · Tracking Solver", "Table 2 · reflection-law residual", 2},
            {"Test 3 · G1–G5 vs SolTrace", "Table 3-4 · MCRT validation", 3},
            {"Test 4 · Mirror Count Scaling", "Table 6", 4},
            {"Test 5 · p/w Sweep", "Table 7, Fig. 3 · 3 sites x 4 seasons", 5},
            {"Test 6 · Metaheuristic Opt.", "Table 10 · Cg≥20", 6},
            {"Test 7 · Temporal Discret.", "Table 11 · resolution sensitivity", 7},
            {"Test 8 · Hr/Wf Scaling", "Table 8, Fig. 4 · dimensionless height", 8},
            {"Test 9 · Well-posedness", "Fig. 5 · J(N), two modes", 9},
            {"Test 10 · Sensitivity Analysis", "ρ_m, σ_opt — supplementary", 10},
            {"Test 11 · Low-Sun-Angle Val.", "G2, 6 hours — supplementary", 11},
        };

        for (Object[] d : defs) {
            grid.add(makeBtn((String) d[0], (String) d[1], (Integer) d[2], false));
        }

        // Run All Tests — full-width green button
        JPanel runAll = new JPanel(new GridLayout(1, 1));
        runAll.setOpaque(false);
        runAll.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        runAll.add(makeBtn("Run All Tests",
                "Tests 1–9  ·  complete validation suite", 0, true));

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(grid, BorderLayout.CENTER);
        top.add(runAll, BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);

        // Log output area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(BG_DARK);
        logArea.setForeground(new Color(74, 222, 128));
        // Use a font with good Unicode coverage to avoid garbled characters
        logArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        logArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        logArea.setLineWrap(false);
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setBackground(BG_DARK);
        progressBar.setForeground(ACCENT_ORANGE);
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        progressBar.setPreferredSize(new Dimension(0, 22));

        // CSV links panel — populated after tests produce output files
        csvLinkPanel = new JPanel();
        csvLinkPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 4));
        csvLinkPanel.setBackground(new Color(15, 20, 30));
        csvLinkPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        csvLinkPanel.setPreferredSize(new Dimension(0, 32));
        csvLinkPanel.setVisible(false);

        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);

        // Chart area — shown only for tests that produce a manuscript figure
        chartContainer = new JPanel(new BorderLayout());
        chartContainer.setOpaque(false);
        chartContainer.setPreferredSize(new Dimension(0, 260));
        chartContainer.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        chartContainer.setVisible(false);
        bottom.add(chartContainer, BorderLayout.NORTH);

        bottom.add(scroll, BorderLayout.CENTER);

        // Stack csvLinkPanel + progressBar at the bottom
        JPanel statusBar = new JPanel(new BorderLayout(0, 3));
        statusBar.setOpaque(false);
        statusBar.add(csvLinkPanel, BorderLayout.CENTER);
        statusBar.add(progressBar, BorderLayout.SOUTH);

        bottom.add(statusBar, BorderLayout.SOUTH);
        panel.add(bottom, BorderLayout.CENTER);

        return panel;
    }

    // ================================================================
    // PRESET PANEL (right side) — geometry + optimization bounds only
    // ================================================================
    private JScrollPane buildPresetPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 12, 15, 12));

        ConfigManager.loadConfig();

        // --- Fixed Geometry ---
        // D_r and L are fixed design inputs, not optimization variables.
        // D_r rationale: optimal value requires coupled optical-thermal analysis.
        JPanel geomGroup = makeGroup("Fixed Geometry");
        JTextField lenField = makeField(String.valueOf(
                ConfigManager.getDouble("mirror_length_cm", 200.0)));
        JTextField drField = makeField(String.valueOf(
                ConfigManager.getDouble("receiver_diameter_cm", 16.0)));
        geomGroup.add(makeRow("Mirror Length L (cm):", lenField));
        geomGroup.add(makeRow("Receiver Diameter Dr (cm) — fixed:", drField));
        panel.add(geomGroup);
        panel.add(Box.createVerticalStrut(15));

        // --- Optimization Bounds (4 variables: Hr, w, p, N) ---
        JPanel boundsGroup = makeGroup("Optimization Search Bounds (Min – Max)");
        JTextField minMirrors = makeField(String.valueOf(ConfigManager.getInt("min_mirrors", 4)));
        JTextField maxMirrors = makeField(String.valueOf(ConfigManager.getInt("max_mirrors", 50)));
        JTextField minRecH = makeField(String.valueOf(ConfigManager.getDouble("min_rec_height", 50.0)));
        JTextField maxRecH = makeField(String.valueOf(ConfigManager.getDouble("max_rec_height", 600.0)));
        JTextField minMirW = makeField(String.valueOf(ConfigManager.getDouble("min_mirror_width", 5.0)));
        JTextField maxMirW = makeField(String.valueOf(ConfigManager.getDouble("max_mirror_width", 40.0)));
        JTextField minMirS = makeField(String.valueOf(ConfigManager.getDouble("min_mirror_spacing", 15.0)));
        JTextField maxMirS = makeField(String.valueOf(ConfigManager.getDouble("max_mirror_spacing", 150.0)));

        boundsGroup.add(makeMinMax("Number of Mirrors N:", minMirrors, maxMirrors));
        boundsGroup.add(makeMinMax("Receiver Height Hr (cm):", minRecH, maxRecH));
        boundsGroup.add(makeMinMax("Mirror Width w (cm):", minMirW, maxMirW));
        boundsGroup.add(makeMinMax("Mirror Spacing p (cm):", minMirS, maxMirS));
        panel.add(boundsGroup);

        panel.add(Box.createVerticalGlue());

        // Apply button
        JButton applyBtn = new JButton("Apply & Save Settings");
        applyBtn.setBackground(ACCENT_ORANGE);
        applyBtn.setForeground(Color.BLACK);
        applyBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        applyBtn.setFocusPainted(false);
        applyBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        applyBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        applyBtn.setMaximumSize(new Dimension(260, 45));

        applyBtn.addActionListener(e -> {
            ConfigManager.setProperty("mirror_length_cm", lenField.getText());
            ConfigManager.setProperty("receiver_diameter_cm", drField.getText());
            ConfigManager.setProperty("min_mirrors", minMirrors.getText());
            ConfigManager.setProperty("max_mirrors", maxMirrors.getText());
            ConfigManager.setProperty("min_rec_height", minRecH.getText());
            ConfigManager.setProperty("max_rec_height", maxRecH.getText());
            ConfigManager.setProperty("min_mirror_width", minMirW.getText());
            ConfigManager.setProperty("max_mirror_width", maxMirW.getText());
            ConfigManager.setProperty("min_mirror_spacing", minMirS.getText());
            ConfigManager.setProperty("max_mirror_spacing", maxMirS.getText());
            ConfigManager.saveConfig();
            DesignParameters.updateBoundsFromConfig();
            Constants.updateFromConfig();
            JOptionPane.showMessageDialog(this,
                    "Settings saved and applied to simulation engine.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        panel.add(Box.createVerticalStrut(10));
        panel.add(applyBtn);
        panel.add(Box.createVerticalStrut(5));

        JScrollPane sp = new JScrollPane(panel);
        sp.setPreferredSize(new Dimension(300, 860));
        sp.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        presetScrollPane = sp;
        return sp;
    }

    // ================================================================
    // BUTTON FACTORY
    // ================================================================
    /**
     * Creates a two-line styled test button.
     *
     * @param title Bold main label (top line)
     * @param reference Paper Table/Figure reference (bottom line, blue)
     * @param testId Passed to runTest()
     * @param green If true, uses green accent (Run All Tests)
     */
    private JButton makeBtn(String title, String reference,
            int testId, boolean green) {
        String refColor = green ? "#4ADE80" : "#38BDF8";
        String html = "<html><center>"
                + "<b style='font-size:12px'>" + title + "</b><br>"
                + "<span style='font-size:10px;color:" + refColor + "'>"
                + reference + "</span>"
                + "</center></html>";

        JButton btn = new JButton(html);
        Color bgNormal = green ? new Color(20, 45, 20) : BTN_DEFAULT;
        Color bgHover = green ? new Color(30, 70, 30) : BTN_HOVER;

        btn.setBackground(bgNormal);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(
                green ? new Color(45, 90, 45) : BORDER_COLOR));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 54));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bgHover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bgNormal);
            }
        });
        btn.addActionListener(e -> runTest(testId));
        return btn;
    }

    // ================================================================
    // PRESET LOCK / UNLOCK
    // ================================================================
    /**
     * Locks (editable=false) or unlocks (editable=true) all preset fields.
     *
     * Called by the tab ChangeListener: - Manuscript Validation tab selected →
     * locked (grey, read-only) - Design New Fresnel System tab selected →
     * unlocked (editable)
     *
     * Locking prevents accidental parameter changes that would break
     * reproducibility of the manuscript validation results.
     */
    private void setPresetEditable(boolean editable) {
        for (JComponent c : presetFields) {
            c.setEnabled(editable);
            if (c instanceof JTextField tf) {
                tf.setBackground(editable ? BG_DARK : new Color(18, 22, 30));
                tf.setForeground(editable ? TEXT_PRIMARY : new Color(100, 110, 120));
            }
        }
        // Apply button follows the same lock state
        if (presetScrollPane != null) {
            findApplyButton(presetScrollPane).ifPresent(b -> b.setEnabled(editable));
        }
    }

    private java.util.Optional<JButton> findApplyButton(java.awt.Container container) {
        for (java.awt.Component c : container.getComponents()) {
            if (c instanceof JButton btn && btn.getText().contains("Apply")) {
                return java.util.Optional.of(btn);
            }
            if (c instanceof java.awt.Container sub) {
                var found = findApplyButton(sub);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return java.util.Optional.empty();
    }

    // ================================================================
    // UI COMPONENT HELPERS
    // ================================================================
    private JPanel makeGroup(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        TitledBorder b = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR), title);
        b.setTitleColor(TEXT_SECONDARY);
        b.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        p.setBorder(BorderFactory.createCompoundBorder(b,
                BorderFactory.createEmptyBorder(10, 8, 10, 8)));
        p.setAlignmentX(Component.CENTER_ALIGNMENT);
        return p;
    }

    private JPanel makeRow(String label, JTextField field) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(260, 50));
        p.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel l = new JLabel(label);
        l.setForeground(TEXT_PRIMARY);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setMaximumSize(new Dimension(260, 28));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(l);
        p.add(Box.createVerticalStrut(3));
        p.add(field);
        p.add(Box.createVerticalStrut(8));
        return p;
    }

    private JPanel makeMinMax(String label, JTextField minF, JTextField maxF) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(260, 75));
        p.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setForeground(TEXT_PRIMARY);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lbl);
        p.add(Box.createVerticalStrut(3));

        JPanel row = new JPanel(new GridLayout(1, 2, 8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(260, 28));

        row.add(labeled("Min:", minF));
        row.add(labeled("Max:", maxF));
        p.add(row);
        p.add(Box.createVerticalStrut(8));
        return p;
    }

    private JPanel labeled(String label, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(5, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setForeground(TEXT_SECONDARY);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        p.add(l, BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JTextField makeField(String text) {
        JTextField f = new JTextField(text);
        f.setBackground(BG_DARK);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(TEXT_PRIMARY);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        presetFields.add(f);   // register for lock/unlock
        return f;
    }

    // ================================================================
    // TEST RUNNER
    // ================================================================
    private void runTest(int testId) {
        logArea.setText("");
        csvLinkPanel.removeAll();
        csvLinkPanel.setVisible(false);
        progressBar.setIndeterminate(true);
        progressBar.setString("Running " + (testId == 0 ? "all tests" : "Test " + testId) + "...");

        SwingWorker<Void, String> worker = new SwingWorker<>() {

            // Buffer to handle \r (carriage return) — convert to \n for JTextArea
            private final StringBuilder lineBuffer = new StringBuilder();
            private ChartPanel.Spec[] chartSpecs = null;

            @Override
            protected Void doInBackground() {
                PrintStream ps = new PrintStream(new OutputStream() {
                    @Override
                    public void write(int b) {
                        char c = (char) b;
                        if (c == '\r') {
                            // Carriage return: replace current line (used by run progress)
                            // Convert to newline so each run appears on its own line
                            publish("\n");
                        } else {
                            publish(String.valueOf(c));
                        }
                    }
                }, true, java.nio.charset.StandardCharsets.UTF_8);

                PrintStream oldOut = System.out;
                System.setOut(ps);
                try {
                    TestOptimization.runSelectedTest(testId);
                    chartSpecs = computeChartSpecs(testId);
                } catch (Exception ex) {
                    publish("\n[ERROR] " + ex.getMessage() + "\n");
                    ex.printStackTrace();
                } finally {
                    System.setOut(oldOut);
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String s : chunks) {
                    logArea.append(s);
                }
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }

            @Override
            protected void done() {
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                progressBar.setString("Completed");
                // Scan log for "Saved: *.csv" lines and add clickable links
                scanAndAddCsvLinks(logArea.getText());

                chartContainer.removeAll();
                if (chartSpecs != null && chartSpecs.length == 2) {
                    chartContainer.add(new DualChartPanel(chartSpecs[0], chartSpecs[1]),
                            BorderLayout.CENTER);
                    chartContainer.setVisible(true);
                } else {
                    chartContainer.setVisible(false);
                }
                chartContainer.revalidate();
                chartContainer.repaint();
            }
        };
        worker.execute();
    }

    /**
     * Maps a test ID to its manuscript figure, if it has one.
     * Test 1 -> Fig. 2, Test 5 -> Fig. 3, Test 8 -> Fig. 4, Test 9 -> Fig. 5.
     * Tests without a figure (2, 3, 4, 6, 7) and "Run All" (0) return null.
     */
    private ChartPanel.Spec[] computeChartSpecs(int testId) {
        return switch (testId) {
            case 1 -> TestOptimization.getFig2ChartData();
            case 5 -> TestOptimization.getFig3ChartData();
            case 8 -> TestOptimization.getFig4ChartData();
            case 9 -> TestOptimization.getFig5ChartData();
            default -> null;
        };
    }

    // ================================================================
    // CSV LINK DETECTION
    // ================================================================
    /**
     * Scans the log output for "Saved: filename.csv" lines and adds a clickable
     * hyperlink button for each file found.
     */
    private void scanAndAddCsvLinks(String logText) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "Saved:\\s*(\\S+\\.csv)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(logText);

        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        while (matcher.find()) {
            seen.add(matcher.group(1));
        }

        if (seen.isEmpty()) {
            return;
        }

        // Header label
        JLabel header = new JLabel("CSV outputs:");
        header.setForeground(TEXT_SECONDARY);
        header.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        csvLinkPanel.add(header);

        for (String filename : seen) {
            java.io.File file = new java.io.File(filename);
            JButton link = new JButton(filename);
            link.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            link.setForeground(new Color(56, 189, 248));  // light blue
            link.setBackground(new Color(20, 30, 45));
            link.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(40, 60, 80)),
                    BorderFactory.createEmptyBorder(2, 8, 2, 8)));
            link.setCursor(new Cursor(Cursor.HAND_CURSOR));
            link.setFocusPainted(false);
            link.setToolTipText("Click to reveal in folder: " + file.getAbsolutePath());

            link.addActionListener(e -> new Thread(() -> {
                try {
                    java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                    if (!file.exists()) {
                        SwingUtilities.invokeLater(()
                                -> JOptionPane.showMessageDialog(this,
                                        "File not found:\n" + file.getAbsolutePath(),
                                        "File Not Found", JOptionPane.WARNING_MESSAGE));
                        return;
                    }

                    String os = System.getProperty("os.name").toLowerCase();

                    if (os.contains("win")) {
                        // Windows: Explorer opens and selects the file
                        Runtime.getRuntime().exec(new String[]{
                            "explorer.exe", "/select,",
                            file.getAbsolutePath()});

                    } else if (os.contains("mac")) {
                        // macOS: Finder opens and selects the file
                        Runtime.getRuntime().exec(new String[]{
                            "open", "-R", file.getAbsolutePath()});

                    } else {
                        // Linux: open the containing folder (xdg-open)
                        // Most file managers (Nautilus, Dolphin, Thunar)
                        // will open the directory; user selects the file.
                        Runtime.getRuntime().exec(new String[]{
                            "xdg-open", file.getParentFile().getAbsolutePath()});
                    }

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(()
                            -> JOptionPane.showMessageDialog(this,
                                    "Cannot open folder:\n" + ex.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE));
                }
            }, "csv-reveal-" + filename).start());
            csvLinkPanel.add(link);
        }

        csvLinkPanel.setVisible(true);
        csvLinkPanel.revalidate();
        csvLinkPanel.repaint();
    }
}