package jazarifresnelsim.domain;

import jazarifresnelsim.models.MirrorPosition;
import jazarifresnelsim.models.SimulationState;
import jazarifresnelsim.models.SolarPosition;
import java.util.List;

/**
 * Inter-mirror shading and blocking loss calculation for LFR systems.
 *
 * Implements Eq. (10) of the manuscript: eta_sb,i = max(0, (w - sum_{j != i}
 * l_loss,ij) / w)
 *
 * where l_loss,ij = max(l_sh,ij, l_bl,ij) prevents double-counting of shading
 * and blocking from the same neighbour.
 *
 * VERSION 4.4 — Directional relevance re-connected.
 *
 * BUG FOUND (via Test 11, low-sun-angle validation against SolTrace, G2
 * geometry, Diyarbakir June 21): at low solar altitude the formula summed
 * a shading/blocking contribution from EVERY other mirror in the field,
 * including mirrors on the side that cannot physically cast a shadow or
 * block the reflected beam given the sun's actual azimuth. The
 * isShadowRelevant()/isBlockingRelevant() directional-gate helpers already
 * existed in this class but were not called by the active formula -- dead
 * code, most likely left over from an earlier version. At altitude=10.4 deg
 * (06:00) this produced eta_opt=2.6% versus SolTrace's 30.4% for the same
 * geometry and sun position (RMSE 11.96 pp over six hours spanning 10-75
 * deg altitude, versus 0.25 pp at solar noon across five geometries --
 * Table 3-4). At high sun altitude the mirrors on the "wrong" side already
 * contributed close to zero loss anyway, so this fix is not expected to
 * change the already-validated solar-noon numbers (Tables 3-4, 6, 10, 11
 * and Fig. 3-5) -- re-run those after applying this fix to confirm.
 *
 * OPEN QUESTION, TESTED AND RESOLVED: a max-based union of overlapping
 * neighbour contributions (in place of the sum above) was tried and gave
 * IDENTICAL results for the G2/six-hour test case -- for this geometry,
 * at most one neighbour per side ever contributes a nonzero term, so sum
 * and union coincide. The remaining low-sun-angle gap (RMSE ~5.8 pp across
 * 10-75 deg altitude, versus 0.25 pp at solar noon) is not explained by
 * sum-vs-union double counting. Manual inspection at 17:00 (alt=29.6 deg)
 * confirms the formula correctly finds no geometric shadow overlap between
 * any mirror pair at that hour (hs+hi < d for every candidate neighbour);
 * the discrepancy against SolTrace there is not a coding bug in this
 * formula, but most likely reflects the inherent limitation of a 1-D,
 * projected-half-width shading approximation relative to full 3-D ray
 * tracing at extreme oblique angles. Left as a documented limitation
 * rather than patched further; revisiting it would mean replacing this
 * projected-half-width model with an exact shadow-polygon intersection,
 * a larger undertaking than this fix.
 *
 * Reference: Abbas et al. (2013), Sharma et al. (2015), Grena (2024).
 *
 * @author Yunus Demirtas, Musa Atas — Siirt University
 * @version 4.4
 */
public class ShadingDetector {

