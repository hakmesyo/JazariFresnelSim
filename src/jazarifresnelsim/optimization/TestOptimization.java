package jazarifresnelsim.optimization;

import jazarifresnelsim.optimization.algorithms.*;
import jazarifresnelsim.optimization.problem.*;
import jazarifresnelsim.optimization.evaluation.*;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Test class demonstrating the usage of Fresnel system optimization framework.
 * This class provides a comprehensive example of setting up and running
 * different optimization algorithms for Fresnel system design optimization.
 */
public class TestOptimization {

    public static void main(String[] args) {
        // Run the optimization test scenario
        runOptimizationTest();
    }

    /**
     * Runs a complete optimization test scenario including multiple algorithms
     * and comprehensive performance analysis.
     */
    public static void runOptimizationTest() {
        try {
            // 1. Problem definition
            System.out.println("1. Setting up the optimization problem...");
            FresnelDesignProblem problem = createProblem();

            // 2. Initial design
            System.out.println("2. Creating initial design parameters...");
            DesignParameters initialParams = createInitialDesign();

            // 3. Setup algorithms
            System.out.println("3. Initializing optimization algorithms...");
            List<IOptimizationAlgorithm> algorithms = setupAlgorithms();

            // 4. Setup evaluation framework
            System.out.println("4. Setting up evaluation framework...");
            DesignEvaluator evaluator = new DesignEvaluator(problem, getEvaluationTimes());
            OptimizationComparison comparison = new OptimizationComparison(evaluator, 3, 30);

            // 5. Run optimization
            System.out.println("5. Starting optimization process...\n");
            Map<String, Object> constraints = new HashMap<>();
            OptimizationComparison.ComparisonResult results
                    = comparison.compareAlgorithms(algorithms, problem, initialParams, constraints);

            // 6. Analyze and report results
            System.out.println("\n=== OPTIMIZATION RESULTS ===");
            analyzeResults(results);

        } catch (Exception e) {
            System.err.println("Error during test execution: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates the Fresnel design optimization problem for a specific location.
     */
    private static FresnelDesignProblem createProblem() {
        // Define problem for Siirt University location
        double latitude = 37.962984;   // Latitude
        double longitude = 41.850347;  // Longitude

        return new FresnelDesignProblem(
                latitude,
                longitude,
                getEvaluationTimes()
        );
    }

    /**
     * Defines evaluation times for performance assessment. Includes seasonal
     * evaluations and hourly analysis for summer day.
     */
    private static List<LocalDateTime> getEvaluationTimes() {
        List<LocalDateTime> times = new ArrayList<>();

        // Characteristic days for four seasons
        int year = 2024;
        times.add(LocalDateTime.of(year, Month.MARCH, 21, 12, 0));     // Spring equinox
        times.add(LocalDateTime.of(year, Month.JUNE, 21, 12, 0));      // Summer solstice
        times.add(LocalDateTime.of(year, Month.SEPTEMBER, 23, 12, 0)); // Autumn equinox
        times.add(LocalDateTime.of(year, Month.DECEMBER, 21, 12, 0));  // Winter solstice

        // Hourly evaluation for summer day
        LocalDateTime summerDay = LocalDateTime.of(year, Month.JUNE, 21, 0, 0);
        for (int hour = 8; hour <= 16; hour++) {
            times.add(summerDay.withHour(hour));
        }

        return times;
    }

    /**
     * Creates initial design parameters for the optimization. These values
     * serve as the starting point for all algorithms.
     */
    private static DesignParameters createInitialDesign() {
        return new DesignParameters(
                130.0, // receiverHeight (cm)
                16.0, // receiverDiameter (cm)
                20.0, // mirrorWidth (cm)
                //100.0, // mirrorLength (cm)
                30.0, // mirrorSpacing (cm)
                4 // numberOfMirrors
        );
    }

    /**
     * Sets up and configures all optimization algorithms with their parameters.
     */
    private static List<IOptimizationAlgorithm> setupAlgorithms() {
        List<IOptimizationAlgorithm> algorithms = new ArrayList<>();

        // 1. Genetic Algorithm
        GeneticAlgorithm ga = new GeneticAlgorithm();
        Map<String, Object> gaParams = new HashMap<>();
        gaParams.put("populationSize", 50);
        gaParams.put("maxGenerations", 100);
        gaParams.put("crossoverRate", 0.8);
        gaParams.put("mutationRate", 0.1);
        gaParams.put("elitismRate", 0.1);
        ga.setParameters(gaParams);
        algorithms.add(ga);

        // 2. Particle Swarm Optimization
        ParticleSwarm pso = new ParticleSwarm();
        Map<String, Object> psoParams = new HashMap<>();
        psoParams.put("swarmSize", 30);
        psoParams.put("maxIterations", 100);
        psoParams.put("inertiaWeight", 0.729);
        psoParams.put("cognitiveWeight", 1.49445);
        psoParams.put("socialWeight", 1.49445);
        pso.setParameters(psoParams);
        algorithms.add(pso);

        // 3. Simulated Annealing
        SimulatedAnnealing sa = new SimulatedAnnealing();
        Map<String, Object> saParams = new HashMap<>();
        saParams.put("initialTemperature", 1000.0);
        saParams.put("coolingRate", 0.95);
        saParams.put("maxIterations", 1000);
        saParams.put("minTemperature", 1e-10);
        sa.setParameters(saParams);
        algorithms.add(sa);

        // 4. Reinforcement Learning
        ReinforcementLearning rl = new ReinforcementLearning();
        Map<String, Object> rlParams = new HashMap<>();
        rlParams.put("learningRate", 0.1);
        rlParams.put("discountFactor", 0.9);
        rlParams.put("explorationRate", 0.3);
        rlParams.put("maxEpisodes", 1000);
        rlParams.put("stepsPerEpisode", 100);
        rl.setParameters(rlParams);
        algorithms.add(rl);

        return algorithms;
    }

    /**
     * Analyzes and prints detailed results from the optimization comparison.
     * Includes performance statistics and identifies the best performing
     * algorithm.
     */
    /**
     * Analyzes and prints detailed results from the optimization comparison.
     * Includes area-normalized performance metrics and statistical analysis.
     */
    private static void analyzeResults(OptimizationComparison.ComparisonResult results) {
        // Get statistics for all algorithms
        Map<String, OptimizationComparison.AlgorithmStats> stats = results.getStatistics();

        System.out.println("\n=== OPTIMIZATION RESULTS ===");
        System.out.println("\nDetailed Performance Analysis for Each Algorithm:");
        System.out.println("----------------------------------------");

        // Track best overall performance
        String bestOverallAlgorithm = "";
        double bestOverallEfficiency = Double.NEGATIVE_INFINITY;
        DesignSolution bestOverallSolution = null;
        DesignEvaluator.EvaluationReport bestReport = null;

        // Analyze each algorithm's performance
        for (Map.Entry<String, OptimizationComparison.AlgorithmStats> entry : stats.entrySet()) {
            String algorithmName = entry.getKey();
            OptimizationComparison.AlgorithmStats stat = entry.getValue();

            System.out.println("\nALGORITHM: " + algorithmName);
            System.out.println("----------------------------------------");

            // Performance Statistics
            System.out.println("Performance Statistics:");
            System.out.println("  Mirror Area Efficiency:");
            System.out.println("    Best: " + String.format("%.2f kW/m²", stat.objectiveStats.max));
            System.out.println("    Average: " + String.format("%.2f kW/m²", stat.objectiveStats.mean));
            System.out.println("    Std Dev: " + String.format("%.2f kW/m²", stat.objectiveStats.stdDev));

            // Execution Time Statistics
            System.out.println("\n  Execution Time:");
            System.out.println("    Average: " + String.format("%.2f sec", stat.timeStats.mean / 1000.0));
            System.out.println("    Fastest: " + String.format("%.2f sec", stat.timeStats.min / 1000.0));
            System.out.println("    Slowest: " + String.format("%.2f sec", stat.timeStats.max / 1000.0));

            // Convergence Analysis
            System.out.println("\n  Convergence Rate:");
            System.out.println("    Average Improvement: "
                    + String.format("%.4f kW/m²/iteration", stat.convergenceStats.mean));
            System.out.println("    Final Convergence: "
                    + String.format("%.4f kW/m²", stat.convergenceStats.max));

            // Get best run details for this algorithm
            List<OptimizationComparison.AlgorithmRun> runs = results.getAlgorithmRuns().get(algorithmName);
            OptimizationComparison.AlgorithmRun bestRun = runs.stream()
                    .max(Comparator.comparing(run -> run.getBestSolution().getObjectiveValue()))
                    .orElseThrow();

            DesignSolution bestSolution = bestRun.getBestSolution();
            DesignEvaluator.EvaluationReport report = bestRun.getEvaluationReport();

            // Detailed metrics for best solution
            System.out.println("\n  Best Solution Metrics:");
            System.out.println("    Total Energy Output: "
                    + String.format("%.2f kW", report.getMetrics().get(DesignEvaluator.TOTAL_ENERGY)));
            System.out.println("    Mirror Area Efficiency: "
                    + String.format("%.2f kW/m²", report.getMetrics().get(DesignEvaluator.MIRROR_AREA_EFFICIENCY)));
            System.out.println("    Land Area Efficiency: "
                    + String.format("%.2f kW/m²", report.getMetrics().get(DesignEvaluator.LAND_AREA_EFFICIENCY)));
            System.out.println("    Cost Effectiveness: "
                    + String.format("%.2f kW/$", report.getMetrics().get(DesignEvaluator.COST_EFFECTIVENESS)));

            // Hourly Performance Analysis (YENİ EKLENDİ)
//            System.out.println("\n  Hourly Performance:");
            Map<LocalDateTime, Double> hourlyPerformance = report.getHourlyPerformance();
//            hourlyPerformance.entrySet().stream()
//                    .sorted(Map.Entry.comparingByKey())
//                    .forEach(e -> {
//                        LocalDateTime time = e.getKey();
//                        Double energy = e.getValue();
//                        System.out.println(String.format("    %s: %.2f kW",
//                                time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
//                                energy));
//                    });

            // Peak Performance Time Detection (YENİ EKLENDİ)
            Map.Entry<LocalDateTime, Double> bestTimeEntry = hourlyPerformance.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);
            if (bestTimeEntry != null) {
                System.out.println("\n  Peak Performance Time:");
                System.out.println("    " + bestTimeEntry.getKey().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        + ": " + String.format("%.2f kW", bestTimeEntry.getValue()));
            }

            // Best solution parameters
            DesignParameters params = bestSolution.getParameters();
            System.out.println("\n  Best Solution Parameters:");
            System.out.println("    Receiver Height: " + String.format("%.1f cm", params.getReceiverHeight()));
            System.out.println("    Receiver Diameter: " + String.format("%.1f cm", params.getReceiverDiameter()));
            System.out.println("    Mirror Width: " + String.format("%.1f cm", params.getMirrorWidth()));
            System.out.println("    Mirror Length: " + String.format("%.1f cm", params.getMirrorLength()));
            System.out.println("    Mirror Spacing: " + String.format("%.1f cm", params.getMirrorSpacing()));
            System.out.println("    Number of Mirrors: " + params.getNumberOfMirrors());

            // Update best overall if necessary
            if (stat.objectiveStats.max > bestOverallEfficiency) {
                bestOverallEfficiency = stat.objectiveStats.max;
                bestOverallAlgorithm = algorithmName;
                bestOverallSolution = bestSolution;
                bestReport = report;
            }

            System.out.println("----------------------------------------");
        }

        // Print overall best results
        System.out.println("\n=== BEST OVERALL PERFORMANCE ===");
        System.out.println("Algorithm: " + bestOverallAlgorithm);
        System.out.println("Maximum Efficiency: " + String.format("%.2f kW/m²", bestOverallEfficiency));

        if (bestReport != null) {
            //System.out.println("\nBest Overall Hourly Performance:");
            Map<LocalDateTime, Double> bestHourlyPerformance = bestReport.getHourlyPerformance();
//            bestHourlyPerformance.entrySet().stream()
//                    .sorted(Map.Entry.comparingByKey())
//                    .forEach(e -> {
//                        LocalDateTime time = e.getKey();
//                        Double energy = e.getValue();
//                        System.out.println(String.format("    %s: %.2f kW",
//                                time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
//                                energy));
//                    });
        }

        // Print optimization implications
        System.out.println("\n=== OPTIMIZATION IMPLICATIONS ===");
        System.out.println("1. Mirror Area Efficiency indicates the system's power density");
        System.out.println("2. Land Area Efficiency shows the total space utilization");
        System.out.println("3. Cost Effectiveness represents the economic viability");

        // Print recommendations
        System.out.println("\n=== RECOMMENDATIONS ===");
        if (bestOverallSolution != null) {
            DesignParameters best = bestOverallSolution.getParameters();
            System.out.println("Based on the optimization results:");
            System.out.println("1. Optimal mirror configuration: " + best.getNumberOfMirrors() + " mirrors");
            System.out.println("2. Recommended mirror dimensions: "
                    + String.format("%.1f x %.1f cm", best.getMirrorWidth(), best.getMirrorLength()));
            System.out.println("3. Optimal receiver height: "
                    + String.format("%.1f cm", best.getReceiverHeight()));
        }
    }
}
