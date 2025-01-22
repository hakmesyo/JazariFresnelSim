package jazarifresnelsim.optimization.algorithms;

import jazarifresnelsim.optimization.problem.DesignParameters;
import jazarifresnelsim.optimization.problem.DesignSolution;
import java.util.*;
import jazarifresnelsim.optimization.problem.FresnelDesignProblem;

public class SimulatedAnnealing implements IOptimizationAlgorithm {

    // Güncellenen parametreler
    private double initialTemperature = 1000.0;
    private double coolingRate = 0.995;    // Daha yavaş soğuma
    private int maxIterations = 50000;     // Daha fazla iterasyon
    private double minTemperature = 1e-8;  // Daha düşük minimum sıcaklık

    // Yeni eklenen parametreler
    private static final double ADAPTIVE_RATE = 0.001;  // Adaptif soğuma için
    private static final double MAX_PERTURBATION = 0.1; // Maximum değişim oranı (%10)
    private static final double CHANGE_PROBABILITY = 0.5; // Parametre değişim olasılığı

    private List<DesignSolution> history;
    private Random random;
    private int currentIteration;
    private double currentTemperature;

    public SimulatedAnnealing() {
        this.random = new Random();
        this.history = new ArrayList<>();
        reset();
    }

    @Override
    public DesignSolution optimize(FresnelDesignProblem problem,
            DesignParameters initialParams,
            Map<String, Object> constraints) {
        DesignParameters currentSolution = initialParams;
        double currentEnergy = problem.evaluateDesign(currentSolution);

        DesignParameters bestSolution = currentSolution;
        double bestEnergy = currentEnergy;

        int noImprovementCount = 0;
        double adaptiveCoolingRate = coolingRate;

        while (!isTerminationCriteriaMet()) {
            // Adaptif soğuma oranı güncelleme
            if (noImprovementCount > 1000) {
                adaptiveCoolingRate = Math.pow(coolingRate, 1.0 + ADAPTIVE_RATE * noImprovementCount);
            }

            // Yeni çözüm üret
            DesignParameters newSolution = generateNeighbor(currentSolution);
            double newEnergy = problem.evaluateDesign(newSolution);

            // Kabul olasılığı hesapla
            if (acceptSolution(currentEnergy, newEnergy, currentTemperature)) {
                currentSolution = newSolution;
                currentEnergy = newEnergy;

                // En iyi çözümü güncelle
                if (newEnergy > bestEnergy) {
                    bestSolution = newSolution;
                    bestEnergy = newEnergy;
                    noImprovementCount = 0;
                } else {
                    noImprovementCount++;
                }
            } else {
                noImprovementCount++;
            }

            // History'ye kaydet
            history.add(new DesignSolution(bestSolution, bestEnergy));

            // Sıcaklığı düşür
            currentTemperature *= adaptiveCoolingRate;
            currentIteration++;

            // Periyodik yeniden ısıtma
            if (currentTemperature < minTemperature && currentIteration < maxIterations) {
                currentTemperature = initialTemperature * 0.5;
                noImprovementCount = 0;
            }
        }

        return new DesignSolution(bestSolution, bestEnergy);
    }

    private boolean acceptSolution(double currentEnergy, double newEnergy, double temperature) {
        if (newEnergy > currentEnergy) {
            return true;
        }
        // Düzeltilmiş acceptance probability hesabı
        double acceptanceProb = Math.exp(-(currentEnergy - newEnergy) / temperature);
        return random.nextDouble() < acceptanceProb;
    }

    private DesignParameters generateNeighbor(DesignParameters current) {
        double receiverHeight = current.getReceiverHeight();
        double receiverDiameter = current.getReceiverDiameter();
        double mirrorWidth = current.getMirrorWidth();
        double mirrorSpacing = current.getMirrorSpacing();
        int numberOfMirrors = current.getNumberOfMirrors();

        // Her parametre için kontrollü değişim
        if (random.nextDouble() < CHANGE_PROBABILITY) {
            double perturbation = (random.nextGaussian() * MAX_PERTURBATION);
            receiverHeight = clamp(
                    receiverHeight * (1 + perturbation),
                    DesignParameters.MIN_RECEIVER_HEIGHT,
                    DesignParameters.MAX_RECEIVER_HEIGHT
            );
        }

        if (random.nextDouble() < CHANGE_PROBABILITY) {
            double perturbation = (random.nextGaussian() * MAX_PERTURBATION);
            receiverDiameter = clamp(
                    receiverDiameter * (1 + perturbation),
                    DesignParameters.MIN_RECEIVER_DIAMETER,
                    DesignParameters.MAX_RECEIVER_DIAMETER
            );
        }

        if (random.nextDouble() < CHANGE_PROBABILITY) {
            double perturbation = (random.nextGaussian() * MAX_PERTURBATION);
            mirrorWidth = clamp(
                    mirrorWidth * (1 + perturbation),
                    DesignParameters.MIN_MIRROR_WIDTH,
                    DesignParameters.MAX_MIRROR_WIDTH
            );
        }

        if (random.nextDouble() < CHANGE_PROBABILITY) {
            double perturbation = (random.nextGaussian() * MAX_PERTURBATION);
            mirrorSpacing = clamp(
                    mirrorSpacing * (1 + perturbation),
                    DesignParameters.MIN_MIRROR_SPACING,
                    DesignParameters.MAX_MIRROR_SPACING
            );
        }

        if (random.nextDouble() < CHANGE_PROBABILITY) {
            // Ayna sayısı için daha kontrollü değişim
            int step = random.nextBoolean() ? 1 : -1;
            numberOfMirrors = (int) clamp(
                    numberOfMirrors + step,
                    DesignParameters.MIN_NUMBER_OF_MIRRORS,
                    DesignParameters.MAX_NUMBER_OF_MIRRORS
            );
        }

        return new DesignParameters(
                receiverHeight,
                receiverDiameter,
                mirrorWidth,
                mirrorSpacing,
                numberOfMirrors
        );
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        if (parameters.containsKey("initialTemperature")) {
            this.initialTemperature = (double) parameters.get("initialTemperature");
        }
        if (parameters.containsKey("coolingRate")) {
            this.coolingRate = (double) parameters.get("coolingRate");
        }
        if (parameters.containsKey("maxIterations")) {
            this.maxIterations = (int) parameters.get("maxIterations");
        }
        if (parameters.containsKey("minTemperature")) {
            this.minTemperature = (double) parameters.get("minTemperature");
        }
    }

    @Override
    public List<DesignSolution> getHistory() {
        return new ArrayList<>(history);
    }

    @Override
    public boolean isTerminationCriteriaMet() {
        return currentIteration >= maxIterations || 
               (currentTemperature < minTemperature && currentIteration > maxIterations/2);
    }

    @Override
    public void reset() {
        this.currentIteration = 0;
        this.currentTemperature = initialTemperature;
        this.history.clear();
    }

    @Override
    public String getAlgorithmName() {
        return "Simulated Annealing";
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
        params.put("initialTemperature", initialTemperature);
        params.put("coolingRate", coolingRate);
        params.put("maxIterations", maxIterations);
        params.put("minTemperature", minTemperature);
        return params;
    }
}