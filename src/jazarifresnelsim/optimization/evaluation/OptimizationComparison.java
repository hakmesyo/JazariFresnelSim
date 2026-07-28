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
 * Runs multiple optimization algorithms and compares their performance.
 *
 * Each algorithm is executed {@code numberOfRuns} times independently.
 * Statistics (best, mean, std-dev, convergence rate) are collected for
 * comparison — see manuscript Table 14–15 and Fig. 9.
 *
 * @author Yunus Demirtas, Musa Atas — Siirt University
 * @version 4.1
 */
public class OptimizationComparison {

    private final int           numberOfRuns;
    private final int           timeoutMinutes;
    private final DesignEvaluator evaluator;

    public OptimizationComparison(DesignEvaluator evaluator,
                                  int numberOfRuns,
                                  int timeoutMinutes) {
        this.evaluator      = evaluator;
        this.numberOfRuns   = numberOfRuns;
        this.timeoutMinutes = timeoutMinutes;
    }

    // ================================================================
    // PUBLIC API
    // ================================================================

    /**
     * Runs all algorithms and returns consolidated comparison results.
     */
    public ComparisonResult compareAlgorithms(
            List<IOptimizationAlgorithm> algorithms,
            FresnelDesignProblem problem,
            DesignParameters initialParams,
            Map<String, Object> constraints) {

        Map<String, List<AlgorithmRun>> allRuns = new HashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(algorithms.size(), Runtime.getRuntime().availableProcessors()));

        List<Future<AlgorithmResult>> futures = new ArrayList<>();

        for (IOptimizationAlgorithm algorithm : algorithms) {
            String name = algorithm.getAlgorithmName();
            allRuns.put(name, Collections.synchronizedList(new ArrayList<>()));

            for (int run = 0; run < numberOfRuns; run++) {
                futures.add(executor.submit(() -> {
                    try {
                        IOptimizationAlgorithm copy =
                                algorithm.getClass().getDeclaredConstructor().newInstance();
                        copy.setParameters(algorithm.getParameters());
                        AlgorithmResult result = runAlgorithm(copy, problem, initialParams, constraints);
                        allRuns.get(name).add(result.run);
                        return result;
                    } catch (Exception e) {
                        throw new RuntimeException("Run error: " + e.getMessage(), e);
                    }
                }));
            }
        }

