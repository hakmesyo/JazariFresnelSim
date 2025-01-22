package jazarifresnelsim.optimization.problem;

import static jazarifresnelsim.domain.Constants.REFLECTOR_LENGTH;

/**
 * Represents the design parameters of a Linear Fresnel Reflector system. The
 * receiver length is always equal to mirror length in calculations.
 */
public class DesignParameters {

    // System dimensions (all in centimeters)
    private double receiverHeight;    // cm
    private double receiverDiameter;  // cm
    private double mirrorWidth;       // cm
    private final double mirrorLength;      // cm optimize edilemyecek
    private double mirrorSpacing;     // cm
    private int numberOfMirrors;      // count

    // Parameter bounds (all in centimeters)
    public static final double MIN_RECEIVER_HEIGHT = 30.0;
    public static final double MAX_RECEIVER_HEIGHT = 300.0;
    public static final double MIN_RECEIVER_DIAMETER = 10.0;
    public static final double MAX_RECEIVER_DIAMETER = 50.0;
    public static final double MIN_MIRROR_WIDTH = 5.0;
    public static final double MAX_MIRROR_WIDTH = 30.0;
//    public static final double MIN_MIRROR_LENGTH = 50.0;
//    public static final double MAX_MIRROR_LENGTH = 200.0;
    public static final double MIN_MIRROR_SPACING = 20.0;
    public static final double MAX_MIRROR_SPACING = 70.0;
    public static final int MIN_NUMBER_OF_MIRRORS = 2;
    public static final int MAX_NUMBER_OF_MIRRORS = 10;

    private DesignParameters(double receiverHeight, double receiverDiameter,
            double mirrorWidth, 
            double mirrorLength,
            double mirrorSpacing, 
            int numberOfMirrors) {
        validateParameters(
                receiverHeight, 
                receiverDiameter, 
                mirrorWidth,
                mirrorSpacing, 
                numberOfMirrors);

        this.receiverHeight = receiverHeight;
        this.receiverDiameter = receiverDiameter;
        this.mirrorWidth = mirrorWidth;
        this.mirrorLength = mirrorLength;
        this.mirrorSpacing = mirrorSpacing;
        this.numberOfMirrors = numberOfMirrors;
    }

    public DesignParameters(
            double receiverHeight, 
            double receiverDiameter,
            double mirrorWidth, 
            double mirrorSpacing,
            int numberOfMirrors) {
        
        this(
                receiverHeight, 
                receiverDiameter, 
                mirrorWidth,
                REFLECTOR_LENGTH, // Sabit bir değer kullan
                mirrorSpacing, numberOfMirrors);
    }

    private void validateParameters(
            double receiverHeight, 
            double receiverDiameter,
            double mirrorWidth,
            //double mirrorLength,
            double mirrorSpacing, 
            int numberOfMirrors) {
        if (receiverHeight < MIN_RECEIVER_HEIGHT || receiverHeight > MAX_RECEIVER_HEIGHT) {
            throw new IllegalArgumentException("Invalid receiver height");
        }

        if (receiverDiameter < MIN_RECEIVER_DIAMETER || receiverDiameter > MAX_RECEIVER_DIAMETER) {
            throw new IllegalArgumentException("Invalid receiver diameter");
        }

        if (mirrorWidth < MIN_MIRROR_WIDTH || mirrorWidth > MAX_MIRROR_WIDTH) {
            throw new IllegalArgumentException("Invalid mirror width");
        }

//        if (mirrorLength < MIN_MIRROR_LENGTH || mirrorLength > MAX_MIRROR_LENGTH) {
//            throw new IllegalArgumentException("Invalid mirror length");
//        }

        if (mirrorSpacing < MIN_MIRROR_SPACING || mirrorSpacing > MAX_MIRROR_SPACING) {
            throw new IllegalArgumentException("Invalid mirror spacing");
        }

        if (numberOfMirrors < MIN_NUMBER_OF_MIRRORS || numberOfMirrors > MAX_NUMBER_OF_MIRRORS) {
            throw new IllegalArgumentException("Invalid number of mirrors");
        }
    }

    // Getters
    public double getReceiverHeight() {
        return receiverHeight;
    }

    public double getReceiverDiameter() {
        return receiverDiameter;
    }

    public double getMirrorWidth() {
        return mirrorWidth;
    }

    public double getMirrorLength() {
        return mirrorLength;
    }

    public double getMirrorSpacing() {
        return mirrorSpacing;
    }

    public int getNumberOfMirrors() {
        return numberOfMirrors;
    }

    /**
     * Calculates total mirror area of the system
     *
     * @return Total mirror area in square meters
     */
    public double getTotalMirrorArea() {
        return mirrorWidth * mirrorLength * numberOfMirrors;
    }

    /**
     * Calculates total width of ground coverage
     *
     * @return Total width in meters
     */
    public double getTotalGroundCoverageWidth() {
        return mirrorSpacing * (numberOfMirrors - 1);
    }

    @Override
    public String toString() {
        return String.format(
                "Design Parameters:\n"
                + "Receiver Height: %.2f m\n"
                + "Receiver Diameter: %.2f m\n"
                + "Mirror Width: %.2f m\n"
                //+ "Mirror Length: %.2f m\n"
                + "Mirror Spacing: %.2f m\n"
                + "Number of Mirrors: %d\n"
                + "Total Mirror Area: %.2f m²\n"
                + "Ground Coverage Width: %.2f m",
                receiverHeight, 
                receiverDiameter, 
                mirrorWidth, 
                //mirrorLength,
                mirrorSpacing, 
                numberOfMirrors,
                getTotalMirrorArea(), getTotalGroundCoverageWidth()
        );
    }
}
