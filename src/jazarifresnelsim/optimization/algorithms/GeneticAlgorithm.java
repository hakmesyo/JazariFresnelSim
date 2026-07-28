package jazarifresnelsim.optimization.algorithms;

import jazarifresnelsim.optimization.problem.DesignParameters;
import jazarifresnelsim.optimization.problem.DesignSolution;
import jazarifresnelsim.optimization.problem.FresnelDesignProblem;

import java.util.*;

/**
 * Genetic Algorithm for LFR optical design optimization.
 *
 * Search space (4 variables):
 *   H_r ∈ [MIN_RECEIVER_HEIGHT,  MAX_RECEIVER_HEIGHT]  — receiver height [cm]
 *   w   ∈ [MIN_MIRROR_WIDTH,     MAX_MIRROR_WIDTH]     — mirror width    [cm]
 *   p   ∈ [MIN_MIRROR_SPACING,   MAX_MIRROR_SPACING]   — mirror spacing  [cm]
 *   N   ∈ [MIN_NUMBER_OF_MIRRORS,MAX_NUMBER_OF_MIRRORS] — mirror count
 *
 * D_r (receiver diameter) is fixed and not part of the search space.
 * See DesignParameters for the full rationale.
 *
 * Hyperparameters match Table 13 of the manuscript.
 *
 * @author Yunus Demirtas, Musa Atas — Siirt University
 * @version 4.2
 */
public class GeneticAlgorithm implements IOptimizationAlgorithm {

    // Hyperparameters — Table 13
    private int    populationSize = 50;
    private int    maxGenerations = 100;
    private double crossoverRate  = 0.8;
    private double mutationRate   = 0.1;
    private double elitismRate    = 0.1;

    private List<DesignSolution> history;
    private Random               random;
    private int                  currentGeneration;
    private List<DesignSolution> population;
    private DesignSolution       bestSolution;

    public GeneticAlgorithm() {
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
        initPopulation(initialParams, problem);

        while (!isTerminationCriteriaMet()) {
            population.sort((a, b) ->
                    Double.compare(b.getObjectiveValue(), a.getObjectiveValue()));
            updateBest();

            List<DesignSolution> next = new ArrayList<>();

            // Elitism
            int eliteCount = (int)(populationSize * elitismRate);
            for (int i = 0; i < eliteCount; i++) next.add(population.get(i));

            // Crossover + mutation
            while (next.size() < populationSize) {
                DesignParameters p1 = selectParent().getParameters();
                DesignParameters p2 = selectParent().getParameters();
                DesignParameters child = (random.nextDouble() < crossoverRate)
                        ? crossover(p1, p2)
                        : (random.nextBoolean() ? p1 : p2);
                if (random.nextDouble() < mutationRate) child = mutate(child);
                next.add(new DesignSolution(child, problem.evaluateDesign(child)));
            }

            population = next;
            currentGeneration++;
            history.add(bestSolution);
        }
        return bestSolution;
    }

    // ================================================================
    // POPULATION OPERATIONS
    // ================================================================
    private void initPopulation(DesignParameters initial,
                                 FresnelDesignProblem problem) {
        population = new ArrayList<>();
        population.add(new DesignSolution(initial,
                problem.evaluateDesign(initial)));
        while (population.size() < populationSize) {
            DesignParameters p = randomParams();
            population.add(new DesignSolution(p, problem.evaluateDesign(p)));
        }
    }

    private void updateBest() {
        DesignSolution top = population.get(0);
        if (bestSolution == null
                || top.getObjectiveValue() > bestSolution.getObjectiveValue())
            bestSolution = top;
    }

    private DesignSolution selectParent() {
        // Tournament selection (size 3)
        DesignSolution best = null;
        for (int i = 0; i < 3; i++) {
            DesignSolution c = population.get(random.nextInt(population.size()));
            if (best == null || c.getObjectiveValue() > best.getObjectiveValue())
                best = c;
        }
        return best;
    }

