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
        // L = 1000 cm (10 m): Pilot-plant scale, consistent with literature
        //   (Barbón et al. 2021: 3-5 m; Moghimi et al. 2015: 12 m).
        //   Keeps end losses realistic but not dominant across all geometries.
        // Dr = 10 cm: Mid-range single-tube receiver diameter.
        //   Literature range: 7-18 cm (Zhu et al. 2014).
        //   Matches Barbón et al. experimental setup ensuring internal consistency.
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
        // 3. OPTIMIZATION BOUNDS (Search Space Constraints)
        // ============================================================
        
        // MIRROR COUNT (N)
        // Min 4: A smaller field cannot provide sufficient concentration for industrial steam.
        // Max 30: Extremely wide fields suffer from severe spillage and shading.
        defaults.setProperty("min_mirrors",       "4");
        defaults.setProperty("max_mirrors",       "50");
        
        // RECEIVER HEIGHT (Hr)
        // Min 50 cm: Structural clearance limit.
        // Max 300 cm: Practical threshold for small-to-medium pilot plants to avoid excessive wind load.
        defaults.setProperty("min_rec_height",    "100.0");
        defaults.setProperty("max_rec_height",    "600.0");
        
        // RECEIVER DIAMETER (Dr) - Often fixed, but bounds kept for UI flexibility.
        defaults.setProperty("min_rec_diam",      "5.0");
        defaults.setProperty("max_rec_diam",      "50.0");
        
        // MIRROR WIDTH (w)
        // CRITICAL CONSTRAINT: 
        // Min 15 cm: Narrower mirrors require too many tracking drives, increasing CAPEX drastically.
        // Max 60 cm: Wider flat mirrors suffer from excessive transversal aberration (optical defocusing).
        defaults.setProperty("min_mirror_width",  "5.0");
        defaults.setProperty("max_mirror_width",  "40.0");
        
        // MIRROR SPACING (p) - Center-to-center distance
        // CRITICAL CONSTRAINT:
        // Min 20 cm: Must be strictly > w to leave a physical gap for rotation and wind clearance.
        // Max 100 cm: Prevents the optimizer from wasting land area excessively.
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