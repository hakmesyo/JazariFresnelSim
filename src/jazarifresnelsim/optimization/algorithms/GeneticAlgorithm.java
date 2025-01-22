package jazarifresnelsim.optimization.algorithms;

import jazarifresnelsim.optimization.problem.FresnelDesignProblem;
import jazarifresnelsim.optimization.problem.DesignParameters;
import jazarifresnelsim.optimization.problem.DesignSolution;

import java.util.*;

public class GeneticAlgorithm implements IOptimizationAlgorithm {

    private int populationSize = 50;
    private int maxGenerations = 100;
    private double crossoverRate = 0.8;
    private double mutationRate = 0.1;
    private double elitismRate = 0.1;

    private List<DesignSolution> history;
    private Random random;
    private int currentGeneration;
    private List<DesignSolution> currentPopulation;
    private DesignSolution bestSolution;

    public GeneticAlgorithm() {
        this.random = new Random();
        this.history = new ArrayList<>();
        reset();
    }

    @Override
    public DesignSolution optimize(FresnelDesignProblem problem,
            DesignParameters initialParams,
            Map<String, Object> constraints) {
        // Initialize population
        initializePopulation(initialParams, problem);

        while (!isTerminationCriteriaMet()) {
            // Evaluate current population
            evaluatePopulation(problem);

            // Store best solution
            updateBestSolution();

            // Create next generation
            List<DesignSolution> nextGeneration = new ArrayList<>();

            // Elitism: Keep best individuals
            int eliteCount = (int) (populationSize * elitismRate);
            for (int i = 0; i < eliteCount; i++) {
                nextGeneration.add(currentPopulation.get(i));
            }

            // Generate rest of the population through crossover and mutation
            while (nextGeneration.size() < populationSize) {
                DesignParameters parent1 = selectParent().getParameters();
                DesignParameters parent2 = selectParent().getParameters();

                DesignParameters child;
                if (random.nextDouble() < crossoverRate) {
                    child = crossover(parent1, parent2);
                } else {
                    child = random.nextBoolean() ? parent1 : parent2;
                }

                if (random.nextDouble() < mutationRate) {
                    child = mutate(child);
                }

                double fitness = problem.evaluateDesign(child);
                nextGeneration.add(new DesignSolution(child, fitness));
            }

            currentPopulation = nextGeneration;
            currentGeneration++;

            // Record history
            history.add(bestSolution);
        }

        return bestSolution;
    }

    private void initializePopulation(DesignParameters initial, FresnelDesignProblem problem) {
        currentPopulation = new ArrayList<>();

        // Add initial solution
        double initialFitness = problem.evaluateDesign(initial);
        currentPopulation.add(new DesignSolution(initial, initialFitness));

        // Generate random solutions
        while (currentPopulation.size() < populationSize) {
            DesignParameters params = generateRandomSolution(initial);
            double fitness = problem.evaluateDesign(params);
            currentPopulation.add(new DesignSolution(params, fitness));
        }
    }

