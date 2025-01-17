package jazarifresnelsim.model;

/**
 * Represents the position of the sun in the sky using altitude and azimuth angles.
 * This class is immutable to ensure thread safety and prevent unwanted modifications.
 * 
 * 
 * @version 1.0
 */
public class SolarPosition {
    /** The altitude angle of the sun in degrees (elevation above horizon) */
    public final double altitudeAngle;
    
    /** The azimuth angle of the sun in degrees (angle from north, clockwise) */
    public final double azimuthAngle;
    
    /** The solar radiation intensity in W/m² */
    public final double solarIntensity;
    
    /**
     * Constructs a new SolarPosition with the specified parameters
     * 
     * @param altitudeAngle Sun's altitude angle in degrees (0-90)
     * @param azimuthAngle Sun's azimuth angle in degrees (0-360)
     * @param solarIntensity Solar radiation intensity in W/m²
     */
    public SolarPosition(double altitudeAngle, double azimuthAngle, double solarIntensity) {
        this.altitudeAngle = altitudeAngle;
        this.azimuthAngle = azimuthAngle;
        this.solarIntensity = solarIntensity;
    }
    
    @Override
    public String toString() {
        return String.format("SolarPosition[altitude=%.2f°, azimuth=%.2f°, intensity=%.1f W/m²]",
                           altitudeAngle, azimuthAngle, solarIntensity);
    }
}