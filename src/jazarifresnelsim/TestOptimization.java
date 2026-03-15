package jazarifresnelsim;

import jazarifresnelsim.optimization.algorithms.*;
import jazarifresnelsim.optimization.problem.*;
import jazarifresnelsim.optimization.evaluation.*;
import jazarifresnelsim.domain.SolarCalculator;
import jazarifresnelsim.models.SolarPosition;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Comprehensive validation and optimization test suite for JazariFresnelSim.
 *
 * This class provides all reproducible experiments reported in: "Real-Time
 * Optical Simulation and Rapid Parametric Exploration of Linear Fresnel
 * Reflector Systems Using an Analytical Tracking Model"
 *
 * Submitted to Solar Energy (Elsevier), 2026.
 *
 * Usage: Run main() and select a test scenario from the interactive menu. All
 * results are printed to stdout for easy verification.
 *
 * @author Yunus Demirtas, Musa Atas
 * @version 2.1
 */
public class TestOptimization {

    private static final double LAT_DIYARBAKIR = 37.962984;
    private static final double LON_DIYARBAKIR = 41.850347;
    private static final double LAT_ALMERIA = 36.84;
    private static final double LON_ALMERIA = -2.46;
    private static final double LAT_RIYADH = 24.63;
    private static final double LON_RIYADH = 46.72;
    private static final int NUM_OPTIMIZATION_RUNS = 30;
    private static final double LAT_BERLIN = 52.52;
    private static final double LON_BERLIN = 13.405;
    private static final double LAT_JEDDAH = 21.49;
    private static final double LON_JEDDAH = 39.19;

