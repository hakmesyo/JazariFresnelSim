package jazarifresnelsim.optimization.algorithms;

import jazarifresnelsim.optimization.problem.DesignParameters;
import jazarifresnelsim.optimization.problem.DesignSolution;
import jazarifresnelsim.optimization.problem.FresnelDesignProblem;

import java.util.*;

/**
 * Particle Swarm Optimization for LFR optical design.
 *
 * Search space (4 variables — velocity vector size 4):
 *   [0] H_r — receiver height [cm]
 *   [1] w   — mirror width    [cm]
 *   [2] p   — mirror spacing  [cm]
 *   [3] N   — mirror count    (discrete, treated as continuous then rounded)
 *
 * D_r is fixed; see DesignParameters for rationale.
 * Hyperparameters match Table 13 of the manuscript.
 *
 * @author Yunus Demirtas, Musa Atas — Siirt University
 * @version 4.2
 */
public class ParticleSwarm implements IOptimizationAlgorithm {

    // Hyperparameters — Table 13 (Clerc's constriction coefficients)
    private int    swarmSize      = 30;
    private int    maxIterations  = 100;
    private double inertiaWeight  = 0.729;
    private double cognitiveWeight= 1.49445;
    private double socialWeight   = 1.49445;

    private static final int DIM = 4;  // H_r, w, p, N

    private List<DesignSolution> history;
    private Random               random;
    private int                  currentIteration;
    private List<Particle>       swarm;
    private DesignParameters     globalBest;
    private double               globalBestFitness;

    public ParticleSwarm() {
        this.random  = new Random();
        this.history = new ArrayList<>();
        reset();
    }

    // ================================================================
    // PARTICLE
    // ================================================================
    private class Particle {
        DesignParameters position;
        DesignParameters personalBest;
        double[]         velocity = new double[DIM];
        double           fitness             = Double.NEGATIVE_INFINITY;
        double           personalBestFitness = Double.NEGATIVE_INFINITY;

        Particle(DesignParameters pos) {
            this.position     = pos;
            this.personalBest = pos;
            // Random initial velocity — 10 % of range
            velocity[0] = rndVel(DesignParameters.MAX_RECEIVER_HEIGHT - DesignParameters.MIN_RECEIVER_HEIGHT);
            velocity[1] = rndVel(DesignParameters.MAX_MIRROR_WIDTH    - DesignParameters.MIN_MIRROR_WIDTH);
            velocity[2] = rndVel(DesignParameters.MAX_MIRROR_SPACING  - DesignParameters.MIN_MIRROR_SPACING);
            velocity[3] = rndVel(DesignParameters.MAX_NUMBER_OF_MIRRORS - DesignParameters.MIN_NUMBER_OF_MIRRORS);
        }
        private double rndVel(double range) { return (random.nextDouble() - 0.5) * range * 0.1; }
    }

    // ================================================================
    // MAIN LOOP
    // ================================================================
    @Override
    public DesignSolution optimize(FresnelDesignProblem problem,
                                   DesignParameters     initialParams,
                                   Map<String, Object>  constraints) {
        initSwarm(initialParams);

        while (!isTerminationCriteriaMet()) {
            for (Particle p : swarm) {
                p.fitness = problem.evaluateDesign(p.position);
                if (p.fitness > p.personalBestFitness) {
                    p.personalBest        = p.position;
                    p.personalBestFitness = p.fitness;
                    if (p.fitness > globalBestFitness) {
                        globalBest        = p.position;
                        globalBestFitness = p.fitness;
                    }
                }
            }
            updateSwarm();
            history.add(new DesignSolution(globalBest, globalBestFitness));
            currentIteration++;
        }
        return new DesignSolution(globalBest, globalBestFitness);
    }

