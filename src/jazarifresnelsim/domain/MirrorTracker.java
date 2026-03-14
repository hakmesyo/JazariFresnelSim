package jazarifresnelsim.domain;

import jazarifresnelsim.models.SolarPosition;
import jazarifresnelsim.models.SimulationState;

/**
 * Encapsulates the mathematical model for individual mirror tracking optimization.
 * Stateless class as defined in the paper's Domain Layer (Figure 2).
 */
public class MirrorTracker {

    public double calculateOptimalMirrorAngle(double mirrorX, SolarPosition sunPos, SimulationState state) {
        double sunAltitude = Math.toRadians(sunPos.getAltitudeAngle());
        double sunAzimuth = Math.toRadians(sunPos.getAzimuthAngle());

        double[] sunRay = {
            -Math.cos(sunAltitude) * Math.sin(sunAzimuth),
            Math.cos(sunAltitude) * Math.cos(sunAzimuth),
            Math.sin(sunAltitude)
        };

        double receiverHeight = state.getReceiverHeight() / 100.0; 
        double supportHeight = state.getSupportHeight() / 100.0;

        double[] targetRay = {
            -mirrorX,
            0,
            receiverHeight - (supportHeight + 0.02) 
        };

        double targetMagnitude = Math.sqrt(
                targetRay[0] * targetRay[0]
                + targetRay[1] * targetRay[1]
                + targetRay[2] * targetRay[2]
        );

        for (int i = 0; i < 3; i++) targetRay[i] /= targetMagnitude;

        double[] normalVector = {
            sunRay[0] + targetRay[0],
            sunRay[1] + targetRay[1],
            sunRay[2] + targetRay[2]
        };

        double normalMagnitude = Math.sqrt(
                normalVector[0] * normalVector[0]
                + normalVector[1] * normalVector[1]
                + normalVector[2] * normalVector[2]
        );

        for (int i = 0; i < 3; i++) normalVector[i] /= normalMagnitude;

        return Math.toDegrees(Math.atan2(normalVector[0], normalVector[2]));
    }
}