    private DesignParameters generateRandomSolution(DesignParameters base) {
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

    private void evaluatePopulation(FresnelDesignProblem problem) {
        currentPopulation.sort((a, b)
                -> Double.compare(b.getObjectiveValue(), a.getObjectiveValue()));
    }

    private void updateBestSolution() {
        DesignSolution currentBest = currentPopulation.get(0);
        if (bestSolution == null
                || currentBest.getObjectiveValue() > bestSolution.getObjectiveValue()) {
            bestSolution = currentBest;
        }
    }

    private DesignSolution selectParent() {
        // Tournament selection
        int tournamentSize = 3;
        DesignSolution best = null;

        for (int i = 0; i < tournamentSize; i++) {
            int index = random.nextInt(currentPopulation.size());
            DesignSolution candidate = currentPopulation.get(index);
            if (best == null
                    || candidate.getObjectiveValue() > best.getObjectiveValue()) {
                best = candidate;
            }
        }

        return best;
    }

    private DesignParameters crossover(DesignParameters parent1, DesignParameters parent2) {
        // Single-point crossover for continuous parameters
        double alpha = random.nextDouble();

        double receiverHeight = interpolate(
                parent1.getReceiverHeight(),
                parent2.getReceiverHeight(), alpha);
        double receiverDiameter = interpolate(
                parent1.getReceiverDiameter(),
                parent2.getReceiverDiameter(), alpha);
        double mirrorWidth = interpolate(
                parent1.getMirrorWidth(),
                parent2.getMirrorWidth(), alpha);
        double mirrorLength = interpolate(
                parent1.getMirrorLength(),
                parent2.getMirrorLength(), alpha);
        double mirrorSpacing = interpolate(
                parent1.getMirrorSpacing(),
                parent2.getMirrorSpacing(), alpha);

        // Discrete crossover for number of mirrors
        int numberOfMirrors = random.nextBoolean()
                ? parent1.getNumberOfMirrors()
                : parent2.getNumberOfMirrors();

        return new DesignParameters(
                receiverHeight, 
                receiverDiameter,
                mirrorWidth, 
                //mirrorLength,
                mirrorSpacing, 
                numberOfMirrors);
    }

    private double interpolate(double val1, double val2, double alpha) {
        return val1 * alpha + val2 * (1 - alpha);
    }

    private DesignParameters mutate(DesignParameters params) {
        double receiverHeight = mutateParameter(
                params.getReceiverHeight(),
                DesignParameters.MIN_RECEIVER_HEIGHT,
                DesignParameters.MAX_RECEIVER_HEIGHT);
        double receiverDiameter = mutateParameter(
                params.getReceiverDiameter(),
                DesignParameters.MIN_RECEIVER_DIAMETER,
                DesignParameters.MAX_RECEIVER_DIAMETER);
        double mirrorWidth = mutateParameter(
                params.getMirrorWidth(),
                DesignParameters.MIN_MIRROR_WIDTH,
                DesignParameters.MAX_MIRROR_WIDTH);
//        double mirrorLength = mutateParameter(
//                params.getMirrorLength(),
//                DesignParameters.MIN_MIRROR_LENGTH,
//                DesignParameters.MAX_MIRROR_LENGTH);
        double mirrorSpacing = mutateParameter(
                params.getMirrorSpacing(),
                DesignParameters.MIN_MIRROR_SPACING,
                DesignParameters.MAX_MIRROR_SPACING);

        int numberOfMirrors = params.getNumberOfMirrors();
        if (random.nextDouble() < 0.2) {  // 20% chance to mutate
            numberOfMirrors += random.nextBoolean() ? 1 : -1;
            numberOfMirrors = Math.max(DesignParameters.MIN_NUMBER_OF_MIRRORS,
                    Math.min(DesignParameters.MAX_NUMBER_OF_MIRRORS, numberOfMirrors));
        }

        return new DesignParameters(
                receiverHeight, 
                receiverDiameter,
                mirrorWidth, 
                //mirrorLength,
                mirrorSpacing, 
                numberOfMirrors);
    }

    private double mutateParameter(double value, double min, double max) {
        double perturbation = random.nextGaussian() * 0.1; // 10% standard deviation
        return Math.max(min, Math.min(max, value * (1 + perturbation)));
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        if (parameters.containsKey("populationSize")) {
            this.populationSize = (int) parameters.get("populationSize");
        }
        if (parameters.containsKey("maxGenerations")) {
            this.maxGenerations = (int) parameters.get("maxGenerations");
        }
        if (parameters.containsKey("crossoverRate")) {
            this.crossoverRate = (double) parameters.get("crossoverRate");
        }
        if (parameters.containsKey("mutationRate")) {
            this.mutationRate = (double) parameters.get("mutationRate");
        }
        if (parameters.containsKey("elitismRate")) {
            this.elitismRate = (double) parameters.get("elitismRate");
        }
    }

    @Override
    public List<DesignSolution> getHistory() {
        return new ArrayList<>(history);
    }

    @Override
    public boolean isTerminationCriteriaMet() {
        return currentGeneration >= maxGenerations;
    }

    @Override
    public void reset() {
        this.currentGeneration = 0;
        this.currentPopulation = new ArrayList<>();
        this.bestSolution = null;
        this.history.clear();
    }

    @Override
    public String getAlgorithmName() {
        return "Genetic Algorithm";
    }

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
        params.put("populationSize", populationSize);
        params.put("maxGenerations", maxGenerations);
        params.put("crossoverRate", crossoverRate);
        params.put("mutationRate", mutationRate);
        params.put("elitismRate", elitismRate);
        return params;
    }
}
