package jazarifresnelsim.optimization.algorithms;

import jazarifresnelsim.optimization.problem.FresnelDesignProblem;
import jazarifresnelsim.optimization.problem.DesignParameters;
import jazarifresnelsim.optimization.problem.DesignSolution;

import java.util.*;

public class ReinforcementLearning implements IOptimizationAlgorithm {

    // RL parameters
    private double learningRate = 0.1;
    private double discountFactor = 0.9;
    private double explorationRate = 0.3;
    private int maxEpisodes = 1000;
    private int stepsPerEpisode = 100;

    // State-Action space discretization
    private static final int STATE_DIVISIONS = 5;
    private static final int ACTION_DIVISIONS = 3;

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
        params.put("learningRate", learningRate);
        params.put("discountFactor", discountFactor);
        params.put("explorationRate", explorationRate);
        params.put("maxEpisodes", maxEpisodes);
        params.put("stepsPerEpisode", stepsPerEpisode);
        return params;
    }

    private class StateAction {

        final int[] state;
        final int[] action;

        StateAction(int[] state, int[] action) {
            this.state = state;
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            StateAction that = (StateAction) o;
            return Arrays.equals(state, that.state) && Arrays.equals(action, that.action);
        }

        @Override
        public int hashCode() {
            int result = Arrays.hashCode(state);
            result = 31 * result + Arrays.hashCode(action);
            return result;
        }
    }

    private Map<StateAction, Double> qTable;
    private List<DesignSolution> history;
    private Random random;
    private int currentEpisode;
    private DesignSolution bestSolution;
    private double bestFitness;

    public ReinforcementLearning() {
        this.random = new Random();
        this.history = new ArrayList<>();
        this.qTable = new HashMap<>();
        reset();
    }

    @Override
    public DesignSolution optimize(FresnelDesignProblem problem,
            DesignParameters initialParams,
            Map<String, Object> constraints) {
        bestSolution = null;
        bestFitness = Double.NEGATIVE_INFINITY;

        while (!isTerminationCriteriaMet()) {
            // Run one episode
            DesignParameters currentParams = initialParams;
            double episodeBestFitness = Double.NEGATIVE_INFINITY;
            DesignParameters episodeBestParams = null;

            for (int step = 0; step < stepsPerEpisode; step++) {
                // Get current state
                int[] state = discretizeState(currentParams);

                // Choose action using epsilon-greedy policy
                int[] action = selectAction(state);

                // Apply action to get new parameters
                DesignParameters newParams = applyAction(currentParams, action);

                // Get reward (fitness improvement)
                double currentFitness = problem.evaluateDesign(currentParams);
                double newFitness = problem.evaluateDesign(newParams);
                double reward = newFitness - currentFitness;

                // Update episode best
                if (newFitness > episodeBestFitness) {
                    episodeBestFitness = newFitness;
                    episodeBestParams = newParams;
                }

                // Update Q-value
                StateAction sa = new StateAction(state, action);
                int[] newState = discretizeState(newParams);
                double maxNextQ = getMaxQValue(newState);

                double oldQ = qTable.getOrDefault(sa, 0.0);
                double newQ = oldQ + learningRate * (reward + discountFactor * maxNextQ - oldQ);
                qTable.put(sa, newQ);

                currentParams = newParams;
            }

            // Update global best
            if (episodeBestFitness > bestFitness) {
                bestFitness = episodeBestFitness;
                bestSolution = new DesignSolution(episodeBestParams, episodeBestFitness);
            }

            // Record history
            history.add(bestSolution);

            // Decay exploration rate
            explorationRate *= 0.995;
            currentEpisode++;
        }

        return bestSolution;
    }

    private int[] discretizeState(DesignParameters params) {
        int[] state = new int[5];

        state[0] = discretize(params.getReceiverHeight(),
                DesignParameters.MIN_RECEIVER_HEIGHT,
                DesignParameters.MAX_RECEIVER_HEIGHT,
                STATE_DIVISIONS);
        state[1] = discretize(params.getReceiverDiameter(),
                DesignParameters.MIN_RECEIVER_DIAMETER,
                DesignParameters.MAX_RECEIVER_DIAMETER,
                STATE_DIVISIONS);
        state[2] = discretize(params.getMirrorWidth(),
                DesignParameters.MIN_MIRROR_WIDTH,
                DesignParameters.MAX_MIRROR_WIDTH,
                STATE_DIVISIONS);
//        state[3] = discretize(params.getMirrorLength(),
//                DesignParameters.MIN_MIRROR_LENGTH,
//                DesignParameters.MAX_MIRROR_LENGTH,
//                STATE_DIVISIONS);
        state[3] = discretize(params.getMirrorSpacing(),
                DesignParameters.MIN_MIRROR_SPACING,
                DesignParameters.MAX_MIRROR_SPACING,
                STATE_DIVISIONS);
        state[4] = discretize(params.getNumberOfMirrors(),
                DesignParameters.MIN_NUMBER_OF_MIRRORS,
                DesignParameters.MAX_NUMBER_OF_MIRRORS,
                STATE_DIVISIONS);

        return state;
    }

    private int discretize(double value, double min, double max, int divisions) {
        double step = (max - min) / divisions;
        int index = (int) ((value - min) / step);
        return Math.max(0, Math.min(divisions - 1, index));
    }

    private int[] selectAction(int[] state) {
        if (random.nextDouble() < explorationRate) {
            return generateRandomAction();
        }
        return getBestAction(state);
    }

    private int[] generateRandomAction() {
        int[] action = new int[6];
        for (int i = 0; i < action.length; i++) {
            action[i] = random.nextInt(ACTION_DIVISIONS) - 1; // -1, 0, or 1
        }
        return action;
    }

    private int[] getBestAction(int[] state) {
        int[] bestAction = null;
        double bestQ = Double.NEGATIVE_INFINITY;

        // Try all possible actions
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    int[] action = {i, j, k, 0, 0, 0}; // Simplified action space
                    StateAction sa = new StateAction(state, action);
                    double q = qTable.getOrDefault(sa, 0.0);

                    if (q > bestQ) {
                        bestQ = q;
                        bestAction = action;
                    }
                }
            }
        }

        return bestAction != null ? bestAction : generateRandomAction();
    }

    private double getMaxQValue(int[] state) {
        double maxQ = Double.NEGATIVE_INFINITY;

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    int[] action = {i, j, k, 0, 0, 0};
                    StateAction sa = new StateAction(state, action);
                    double q = qTable.getOrDefault(sa, 0.0);
                    maxQ = Math.max(maxQ, q);
                }
            }
        }

        return maxQ == Double.NEGATIVE_INFINITY ? 0.0 : maxQ;
    }

    private DesignParameters applyAction(DesignParameters current, int[] action) {
        double stepSize = 0.05; // 5% change

        double newReceiverHeight = updateParameter(
                current.getReceiverHeight(),
                action[0],
                DesignParameters.MIN_RECEIVER_HEIGHT,
                DesignParameters.MAX_RECEIVER_HEIGHT,
                stepSize);

        double newReceiverDiameter = updateParameter(
                current.getReceiverDiameter(),
                action[1],
                DesignParameters.MIN_RECEIVER_DIAMETER,
                DesignParameters.MAX_RECEIVER_DIAMETER,
                stepSize);

        double newMirrorWidth = updateParameter(
                current.getMirrorWidth(),
                action[2],
                DesignParameters.MIN_MIRROR_WIDTH,
                DesignParameters.MAX_MIRROR_WIDTH,
                stepSize);

//        double newMirrorLength = updateParameter(
//                current.getMirrorLength(),
//                action[3],
//                DesignParameters.MIN_MIRROR_LENGTH,
//                DesignParameters.MAX_MIRROR_LENGTH,
//                stepSize);

        double newMirrorSpacing = updateParameter(
                current.getMirrorSpacing(),
                action[4],
                DesignParameters.MIN_MIRROR_SPACING,
                DesignParameters.MAX_MIRROR_SPACING,
                stepSize);

        int newNumberOfMirrors = (int) updateParameter(
                current.getNumberOfMirrors(),
                action[5],
                DesignParameters.MIN_NUMBER_OF_MIRRORS,
                DesignParameters.MAX_NUMBER_OF_MIRRORS,
                1); // Step size of 1 for discrete parameter

        return new DesignParameters(
                newReceiverHeight, 
                newReceiverDiameter,
                newMirrorWidth, 
                //newMirrorLength,
                newMirrorSpacing, 
                newNumberOfMirrors);
    }

    private double updateParameter(double current, int action, double min, double max, double stepSize) {
        double step = (max - min) * stepSize;
        double newValue = current + action * step;
        return Math.max(min, Math.min(max, newValue));
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        if (parameters.containsKey("learningRate")) {
            this.learningRate = (double) parameters.get("learningRate");
        }
        if (parameters.containsKey("discountFactor")) {
            this.discountFactor = (double) parameters.get("discountFactor");
        }
        if (parameters.containsKey("explorationRate")) {
            this.explorationRate = (double) parameters.get("explorationRate");
        }
        if (parameters.containsKey("maxEpisodes")) {
            this.maxEpisodes = (int) parameters.get("maxEpisodes");
        }
        if (parameters.containsKey("stepsPerEpisode")) {
            this.stepsPerEpisode = (int) parameters.get("stepsPerEpisode");
        }
    }

    @Override
    public List<DesignSolution> getHistory() {
        return new ArrayList<>(history);
    }

    @Override
    public boolean isTerminationCriteriaMet() {
        return currentEpisode >= maxEpisodes;
    }

    @Override
    public void reset() {
        this.currentEpisode = 0;
        this.qTable.clear();
        this.history.clear();
        this.bestSolution = null;
        this.bestFitness = Double.NEGATIVE_INFINITY;
    }

    @Override
    public String getAlgorithmName() {
        return "Q-Learning";
    }
}
