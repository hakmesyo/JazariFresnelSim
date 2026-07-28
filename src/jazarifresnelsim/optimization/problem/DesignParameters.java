package jazarifresnelsim.optimization.problem;

import jazarifresnelsim.domain.ConfigManager;

/**
 * Geometric design parameters for LFR optimization.
 *
 * OPTIMIZABLE parameters (4 continuous + 1 discrete): H_r — receiver height
 * [cm] w — mirror width [cm] p — mirror spacing [cm] N — number of mirrors
 * [integer]
 *
 * FIXED parameters (not optimization variables): D_r — receiver diameter [cm]
 * fixed = 16 cm L — mirror length [cm] fixed from config
 *
 * Rationale for fixing D_r: (1) Its optimal value requires coupled
 * optical-thermal analysis (convection, radiation, CPC acceptance angle) that
 * lies outside the scope of this optical pre-screening framework. (2) In
 * commercial LFR practice, the receiver tube is a standardized component (e.g.
 * 70 mm evacuated tube); the mirror field is then optimized around it, not the
 * other way around. (3) Varying D_r in a purely optical model produces a
 * degenerate optimum (minimize D_r → minimize spillage) that contradicts
 * thermal reality where a larger aperture reduces heat losses.
 *
 * See manuscript Section 1.2 and Section 5 for scope discussion.
 *
 * @author Yunus Demirtas, Musa Atas — Siirt University
 * @version 4.2
 */
public class DesignParameters {

    // ----------------------------------------------------------------
    // Optimizable fields
    // ----------------------------------------------------------------
    private double receiverHeight;   // H_r [cm]
    private double mirrorWidth;      // w   [cm]
    private double mirrorSpacing;    // p   [cm]
    private int numberOfMirrors;  // N

    // ----------------------------------------------------------------
    // Fixed fields — loaded once, never changed by optimizer
    // ----------------------------------------------------------------
    private final double receiverDiameter;  // D_r [cm] — fixed
    private final double mirrorLength;      // L   [cm] — fixed

    // ----------------------------------------------------------------
    // Optimization bounds (4 variables)
    // ----------------------------------------------------------------
    public static double MIN_RECEIVER_HEIGHT = 30.0;
    public static double MAX_RECEIVER_HEIGHT = 300.0;
    public static double MIN_MIRROR_WIDTH = 5.0;
    public static double MAX_MIRROR_WIDTH = 30.0;
    public static double MIN_MIRROR_SPACING = 15.0;
    public static double MAX_MIRROR_SPACING = 80.0;
    public static int MIN_NUMBER_OF_MIRRORS = 2;
    public static int MAX_NUMBER_OF_MIRRORS = 10;

    // ----------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------
    /**
     * Primary constructor — fixed parameters loaded from ConfigManager.
     *
     * @param receiverHeight H_r [cm]
     * @param mirrorWidth w [cm]
     * @param mirrorSpacing p [cm]
     * @param numberOfMirrors N
     */
    public DesignParameters(double receiverHeight,
            double mirrorWidth,
            double mirrorSpacing,
            int numberOfMirrors) {
        this.receiverHeight = receiverHeight;
        this.mirrorWidth = mirrorWidth;
        this.mirrorSpacing = mirrorSpacing;
        this.numberOfMirrors = numberOfMirrors;
        this.receiverDiameter = ConfigManager.getDouble("receiver_diameter_cm", 10.0);
        this.mirrorLength = ConfigManager.getDouble("mirror_length_cm", 1000.0);
    }

    /**
     * Updates optimization bounds from config file.
     */
    public static void updateBoundsFromConfig() {
        MIN_RECEIVER_HEIGHT = ConfigManager.getDouble("min_rec_height", 30.0);
        MAX_RECEIVER_HEIGHT = ConfigManager.getDouble("max_rec_height", 300.0);
        MIN_MIRROR_WIDTH = ConfigManager.getDouble("min_mirror_width", 5.0);
        MAX_MIRROR_WIDTH = ConfigManager.getDouble("max_mirror_width", 30.0);
        MIN_MIRROR_SPACING = ConfigManager.getDouble("min_mirror_spacing", 15.0);
        MAX_MIRROR_SPACING = ConfigManager.getDouble("max_mirror_spacing", 80.0);
        MIN_NUMBER_OF_MIRRORS = ConfigManager.getInt("min_mirrors", 2);
        MAX_NUMBER_OF_MIRRORS = ConfigManager.getInt("max_mirrors", 10);
    }

    public double calculateGroundArea() {
        // Toplam Genişlik = (N-1)*p + w
        double fieldWidthM = ((numberOfMirrors - 1) * mirrorSpacing + mirrorWidth) / 100.0;
        double fieldLengthM = mirrorLength / 100.0;
        return fieldWidthM * fieldLengthM; // m2
    }

    // ----------------------------------------------------------------
    // Getters — optimizable
    // ----------------------------------------------------------------
    public double getReceiverHeight() {
        return receiverHeight;
    }

    public double getMirrorWidth() {
        return mirrorWidth;
    }

    public double getMirrorSpacing() {
        return mirrorSpacing;
    }

    public int getNumberOfMirrors() {
        return numberOfMirrors;
    }

    // ----------------------------------------------------------------
    // Getters — fixed
    // ----------------------------------------------------------------
    public double getReceiverDiameter() {
        return receiverDiameter;
    }

    public double getMirrorLength() {
        return mirrorLength;
    }

    // ----------------------------------------------------------------
    // Convenience metrics
    // ----------------------------------------------------------------
    public double getTotalMirrorArea() {
        return mirrorWidth * mirrorLength * numberOfMirrors;
    }

    public double getFieldWidth() {
        return mirrorSpacing * (numberOfMirrors - 1);
    }

    @Override
    public String toString() {
        return String.format(
                "DesignParameters{Hr=%.1f cm, w=%.1f cm, p=%.1f cm, N=%d | "
                + "Dr=%.1f cm (fixed), L=%.1f cm (fixed)}",
                receiverHeight, mirrorWidth, mirrorSpacing, numberOfMirrors,
                receiverDiameter, mirrorLength);
    }
}
