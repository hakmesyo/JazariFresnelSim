// MirrorPosition.java
package jazarifresnelsim.models;

/**
 * Represents the position and orientation of a single mirror in the Fresnel system.
 * This class is immutable.
 */
public final class MirrorPosition {
    private final double rotationAngle;    // Mirror's rotation angle in degrees
    private final double xOffset;          // Mirror's X position offset from center
    private final double height;           // Mirror's height from ground
    private final int mirrorIndex;         // Mirror's index in the array

    public MirrorPosition(double rotationAngle, double xOffset, double height, int mirrorIndex) {
        this.rotationAngle = rotationAngle;
        this.xOffset = xOffset;
        this.height = height;
        this.mirrorIndex = mirrorIndex;
    }

    public double getRotationAngle() {
        return rotationAngle;
    }

    public double getXOffset() {
        return xOffset;
    }

    public double getHeight() {
        return height;
    }

    public int getMirrorIndex() {
        return mirrorIndex;
    }
}
