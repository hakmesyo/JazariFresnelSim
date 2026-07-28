package jazarifresnelsim.optimization.algorithms;

import jazarifresnelsim.optimization.problem.DesignParameters;
import jazarifresnelsim.optimization.problem.DesignSolution;
import jazarifresnelsim.optimization.problem.FresnelDesignProblem;

import java.util.*;

/**
 * Simulated Annealing for LFR optical design optimization.
 *
 * Search space (4 variables):
 *   H_r — receiver height [cm]
 *   w   — mirror width    [cm]
 *   p   — mirror spacing  [cm]
 *   N   — mirror count    (discrete ± 1 per step)
 *
 * D_r is fixed; see DesignParameters for rationale.
 * Hyperparameters match Table 13 of the manuscript.
 *
 * @author Yunus Demirtas, Musa Atas — Siirt University
 * @version 4.2
 */
public class SimulatedAnnealing implements IOptimizationAlgorithm {

    // Hyperparameters — Table 13
    private double initialTemperature = 1000.0;
    private double coolingRate        = 0.95;
    private int    maxIterations      = 1000;
    private double minTemperature     = 1e-8;

    // Perturbation constants
    private static final double MAX_PERTURBATION    = 0.10;  // 10 % Gaussian step
    private static final double CHANGE_PROBABILITY  = 0.50;  // per-parameter mutation prob.

    private List<DesignSolution> history;
    private Random               random;
    private int                  currentIteration;
    private double               currentTemperature;

    public SimulatedAnnealing() {
        this.random  = new Random();
        this.history = new ArrayList<>();
        reset();
    }

    // ================================================================
    // MAIN LOOP
    // ================================================================
    @Override
    public DesignSolution optimize(FresnelDesignProblem problem,
                                   DesignParameters     initialParams,
                                   Map<String, Object>  constraints) {
        DesignParameters current      = initialParams;
        double           currentEnergy= problem.evaluateDesign(current);
        DesignParameters best         = current;
        double           bestEnergy   = currentEnergy;

        while (!isTerminationCriteriaMet()) {
            DesignParameters neighbor     = neighbor(current);
            double           neighborE    = problem.evaluateDesign(neighbor);

            if (accept(currentEnergy, neighborE, currentTemperature)) {
                current      = neighbor;
                currentEnergy= neighborE;
                if (neighborE > bestEnergy) {
                    best       = neighbor;
                    bestEnergy = neighborE;
                }
            }

            history.add(new DesignSolution(best, bestEnergy));
            currentTemperature *= coolingRate;

            // Periodic reheat to escape local optima
            if (currentTemperature < minTemperature
                    && currentIteration < maxIterations / 2) {
                currentTemperature = initialTemperature * 0.5;
            }
            currentIteration++;
        }
        return new DesignSolution(best, bestEnergy);
    }

    // ================================================================
    // SA OPERATORS — 4-parameter space (no D_r)
    // ================================================================
    private DesignParameters neighbor(DesignParameters p) {
        double Hr = p.getReceiverHeight();
        double w  = p.getMirrorWidth();
        double sp = p.getMirrorSpacing();
        int    N  = p.getNumberOfMirrors();

        if (random.nextDouble() < CHANGE_PROBABILITY)
            Hr = clamp(Hr * (1 + random.nextGaussian() * MAX_PERTURBATION),
                    DesignParameters.MIN_RECEIVER_HEIGHT, DesignParameters.MAX_RECEIVER_HEIGHT);
        if (random.nextDouble() < CHANGE_PROBABILITY)
            w  = clamp(w  * (1 + random.nextGaussian() * MAX_PERTURBATION),
                    DesignParameters.MIN_MIRROR_WIDTH, DesignParameters.MAX_MIRROR_WIDTH);
        if (random.nextDouble() < CHANGE_PROBABILITY)
            sp = clamp(sp * (1 + random.nextGaussian() * MAX_PERTURBATION),
                    DesignParameters.MIN_MIRROR_SPACING, DesignParameters.MAX_MIRROR_SPACING);
        if (random.nextDouble() < CHANGE_PROBABILITY)
            N  = clampI(N + (random.nextBoolean() ? 1 : -1),
                    DesignParameters.MIN_NUMBER_OF_MIRRORS, DesignParameters.MAX_NUMBER_OF_MIRRORS);

        return new DesignParameters(Hr, w, sp, N);
    }

    private boolean accept(double current, double next, double temp) {
        if (next > current) return true;
        return random.nextDouble() < Math.exp(-(current - next) / temp);
    }

    // ================================================================
    // UTILITY
    // ================================================================
    private double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
    private int    clampI(int v, int min, int max)         { return Math.max(min, Math.min(max, v)); }

    // ================================================================
    // INTERFACE
    // ================================================================
    @Override public void setParameters(Map<String, Object> p) {
        if (p.containsKey("initialTemperature")) initialTemperature = (double) p.get("initialTemperature");
        if (p.containsKey("coolingRate"))        coolingRate        = (double) p.get("coolingRate");
        if (p.containsKey("maxIterations"))      maxIterations      = (int)    p.get("maxIterations");
        if (p.containsKey("minTemperature"))     minTemperature     = (double) p.get("minTemperature");
    }
    @Override public Map<String, Object> getParameters() {
        Map<String, Object> p = new HashMap<>();
        p.put("initialTemperature", initialTemperature);
        p.put("coolingRate",        coolingRate);
        p.put("maxIterations",      maxIterations);
        p.put("minTemperature",     minTemperature);
        return p;
    }
    @Override public List<DesignSolution>    getHistory()               { return new ArrayList<>(history); }
    @Override public boolean                 isTerminationCriteriaMet() {
        return currentIteration >= maxIterations
            || (currentTemperature < minTemperature && currentIteration > maxIterations / 2);
    }
    @Override public void reset() {
        currentIteration   = 0;
        currentTemperature = initialTemperature;
        history.clear();
    }
    @Override public String getAlgorithmName() { return "Simulated Annealing"; }
    @Override public IOptimizationAlgorithm clone() {
        try { return (IOptimizationAlgorithm) super.clone(); }
        catch (CloneNotSupportedException e) { throw new RuntimeException(e); }
    }
}