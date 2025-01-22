package jazarifresnelsim.optimization.algorithms;

import jazarifresnelsim.optimization.problem.FresnelDesignProblem;
import jazarifresnelsim.optimization.problem.DesignParameters;
import jazarifresnelsim.optimization.problem.DesignSolution;

import java.util.List;
import java.util.Map;

/**
 * Interface for optimization algorithms used in Fresnel system design
 * optimization. Each implementing class represents a different optimization
 * strategy.
 */
public interface IOptimizationAlgorithm extends Cloneable {

    /**
     * Executes the optimization algorithm to find the best design parameters.
     *
     * @param problem The Fresnel design optimization problem
     * @param initialParams Initial design parameters to start optimization from
     * @param constraints Map of constraint parameters (algorithm specific)
     * @return The best solution found by the algorithm
     */
    DesignSolution optimize(FresnelDesignProblem problem,
            DesignParameters initialParams,
            Map<String, Object> constraints);

    /**
     * Sets the algorithm-specific parameters.
     *
     * @param parameters Map of parameter name-value pairs
     */
    void setParameters(Map<String, Object> parameters);

    /**
     * Returns the optimization history for analysis.
     *
     * @return List of solutions evaluated during optimization
     */
    List<DesignSolution> getHistory();

    /**
     * Checks if the optimization process should be terminated.
     *
     * @return true if termination criteria are met
     */
    boolean isTerminationCriteriaMet();

    /**
     * Resets the algorithm's state for a new optimization run.
     */
    void reset();

    /**
     * Gets the name of the algorithm.
     *
     * @return Algorithm name
     */
    String getAlgorithmName();

    IOptimizationAlgorithm clone();

    Map<String, Object> getParameters();
}
