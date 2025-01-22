package jazarifresnelsim.optimization.problem;

import jazarifresnelsim.models.SolarPosition;
import jazarifresnelsim.domain.SolarCalculator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jazarifresnelsim.models.MirrorPosition;
import jazarifresnelsim.models.SimulationState;

public class FresnelDesignProblem {

    private final SolarCalculator solarCalculator;
    // Physical constants based on SolarCalculator
    private static final double MIRROR_REFLECTIVITY = 0.92;  // Mirror reflection efficiency
    private static final double RECEIVER_ABSORPTIVITY = 0.95; // Receiver absorption efficiency
    private static final double SHADING_FACTOR = 0.95;       // General shading factor
    private static final double TRACKING_ACCURACY = 0.98;    // Tracking system accuracy
    private static final double DIRT_FACTOR = 0.97;          // Mirror cleanliness
    private static final double THERMAL_EFFICIENCY = 0.70;   // Overall thermal efficiency

    // Cost parameters
    private static final double MIRROR_COST_PER_M2 = 200.0;   // $/m²
    private static final double RECEIVER_COST_PER_M = 500.0;  // $/m
    private static final double SUPPORT_STRUCTURE_RATIO = 0.3; // 30% of mirror cost
    private static final double INSTALLATION_FACTOR = 1.2;    // 20% installation cost

    private final double latitude;
    private final double longitude;
    private final List<LocalDateTime> evaluationTimes;

    public FresnelDesignProblem(double latitude, double longitude, List<LocalDateTime> evaluationTimes) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.evaluationTimes = evaluationTimes;
        this.solarCalculator = new SolarCalculator(latitude, longitude, 0);
    }

    public double evaluateDesign(DesignParameters params) {
        Map<LocalDateTime, Double> energyByTime = evaluateDesignForAllTimes(params);

        // Toplam enerjiyi hesapla
        double totalEnergy = 0.0;
        int validTimePoints = 0;

        for (Double energy : energyByTime.values()) {
            if (energy > 0) {
                totalEnergy += energy;
                validTimePoints++;
            }
        }

        return validTimePoints > 0 ? totalEnergy / validTimePoints : 0;
    }

    public Map<LocalDateTime, Double> evaluateDesignForAllTimes(DesignParameters params) {
        Map<LocalDateTime, Double> energyByTime = new HashMap<>();
        SimulationState state = new SimulationState();

        // Set basic simulation parameters
        state.setLatitude(latitude);
        state.setLongitude(longitude);
        state.setReceiverHeight((float) params.getReceiverHeight());
        state.setReceiverDiameter((float) params.getReceiverDiameter());
        state.setReflectorWidth((float) params.getMirrorWidth());
        state.setReflectorSpacing((float) params.getMirrorSpacing());
        state.setNumReflectors(params.getNumberOfMirrors());

        for (LocalDateTime time : evaluationTimes) {
            SolarPosition sunPosition = solarCalculator.calculateSolarPosition(time);

            if (sunPosition.getAltitudeAngle() > 0) {
                state.setCurrentTime(time);
                state.setCurrentSolarPosition(sunPosition);

                List<MirrorPosition> mirrorPositions = calculateMirrorPositions(state, sunPosition);
                state.updateMirrorPositions(mirrorPositions);

                double energy = solarCalculator.calculateTotalEnergy(state);
                energyByTime.put(time, energy);
            } else {
                energyByTime.put(time, 0.0);
            }
        }

        return energyByTime;
    }

    /**
     * Calculates optimal mirror positions based on current solar position
     *
     * @param state Current simulation state
     * @param sunPos Current solar position
     * @return List of mirror positions with optimal angles
     */
    private List<MirrorPosition> calculateMirrorPositions(SimulationState state, SolarPosition sunPos) {
        List<MirrorPosition> positions = new ArrayList<>();
        int numReflectors = state.getNumReflectors();
        float spacing = state.getReflectorSpacing();

        for (int i = 0; i < numReflectors; i++) {
            // Calculate mirror offset from center
            double offset = (i < numReflectors / 2)
                    ? -(i + 0.5) : (i - numReflectors / 2 + 0.5);

            double xOffset = offset * spacing;

            // Calculate optimal rotation angle for this mirror
            double rotationAngle = solarCalculator.calculateOptimalMirrorAngle(
                    xOffset / 100.0, sunPos, state);

            // Create mirror position with calculated parameters
            positions.add(new MirrorPosition(
                    rotationAngle,
                    xOffset,
                    state.getSupportHeight() + 2,
                    i
            ));
        }

        return positions;
    }

    private double calculateCosineEfficiency(SolarPosition sunPos) {
        double altitude = Math.toRadians(sunPos.getAltitudeAngle());
        return Math.sin(altitude);  // Simple cosine efficiency based on sun height
    }

    private double calculateShadingEfficiency(DesignParameters params, SolarPosition sunPos) {
        double altitude = Math.toRadians(sunPos.getAltitudeAngle());
        double azimuth = Math.toRadians(sunPos.getAzimuthAngle());

        // Calculate shadow length
        double shadowLength = params.getMirrorWidth() * Math.cos(altitude);

        // Calculate effective spacing
        double effectiveSpacing = params.getMirrorSpacing();

        // Basic shading efficiency
        double shadingEff = Math.min(1.0, effectiveSpacing / Math.max(shadowLength, 0.1));

        // Add end losses
        double endLossEff = calculateEndLossEfficiency(params, altitude);

        return Math.max(0.7, shadingEff * endLossEff * SHADING_FACTOR);
    }

    private double calculateEndLossEfficiency(DesignParameters params, double altitude) {
        double tanAlt = Math.tan(altitude);
        if (tanAlt < 0.1) {
            return 0.7; // Minimum efficiency for very low angles
        }
        double length = params.getMirrorLength();
        double height = params.getReceiverHeight();

        double endLoss = height / tanAlt;
        return Math.min(1.0, (length - endLoss) / length);
    }

    private double calculateTotalMirrorArea(DesignParameters params) {
        return (params.getMirrorWidth() * params.getMirrorLength()
                * params.getNumberOfMirrors()) / 10000.0; // cm² to m²
    }

    private double calculateTotalLandArea(DesignParameters params) {
        double totalWidth = (params.getNumberOfMirrors() - 1) * params.getMirrorSpacing()
                + params.getMirrorWidth();
        return (totalWidth * params.getMirrorLength() * 1.2) / 10000.0; // 20% margin
    }

    private double calculateSystemCost(DesignParameters params) {
        // Mirror cost
        double mirrorArea = calculateTotalMirrorArea(params);
        double mirrorCost = mirrorArea * MIRROR_COST_PER_M2;

        // Receiver cost
        double receiverLength = params.getMirrorLength() / 100.0; // cm to m
        double receiverCost = receiverLength * RECEIVER_COST_PER_M;

        // Support structure cost
        double supportCost = mirrorCost * SUPPORT_STRUCTURE_RATIO;

        // Total cost including installation
        return (mirrorCost + receiverCost + supportCost) * INSTALLATION_FACTOR;
    }
}