    /**
     * Calculates the combined shading and blocking efficiency for mirror i.
     *
     * For each neighbour j, the projected shadow width (along sun direction)
     * and blocking width (along reflected direction) are compared against the
     * clear gap between mirror edges. The combined loss per neighbour is
     * max(shading, blocking) to avoid double-counting.
     *
     * @param currentMirror mirror i being evaluated
     * @param allMirrors all mirrors in the field
     * @param state simulation state (geometry parameters)
     * @param sunPos current solar position
     * @return eta_sb,i in [0, 1]
     */
    public double calculateBlockingAndShadingLoss(
            MirrorPosition currentMirror,
            List<MirrorPosition> allMirrors,
            SimulationState state,
            SolarPosition sunPos) {

        double w_m = state.getReflectorWidth() / 100.0;   // ayna genisligi [m]
        double Hs_m = state.getSupportHeight() / 100.0;
        double Hr_m = state.getReceiverHeight() / 100.0;

        double xi = currentMirror.getXOffset() / 100.0;
        double ti = Math.toRadians(currentMirror.getRotationAngle());

        if (w_m <= 1e-9) {
            return 0.0;
        }

        // ---- gunes yonu, enine duzlem ----
        double alt = Math.toRadians(sunPos.getAltitudeAngle());
        double azi = Math.toRadians(sunPos.getAzimuthAngle());

        double s_x = -Math.cos(alt) * Math.sin(azi);
        double s_y = Math.sin(alt);

        if (s_y <= 1e-9) {
            return 0.0;            // gunes ufkun altinda
        }
        double tanSun = s_x / s_y;

        // ---- yansiyan yon: ayna i -> alici ----
        double r_x = -xi;
        double r_y = Hr_m - Hs_m;

        if (Math.abs(r_y) < 1e-9) {
            return 0.0;
        }
        double tanRef = r_x / r_y;

        // ayna i'nin yatay duzlemdeki izdusum yari-genisligi
        double hi = 0.5 * w_m * Math.abs(Math.cos(ti));

        double totalLost = 0.0;

        for (MirrorPosition other : allMirrors) {

            if (other.getMirrorIndex() == currentMirror.getMirrorIndex()) {
                continue;
            }

            double xj = other.getXOffset() / 100.0;
            double tj = Math.toRadians(other.getRotationAngle());
            double d = Math.abs(xi - xj);

            // ayna j'nin golgesi (gunes yonunde izdusum) -- sadece gunese
            // gore "yukari akim" tarafindaki komsular golge dusurebilir
            double l_sh = 0.0;
            if (isShadowRelevant(xi, xj, s_x)) {
                double hs = 0.5 * w_m * Math.abs(Math.cos(tj) + Math.sin(tj) * tanSun);
                l_sh = Math.max(0.0, hs + hi - d);
            }

            // ayna j'nin siluети (yansiyan huzme yonunde izdusum) -- sadece
            // yansima yonune gore ilgili taraftaki komsular bloklayabilir
            double l_bl = 0.0;
            if (isBlockingRelevant(xi, xj, r_x)) {
                double hb = 0.5 * w_m * Math.abs(Math.cos(tj) - Math.sin(tj) * tanRef);
                l_bl = Math.max(0.0, hb + hi - d);
            }

            // ayni komsudan golge+bloklama cift sayilmasin
            totalLost += Math.min(w_m, Math.max(l_sh, l_bl));
        }

        return Math.max(0.0, (w_m - totalLost) / w_m);
    }

//    public double calculateBlockingAndShadingLoss(
//            MirrorPosition       currentMirror,
//            List<MirrorPosition> allMirrors,
//            SimulationState      state,
//            SolarPosition        sunPos) {
//
//        double w_m  = state.getReflectorWidth()   / 100.0;  // mirror width [m]
//        double Hs_m = state.getSupportHeight()     / 100.0;
//        double Hr_m = state.getReceiverHeight()    / 100.0;
//        double xi   = currentMirror.getXOffset()   / 100.0;  // [m]
//
//        // 3D sun direction components
//        double sunAltRad = Math.toRadians(sunPos.getAltitudeAngle());
//        double sunAzRad  = Math.toRadians(sunPos.getAzimuthAngle());
//        double s_x = -Math.cos(sunAltRad) * Math.sin(sunAzRad);
//        double s_z =  Math.sin(sunAltRad);
//
//        // Guard: sun at or below horizon
//        if (Math.abs(s_z) < 1e-9) return 0.0;
//
//        // Reflected direction from mirror i toward receiver (transverse plane)
//        double r_x = -(xi);
//        double r_z = Hr_m - Hs_m;
//        double r_mag = Math.sqrt(r_x * r_x + r_z * r_z);
//        r_x /= r_mag;
//        r_z /= r_mag;
//
//        double totalLost = 0.0;
//
//        for (MirrorPosition other : allMirrors) {
//            if (other.getMirrorIndex() == currentMirror.getMirrorIndex()) continue;
//
//            double xj  = other.getXOffset() / 100.0;
//            double dx  = Math.abs(xi - xj);          // centre-to-centre [m]
//            double gap = dx - w_m;                    // clear gap between edges [m]
//            // gap can be negative for overlapping mirrors (should not occur
//            // in valid LFR geometry, but clamp to 0 for robustness)
//            gap = Math.max(0.0, gap);
//
//            // --- SHADING: shadow of neighbour j projected along sun direction ---
//            double l_sh = 0.0;
//            if (isShadowRelevant(xi, xj, s_x)) {
//                // Projected shadow width = w * |s_x / s_z|  (transverse plane)
//                double projShadow = w_m * Math.abs(s_x / s_z);
//                // Shadow overlaps mirror i only if it exceeds the clear gap
//                l_sh = Math.max(0.0, projShadow - gap);
//                // Clamp to mirror width (cannot lose more than the full width)
//                l_sh = Math.min(l_sh, w_m);
//            }
//
//            // --- BLOCKING: reflected beam from mirror i blocked by neighbour j ---
//            double l_bl = 0.0;
//            if (isBlockingRelevant(xi, xj, r_x) && Math.abs(r_z) > 1e-9) {
//                double projBlock = w_m * Math.abs(r_x / r_z);
//                l_bl = Math.max(0.0, projBlock - gap);
//                l_bl = Math.min(l_bl, w_m);
//            }
//
//            // Combined loss — max avoids double-counting — Eq. (10)
//            totalLost += Math.max(l_sh, l_bl);
//        }
//
//        // eta_sb,i = max(0, (w - totalLost) / w) — Eq. (10)
//        return Math.max(0.0, (w_m - totalLost) / w_m);
//    }
    // ================================================================
    // RELEVANCE CHECKS
    // ================================================================
    /**
     * Returns true if mirror at xj can cast a shadow on mirror at xi. Shadow
     * travels in the direction of incident sunlight (s_x component). Only
     * mirrors on the upstream side can shade downstream mirrors.
     */
    private boolean isShadowRelevant(double xi, double xj, double s_x) {
        if (Math.abs(s_x) < 1e-9) {
            return false;   // sun directly overhead
        }        // Sun comes from left (s_x < 0): only mirrors to the LEFT shade xi
        if (s_x < 0 && xj >= xi) {
            return false;
        }
        if (s_x > 0 && xj <= xi) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if mirror at xj can block the reflected beam from xi.
     * Blocking occurs along the reflected direction (r_x component).
     */
    private boolean isBlockingRelevant(double xi, double xj, double r_x) {
        if (Math.abs(r_x) < 1e-9) {
            return false;
        }
        if (r_x < 0 && xj >= xi) {
            return false;
        }
        if (r_x > 0 && xj <= xi) {
            return false;
        }
        return true;
    }
}