package jazarifresnelsim.optimization.evaluation;

import jazarifresnelsim.optimization.problem.FresnelDesignProblem;
import jazarifresnelsim.optimization.problem.DesignParameters;
import jazarifresnelsim.optimization.problem.DesignSolution;
import jazarifresnelsim.models.SolarPosition;
import jazarifresnelsim.domain.SolarCalculator;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Evaluates Fresnel system designs using multiple performance metrics
 */
public class DesignEvaluator {
    // Performance metric keys

    public static final String TOTAL_ENERGY = "totalEnergy";              // kW
    public static final String MIRROR_AREA_EFFICIENCY = "mirrorEfficiency"; // kW/m² mirror 
    public static final String LAND_AREA_EFFICIENCY = "landEfficiency";     // kW/m² land
    public static final String COST_EFFECTIVENESS = "costEffectiveness";    // kW/$

    // Cost parameters for economic analysis
    private static final double MIRROR_COST_PER_M2 = 100.0;   // USD/m²
    private static final double RECEIVER_COST_PER_M = 500.0;   // USD/m
    private static final double SUPPORT_STRUCTURE_RATIO = 0.3; // 30% of mirror cost
    private static final double INSTALLATION_FACTOR = 1.2;     // 20% installation cost

    private final FresnelDesignProblem problem;
    private final List<LocalDateTime> evaluationTimes;

    public DesignEvaluator(FresnelDesignProblem problem, List<LocalDateTime> evaluationTimes) {
        this.problem = problem;
        this.evaluationTimes = new ArrayList<>(evaluationTimes);
    }

    /**
     * Evaluates design by calculating multiple performance metrics
     */
    public Map<String, Double> evaluateDesign(DesignParameters params) {
        Map<String, Double> metrics = new HashMap<>();

        // Calculate base energy output
        double totalEnergy = problem.evaluateDesign(params);

        // Calculate areas
        double mirrorArea = calculateMirrorArea(params);
        double landArea = calculateLandArea(params);

        // Calculate system cost
        double systemCost = calculateSystemCost(params);

        // Store all metrics with better format handling
        metrics.put(TOTAL_ENERGY, totalEnergy);
        metrics.put(MIRROR_AREA_EFFICIENCY, mirrorArea > 0 ? totalEnergy / mirrorArea : 0);
        metrics.put(LAND_AREA_EFFICIENCY, landArea > 0 ? totalEnergy / landArea : 0);
        metrics.put(COST_EFFECTIVENESS, systemCost > 0 ? (totalEnergy * 1000) / systemCost : 0); // Convert kW to W for better scale
        return metrics;
    }

    private double calculateMirrorArea(DesignParameters params) {
        return (params.getMirrorWidth() * params.getMirrorLength()
                * params.getNumberOfMirrors()) / 10000.0;  // cm² to m²
    }

    private double calculateLandArea(DesignParameters params) {
        double totalWidth = (params.getNumberOfMirrors() - 1) * params.getMirrorSpacing()
                + params.getMirrorWidth();
        return (totalWidth * params.getMirrorLength() * 1.2) / 10000.0;  // cm² to m²
    }

    /**
     * Creates detailed evaluation report for design solution
     */
    public EvaluationReport createReport(DesignSolution solution) {
        Map<String, Double> metrics = evaluateDesign(solution.getParameters());

        // Create hourly performance data
        Map<LocalDateTime, Double> hourlyPerformance = new HashMap<>();
        for (LocalDateTime time : evaluationTimes) {
            double energy = problem.evaluateDesign(solution.getParameters());
            hourlyPerformance.put(time, energy);
        }

        return new EvaluationReport(
                solution,
                metrics,
                hourlyPerformance,
                calculateSystemCost(solution.getParameters()),
                calculateLandArea(solution.getParameters())
        );
    }

    private double calculateSystemCost(DesignParameters params) {
        // Calculate mirror cost
        double mirrorArea = calculateMirrorArea(params);
        double mirrorCost = mirrorArea * MIRROR_COST_PER_M2;

        // Calculate receiver cost
        double receiverLength = params.getMirrorLength() / 100.0; // cm to m
        double receiverCost = receiverLength * RECEIVER_COST_PER_M;

        // Calculate support structure cost
        double supportCost = mirrorCost * SUPPORT_STRUCTURE_RATIO;

        // Calculate total cost
        double totalCost = (mirrorCost + receiverCost + supportCost) * INSTALLATION_FACTOR;

//        // Debug prints to trace the calculation
//        System.out.println("Cost Breakdown:");
//        System.out.println("Mirror Area: " + mirrorArea + " m²");
//        System.out.println("Mirror Cost: $" + mirrorCost);
//        System.out.println("Receiver Cost: $" + receiverCost);
//        System.out.println("Support Cost: $" + supportCost);
//        System.out.println("Total Cost: $" + totalCost);


        return Math.max(totalCost, 1.0); // Prevent division by zero
    }

    /**
     * Represents comprehensive evaluation report for design solution
     */
    public static class EvaluationReport {

        private final DesignSolution solution;
        private final Map<String, Double> metrics;
        private final Map<LocalDateTime, Double> hourlyPerformance;
        private final double systemCost;
        private final double landUse;

        public EvaluationReport(DesignSolution solution,
                Map<String, Double> metrics,
                Map<LocalDateTime, Double> hourlyPerformance,
                double systemCost,
                double landUse) {
            this.solution = solution;
            this.metrics = new HashMap<>(metrics);
            this.hourlyPerformance = new HashMap<>(hourlyPerformance);
            this.systemCost = systemCost;
            this.landUse = landUse;
        }

        public DesignSolution getSolution() {
            return solution;
        }

        public Map<String, Double> getMetrics() {
            return new HashMap<>(metrics);
        }

        public Map<LocalDateTime, Double> getHourlyPerformance() {
            return new HashMap<>(hourlyPerformance);
        }

        public double getSystemCost() {
            return systemCost;
        }

        public double getLandUse() {
            return landUse;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Design Evaluation Report\n");
            sb.append("=======================\n\n");

            sb.append("Design Parameters:\n");
            sb.append(solution.getParameters().toString()).append("\n\n");

            sb.append("Performance Metrics:\n");
            metrics.forEach((key, value)
                    -> sb.append(String.format("%-20s: %.2f\n", key, value)));

            sb.append(String.format("\nSystem Cost: $%.2f\n", systemCost));
            sb.append(String.format("Land Use: %.2f m²\n", landUse));

            return sb.toString();
        }
    }
}
