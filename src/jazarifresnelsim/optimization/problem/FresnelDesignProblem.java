package jazarifresnelsim.optimization.problem;

import jazarifresnelsim.domain.Constants;
import jazarifresnelsim.domain.MirrorTracker;
import jazarifresnelsim.domain.ShadingDetector;
import jazarifresnelsim.domain.SolarCalculator;
import jazarifresnelsim.domain.SolarData;
import jazarifresnelsim.models.MirrorPosition;
import jazarifresnelsim.models.SimulationState;
import jazarifresnelsim.models.SolarPosition;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LFR design optimization problem: Maps geometric parameters to annual energy
 * yield.
 *
 * VERSION 4.8 - TECHNO-ECONOMIC OPTIMIZATION EDITION (Journal Paper Version) 1.
 * Preserves original parallel processing and metric breakdown. 2. Integrates
 * TMY-based seasonal optimization (Jeddah, Diyarbakir, Berlin). 3. Implements a
 * Techno-Economic Objective Function (LCOE Proxy) that balances total energy
 * harvest against normalized mechanical and structural costs.
 *
 * @author Yunus Demirtas, Musa Atas — Siirt University
 * @version 4.8
 */
public class FresnelDesignProblem {

    private final SolarCalculator solarCalculator;
    private final double latitude;
    private final double longitude;
    private final List<LocalDateTime> evaluationTimes;
    private final Map<String, double[]> locationDniSet;
    private final String locationName;

    /**
     * LEGACY CONSTRUCTOR: Used for specific lists of evaluation times.
     * Maintains backward compatibility with earlier purely optical tests.
     */
    public FresnelDesignProblem(double latitude, double longitude,
            List<LocalDateTime> evaluationTimes) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.evaluationTimes = evaluationTimes;
        this.solarCalculator = new SolarCalculator(latitude, longitude, 0);
        this.locationName = "Default";
        this.locationDniSet = null;
    }

    /**
     * TMY CONSTRUCTOR: Designed for location-specific techno-economic
     * optimization. Automatically loads True Meteorological Year (TMY)
     * datasets.
     */
    public FresnelDesignProblem(String locationName, double latitude, double longitude) {
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.evaluationTimes = new ArrayList<>();
        this.solarCalculator = new SolarCalculator(latitude, longitude, 0);
        this.locationDniSet = SolarData.getAllLocationData().get(locationName);
    }

    // ============================================================================
