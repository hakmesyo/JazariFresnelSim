package jazarifresnelsim.optimization;

import jazarifresnelsim.optimization.algorithms.*;
import jazarifresnelsim.optimization.problem.*;
import jazarifresnelsim.optimization.evaluation.*;
import jazarifresnelsim.domain.SolarCalculator;
import jazarifresnelsim.domain.ConfigManager;
import jazarifresnelsim.domain.Constants;
import jazarifresnelsim.models.SolarPosition;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import jazarifresnelsim.domain.MirrorTracker;
import jazarifresnelsim.models.SimulationState;

/**
 * Validation and optimization test suite for JazariFresnelSim.
 *
 * Each public static method corresponds to one test button in the Manuscript
 * Validation panel and reproduces a specific table or figure from the paper.
 * Entry point: runSelectedTest(int).
 *
 * DesignParameters constructor signature (v4.2): new DesignParameters(Hr, w, p,
 * N) -- D_r and L are fixed and loaded internally from ConfigManager.
 *
 * @author Yunus Demirtas, Musa Atas -- Siirt University
 * @version 4.2
 */
public class TestOptimization {

    // ----------------------------------------------------------------
    // Location constants
    // ----------------------------------------------------------------
    private static final double LAT_DIYARBAKIR = Constants.DEFAULT_LATITUDE;
    private static final double LON_DIYARBAKIR = Constants.DEFAULT_LONGITUDE;
    private static final double LAT_BERLIN = 52.52;
    private static final double LON_BERLIN = 13.405;
    private static final double LAT_JEDDAH = 21.49;
    private static final double LON_JEDDAH = 39.19;

    private static final int NUM_RUNS = 30;

    // ================================================================
// MANUSCRIPT VALIDATION FIXTURE
// Deliberately hermetic: a published table must not silently change
// when a user edits config.properties.
// ================================================================
    public static final double FIX_W_CM = 10.0;   // mirror width
    public static final double FIX_P_CM = 15.0;   // pitch (G2)
    public static final double FIX_HR_CM = 130.0;   // receiver height (G2)
    public static final double FIX_L_CM = 1000.0;   // mirror length
    public static final double FIX_DR_CM = 10.0;   // receiver diameter
    public static final int FIX_N = 6;   // mirror count (G2)

    private static void applyValidationFixture() {
        ConfigManager.setProperty("mirror_width_cm", String.valueOf(FIX_W_CM));
        ConfigManager.setProperty("mirror_spacing_cm", String.valueOf(FIX_P_CM));
        ConfigManager.setProperty("receiver_height_cm", String.valueOf(FIX_HR_CM));
        ConfigManager.setProperty("mirror_length_cm", String.valueOf(FIX_L_CM));
        ConfigManager.setProperty("receiver_diameter_cm", String.valueOf(FIX_DR_CM));
        ConfigManager.setProperty("num_mirrors", String.valueOf(FIX_N));
        ConfigManager.setProperty("min_rec_height", "50.0");   // TANI: sinir bagliyici mi?
        Constants.updateFromConfig();
        DesignParameters.updateBoundsFromConfig();
        System.out.printf("[FIXTURE] w=%.0f p=%.0f Hr=%.0f L=%.0f Dr=%.0f N=%d | min_Hr=%.0f%n",
                FIX_W_CM, FIX_P_CM, FIX_HR_CM, FIX_L_CM, FIX_DR_CM, FIX_N,
                DesignParameters.MIN_RECEIVER_HEIGHT);
    }

    // ================================================================
    // ENTRY POINT
    // ================================================================
    public static void runSelectedTest(int choice) {
        ConfigManager.loadConfig();
        DesignParameters.updateBoundsFromConfig();
        Constants.updateFromConfig();
        applyValidationFixture();

        System.out.println("=== Running Test " + choice + " ===");
        long t0 = System.currentTimeMillis();

        switch (choice) {
            case 1 ->
                runExtremeAngleAnalysis();
            case 2 ->
                runMirrorCountScaling();
            case 3 ->
                runGlobalOptimization();
            //runOptimizationComparison();
            case 4 ->
                runTemporalSensitivity();
            case 5 ->
                runSpacingSweepExport();
            case 6 ->
                runHeightSweepExport();
            case 7 ->
                runDailyEfficiencyProfile();
            case 8 ->
                runConvergenceExport();
            case 9 ->
                runSolTraceValidation();
            case 10 ->
                runTable6();
            case 11 ->
                runSolarPositionVerification();
            case 12 ->
                runTrackingVerification();
            case 13 ->
                runWellPosednessSweep();
            case 0 ->
                runAllTests();
            default ->
                System.out.println("Invalid test ID: " + choice);
        }
        System.out.printf("%n--- Completed in %.2f s ---%n%n",
                (System.currentTimeMillis() - t0) / 1000.0);
    }

