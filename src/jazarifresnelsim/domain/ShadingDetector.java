package jazarifresnelsim.domain;

import java.util.List;
import jazarifresnelsim.models.MirrorPosition;
import jazarifresnelsim.models.SimulationState;
import jazarifresnelsim.models.SolarPosition;

/**
 * Encapsulates the mathematical model for detecting inter-mirror shading and blocking.
 * Stateless class as defined in the paper's Domain Layer (Figure 2).
 */
public class ShadingDetector {

    public double calculateBlockingAndShadingLoss(MirrorPosition currentMirror,
            List<MirrorPosition> allMirrors, SimulationState state, SolarPosition sunPos) {
        
        double mirrorWidth = state.getReflectorWidth() / 100.0;
        double supportHeight = state.getSupportHeight() / 100.0;
        double currentX = currentMirror.getXOffset() / 100.0;
        double receiverHeight = state.getReceiverHeight() / 100.0;

        double sunAltRad = Math.toRadians(sunPos.getAltitudeAngle());
        double sunAzRad = Math.toRadians(sunPos.getAzimuthAngle());
        double[] sunVector = {
            -Math.cos(sunAltRad) * Math.sin(sunAzRad),
            Math.cos(sunAltRad) * Math.cos(sunAzRad),
            Math.sin(sunAltRad)
        };

        double[] reflectedVector = calculateReflectedVector(currentX, receiverHeight, supportHeight);

        double effectiveArea = mirrorWidth;
        double totalLostArea = 0.0;

        for (MirrorPosition otherMirror : allMirrors) {
            if (otherMirror.getMirrorIndex() == currentMirror.getMirrorIndex()) continue;

            double otherX = otherMirror.getXOffset() / 100.0;
            double distance = Math.abs(currentX - otherX);

            double neighborShadow = 0.0;
            double neighborBlock = 0.0;

            if (checkShadowing(currentX, otherX, sunVector, mirrorWidth, supportHeight)) {
                neighborShadow = calculateOverlap(distance, mirrorWidth, sunVector);
            }

            if (checkBlocking(currentX, otherX, reflectedVector, mirrorWidth, supportHeight)) {
                neighborBlock = calculateOverlap(distance, mirrorWidth, reflectedVector);
            }

            // REVIEWER 2 BUG FIX: DOUBLE COUNTING PREVENTION
            totalLostArea += Math.max(neighborShadow, neighborBlock);
        }

        return Math.max(0.0, (effectiveArea - totalLostArea) / effectiveArea);
    }

    private double[] calculateReflectedVector(double x, double receiverHeight, double supportHeight) {
        double[] vector = {-x, 0, receiverHeight - supportHeight};
        double magnitude = Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]);
        for (int i = 0; i < 3; i++) vector[i] /= magnitude;
        return vector;
    }

    private boolean checkShadowing(double currentX, double otherX, double[] sunVector, double mirrorWidth, double height) {
        if (sunVector[0] < 0 && otherX >= currentX) return false;
        if (sunVector[0] > 0 && otherX <= currentX) return false;

        double dx = Math.abs(currentX - otherX);
        double projectedWidth = mirrorWidth * Math.abs(sunVector[0] / sunVector[2]);
        return dx < projectedWidth;
    }

    private boolean checkBlocking(double currentX, double otherX, double[] reflectedVector, double mirrorWidth, double height) {
        if (reflectedVector[0] < 0 && otherX >= currentX) return false;
        if (reflectedVector[0] > 0 && otherX <= currentX) return false;

        double dx = Math.abs(currentX - otherX);
        double projectedWidth = mirrorWidth * Math.abs(reflectedVector[0] / reflectedVector[2]);
        return dx < projectedWidth;
    }

    private double calculateOverlap(double distance, double width, double[] vector) {
        double projectedWidth = width * Math.abs(vector[0] / vector[2]);
        return Math.max(0.0, projectedWidth - distance);
    }
}