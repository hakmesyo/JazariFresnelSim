package jazarifresnelsim.domain;

import java.awt.Color;

/**
 * Global constants and runtime parameters for JazariFresnelSim.
 *
 * VERSION 4.3 — Geometry defaults updated.
 *
 * Changes from v4.1:
 *   MIRROR_LENGTH_CM : 200 cm -> 1000 cm (10 m)
 *     Rationale: 2 m caused severe end losses for peripheral mirrors in
 *     G5 (x_i = 3 m) even at noon. 10 m is consistent with pilot-plant
 *     literature (Barbón et al. 2021: 3-5 m; Moghimi et al. 2015: 12 m)
 *     and keeps end losses negligible across all G1-G5 geometries,
 *     allowing cleaner optical comparison with SolTrace.
 *
 *   RECEIVER_DIAMETER_CM : 16 cm -> 10 cm
 *     Rationale: 16 cm is at the upper end of the literature range
 *     (7-18 cm, Zhu et al. 2014). 10 cm matches the Barbón et al. [4]
 *     experimental setup used for mirror tracking validation (Table 4),
 *     ensuring internal consistency. As a fixed parameter (see
 *     DesignParameters), its value does not affect relative rankings but
 *     does affect absolute spillage predictions.
 *
 * @author Yunus Demirtas, Musa Atas — Siirt University
 * @version 4.3
 */
public final class Constants {

    private Constants() {}

    // ================================================================
    // OPTICAL CONSTANTS
    // ================================================================

    /**
     * Mirror specular reflectivity rho_m — Eq. (14).
     * Typical value for commercial solar mirrors: 0.92-0.95.
     * Not final: TestOptimization.runSensitivityAnalysis() varies this
     * around the baseline to test how strongly p/w_opt and Hr_opt depend
     * on it, then restores the baseline value when done.
     */
    public static double MIRROR_REFLECTIVITY = 0.92;

    /**
     * Effective optical error sigma_opt [rad] — standard deviation of the
     * 1-D projection of the solar disc, used in the spillage convolution
     * of Eq. (15). The sun is declared by its geometric half-angle
     * (4.65 mrad); the 1-D projection of a uniform disc onto one
     * transverse axis has standard deviation half of that, i.e.
     * sigma_opt = 4.65 / 2 = 2.325 mrad. Confirmed by direct measurement
     * against SolTrace: 2.333 mrad over a 5 m path (2e4 rays). No mirror
     * slope error is included, consistent with the ray-tracing
     * configuration used for validation (manuscript Sec. 3.5, Pitfall 3).
     * Not final: see MIRROR_REFLECTIVITY above.
     */
    public static double SIGMA_OPT = 2.325e-3;

    /** Solar constant [W/m^2] — Hottel clear-sky DNI model. */
    public static final double SOLAR_CONSTANT = 1361.0;

    // ================================================================
    // DEFAULT LOCATION — Diyarbakir, Turkey (paper primary site)
    // ================================================================

    public static final double DEFAULT_LATITUDE  = 37.91;
    public static final double DEFAULT_LONGITUDE = 40.24;

    // ================================================================
    // UI / PROCESSING CONSTANTS
    // ================================================================

    public static final int WINDOW_WIDTH     = 1280;
    public static final int WINDOW_HEIGHT    = 720;
    public static final int GUI_PANEL_WIDTH  = 350;
    public static final int GUI_PANEL_HEIGHT = 600;
    public static final int GUI_BAR_HEIGHT   = 25;
    public static final int GUI_SPACING      = 15;
    public static final int TEXT_HEIGHT      = 22;
    public static final int BUTTON_HEIGHT    = 25;

    public static final Color BACKGROUND_COLOR = new Color(40,  44,  52);
    public static final Color FOREGROUND_COLOR = new Color(220, 220, 220);
    public static final Color ACTIVE_COLOR     = new Color(0,   150, 255);
    public static final Color LABEL_COLOR      = new Color(180, 180, 180);
    public static final Color TEXT_COLOR       = new Color(255, 255, 255);

    // ================================================================
    // RUNTIME PARAMETERS — loaded from ConfigManager at startup
    // ================================================================

    // Geometry
    public static int   NUM_MIRRORS;
    public static float MIRROR_WIDTH_CM;
    public static float MIRROR_SPACING_CM;
    public static float MIRROR_LENGTH_CM;
    public static float RECEIVER_HEIGHT_CM;
    public static float SUPPORT_HEIGHT_CM;
    public static float RECEIVER_DIAMETER_CM;

    // Location
    public static double LATITUDE_DEG;
    public static double LONGITUDE_DEG;
    public static double ALTITUDE_M;

    // ================================================================
    // INITIALIZATION — load from ConfigManager on class load
    // ================================================================

    static {
        updateFromConfig();
    }

    /**
     * Reloads all runtime parameters from ConfigManager.
     * Call after ConfigManager.loadConfig() or after the user changes
     * settings in the UI preset panel.
     */
    public static void updateFromConfig() {
        NUM_MIRRORS          = ConfigManager.getInt   ("num_mirrors",          6);
        MIRROR_WIDTH_CM      = (float) ConfigManager.getDouble("mirror_width_cm",      10.0);
        MIRROR_SPACING_CM    = (float) ConfigManager.getDouble("mirror_spacing_cm",    30.0);
        MIRROR_LENGTH_CM     = (float) ConfigManager.getDouble("mirror_length_cm",   1000.0);
        RECEIVER_HEIGHT_CM   = (float) ConfigManager.getDouble("receiver_height_cm",  130.0);
        SUPPORT_HEIGHT_CM    = (float) ConfigManager.getDouble("support_height_cm",    30.0);
        RECEIVER_DIAMETER_CM = (float) ConfigManager.getDouble("receiver_diameter_cm", 10.0);

        LATITUDE_DEG  = ConfigManager.getDouble("latitude_deg",  DEFAULT_LATITUDE);
        LONGITUDE_DEG = ConfigManager.getDouble("longitude_deg", DEFAULT_LONGITUDE);
        ALTITUDE_M    = ConfigManager.getDouble("altitude_m",    600.0);
    }
}