package jazarifresnelsim.optimization.problem;

import jazarifresnelsim.models.SolarPosition;
import jazarifresnelsim.domain.SolarCalculator;
import jazarifresnelsim.domain.ShadingDetector;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jazarifresnelsim.domain.MirrorTracker;
import jazarifresnelsim.models.MirrorPosition;
import jazarifresnelsim.models.SimulationState;

/**
 * Defines the LFR design optimization problem: maps a set of geometric
 * design parameters to an estimated annual optical-thermal energy yield.
 *
 * Also exposes evaluateOpticalMetrics() for per-mirror cosine efficiency
 * and shading loss breakdown used in Figure 6 (spacing sweep export).
 *
 * @author Yunus Demirtas, Musa Atas — Siirt University
 * @version 2.2
 */
public class FresnelDesignProblem {

    private final SolarCalculator solarCalculator;

    // ----------------------------------------------------------------
    // Simplified thermal / optical scalars (first-order model)
    // ----------------------------------------------------------------
    private static final double MIRROR_REFLECTIVITY   = 0.92;
    private static final double RECEIVER_ABSORPTIVITY = 0.95;
    private static final double SHADING_FACTOR        = 0.95;
    private static final double TRACKING_ACCURACY     = 0.98;
    private static final double DIRT_FACTOR           = 0.97;
    private static final double THERMAL_EFFICIENCY    = 0.70;

    // ----------------------------------------------------------------
    // Cost parameters (reserved for future LCOE extension)
    // ----------------------------------------------------------------
    private static final double MIRROR_COST_PER_M2       = 200.0;
    private static final double RECEIVER_COST_PER_M      = 500.0;
    private static final double SUPPORT_STRUCTURE_RATIO  = 0.3;
    private static final double INSTALLATION_FACTOR      = 1.2;

    private final double latitude;
    private final double longitude;
    private final List<LocalDateTime> evaluationTimes;

    public FresnelDesignProblem(double latitude, double longitude,
            List<LocalDateTime> evaluationTimes) {
        this.latitude        = latitude;
        this.longitude       = longitude;
        this.evaluationTimes = evaluationTimes;
        this.solarCalculator = new SolarCalculator(latitude, longitude, 0);
    }

    // ================================================================
    // PRIMARY OBJECTIVE — annual yield [W·h normalised]
    // ================================================================

    /**
     * Evaluates the design and returns the mean hourly energy yield
     * (objective value for optimisation).
     */
    public double evaluateDesign(DesignParameters params) {
        Map<LocalDateTime, Double> energyByTime = evaluateDesignForAllTimes(params);
        double totalEnergy   = 0.0;
        int    validPoints   = 0;
        for (Double energy : energyByTime.values()) {
            if (energy > 0) { totalEnergy += energy; validPoints++; }
        }
        return validPoints > 0 ? totalEnergy / validPoints : 0;
    }

    /**
     * Evaluates energy output [W] at every evaluation time point.
     * Runs in parallel for performance.
     */
    public Map<LocalDateTime, Double> evaluateDesignForAllTimes(DesignParameters params) {
        Map<LocalDateTime, Double> energyByTime = new ConcurrentHashMap<>();
        SimulationState state = buildState(params);

        evaluationTimes.parallelStream().forEach(time -> {
            SolarPosition sunPosition = solarCalculator.calculateSolarPosition(time);
            if (sunPosition.getAltitudeAngle() > 0) {
                state.setCurrentTime(time);
                state.setCurrentSolarPosition(sunPosition);
                List<MirrorPosition> mirrors = calculateMirrorPositions(state, sunPosition);
                state.updateMirrorPositions(mirrors);
                energyByTime.put(time, solarCalculator.calculateTotalEnergy(state));
            } else {
                energyByTime.put(time, 0.0);
            }
        });
        return energyByTime;
    }

    // ================================================================
    // OPTICAL METRICS — for Figure 6 spacing sweep
    // ================================================================

