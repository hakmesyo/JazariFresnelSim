package jazarifresnelsim.optimization.problem;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a solution in the Fresnel system design optimization problem.
 * Contains both the design parameters and their evaluated performance metrics.
 *
 * VERSION 3.0 - OPTICAL CORE
 * The objectiveValue now represents the instantaneous or annual optical intercept
 * power (W or Wh), not thermal energy. This serves as a relative ranking metric
 * for design optimization, consistent with the manuscript scope.
 *
 * For thermal conversion, users should post-process the optical output.
 */
public class DesignSolution implements Comparable<DesignSolution> {
    private final DesignParameters parameters;
    private final double objectiveValue; // Optical intercept power (fitness value) [W]
    private final Map<LocalDateTime, Double> opticalPowerByTime; // Optical power at specific times [W]
    private final Map<String, Double> performanceMetrics; // Additional performance metrics

    /**
     * Creates a new design solution with the given parameters and objective value
     *
     * @param parameters The design parameters
     * @param objectiveValue The evaluated objective value (optical intercept power) [W]
     */
    public DesignSolution(DesignParameters parameters, double objectiveValue) {
        this.parameters = parameters;
        this.objectiveValue = objectiveValue;
        this.opticalPowerByTime = new HashMap<>();
        this.performanceMetrics = new HashMap<>();
    }

    /**
     * Creates a new design solution with detailed performance data
     *
     * @param parameters The design parameters
     * @param objectiveValue The evaluated objective value (optical power) [W]
     * @param opticalPowerByTime Map of optical power at specific times [W]
     * @param metrics Additional performance metrics
     */
    public DesignSolution(DesignParameters parameters,
                         double objectiveValue,
                         Map<LocalDateTime, Double> opticalPowerByTime,
                         Map<String, Double> metrics) {
        this.parameters = parameters;
        this.objectiveValue = objectiveValue;
        this.opticalPowerByTime = new HashMap<>(opticalPowerByTime);
        this.performanceMetrics = new HashMap<>(metrics);
    }

    /**
     * Gets the design parameters of this solution
     *
     * @return The design parameters
     */
    public DesignParameters getParameters() {
        return parameters;
    }

    /**
     * Gets the total optical intercept power (objective value)
     *
     * @return The objective value [W]
     */
    public double getObjectiveValue() {
        return objectiveValue;
    }

    /**
     * Gets the optical intercept power at a specific time
     *
     * @param time The time to get optical power for
     * @return The optical power at the specified time [W], or 0 if not available
     */
    public double getOpticalPowerAt(LocalDateTime time) {
        return opticalPowerByTime.getOrDefault(time, 0.0);
    }

    /**
     * Gets all optical power outputs by time
     *
     * @return Map of time to optical power [W]
     */
    public Map<LocalDateTime, Double> getOpticalPowerByTime() {
        return new HashMap<>(opticalPowerByTime);
    }

    /**
     * Gets the value of a specific performance metric
     *
     * @param metricName The name of the metric
     * @return The metric value, or 0 if not available
     */
    public double getMetric(String metricName) {
        return performanceMetrics.getOrDefault(metricName, 0.0);
    }

    /**
     * Gets all performance metrics
     *
     * @return Map of metric names to values
     */
    public Map<String, Double> getPerformanceMetrics() {
        return new HashMap<>(performanceMetrics);
    }

    /**
     * Sets the optical intercept power for a specific time
     *
     * @param time The time point
     * @param power The optical power value [W]
     */
    public void setOpticalPowerForTime(LocalDateTime time, double power) {
        opticalPowerByTime.put(time, power);
    }

    /**
     * Sets a performance metric value
     *
     * @param metricName The name of the metric
     * @param value The metric value
     */
    public void setMetric(String metricName, double value) {
        performanceMetrics.put(metricName, value);
    }

    // --- DEPRECATED METHODS FOR BACKWARD COMPATIBILITY ---
    @Deprecated
    public double getEnergyAt(LocalDateTime time) {
        return getOpticalPowerAt(time);
    }

    @Deprecated
    public Map<LocalDateTime, Double> getEnergyByTime() {
        return getOpticalPowerByTime();
    }

    @Deprecated
    public void setEnergyForTime(LocalDateTime time, double energy) {
        setOpticalPowerForTime(time, energy);
    }

    @Override
    public int compareTo(DesignSolution other) {
        // For maximization problem, higher objective values are better
        return Double.compare(this.objectiveValue, other.objectiveValue);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Design Solution:\n");
        sb.append("Objective: ").append(String.format("%.2f W_opt\n", objectiveValue));
        sb.append("Note: Relative optical intercept metric for ranking.\n");
        sb.append("Design Parameters:\n").append(parameters.toString()).append("\n");

        if (!performanceMetrics.isEmpty()) {
            sb.append("Performance Metrics:\n");
            performanceMetrics.forEach((metric, value) ->
                sb.append(String.format(" %s: %.2f\n", metric, value)));
        }

        if (!opticalPowerByTime.isEmpty()) {
            sb.append("Optical Intercept Power by Time:\n");
            opticalPowerByTime.forEach((time, power) ->
                sb.append(String.format(" %s: %.2f W\n", time, power)));
        }

        return sb.toString();
    }
}