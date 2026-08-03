package jazarifresnelsim.domain;

import java.io.*;
import java.util.Properties;

/**
 * Persistent configuration manager for JazariFresnelSim.
 *
 * Stores only optical and geometric parameters.
 * 
 * VERSION 4.8 - JOURNAL PAPER EDITION
 * Optimization bounds have been updated to reflect realistic commercial and 
 * manufacturability constraints for Linear Fresnel Reflectors (LFR).
 * This prevents metaheuristic algorithms from converging to physically 
 * impractical geometries (e.g., razor-thin mirrors or zero-gap spacing).
 *
 * @author Yunus Demirtas, Musa Atas — Siirt University
 * @version 4.8
 */
public class ConfigManager {

    private static final String CONFIG_FILE = "config.properties";
    private static Properties properties;
    private static final Properties defaults;

    static {
        defaults = new Properties();

        // ============================================================
        // 1. FIXED GEOMETRY (Base Configuration)
        // ============================================================
        defaults.setProperty("num_mirrors",          "6");
        defaults.setProperty("mirror_width_cm",      "20.0");
        defaults.setProperty("mirror_spacing_cm",    "25.0");
        defaults.setProperty("mirror_length_cm",     "1000.0");
        defaults.setProperty("receiver_height_cm",   "150.0");
        defaults.setProperty("support_height_cm",    "30.0");
        defaults.setProperty("receiver_diameter_cm", "10.0");

        // ============================================================
        // 2. LOCATION (Diyarbakir, Turkey — paper default site)
        // ============================================================
        defaults.setProperty("latitude_deg",  "37.91");
        defaults.setProperty("longitude_deg", "40.24");
        defaults.setProperty("altitude_m",    "600.0");

        // ============================================================
        // 3. OPTIMIZATION BOUNDS (Search Space Constraints, manuscript Sec. 5.6)
        // ============================================================

        // MIRROR COUNT (N) — manuscript range [4, 50]
        defaults.setProperty("min_mirrors",       "4");
        defaults.setProperty("max_mirrors",       "50");

        // RECEIVER HEIGHT (Hr) — manuscript range [50, 600] cm
        defaults.setProperty("min_rec_height",    "50.0");
        defaults.setProperty("max_rec_height",    "600.0");

        // RECEIVER DIAMETER (Dr) - fixed in this study; bounds kept for UI flexibility.
        defaults.setProperty("min_rec_diam",      "5.0");
        defaults.setProperty("max_rec_diam",      "50.0");

        // MIRROR WIDTH (w) — manuscript range [5, 40] cm
        defaults.setProperty("min_mirror_width",  "5.0");
        defaults.setProperty("max_mirror_width",  "40.0");

        // MIRROR SPACING (p) — manuscript range [15, 150] cm
        defaults.setProperty("min_mirror_spacing","15.0");
        defaults.setProperty("max_mirror_spacing","150.0");

        properties = new Properties(defaults);
        loadConfig();
    }

    /** 
     * Loads configuration from file; creates with defaults if absent. 
     */
    public static void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (InputStream in = new FileInputStream(configFile)) {
                properties.load(in);
            } catch (IOException ex) {
                System.err.println("Config load error — using defaults: " + ex.getMessage());
            }
        } else {
            saveConfig();
        }
    }

    /** 
     * Saves current configuration to file. 
     * NOTE: If you change bounds in UI and click 'Apply & Save', 
     * it overwrites config.properties. To revert to defaults, delete the file.
     */
    public static void saveConfig() {
        try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
            properties.store(out, "JazariFresnelSim v4.8 — Engineering Constrained Config");
        } catch (IOException ex) {
            System.err.println("Config save error: " + ex.getMessage());
        }
    }

    public static String getProperty(String key)                        { return properties.getProperty(key); }
    public static void   setProperty(String key, String value)          { properties.setProperty(key, value); }

    public static double getDouble(String key, double defaultValue) {
        try   { return Double.parseDouble(properties.getProperty(key)); }
        catch (NumberFormatException | NullPointerException e) { return defaultValue; }
    }

    public static int getInt(String key, int defaultValue) {
        try   { return Integer.parseInt(properties.getProperty(key)); }
        catch (NumberFormatException | NullPointerException e) { return defaultValue; }
    }
}