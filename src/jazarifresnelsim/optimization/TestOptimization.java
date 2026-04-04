package jazarifresnelsim.optimization;

import jazarifresnelsim.optimization.algorithms.*;
import jazarifresnelsim.optimization.problem.*;
import jazarifresnelsim.optimization.evaluation.*;
import jazarifresnelsim.domain.SolarCalculator;
import jazarifresnelsim.models.SolarPosition;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

/**
 * Comprehensive validation and optimization test suite for JazariFresnelSim.
 *
 * Reproduces all computational results reported in:
 * "Rapid Optical-Thermal Design of Linear Fresnel Reflectors:
 *  An Open-Source Analytical Framework and Dimensionless Sizing Rules"
 * Solar Energy (Elsevier), 2026.
 *
 * NOTE — Validation tables not reproduced here:
 *   Table 3  (Solar position vs NREL SPA)      — requires NREL SPA reference data
 *   Table 4  (Center mirror analytic check)     — verified analytically in manuscript
 *   Table 5  (Mirror angles vs Barbón et al.)   — requires Barbón experimental data
 *   Tables 6-8 (Optical efficiency vs SolTrace) — requires SolTrace MCRT software
 *   Table 10 (V&V summary)                      — summary table, no computation needed
 *   Table 11 (Baseline configurations)          — definition table, no computation needed
 *   Table 12 (Design Rule 1 validation)         — derived from Test [1] spacing sweep
 *
 * Menu (ordered by manuscript appearance):
 *   [1]  Extreme-Angle Error Analysis      — Table 9
 *   [2]  Mirror Count Scaling              — Table 13
 *   [3]  Metaheuristic Optimization        — Tables 14-15
 *   [4]  Temporal Discretization           — Table 16
 *   [5]  Spacing Sweep Export              — Fig. 6  (CSV)
 *   [6]  Height Sweep Export               — Fig. 7  (CSV)
 *   [7]  Daily Efficiency Profile Export   — Fig. 8  (CSV)
 *   [8]  Convergence Data Export           — Fig. 9  (CSV)
 *   [9]  Run ALL Tests
 *   [0]  Exit
 *
 * @author Yunus Demirtas, Musa Atas — Siirt University
 * @version 2.2
 */
public class TestOptimization {

    // ----------------------------------------------------------------
    // Location constants
    // ----------------------------------------------------------------
    private static final double LAT_DIYARBAKIR = 37.962984;
    private static final double LON_DIYARBAKIR = 41.850347;
    private static final double LAT_BERLIN     = 52.52;
    private static final double LON_BERLIN     = 13.405;
    private static final double LAT_JEDDAH     = 21.49;
    private static final double LON_JEDDAH     = 39.19;

    private static final int NUM_OPTIMIZATION_RUNS = 30;