    // ================================================================
    // SWARM OPERATIONS
    // ================================================================
    private void initSwarm(DesignParameters initial) {
        swarm = new ArrayList<>();
        globalBestFitness = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < swarmSize; i++)
            swarm.add(new Particle(randomParams()));
    }

    private void updateSwarm() {
        for (Particle p : swarm) {
            double[] cur  = toArray(p.position);
            double[] pb   = toArray(p.personalBest);
            double[] gb   = toArray(globalBest);

            for (int i = 0; i < DIM; i++) {
                double r1 = random.nextDouble();
                double r2 = random.nextDouble();
                p.velocity[i] = inertiaWeight   * p.velocity[i]
                              + cognitiveWeight  * r1 * (pb[i] - cur[i])
                              + socialWeight     * r2 * (gb[i] - cur[i]);
                p.velocity[i] = clampVel(p.velocity[i], i);
            }
            p.position = fromArray(cur, p.velocity);
        }
    }

    // ================================================================
    // CONVERSION HELPERS — 4-element array ↔ DesignParameters
    // ================================================================
    private double[] toArray(DesignParameters p) {
        return new double[]{
            p.getReceiverHeight(),
            p.getMirrorWidth(),
            p.getMirrorSpacing(),
            p.getNumberOfMirrors()
        };
    }

    private DesignParameters fromArray(double[] cur, double[] vel) {
        double Hr = clamp(cur[0] + vel[0],
                DesignParameters.MIN_RECEIVER_HEIGHT,  DesignParameters.MAX_RECEIVER_HEIGHT);
        double w  = clamp(cur[1] + vel[1],
                DesignParameters.MIN_MIRROR_WIDTH,     DesignParameters.MAX_MIRROR_WIDTH);
        double p  = clamp(cur[2] + vel[2],
                DesignParameters.MIN_MIRROR_SPACING,   DesignParameters.MAX_MIRROR_SPACING);
        int    N  = (int) clamp(cur[3] + vel[3],
                DesignParameters.MIN_NUMBER_OF_MIRRORS, DesignParameters.MAX_NUMBER_OF_MIRRORS);
        return new DesignParameters(Hr, w, p, N);
    }

    private double clampVel(double v, int idx) {
        double max = switch (idx) {
            case 0 -> (DesignParameters.MAX_RECEIVER_HEIGHT - DesignParameters.MIN_RECEIVER_HEIGHT) * 0.1;
            case 1 -> (DesignParameters.MAX_MIRROR_WIDTH    - DesignParameters.MIN_MIRROR_WIDTH)    * 0.1;
            case 2 -> (DesignParameters.MAX_MIRROR_SPACING  - DesignParameters.MIN_MIRROR_SPACING)  * 0.1;
            case 3 -> 2.0;
            default -> throw new IllegalArgumentException("Invalid index " + idx);
        };
        return Math.max(-max, Math.min(max, v));
    }

    private DesignParameters randomParams() {
        return new DesignParameters(
                rnd(DesignParameters.MIN_RECEIVER_HEIGHT,  DesignParameters.MAX_RECEIVER_HEIGHT),
                rnd(DesignParameters.MIN_MIRROR_WIDTH,     DesignParameters.MAX_MIRROR_WIDTH),
                rnd(DesignParameters.MIN_MIRROR_SPACING,   DesignParameters.MAX_MIRROR_SPACING),
                random.nextInt(DesignParameters.MAX_NUMBER_OF_MIRRORS
                             - DesignParameters.MIN_NUMBER_OF_MIRRORS + 1)
                             + DesignParameters.MIN_NUMBER_OF_MIRRORS);
    }

    private double rnd(double min, double max)         { return min + random.nextDouble() * (max - min); }
    private double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }

    // ================================================================
    // INTERFACE
    // ================================================================
    @Override public void setParameters(Map<String, Object> p) {
        if (p.containsKey("swarmSize"))       swarmSize       = (int)    p.get("swarmSize");
        if (p.containsKey("maxIterations"))   maxIterations   = (int)    p.get("maxIterations");
        if (p.containsKey("inertiaWeight"))   inertiaWeight   = (double) p.get("inertiaWeight");
        if (p.containsKey("cognitiveWeight")) cognitiveWeight = (double) p.get("cognitiveWeight");
        if (p.containsKey("socialWeight"))    socialWeight    = (double) p.get("socialWeight");
    }
    @Override public Map<String, Object> getParameters() {
        Map<String, Object> p = new HashMap<>();
        p.put("swarmSize",       swarmSize);
        p.put("maxIterations",   maxIterations);
        p.put("inertiaWeight",   inertiaWeight);
        p.put("cognitiveWeight", cognitiveWeight);
        p.put("socialWeight",    socialWeight);
        return p;
    }
    @Override public List<DesignSolution>    getHistory()               { return new ArrayList<>(history); }
    @Override public boolean                 isTerminationCriteriaMet() { return currentIteration >= maxIterations; }
    @Override public void reset() {
        currentIteration  = 0;
        swarm             = new ArrayList<>();
        globalBest        = null;
        globalBestFitness = Double.NEGATIVE_INFINITY;
        history.clear();
    }
    @Override public String getAlgorithmName() { return "Particle Swarm Optimization"; }
    @Override public IOptimizationAlgorithm clone() {
        try { return (IOptimizationAlgorithm) super.clone(); }
        catch (CloneNotSupportedException e) { throw new RuntimeException(e); }
    }
}