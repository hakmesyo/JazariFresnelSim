package jazarifresnelsim.optimization.evaluation;

import jazarifresnelsim.optimization.algorithms.IOptimizationAlgorithm;
import jazarifresnelsim.optimization.problem.FresnelDesignProblem;
import jazarifresnelsim.optimization.problem.DesignParameters;
import jazarifresnelsim.optimization.problem.DesignSolution;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Compares the performance of different optimization algorithms
 */
public class OptimizationComparison {

    private final int numberOfRuns;
    private final int timeoutMinutes;
    private final DesignEvaluator evaluator;

    public OptimizationComparison(DesignEvaluator evaluator, int numberOfRuns, int timeoutMinutes) {
        this.evaluator = evaluator;
        this.numberOfRuns = numberOfRuns;
        this.timeoutMinutes = timeoutMinutes;
    }

    /**
     * Runs comparison of multiple optimization algorithms
     */
    public ComparisonResult compareAlgorithms(
            List<IOptimizationAlgorithm> algorithms,
            FresnelDesignProblem problem,
            DesignParameters initialParams,
            Map<String, Object> constraints) {

        Map<String, List<AlgorithmRun>> allRuns = new HashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(algorithms.size(), Runtime.getRuntime().availableProcessors())
        );

        // Her algoritma ve çalıştırma için görevleri hazırla
        List<Future<AlgorithmResult>> futures = new ArrayList<>();

        // Thread-safe liste oluştur
        List<IOptimizationAlgorithm> threadSafeAlgorithms
                = Collections.synchronizedList(new ArrayList<>(algorithms));

        // Submit tasks for each algorithm
        for (IOptimizationAlgorithm algorithm : threadSafeAlgorithms) {
            String algorithmName = algorithm.getAlgorithmName();
            allRuns.put(algorithmName, Collections.synchronizedList(new ArrayList<>()));

            for (int run = 0; run < numberOfRuns; run++) {
                final int runNumber = run;
                futures.add(executor.submit(() -> {
                    try {
                        // Her çalıştırma için algoritmanın yeni bir kopyasını oluştur
                        IOptimizationAlgorithm algorithmCopy = algorithm.getClass().getDeclaredConstructor().newInstance();
                        algorithmCopy.setParameters(algorithm.getParameters());

                        AlgorithmResult result = runAlgorithm(algorithmCopy, problem, initialParams, constraints);
                        allRuns.get(algorithmName).add(result.run);
                        return result;
                    } catch (Exception e) {
                        throw new RuntimeException("Error in run " + runNumber + ": " + e.getMessage(), e);
                    }
                }));
            }
        }

        // Collect results
        for (Future<AlgorithmResult> future : futures) {
            try {
                future.get(timeoutMinutes, TimeUnit.MINUTES);
            } catch (Exception e) {
                System.err.println("Error running algorithm: " + e.getMessage());
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutMinutes, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }

        return new ComparisonResult(allRuns);
    }

    private AlgorithmResult runAlgorithm(
            IOptimizationAlgorithm algorithm,
            FresnelDesignProblem problem,
            DesignParameters initialParams,
            Map<String, Object> constraints) {

        algorithm.reset();
        Instant start = Instant.now();

        DesignSolution solution = algorithm.optimize(problem, initialParams, constraints);

        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);

        // Create detailed evaluation report
        DesignEvaluator.EvaluationReport report = evaluator.createReport(solution);

        AlgorithmRun run = new AlgorithmRun(
                solution,
                report,
                algorithm.getHistory(),
                duration
        );

