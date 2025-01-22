package jazarifresnelsim.optimization.problem;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a solution in the Fresnel system design optimization problem.
 * Contains both the design parameters and their evaluated performance metrics.
 */
public class DesignSolution implements Comparable<DesignSolution> {
    private final DesignParameters parameters;
    private final double objectiveValue;  // Total energy output (fitness value)
    private final Map<LocalDateTime, Double> energyByTime;  // Energy output at specific times
    private final Map<String, Double> performanceMetrics;  // Additional performance metrics
    
    /**
     * Creates a new design solution with the given parameters and objective value
     * 
     * @param parameters The design parameters
     * @param objectiveValue The evaluated objective value (total energy output)
     */
    public DesignSolution(DesignParameters parameters, double objectiveValue) {
        this.parameters = parameters;
        this.objectiveValue = objectiveValue;
        this.energyByTime = new HashMap<>();
        this.performanceMetrics = new HashMap<>();
    }
    
    /**
     * Creates a new design solution with detailed performance data
     * 
     * @param parameters The design parameters
     * @param objectiveValue The evaluated objective value
     * @param energyByTime Map of energy output at specific times
     * @param metrics Additional performance metrics
     */
    public DesignSolution(DesignParameters parameters, 
                         double objectiveValue,
                         Map<LocalDateTime, Double> energyByTime,
                         Map<String, Double> metrics) {
        this.parameters = parameters;
        this.objectiveValue = objectiveValue;
        this.energyByTime = new HashMap<>(energyByTime);
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
     * Gets the total energy output (objective value)
     * 
     * @return The objective value
     */
    public double getObjectiveValue() {
        return objectiveValue;
    }
    
    /**
     * Gets the energy output at a specific time
     * 
     * @param time The time to get energy output for
     * @return The energy output at the specified time, or 0 if not available
     */
    public double getEnergyAt(LocalDateTime time) {
        return energyByTime.getOrDefault(time, 0.0);
    }
    
    /**
     * Gets all energy outputs by time
     * 
     * @return Map of time to energy output
     */
    public Map<LocalDateTime, Double> getEnergyByTime() {
        return new HashMap<>(energyByTime);
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
     * Sets the energy output for a specific time
     * 
     * @param time The time point
     * @param energy The energy output value
     */
    public void setEnergyForTime(LocalDateTime time, double energy) {
        energyByTime.put(time, energy);
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
    
    @Override
    public int compareTo(DesignSolution other) {
        // For maximization problem, higher objective values are better
        return Double.compare(this.objectiveValue, other.objectiveValue);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Design Solution:\n");
        sb.append("Objective Value: ").append(String.format("%.2f kW\n", objectiveValue));
        sb.append("Design Parameters:\n").append(parameters.toString()).append("\n");
        
        if (!performanceMetrics.isEmpty()) {
            sb.append("Performance Metrics:\n");
            performanceMetrics.forEach((metric, value) -> 
                sb.append(String.format("  %s: %.2f\n", metric, value)));
        }
        
        if (!energyByTime.isEmpty()) {
            sb.append("Energy Output by Time:\n");
            energyByTime.forEach((time, energy) -> 
                sb.append(String.format("  %s: %.2f kW\n", time, energy)));
        }
        
        return sb.toString();
    }
}