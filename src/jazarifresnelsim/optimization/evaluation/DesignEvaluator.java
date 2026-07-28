package jazarifresnelsim.optimization.evaluation;

import jazarifresnelsim.optimization.problem.FresnelDesignProblem;
import jazarifresnelsim.optimization.problem.DesignParameters;
import jazarifresnelsim.optimization.problem.DesignSolution;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Evaluates LFR designs using optical performance metrics only.
 *
 * The sole objective is the annual optical intercept power Q_opt [W·h],
 * consistent with Eq. (14)–(16) of the manuscript. No cost, thermal,
 * or cleanliness factors are included — those lie outside the scope of
 * the optical pre-screening framework.
 *
 * Metrics produced:
 *   TOTAL_OPTICAL_POWER  — mean hourly Q_opt across evaluation times [W]
 *   MIRROR_AREA_EFF      — Q_opt per unit mirror aperture area [W/m²]
 *   LAND_AREA_EFF        — Q_opt per unit ground coverage area [W/m²]
 *
 * @author Yunus Demirtas, Musa Atas — Siirt University
 * @version 4.1
 */
public class DesignEvaluator {

    // Metric keys
    public static final String TOTAL_OPTICAL_POWER = "totalOpticalPower";
    public static final String MIRROR_AREA_EFF     = "mirrorAreaEfficiency";
    public static final String LAND_AREA_EFF       = "landAreaEfficiency";

    private final FresnelDesignProblem problem;
    private final List<LocalDateTime>  evaluationTimes;

    public DesignEvaluator(FresnelDesignProblem problem,
                           List<LocalDateTime> evaluationTimes) {
        this.problem         = problem;
        this.evaluationTimes = new ArrayList<>(evaluationTimes);
    }

    /**
     * Evaluates a design and returns optical performance metrics.
     *
     * @param params design parameters
     * @return map of metric name → value
     */
    public Map<String, Double> evaluateDesign(DesignParameters params) {
        Map<String, Double> metrics = new HashMap<>();

        double totalPower = problem.evaluateDesign(params);
        double mirrorArea = mirrorArea(params);
        double landArea   = landArea(params);

        metrics.put(TOTAL_OPTICAL_POWER, totalPower);
        metrics.put(MIRROR_AREA_EFF, mirrorArea > 0 ? totalPower / mirrorArea : 0.0);
        metrics.put(LAND_AREA_EFF,   landArea   > 0 ? totalPower / landArea   : 0.0);

        return metrics;
    }

    // ----------------------------------------------------------------
    // Area helpers
    // ----------------------------------------------------------------

    /** Total mirror aperture area [m²]. */
    private double mirrorArea(DesignParameters p) {
        return (p.getMirrorWidth() * p.getMirrorLength() * p.getNumberOfMirrors()) / 10_000.0;
    }

    /** Approximate ground coverage width × mirror length [m²]. */
    private double landArea(DesignParameters p) {
        double totalWidth = (p.getNumberOfMirrors() - 1) * p.getMirrorSpacing()
                          + p.getMirrorWidth();
        return (totalWidth * p.getMirrorLength() * 1.2) / 10_000.0;
    }
}