    // ================================================================
    // MAIN
    // ================================================================
    public static void main(String[] args) {
        printBanner();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            printMenu();
            System.out.print("\nSelect option (0-9): ");
            String input = scanner.nextLine().trim();
            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.\n");
                continue;
            }
            System.out.println();
            long startTime = System.currentTimeMillis();
            switch (choice) {
                case 1: runExtremeAngleAnalysis();    break;
                case 2: runMirrorCountScaling();      break;
                case 3: runOptimizationComparison();  break;
                case 4: runTemporalSensitivity();     break;
                case 5: runSpacingSweepExport();      break;
                case 6: runHeightSweepExport();       break;
                case 7: runDailyEfficiencyProfile();  break;
                case 8: runConvergenceExport();       break;
                case 9: runAllTests();                break;
                case 0:
                    System.out.println("Exiting.");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid option.\n");
                    continue;
            }
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.printf("%n--- Completed in %.2f seconds ---%n%n", elapsed / 1000.0);
        }
    }

    // ================================================================
    // BANNER & MENU
    // ================================================================
    private static void printBanner() {
        System.out.println("================================================================");
        System.out.println("  JazariFresnelSim — Validation & Optimization Test Suite v2.2");
        System.out.println("  Paper: Rapid Optical-Thermal Design of LFR Systems");
        System.out.println("  Journal: Solar Energy (Elsevier), 2026");
        System.out.println("================================================================\n");
    }

    private static void printMenu() {
        System.out.println("========== MAIN MENU ==========");
        System.out.println("  --- Tabular results (manuscript order) ---");
        System.out.println("  [1]  Extreme-Angle Error Analysis      — Table 9");
        System.out.println("  [2]  Mirror Count Scaling              — Table 13");
        System.out.println("  [3]  Metaheuristic Optimization        — Tables 14-15");
        System.out.println("  [4]  Temporal Discretization           — Table 16");
        System.out.println("  --- Figure data export (CSV + Python) ---");
        System.out.println("  [5]  Spacing Sweep Export              — Fig. 6");
        System.out.println("  [6]  Height Sweep Export               — Fig. 7");
        System.out.println("  [7]  Daily Efficiency Profile Export   — Fig. 8");
        System.out.println("  [8]  Convergence Data Export           — Fig. 9");
        System.out.println("  ---");
        System.out.println("  [9]  Run ALL Tests");
        System.out.println("  [0]  Exit");
        System.out.println("================================");
    }

    // ================================================================
    // TEST 1: EXTREME-ANGLE ANNUAL ERROR ANALYSIS — Table 9
    // ================================================================
    /**
     * Quantifies the annual energy yield deviation caused by the analytical
     * model's sharp geometric cutoff beyond theta_T = 55 degrees.
     * Evaluated at Diyarbakir, Berlin, and Jeddah.
     * Reproduces Table 9 of the manuscript.
     */
    public static void runExtremeAngleAnalysis() {
        System.out.println("=== TEST 1: Extreme-Angle Annual Error Analysis (Table 9) ===\n");
        double[][] locs = {
            {LAT_DIYARBAKIR, LON_DIYARBAKIR},
            {LAT_BERLIN,     LON_BERLIN},
            {LAT_JEDDAH,     LON_JEDDAH}
        };
        String[] locNames = {
            "Diyarbakir (37.96°N)",
            "Berlin     (52.52°N)",
            "Jeddah     (21.49°N)"
        };
        System.out.printf("%-22s %10s %12s %14s %16s%n",
                "Location", "Daylight h", "h θT>55°", "Fraction (%)", "Yield Dev (%)");
        System.out.println("-".repeat(78));
        for (int i = 0; i < 3; i++) {
            SolarCalculator calc = new SolarCalculator(locs[i][0], locs[i][1], 0);
            int    total = 0, extreme = 0;
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
                                double aR  = Math.toRadians(pos.getAltitudeAngle());
                                double azR = Math.toRadians(pos.getAzimuthAngle());
                                double tT  = 90.0 - Math.abs(Math.toDegrees(
                                        Math.atan(Math.tan(aR) / Math.cos(azR))));
                                totalE += dni * Math.max(0, Math.cos(Math.toRadians(tT)));
                                if (tT > 55) {
                                    extreme++;
                                    extremeE += dni * 0.3;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
            System.out.printf("%-22s %10d %12d %14.1f %16.1f%n",
                    locNames[i], total, extreme,
                    100.0 * extreme / total,
                    100.0 * extremeE / totalE);
        }
    }

    // ================================================================
    // TEST 2: MIRROR COUNT SCALING — Table 13
    // ================================================================
    /**
     * Evaluates energy output and marginal gain per additional mirror pair
     * for N = 2, 4, 6, 8, 10 at solar noon on June 21, Diyarbakir.
     * Standard config: p=30cm, Hr=130cm, w=20cm, Dr=16cm.
     * Reproduces Table 13 of the manuscript.
     */
    public static void runMirrorCountScaling() {
        System.out.println("=== TEST 2: Mirror Count Scaling (Table 13) ===");
        System.out.printf("Config: p=30cm, Hr=130cm, w=20cm | June 21 solar noon | Diyarbakir%n%n");
        System.out.printf("%-6s %15s %15s %20s%n",
                "N", "Energy (W)", "Field W (cm)", "Marginal (W/mirror)");
        System.out.println("-".repeat(60));
        double prevE = 0;
        int    prevN = 0;
        for (int n : new int[]{2, 4, 6, 8, 10}) {
            var times = new ArrayList<LocalDateTime>();
            times.add(LocalDateTime.of(2024, Month.JUNE, 21, 12, 0));
            double e = new FresnelDesignProblem(LAT_DIYARBAKIR, LON_DIYARBAKIR, times)
                    .evaluateDesignForAllTimes(new DesignParameters(130, 16, 20, 30, n))
                    .values().stream().mapToDouble(Double::doubleValue).sum();
            String margStr = (prevN > 0)
                    ? String.format("%.1f", (e - prevE) / (n - prevN)) : "—";
            System.out.printf("%-6d %15.4f %15.0f %20s%n",
                    n, e, (n - 1) * 30.0, margStr);
            prevE = e;
            prevN = n;
        }
    }

    // ================================================================
    // TEST 3: METAHEURISTIC OPTIMIZATION — Tables 14-15
    // ================================================================
    /**
     * Runs GA, PSO, and SA over 30 independent runs each and reports
     * execution time, best yield, and standard deviation.
     * 5-parameter optimization over H=144 representative annual hours.
     * Reproduces Tables 14-15 of the manuscript.
     */
    public static void runOptimizationComparison() {
        System.out.println("=== TEST 3: Metaheuristic Optimization (Tables 14-15) ===");
        System.out.printf("Location: Diyarbakir | H=144 | %d runs per algorithm%n%n",
                NUM_OPTIMIZATION_RUNS);
        try {
            List<LocalDateTime> times = getEvaluationTimes(144);
            FresnelDesignProblem problem = new FresnelDesignProblem(
                    LAT_DIYARBAKIR, LON_DIYARBAKIR, times);
            DesignParameters init = new DesignParameters(130.0, 16.0, 20.0, 30.0, 4);
            List<IOptimizationAlgorithm> algos = setupAlgorithms();
            DesignEvaluator evaluator = new DesignEvaluator(problem, times);
            OptimizationComparison comp = new OptimizationComparison(
                    evaluator, algos.size(), NUM_OPTIMIZATION_RUNS);
            OptimizationComparison.ComparisonResult results =
                    comp.compareAlgorithms(algos, problem, init, new HashMap<>());
            printOptimizationResults(results);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printOptimizationResults(
            OptimizationComparison.ComparisonResult results) {
        Map<String, OptimizationComparison.AlgorithmStats> stats = results.getStatistics();
        System.out.println("\n--- Table 15: Algorithm Performance ---");
        System.out.printf("%-10s %12s %18s %12s %20s%n",
                "Algorithm", "Time (s)", "Best (kW/m²)", "Std Dev", "Convergence");
        System.out.println("-".repeat(76));
        String bestAlgo  = "";
        double bestYield = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, OptimizationComparison.AlgorithmStats> e : stats.entrySet()) {
            String name = e.getKey();
            var    stat = e.getValue();
            String type = stat.objectiveStats.stdDev < 0.01 ? "Global"
                        : stat.objectiveStats.stdDev < 15.0 ? "Near-global"
                        : "Local/Inconsistent";
            System.out.printf("%-10s %12.2f %18.2f %12.2f %20s%n",
                    name, stat.timeStats.mean / 1000.0,
                    stat.objectiveStats.max, stat.objectiveStats.stdDev, type);
            if (stat.objectiveStats.max > bestYield) {
                bestYield = stat.objectiveStats.max;
                bestAlgo  = name;
            }
        }
        var runs = results.getAlgorithmRuns().get(bestAlgo);
        if (runs != null) {
            var best = runs.stream()
                    .max(Comparator.comparing(r -> r.getBestSolution().getObjectiveValue()))
                    .orElse(null);
            if (best != null) {
                DesignParameters p = best.getBestSolution().getParameters();
                System.out.printf("%n--- Best Solution (%s) ---%n", bestAlgo);
                System.out.printf(
                        "  Hr=%.1f cm, Dr=%.1f cm, w=%.1f cm, p=%.1f cm, N=%d%n"
                        + "  Annual yield = %.2f kW/m²%n",
                        p.getReceiverHeight(), p.getReceiverDiameter(),
                        p.getMirrorWidth(), p.getMirrorSpacing(),
                        p.getNumberOfMirrors(), bestYield);
            }
        }
    }

    // ================================================================
    // TEST 4: TEMPORAL DISCRETIZATION SENSITIVITY — Table 16
    // ================================================================
    /**
     * Evaluates sensitivity of the PSO-optimized solution to temporal
     * discretization: H = 144, 288, and 4380 evaluation hours.
     * Reproduces Table 16 of the manuscript.
     */
    public static void runTemporalSensitivity() {
        System.out.println("=== TEST 4: Temporal Discretization Sensitivity (Table 16) ===\n");
        int[]    hVals  = {144, 288, 4380};
        String[] labels = {"144  (12 mo × 12 h)", "288  (12 mo × 24 h)", "4380 (full year)"};
        System.out.printf("%-22s %6s %8s %8s %12s %10s%n",
                "Resolution", "N", "p (cm)", "Hr (cm)", "Yield", "Time (s)");
        System.out.println("-".repeat(72));
        for (int idx = 0; idx < hVals.length; idx++) {
            List<LocalDateTime> times = getEvaluationTimes(hVals[idx]);
            FresnelDesignProblem problem = new FresnelDesignProblem(
                    LAT_DIYARBAKIR, LON_DIYARBAKIR, times);
            ParticleSwarm pso = buildPSO();
            long t0  = System.currentTimeMillis();
            var  sol = pso.optimize(problem,
                    new DesignParameters(130, 16, 20, 30, 4), new HashMap<>());
            double sec = (System.currentTimeMillis() - t0) / 1000.0;
            var p = sol.getParameters();
            System.out.printf("%-22s %6d %8.1f %8.1f %12.2f %10.2f%n",
                    labels[idx], p.getNumberOfMirrors(), p.getMirrorSpacing(),
                    p.getReceiverHeight(), sol.getObjectiveValue(), sec);
        }
    }

    // ================================================================
    // TEST 5: SPACING SWEEP EXPORT — Fig. 6 (CSV)
    // ================================================================
    /**
     * Parametric sweep over mirror spacing p = 20-70 cm at solar noon,
     * June 21, Diyarbakir. Standard config: N=6, Hr=130cm, w=20cm.
     * Exports spacing_sweep.csv for Python plotting.
     * Next step: python scripts/plot_figure6.py
     */
    public static void runSpacingSweepExport() {
        System.out.println("=== TEST 5: Spacing Sweep Export (Fig. 6) ===\n");
        System.out.println("Config: N=6, Hr=130cm, w=20cm | June 21 daily average");
        System.out.println("Design Rule 1: p/w > 3.0 → shading < 2%\n");

        // Mirror spacings matching Figure 6 x-axis (15-60 cm)
        int[] spacings = {15, 20, 25, 30, 35, 40, 45, 50, 55, 60};

        // Daily evaluation: hourly 7-18 on June 21
        List<LocalDateTime> dailyTimes = new ArrayList<>();
        for (int h = 7; h <= 18; h++)
            dailyTimes.add(LocalDateTime.of(2024, Month.JUNE, 21, h, 0));

        // Three locations (reserved for future multi-site validation)
        // double[][] locs     = {{LAT_DIYARBAKIR, LON_DIYARBAKIR},
        //                        {LAT_BERLIN,     LON_BERLIN},
        //                        {LAT_JEDDAH,     LON_JEDDAH}};
        // String[]   locNames = {"Diyarbakir", "Berlin", "Jeddah"};

        // ── Figure 6 data: Diyarbakir only → spacing_sweep.csv ──────────────
        String csvFile = "spacing_sweep.csv";
        try (java.io.PrintWriter pw =
                new java.io.PrintWriter(new java.io.FileWriter(csvFile))) {

            pw.println("spacing_cm,cosine_efficiency_pct,shading_loss_pct,pw_ratio");
            System.out.println("--- Diyarbakir (Fig. 6 data) ---");
            System.out.printf("%-14s %20s %18s %8s%n",
                    "Spacing p (cm)", "Cosine eff. (%)", "Shading loss (%)", "p/w");
            System.out.println("-".repeat(64));

            for (int sp : spacings) {
                FresnelDesignProblem problem =
                        new FresnelDesignProblem(LAT_DIYARBAKIR, LON_DIYARBAKIR, dailyTimes);
                DesignParameters params = new DesignParameters(130, 16, 20, sp, 6);
                double sumCosine = 0, sumShading = 0;
                int validHours = 0;
                for (LocalDateTime t : dailyTimes) {
                    Map<String, Double> metrics = problem.evaluateOpticalMetrics(params, t);
                    double cos  = metrics.get("cosine");
                    double shad = metrics.get("shading");
                    if (cos > 0) { sumCosine += cos; sumShading += shad; validHours++; }
                }
                double avgCosine  = validHours > 0 ? sumCosine  / validHours : 0;
                double avgShading = validHours > 0 ? sumShading / validHours : 0;
                double ratio = sp / 20.0;
                String flag  = (ratio >= 3.0) ? " ← Rule 1" : "";
                System.out.printf("%-14d %20.2f %18.2f %8.2f%s%n",
                        sp, avgCosine, avgShading, ratio, flag);
                pw.printf(Locale.US, "%d,%.4f,%.4f,%.4f%n",
                        sp, avgCosine, avgShading, ratio);
            }
            System.out.printf("%nSaved: %s%n", csvFile);
            System.out.println("Next step: python scripts/plot_figure6.py\n");
        } catch (java.io.IOException e) {
            System.err.println("Error writing " + csvFile + ": " + e.getMessage());
        }

        // ── Table 12: shading at different p/w ratios — Diyarbakir ─────────
        // Design Rule 1 validation: p/w > 3.0 → shading < 2%
        // Standard config: N=6, w=20cm, Hr=130cm, June 21 daily average
        System.out.println("--- Table 12: Shading loss (%) vs p/w ratio (Diyarbakir) ---");
        System.out.printf("%-8s %18s%n", "p/w", "Shading loss (%)");
        System.out.println("-".repeat(28));

        double[] pwRatios = {1.0, 1.5, 2.0, 2.5, 3.0};
        int      wFixed   = 20;

        String table12File = "table12_shading.csv";
        try (java.io.PrintWriter pw =
                new java.io.PrintWriter(new java.io.FileWriter(table12File))) {
            pw.println("pw_ratio,shading_loss_pct");

            for (double ratio : pwRatios) {
                int p = (int) Math.round(ratio * wFixed);
                FresnelDesignProblem problem =
                        new FresnelDesignProblem(LAT_DIYARBAKIR, LON_DIYARBAKIR, dailyTimes);
                DesignParameters params = new DesignParameters(130, 16, wFixed, p, 6);
                double sumShading = 0;
                int validHours = 0;
                for (LocalDateTime t : dailyTimes) {
                    Map<String, Double> metrics = problem.evaluateOpticalMetrics(params, t);
                    double cos  = metrics.get("cosine");
                    double shad = metrics.get("shading");
                    if (cos > 0) { sumShading += shad; validHours++; }
                }
                double avgShading = validHours > 0 ? sumShading / validHours : 0;
                String flag = (ratio >= 3.0) ? " ← Rule 1" : "";
                System.out.printf("%-8.1f %17.1f%%%s%n", ratio, avgShading, flag);
                pw.printf(Locale.US, "%.1f,%.2f%n", ratio, avgShading);
            }
            System.out.printf("%nSaved: %s%n", table12File);
        } catch (java.io.IOException e) {
            System.err.println("Error writing " + table12File + ": " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 6: HEIGHT SWEEP EXPORT — Fig. 7 (CSV)
    // ================================================================
    /**
     * Parametric sweep over receiver height Hr = 80-250 cm at solar noon,
     * June 21, Diyarbakir.
     * Standard N=6 (p=30cm, Wf=150cm) and High-Conc N=10 (p=40cm, Wf=360cm).
     * NOTE: Figure legend must read "10-mirror" — consistent with Table 11.
     * Exports height_sweep.csv for Python plotting.
     * Next step: python scripts/plot_figure7.py
     */
    public static void runHeightSweepExport() {
        System.out.println("=== TEST 6: Height Sweep Export (Fig. 7) ===\n");
        System.out.println("Standard  N=6  (p=30cm, Wf=150cm)");
        System.out.println("High-Conc N=10 (p=40cm, Wf=360cm)");
        System.out.println("NOTE: Figure legend = '10-mirror' per Table 11\n");

        int[]  heights = {80, 90, 100, 110, 120, 130, 140, 150,
                          160, 170, 180, 200, 220, 250};
        double Wf6     = 5.0 * 30.0;    // (N-1)*p = 150 cm
        double Wf10    = 9.0 * 40.0;    // (N-1)*p = 360 cm
        String csvFile = "height_sweep.csv";

        try (java.io.PrintWriter pw =
                new java.io.PrintWriter(new java.io.FileWriter(csvFile))) {
            pw.println("hr_cm,hr_wf_6mirror,energy_6mirror_W,hr_wf_10mirror,energy_10mirror_W");
            System.out.printf("%-10s %12s %16s %13s %16s%n",
                    "Hr (cm)", "Hr/Wf N=6", "6-mirror (W)", "Hr/Wf N=10", "10-mirror (W)");
            System.out.println("-".repeat(72));
            for (int hr : heights) {
                var times = new ArrayList<LocalDateTime>();
                times.add(LocalDateTime.of(2024, Month.JUNE, 21, 12, 0));
                double e6 = new FresnelDesignProblem(LAT_DIYARBAKIR, LON_DIYARBAKIR, times)
                        .evaluateDesignForAllTimes(
                                new DesignParameters(hr, 16, 20, 30, 6))
                        .values().stream().mapToDouble(Double::doubleValue).sum();
                double e10 = new FresnelDesignProblem(LAT_DIYARBAKIR, LON_DIYARBAKIR, times)
                        .evaluateDesignForAllTimes(
                                new DesignParameters(hr, 16, 20, 40, 10))
                        .values().stream().mapToDouble(Double::doubleValue).sum();
                double r6  = hr / Wf6;
                double r10 = hr / Wf10;
                System.out.printf("%-10d %12.3f %16.4f %13.3f %16.4f%n",
                        hr, r6, e6, r10, e10);
                pw.printf(Locale.US, "%d,%.4f,%.6f,%.4f,%.6f%n",
                        hr, r6, e6, r10, e10);
            }
            System.out.printf("%nSaved: %s%n", csvFile);
            System.out.println("Next step: python scripts/plot_figure7.py");
        } catch (java.io.IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 7: DAILY EFFICIENCY PROFILE EXPORT — Fig. 8 (CSV)
    // ================================================================
    /**
     * Computes hourly optical efficiency for three baseline configurations
     * (Table 11) on June 21 at Diyarbakir.
     * Exports daily_efficiency_profile.csv for Python plotting.
     * Next step: python scripts/plot_figure8.py
     *
     * Configurations (Table 11):
     *   Standard  : N=6,  p=30cm, Hr=130cm, w=20cm
     *   High-Conc : N=10, p=40cm, Hr=180cm, w=20cm
     *   Compact   : N=4,  p=25cm, Hr=100cm, w=25cm
     */
    public static void runDailyEfficiencyProfile() {
        System.out.println("=== TEST 7: Daily Efficiency Profile Export (Fig. 8) ===\n");
        System.out.println("Date: June 21 | Diyarbakir (37.96°N, 41.85°E)");
        System.out.println("Configurations: Standard N=6 | High-Conc N=10 | Compact N=4\n");

        int[]    nArr     = {6,   10,  4};
        int[]    pArr     = {30,  40,  25};
        int[]    hrArr    = {130, 180, 100};
        int[]    wArr     = {20,  20,  25};
        String[] cfgNames = {"Standard (N=6)", "High-Conc. (N=10)", "Compact (N=4)"};

        int numHours = 15;
        int[] hours  = new int[numHours];
        for (int i = 0; i < numHours; i++) hours[i] = i + 6;

        double[][] etaOpt = new double[3][numHours];

        for (int c = 0; c < 3; c++) {
            DesignParameters params =
                    new DesignParameters(hrArr[c], 16, wArr[c], pArr[c], nArr[c]);

            // Use daily times list for this config
            List<LocalDateTime> times = new ArrayList<>();
            for (int h : hours)
                times.add(LocalDateTime.of(2024, Month.JUNE, 21, h, 0));

            FresnelDesignProblem problem =
                    new FresnelDesignProblem(LAT_DIYARBAKIR, LON_DIYARBAKIR, times);

            for (int hi = 0; hi < numHours; hi++) {
                LocalDateTime t = LocalDateTime.of(2024, Month.JUNE, 21, hours[hi], 0);

                // Use evaluateOpticalMetrics — thread-safe, returns cosine directly
                Map<String, Double> metrics = problem.evaluateOpticalMetrics(params, t);
                double cos  = metrics.get("cosine");
                double shad = metrics.get("shading");

                if (cos > 0) {
                    // eta_opt = cosine_eff * (1 - shading_loss/100) * rho_m
                    // cosine is already averaged [%], shading is loss [%]
                    double etaCos  = cos / 100.0;
                    double etaShad = 1.0 - shad / 100.0;
                    etaOpt[c][hi]  = etaCos * etaShad * 0.92 * 100.0; // rho_m = 0.92
                } else {
                    etaOpt[c][hi] = 0.0;
                }
            }
        }

        // Print table
        System.out.printf("%-6s", "Hour");
        for (String n : cfgNames) System.out.printf(" %22s", n + " (%)");
        System.out.println();
        System.out.println("-".repeat(6 + 3 * 23));
        for (int hi = 0; hi < numHours; hi++) {
            System.out.printf("%-6d", hours[hi]);
            for (int c = 0; c < 3; c++)
                System.out.printf(" %22.4f", etaOpt[c][hi]);
            System.out.println();
        }

        // Export CSV
        String csvFile = "daily_efficiency_profile.csv";
        try (java.io.PrintWriter pw =
                new java.io.PrintWriter(new java.io.FileWriter(csvFile))) {
            pw.println("hour,standard_n6,highconc_n10,compact_n4");
            for (int hi = 0; hi < numHours; hi++)
                pw.printf(Locale.US, "%d,%.6f,%.6f,%.6f%n",
                        hours[hi], etaOpt[0][hi], etaOpt[1][hi], etaOpt[2][hi]);
            System.out.printf("%nSaved: %s%n", csvFile);
            System.out.println("Next step: python scripts/plot_figure8.py");
        } catch (java.io.IOException e) {
            System.err.println("Error writing " + csvFile + ": " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 8: CONVERGENCE DATA EXPORT — Fig. 9 (CSV)
    // ================================================================
    /**
     * Runs GA, PSO, SA each 30 times and exports per-iteration best fitness.
     * Output: convergence_SA.csv, convergence_GA.csv, convergence_PSO.csv
     * Next step: python scripts/plot_convergence.py
     */
    public static void runConvergenceExport() {
        System.out.println("=== TEST 8: Convergence Data Export (Fig. 9) ===");
        System.out.printf("Runs per algorithm: %d | Diyarbakir | H=144%n%n",
                NUM_OPTIMIZATION_RUNS);
        List<LocalDateTime> times = getEvaluationTimes(144);
        FresnelDesignProblem problem = new FresnelDesignProblem(
                LAT_DIYARBAKIR, LON_DIYARBAKIR, times);
        DesignParameters init = new DesignParameters(130.0, 16.0, 20.0, 30.0, 4);

        exportAlgorithmConvergence("SA", () -> {
            SimulatedAnnealing sa = new SimulatedAnnealing();
            Map<String, Object> sp = new HashMap<>();
            sp.put("initialTemperature", 1000.0);
            sp.put("coolingRate",        0.95);
            sp.put("maxIterations",      1000);
            sp.put("minTemperature",     1e-10);
            sa.setParameters(sp);
            return sa;
        }, problem, init);

        exportAlgorithmConvergence("GA", () -> {
            GeneticAlgorithm ga = new GeneticAlgorithm();
            Map<String, Object> gp = new HashMap<>();
            gp.put("populationSize", 50);
            gp.put("maxGenerations", 100);
            gp.put("crossoverRate",  0.8);
            gp.put("mutationRate",   0.1);
            gp.put("elitismRate",    0.1);
            ga.setParameters(gp);
            return ga;
        }, problem, init);

        exportAlgorithmConvergence("PSO", () -> {
            ParticleSwarm pso = new ParticleSwarm();
            Map<String, Object> pp = new HashMap<>();
            pp.put("swarmSize",       30);
            pp.put("maxIterations",   100);
            pp.put("inertiaWeight",   0.729);
            pp.put("cognitiveWeight", 1.49445);
            pp.put("socialWeight",    1.49445);
            pso.setParameters(pp);
            return pso;
        }, problem, init);

        System.out.println("\n=== Convergence CSV files exported. ===");
        System.out.println("Next step: python scripts/plot_convergence.py");
    }

    @FunctionalInterface
    private interface AlgorithmFactory {
        IOptimizationAlgorithm create();
    }

    private static void exportAlgorithmConvergence(String name,
            AlgorithmFactory factory, FresnelDesignProblem problem,
            DesignParameters init) {
        System.out.printf("Running %s (%d runs)...%n", name, NUM_OPTIMIZATION_RUNS);
        String csvFile = "convergence_" + name + ".csv";
        try (java.io.PrintWriter pw =
                new java.io.PrintWriter(new java.io.FileWriter(csvFile))) {
            pw.println("run,iteration,best_fitness");
            double[] finalFitness = new double[NUM_OPTIMIZATION_RUNS];
            long[]   runTimes     = new long[NUM_OPTIMIZATION_RUNS];
            for (int r = 0; r < NUM_OPTIMIZATION_RUNS; r++) {
                IOptimizationAlgorithm algo = factory.create();
                algo.reset();
                long t0  = System.currentTimeMillis();
                DesignSolution sol = algo.optimize(problem, init, new HashMap<>());
                runTimes[r]     = System.currentTimeMillis() - t0;
                finalFitness[r] = sol.getObjectiveValue();
                List<DesignSolution> history = algo.getHistory();
                double bestSoFar = Double.NEGATIVE_INFINITY;
                for (int i = 0; i < history.size(); i++) {
                    bestSoFar = Math.max(bestSoFar,
                            history.get(i).getObjectiveValue());
                    pw.printf(Locale.US, "%d,%d,%.6f%n", r, i, bestSoFar);
                }
                System.out.printf("  Run %d/%d → %.2f (%d ms)\r",
                        r + 1, NUM_OPTIMIZATION_RUNS,
                        finalFitness[r], runTimes[r]);
            }
            System.out.println();
            double mean = Arrays.stream(finalFitness).average().orElse(0);
            double best = Arrays.stream(finalFitness).max().orElse(0);
            double std  = Math.sqrt(Arrays.stream(finalFitness)
                    .map(v -> (v - mean) * (v - mean)).average().orElse(0));
            double avgT = Arrays.stream(runTimes).average().orElse(0);
            System.out.printf("  %s: Best=%.2f | Mean=%.2f | Std=%.2f | AvgTime=%.1f ms%n",
                    name, best, mean, std, avgT);
            System.out.printf("  Saved: %s%n", csvFile);
        } catch (java.io.IOException e) {
            System.err.println("Error writing " + csvFile + ": " + e.getMessage());
        }
    }

    // ================================================================
    // TEST 9: RUN ALL TESTS
    // ================================================================
    public static void runAllTests() {
        System.out.println("=== RUNNING ALL TESTS (1-8) ===\n");
        String sep = "\n" + "=".repeat(64) + "\n";
        runExtremeAngleAnalysis();    System.out.print(sep);
        runMirrorCountScaling();      System.out.print(sep);
        runOptimizationComparison();  System.out.print(sep);
        runTemporalSensitivity();     System.out.print(sep);
        runSpacingSweepExport();      System.out.print(sep);
        runHeightSweepExport();       System.out.print(sep);
        runDailyEfficiencyProfile();  System.out.print(sep);
        runConvergenceExport();
        System.out.println("\n=== ALL TESTS COMPLETED ===");
        System.out.println("CSV files ready for Python plotting:");
        System.out.println("  spacing_sweep.csv            → python scripts/plot_figure6.py");
        System.out.println("  height_sweep.csv             → python scripts/plot_figure7.py");
        System.out.println("  daily_efficiency_profile.csv → python scripts/plot_figure8.py");
        System.out.println("  convergence_*.csv            → python scripts/plot_convergence.py");
    }

    // ================================================================
    // HELPERS
    // ================================================================

    /**
     * Generates representative evaluation time points for annual optimization.
     *
     * H=144  : 12 months x 12 daylight hours (7-18) on the 15th  = 144 points
     * H=288  : 12 months x 24 half-hourly slots on the 15th       = 288 points
     * H>288  : Full year, all days, hourly 7-18                   ~= 4380 points
     */
    private static List<LocalDateTime> getEvaluationTimes(int H) {
        List<LocalDateTime> t = new ArrayList<>();
        int y = 2024;
        if (H <= 144) {
            for (Month m : Month.values())
                for (int h = 7; h <= 18; h++)
                    t.add(LocalDateTime.of(y, m, 15, h, 0));
        } else if (H <= 288) {
            for (Month m : Month.values())
                for (int h = 7; h <= 18; h++) {
                    t.add(LocalDateTime.of(y, m, 15, h,  0));
                    t.add(LocalDateTime.of(y, m, 15, h, 30));
                }
        } else {
            for (int m = 1; m <= 12; m++) {
                int mx = LocalDateTime.of(y, m, 1, 0, 0).getMonth().length(true);
                for (int d = 1; d <= mx; d++)
                    for (int h = 7; h <= 18; h++)
                        try { t.add(LocalDateTime.of(y, m, d, h, 0)); }
                        catch (Exception ignored) {}
            }
        }
        return t;
    }

    private static ParticleSwarm buildPSO() {
        ParticleSwarm pso = new ParticleSwarm();
        Map<String, Object> pp = new HashMap<>();
        pp.put("swarmSize",       30);
        pp.put("maxIterations",   100);
        pp.put("inertiaWeight",   0.729);
        pp.put("cognitiveWeight", 1.49445);
        pp.put("socialWeight",    1.49445);
        pso.setParameters(pp);
        return pso;
    }

    private static List<IOptimizationAlgorithm> setupAlgorithms() {
        List<IOptimizationAlgorithm> a = new ArrayList<>();

        GeneticAlgorithm ga = new GeneticAlgorithm();
        Map<String, Object> gp = new HashMap<>();
        gp.put("populationSize", 50);
        gp.put("maxGenerations", 100);
        gp.put("crossoverRate",  0.8);
        gp.put("mutationRate",   0.1);
        gp.put("elitismRate",    0.1);
        ga.setParameters(gp);
        a.add(ga);

        a.add(buildPSO());

        SimulatedAnnealing sa = new SimulatedAnnealing();
        Map<String, Object> sp = new HashMap<>();
        sp.put("initialTemperature", 1000.0);
        sp.put("coolingRate",        0.95);
        sp.put("maxIterations",      1000);
        sp.put("minTemperature",     1e-10);
        sa.setParameters(sp);
        a.add(sa);

        return a;
    }
}