    // ================================================================
    // GENETIC OPERATORS — 4-parameter space (no D_r)
    // ================================================================
    private DesignParameters randomParams() {
        return new DesignParameters(
                rnd(DesignParameters.MIN_RECEIVER_HEIGHT,  DesignParameters.MAX_RECEIVER_HEIGHT),
                rnd(DesignParameters.MIN_MIRROR_WIDTH,     DesignParameters.MAX_MIRROR_WIDTH),
                rnd(DesignParameters.MIN_MIRROR_SPACING,   DesignParameters.MAX_MIRROR_SPACING),
                random.nextInt(DesignParameters.MAX_NUMBER_OF_MIRRORS
                             - DesignParameters.MIN_NUMBER_OF_MIRRORS + 1)
                             + DesignParameters.MIN_NUMBER_OF_MIRRORS);
    }

    private DesignParameters crossover(DesignParameters p1, DesignParameters p2) {
        double a = random.nextDouble();
        return new DesignParameters(
                lerp(p1.getReceiverHeight(), p2.getReceiverHeight(), a),
                lerp(p1.getMirrorWidth(),    p2.getMirrorWidth(),    a),
                lerp(p1.getMirrorSpacing(),  p2.getMirrorSpacing(),  a),
                random.nextBoolean() ? p1.getNumberOfMirrors()
                                     : p2.getNumberOfMirrors());
    }

    private DesignParameters mutate(DesignParameters p) {
        int newN = p.getNumberOfMirrors();
        if (random.nextDouble() < 0.2)
            newN = clampI(newN + (random.nextBoolean() ? 1 : -1),
                    DesignParameters.MIN_NUMBER_OF_MIRRORS,
                    DesignParameters.MAX_NUMBER_OF_MIRRORS);
        return new DesignParameters(
                perturb(p.getReceiverHeight(), DesignParameters.MIN_RECEIVER_HEIGHT, DesignParameters.MAX_RECEIVER_HEIGHT),
                perturb(p.getMirrorWidth(),    DesignParameters.MIN_MIRROR_WIDTH,    DesignParameters.MAX_MIRROR_WIDTH),
                perturb(p.getMirrorSpacing(),  DesignParameters.MIN_MIRROR_SPACING,  DesignParameters.MAX_MIRROR_SPACING),
                newN);
    }

    // ================================================================
    // UTILITY
    // ================================================================
    private double rnd(double min, double max)  { return min + random.nextDouble() * (max - min); }
    private double lerp(double a, double b, double t) { return a * t + b * (1 - t); }
    private double perturb(double v, double min, double max) {
        return clamp(v * (1 + random.nextGaussian() * 0.1), min, max);
    }
    private double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
    private int    clampI(int v, int min, int max)         { return Math.max(min, Math.min(max, v)); }

    // ================================================================
    // INTERFACE
    // ================================================================
    @Override public void   setParameters(Map<String, Object> p) {
        if (p.containsKey("populationSize")) populationSize = (int)    p.get("populationSize");
        if (p.containsKey("maxGenerations")) maxGenerations = (int)    p.get("maxGenerations");
        if (p.containsKey("crossoverRate"))  crossoverRate  = (double) p.get("crossoverRate");
        if (p.containsKey("mutationRate"))   mutationRate   = (double) p.get("mutationRate");
        if (p.containsKey("elitismRate"))    elitismRate    = (double) p.get("elitismRate");
    }
    @Override public Map<String, Object> getParameters() {
        Map<String, Object> p = new HashMap<>();
        p.put("populationSize", populationSize);
        p.put("maxGenerations", maxGenerations);
        p.put("crossoverRate",  crossoverRate);
        p.put("mutationRate",   mutationRate);
        p.put("elitismRate",    elitismRate);
        return p;
    }
    @Override public List<DesignSolution>    getHistory()               { return new ArrayList<>(history); }
    @Override public boolean                 isTerminationCriteriaMet() { return currentGeneration >= maxGenerations; }
    @Override public void reset() {
        currentGeneration = 0;
        population = new ArrayList<>();
        bestSolution = null;
        history.clear();
    }
    @Override public String getAlgorithmName() { return "Genetic Algorithm"; }
    @Override public IOptimizationAlgorithm clone() {
        try { return (IOptimizationAlgorithm) super.clone(); }
        catch (CloneNotSupportedException e) { throw new RuntimeException(e); }
    }
}