        return new AlgorithmResult(algorithm.getAlgorithmName(), run);
    }

    private static class AlgorithmResult {

        final String algorithmName;
        final AlgorithmRun run;

        AlgorithmResult(String algorithmName, AlgorithmRun run) {
            this.algorithmName = algorithmName;
            this.run = run;
        }
    }

    /**
     * Represents a single run of an optimization algorithm
     */
    public static class AlgorithmRun {

        private final DesignSolution bestSolution;
        private final DesignEvaluator.EvaluationReport evaluationReport;
        private final List<DesignSolution> optimizationHistory;
        private final Duration runTime;

        public AlgorithmRun(DesignSolution bestSolution,
                DesignEvaluator.EvaluationReport evaluationReport,
                List<DesignSolution> optimizationHistory,
                Duration runTime) {
            this.bestSolution = bestSolution;
            this.evaluationReport = evaluationReport;
            this.optimizationHistory = new ArrayList<>(optimizationHistory);
            this.runTime = runTime;
        }

        public DesignSolution getBestSolution() {
            return bestSolution;
        }

        public DesignEvaluator.EvaluationReport getEvaluationReport() {
            return evaluationReport;
        }

        public List<DesignSolution> getOptimizationHistory() {
            return new ArrayList<>(optimizationHistory);
        }

        public Duration getRunTime() {
            return runTime;
        }

        /**
         * Gets convergence history data
         */
        public List<Double> getConvergenceHistory() {
            return optimizationHistory.stream()
                    .map(DesignSolution::getObjectiveValue)
                    .toList();
        }
    }

    /**
     * Contains the results of comparing multiple optimization algorithms
     */
    public static class ComparisonResult {

        private final Map<String, List<AlgorithmRun>> algorithmRuns;

        public ComparisonResult(Map<String, List<AlgorithmRun>> algorithmRuns) {
            this.algorithmRuns = new HashMap<>(algorithmRuns);
        }

        public Map<String, List<AlgorithmRun>> getAlgorithmRuns() {
            return new HashMap<>(algorithmRuns);
        }

        /**
         * Gets statistical summary for each algorithm
         */
        public Map<String, AlgorithmStats> getStatistics() {
            Map<String, AlgorithmStats> stats = new HashMap<>();

            algorithmRuns.forEach((name, runs) -> {
                List<Double> objectives = new ArrayList<>();
                List<Long> times = new ArrayList<>();
                List<Double> convergenceRates = new ArrayList<>();

                for (AlgorithmRun run : runs) {
                    objectives.add(run.bestSolution.getObjectiveValue());
                    times.add(run.runTime.toMillis());
                    convergenceRates.add(calculateConvergenceRate(run.getConvergenceHistory()));
                }

                stats.put(name, new AlgorithmStats(
                        calculateStats(objectives),
                        calculateStats(times.stream().mapToDouble(t -> t).boxed().toList()),
                        calculateStats(convergenceRates)
                ));
            });

            return stats;
        }

        private double calculateConvergenceRate(List<Double> history) {
            if (history.size() < 2) {
                return 0.0;
            }

            double total = 0.0;
            for (int i = 1; i < history.size(); i++) {
                double improvement = history.get(i) - history.get(i - 1);
                total += Math.max(0, improvement);
            }

            return total / (history.size() - 1);
        }

        private StatsData calculateStats(List<Double> values) {
            DoubleSummaryStatistics stats = values.stream()
                    .mapToDouble(Double::doubleValue)
                    .summaryStatistics();

            double median = calculateMedian(values);
            double stdDev = calculateStdDev(values, stats.getAverage());

            return new StatsData(
                    stats.getMin(),
                    stats.getMax(),
                    stats.getAverage(),
                    median,
                    stdDev
            );
        }

        private double calculateMedian(List<Double> values) {
            List<Double> sorted = new ArrayList<>(values);
            Collections.sort(sorted);
            int middle = sorted.size() / 2;
            if (sorted.size() % 2 == 0) {
                return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
            }
            return sorted.get(middle);
        }

        private double calculateStdDev(List<Double> values, double mean) {
            return Math.sqrt(values.stream()
                    .mapToDouble(v -> Math.pow(v - mean, 2))
                    .average()
                    .orElse(0.0));
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Optimization Algorithm Comparison\n");
            sb.append("================================\n\n");

            Map<String, AlgorithmStats> stats = getStatistics();
            stats.forEach((name, stat) -> {
                sb.append(name).append(":\n");
                sb.append("Objective Value Statistics:\n");
                sb.append(stat.objectiveStats.toString("  "));
                sb.append("\nExecution Time Statistics (ms):\n");
                sb.append(stat.timeStats.toString("  "));
                sb.append("\nConvergence Rate Statistics:\n");
                sb.append(stat.convergenceStats.toString("  "));
                sb.append("\n\n");
            });

            return sb.toString();
        }
    }

    /**
     * Statistical data for algorithm performance metrics
     */
    public static class AlgorithmStats {

        public final StatsData objectiveStats;
        public final StatsData timeStats;
        public final StatsData convergenceStats;

        public AlgorithmStats(StatsData objectiveStats,
                StatsData timeStats,
                StatsData convergenceStats) {
            this.objectiveStats = objectiveStats;
            this.timeStats = timeStats;
            this.convergenceStats = convergenceStats;
        }
    }

    /**
     * Contains statistical measures for a set of values
     */
    public static class StatsData {

        public final double min;
        public final double max;
        public final double mean;
        public final double median;
        public final double stdDev;

        public StatsData(double min, double max, double mean,
                double median, double stdDev) {
            this.min = min;
            this.max = max;
            this.mean = mean;
            this.median = median;
            this.stdDev = stdDev;
        }

        public String toString(String indent) {
            return String.format("%sMin: %.2f\n%sMax: %.2f\n%sMean: %.2f\n%sMedian: %.2f\n%sStd Dev: %.2f",
                    indent, min,
                    indent, max,
                    indent, mean,
                    indent, median,
                    indent, stdDev);
        }
    }
}