    // ================================================================
    // TEST 1 -- Extreme-angle annual error analysis (Table 8, Fig. 4)
    // ================================================================
    public static void runExtremeAngleAnalysis() {
        System.out.println("=== TEST 1: Extreme-Angle Annual Error (Table 8) ===\n");
        double[][] locs = {
            {LAT_DIYARBAKIR, LON_DIYARBAKIR},
            {LAT_BERLIN, LON_BERLIN},
            {LAT_JEDDAH, LON_JEDDAH}
        };
        String[] names = {
            String.format("Diyarbakir (%.2fdegN)", LAT_DIYARBAKIR),
            "Berlin (52.52degN)",
            "Jeddah (21.49degN)"
        };
        System.out.printf("%-22s %10s %12s %14s %16s%n",
                "Location", "Daylight h", "h thetaT>55deg", "Fraction (%)", "Yield dev (%)");
        System.out.println("-".repeat(78));

        for (int i = 0; i < 3; i++) {
            SolarCalculator calc = new SolarCalculator(locs[i][0], locs[i][1], 0);
            int total = 0, extreme = 0;
            double totalE = 0, extremeE = 0;
            for (int m = 1; m <= 12; m++) {
                for (int d = 1; d <= 28; d++) {
                    for (int h = 5; h <= 20; h++) {
                        try {
                            SolarPosition pos = calc.calculateSolarPosition(
                                    LocalDateTime.of(2024, m, d, h, 0));
                            if (pos.getAltitudeAngle() > 5.0) {
                                total++;
                                double dni = pos.getSolarIntensity();
                                double aR = Math.toRadians(pos.getAltitudeAngle());
                                double azR = Math.toRadians(pos.getAzimuthAngle());
                                //double tT = 90.0 - Math.abs(Math.toDegrees(Math.atan(Math.tan(aR) / Math.cos(azR))));
                                double tT = Math.abs(Math.toDegrees(Math.atan(Math.sin(azR) / Math.tan(aR))));
                                totalE += dni * Math.max(0, Math.cos(Math.toRadians(tT)));
                                if (tT > 55) {
                                    extreme++;
                                    extremeE += dni * 0.3;
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            System.out.printf("%-22s %10d %12d %14.1f %16.1f%n",
                    names[i], total, extreme,
                    100.0 * extreme / total,
                    100.0 * extremeE / totalE);
        }
    }

    // ================================================================
    // TEST 2 -- Mirror count scaling (Table 12)
    // ================================================================
    public static void runMirrorCountScaling() {
        System.out.println("\n=== TEST 2: Mirror Count Scaling (Table 12) ===");

//        double w = ConfigManager.getDouble("min_mirror_width", 10.0);
//        double p = ConfigManager.getDouble("min_mirror_spacing", 15.0);
//        double hr = ConfigManager.getDouble("min_rec_height", 130.0);
        double w = FIX_W_CM;
        double p = FIX_P_CM;
        double hr = FIX_HR_CM;

        System.out.printf("Preset -- w=%.0f cm, p=%.0f cm, Hr=%.0f cm%n%n", w, p, hr);
        System.out.printf("%-6s %20s %18s %25s%n",
                "N", "Optical power (W)", "Field width (cm)", "Marginal gain (W/mirror)");
        System.out.println("-".repeat(73));

        List<LocalDateTime> times = List.of(
                LocalDateTime.of(2024, Month.JUNE, 21, 12, 0));
        FresnelDesignProblem problem
                = new FresnelDesignProblem(LAT_DIYARBAKIR, LON_DIYARBAKIR, times);

        double prevE = 0;
        int prevN = 0;
        for (int n : new int[]{2, 4, 6, 8, 10}) {
            // 4-param constructor: Hr, w, p, N
            double e = problem.evaluateAllTimes(new DesignParameters(hr, w, p, n))
                    .values().stream().mapToDouble(Double::doubleValue).sum();
            String marg = (prevN > 0)
                    ? String.format("%.2f", (e - prevE) / (n - prevN)) : "--";
            System.out.printf("%-6d %20.4f %18.0f %25s%n",
                    n, e, (n - 1) * p, marg);
            prevE = e;
            prevN = n;
        }
        System.out.println("\n[Note] Marginal gain decreases with N -- Eq. (17) and Design Rule 2.");
    }

    // ================================================================
    // TEST 3 -- Metaheuristic optimization (Table 14--15, Fig. 9)
    // ================================================================
    public static void runOptimizationComparison() {
        System.out.println("=== TEST 3: Metaheuristic Optimization (Table 14--15) ===");
        System.out.printf("Site: %.2fdegN %.2fdegE | H=144 | %d runs/algorithm%n%n",
                LAT_DIYARBAKIR, LON_DIYARBAKIR, NUM_RUNS);
        try {
            List<LocalDateTime> times = evaluationTimes(144);
            FresnelDesignProblem problem = new FresnelDesignProblem(
                    LAT_DIYARBAKIR, LON_DIYARBAKIR, times);
            // Initial guess -- 4-param constructor
            DesignParameters init = new DesignParameters(130.0, 10.0, 15.0, 4);
            List<IOptimizationAlgorithm> algos = buildAlgorithms();
            DesignEvaluator evaluator = new DesignEvaluator(problem, times);
            OptimizationComparison comp
                    = new OptimizationComparison(evaluator, algos.size(), NUM_RUNS);
            printOptResults(comp.compareAlgorithms(algos, problem, init, new HashMap<>()));
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printOptResults(OptimizationComparison.ComparisonResult results) {
        var stats = results.getStatistics();
        System.out.println("\n--- Algorithm Performance (Table 14) ---");
        System.out.printf("%-26s %10s %14s %10s %18s%n",
                "Algorithm", "Time (s)", "Best (kWh)", "Std dev", "Convergence");
        System.out.println("-".repeat(80));
        // Use arrays as effectively-final containers so lambda can capture them
        String[] bestAlgo = {""};
        double[] bestYield = {Double.NEGATIVE_INFINITY};

        for (var e : stats.entrySet()) {
            var s = e.getValue();
            String type = s.objectiveStats.stdDev < 0.01 ? "Global"
                    : s.objectiveStats.stdDev < 15.0 ? "Near-global" : "Local/inconsistent";
            System.out.printf("%-26s %10.2f %14.2f %10.2f %18s%n",
                    e.getKey(), s.timeStats.mean / 1000.0,
                    s.objectiveStats.max, s.objectiveStats.stdDev, type);
            if (s.objectiveStats.max > bestYield[0]) {
                bestYield[0] = s.objectiveStats.max;
                bestAlgo[0] = e.getKey();
            }
        }
        var runs = results.getAlgorithmRuns().get(bestAlgo[0]);
        if (runs != null) {
            runs.stream()
                    .max(Comparator.comparing(r -> r.getBestSolution().getObjectiveValue()))
                    .ifPresent(r -> {
                        DesignParameters p = r.getBestSolution().getParameters();
                        System.out.printf("%n--- Best solution (%s) ---%n"
                                + "  Hr=%.1f cm, w=%.1f cm, p=%.1f cm, N=%d%n"
                                + "  Dr=%.1f cm (fixed) | Yield = %.2f kWh%n",
                                bestAlgo[0],
                                p.getReceiverHeight(),
                                p.getMirrorWidth(),
                                p.getMirrorSpacing(),
                                p.getNumberOfMirrors(),
                                p.getReceiverDiameter(),
                                bestYield[0]);
                    });
        }
    }

    // ================================================================
    // TEST 4 -- Temporal discretization sensitivity (Table 15)
    // ================================================================
    public static void runTemporalSensitivity() {
        System.out.println("=== TEST 4: Temporal Discretization Sensitivity (Table 15) ===\n");
        int[] hVals = {144, 288, 4380};
        String[] labels = {"144 (12 mo x 12 h)", "288 (12 mo x 24 h)", "4380 (full year)"};
        System.out.printf("%-22s %6s %8s %12s %10s%n",
                "Resolution", "N", "p (cm)", "Yield", "Time (s)");
        System.out.println("-".repeat(62));
        for (int i = 0; i < hVals.length; i++) {
            List<LocalDateTime> times = evaluationTimes(hVals[i]);
            FresnelDesignProblem problem = new FresnelDesignProblem(
                    LAT_DIYARBAKIR, LON_DIYARBAKIR, times);
            ParticleSwarm pso = buildPSO();
            long t0 = System.currentTimeMillis();
            var sol = pso.optimize(problem,
                    new DesignParameters(130, 10, 15, 4), new HashMap<>());
            double sec = (System.currentTimeMillis() - t0) / 1000.0;
            var p = sol.getParameters();
            System.out.printf("%-22s %6d %8.1f %12.2f %10.2f%n",
                    labels[i], p.getNumberOfMirrors(),
                    p.getMirrorSpacing(), sol.getObjectiveValue(), sec);
        }
    }

// ============================================================================
// REPLACEMENT for TestOptimization.runSpacingSweepExport()   [v2 - ENERGY BASED]
//
// WHY v1 WAS UNRELIABLE
// ---------------------
// v1 averaged eta_opt UNWEIGHTED over integer clock hours. Near sunrise and
// sunset eta_opt collapses and shading explodes, but those hours carry almost
// no energy. The unweighted mean was therefore dominated by 1-2 near-horizon
// samples, and whether a given sample landed at alpha = 2 deg or alpha = 8 deg
// depended on the equation-of-time offset for that date.
//
// Symptom: Mar 21 and Sep 21 have almost identical solar geometry
// (declination -0.4 deg vs +0.9 deg) yet v1 reported shading of 5.95% vs
// 15.73% at Diyarbakir - a factor of 2.6 that no physical mechanism explains.
// The eta curves were also non-monotonic (68.74 -> 65.95 -> 67.45), which a
// real optical curve cannot be.
//
// WHAT v2 DOES INSTEAD
// --------------------
// 1) Integrates DAILY OPTICAL ENERGY  E = sum(Q_opt * dt)  instead of
//    averaging an intensive quantity. Low-sun hours now contribute in
//    proportion to the energy they actually deliver.
// 2) 15-minute time steps instead of 60, removing the sampling artefact.
// 3) alpha > 5 deg cutoff, consistent with the solar-position validation.
// 4) N and w are held FIXED and only p varies, so A_field is identical for
//    every point on the sweep. E is therefore directly comparable with no
//    normalisation at all.
// 5) Adds a second, independent criterion: energy per unit GROUND area,
//         E_land = E / (((N-1)p + w) * L)
//    Wider pitch buys optical performance but costs land. The two optima
//    differ, and that trade-off is itself a reportable design result.
//
// CONSOLE OUTPUT (12 rows: 3 sites x 4 seasons)
//   opt p/w (E)     pitch that maximises daily optical energy
//   opt p/w (land)  pitch that maximises energy per unit ground area
//   E@opt           daily optical energy at that optimum [Wh]
//   E ratios        E at p/w = 1.5 / 2.0 / 3.0 relative to E@opt, so the
//                   flatness of the optimum is visible
// FILE OUTPUT
//   pw_sweep_energy.csv - full curves for Fig. 6
// ============================================================================
    public static void runSpacingSweepExport() {

        System.out.println("=== TEST 5 v2: p/w Sweep, ENERGY BASED | 3 sites x 4 seasons ===\n");

        final double w = 10.0;    // mirror width    [cm]  fixed
        final double Hr = 130.0;   // receiver height [cm]  fixed
        final int N = 6;       // mirror count          fixed
        final double L = ConfigManager.getDouble("mirror_length_cm", 1000.0);

        final double DT_HOURS = 0.25;   // 15-minute steps
        final double ALT_FLOOR = 5.0;    // ignore alpha < 5 deg

        double[] pwRatios = {1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7,
            1.8, 1.9, 2.0, 2.2, 2.4, 2.6, 2.8, 3.0,
            3.2, 3.4, 3.6, 3.8, 4.0};

        String[] siteNames = {"Diyarbakir", "Berlin", "Jeddah"};
        double[][] siteCoords = {
            {LAT_DIYARBAKIR, LON_DIYARBAKIR},
            {LAT_BERLIN, LON_BERLIN},
            {LAT_JEDDAH, LON_JEDDAH}
        };

        int[][] dates = {{3, 21}, {6, 21}, {9, 21}, {12, 21}};
        String[] dateNames = {"Mar 21", "Jun 21", "Sep 21", "Dec 21"};

        System.out.printf("Fixed: N=%d, w=%.0f cm, Hr=%.0f cm, L=%.0f cm | dt=%.2f h | alpha>%.0f deg%n%n",
                N, w, Hr, L, DT_HOURS, ALT_FLOOR);
        System.out.printf("Sweep: %d points, p/w from %.1f to %.1f%n%n",
                pwRatios.length, pwRatios[0], pwRatios[pwRatios.length - 1]);
        System.out.printf("%-12s %-8s %10s %12s %12s %8s %8s %8s%n",
                "Site", "Date", "opt p/w", "opt p/w", "E@opt", "E1.5", "E2.0", "E3.0");
        System.out.printf("%-12s %-8s %10s %12s %12s %8s %8s %8s%n",
                "", "", "(energy)", "(per land)", "(Wh)", "/Emax", "/Emax", "/Emax");
        System.out.println("-".repeat(88));

        String csv = "pw_sweep_energy.csv";
        try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter(csv))) {

            out.println("site,date,pw_ratio,p_cm,daily_energy_Wh,"
                    + "ground_area_m2,energy_per_land_Wh_m2,valid_steps");

            for (int s = 0; s < siteNames.length; s++) {
                for (int d = 0; d < dates.length; d++) {

                    // --- build 15-minute time grid for this day ---
                    List<LocalDateTime> grid = new ArrayList<>();
                    for (int h = 3; h <= 21; h++) {
                        for (int mm = 0; mm < 60; mm += 15) {
                            grid.add(LocalDateTime.of(2024, dates[d][0], dates[d][1], h, mm));
                        }
                    }

                    SolarCalculator calc = new SolarCalculator(
                            siteCoords[s][0], siteCoords[s][1], 0);

                    // keep only steps with sufficient solar altitude
                    List<LocalDateTime> times = new ArrayList<>();
                    for (LocalDateTime t : grid) {
                        if (calc.calculateSolarPosition(t).getAltitudeAngle() > ALT_FLOOR) {
                            times.add(t);
                        }
                    }
                    if (times.isEmpty()) {
                        System.out.printf("%-12s %-8s   (no daylight above cutoff)%n",
                                siteNames[s], dateNames[d]);
                        continue;
                    }

                    FresnelDesignProblem prob = new FresnelDesignProblem(
                            siteCoords[s][0], siteCoords[s][1], times);

                    double bestE = -1.0, bestPwE = 0.0;
                    double bestLand = -1.0, bestPwLand = 0.0;
                    double e15 = 0.0, e20 = 0.0, e30 = 0.0;

                    for (double pw : pwRatios) {

                        double p = pw * w;
                        DesignParameters params = new DesignParameters(Hr, w, p, N);

                        // daily optical energy [Wh]
                        double energyWh = 0.0;
                        for (LocalDateTime t : times) {
                            energyWh += prob.evaluateOpticalMetrics(params, t).get("Q_opt")
                                    * DT_HOURS;
                        }

                        // ground footprint [m2]
                        double groundM2 = (((N - 1) * p + w) / 100.0) * (L / 100.0);
                        double perLand = energyWh / groundM2;

                        if (energyWh > bestE) {
                            bestE = energyWh;
                            bestPwE = pw;
                        }
                        if (perLand > bestLand) {
                            bestLand = perLand;
                            bestPwLand = pw;
                        }

                        if (Math.abs(pw - 1.5) < 1e-6) {
                            e15 = energyWh;
                        }
                        if (Math.abs(pw - 2.0) < 1e-6) {
                            e20 = energyWh;
                        }
                        if (Math.abs(pw - 3.0) < 1e-6) {
                            e30 = energyWh;
                        }

                        out.printf(Locale.US, "%s,%s,%.2f,%.1f,%.4f,%.4f,%.4f,%d%n",
                                siteNames[s], dateNames[d], pw, p,
                                energyWh, groundM2, perLand, times.size());
                    }

                    System.out.printf("%-12s %-8s %10.1f %12.1f %12.1f %8.3f %8.3f %8.3f%n",
                            siteNames[s], dateNames[d],
                            bestPwE, bestPwLand, bestE,
                            e15 / bestE, e20 / bestE, e30 / bestE);
                }
            }

            System.out.printf("%nSaved: %s%n", csv);

        } catch (java.io.IOException e) {
            System.err.println("Write error: " + e.getMessage());
        }

        System.out.println();
        System.out.println("[How to read this]");
        System.out.println("  E1.5 / E2.0 / E3.0 are daily energies relative to the optimum.");
        System.out.println("  Values close to 1.000 mean the optimum is FLAT - i.e. the exact");
        System.out.println("  pitch matters little and a single recommended p/w is defensible.");
        System.out.println("  A sharp optimum that also drifts with latitude/season would mean");
        System.out.println("  the rule must be stated as a function of site, not as a constant.");
    }

// ============================================================================
// REPLACEMENT for TestOptimization.runHeightSweepExport()   [v3 - CLEAN SWEEP]
//
// WHY v2 WAS INCONCLUSIVE
// -----------------------
// v2 varied N, w and p simultaneously, so three dimensionless groups moved at
// once and the result could not be attributed to any of them:
//
//   Wf(m)   Hr_opt   Hr/Wf     w(cm)   note
//   0.85     118     1.388     10.0    <- HIT THE SWEEP CEILING (1.40*Wf=119)
//   1.45     167     1.15      10.0
//   2.23     128     0.59      13.3    <- only row with w != Dr
//   3.90     244     0.63      10.0
//   7.35     289     0.39      10.0
//
// Two defects:
//   (a) The 0.85 m row was truncated by my own sweep ceiling. All three sites
//       returned exactly 118 cm because the search stopped at 119 cm. That
//       1.388 is an artefact, not a measurement.
//   (b) The 2.23 m row was the only configuration with w = 13.3 cm while
//       Dr = 10 cm. Once w > Dr the mirror image is wider than the absorber,
//       the system becomes spillage-dominated, and the optimiser pulls Hr
//       DOWN to shorten the slant distance. That single row is why the trend
//       looked non-monotonic (167 -> 128 -> 244).
//
// WHAT v3 CHANGES
// ---------------
//   w = 10 cm and p = 15 cm are held FIXED for every configuration, so
//       w / Dr = 1.0     and     p / w = 1.5
//   are constant and ONLY N varies. Field width then spans 0.55 m to 5.95 m
//   (more than a factor of ten) with every other dimensionless group frozen.
//
//   The sweep is uniform in Hr/Wf from 0.2 to 3.0 (40 points), so the search
//   range scales with the geometry and cannot clip the optimum.
//
//   Energy is integrated over four representative days at 15-minute steps
//   with an alpha > 5 deg cutoff - the metric that passed the equinox
//   symmetry check in the p/w sweep.
//
// READING THE RESULT
//   Hr/Wf constant down the column        -> genuine dimensionless rule
//   Hr/Wf drifting monotonically with Wf  -> report as a fitted scaling,
//                                            not as a constant
//   E@0.5 / E@1.0 / E@1.5 near 1.000      -> optimum is flat, so a single
//                                            recommended ratio is defensible
//                                            even if the peak drifts
// ============================================================================
    public static void runHeightSweepExport() {

        System.out.println("=== TEST 6 v3: Hr/Wf scaling | w and p/w held fixed ===\n");

        final double DT_HOURS = 0.25;
        final double ALT_FLOOR = 5.0;

        final double W_CM = 10.0;   // = Dr, so w/Dr = 1.0 throughout
        final double P_CM = 15.0;   // so p/w = 1.5 throughout

        int[] cfgN = {4, 6, 10, 16, 24, 40};

        String[] siteNames = {"Jeddah", "Diyarbakir", "Berlin"};
        double[] siteLat = {LAT_JEDDAH, LAT_DIYARBAKIR, LAT_BERLIN};
        double[] siteLon = {LON_JEDDAH, LON_DIYARBAKIR, LON_BERLIN};

        int[][] dates = {{3, 21}, {6, 21}, {9, 21}, {12, 21}};

        final double R_HI = 3.00;   // Hr/Wf sweep end
//        final double R_LO = 0.20;   // Hr/Wf sweep start
//        final int NSTEPS = 40;
        final double R_LO = 0.05;   // eski: 0.20
        final int NSTEPS = 60;     // eski: 40

        System.out.printf("Fixed: w=%.0f cm (w/Dr=1.0), p=%.0f cm (p/w=1.5) | Hr/Wf swept %.1f..%.1f%n%n",
                W_CM, P_CM, R_LO, R_HI);
        System.out.printf("%-12s %5s %8s %11s %9s %12s %8s %8s %8s%n",
                "Site", "N", "Wf(m)", "Hr_opt(cm)", "Hr/Wf", "E@opt(kWh)",
                "E@0.5", "E@1.0", "E@1.5");
        System.out.println("-".repeat(94));

        String csv = "hr_scaling_sweep.csv";
        try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter(csv))) {

            out.println("site,latitude,N,Wf_m,hr_over_wf,Hr_cm,energy_kWh");

            for (int s = 0; s < siteNames.length; s++) {

                SolarCalculator calc = new SolarCalculator(siteLat[s], siteLon[s], 0);
                List<LocalDateTime> times = new ArrayList<>();
                for (int[] dt : dates) {
                    for (int h = 3; h <= 21; h++) {
                        for (int mm = 0; mm < 60; mm += 15) {
                            LocalDateTime t = LocalDateTime.of(2024, dt[0], dt[1], h, mm);
                            if (calc.calculateSolarPosition(t).getAltitudeAngle() > ALT_FLOOR) {
                                times.add(t);
                            }
                        }
                    }
                }
                if (times.isEmpty()) {
                    continue;
                }

                FresnelDesignProblem prob
                        = new FresnelDesignProblem(siteLat[s], siteLon[s], times);

                for (int N : cfgN) {

                    double Wf_m = ((N - 1) * P_CM + W_CM) / 100.0;

                    double bestE = -1.0, bestRatio = 0.0;
                    int bestHr = 0;
                    double e05 = 0.0, e10 = 0.0, e15 = 0.0;

                    for (int k = 0; k <= NSTEPS; k++) {

                        double ratio = R_LO + (R_HI - R_LO) * k / (double) NSTEPS;
                        double hrCm = ratio * Wf_m * 100.0;
                        if (hrCm < 20.0) {
                            continue;
                        }

                        DesignParameters params
                                = new DesignParameters(hrCm, W_CM, P_CM, N);

                        double energyWh = 0.0;
                        for (LocalDateTime t : times) {
                            energyWh += prob.evaluateOpticalMetrics(params, t).get("Q_opt")
                                    * DT_HOURS;
                        }

                        if (energyWh > bestE) {
                            bestE = energyWh;
                            bestRatio = ratio;
                            bestHr = (int) Math.round(hrCm);
                        }
                        if (Math.abs(ratio - 0.5) < 0.036) {
                            e05 = energyWh;
                        }
                        if (Math.abs(ratio - 1.0) < 0.036) {
                            e10 = energyWh;
                        }
                        if (Math.abs(ratio - 1.5) < 0.036) {
                            e15 = energyWh;
                        }

                        out.printf(Locale.US, "%s,%.2f,%d,%.4f,%.4f,%.1f,%.4f%n",
                                siteNames[s], siteLat[s], N, Wf_m, ratio, hrCm,
                                energyWh / 1000.0);
                    }

                    System.out.printf("%-12s %5d %8.3f %11d %9.3f %12.1f %8.3f %8.3f %8.3f%n",
                            siteNames[s], N, Wf_m, bestHr, bestRatio, bestE / 1000.0,
                            e05 / bestE, e10 / bestE, e15 / bestE);
                }
                System.out.println();
            }

            System.out.printf("Saved: %s%n", csv);

        } catch (java.io.IOException e) {
            System.err.println("Write error: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 7 -- Daily efficiency profile export (Fig. 8)
    // ================================================================
    public static void runDailyEfficiencyProfile() {
        System.out.println("=== TEST 7: Daily Efficiency Profile (Fig. 8) ===\n");
        System.out.printf("June 21 | %.2fdegN %.2fdegE%n", LAT_DIYARBAKIR, LON_DIYARBAKIR);

        // Baseline configurations -- Table 10
        int[] nArr = {6, 10, 4};
        int[] pArr = {15, 20, 10};
        int[] hrArr = {130, 180, 100};
        int[] wArr = {10, 10, 12};
        String[] names = {"Standard (N=6)", "High-Conc. (N=10)", "Compact (N=4)"};

        int[] hours = new int[15];
        for (int i = 0; i < 15; i++) {
            hours[i] = i + 6;
        }

        double[][] etaOpt = new double[3][15];
        for (int c = 0; c < 3; c++) {
            // 4-param constructor
            DesignParameters params = new DesignParameters(hrArr[c], wArr[c], pArr[c], nArr[c]);
            List<LocalDateTime> times = new ArrayList<>();
            for (int h : hours) {
                times.add(LocalDateTime.of(2024, Month.JUNE, 21, h, 0));
            }
            FresnelDesignProblem prob
                    = new FresnelDesignProblem(LAT_DIYARBAKIR, LON_DIYARBAKIR, times);
            for (int hi = 0; hi < 15; hi++) {
                var m = prob.evaluateOpticalMetrics(params,
                        LocalDateTime.of(2024, Month.JUNE, 21, hours[hi], 0));
                // Full η_opt from evaluateOpticalMetrics already includes
                // cosine, shading, end-loss, spillage, and ρ_m -- Eq. (14)
                etaOpt[c][hi] = m.get("eta_opt");
            }
        }

        System.out.printf("%-6s", "Hour");
        for (String n : names) {
            System.out.printf(" %22s", n + " (%)");
        }
        System.out.println();
        System.out.println("-".repeat(6 + 3 * 23));
        for (int hi = 0; hi < 15; hi++) {
            System.out.printf("%-6d", hours[hi]);
            for (int c = 0; c < 3; c++) {
                System.out.printf(" %22.4f", etaOpt[c][hi]);
            }
            System.out.println();
        }

        String csv = "daily_efficiency_profile.csv";
        try (var pw = new java.io.PrintWriter(new java.io.FileWriter(csv))) {
            pw.println("hour,standard_n6,highconc_n10,compact_n4");
            for (int hi = 0; hi < 15; hi++) {
                pw.printf(Locale.US, "%d,%.6f,%.6f,%.6f%n",
                        hours[hi], etaOpt[0][hi], etaOpt[1][hi], etaOpt[2][hi]);
            }
            System.out.printf("%nSaved: %s%n", csv);
        } catch (java.io.IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 8 -- Convergence data export (Fig. 9)
    // ================================================================
    public static void runConvergenceExport() {
        System.out.println("=== TEST 8: Convergence Export (Fig. 9) ===");
        System.out.printf("%d runs/algorithm | %.2fdegN %.2fdegE | H=144%n%n",
                NUM_RUNS, LAT_DIYARBAKIR, LON_DIYARBAKIR);
        List<LocalDateTime> times = evaluationTimes(144);
        FresnelDesignProblem problem = new FresnelDesignProblem(
                LAT_DIYARBAKIR, LON_DIYARBAKIR, times);
        DesignParameters init = new DesignParameters(130.0, 10.0, 15.0, 4);

        exportConvergence("SA", () -> buildSA(), problem, init);
        exportConvergence("GA", () -> buildGA(), problem, init);
        exportConvergence("PSO", () -> buildPSO(), problem, init);
        System.out.println("\n=== Convergence CSV files exported. ===");
    }

    @FunctionalInterface
    private interface AlgoFactory {

        IOptimizationAlgorithm create();
    }

    private static void exportConvergence(String name, AlgoFactory factory,
            FresnelDesignProblem problem,
            DesignParameters init) {
        System.out.printf("Running %s (%d runs)...%n", name, NUM_RUNS);
        String csv = "convergence_" + name + ".csv";
        try (var pw = new java.io.PrintWriter(new java.io.FileWriter(csv))) {
            pw.println("run,iteration,best_fitness");
            double[] finals = new double[NUM_RUNS];
            long[] times2 = new long[NUM_RUNS];
            for (int r = 0; r < NUM_RUNS; r++) {
                var algo = factory.create();
                algo.reset();
                long t0 = System.currentTimeMillis();
                var sol = algo.optimize(problem, init, new HashMap<>());
                times2[r] = System.currentTimeMillis() - t0;
                finals[r] = sol.getObjectiveValue();
                double best = Double.NEGATIVE_INFINITY;
                var hist = algo.getHistory();
                for (int i = 0; i < hist.size(); i++) {
                    best = Math.max(best, hist.get(i).getObjectiveValue());
                    pw.printf(Locale.US, "%d,%d,%.6f%n", r, i, best);
                }
                System.out.printf("  Run %d/%d -> %.2f (%d ms)%n",
                        r + 1, NUM_RUNS, finals[r], times2[r]);
            }
            System.out.println();
            double mean = Arrays.stream(finals).average().orElse(0);
            double best = Arrays.stream(finals).max().orElse(0);
            double std = Math.sqrt(Arrays.stream(finals)
                    .map(v -> (v - mean) * (v - mean)).average().orElse(0));
            System.out.printf("  %s: best=%.2f mean=%.2f std=%.2f avg=%.1f ms%n",
                    name, best, mean, std,
                    Arrays.stream(times2).average().orElse(0));
            System.out.printf("  Saved: %s%n", csv);
        } catch (java.io.IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    // ================================================================
    // ================================================================
    // ================================================================
    // TEST 9 -- SolTrace cross-validation (Table 6 + Table 7)
    //
    // Generates JFS daily-average eta_opt for 28 configurations spanning
    // three parametric sweeps:
    //   - Spacing sweep  (p/w = 0.75..3.5, 10 configs) -> Table 11 + Spearman
    //   - Mirror count   (N = 2,4,6,8,10,12, 5 configs)-> Table 12 + Spearman
    //   - Height sweep   (Hr = 80..250, 8 configs)      -> Fig.7   + Spearman
    //   - G1-G5 baseline (5 configs)                    -> Table 6
    //
    // Spearman rho is computed from JFS ranks alone here.
    // SolTrace ranks must be added manually after running the companion
    // SolTrace script; paste them into the CSV column "soltrace_rank".
    //
    // Output: soltrace_validation_jfs.csv  (Table 6 + Table 7 source data)
    // ================================================================
    public static void runSolTraceValidation() {
        System.out.println("=== TEST 9: SolTrace Cross-Validation (Table 6 + Table 7) ===");
        System.out.printf("Site: %.2f degN %.2f degE | June 21 daily avg (07:00-18:00)%n",
                LAT_DIYARBAKIR, LON_DIYARBAKIR);
        System.out.println("Dr=10 cm, L=1000 cm, w=10 cm (fixed) | rho_m=0.92 | sigma=4.65 mrad");
        System.out.println();

        // Daily evaluation times 07:00-18:00
        List<LocalDateTime> dailyTimes = new ArrayList<>();
        for (int h = 7; h <= 18; h++) {
            dailyTimes.add(LocalDateTime.of(2024, Month.JUNE, 21, h, 0));
        }

        FresnelDesignProblem problem
                = new FresnelDesignProblem(LAT_DIYARBAKIR, LON_DIYARBAKIR, dailyTimes);

        // ----------------------------------------------------------------
        // Build 28-configuration set
        // Each entry: { label, Hr, w, p, N, sweep_group }
        // ----------------------------------------------------------------
        List<Object[]> configs = new ArrayList<>();

        // GROUP A — G1-G5 baseline geometries (Table 6), w=10cm
        configs.add(new Object[]{"G1-Compact", 100.0, 10.0, 10.0, 4, "Baseline"});
        configs.add(new Object[]{"G2-Standard", 130.0, 10.0, 15.0, 6, "Baseline"});
        configs.add(new Object[]{"G3-WideSpaced", 130.0, 10.0, 25.0, 6, "Baseline"});
        configs.add(new Object[]{"G4-HighFocus", 200.0, 10.0, 17.5, 8, "Baseline"});
        configs.add(new Object[]{"G5-LargeField", 250.0, 10.0, 20.0, 16, "Baseline"});

        // GROUP B — Spacing sweep (p/w = 0.75..3.5, N=6, Hr=130, w=10)
        // p/w: 0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.25, 2.5, 3.0, 3.5
        double[] pwRatios = {0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.25, 2.5, 3.0, 3.5};
        for (double pw : pwRatios) {
            double p = pw * 10.0;  // w=10cm
            configs.add(new Object[]{
                String.format("Sp-pw%.2f", pw), 130.0, 10.0, p, 6, "SpacingSweep"});
        }

        // GROUP C — Mirror count sweep (N=2..10, p=15, Hr=130, w=10)
        int[] nVals = {2, 4, 6, 8, 10};
        for (int n : nVals) {
            configs.add(new Object[]{
                "Mc-N" + n, 130.0, 10.0, 15.0, n, "MirrorCount"});
        }

        // GROUP D — Receiver height sweep (Hr=80..250, N=6, p=15, w=10)
        int[] hrVals = {80, 100, 120, 140, 160, 180, 210, 250};
        for (int hr : hrVals) {
            configs.add(new Object[]{
                "Hr-" + hr, (double) hr, 10.0, 15.0, 6, "HeightSweep"});
        }

        System.out.printf("Total configurations: %d%n%n", configs.size());

        // ----------------------------------------------------------------
        // Evaluate all configurations
        // ----------------------------------------------------------------
        double[] etaJFS = new double[configs.size()];

        System.out.printf("%-18s %-12s %4s %6s %6s | %9s %9s %9s %9s %11s%n",
                "Label", "Group", "N", "p/w", "Hr",
                "cos(%)", "shad(%)", "end(%)", "spill(%)", "eta_opt(%)");
        System.out.println("-".repeat(100));

        for (int i = 0; i < configs.size(); i++) {
            Object[] c = configs.get(i);
            String label = (String) c[0];
            double hr = (double) c[1];
            double w = (double) c[2];
            double p = (double) c[3];
            int n = (Integer) c[4];
            String grp = (String) c[5];

            DesignParameters params = new DesignParameters(hr, w, p, n);

            double sumCos = 0, sumShad = 0, sumEnd = 0, sumSpill = 0, sumEta = 0;
            int valid = 0;
            for (LocalDateTime t : dailyTimes) {
                Map<String, Double> m = problem.evaluateOpticalMetrics(params, t);
                if (m.get("cosine") > 0) {
                    sumCos += m.get("cosine");
                    sumShad += m.get("shading");
                    sumEnd += m.get("endloss");
                    sumSpill += m.get("spillage");
                    sumEta += m.get("eta_opt");
                    valid++;
                }
            }
            double avgCos = valid > 0 ? sumCos / valid : 0;
            double avgShad = valid > 0 ? sumShad / valid : 0;
            double avgEnd = valid > 0 ? sumEnd / valid : 0;
            double avgSpill = valid > 0 ? sumSpill / valid : 0;
            double avgEta = valid > 0 ? sumEta / valid : 0;

            etaJFS[i] = avgEta;

            System.out.printf("%-18s %-12s %4d %6.2f %6.0f | %9.2f %9.2f %9.2f %9.2f %11.2f%n",
                    label, grp, n, p / w, hr,
                    avgCos, avgShad, avgEnd, avgSpill, avgEta);
        }

        // ----------------------------------------------------------------
        // Compute JFS ranks (1 = best eta_opt)
        // ----------------------------------------------------------------
        int[] jfsRank = computeRanks(etaJFS);

        // ----------------------------------------------------------------
        // Print ranking summary per group
        // ----------------------------------------------------------------
        System.out.println();
        System.out.println("--- JFS Rankings by Group ---");
        String[] groups = {"Baseline", "SpacingSweep", "MirrorCount", "HeightSweep"};
        for (String grp : groups) {
            System.out.printf("%n  [%s]%n", grp);
            System.out.printf("  %-18s %11s  %4s%n", "Label", "eta_opt(%)", "Rank");
            System.out.println("  " + "-".repeat(38));
            for (int i = 0; i < configs.size(); i++) {
                if (configs.get(i)[5].equals(grp)) {
                    System.out.printf("  %-18s %11.2f  %4d%n",
                            configs.get(i)[0], etaJFS[i], jfsRank[i]);
                }
            }
        }

        // ----------------------------------------------------------------
        // Spearman placeholder note
        // ----------------------------------------------------------------
        System.out.println();
        System.out.println("--- Spearman rho (Table 7) ---");
        System.out.println("JFS ranks computed above. Run companion SolTrace script");
        System.out.println("to get SolTrace ranks, then compute:");
        System.out.println("  rho = 1 - 6*sum(d_i^2) / (n*(n^2-1))");
        System.out.printf("  Expected: rho = 1.000 across all %d configurations.%n",
                configs.size());

        // ----------------------------------------------------------------
        // CSV export — ready for SolTrace rank column to be filled in
        // ----------------------------------------------------------------
        String csv = "soltrace_validation_jfs.csv";
        try (var pw = new java.io.PrintWriter(new java.io.FileWriter(csv))) {
            pw.println("label,group,Hr_cm,w_cm,p_cm,N,pw_ratio,"
                    + "eta_opt_jfs_pct,jfs_rank,soltrace_eta_pct,soltrace_rank,d,d_sq");
            for (int i = 0; i < configs.size(); i++) {
                Object[] c = configs.get(i);
                double p = (double) c[3];
                double w = (double) c[2];
                pw.printf(Locale.US,
                        "%s,%s,%.1f,%.1f,%.1f,%d,%.3f,%.4f,%d,,,,\n",
                        c[0], c[5], c[1], w, p, c[4], p / w,
                        etaJFS[i], jfsRank[i]);
            }
            System.out.printf("%nSaved: %s%n", csv);
            System.out.println("Fill columns soltrace_eta_pct and soltrace_rank after");
            System.out.println("running the SolTrace validation script.");
        } catch (java.io.IOException e) {
            System.err.println("CSV write error: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 10 -- Table 6 generator
    //
    // Produces the JFS side of Table 6:
    //   "System-level optical efficiency validation: analytical model
    //    vs. SolTrace MCRT at solar noon on June 21, Diyarbakir."
    //
    // SolTrace values must be filled in manually after running the
    // companion SolTrace script. The CSV has placeholder columns for
    // eta_soltrace, delta_eta, and daily_yield_dev.
    //
    // Solar noon is used here (consistent with Table 6 in manuscript)
    // because SolTrace validation is typically reported at a single
    // representative time point. Daily-average Spearman is in Test 9.
    // ================================================================
    public static void runTable6() {
        System.out.println("=== TEST 10: Table 6 Generator (G1-G5 vs SolTrace) ===");
        System.out.printf("Site: %.2f degN %.2f degE | Solar noon June 21%n",
                LAT_DIYARBAKIR, LON_DIYARBAKIR);
        System.out.println("Dr=10 cm, L=1000 cm, w=10 cm (fixed)");
        System.out.println();

        // G1-G5 geometries — Table 5 of manuscript
        Object[][] geometries = {
            {"G1 (Compact)", 100.0, 10.0, 10.0, 4},
            {"G2 (Standard)", 130.0, 10.0, 15.0, 6},
            {"G3 (Wide-spaced)", 130.0, 10.0, 25.0, 6},
            {"G4 (High-focus)", 200.0, 10.0, 17.5, 8},
            {"G5 (Large-field)", 250.0, 10.0, 20.0, 16},};

        // Solar noon evaluation
        LocalDateTime solarNoon = LocalDateTime.of(2024, Month.JUNE, 21, 12, 0);
        List<LocalDateTime> times = List.of(solarNoon);
        FresnelDesignProblem problem
                = new FresnelDesignProblem(LAT_DIYARBAKIR, LON_DIYARBAKIR, times);

        // Table 6 header
        System.out.printf("%-20s %14s %16s %10s %18s%n",
                "Geometry",
                "eta_JFS (%)",
                "eta_SolTrace (%)",
                "Delta (pp)",
                "Daily yield dev (%)");
        System.out.println("-".repeat(82));

        double[] etaJFS = new double[geometries.length];

        for (int i = 0; i < geometries.length; i++) {
            Object[] g = geometries[i];
            String name = (String) g[0];
            double hr = (double) g[1];
            double w = (double) g[2];
            double p = (double) g[3];
            int n = (Integer) g[4];

            DesignParameters params = new DesignParameters(hr, w, p, n);
            Map<String, Double> m = problem.evaluateOpticalMetrics(params, solarNoon);
            etaJFS[i] = m.get("eta_opt");

            System.out.printf("%-20s %14.2f %16s %10s %18s%n",
                    name, etaJFS[i],
                    "[SolTrace]", "[Δ]", "[dev]");
        }

        System.out.println("-".repeat(82));
        System.out.printf("%-20s %14s %16s %10s %18s%n",
                "RMSE", "", "[x.x pp]", "", "[x.x%]");

        // JFS ranking
        System.out.println();
        System.out.println("--- JFS Ranking at Solar Noon ---");
        System.out.printf("%-4s %-20s %12s%n", "Rank", "Geometry", "eta_JFS (%)");
        System.out.println("-".repeat(40));
        Integer[] idx = {0, 1, 2, 3, 4};
        Arrays.sort(idx, (a, b) -> Double.compare(etaJFS[b], etaJFS[a]));
        for (int r = 0; r < idx.length; r++) {
            System.out.printf("%-4d %-20s %12.2f%n",
                    r + 1, geometries[idx[r]][0], etaJFS[idx[r]]);
        }

        // Per-component breakdown for transparency
        System.out.println();
        System.out.println("--- Per-component breakdown (solar noon) ---");
        System.out.printf("%-20s %8s %8s %8s %8s %10s%n",
                "Geometry", "cos(%)", "shad(%)", "end(%)", "spill(%)", "eta_opt(%)");
        System.out.println("-".repeat(66));
        for (int i = 0; i < geometries.length; i++) {
            Object[] g = geometries[i];
            DesignParameters params = new DesignParameters(
                    (double) g[1], (double) g[2], (double) g[3], (Integer) g[4]);
            Map<String, Double> m = problem.evaluateOpticalMetrics(params, solarNoon);
            System.out.printf("%-20s %8.2f %8.2f %8.2f %8.2f %10.2f%n",
                    g[0],
                    m.get("cosine"), m.get("shading"),
                    m.get("endloss"), m.get("spillage"),
                    m.get("eta_opt"));
        }

        // CSV export — ready for SolTrace values
        String csv = "table6_jfs.csv";
        try (var pw = new java.io.PrintWriter(new java.io.FileWriter(csv))) {
            pw.println("geometry,Hr_cm,w_cm,p_cm,N,pw_ratio,"
                    + "eta_jfs_pct,eta_soltrace_pct,delta_pp,daily_yield_dev_pct");
            for (int i = 0; i < geometries.length; i++) {
                Object[] g = geometries[i];
                double p = (double) g[3];
                double w = (double) g[2];
                pw.printf(Locale.US,
                        "%s,%.1f,%.1f,%.1f,%d,%.2f,%.4f,,,%n",
                        g[0], g[1], w, p, g[4], p / w, etaJFS[i]);
            }
            System.out.printf("%nSaved: %s%n", csv);
            System.out.println("Fill eta_soltrace_pct column after running SolTrace script.");
            System.out.println("delta_pp and daily_yield_dev_pct will be computed automatically.");
        } catch (java.io.IOException e) {
            System.err.println("CSV write error: " + e.getMessage());
        }
    }

    /**
     * Computes ranks for an array of values (1 = highest value). Ties receive
     * the average rank (standard competition ranking).
     */
    private static int[] computeRanks(double[] values) {
        int n = values.length;
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) {
            idx[i] = i;
        }
        Arrays.sort(idx, (a, b) -> Double.compare(values[b], values[a]));
        int[] rank = new int[n];
        for (int r = 0; r < n; r++) {
            rank[idx[r]] = r + 1;
        }
        return rank;
    }

    // ================================================================
    // TEST 0 -- Run all tests
    // ================================================================
    public static void runAllTests() {
        String sep = "\n" + "=".repeat(60) + "\n";
        runExtremeAngleAnalysis();
        System.out.print(sep);
        runMirrorCountScaling();
        System.out.print(sep);
        runOptimizationComparison();
        System.out.print(sep);
        runTemporalSensitivity();
        System.out.print(sep);
        runSpacingSweepExport();
        System.out.print(sep);
        runHeightSweepExport();
        System.out.print(sep);
        runDailyEfficiencyProfile();
        System.out.print(sep);
        runConvergenceExport();
        System.out.println("\n=== ALL TESTS COMPLETED ===");
    }

    // ================================================================
    // HELPERS
    // ================================================================
    private static List<LocalDateTime> evaluationTimes(int H) {
        List<LocalDateTime> t = new ArrayList<>();
        int y = 2024;
        if (H <= 144) {
            for (Month m : Month.values()) {
                for (int h = 7; h <= 18; h++) {
                    t.add(LocalDateTime.of(y, m, 15, h, 0));
                }
            }
        } else if (H <= 288) {
            for (Month m : Month.values()) {
                for (int h = 7; h <= 18; h++) {
                    t.add(LocalDateTime.of(y, m, 15, h, 0));
                    t.add(LocalDateTime.of(y, m, 15, h, 30));
                }
            }
        } else {
            for (int m = 1; m <= 12; m++) {
                int max = LocalDateTime.of(y, m, 1, 0, 0).getMonth().length(true);
                for (int d = 1; d <= max; d++) {
                    for (int h = 7; h <= 18; h++)
                        try {
                        t.add(LocalDateTime.of(y, m, d, h, 0));
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return t;
    }

    private static List<IOptimizationAlgorithm> buildAlgorithms() {
        return List.of(buildGA(), buildPSO(), buildSA());
    }

    private static GeneticAlgorithm buildGA() {
        GeneticAlgorithm ga = new GeneticAlgorithm();
        ga.setParameters(Map.of("populationSize", 50, "maxGenerations", 100,
                "crossoverRate", 0.8, "mutationRate", 0.1, "elitismRate", 0.1));
        return ga;
    }

    private static ParticleSwarm buildPSO() {
        ParticleSwarm pso = new ParticleSwarm();
        pso.setParameters(Map.of("swarmSize", 30, "maxIterations", 100,
                "inertiaWeight", 0.729, "cognitiveWeight", 1.49445, "socialWeight", 1.49445));
        return pso;
    }

    private static SimulatedAnnealing buildSA() {
        SimulatedAnnealing sa = new SimulatedAnnealing();
        sa.setParameters(Map.of("initialTemperature", 1000.0, "coolingRate", 0.95,
                "maxIterations", 1000, "minTemperature", 1e-10));
        return sa;
    }

    /**
     * LFR GLOBAL OPTIMIZATION BENCHMARK & RANKING VALIDATION
     *
     * PHASE 1: Metaheuristic Algorithm Comparison (GA, PSO, SA) PHASE 2:
     * Stratified Spectrum Generation for SolTrace MCRT Cross-Validation
     *
     * @author Yunus Demirtas, Musa Atas — Siirt University
     */
    public static void runGlobalOptimization() {
        String[] locations = {"Jeddah", "Diyarbakir", "Berlin"};
        double[][] coords = {{21.49, 39.19}, {37.91, 40.24}, {52.52, 13.40}};

        int numRuns = 10;

        // --- DYNAMIC BOUNDS FETCHING FROM CONFIG ---
        // This ensures Phase 2 samples stay within the user-defined industrial limits.
        double minH = ConfigManager.getDouble("min_rec_height", 50.0);
        double maxH = ConfigManager.getDouble("max_rec_height", 400.0);
        double minW = ConfigManager.getDouble("min_mirror_width", 15.0);
        double maxW = ConfigManager.getDouble("max_mirror_width", 25.0);
        double minP = ConfigManager.getDouble("min_mirror_spacing", 20.0);
        double maxP = ConfigManager.getDouble("max_mirror_spacing", 100.0);
        int minN = ConfigManager.getInt("min_mirrors", 4);
        int maxN = ConfigManager.getInt("max_mirrors", 30);
        DesignParameters initialParams = new DesignParameters(150.0, minW, minP, 10);

        double searchSpaceVolume = (maxN - minN) * (maxH - minH) * (maxW - minW) * (maxP - minP);

        System.out.println("\n" + "=".repeat(130));
        System.out.println("                 LFR GLOBAL OPTIMIZATION & STRATIFIED RANKING VALIDATION REPORT");
        System.out.println(" Methodology: 4-Season TMY Data | Objective: Compounded Performance Index (CPI)");
        System.out.printf(" SAMPLEABLE SEARCH SPACE VOLUME: %.2e units | Stochastic Runs per Algo: %d\n", searchSpaceVolume, numRuns);
        System.out.println("=".repeat(130));

        for (int i = 0; i < 3; i++) {
            FresnelDesignProblem problem = new FresnelDesignProblem(locations[i], coords[i][0], coords[i][1]);

            List<IOptimizationAlgorithm> algorithms = Arrays.asList(
                    new GeneticAlgorithm(),
                    new ParticleSwarm(),
                    new SimulatedAnnealing()
            );

            Map<String, OptimizationStats> cityResults = new LinkedHashMap<>();
            double bestMeanForCity = -1.0;
            String winnerAlgoName = "";

            // --- PHASE 1: ALGORITHM BENCHMARKING ---
            for (IOptimizationAlgorithm algo : algorithms) {
                OptimizationStats stats = runAlgorithmWithStats(algo, problem, initialParams, numRuns);
                String name = algo.getAlgorithmName()
                        .replace("Genetic Algorithm", "GA")
                        .replace("Particle Swarm Optimization", "PSO")
                        .replace("Simulated Annealing", "SA");
                cityResults.put(name, stats);

                if (stats.meanFitness > bestMeanForCity) {
                    bestMeanForCity = stats.meanFitness;
                    winnerAlgoName = name;
                }
            }

            // Table Display
            System.out.printf("| %-12s | %-10s | %-12s | %-12s | %-10s | %-7s | %-45s |\n",
                    "Location", "Algorithm", "Best CPI", "Mean CPI", "Std.Dev", "Time(s)", "Optimal Configuration (Best)");
            String separator = "+" + "-".repeat(14) + "+" + "-".repeat(12) + "+" + "-".repeat(14) + "+" + "-".repeat(14) + "+" + "-".repeat(12) + "+" + "-".repeat(9) + "+" + "-".repeat(47) + "+";
            System.out.println(separator);

            for (Map.Entry<String, OptimizationStats> entry : cityResults.entrySet()) {
                boolean isWinner = entry.getKey().equals(winnerAlgoName);
                String prefix = isWinner ? ">> " : "   ";
                System.out.printf("| %-12s | %-10s | %12.2f | %12.2f | %10.2f | %7.2f | %-45s |\n",
                        isWinner ? prefix + locations[i] : locations[i],
                        isWinner ? "*" + entry.getKey() + "*" : entry.getKey(),
                        entry.getValue().bestFitness, entry.getValue().meanFitness,
                        entry.getValue().stdDev, entry.getValue().avgTime, entry.getValue().bestConfig);
            }
            System.out.println(separator);

// --- PHASE 2: STRATIFIED SPECTRUM GENERATION (Rank #1 to #1000) ---
            // This phase generates a broad spectrum of designs to validate the 
            // analytical model's ranking consistency against high-fidelity MCRT.
            System.out.println("\n>>> GENERATING SPECTRUM SAMPLES FOR MCRT VALIDATION <<<");
            List<DesignSolution> spectrumPool = new ArrayList<>();
            Random rnd = new Random(42); // Seeded for scientific reproducibility

            for (int k = 0; k < 1000; k++) {
                // 1. Facet Width (w): Sampled within industrial bounds set in UI/Config
                double currentW = minW + rnd.nextDouble() * (maxW - minW);

                // 2. Mirror Spacing (p): Enforced p >= w + 10cm to ensure mechanical 
                // clearance for rotation and structural maintenance.
                double physicalMinP = Math.max(minP, currentW + 10.0);
                double effectiveMaxP = Math.max(physicalMinP + 10.0, maxP);
                double currentP = physicalMinP + rnd.nextDouble() * (effectiveMaxP - physicalMinP);

                // 3. Receiver Height (Hr): Minimum threshold of 150 cm enforced to 
                // prevent low-tower optical paradoxes and ensure industrial relevance.
                double samplingMinH = Math.max(minH, 150.0);
                double currentHr = samplingMinH + rnd.nextDouble() * (maxH - samplingMinH);

                // 4. Mirror Count (N): Enforced EVEN numbers (Symmetry Constraint)
                // Mirrors must be in pairs to maintain balanced optical and structural loading.
                int rawN = minN + rnd.nextInt(maxN - minN + 1);
                int evenN = (rawN / 2) * 2;
                if (evenN < 4) {
                    evenN = 4; // Minimum scale for concentrating collectors
                }
                // Create the design candidate with enforced symmetry and spacing
                DesignParameters p = new DesignParameters(
                        currentHr,
                        currentW,
                        currentP,
                        evenN
                );

                // Evaluate and add to the ranking pool
                spectrumPool.add(new DesignSolution(p, problem.evaluateDesign(p)));
            }

            // Order results to pick representatives from the entire spectrum
            spectrumPool.sort((a, b) -> Double.compare(b.getObjectiveValue(), a.getObjectiveValue()));

            // Representative indices for SolTrace verification (Best, Average, Poor, Worst)
            int[] indices = {0, 5, 25, 75, 150, 300, 500, 700, 850, 999};

            System.out.println("SOLTRACE VALIDATION CODE (Copy these arrays into SolTrace v1.6):");
            System.out.println("// --- " + locations[i].toUpperCase() + " Spectrum Data ---");

            System.out.print("N_list  = [");
            for (int idx : indices) {
                System.out.print((int) spectrumPool.get(idx).getParameters().getNumberOfMirrors() + (idx == 999 ? "" : ", "));
            }
            System.out.println("];");

            System.out.print("Hr_list = [");
            for (int idx : indices) {
                System.out.print(String.format(Locale.US, "%.4f%s", spectrumPool.get(idx).getParameters().getReceiverHeight() / 100.0, idx == 999 ? "" : ", "));
            }
            System.out.println("]; // in meters");

            System.out.print("w_list  = [");
            for (int idx : indices) {
                System.out.print(String.format(Locale.US, "%.4f%s", spectrumPool.get(idx).getParameters().getMirrorWidth() / 100.0, idx == 999 ? "" : ", "));
            }
            System.out.println("]; // in meters");

            System.out.print("p_list  = [");
            for (int idx : indices) {
                System.out.print(String.format(Locale.US, "%.4f%s", spectrumPool.get(idx).getParameters().getMirrorSpacing() / 100.0, idx == 999 ? "" : ", "));
            }
            System.out.println("]; // in meters");

            System.out.println("JAVA_STRATIFIED_SCORES = [");
            for (int idx : indices) {
                System.out.println("  Rank #" + (idx + 1) + " : " + String.format(Locale.US, "%.2f", spectrumPool.get(idx).getObjectiveValue()));
            }
            System.out.println("];\n" + "-".repeat(110) + "\n");
        }
    }

    /**
     * Enhanced Stats class for stochastic tracking.
     */
    private static class OptimizationStats {

        double bestFitness;
        double meanFitness;
        double stdDev;
        double avgTime;
        String bestConfig;
        List<DesignSolution> allRuns = new ArrayList<>();
    }

    private static OptimizationStats runAlgorithmWithStats(IOptimizationAlgorithm algo, FresnelDesignProblem problem, DesignParameters init, int runs) {
        long totalTime = 0;
        OptimizationStats stats = new OptimizationStats();

        for (int r = 0; r < runs; r++) {
            algo.reset();
            long t0 = System.currentTimeMillis();
            DesignSolution sol = algo.optimize(problem, init, new HashMap<>());
            totalTime += (System.currentTimeMillis() - t0);
            stats.allRuns.add(sol);
        }

        stats.bestFitness = stats.allRuns.stream().mapToDouble(DesignSolution::getObjectiveValue).max().orElse(0);
        stats.meanFitness = stats.allRuns.stream().mapToDouble(DesignSolution::getObjectiveValue).average().orElse(0);

        double mean = stats.meanFitness;
        double variance = stats.allRuns.stream().mapToDouble(s -> Math.pow(s.getObjectiveValue() - mean, 2)).average().orElse(0);
        stats.stdDev = Math.sqrt(variance);
        stats.avgTime = (totalTime / (double) runs) / 1000.0;

        DesignSolution bestOfAll = stats.allRuns.stream().max(Comparator.comparing(DesignSolution::getObjectiveValue)).get();
        DesignParameters p = bestOfAll.getParameters();
        stats.bestConfig = String.format("N:%d, Hr:%.1fcm, w:%.1fcm, p:%.1fcm, Gap:%.1fcm",
                p.getNumberOfMirrors(), p.getReceiverHeight(),
                p.getMirrorWidth(), p.getMirrorSpacing(),
                (p.getMirrorSpacing() - p.getMirrorWidth()));

        return stats;
    }

    // ============================================================================
// NEW TEST 11 - SOLAR POSITION VERIFICATION vs NREL SPA
// Add this method to TestOptimization.java and wire it into runSelectedTest:
//     case 11 -> runSolarPositionVerification();
//
// WHY THIS EXISTS
// ---------------
// The manuscript's first validation layer (solar position vs NREL SPA) had no
// generating code anywhere in the project, and the published figure peaked at
// ~68 deg solar altitude for Diyarbakir on June 21. The physical value is
// 74.85 deg at the nearest whole hour (75.53 deg at true solar noon, which
// falls at 12:21 local time). Both curves in that figure agreed with each
// other, which means the "NREL SPA" reference was produced by the same faulty
// path as the model - a circular comparison.
//
// REFERENCE DATA
// --------------
// The table below was generated with pvlib 0.15.2 (pvlib.solarposition.
// spa_python), a validated implementation of NREL's Solar Position Algorithm
// (Reda & Andreas, 2004; NREL/TP-560-34302). Values are APPARENT elevation,
// i.e. refraction-corrected, matching the Bennett correction applied inside
// SolarCalculator. Pressure 101325 Pa, temperature 12 C, altitude 0 m.
//
// TIME CONVENTION - must match SolarCalculator exactly:
//   tzMeridian = 15 * round(longitude / 15)
//   Diyarbakir  lon 40.240 -> UTC+3
//   Berlin      lon 13.405 -> UTC+1
//   Jeddah      lon 39.190 -> UTC+3
// No daylight saving is applied on either side.
//
// Only samples with apparent elevation > 5 deg are included; below that,
// refraction models diverge and the geometry is irrelevant for an LFR.
//
// COLUMNS: site, month, day, local hour, SPA apparent elevation, SPA azimuth
// ============================================================================
    public static void runSolarPositionVerification() {

        System.out.println("=== TEST 11: Solar Position vs NREL SPA (pvlib 0.15.2) ===\n");

        // site, lat, lon
        Object[][] sites = {
            {"Diyarbakir", LAT_DIYARBAKIR, LON_DIYARBAKIR},
            {"Berlin", LAT_BERLIN, LON_BERLIN},
            {"Jeddah", LAT_JEDDAH, LON_JEDDAH}
        };

        // site, month, day, hour, spa_elevation_deg, spa_azimuth_deg
        Object[][] REF = {
            {"Diyarbakir", 3, 21, 7, 7.0425, 94.8988},
            {"Diyarbakir", 3, 21, 8, 18.6331, 104.5914},
            {"Diyarbakir", 3, 21, 9, 29.7329, 115.6596},
            {"Diyarbakir", 3, 21, 10, 39.728, 129.313},
            {"Diyarbakir", 3, 21, 11, 47.6623, 147.0015},
            {"Diyarbakir", 3, 21, 12, 52.1148, 169.3441},
            {"Diyarbakir", 3, 21, 13, 51.8038, 193.7945},
            {"Diyarbakir", 3, 21, 14, 46.8355, 215.6154},
            {"Diyarbakir", 3, 21, 15, 38.58, 232.736},
            {"Diyarbakir", 3, 21, 16, 28.412, 245.9991},
            {"Diyarbakir", 3, 21, 17, 17.2316, 256.8512},
            {"Diyarbakir", 3, 21, 18, 5.6381, 266.4607},
            {"Diyarbakir", 6, 21, 6, 10.3693, 68.2183},
            {"Diyarbakir", 6, 21, 7, 21.5904, 76.4392},
            {"Diyarbakir", 6, 21, 8, 33.2356, 84.6793},
            {"Diyarbakir", 6, 21, 9, 45.0461, 93.7992},
            {"Diyarbakir", 6, 21, 10, 56.6975, 105.4891},
            {"Diyarbakir", 6, 21, 11, 67.4652, 124.1395},
            {"Diyarbakir", 6, 21, 12, 74.8548, 161.3218},
            {"Diyarbakir", 6, 21, 13, 73.296, 212.7664},
            {"Diyarbakir", 6, 21, 14, 64.3907, 242.7301},
            {"Diyarbakir", 6, 21, 15, 53.223, 258.4842},
            {"Diyarbakir", 6, 21, 16, 41.4767, 269.1213},
            {"Diyarbakir", 6, 21, 17, 29.6882, 277.8531},
            {"Diyarbakir", 6, 21, 18, 18.1421, 286.0149},
            {"Diyarbakir", 6, 21, 19, 7.115, 294.3518},
            {"Diyarbakir", 9, 21, 7, 9.8535, 97.0137},
            {"Diyarbakir", 9, 21, 8, 21.3628, 106.9638},
            {"Diyarbakir", 9, 21, 9, 32.2484, 118.5445},
            {"Diyarbakir", 9, 21, 10, 41.8412, 133.0457},
            {"Diyarbakir", 9, 21, 11, 49.0692, 151.8747},
            {"Diyarbakir", 9, 21, 12, 52.4498, 175.0901},
            {"Diyarbakir", 9, 21, 13, 50.9373, 199.2797},
            {"Diyarbakir", 9, 21, 14, 45.0335, 219.9854},
            {"Diyarbakir", 9, 21, 15, 36.208, 236.0336},
            {"Diyarbakir", 9, 21, 16, 25.7254, 248.5783},
            {"Diyarbakir", 9, 21, 17, 14.3848, 259.0223},
            {"Diyarbakir", 12, 21, 9, 13.2245, 134.4099},
            {"Diyarbakir", 12, 21, 10, 20.7462, 146.439},
            {"Diyarbakir", 12, 21, 11, 26.0562, 160.2655},
            {"Diyarbakir", 12, 21, 12, 28.5462, 175.4916},
            {"Diyarbakir", 12, 21, 13, 27.8659, 191.0835},
            {"Diyarbakir", 12, 21, 14, 24.1159, 205.8088},
            {"Diyarbakir", 12, 21, 15, 17.7876, 218.8835},
            {"Diyarbakir", 12, 21, 16, 9.5271, 230.173},
            {"Berlin", 3, 21, 7, 7.5169, 99.011},
            {"Berlin", 3, 21, 8, 16.2639, 111.4335},
            {"Berlin", 3, 21, 9, 24.2948, 124.9455},
            {"Berlin", 3, 21, 10, 31.0259, 140.0997},
            {"Berlin", 3, 21, 11, 35.7926, 157.1649},
            {"Berlin", 3, 21, 12, 37.949, 175.7507},
            {"Berlin", 3, 21, 13, 37.1352, 194.6712},
            {"Berlin", 3, 21, 14, 33.4943, 212.5332},
            {"Berlin", 3, 21, 15, 27.5811, 228.562},
            {"Berlin", 3, 21, 16, 20.077, 242.7731},
            {"Berlin", 3, 21, 17, 11.6023, 255.6303},
            {"Berlin", 6, 21, 5, 8.8352, 62.5518},
            {"Berlin", 6, 21, 6, 17.2432, 73.6957},
            {"Berlin", 6, 21, 7, 26.1766, 84.9507},
            {"Berlin", 6, 21, 8, 35.2746, 96.9684},
            {"Berlin", 6, 21, 9, 44.1156, 110.7065},
            {"Berlin", 6, 21, 10, 52.0788, 127.5875},
            {"Berlin", 6, 21, 11, 58.1471, 149.3211},
            {"Berlin", 6, 21, 12, 60.8822, 176.0898},
            {"Berlin", 6, 21, 13, 59.2997, 203.6937},
            {"Berlin", 6, 21, 14, 54.0014, 226.9522},
            {"Berlin", 6, 21, 15, 46.4384, 245.013},
            {"Berlin", 6, 21, 16, 37.7649, 259.4553},
            {"Berlin", 6, 21, 17, 28.69, 271.8357},
            {"Berlin", 6, 21, 18, 19.6742, 283.226},
            {"Berlin", 6, 21, 19, 11.0835, 294.3476},
            {"Berlin", 9, 21, 7, 9.6495, 101.8365},
            {"Berlin", 9, 21, 8, 18.254, 114.4812},
            {"Berlin", 9, 21, 9, 26.0069, 128.354},
            {"Berlin", 9, 21, 10, 32.307, 143.9646},
            {"Berlin", 9, 21, 11, 36.4779, 161.4546},
            {"Berlin", 9, 21, 12, 37.9165, 180.23},
            {"Berlin", 9, 21, 13, 36.3751, 198.9798},
            {"Berlin", 9, 21, 14, 32.1162, 216.4081},
            {"Berlin", 9, 21, 15, 25.7484, 231.9485},
            {"Berlin", 9, 21, 16, 17.9459, 245.761},
            {"Berlin", 9, 21, 17, 9.3063, 258.3611},
            {"Berlin", 12, 21, 10, 9.4232, 151.2497},
            {"Berlin", 12, 21, 11, 12.8119, 164.8253},
            {"Berlin", 12, 21, 12, 14.0976, 178.9005},
            {"Berlin", 12, 21, 13, 13.1547, 193.0148},
            {"Berlin", 12, 21, 14, 10.0757, 206.6918},
            {"Berlin", 12, 21, 15, 5.1577, 219.6215},
            {"Jeddah", 3, 21, 7, 7.1654, 92.3449},
            {"Jeddah", 3, 21, 8, 20.9896, 98.1731},
            {"Jeddah", 3, 21, 9, 34.6476, 105.1749},
            {"Jeddah", 3, 21, 10, 47.774, 114.8841},
            {"Jeddah", 3, 21, 11, 59.5728, 130.7295},
            {"Jeddah", 3, 21, 12, 67.7468, 159.6439},
            {"Jeddah", 3, 21, 13, 67.8076, 200.0178},
            {"Jeddah", 3, 21, 14, 59.7069, 229.1417},
            {"Jeddah", 3, 21, 15, 47.9368, 245.1069},
            {"Jeddah", 3, 21, 16, 34.8244, 254.8756},
            {"Jeddah", 3, 21, 17, 21.1764, 261.9144},
            {"Jeddah", 3, 21, 18, 7.36, 267.7725},
            {"Jeddah", 6, 21, 7, 16.032, 70.6143},
            {"Jeddah", 6, 21, 8, 29.316, 74.378},
            {"Jeddah", 6, 21, 9, 42.841, 77.4121},
            {"Jeddah", 6, 21, 10, 56.5138, 79.6508},
            {"Jeddah", 6, 21, 11, 70.2604, 80.303},
            {"Jeddah", 6, 21, 12, 83.8776, 70.2776},
            {"Jeddah", 6, 21, 13, 81.7188, 285.2352},
            {"Jeddah", 6, 21, 14, 68.0316, 279.5918},
            {"Jeddah", 6, 21, 15, 54.2911, 280.6413},
            {"Jeddah", 6, 21, 16, 40.637, 283.0298},
            {"Jeddah", 6, 21, 17, 27.1436, 286.1787},
            {"Jeddah", 6, 21, 18, 13.9115, 290.0711},
            {"Jeddah", 9, 21, 7, 10.4531, 93.5501},
            {"Jeddah", 9, 21, 8, 24.2619, 99.5894},
            {"Jeddah", 9, 21, 9, 37.8246, 107.0749},
            {"Jeddah", 9, 21, 10, 50.7284, 117.8411},
            {"Jeddah", 9, 21, 11, 61.9532, 136.07},
            {"Jeddah", 9, 21, 12, 68.5991, 168.8682},
            {"Jeddah", 9, 21, 13, 66.3896, 208.3386},
            {"Jeddah", 9, 21, 14, 57.0647, 233.619},
            {"Jeddah", 9, 21, 15, 44.8534, 247.6113},
            {"Jeddah", 9, 21, 16, 31.5669, 256.5149},
            {"Jeddah", 9, 21, 17, 17.8451, 263.1588},
            {"Jeddah", 12, 21, 8, 12.2177, 121.4661},
            {"Jeddah", 12, 21, 9, 23.5524, 129.6073},
            {"Jeddah", 12, 21, 10, 33.4271, 140.5075},
            {"Jeddah", 12, 21, 11, 40.9102, 155.0193},
            {"Jeddah", 12, 21, 12, 44.7839, 173.057},
            {"Jeddah", 12, 21, 13, 44.1198, 192.3386},
            {"Jeddah", 12, 21, 14, 39.0959, 209.4993},
            {"Jeddah", 12, 21, 15, 30.8173, 222.9252},
            {"Jeddah", 12, 21, 16, 20.4485, 232.9458},
            {"Jeddah", 12, 21, 17, 8.8284, 240.466},};

        System.out.printf("%-12s %-8s %6s %12s %12s %12s %12s%n",
                "Site", "Date", "n", "RMSE alt", "MaxAE alt", "RMSE azi", "MaxAE azi");
        System.out.println("-".repeat(80));

        String csv = "spa_verification.csv";
        double gSumA = 0, gSumZ = 0;
        int gN = 0;
        double gMaxA = 0, gMaxZ = 0;

        try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter(csv))) {

            out.println("site,month,day,hour,spa_alt,jfs_alt,err_alt,spa_azi,jfs_azi,err_azi");

            for (Object[] site : sites) {

                String name = (String) site[0];
                double lat = (Double) site[1];
                double lon = (Double) site[2];
                SolarCalculator calc = new SolarCalculator(lat, lon, 0);

                int[][] dates = {{3, 21}, {6, 21}, {9, 21}, {12, 21}};
                String[] dateNames = {"Mar 21", "Jun 21", "Sep 21", "Dec 21"};

                for (int di = 0; di < dates.length; di++) {

                    double sumA = 0, sumZ = 0, maxA = 0, maxZ = 0;
                    int n = 0;

                    for (Object[] r : REF) {

                        if (!r[0].equals(name)) {
                            continue;
                        }
                        if ((Integer) r[1] != dates[di][0]) {
                            continue;
                        }
                        if ((Integer) r[2] != dates[di][1]) {
                            continue;
                        }

                        int hour = (Integer) r[3];
                        double spaAlt = (Double) r[4];
                        double spaAzi = (Double) r[5];

                        SolarPosition pos = calc.calculateSolarPosition(
                                LocalDateTime.of(2024, dates[di][0], dates[di][1], hour, 0));

                        double jfsAlt = pos.getAltitudeAngle();
                        double jfsAzi = pos.getAzimuthAngle();

                        double eA = jfsAlt - spaAlt;
                        double eZ = jfsAzi - spaAzi;
                        if (eZ > 180.0) {
                            eZ -= 360.0;
                        }
                        if (eZ < -180.0) {
                            eZ += 360.0;
                        }

                        sumA += eA * eA;
                        sumZ += eZ * eZ;
                        n++;
                        maxA = Math.max(maxA, Math.abs(eA));
                        maxZ = Math.max(maxZ, Math.abs(eZ));

                        gSumA += eA * eA;
                        gSumZ += eZ * eZ;
                        gN++;
                        gMaxA = Math.max(gMaxA, Math.abs(eA));
                        gMaxZ = Math.max(gMaxZ, Math.abs(eZ));

                        out.printf(Locale.US, "%s,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f%n",
                                name, dates[di][0], dates[di][1], hour,
                                spaAlt, jfsAlt, eA, spaAzi, jfsAzi, eZ);
                    }

                    if (n == 0) {
                        continue;
                    }

                    System.out.printf("%-12s %-8s %6d %12.4f %12.4f %12.4f %12.4f%n",
                            name, dateNames[di], n,
                            Math.sqrt(sumA / n), maxA, Math.sqrt(sumZ / n), maxZ);
                }
            }

            System.out.println("-".repeat(80));
            System.out.printf("%-12s %-8s %6d %12.4f %12.4f %12.4f %12.4f%n",
                    "OVERALL", "", gN,
                    Math.sqrt(gSumA / gN), gMaxA, Math.sqrt(gSumZ / gN), gMaxZ);

            System.out.printf("%nSaved: %s%n", csv);

        } catch (java.io.IOException e) {
            System.err.println("Write error: " + e.getMessage());
        }

        System.out.println();
        System.out.println("[Sanity check] Diyarbakir, June 21, 12:00 local:");
        System.out.println("  NREL SPA apparent elevation = 74.8548 deg");
        System.out.println("  The published Figure 3 peaks near 68 deg, which is wrong by ~6.9 deg.");
        System.out.println("  Whatever this test reports is the number that belongs in the paper.");
    }

    // ============================================================================
// NEW TEST 12 - MIRROR TRACKING VERIFICATION AGAINST THE LAW OF REFLECTION
//
// Add to TestOptimization.java and wire in:
//     case 12 -> runTrackingVerification();
// GUI:  { "Test 12 · Tracking Solver",  "Reflection-law residual", 12 },
//
// WHY THIS REPLACES THE BARBON COMPARISON
// ---------------------------------------
// The manuscript's component-level validation cited Barbon et al. (Energies
// 2021, 14, 2883) for per-mirror tilt angles at Mieres, Spain. That paper
// contains no per-mirror tracking angles: it derives the two LONGITUDINAL
// tilt angles of the mobile structure (beta_M = lambda/2) and of the
// secondary reflector system (beta_a = lambda). Its case studies are Almeria,
// Rome, Budapest, Berlin and Helsinki - Mieres does not appear. The cited
// quantity and the cited geometry are both absent from the reference.
//
// Comparing two implementations of the same closed-form bisector law would in
// any case be circular. This test instead checks the tracking solver against
// an INDEPENDENT physical law:
//
//   1. take theta_i from MirrorTracker
//   2. build the mirror normal  n = (sin theta, 0, cos theta)
//   3. apply the full 3-D specular reflection law  r = 2(s.n)n - s
//   4. project r onto the transverse (x-z) plane
//   5. compare its direction with the direction to the receiver
//
// A correct single-axis tracker must make the transverse projection of the
// reflected ray point exactly at the receiver. The residual should be at
// machine precision. Nothing in steps 2-5 reuses the tracking derivation, so
// the test is a genuine verification rather than a restatement.
//
// WHAT IT CAUGHT
// --------------
// The previous implementation summed the 3-D unit vectors,
//     n = s_3D + t_3D,   theta = atan2(n_x, n_z)
// which weights the sun direction by |(s_x, s_z)| < 1 relative to the target
// and is therefore not the bisector of the PROJECTED directions. Derivation:
// with n = (sin theta, 0, cos theta) the reflected ray satisfies
//     (r_x, r_z) ~ (sin(2 theta - psi_s), cos(2 theta - psi_s))
// so aiming at the receiver requires 2 theta - psi_s = psi_t, i.e.
//     theta = (psi_s + psi_t) / 2
// with psi_s = atan2(s_x, s_z) the TRANSVERSE profile angle of the sun.
// The legacy column below quantifies the error the old form would have made.
// ============================================================================
    public static void runTrackingVerification() {

        System.out.println("=== TEST 12: Tracking Solver vs Law of Reflection ===\n");

        final double ALT_FLOOR = 5.0;

        String[] siteNames = {"Jeddah", "Diyarbakir", "Berlin"};
        double[] siteLat = {LAT_JEDDAH, LAT_DIYARBAKIR, LAT_BERLIN};
        double[] siteLon = {LON_JEDDAH, LON_DIYARBAKIR, LON_BERLIN};

        int[][] dates = {{3, 21}, {6, 21}, {9, 21}, {12, 21}};
        String[] dateNames = {"Mar 21", "Jun 21", "Sep 21", "Dec 21"};

        // G2 Standard geometry
        int N = 6;
        double w = FIX_W_CM;
        double p = FIX_P_CM;
        double Hr = FIX_HR_CM;

        System.out.printf("Geometry: N=%d, w=%.0f cm, p=%.0f cm, Hr=%.0f cm%n%n", N, w, p, Hr);
        System.out.printf("%-12s %-8s %7s %14s %14s %14s%n",
                "Site", "Date", "n", "max resid.", "RMS resid.", "legacy max");
        System.out.printf("%-12s %-8s %7s %14s %14s %14s%n",
                "", "", "", "(deg)", "(deg)", "(deg)");
        System.out.println("-".repeat(74));

        String csv = "tracking_verification.csv";
        double gMax = 0.0, gLegacyMax = 0.0, gSum = 0.0;
        int gN = 0;

        try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter(csv))) {

            out.println("site,month,day,hour,minute,mirror,x_m,theta_deg,"
                    + "residual_deg,legacy_theta_deg,legacy_residual_deg");

            for (int s = 0; s < siteNames.length; s++) {

                SolarCalculator calc = new SolarCalculator(siteLat[s], siteLon[s], 0);
                MirrorTracker trk = new MirrorTracker();

                SimulationState st = new SimulationState();
                st.setLatitude(siteLat[s]);
                st.setLongitude(siteLon[s]);
                st.setNumReflectors(N);
                st.setReflectorWidth((float) w);
                st.setReflectorSpacing((float) p);
                st.setReceiverHeight((float) Hr);
                st.setSupportHeight(30.0f);
                st.setReflectorLength((float) FIX_L_CM);
                st.setReceiverDiameter((float) FIX_DR_CM);

                double dh = (Hr - 30.0) / 100.0;   // receiver height above mirror plane [m]

                for (int di = 0; di < dates.length; di++) {

                    double maxR = 0.0, sumR2 = 0.0, maxLegacy = 0.0;
                    int n = 0;

                    for (int h = 4; h <= 20; h++) {
                        for (int mm = 0; mm < 60; mm += 15) {

                            LocalDateTime t
                                    = LocalDateTime.of(2024, dates[di][0], dates[di][1], h, mm);
                            SolarPosition pos = calc.calculateSolarPosition(t);
                            if (pos.getAltitudeAngle() <= ALT_FLOOR) {
                                continue;
                            }

                            double alt = Math.toRadians(pos.getAltitudeAngle());
                            double azi = Math.toRadians(pos.getAzimuthAngle());

                            // 3-D sun unit vector: x transverse, y north, z up
                            double sx = -Math.cos(alt) * Math.sin(azi);
                            double sy = Math.cos(alt) * Math.cos(azi);
                            double sz = Math.sin(alt);

                            for (int i = 0; i < N; i++) {

                                double off = (i < N / 2) ? -(i + 0.5) : (i - N / 2 + 0.5);
                                double xm = off * p / 100.0;      // [m]

                                // ---- tracking angle under test ----
                                double thetaDeg = trk.calculateOptimalMirrorAngle(xm, pos, st);
                                double th = Math.toRadians(thetaDeg);

                                // ---- independent check: law of reflection ----
                                double nx = Math.sin(th), nz = Math.cos(th);
                                double sdotn = sx * nx + sz * nz;          // n_y = 0
                                double rx = 2.0 * sdotn * nx - sx;
                                double rz = 2.0 * sdotn * nz - sz;

                                double aimed = Math.atan2(rx, rz);        // reflected, transverse
                                double target = Math.atan2(-xm, dh);       // toward receiver
                                double resid = Math.toDegrees(Math.abs(aimed - target));
                                if (resid > 180.0) {
                                    resid = Math.abs(resid - 360.0);
                                }

                                // ---- legacy 3-D vector-sum formulation ----
                                double tmag = Math.sqrt(xm * xm + dh * dh);
                                double tx = -xm / tmag, tz = dh / tmag;
                                double legacyTheta = Math.atan2(sx + tx, sz + tz);
                                double lnx = Math.sin(legacyTheta), lnz = Math.cos(legacyTheta);
                                double lsdotn = sx * lnx + sz * lnz;
                                double lrx = 2.0 * lsdotn * lnx - sx;
                                double lrz = 2.0 * lsdotn * lnz - sz;
                                double lResid = Math.toDegrees(
                                        Math.abs(Math.atan2(lrx, lrz) - target));
                                if (lResid > 180.0) {
                                    lResid = Math.abs(lResid - 360.0);
                                }

                                maxR = Math.max(maxR, resid);
                                maxLegacy = Math.max(maxLegacy, lResid);
                                sumR2 += resid * resid;
                                n++;

                                gMax = Math.max(gMax, resid);
                                gLegacyMax = Math.max(gLegacyMax, lResid);
                                gSum += resid * resid;
                                gN++;

                                out.printf(Locale.US,
                                        "%s,%d,%d,%d,%d,%d,%.4f,%.6f,%.3e,%.6f,%.6f%n",
                                        siteNames[s], dates[di][0], dates[di][1], h, mm, i,
                                        xm, thetaDeg, resid,
                                        Math.toDegrees(legacyTheta), lResid);
                            }
                        }
                    }

                    if (n == 0) {
                        continue;
                    }
                    System.out.printf("%-12s %-8s %7d %14.3e %14.3e %14.4f%n",
                            siteNames[s], dateNames[di], n,
                            maxR, Math.sqrt(sumR2 / n), maxLegacy);
                }
            }

            System.out.println("-".repeat(74));
            System.out.printf("%-12s %-8s %7d %14.3e %14.3e %14.4f%n",
                    "OVERALL", "", gN, gMax, Math.sqrt(gSum / gN), gLegacyMax);

            System.out.printf("%nSaved: %s%n", csv);

        } catch (java.io.IOException e) {
            System.err.println("Write error: " + e.getMessage());
        }

        System.out.println();
        System.out.println("[Interpretation]");
        System.out.println("  Residual ~1e-13 deg  -> the tracking solver is exact to machine");
        System.out.println("  precision: the reflected ray's transverse projection lands on the");
        System.out.println("  receiver at every sampled instant, for every mirror, at all three");
        System.out.println("  latitudes and in all four seasons.");
        System.out.println("  The 'legacy max' column is what the previous 3-D vector-sum");
        System.out.println("  formulation would have produced. Report both in the manuscript:");
        System.out.println("  the correction is a genuine methodological result, not an erratum.");
    }

    // ============================================================================
// NEW TEST 13 - WELL-POSEDNESS OF THE OPTICAL OBJECTIVE
//
// Add to TestOptimization.java and wire in:
//     case 13 -> runWellPosednessSweep();
// GUI:  { "Test 13 · Well-posedness",  "Fig. 5  ·  J(N), two modes",  13 },
//
// PURPOSE
// -------
// Section 4.4 argues that a purely optical objective has no interior optimum
// in mirror count, and that imposing a minimum geometric concentration ratio
// renders the problem well posed. At present that argument has to be
// assembled by the reader from three separate tables. This sweep produces it
// directly.
//
// Two modes, both maximising J = E_opt / A_ground and both with the receiver
// height optimised independently at every N so that the N dependence is
// isolated:
//
//   MODE A (unconstrained)   w = 10 cm fixed, p = 15 cm fixed.
//       A_field  = N w L                grows with N
//       A_ground = [(N-1)p + w] L       grows faster
//       The packing ratio N w / [(N-1)p + w] falls monotonically toward w/p,
//       and optical efficiency falls as well. J therefore decreases
//       monotonically and the optimum sits at the lower bound of N.
//
//   MODE B (Cg >= 20)        w = Cg_min * Dr / N,  p = 1.5 w.
//       N w = Cg_min * Dr = 2.0 m is fixed, so the aperture is constant and
//       the ground area varies by less than a factor 1.5 over the whole
//       range. Only the optics change with N: small N means very wide mirrors
//       (w/Dr = 4 at N = 5, spillage-dominated), large N means a very wide
//       field (cosine- and end-loss-dominated). An interior optimum is
//       therefore expected.
//
// The feasible range is set by the mirror-width bounds: with Cg_min = 20 and
// Dr = 10 cm, w = 200/N cm lies in [5, 40] cm for 5 <= N <= 40.
//
// OUTPUT
//   console : optimum N for each mode and site, plus the J profile
//   file    : wellposedness_sweep.csv  (source data for Fig. 5)
// ============================================================================
    public static void runWellPosednessSweep() {

        System.out.println("=== TEST 13: Well-posedness of the optical objective ===\n");

        final double DT_HOURS = 0.25;
        final double ALT_FLOOR = 5.0;
        final double CG_MIN = 20.0;
        final double DR_CM = FIX_DR_CM;      // 10 cm
        final double L_CM = FIX_L_CM;       // 1000 cm

        final int N_LO = 5, N_HI = 40;           // w = 200/N in [5, 40] cm

        // receiver-height search: uniform in Hr/Wf
        final double R_LO = 0.10, R_HI = 2.00;
        final int R_N = 40;

        String[] siteNames = {"Jeddah", "Diyarbakir", "Berlin"};
        double[] siteLat = {LAT_JEDDAH, LAT_DIYARBAKIR, LAT_BERLIN};
        double[] siteLon = {LON_JEDDAH, LON_DIYARBAKIR, LON_BERLIN};

        int[][] dates = {{3, 21}, {6, 21}, {9, 21}, {12, 21}};

        System.out.printf("Mode A: w=10 cm, p=15 cm fixed%n");
        System.out.printf("Mode B: w=%.0f/N cm so that Cg=%.0f, p=1.5w%n",
                CG_MIN * DR_CM, CG_MIN);
        System.out.printf("Hr optimised at every N over Hr/Wf in [%.2f, %.2f]%n%n",
                R_LO, R_HI);

        String csv = "wellposedness_sweep.csv";
        try (java.io.PrintWriter out = new java.io.PrintWriter(new java.io.FileWriter(csv))) {

            out.println("site,mode,N,w_cm,p_cm,Wf_m,Hr_opt_cm,A_field_m2,"
                    + "A_ground_m2,energy_kWh,J_Wh_per_m2");

            for (int s = 0; s < siteNames.length; s++) {

                SolarCalculator calc = new SolarCalculator(siteLat[s], siteLon[s], 0);
                List<LocalDateTime> times = new ArrayList<>();
                for (int[] dt : dates) {
                    for (int h = 3; h <= 21; h++) {
                        for (int mm = 0; mm < 60; mm += 15) {
                            LocalDateTime t = LocalDateTime.of(2024, dt[0], dt[1], h, mm);
                            if (calc.calculateSolarPosition(t).getAltitudeAngle() > ALT_FLOOR) {
                                times.add(t);
                            }
                        }
                    }
                }
                if (times.isEmpty()) {
                    continue;
                }

                FresnelDesignProblem prob
                        = new FresnelDesignProblem(siteLat[s], siteLon[s], times);

                for (int mode = 0; mode < 2; mode++) {

                    String modeName = (mode == 0) ? "A_unconstrained" : "B_Cg20";
                    double bestJ = -1.0;
                    int bestN = 0;

                    System.out.printf("--- %s | mode %s ---%n", siteNames[s], modeName);
                    System.out.printf("%6s %8s %8s %9s %11s %14s%n",
                            "N", "w(cm)", "p(cm)", "Wf(m)", "Hr_opt(cm)", "J(Wh/m2)");

                    for (int N = N_LO; N <= N_HI; N++) {

                        double w_cm, p_cm;
                        if (mode == 0) {
                            w_cm = 10.0;
                            p_cm = 15.0;
                        } else {
                            w_cm = CG_MIN * DR_CM / N;      // Cg exactly at the bound
                            p_cm = 1.5 * w_cm;
                        }

                        double Wf_m = ((N - 1) * p_cm + w_cm) / 100.0;
                        double ground = Wf_m * (L_CM / 100.0);
                        double aperture = N * (w_cm / 100.0) * (L_CM / 100.0);

                        // optimise receiver height at this N
                        double bestE = -1.0;
                        double bestHr = 0.0;
                        for (int k = 0; k <= R_N; k++) {
                            double ratio = R_LO + (R_HI - R_LO) * k / (double) R_N;
                            double hrCm = ratio * Wf_m * 100.0;
                            if (hrCm < 40.0) {
                                continue;      // structural clearance
                            }
                            DesignParameters params
                                    = new DesignParameters(hrCm, w_cm, p_cm, N);

                            double energyWh = 0.0;
                            for (LocalDateTime t : times) {
                                energyWh += prob.evaluateOpticalMetrics(params, t)
                                        .get("Q_opt") * DT_HOURS;
                            }
                            if (energyWh > bestE) {
                                bestE = energyWh;
                                bestHr = hrCm;
                            }
                        }
                        if (bestE < 0) {
                            continue;
                        }

                        double J = bestE / ground;
                        if (J > bestJ) {
                            bestJ = J;
                            bestN = N;
                        }

                        // print every fifth point to keep the console readable
                        if (N % 5 == 0 || N == N_LO) {
                            System.out.printf("%6d %8.2f %8.2f %9.3f %11.0f %14.1f%n",
                                    N, w_cm, p_cm, Wf_m, bestHr, J);
                        }

                        out.printf(Locale.US,
                                "%s,%s,%d,%.3f,%.3f,%.4f,%.1f,%.4f,%.4f,%.4f,%.4f%n",
                                siteNames[s], modeName, N, w_cm, p_cm, Wf_m, bestHr,
                                aperture, ground, bestE / 1000.0, J);
                    }

                    String flag = (bestN == N_LO || bestN == N_HI)
                            ? "  <-- AT A BOUND, no interior optimum"
                            : "  <-- interior optimum";
                    System.out.printf("  optimum: N = %d, J = %.1f Wh/m2%s%n%n",
                            bestN, bestJ, flag);
                }
            }

            System.out.printf("Saved: %s%n", csv);

        } catch (java.io.IOException e) {
            System.err.println("Write error: " + e.getMessage());
        }

        System.out.println();
        System.out.println("[Expected reading]");
        System.out.println("  Mode A drives N to its lower bound: the optical objective alone");
        System.out.println("  cannot size the field. Mode B, with the concentration ratio held");
        System.out.println("  at its thermal minimum, produces a genuine interior optimum.");
        System.out.println("  If mode B also lands on a bound, the argument of Section 4.4");
        System.out.println("  needs revising and should be reported as such.");
    }
}
