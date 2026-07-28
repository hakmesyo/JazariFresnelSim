package jazarifresnelsim;

import com.formdev.flatlaf.FlatDarkLaf;
import jazarifresnelsim.ui.DesignFresnelSystemFrame;
import processing.core.PApplet;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern launcher window for JazariFresnelSim. Provides two entry points:
 * Interactive 3D Simulator and Design Fresnel System.
 *
 * This is the main entry point when running the JAR file.
 *
 * @author Yunus Demirtaş, Musa Ataş — Siirt University
 * @version 3.0
 */
public class JazariLauncher extends JFrame {

    // Color palette - DesignFresnelSystemFrame ile uyumlu
    private static final Color BG_DARK = new Color(11, 14, 20);      // #0B0E14
    private static final Color BG_CARD = new Color(21, 27, 38);      // #151B26
    private static final Color ACCENT_ORANGE = new Color(255, 106, 0); // #FF6A00
    private static final Color ACCENT_BLUE = new Color(56, 189, 248);
    private static final Color TEXT_PRIMARY = new Color(240, 240, 240);
    private static final Color TEXT_SECONDARY = new Color(148, 163, 184);
    private static final Color BORDER_COLOR = new Color(45, 55, 72);

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
        JLabel version = new JLabel("v3.0  •  Solar Energy (Elsevier) 2026", SwingConstants.CENTER);
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
                "\uD83C\uDF1E", // 🌞
                "Interactive 3D Simulator",
                "Real-time mirror tracking, ray visualization, and live performance metrics",
                ACCENT_ORANGE,
                e -> launch3DSimulator()
        );
        mainPanel.add(simButton);
        mainPanel.add(Box.createVerticalStrut(16));

        // --- Design Fresnel System Button ---
        JPanel designButton = createLaunchButton(
                "\uD83D\uDCCA", // 📊
                "Design Fresnel System",
                "Configure parameters, run validations, and design new Fresnel systems",
                ACCENT_BLUE,
                e -> openDesignFresnelSystem()
        );
        mainPanel.add(designButton);
        mainPanel.add(Box.createVerticalStrut(28));

        // --- Footer ---
        JSeparator sep2 = new JSeparator();
        sep2.setForeground(BORDER_COLOR);
        sep2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        mainPanel.add(sep2);
        mainPanel.add(Box.createVerticalStrut(12));

        JLabel footer = new JLabel(
                "<html><center>Siirt University • Department of Computer & Mechanical Engineering<br>"
                + "<font color='#64748B'>MIT License • github.com/hakmesyo/JazariFresnelSim</font></center></html>",
                SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.setForeground(TEXT_SECONDARY);
        footer.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(footer);

        add(mainPanel, BorderLayout.CENTER);

        // Set icon if available
        try {
            setIconImage(new ImageIcon(getClass().getResource("/icon.png")).getImage());
        } catch (Exception ignored) {
        }
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
                Color bg = hovered ? new Color(30, 40, 55) : BG_CARD;
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
        JLabel arrow = new JLabel("❯");
        arrow.setFont(new Font("Segoe UI", Font.BOLD, 20));
        arrow.setForeground(TEXT_SECONDARY);
        arrow.setHorizontalAlignment(SwingConstants.CENTER);
        arrow.setPreferredSize(new Dimension(30, 48));
        card.add(arrow, BorderLayout.EAST);

        return card;
    }

    /**
     * Opens the new Design Fresnel System window.
     * Replaces the old terminal-based test suite.
     */
    private void openDesignFresnelSystem() {
        SwingUtilities.invokeLater(() -> {
            DesignFresnelSystemFrame frame = new DesignFresnelSystemFrame();
            frame.setVisible(true);
            // İstersen launcher'ı gizle: this.setVisible(false);
        });
    }

    /**
     * Launches the Processing-based 3D interactive simulator. Uses a separate
     * process with correct native library paths to avoid JOGL DLL issues.
     */
    private void launch3DSimulator() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            try {
                String javaHome = System.getProperty("java.home");
                String javaBin = javaHome + java.io.File.separator + "bin" + java.io.File.separator + "java";

                java.io.File execLocation = new java.io.File(
                        JazariLauncher.class.getProtectionDomain()
                                .getCodeSource().getLocation().toURI());
                String execPath = execLocation.getAbsolutePath();
                java.io.File execDir = execLocation.getParentFile();

                String cp = System.getProperty("java.class.path");

                // JAR'dan çalışıyorsa "libs" klasörünü ekle
                if (execPath.toLowerCase().endsWith(".jar")) {
                    java.io.File libDir = new java.io.File(execDir, "libs");
                    if (libDir.exists() && libDir.isDirectory()) {
                        cp += java.io.File.pathSeparator + libDir.getAbsolutePath() + java.io.File.separator + "*";
                    }
                }

                // Kurşun geçirmez natives klasörü bulucu
                java.io.File nativeBaseDir = null;
                java.io.File[] possibleLocations = {
                    new java.io.File(execDir, "natives"),
                    new java.io.File(System.getProperty("user.dir"), "natives"),
                    new java.io.File(execDir.getParentFile(), "natives"),
                    new java.io.File(execDir.getParentFile().getParentFile(), "natives")
                };

                for (java.io.File loc : possibleLocations) {
                    if (loc != null && loc.exists() && loc.isDirectory()) {
                        nativeBaseDir = loc;
                        break;
                    }
                }

                if (nativeBaseDir == null) {
                    throw new RuntimeException("Kritik Hata: 'natives' klasörü hiçbir yerde bulunamadı!");
                }

                java.io.File exactNativeDir = new java.io.File(nativeBaseDir, "windows-amd64");

                ProcessBuilder pb = new ProcessBuilder(
                        javaBin,
                        "-Djava.library.path=" + exactNativeDir.getAbsolutePath(),
                        "-cp", cp,
                        "jazarifresnelsim.FresnelSimulator"
                );

                pb.directory(execDir);
                pb.inheritIO();
                pb.start();

                SwingUtilities.invokeLater(() -> setState(Frame.ICONIFIED));

            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    try {
                        PApplet.main(new String[]{FresnelSimulator.class.getName()});
                        setState(Frame.ICONIFIED);
                    } catch (Exception ex2) {
                        JOptionPane.showMessageDialog(this,
                                "Error launching 3D Simulator:\n" + ex2.getMessage(),
                                "Launch Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } finally {
                SwingUtilities.invokeLater(() -> setCursor(Cursor.getDefaultCursor()));
            }
        }).start();
    }

    /**
     * Main entry point for the JAR file.
     */
    public static void main(String[] args) {
        // FlatLaf kurulumu - UI oluşmadan önce çağrılmalı
        FlatDarkLaf.setup();
        
        // Font anti-aliasing
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Launch
        SwingUtilities.invokeLater(() -> {
            JazariLauncher launcher = new JazariLauncher();
            launcher.setVisible(true);
        });
    }
}