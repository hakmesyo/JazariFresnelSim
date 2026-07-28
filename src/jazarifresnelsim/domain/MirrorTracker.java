package jazarifresnelsim.domain;

import jazarifresnelsim.models.SolarPosition;
import jazarifresnelsim.models.SimulationState;

/**
 * Bisector-based mirror tracking for LFR systems — Eq. (6)–(8).
 *
 * The mirror normal bisects the angle between the incident solar direction and
 * the target direction (mirror → receiver). The tilt angle is computed
 * deterministically for each mirror at each time step, yielding O(N)
 * complexity.
 *
 * VERSION 4.0 — PAPER-CONSISTENT
 *
 * Changes from v3.0: - REMOVED: +0.02 m offset on support height (undocumented
 * fudge factor) - Target vector now uses exact (H_r - H_s) as defined in Eq.
 * (7)
 *
 * @author Yunus Demirtas, Musa Atas — Siirt University
 * @version 4.0
 */
public class MirrorTracker {

    /**
     * Calculates the optimal tilt angle for a mirror at horizontal position
     * mirrorX.
     *
     * Uses the bisector method: n̂_i = (ŝ + t̂_i) / ||ŝ + t̂_i|| — Eq. (8)
     * where ŝ is the sun direction and t̂_i is the target direction to
     * receiver. The tilt angle is θ_i = atan2(n_x, n_z).
     *
     * @param mirrorX horizontal offset of mirror from field center [m]
     * @param sunPos current solar position
     * @param state simulation state containing receiver/support heights
     * @return optimal tilt angle [degrees]
     */
    public double calculateOptimalMirrorAngle(double mirrorX,
            SolarPosition sunPos, SimulationState state) {

        double alt = Math.toRadians(sunPos.getAltitudeAngle());
        double azi = Math.toRadians(sunPos.getAzimuthAngle());

        // Sun vector components in the transverse plane (x) and vertical (z).
        // The mirror rotates about the N-S axis, so only the projection of the
        // sun direction onto the plane normal to that axis governs tracking.
        double sx = -Math.cos(alt) * Math.sin(azi);
        double sz = Math.sin(alt);

        if (Math.sqrt(sx * sx + sz * sz) < 1e-12) {
            return 0.0;
        }

        double psi_s = Math.atan2(sx, sz);

        double dh = (state.getReceiverHeight() - state.getSupportHeight()) / 100.0;
        if (Math.abs(dh) < 1e-12) {
            return 0.0;
        }

        double psi_t = Math.atan2(-mirrorX, dh);

        return Math.toDegrees(0.5 * (psi_s + psi_t));
    }
}