    /**
     * Evaluates per-mirror optical metrics at a single solar time.
     *
     * Returns a Map with:
     *   "cosine"  — average cosine efficiency across all mirrors [%]
     *   "shading" — average shading+blocking loss across all mirrors [%]
     *
     * Uses the actual ShadingDetector (3D vector projection) for
     * physically correct shading/blocking calculation, consistent
     * with the energy evaluation pipeline.
     *
     * @param params design parameters
     * @param time   evaluation time
     * @return optical metric map
     */
    public Map<String, Double> evaluateOpticalMetrics(
            DesignParameters params, LocalDateTime time) {

        Map<String, Double> zero = new HashMap<>();
        zero.put("cosine",  0.0);
        zero.put("shading", 0.0);

        SolarPosition sunPos = solarCalculator.calculateSolarPosition(time);
        if (sunPos.getAltitudeAngle() <= 0) return zero;

        SimulationState state = buildState(params);
        state.setCurrentTime(time);
        state.setCurrentSolarPosition(sunPos);

        List<MirrorPosition> mirrors = calculateMirrorPositions(state, sunPos);
        state.updateMirrorPositions(mirrors);

        ShadingDetector shadingDetector = new ShadingDetector();
        double totalCosine  = 0.0;
        double totalShading = 0.0;
        int    N            = params.getNumberOfMirrors();

        for (MirrorPosition mirror : mirrors) {
            totalCosine += solarCalculator.calculateCosineEfficiency(mirror, sunPos);
            double shadingEff = shadingDetector.calculateBlockingAndShadingLoss(
                    mirror, mirrors, state, sunPos);
            totalShading += (1.0 - shadingEff);   // convert efficiency → loss
        }

        Map<String, Double> metrics = new HashMap<>();
        metrics.put("cosine",  totalCosine  / N * 100.0);
        metrics.put("shading", totalShading / N * 100.0);
        return metrics;
    }

    // ================================================================
    // INTERNAL HELPERS
    // ================================================================

    /**
     * Builds a SimulationState from design parameters.
     * Centralises state construction to avoid repetition.
     */
    private SimulationState buildState(DesignParameters params) {
        SimulationState state = new SimulationState();
        state.setLatitude(latitude);
        state.setLongitude(longitude);
        state.setReceiverHeight((float) params.getReceiverHeight());
        state.setReceiverDiameter((float) params.getReceiverDiameter());
        state.setReflectorWidth((float) params.getMirrorWidth());
        state.setReflectorSpacing((float) params.getMirrorSpacing());
        state.setNumReflectors(params.getNumberOfMirrors());
        return state;
    }

    /**
     * Calculates optimal mirror tilt angles for the current solar position.
     * Uses the bisector-based law of reflection (Eq. 8 of the manuscript).
     */
    private List<MirrorPosition> calculateMirrorPositions(
            SimulationState state, SolarPosition sunPos) {
        List<MirrorPosition> positions = new ArrayList<>();
        int   numReflectors = state.getNumReflectors();
        float spacing       = state.getReflectorSpacing();
        MirrorTracker tracker = new MirrorTracker();

        for (int i = 0; i < numReflectors; i++) {
            double offset = (i < numReflectors / 2)
                    ? -(i + 0.5) : (i - numReflectors / 2 + 0.5);
            double xOffset = offset * spacing;
            double rotationAngle = tracker.calculateOptimalMirrorAngle(
                    xOffset / 100.0, sunPos, state);
            positions.add(new MirrorPosition(rotationAngle, xOffset,
                    state.getSupportHeight() + 2, i));
        }
        return positions;
    }

    // ================================================================
    // GEOMETRY HELPERS (used internally, kept for future LCOE extension)
    // ================================================================

    private double calculateTotalMirrorArea(DesignParameters params) {
        return (params.getMirrorWidth() * params.getMirrorLength()
                * params.getNumberOfMirrors()) / 10000.0; // cm² → m²
    }

    private double calculateTotalLandArea(DesignParameters params) {
        double totalWidth = (params.getNumberOfMirrors() - 1)
                * params.getMirrorSpacing() + params.getMirrorWidth();
        return (totalWidth * params.getMirrorLength() * 1.2) / 10000.0;
    }

    private double calculateSystemCost(DesignParameters params) {
        double mirrorArea    = calculateTotalMirrorArea(params);
        double mirrorCost    = mirrorArea * MIRROR_COST_PER_M2;
        double receiverCost  = (params.getMirrorLength() / 100.0) * RECEIVER_COST_PER_M;
        double supportCost   = mirrorCost * SUPPORT_STRUCTURE_RATIO;
        return (mirrorCost + receiverCost + supportCost) * INSTALLATION_FACTOR;
    }
}