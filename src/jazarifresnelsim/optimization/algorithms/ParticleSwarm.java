package jazarifresnelsim.optimization.algorithms;

import jazarifresnelsim.optimization.problem.FresnelDesignProblem;
import jazarifresnelsim.optimization.problem.DesignParameters;
import jazarifresnelsim.optimization.problem.DesignSolution;

import java.util.*;

public class ParticleSwarm implements IOptimizationAlgorithm {

    // PSO parameters
    private int swarmSize = 30;
    private int maxIterations = 100;
    private double inertiaWeight = 0.729; // Clerc's constriction coefficient
    private double cognitiveWeight = 1.49445; // Personal best weight
    private double socialWeight = 1.49445;   // Global best weight

    @Override
    public IOptimizationAlgorithm clone() {
        try {
            return (IOptimizationAlgorithm) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("swarmSize", swarmSize);
        params.put("maxIterations", maxIterations);
        params.put("inertiaWeight", inertiaWeight);
        params.put("cognitiveWeight", cognitiveWeight);
        params.put("socialWeight", socialWeight);
        return params;
    }

    private class Particle {

        DesignParameters position;
        DesignParameters personalBest;
        double[] velocity;
        double fitness;
        double personalBestFitness;

        Particle(DesignParameters position, double[] velocity) {
            this.position = position;
            this.velocity = velocity;
            this.personalBest = position;
            this.fitness = Double.NEGATIVE_INFINITY;
            this.personalBestFitness = Double.NEGATIVE_INFINITY;
        }
    }

    private List<DesignSolution> history;
    private Random random;
    private int currentIteration;
    private List<Particle> swarm;
    private DesignParameters globalBest;
    private double globalBestFitness;

    public ParticleSwarm() {
        this.random = new Random();
        this.history = new ArrayList<>();
        reset();
    }

    @Override
    public DesignSolution optimize(FresnelDesignProblem problem,
            DesignParameters initialParams,
            Map<String, Object> constraints) {
        initializeSwarm(initialParams);

        while (!isTerminationCriteriaMet()) {
            // Evaluate current swarm
            for (Particle particle : swarm) {
                particle.fitness = problem.evaluateDesign(particle.position);

                // Update personal best
                if (particle.fitness > particle.personalBestFitness) {
                    particle.personalBest = particle.position;
                    particle.personalBestFitness = particle.fitness;

                    // Update global best
                    if (particle.fitness > globalBestFitness) {
                        globalBest = particle.position;
                        globalBestFitness = particle.fitness;
                    }
                }
            }

            // Update particle velocities and positions
            updateSwarm();

            // Record history
            history.add(new DesignSolution(globalBest, globalBestFitness));
            currentIteration++;
        }

        return new DesignSolution(globalBest, globalBestFitness);
    }

    private void initializeSwarm(DesignParameters initial) {
        swarm = new ArrayList<>();
        globalBestFitness = Double.NEGATIVE_INFINITY;

        // Initialize each particle
        for (int i = 0; i < swarmSize; i++) {
            // Generate random position around initial solution
            DesignParameters position = generateRandomPosition(initial);
            double[] velocity = generateRandomVelocity();

            swarm.add(new Particle(position, velocity));
        }
    }

    private DesignParameters generateRandomPosition(DesignParameters base) {
        // Generate random position with Gaussian perturbation around base
        double receiverHeight = generateRandomParameter(
                DesignParameters.MIN_RECEIVER_HEIGHT,
                DesignParameters.MAX_RECEIVER_HEIGHT);
        double receiverDiameter = generateRandomParameter(
                DesignParameters.MIN_RECEIVER_DIAMETER,
                DesignParameters.MAX_RECEIVER_DIAMETER);
        double mirrorWidth = generateRandomParameter(
                DesignParameters.MIN_MIRROR_WIDTH,
                DesignParameters.MAX_MIRROR_WIDTH);
//        double mirrorLength = generateRandomParameter(
//                DesignParameters.MIN_MIRROR_LENGTH,
//                DesignParameters.MAX_MIRROR_LENGTH);
        double mirrorSpacing = generateRandomParameter(
                DesignParameters.MIN_MIRROR_SPACING,
                DesignParameters.MAX_MIRROR_SPACING);
        int numberOfMirrors = random.nextInt(
                DesignParameters.MAX_NUMBER_OF_MIRRORS
                - DesignParameters.MIN_NUMBER_OF_MIRRORS + 1)
                + DesignParameters.MIN_NUMBER_OF_MIRRORS;

        return new DesignParameters(
                receiverHeight, 
                receiverDiameter,
                mirrorWidth, 
                //mirrorLength,
                mirrorSpacing, 
                numberOfMirrors);
    }

    private double generateRandomParameter(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private double[] generateRandomVelocity() {
        // Initialize velocities as fraction of parameter ranges
        double[] velocity = new double[5];
        velocity[0] = (random.nextDouble() - 0.5)
                * (DesignParameters.MAX_RECEIVER_HEIGHT - DesignParameters.MIN_RECEIVER_HEIGHT) * 0.1;
        velocity[1] = (random.nextDouble() - 0.5)
                * (DesignParameters.MAX_RECEIVER_DIAMETER - DesignParameters.MIN_RECEIVER_DIAMETER) * 0.1;
        velocity[2] = (random.nextDouble() - 0.5)
                * (DesignParameters.MAX_MIRROR_WIDTH - DesignParameters.MIN_MIRROR_WIDTH) * 0.1;
//        velocity[3] = (random.nextDouble() - 0.5)
//                * (DesignParameters.MAX_MIRROR_LENGTH - DesignParameters.MIN_MIRROR_LENGTH) * 0.1;
        velocity[3] = (random.nextDouble() - 0.5)
                * (DesignParameters.MAX_MIRROR_SPACING - DesignParameters.MIN_MIRROR_SPACING) * 0.1;
        velocity[4] = (random.nextDouble() - 0.5) * 2; // For discrete number of mirrors

        return velocity;
    }

    private void updateSwarm() {
        for (Particle particle : swarm) {
            // Update velocity
            for (int i = 0; i < particle.velocity.length; i++) {
                double r1 = random.nextDouble();
                double r2 = random.nextDouble();

                // Get current parameter values
                double currentPos = getParameterValue(particle.position, i);
                double personalBestPos = getParameterValue(particle.personalBest, i);
                double globalBestPos = getParameterValue(globalBest, i);

                // Update velocity using standard PSO formula
                particle.velocity[i] = inertiaWeight * particle.velocity[i]
                        + cognitiveWeight * r1 * (personalBestPos - currentPos)
                        + socialWeight * r2 * (globalBestPos - currentPos);

                // Apply velocity clamping
                particle.velocity[i] = clampVelocity(particle.velocity[i], i);
            }

            // Update position
            particle.position = updatePosition(particle.position, particle.velocity);
        }
    }

    private double getParameterValue(DesignParameters params, int index) {
        return switch (index) {
            case 0 ->
                params.getReceiverHeight();
            case 1 ->
                params.getReceiverDiameter();
            case 2 ->
                params.getMirrorWidth();
//            case 3 ->
//                params.getMirrorLength();
            case 3 ->
                params.getMirrorSpacing();
            case 4 ->
                params.getNumberOfMirrors();
            default ->
                throw new IllegalArgumentException("Invalid parameter index");
        };
    }

    private double clampVelocity(double velocity, int paramIndex) {
        double maxVelocity = switch (paramIndex) {
            case 0 ->
                (DesignParameters.MAX_RECEIVER_HEIGHT - DesignParameters.MIN_RECEIVER_HEIGHT) * 0.1;
            case 1 ->
                (DesignParameters.MAX_RECEIVER_DIAMETER - DesignParameters.MIN_RECEIVER_DIAMETER) * 0.1;
            case 2 ->
                (DesignParameters.MAX_MIRROR_WIDTH - DesignParameters.MIN_MIRROR_WIDTH) * 0.1;
//            case 3 ->
//                (DesignParameters.MAX_MIRROR_LENGTH - DesignParameters.MIN_MIRROR_LENGTH) * 0.1;
            case 3 ->
                (DesignParameters.MAX_MIRROR_SPACING - DesignParameters.MIN_MIRROR_SPACING) * 0.1;
            case 4 ->
                2.0; // For discrete number of mirrors
            default ->
                throw new IllegalArgumentException("Invalid parameter index");
        };

        return Math.max(-maxVelocity, Math.min(maxVelocity, velocity));
    }

    private DesignParameters updatePosition(DesignParameters current, double[] velocity) {
        double newReceiverHeight = clampParameter(
                current.getReceiverHeight() + velocity[0],
                DesignParameters.MIN_RECEIVER_HEIGHT,
                DesignParameters.MAX_RECEIVER_HEIGHT);

        double newReceiverDiameter = clampParameter(
                current.getReceiverDiameter() + velocity[1],
                DesignParameters.MIN_RECEIVER_DIAMETER,
                DesignParameters.MAX_RECEIVER_DIAMETER);

        double newMirrorWidth = clampParameter(
                current.getMirrorWidth() + velocity[2],
                DesignParameters.MIN_MIRROR_WIDTH,
                DesignParameters.MAX_MIRROR_WIDTH);

//        double newMirrorLength = clampParameter(
//                current.getMirrorLength() + velocity[3],
//                DesignParameters.MIN_MIRROR_LENGTH,
//                DesignParameters.MAX_MIRROR_LENGTH);

        double newMirrorSpacing = clampParameter(
                current.getMirrorSpacing() + velocity[3],
                DesignParameters.MIN_MIRROR_SPACING,
                DesignParameters.MAX_MIRROR_SPACING);

        int newNumberOfMirrors = (int) clampParameter(
                current.getNumberOfMirrors() + velocity[4],
                DesignParameters.MIN_NUMBER_OF_MIRRORS,
                DesignParameters.MAX_NUMBER_OF_MIRRORS);

        return new DesignParameters(
                newReceiverHeight, 
                newReceiverDiameter,
                newMirrorWidth, 
                //newMirrorLength,
                newMirrorSpacing, 
                newNumberOfMirrors);
    }

    private double clampParameter(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        if (parameters.containsKey("swarmSize")) {
            this.swarmSize = (int) parameters.get("swarmSize");
        }
        if (parameters.containsKey("maxIterations")) {
            this.maxIterations = (int) parameters.get("maxIterations");
        }
        if (parameters.containsKey("inertiaWeight")) {
            this.inertiaWeight = (double) parameters.get("inertiaWeight");
        }
        if (parameters.containsKey("cognitiveWeight")) {
            this.cognitiveWeight = (double) parameters.get("cognitiveWeight");
        }
        if (parameters.containsKey("socialWeight")) {
            this.socialWeight = (double) parameters.get("socialWeight");
        }
    }

    @Override
    public List<DesignSolution> getHistory() {
        return new ArrayList<>(history);
    }

    @Override
    public boolean isTerminationCriteriaMet() {
        return currentIteration >= maxIterations;
    }

    @Override
    public void reset() {
        this.currentIteration = 0;
        this.swarm = new ArrayList<>();
        this.globalBest = null;
        this.globalBestFitness = Double.NEGATIVE_INFINITY;
        this.history.clear();
    }

    @Override
    public String getAlgorithmName() {
        return "Particle Swarm Optimization";
    }
}