    public static void main(String[] args) {
        printBanner();
        Scanner scanner = new Scanner(System.in);
        while (true) {
            printMenu();
            System.out.print("\nSelect option (0-8): ");
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
                case 1:
                    runOptimizationComparison();
                    break;
                case 2:
                    runExtremeAngleAnalysis();
                    break;
                case 3:
                    runTemporalSensitivity();
                    break;
                case 4:
                    runParametricSpacingSweep();
                    break;
                case 5:
                    runParametricHeightSweep();
                    break;
                case 6:
                    runMirrorCountScaling();
                    break;
                case 7:
                    runAllTests();
                    break;
                case 8:
                    runConvergenceExport();
                    break;
                case 0:
                    System.out.println("Exiting.");
                    scanner.close();
                    return;
                default:
                    System.out.println("Invalid option.\n");
                    continue;
            }
            long elapsed = System.currentTimeMillis() - startTime;
            System.out.println("\n--- Completed in " + String.format("%.2f", elapsed / 1000.0) + " seconds ---\n");
        }
    }

    // ================================================================
    // TEST 8: CONVERGENCE DATA FOR FIGURE 9
    // ================================================================
    /**
     * Generates convergence history CSV files for Figure 9. Runs each algorithm
     * 30 times and exports per-iteration best fitness.
     *
     * Output files: convergence_SA.csv — columns: run, iteration, best_fitness
     * convergence_GA.csv — columns: run, iteration, best_fitness
     * convergence_PSO.csv — columns: run, iteration, best_fitness
     * convergence_summary.csv — columns: algorithm, run, time_ms, best_fitness
     */
    public static void runConvergenceExport() {
        System.out.println("=== TEST 8: Convergence Data Export for Figure 9 ===");
        System.out.println("Runs per algorithm: " + NUM_OPTIMIZATION_RUNS);
        System.out.println("Location: Diyarbakir | H=144\n");

        List<LocalDateTime> times = getEvaluationTimes(144);
        FresnelDesignProblem problem = new FresnelDesignProblem(LAT_DIYARBAKIR, LON_DIYARBAKIR, times);
        DesignParameters init = new DesignParameters(130.0, 16.0, 20.0, 30.0, 4);

        // --- SA ---
        exportAlgorithmConvergence("SA", () -> {
            SimulatedAnnealing sa = new SimulatedAnnealing();
            Map<String, Object> sp = new HashMap<>();
            sp.put("initialTemperature", 1000.0);
            sp.put("coolingRate", 0.95);
            sp.put("maxIterations", 1000);
            sp.put("minTemperature", 1e-10);
            sa.setParameters(sp);
            return sa;
        }, problem, init);

        // --- GA ---
        exportAlgorithmConvergence("GA", () -> {
            GeneticAlgorithm ga = new GeneticAlgorithm();
            Map<String, Object> gp = new HashMap<>();
            gp.put("populationSize", 50);
            gp.put("maxGenerations", 100);
            gp.put("crossoverRate", 0.8);
            gp.put("mutationRate", 0.1);
            gp.put("elitismRate", 0.1);
            ga.setParameters(gp);
            return ga;
        }, problem, init);

        // --- PSO ---
        exportAlgorithmConvergence("PSO", () -> {
            ParticleSwarm pso = new ParticleSwarm();
            Map<String, Object> pp = new HashMap<>();
            pp.put("swarmSize", 30);
            pp.put("maxIterations", 100);
            pp.put("inertiaWeight", 0.729);
            pp.put("cognitiveWeight", 1.49445);
            pp.put("socialWeight", 1.49445);
            pso.setParameters(pp);
            return pso;
        }, problem, init);

        System.out.println("\n=== All convergence CSV files exported. ===");
        System.out.println("Run 'python plot_convergence.py' to generate Figure 9.");
    }

    @FunctionalInterface
    private interface AlgorithmFactory {

        IOptimizationAlgorithm create();
    }

    private static void exportAlgorithmConvergence(String name, AlgorithmFactory factory,
            FresnelDesignProblem problem, DesignParameters init) {
        System.out.println("Running " + name + " (" + NUM_OPTIMIZATION_RUNS + " runs)...");

        String csvFile = "convergence_" + name + ".csv";
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(csvFile))) {
            pw.println("run,iteration,best_fitness");

            double[] finalFitness = new double[NUM_OPTIMIZATION_RUNS];
            long[] runTimes = new long[NUM_OPTIMIZATION_RUNS];

            for (int r = 0; r < NUM_OPTIMIZATION_RUNS; r++) {
                IOptimizationAlgorithm algo = factory.create();
                algo.reset();

                long t0 = System.currentTimeMillis();
                DesignSolution sol = algo.optimize(problem, init, new HashMap<>());
                runTimes[r] = System.currentTimeMillis() - t0;
                finalFitness[r] = sol.getObjectiveValue();

                // Write convergence history for this run
                List<DesignSolution> history = algo.getHistory();
                double bestSoFar = Double.NEGATIVE_INFINITY;
                for (int i = 0; i < history.size(); i++) {
                    double val = history.get(i).getObjectiveValue();
                    bestSoFar = Math.max(bestSoFar, val);
                    pw.println(r + "," + i + "," + String.format(Locale.US, "%.6f", bestSoFar));
                }

                System.out.print("  Run " + (r + 1) + "/" + NUM_OPTIMIZATION_RUNS
                        + " → " + String.format("%.2f", finalFitness[r])
                        + " (" + runTimes[r] + " ms)\r");
            }
            System.out.println();

            // Print summary statistics
            double mean = Arrays.stream(finalFitness).average().orElse(0);
            double best = Arrays.stream(finalFitness).max().orElse(0);
            double std = Math.sqrt(Arrays.stream(finalFitness)
                    .map(v -> (v - mean) * (v - mean)).average().orElse(0));
            double avgTime = Arrays.stream(runTimes).average().orElse(0);

            System.out.println(String.format("  %s: Best=%.2f, Mean=%.2f, Std=%.2f, AvgTime=%.1fms",
                    name, best, mean, std, avgTime));
            System.out.println("  Saved to: " + csvFile);

        } catch (java.io.IOException e) {
            System.err.println("Error writing " + csvFile + ": " + e.getMessage());
        }
    }

    private static void printBanner() {
        System.out.println("================================================================");
        System.out.println("  JazariFresnelSim — Validation & Optimization Test Suite v2.1");
        System.out.println("  Paper: Real-Time Optical Simulation of LFR Systems");
        System.out.println("  Journal: Solar Energy (Elsevier)");
        System.out.println("================================================================\n");
    }

    private static void printMenu() {
        System.out.println("========== MAIN MENU ==========");
        System.out.println("  [1] Metaheuristic Optimization (GA, PSO, SA)  — Table 12-13");
        System.out.println("  [2] Extreme-Angle Annual Error Analysis       — Table 6");
        System.out.println("  [3] Temporal Discretization Sensitivity       — Table 14");
        System.out.println("  [4] Parametric Sweep: Mirror Spacing          — Fig. 6");
        System.out.println("  [5] Parametric Sweep: Receiver Height         — Fig. 7");
        System.out.println("  [6] Mirror Count Scaling                      — Table 7");
        System.out.println("  [7] Run ALL Tests (comprehensive)");
        System.out.println("  [8] Generate Convergence Data                 — Fig. 9");
        System.out.println("  [0] Exit");
        System.out.println("================================");
    }

    // ================================================================
    // TEST 1: METAHEURISTIC OPTIMIZATION
    // ================================================================
    public static void runOptimizationComparison() {
        System.out.println("=== TEST 1: Metaheuristic Optimization Comparison ===");
        System.out.println("Location: Diyarbakir (37.96N, 41.85E) | H=144 | Runs: " + NUM_OPTIMIZATION_RUNS + "\n");
        try {
            FresnelDesignProblem problem = new FresnelDesignProblem(LAT_DIYARBAKIR, LON_DIYARBAKIR, getEvaluationTimes(144));
            DesignParameters init = new DesignParameters(130.0, 16.0, 20.0, 30.0, 4);
            List<IOptimizationAlgorithm> algos = setupAlgorithms();
            DesignEvaluator evaluator = new DesignEvaluator(problem, getEvaluationTimes(144));
            OptimizationComparison comp = new OptimizationComparison(evaluator, algos.size(), NUM_OPTIMIZATION_RUNS);
            OptimizationComparison.ComparisonResult results = comp.compareAlgorithms(algos, problem, init, new HashMap<>());
            printOptimizationResults(results);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printOptimizationResults(OptimizationComparison.ComparisonResult results) {
        Map<String, OptimizationComparison.AlgorithmStats> stats = results.getStatistics();
        System.out.println("\n--- Table 13: Performance Comparison ---");
        System.out.println(String.format("%-25s %12s %18s %12s %18s", "Algorithm", "Time (s)", "Best (kW/m²)", "Std Dev", "Type"));
        System.out.println("-".repeat(85));
        String bestAlgo = "";
        double bestYield = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, OptimizationComparison.AlgorithmStats> e : stats.entrySet()) {
            String name = e.getKey();
            var stat = e.getValue();
            String type = stat.objectiveStats.stdDev < 0.01 ? "Global" : stat.objectiveStats.stdDev < 15.0 ? "Near-global" : "Local/Inconsistent";
            System.out.println(String.format("%-25s %12.2f %18.2f %12.2f %18s", name, stat.timeStats.mean / 1000.0, stat.objectiveStats.max, stat.objectiveStats.stdDev, type));
            if (stat.objectiveStats.max > bestYield) {
                bestYield = stat.objectiveStats.max;
                bestAlgo = name;
            }
        }
        var runs = results.getAlgorithmRuns().get(bestAlgo);
        if (runs != null) {
            var best = runs.stream().max(Comparator.comparing(r -> r.getBestSolution().getObjectiveValue())).orElse(null);
            if (best != null) {
                DesignParameters p = best.getBestSolution().getParameters();
                System.out.println("\n--- Best Solution (" + bestAlgo + ") ---");
                System.out.println("  Hr=" + String.format("%.1f", p.getReceiverHeight()) + "cm, Dr=" + String.format("%.1f", p.getReceiverDiameter())
                        + "cm, w=" + String.format("%.1f", p.getMirrorWidth()) + "cm, p=" + String.format("%.1f", p.getMirrorSpacing())
                        + "cm, N=" + p.getNumberOfMirrors() + " → " + String.format("%.2f", bestYield) + " kW/m²");
            }
        }
    }

// ================================================================
    // TEST 2: EXTREME-ANGLE ANALYSIS — UPDATED LOCATIONS
    // ================================================================
    public static void runExtremeAngleAnalysis() {
        System.out.println("=== TEST 2: Extreme-Angle Annual Error Analysis (Table 8) ===\n");
        double[][] locs = {
            {LAT_DIYARBAKIR, LON_DIYARBAKIR},
            {LAT_BERLIN, LON_BERLIN},
            {LAT_JEDDAH, LON_JEDDAH}
        };
        String[] names = {
            "Diyarbakir (37.96N)",
            "Berlin     (52.52N)",
            "Jeddah     (21.49N)"
        };
        System.out.println(String.format("%-22s %10s %13s %14s %18s",
                "Location", "Daylight h", "h θT>55°", "Fraction(%)", "Yield Dev(%)"));
        System.out.println("-".repeat(80));
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
                                double thetaT = 90.0 - Math.abs(
                                        Math.toDegrees(Math.atan(Math.tan(aR) / Math.cos(azR))));
                                totalE += dni * Math.max(0, Math.cos(Math.toRadians(thetaT)));
                                if (thetaT > 55) {
                                    extreme++;
                                    extremeE += dni * 0.3;
                                }
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }
            System.out.println(String.format("%-22s %10d %13d %14.1f %18.1f",
                    names[i], total, extreme,
                    100.0 * extreme / total,
                    100.0 * extremeE / totalE));
        }
    }

    // ================================================================
    // TEST 3: TEMPORAL SENSITIVITY
    // ================================================================
    public static void runTemporalSensitivity() {
        System.out.println("=== TEST 3: Temporal Discretization Sensitivity (Table 14) ===\n");
        int[] hVals = {144, 288, 4380};
        String[] labels = {"144 (baseline)", "288 (bi-hourly)", "4380 (full year)"};
        System.out.println(String.format("%-20s %6s %8s %8s %12s %10s", "Resolution", "N", "p(cm)", "Hr(cm)", "Yield", "Time(s)"));
        System.out.println("-".repeat(70));
        for (int idx = 0; idx < 3; idx++) {
            var times = getEvaluationTimes(hVals[idx]);
            var problem = new FresnelDesignProblem(LAT_DIYARBAKIR, LON_DIYARBAKIR, times);
            var pso = new ParticleSwarm();
            Map<String, Object> pp = new HashMap<>();
            pp.put("swarmSize", 30);
            pp.put("maxIterations", 100);
            pp.put("inertiaWeight", 0.729);
            pp.put("cognitiveWeight", 1.49445);
            pp.put("socialWeight", 1.49445);
            pso.setParameters(pp);
            long t0 = System.currentTimeMillis();
            var sol = pso.optimize(problem, new DesignParameters(130, 16, 20, 30, 4), new HashMap<>());
            double sec = (System.currentTimeMillis() - t0) / 1000.0;
            var p = sol.getParameters();
            System.out.println(String.format("%-20s %6d %8.1f %8.1f %12.2f %10.2f", labels[idx], p.getNumberOfMirrors(), p.getMirrorSpacing(), p.getReceiverHeight(), sol.getObjectiveValue(), sec));
        }
    }

    // ================================================================
    // TEST 4: SPACING SWEEP
    // ================================================================
    public static void runParametricSpacingSweep() {
        System.out.println("=== TEST 4: Mirror Spacing Sweep (Fig. 6) ===\n");
        System.out.println(String.format("%-12s %15s", "Spacing(cm)", "Energy(W)"));
        System.out.println("-".repeat(30));
        for (int sp : new int[]{20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70}) {
            var times = new ArrayList<LocalDateTime>();
            times.add(LocalDateTime.of(2024, Month.JUNE, 21, 12, 0));
            double e = new FresnelDesignProblem(LAT_DIYARBAKIR, LON_DIYARBAKIR, times).evaluateDesignForAllTimes(new DesignParameters(130, 16, 20, sp, 6)).values().stream().mapToDouble(Double::doubleValue).sum();
            System.out.println(String.format("%-12d %15.4f", sp, e));
        }
    }

    // ================================================================
    // TEST 5: HEIGHT SWEEP
    // ================================================================
    public static void runParametricHeightSweep() {
        System.out.println("=== TEST 5: Receiver Height Sweep (Fig. 7) ===\n");
        System.out.println(String.format("%-10s %18s %18s", "Hr(cm)", "6-mirror(W)", "10-mirror(W)"));
        System.out.println("-".repeat(50));
        for (int hr : new int[]{80, 90, 100, 110, 120, 130, 140, 150, 160, 170, 180, 200, 220, 250}) {
            var times = new ArrayList<LocalDateTime>();
            times.add(LocalDateTime.of(2024, Month.JUNE, 21, 12, 0));
            var p6 = new FresnelDesignProblem(LAT_DIYARBAKIR, LON_DIYARBAKIR, times);
            var p12 = new FresnelDesignProblem(LAT_DIYARBAKIR, LON_DIYARBAKIR, times);
            double e6 = p6.evaluateDesignForAllTimes(new DesignParameters(hr, 16, 20, 30, 6)).values().stream().mapToDouble(Double::doubleValue).sum();
            double e12 = p12.evaluateDesignForAllTimes(new DesignParameters(hr, 16, 20, 40, 10)).values().stream().mapToDouble(Double::doubleValue).sum();
            System.out.println(String.format("%-10d %18.4f %18.4f", hr, e6, e12));
        }
    }

    // ================================================================
    // TEST 6: MIRROR COUNT SCALING
    // ================================================================
    public static void runMirrorCountScaling() {
        System.out.println("=== TEST 6: Mirror Count Scaling (Table 7) ===\n");
        System.out.println(String.format("%-6s %15s %15s", "N", "Energy(W)", "Width(cm)"));
        System.out.println("-".repeat(40));
        for (int n : new int[]{2, 4, 6, 8, 10}) {
            var times = new ArrayList<LocalDateTime>();
            times.add(LocalDateTime.of(2024, Month.JUNE, 21, 12, 0));
            double e = new FresnelDesignProblem(LAT_DIYARBAKIR, LON_DIYARBAKIR, times).evaluateDesignForAllTimes(new DesignParameters(130, 16, 20, 30, n)).values().stream().mapToDouble(Double::doubleValue).sum();
            System.out.println(String.format("%-6d %15.4f %15.0f", n, e, (n - 1) * 30.0));
        }
    }

    // ================================================================
    // TEST 7: ALL
    // ================================================================
    public static void runAllTests() {
        System.out.println("=== RUNNING ALL TESTS ===\n");
        runExtremeAngleAnalysis();
        System.out.println("\n" + "=".repeat(60));
        runParametricSpacingSweep();
        System.out.println("\n" + "=".repeat(60));
        runParametricHeightSweep();
        System.out.println("\n" + "=".repeat(60));
        runMirrorCountScaling();
        System.out.println("\n" + "=".repeat(60));
        runTemporalSensitivity();
        System.out.println("\n" + "=".repeat(60));
        runOptimizationComparison();
        System.out.println("\n" + "=".repeat(60));
        runConvergenceExport();
        System.out.println("\n=== ALL TESTS COMPLETED ===");
    }

    // ================================================================
    // HELPERS
    // ================================================================
    private static List<LocalDateTime> getEvaluationTimes(int H) {
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
                int mx = LocalDateTime.of(y, m, 1, 0, 0).getMonth().length(true);
                for (int d = 1; d <= mx; d++) {
                    for (int h = 7; h <= 18; h++)try {
                        t.add(LocalDateTime.of(y, m, d, h, 0));
                    } catch (Exception e) {
                    }
                }
            }
        }
        return t;
    }

    private static List<IOptimizationAlgorithm> setupAlgorithms() {
        List<IOptimizationAlgorithm> a = new ArrayList<>();
        GeneticAlgorithm ga = new GeneticAlgorithm();
        Map<String, Object> gp = new HashMap<>();
        gp.put("populationSize", 50);
        gp.put("maxGenerations", 100);
        gp.put("crossoverRate", 0.8);
        gp.put("mutationRate", 0.1);
        gp.put("elitismRate", 0.1);
        ga.setParameters(gp);
        a.add(ga);
        ParticleSwarm pso = new ParticleSwarm();
        Map<String, Object> pp = new HashMap<>();
        pp.put("swarmSize", 30);
        pp.put("maxIterations", 100);
        pp.put("inertiaWeight", 0.729);
        pp.put("cognitiveWeight", 1.49445);
        pp.put("socialWeight", 1.49445);
        pso.setParameters(pp);
        a.add(pso);
        SimulatedAnnealing sa = new SimulatedAnnealing();
        Map<String, Object> sp = new HashMap<>();
        sp.put("initialTemperature", 1000.0);
        sp.put("coolingRate", 0.95);
        sp.put("maxIterations", 1000);
        sp.put("minTemperature", 1e-10);
        sa.setParameters(sp);
        a.add(sa);
        return a;
    }
}