// REPLACEMENT for FresnelDesignProblem.evaluateDesign(DesignParameters)
//
// WHAT WAS WRONG WITH THE OLD VERSION
// -----------------------------------
//   double capacityBonus       = Math.log10(effectiveN + 4);
//   double systemUtilityFactor = (cg < 10.0) ? (cg / 10.0) : 1.0;
//   return meanSDY * systemUtilityFactor * capacityBonus;
//
// 1) log10(N+4) appears nowhere in the manuscript and is a monotonically
//    increasing function of N. It ALONE guarantees that the optimiser pushes
//    N to its upper bound. The "optimal mirror count N = 10" result - and
//    therefore Design Rule 2 - was produced by this term, not by optics.
//
// 2) The return value is a dimensionless index, but Table 12 reports it as
//    kWh. That is why the reported 617.7 "kWh" exceeded the physical ceiling
//    of ~397 kWh implied by the stated parameter bounds.
//
// 3) The SolTrace *_spearman.lk scripts reproduce the SAME two factors on
//    their side. Since both metrics share log10(N+4) and the utility factor,
//    the reported rho_s = 1.0 was largely guaranteed by construction rather
//    than earned by the optical model.
//
// 4) The old code silently forced N to even values and p >= w + 10 cm inside
//    the objective. Constraints belong in the search space definition, not
//    hidden in the fitness function where they distort the reported optimum.
//
// WHAT THIS VERSION DOES
// ----------------------
// Objective = daily/annual optical energy per unit GROUND area  [Wh/m^2]
//
//     J = ( sum_h Q_opt(h) * dt ) / A_ground
//     A_ground = ((N-1)*p + w) * L
//
// Dimensionally correct, contains no tunable coefficients, and is exactly the
// metric validated in the p/w sweep. Land normalisation is what keeps the
// problem well posed: maximising raw energy with a free aperture area is
// trivially solved by maximising area, which is why the old formulation
// always returned a boundary solution.
//
// HONEST CAVEAT for the manuscript: some parameters may still saturate their
// bounds. If they do, report it plainly and frame Section 5.6 as an
// algorithmic benchmark (GA vs PSO vs SA on a common problem) rather than as
// a design prescription.
// ============================================================================
    public double evaluateDesign(DesignParameters params) {

        // ---- 1. Build the evaluation time grid --------------------------------
        List<LocalDateTime> times;

        if (evaluationTimes != null && !evaluationTimes.isEmpty()) {
            // Legacy / explicit mode: caller supplied the hours
            times = evaluationTimes;
        } else {
            // TMY mode: four representative days, 08:00-17:00
            times = new ArrayList<>();
            String[] dayCodes = {"0321", "0621", "0921", "1221"};
            for (String code : dayCodes) {
                int month = Integer.parseInt(code.substring(0, 2));
                int day = Integer.parseInt(code.substring(2, 4));
                for (int h = 0; h < 10; h++) {
                    times.add(LocalDateTime.of(2026, month, day, h + 8, 0));
                }
            }
        }

        // ---- 2. Integrate optical energy over the grid ------------------------
        final double DT_HOURS = 1.0;
        double energyWh = 0.0;

        for (LocalDateTime t : times) {

            // TMY DNI when available, otherwise -1 triggers the Hottel clear-sky
            // model inside SolarCalculator
            double dni = -1.0;
            if (locationDniSet != null) {
                String code = String.format("%02d%02d",
                        t.getMonthValue(), t.getDayOfMonth());
                if (locationDniSet.containsKey(code)) {
                    int idx = t.getHour() - 8;
                    if (idx >= 0 && idx < 10) {
                        dni = locationDniSet.get(code)[idx];
                    }
                }
            }

            energyWh += calculatePowerAtTime(params, t, dni) * DT_HOURS;
        }

        // ---- 3. Normalise by ground footprint ---------------------------------
        double groundM2 = params.calculateGroundArea();   // ((N-1)p + w) * L  [m^2]

        if (groundM2 <= 0.0) {
            return 0.0;
        }

        // ---- 4. Concentration-ratio constraint (Eq. 18) ------------------------
        // A purely optical objective is monotone toward the smallest possible
        // field: shrinking the field improves cosine, slant distance and end
        // losses simultaneously. The design problem is only well posed once a
        // non-optical requirement is imposed. The governing one is the minimum
        // geometric concentration ratio set by the target receiver temperature;
        // Cg = 20 corresponds to the low end of LFR direct steam generation
        // (roughly 250-350 C). This is a HARD constraint, C_g >= C_g,min: a
        // design below the minimum concentration ratio is not merely
        // sub-optimal, it does not meet the receiver's thermal requirement and
        // must be excluded from the feasible set, not softly discounted. An
        // earlier soft-penalty formulation (J scaled by cg/CG_MIN below the
        // threshold) let the optimizer trade a small efficiency loss for a
        // large reduction in ground footprint, converging to infeasible
        // designs (Cg well below 20) that do not correspond to the constrained
        // problem in Sec. 5.5-5.6 of the manuscript.
        final double CG_MIN = 20.0;
        double cg = (params.getNumberOfMirrors() * params.getMirrorWidth())
                / params.getReceiverDiameter();

        if (cg < CG_MIN) {
            return 0.0;   // infeasible: rejected, not discounted
        }

        return energyWh / groundM2;                      // [Wh / m2 of land]
    }

    /**
     * Core helper method calculating instantaneous net optical power [W] using
     * TMY DNI.
     */
    private double calculatePowerAtTime(DesignParameters params, LocalDateTime time, double dni) {
        SolarPosition sunPos = solarCalculator.calculateSolarPosition(time, dni);
        if (sunPos.getAltitudeAngle() <= 0 || sunPos.getSolarIntensity() <= 0) {
            return 0.0;
        }

        SimulationState state = buildState(params);
        state.setCurrentTime(time);
        state.setCurrentSolarPosition(sunPos);

        List<MirrorPosition> mirrors = mirrorPositions(state, sunPos);
        return solarCalculator.calculateOpticalPower(state, mirrors);
    }

    // ================================================================
    // PRESERVED CORE METHODS (LEGACY & ANALYTICAL)
    // ================================================================
    /**
     * Evaluates optical power Q_opt [W] at all specified evaluation times.
     * Executes in parallel to maintain original high-performance benchmark
     * speeds.
     */
    public Map<LocalDateTime, Double> evaluateAllTimes(DesignParameters params) {
        Map<LocalDateTime, Double> result = new ConcurrentHashMap<>();

        evaluationTimes.parallelStream().forEach(time -> {
            SolarCalculator localCalc = new SolarCalculator(latitude, longitude, 0);
            // Uses original Hottel Clear-Sky model (triggered by -1)
            SolarPosition sunPos = localCalc.calculateSolarPosition(time, -1);

            if (sunPos.getAltitudeAngle() > 0) {
                SimulationState localState = buildState(params);
                localState.setCurrentTime(time);
                localState.setCurrentSolarPosition(sunPos);
                localState.updateMirrorPositions(mirrorPositions(localState, sunPos));
                result.put(time, localCalc.calculateOpticalPower(localState, localState.getMirrorPositions()));
            } else {
                result.put(time, 0.0);
            }
        });
        return result;
    }

    /**
     * Extracts per-component optical metrics (Cosine, Shading, End-loss,
     * Spillage) at a single time step. Crucial for generating Table 6 and
     * parametric plots.
     */
    public Map<String, Double> evaluateOpticalMetrics(DesignParameters params, LocalDateTime time) {
        Map<String, Double> m = new HashMap<>();
        m.put("cosine", 0.0);
        m.put("shading", 0.0);
        m.put("endloss", 0.0);
        m.put("spillage", 0.0);
        m.put("eta_opt", 0.0);
        m.put("Q_opt", 0.0);

        // Retrieve TMY DNI if applicable to the given timestamp
        double externalDni = -1;
        if (locationDniSet != null) {
            String code = String.format("%02d%02d", time.getMonthValue(), time.getDayOfMonth());
            if (locationDniSet.containsKey(code)) {
                int hIdx = time.getHour() - 8;
                if (hIdx >= 0 && hIdx < 10) {
                    externalDni = locationDniSet.get(code)[hIdx];
                }
            }
        }

        SolarPosition sunPos = solarCalculator.calculateSolarPosition(time, externalDni);
        if (sunPos.getAltitudeAngle() <= 0) {
            return m;
        }

        SimulationState state = buildState(params);
        state.setCurrentTime(time);
        state.setCurrentSolarPosition(sunPos);

        List<MirrorPosition> mirrors = mirrorPositions(state, sunPos);
        state.updateMirrorPositions(mirrors);

        ShadingDetector shading = new ShadingDetector();
        int N = params.getNumberOfMirrors();
        double mirrorArea = (params.getMirrorWidth() / 100.0) * (params.getMirrorLength() / 100.0);
        double dni = sunPos.getSolarIntensity();

        double sumCos = 0, sumShad = 0, sumEnd = 0, sumSpill = 0, totalQ = 0;

        for (MirrorPosition mirror : mirrors) {
            double etaCos = solarCalculator.calculateCosineEfficiency(mirror, sunPos);
            double etaSb = shading.calculateBlockingAndShadingLoss(mirror, mirrors, state, sunPos);
            double fEnd = solarCalculator.calculateEndLossEfficiency(mirror, sunPos, state);
            double fSpill = solarCalculator.calculateSpillageLoss(mirror, state);

            sumCos += etaCos;
            sumShad += (1.0 - etaSb);
            sumEnd += (1.0 - fEnd);
            sumSpill += (1.0 - fSpill);

            totalQ += dni * mirrorArea * Constants.MIRROR_REFLECTIVITY * etaCos * etaSb * fEnd * fSpill;
        }

        m.put("cosine", sumCos / N * 100.0);
        m.put("shading", sumShad / N * 100.0);
        m.put("endloss", sumEnd / N * 100.0);
        m.put("spillage", sumSpill / N * 100.0);
        m.put("eta_opt", (N > 0 && dni > 0 && mirrorArea > 0) ? totalQ / (dni * N * mirrorArea) * 100.0 : 0.0);
        m.put("Q_opt", totalQ);
        return m;
    }

    // ================================================================
    // INTERNAL KINEMATIC HELPERS
    // ================================================================
    /**
     * Instantiates the geometric simulation state for the LFR system.
     */
    private SimulationState buildState(DesignParameters params) {
        SimulationState s = new SimulationState();
        s.setLatitude(latitude);
        s.setLongitude(longitude);
        s.setReceiverHeight((float) params.getReceiverHeight());
        s.setReflectorWidth((float) params.getMirrorWidth());
        s.setReflectorSpacing((float) params.getMirrorSpacing());
        s.setNumReflectors(params.getNumberOfMirrors());
        s.setReflectorLength((float) params.getMirrorLength());
        // Receiver diameter is kept constant throughout optimization
        s.setReceiverDiameter(Constants.RECEIVER_DIAMETER_CM);
        s.setSupportHeight(30.0f);
        return s;
    }

    /**
     * Determines tilt angles for all mirror segments utilizing the
     * deterministic bisector method.
     */
    private List<MirrorPosition> mirrorPositions(SimulationState state, SolarPosition sunPos) {
        List<MirrorPosition> positions = new ArrayList<>();
        int N = state.getNumReflectors();
        float spacing = state.getReflectorSpacing();
        MirrorTracker tracker = new MirrorTracker();

        for (int i = 0; i < N; i++) {
            double offset = (i < N / 2.0) ? -(i + 0.5) : (i - N / 2.0 + 0.5);
            double xOffset = offset * spacing;
            double angle = tracker.calculateOptimalMirrorAngle(xOffset / 100.0, sunPos, state);
            positions.add(new MirrorPosition(angle, xOffset, state.getSupportHeight(), i));
        }
        return positions;
    }
}