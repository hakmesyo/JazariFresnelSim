package jazarifresnelsim;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import processing.core.PApplet;

/**
 * Modern launcher window for JazariFresnelSim.
 * Provides two entry points: Interactive 3D Simulator and Terminal Test Suite.
 * 
 * This is the main entry point when running the JAR file.
 * 
 * @author Yunus Demirtaş, Musa Ataş — Siirt University
 * @version 2.1
 */
public class JazariLauncher extends JFrame {

    // Color palette
    private static final Color BG_DARK = new Color(15, 23, 42);
    private static final Color BG_CARD = new Color(30, 41, 59);
    private static final Color ACCENT_ORANGE = new Color(249, 115, 22);
    private static final Color ACCENT_BLUE = new Color(56, 189, 248);
    private static final Color ACCENT_GREEN = new Color(34, 197, 94);
    private static final Color TEXT_PRIMARY = new Color(248, 250, 252);
    private static final Color TEXT_SECONDARY = new Color(148, 163, 184);
    private static final Color BORDER_COLOR = new Color(51, 65, 85);

    public JazariLauncher() {
        setTitle("JazariFresnelSim");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(560, 520);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        // Main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(BG_DARK);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // --- Header ---
        JLabel sunIcon = new JLabel("\u2600", SwingConstants.CENTER);
        sunIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        sunIcon.setForeground(ACCENT_ORANGE);
        sunIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(sunIcon);
        mainPanel.add(Box.createVerticalStrut(8));

        JLabel title = new JLabel("JazariFresnelSim", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(title);
        mainPanel.add(Box.createVerticalStrut(4));

        JLabel subtitle = new JLabel("Linear Fresnel Reflector Simulation & Optimization", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_SECONDARY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(subtitle);
        mainPanel.add(Box.createVerticalStrut(6));

        // Version + Paper badge
        JLabel version = new JLabel("v2.1  \u2022  Solar Energy (Elsevier) 2026", SwingConstants.CENTER);
        version.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        version.setForeground(new Color(100, 116, 139));
        version.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(version);
        mainPanel.add(Box.createVerticalStrut(28));

        // --- Separator ---
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_COLOR);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        mainPanel.add(sep);
        mainPanel.add(Box.createVerticalStrut(28));

        // --- 3D Simulator Button ---
        JPanel simButton = createLaunchButton(
                "\uD83C\uDF1E",  // 🌞
                "Interactive 3D Simulator",
                "Real-time mirror tracking, ray visualization, and live performance metrics",
                ACCENT_ORANGE,
                e -> launch3DSimulator()
        );
        mainPanel.add(simButton);
        mainPanel.add(Box.createVerticalStrut(16));

        // --- Terminal Tests Button ---
        JPanel termButton = createLaunchButton(
                "\uD83D\uDCCA",  // 📊
                "Validation & Optimization Tests",
                "Reproduce all paper results: parametric sweeps, optimization, convergence data",
                ACCENT_BLUE,
                e -> launchTerminalTests()
        );
        mainPanel.add(termButton);
        mainPanel.add(Box.createVerticalStrut(28));

        // --- Footer ---
        JSeparator sep2 = new JSeparator();
        sep2.setForeground(BORDER_COLOR);
        sep2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        mainPanel.add(sep2);
        mainPanel.add(Box.createVerticalStrut(12));

        JLabel footer = new JLabel(
                "<html><center>Siirt University \u2022 Department of Computer & Mechanical Engineering<br>" +
                "<font color='#64748B'>MIT License \u2022 github.com/hakmesyo/JazariFresnelSim</font></center></html>",
                SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.setForeground(TEXT_SECONDARY);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(footer);

        add(mainPanel, BorderLayout.CENTER);

        // Set icon if available
        try {
            setIconImage(new ImageIcon(getClass().getResource("/icon.png")).getImage());
        } catch (Exception ignored) {}
    }

    /**
     * Creates a styled launch button card.
     */
    private JPanel createLaunchButton(String emoji, String titleText, String descText,
                                       Color accentColor, ActionListener action) {
        JPanel card = new JPanel() {
            private boolean hovered = false;

            {
                setOpaque(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaint();
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "click"));
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Background
                Color bg = hovered ? new Color(40, 52, 72) : BG_CARD;
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));

                // Left accent bar
                g2.setColor(accentColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, 4, getHeight(), 4, 4));

                // Border
                g2.setColor(hovered ? accentColor : BORDER_COLOR);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16));

                g2.dispose();
                super.paintComponent(g);
            }
        };

        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        card.setPreferredSize(new Dimension(480, 80));

        // Emoji icon
        JLabel icon = new JLabel(emoji);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        icon.setForeground(accentColor);
        icon.setPreferredSize(new Dimension(48, 48));
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(icon, BorderLayout.WEST);

        // Text panel
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(titleText);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRIMARY);
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(4));

        JLabel descLabel = new JLabel(descText);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setForeground(TEXT_SECONDARY);
        textPanel.add(descLabel);

        card.add(textPanel, BorderLayout.CENTER);

        // Arrow
        JLabel arrow = new JLabel("\u276F"); // ❯
        arrow.setFont(new Font("Segoe UI", Font.BOLD, 20));
        arrow.setForeground(TEXT_SECONDARY);
        arrow.setHorizontalAlignment(SwingConstants.CENTER);
        arrow.setPreferredSize(new Dimension(30, 48));
        card.add(arrow, BorderLayout.EAST);

        return card;
    }

    /**
     * Launches the Processing-based 3D interactive simulator.
     * Uses a separate process with correct native library paths to avoid JOGL DLL issues.
     */
    private void launch3DSimulator() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            try {
                // Get paths
                String javaHome = System.getProperty("java.home");
                String javaBin = javaHome + java.io.File.separator + "bin" + java.io.File.separator + "java";
                java.io.File jarFile = new java.io.File(
                        JazariLauncher.class.getProtectionDomain()
                                .getCodeSource().getLocation().toURI());
                String jarPath = jarFile.getAbsolutePath();
                String jarDir = jarFile.getParent();

                // Build classpath: JAR + all lib JARs
                StringBuilder cp = new StringBuilder(jarPath);
                java.io.File libDir = new java.io.File(jarDir, "lib");
                if (libDir.exists() && libDir.isDirectory()) {
                    for (java.io.File f : libDir.listFiles()) {
                        if (f.getName().endsWith(".jar")) {
                            cp.append(java.io.File.pathSeparator).append(f.getAbsolutePath());
                        }
                    }
                }

                // Find native libraries directory
                String nativePath = jarDir + java.io.File.separator + "natives" 
                        + java.io.File.separator + "windows-amd64";
                java.io.File nativeDir = new java.io.File(nativePath);
                if (!nativeDir.exists()) {
                    // Try alternative locations
                    nativeDir = new java.io.File(jarDir, "natives");
                    if (!nativeDir.exists()) {
                        nativeDir = new java.io.File(jarDir, "lib");
                    }
                    nativePath = nativeDir.getAbsolutePath();
                }

                // Build the command
                ProcessBuilder pb = new ProcessBuilder(
                        javaBin,
                        "-Djava.library.path=" + nativePath,
                        "-cp", cp.toString(),
                        "jazarifresnelsim.FresnelSimulator"
                );
                pb.directory(new java.io.File(jarDir));
                pb.inheritIO();
                pb.start();

                // Minimize launcher
                SwingUtilities.invokeLater(() -> setState(Frame.ICONIFIED));

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    // Fallback: try launching in-process (works when run from IDE)
                    try {
                        PApplet.main(new String[]{FresnelSimulator.class.getName()});
                        setState(Frame.ICONIFIED);
                    } catch (Exception ex2) {
                        JOptionPane.showMessageDialog(this,
                                "Error launching 3D Simulator:\n" + ex2.getMessage()
                                + "\n\nMake sure the 'natives' and 'lib' folders are"
                                + "\nin the same directory as the JAR file.",
                                "Launch Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } finally {
                SwingUtilities.invokeLater(() -> setCursor(Cursor.getDefaultCursor()));
            }
        }).start();
    }

    /**
     * Launches the terminal-based test suite in a new console window.
     */
    private void launchTerminalTests() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                
                // Get java executable path (handle spaces)
                String javaHome = System.getProperty("java.home");
                String javaBin = javaHome + java.io.File.separator + "bin" + java.io.File.separator + "java";
                
                // Get JAR/classpath location (handle URI encoding and spaces)
                java.io.File jarFile = new java.io.File(
                    JazariLauncher.class.getProtectionDomain()
                        .getCodeSource().getLocation().toURI());
                String jarPath = jarFile.getAbsolutePath();

                ProcessBuilder pb;
                if (os.contains("win")) {
                    // Windows: write a temp batch file to avoid quoting hell
                    java.io.File bat = java.io.File.createTempFile("jazari_run_", ".bat");
                    bat.deleteOnExit();
                    try (java.io.PrintWriter pw = new java.io.PrintWriter(bat)) {
                        pw.println("@echo off");
                        pw.println("title JazariFresnelSim - Test Suite");
                        pw.println("\"" + javaBin + "\" -cp \"" + jarPath + "\" jazarifresnelsim.optimization.TestOptimization");
                        pw.println("pause");
                    }
                    pb = new ProcessBuilder("cmd", "/c", "start", bat.getAbsolutePath());
                } else if (os.contains("mac")) {
                    // macOS: open Terminal.app
                    String cmd = "'" + javaBin + "' -cp '" + jarPath + "' jazarifresnelsim.optimization.TestOptimization";
                    pb = new ProcessBuilder("osascript", "-e",
                            "tell application \"Terminal\" to do script \"" + cmd + "\"");
                } else {
                    // Linux: try common terminal emulators
                    String cmd = "'" + javaBin + "' -cp '" + jarPath + "' jazarifresnelsim.optimization.TestOptimization; read -p 'Press Enter to close...'";
                    pb = new ProcessBuilder("bash", "-c",
                            "x-terminal-emulator -e bash -c \"" + cmd + "\" 2>/dev/null || " +
                            "gnome-terminal -- bash -c \"" + cmd + "\" 2>/dev/null || " +
                            "xterm -e bash -c \"" + cmd + "\" 2>/dev/null");
                }

                pb.start();

            } catch (Exception ex) {
                // Fallback: run in embedded console dialog
                SwingUtilities.invokeLater(() -> {
                    int choice = JOptionPane.showConfirmDialog(this,
                            "Could not open external terminal.\nRun tests in this window instead?",
                            "Terminal Not Found", JOptionPane.YES_NO_OPTION);
                    if (choice == JOptionPane.YES_OPTION) {
                        runTestsEmbedded();
                    }
                });
            } finally {
                SwingUtilities.invokeLater(() -> setCursor(Cursor.getDefaultCursor()));
            }
        }).start();
    }

    /**
     * Fallback: runs TestOptimization.main() directly in the current JVM.
     * Output goes to System.out (visible in the console that launched the JAR).
     */
    private void runTestsEmbedded() {
        new Thread(() -> {
            try {
                System.out.println("\n=== Running TestOptimization in embedded mode ===\n");
                jazarifresnelsim.optimization.TestOptimization.main(new String[]{});
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    /**
     * Main entry point for the JAR file.
     */
    public static void main(String[] args) {
        // Check for CLI mode flag
        if (args.length > 0 && (args[0].equals("--cli") || args[0].equals("--terminal"))) {
            jazarifresnelsim.optimization.TestOptimization.main(new String[]{});
            return;
        }

        // Set system look and feel for native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Enable font anti-aliasing
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Launch
        SwingUtilities.invokeLater(() -> {
            JazariLauncher launcher = new JazariLauncher();
            launcher.setVisible(true);
        });
    }
}