        for (Future<AlgorithmResult> f : futures) {
            try { f.get(timeoutMinutes, TimeUnit.MINUTES); }
            catch (Exception e) { System.err.println("Algorithm run failed: " + e.getMessage()); }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutMinutes, TimeUnit.MINUTES))
                executor.shutdownNow();
        } catch (InterruptedException e) { executor.shutdownNow(); }

        return new ComparisonResult(allRuns);
    }

    // ================================================================
    // INTERNAL
    // ================================================================

    private AlgorithmResult runAlgorithm(IOptimizationAlgorithm algorithm,
                                          FresnelDesignProblem problem,
                                          DesignParameters initialParams,
                                          Map<String, Object> constraints) {
        algorithm.reset();
        Instant start    = Instant.now();
        DesignSolution sol = algorithm.optimize(problem, initialParams, constraints);
        Duration elapsed = Duration.between(start, Instant.now());
        return new AlgorithmResult(algorithm.getAlgorithmName(),
                new AlgorithmRun(sol, algorithm.getHistory(), elapsed));
    }

    private static class AlgorithmResult {
        final String       algorithmName;
        final AlgorithmRun run;
        AlgorithmResult(String name, AlgorithmRun run) {
            this.algorithmName = name;
            this.run = run;
        }
    }

    // ================================================================
    // DATA CLASSES
    // ================================================================

    /** Results from a single algorithm run. */
    public static class AlgorithmRun {
        private final DesignSolution       bestSolution;
        private final List<DesignSolution> optimizationHistory;
        private final Duration             runTime;

        public AlgorithmRun(DesignSolution bestSolution,
                            List<DesignSolution> history,
                            Duration runTime) {
            this.bestSolution        = bestSolution;
            this.optimizationHistory = new ArrayList<>(history);
            this.runTime             = runTime;
        }

        public DesignSolution       getBestSolution()        { return bestSolution; }
        public List<DesignSolution> getOptimizationHistory() { return new ArrayList<>(optimizationHistory); }
        public Duration             getRunTime()             { return runTime; }

        public List<Double> getConvergenceHistory() {
            return optimizationHistory.stream()
                    .map(DesignSolution::getObjectiveValue)
                    .toList();
        }
    }

    /** Aggregated results for all runs of all algorithms. */
    public static class ComparisonResult {
        private final Map<String, List<AlgorithmRun>> algorithmRuns;

        public ComparisonResult(Map<String, List<AlgorithmRun>> runs) {
            this.algorithmRuns = new HashMap<>(runs);
        }

        public Map<String, List<AlgorithmRun>> getAlgorithmRuns() {
            return new HashMap<>(algorithmRuns);
        }

        /** Returns statistical summary per algorithm. */
        public Map<String, AlgorithmStats> getStatistics() {
            Map<String, AlgorithmStats> stats = new HashMap<>();
            algorithmRuns.forEach((name, runs) -> {
                List<Double> objectives = new ArrayList<>();
                List<Long>   times      = new ArrayList<>();
                List<Double> convRates  = new ArrayList<>();
                for (AlgorithmRun run : runs) {
                    objectives.add(run.bestSolution.getObjectiveValue());
                    times.add(run.runTime.toMillis());
                    convRates.add(convergenceRate(run.getConvergenceHistory()));
                }
                stats.put(name, new AlgorithmStats(
                        statsOf(objectives),
                        statsOf(times.stream().mapToDouble(t -> t).boxed().toList()),
                        statsOf(convRates)));
            });
            return stats;
        }

        private double convergenceRate(List<Double> history) {
            if (history.size() < 2) return 0.0;
            double total = 0.0;
            for (int i = 1; i < history.size(); i++)
                total += Math.max(0, history.get(i) - history.get(i - 1));
            return total / (history.size() - 1);
        }

        private StatsData statsOf(List<Double> values) {
            DoubleSummaryStatistics s = values.stream()
                    .mapToDouble(Double::doubleValue).summaryStatistics();
            double mean   = s.getAverage();
            double median = median(values);
            double stdDev = Math.sqrt(values.stream()
                    .mapToDouble(v -> (v - mean) * (v - mean))
                    .average().orElse(0.0));
            return new StatsData(s.getMin(), s.getMax(), mean, median, stdDev);
        }

        private double median(List<Double> values) {
            List<Double> sorted = new ArrayList<>(values);
            Collections.sort(sorted);
            int mid = sorted.size() / 2;
            return sorted.size() % 2 == 0
                    ? (sorted.get(mid - 1) + sorted.get(mid)) / 2.0
                    : sorted.get(mid);
        }
    }

    /** Per-algorithm aggregate statistics. */
    public static class AlgorithmStats {
        public final StatsData objectiveStats;
        public final StatsData timeStats;
        public final StatsData convergenceStats;

        public AlgorithmStats(StatsData obj, StatsData time, StatsData conv) {
            this.objectiveStats   = obj;
            this.timeStats        = time;
            this.convergenceStats = conv;
        }
    }

    /** Descriptive statistics for a numeric series. */
    public static class StatsData {
        public final double min, max, mean, median, stdDev;

        public StatsData(double min, double max, double mean,
                         double median, double stdDev) {
            this.min    = min;
            this.max    = max;
            this.mean   = mean;
            this.median = median;
            this.stdDev = stdDev;
        }

        public String format(String indent) {
            return String.format(
                    "%sMin: %.2f%n%sMax: %.2f%n%sMean: %.2f%n%sMedian: %.2f%n%sStd Dev: %.2f",
                    indent, min, indent, max, indent, mean,
                    indent, median, indent, stdDev);
        }
    }
}