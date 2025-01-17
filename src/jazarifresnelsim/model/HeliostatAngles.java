package jazarifresnelsim.model;

/**
 * Represents the orientation angles of a heliostat (mirror) in the solar collector system.
 * This class is immutable to ensure thread safety and prevent unwanted modifications.
 * 
 * 
 * @version 1.0
 */
public class HeliostatAngles {
    /** The elevation angle of the mirror in degrees */
    public final double elevation;
    
    /** The azimuth angle of the mirror in degrees */
    public final double azimuth;
    
    /** The rotation angle of the mirror around its axis in degrees */
    public final double rotationAngle;
    
    /**
     * Constructs a new HeliostatAngles with the specified orientation angles
     * 
     * @param elevation Mirror elevation angle in degrees
     * @param azimuth Mirror azimuth angle in degrees
     * @param rotationAngle Mirror rotation angle in degrees
     */
    public HeliostatAngles(double elevation, double azimuth, double rotationAngle) {
        this.elevation = elevation;
        this.azimuth = azimuth;
        this.rotationAngle = rotationAngle;
    }
    
    @Override
    public String toString() {
        return String.format("HeliostatAngles[elevation=%.2f°, azimuth=%.2f°, rotation=%.2f°]",
                           elevation, azimuth, rotationAngle);
    }
}
