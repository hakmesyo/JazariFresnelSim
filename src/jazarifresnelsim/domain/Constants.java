// Constants.java
package jazarifresnelsim.domain;

public final class Constants {
    // Reflector parameters (all dimensions in centimeters)
    public static final int NUM_REFLECTORS = 4;          // Number of mirrors (2 left + 2 right)
    public static final float RECEIVER_HEIGHT = 130;     // Height of receiver tube
    public static final float RECEIVER_DIAMETER = 16;    // Diameter of receiver tube
    public static final float REFLECTOR_WIDTH = 20;      // Width of each mirror
    public static final float REFLECTOR_LENGTH = 100;    // Length of each mirror (1 meter)
    public static final float REFLECTOR_SPACING = 30;    // Spacing between mirrors
    public static final float SUPPORT_HEIGHT = 30;       // Height of mirror supports

    // Default location parameters
    public static final double DEFAULT_LATITUDE = 37.962984;   // 37°56'N
    public static final double DEFAULT_LONGITUDE = 41.850347;  // 41°57'E

    // GUI Constants
    public static final int WINDOW_WIDTH = 1280;
    public static final int WINDOW_HEIGHT = 800;
    public static final int GUI_PANEL_WIDTH = 280;
    public static final int GUI_PANEL_HEIGHT = 620;
    public static final int GUI_BAR_HEIGHT = 20;
    public static final int GUI_SPACING = 20;
    public static final int TEXT_HEIGHT = 20;

    private Constants() {
        // Prevent instantiation
    }
}