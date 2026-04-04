package jazarifresnelsim.domain;

import jazarifresnelsim.models.SolarPosition;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import jazarifresnelsim.models.MirrorPosition;
import jazarifresnelsim.models.SimulationState;
import static jazarifresnelsim.optimization.problem.DesignParameters.MIN_MIRROR_SPACING;
import static jazarifresnelsim.optimization.problem.DesignParameters.MIN_RECEIVER_HEIGHT;

/**
 * Solar position calculation and energy output estimation for LFR systems.
 *
 * Optical model: Spencer 7-term Fourier solar position (Eq. 1–5),
 * bisector-based mirror tracking (Eq. 6–8), cosine / shading / blocking /
 * end-loss / first-order spillage corrections (Eq. 9–14).
 *
 * Thermal model: simplified first-order estimate using a fixed overall thermal
 * efficiency factor (eta_th = 0.70) and a distance-dependent loss term. This
 * model intentionally omits secondary optics (CPC), evacuated-tube effects,
 * and detailed HTF flow; it is designed solely as a rapid pre-screening tool
 * to provide a first-order correction distinguishing configurations with
 * different net energy yield potential. See Section 2.4 and Section 6.4 of
 * the accompanying manuscript for a full discussion of limitations.
 *
 * NOTE — what was removed in v2.2:
 *   The ad-hoc Gaussian height-efficiency factor
 *       exp(-HEIGHT_PENALTY_FACTOR * |Hr - OPTIMAL_HEIGHT|)
 *   that was present in earlier versions has been removed. This factor had
 *   no physical basis and artificially created a peak in the Hr sweep at
 *   Hr = 150 cm regardless of system geometry. The thermal model now relies
 *   solely on the physically motivated distance-loss term and the fixed
 *   thermal efficiency scalar.
 *
 * @author  Yunus Demirtas, Musa Atas — Siirt University
 * @version 2.2
 */
public class SolarCalculator {

    // ----------------------------------------------------------------
    // Solar / optical constants
    // ----------------------------------------------------------------
    private static final double SOLAR_CONSTANT        = 1361.0; // W/m²
    private static final double MIRROR_REFLECTIVITY   = 0.92;   // rho_m   (Table 2)
    private static final double RECEIVER_ABSORPTIVITY = 0.95;   // alpha_r (Table 2)
    private static final double SHADING_FACTOR        = 0.95;   // general cleanliness / tracking factor
    private static final double SUN_ANGULAR_WIDTH     = 0.53;   // degrees (basis for beam-spread)

    // ----------------------------------------------------------------
    // Simplified thermal model constants
    // ----------------------------------------------------------------
    /**
     * Fixed overall thermal efficiency scalar eta_th.
     * Represents a first-order estimate for a bare steel receiver tube at
     * moderate temperatures (≈ 150–250 °C) for small-to-medium LFR systems.
     * A full Churchill-Bernstein radiative-convective model is planned for
     * future work once secondary optics (CPC) are integrated.
     */
    private static final double THERMAL_EFFICIENCY    = 0.70;

    /**
     * Distance-loss base factor per unit normalised distance.
     * Accounts for beam divergence and increased spillage with mirror-to-
     * receiver distance. Derived empirically from SolTrace cross-validation
     * across the five benchmark geometries (Section 4.3).
     */
    private static final double DISTANCE_LOSS_FACTOR  = 0.95;

    // ----------------------------------------------------------------
    // Location
    // ----------------------------------------------------------------
    private double latitude;   // radians
    private double longitude;  // radians
    private double altitude;   // metres

    public SolarCalculator(double latitudeDegrees, double longitudeDegrees, double altitude) {
        updateLocation(latitudeDegrees, longitudeDegrees);
        this.altitude = altitude;
    }

    public void updateLocation(double latitudeDegrees, double longitudeDegrees) {
        this.latitude  = Math.toRadians(latitudeDegrees);
        this.longitude = Math.toRadians(longitudeDegrees);
    }

    // ================================================================
    // 1.  SOLAR POSITION — Spencer (1971) 7-term Fourier series
    //     Equations (1)–(5) of the paper
    // ================================================================
    public SolarPosition calculateSolarPosition(LocalDateTime dateTime) {
        int dayOfYear = dateTime.getDayOfYear();

        // Day angle B (radians)
        double B = 2 * Math.PI * (dayOfYear - 1) / 365.0;

        // Solar declination delta — Spencer (1971), Eq. (3)
        double declination = Math.toDegrees(
                0.006918
                - 0.399912 * Math.cos(B)   + 0.070257 * Math.sin(B)
                - 0.006758 * Math.cos(2*B) + 0.000907 * Math.sin(2*B)
                - 0.002697 * Math.cos(3*B) + 0.001480 * Math.sin(3*B));

        // Equation of time E_t [minutes] — Eq. (1)
        double E = 229.18 * (0.000075
                + 0.001868 * Math.cos(B)   - 0.032077 * Math.sin(B)
                - 0.014615 * Math.cos(2*B) - 0.040849 * Math.sin(2*B));

        // True solar time — Eq. (2)
        double hour         = dateTime.getHour() + dateTime.getMinute() / 60.0;
        double lonDeg       = Math.toDegrees(longitude);
        double tzMerid      = 15.0 * Math.round(lonDeg / 15.0); // nearest standard meridian
        double timeCorrection = 4.0 * (lonDeg - tzMerid) + E;
        double solarTime    = hour + timeCorrection / 60.0;

        // Hour angle omega [degrees], negative before solar noon
        double hourAngle = 15.0 * (solarTime - 12.0);

        // Solar altitude alpha — Eq. (4)
        double sinDec = Math.sin(Math.toRadians(declination));
        double cosDec = Math.cos(Math.toRadians(declination));
        double sinLat = Math.sin(latitude);
        double cosLat = Math.cos(latitude);
        double cosHA  = Math.cos(Math.toRadians(hourAngle));

        double sinAlt     = sinLat*sinDec + cosLat*cosDec*cosHA;
        double altitudeDeg = Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, sinAlt))));

        // Solar azimuth A — Eq. (5)
        double cosAz = (sinDec*cosLat - cosDec*sinLat*cosHA)
                       / Math.max(1e-9, Math.cos(Math.toRadians(altitudeDeg)));
        double azimuthDeg = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, cosAz))));
        if (hourAngle > 0) azimuthDeg = 360.0 - azimuthDeg;

        // Atmospheric corrections
        double airMass    = calculateAirMass(altitudeDeg);
        double refraction = calculateAtmosphericRefraction(altitudeDeg);
        altitudeDeg += refraction / 3600.0; // arcsec → degrees

        // DNI with Hottel clear-sky transmittance model
        double solarIntensity = SOLAR_CONSTANT * Math.pow(0.7, Math.pow(airMass, 0.678));

        return new SolarPosition(altitudeDeg, azimuthDeg, solarIntensity);
    }

    // ================================================================
    // 2.  TOTAL ENERGY OUTPUT
    // ================================================================
    /**
     * Computes the estimated instantaneous useful energy output [W] for the
     * entire mirror field.
     *
     * Per-mirror optical power is accumulated, then scaled by the fixed
     * thermal efficiency scalar (eta_th = 0.70) to give a first-order
     * estimate of net useful thermal power. The distance-loss term captures
     * beam-divergence effects with increasing mirror-to-receiver distance.
     *
     * @param state  current simulation state (geometry + time)
     * @return       estimated useful thermal power [W]
     */
    public double calculateTotalEnergy(SimulationState state) {
        SolarPosition sunPos = calculateSolarPosition(state.getCurrentTime());
        double dni = sunPos.getSolarIntensity();

        if (sunPos.getAltitudeAngle() <= 0.0) return 0.0;

        List<MirrorPosition> mirrors = state.getMirrorPositions();
        ShadingDetector shadingDetector = new ShadingDetector();
        double totalEnergy = 0.0;

        for (MirrorPosition mirror : mirrors) {

            // Mirror aperture area [m²]  (dimensions stored in cm)
            double mirrorArea = (state.getReflectorWidth() / 100.0)
                              * (state.getReflectorLength() / 100.0);

            // Optical efficiency components
            double cosineEff   = calculateCosineEfficiency(mirror, sunPos);
            double spillageEff = calculateSpillageLoss(mirror, state);
            double blockingEff = shadingDetector.calculateBlockingAndShadingLoss(
                                     mirror, mirrors, state, sunPos);

            // Distance-loss term — captures beam-divergence penalty with
            // increasing mirror-to-receiver slant distance (see Section 2.4)
            double distanceLoss = calculateDistanceLoss(mirror, state);

            // Per-mirror energy contribution
            double mirrorEnergy = dni
                    * mirrorArea
                    * MIRROR_REFLECTIVITY
                    * cosineEff
                    * spillageEff
                    * blockingEff
                    * RECEIVER_ABSORPTIVITY
                    * SHADING_FACTOR
                    * distanceLoss
                    * THERMAL_EFFICIENCY;   // first-order thermal correction

            totalEnergy += mirrorEnergy;
        }

        return totalEnergy;
    }

    // ================================================================
    // 3.  OPTICAL EFFICIENCY COMPONENTS
    // ================================================================

    /**
     * Distance-loss factor.
     *
     * Models the decrease in effective optical throughput as the slant
     * distance between mirror and receiver increases, capturing first-order
     * beam-divergence effects without probabilistic ray-tracing.
     * The reference distance is set to the minimum geometrically realisable
     * slant distance given the system bounds.
     *
     * NOTE: The ad-hoc Gaussian height-efficiency factor previously present
     * in this class has been removed (see class-level Javadoc).
     */
    private double calculateDistanceLoss(MirrorPosition mirror, SimulationState state) {
        double receiverHeight = state.getReceiverHeight() / 100.0; // cm → m
        double mirrorX        = Math.abs(mirror.getXOffset()) / 100.0;

        double distance    = Math.sqrt(receiverHeight*receiverHeight + mirrorX*mirrorX);
        double minDistance = Math.sqrt(
                (MIN_RECEIVER_HEIGHT / 100.0) * (MIN_RECEIVER_HEIGHT / 100.0)
                + (MIN_MIRROR_SPACING / 100.0) * (MIN_MIRROR_SPACING / 100.0));

        return Math.pow(DISTANCE_LOSS_FACTOR, (distance / minDistance - 1.0));
    }

    /**
     * Cosine efficiency  eta_cos,i = |s_3D . n_3D,i|  — Eq. (9)
     */
    public double calculateCosineEfficiency(MirrorPosition mirror, SolarPosition sunPos) {
        double sunAltRad = Math.toRadians(sunPos.getAltitudeAngle());
        double sunAzRad  = Math.toRadians(sunPos.getAzimuthAngle());

        double[] sunVec = {
            -Math.cos(sunAltRad) * Math.sin(sunAzRad),
             Math.cos(sunAltRad) * Math.cos(sunAzRad),
             Math.sin(sunAltRad)
        };

        double tiltRad = Math.toRadians(mirror.getRotationAngle());
        double[] mirrorNormal = {
             Math.sin(tiltRad),
             0.0,
             Math.cos(tiltRad)
        };

        double dot = 0.0;
        for (int i = 0; i < 3; i++) dot += sunVec[i] * mirrorNormal[i];
        return Math.abs(dot);
    }

    /**
     * First-order spillage factor  f_spill,i = min(1, D_r / w_beam,i)
     *
     * Beam width at receiver plane:
     *   w_beam,i = w_mirror + 2 * d_i * tan(beamSpreadAngle) + additionalSpread
     *
     * where beamSpreadAngle is based on the sun's angular half-width (0.53°).
     * This is a simplified first-order approximation; a full Gaussian optical
     * error convolution is planned for future work.
     */
    public double calculateSpillageLoss(MirrorPosition mirror, SimulationState state) {
        double mirrorWidth      = state.getReflectorWidth()    / 100.0; // cm → m
        double receiverDiameter = state.getReceiverDiameter()  / 100.0;
        double receiverHeight   = state.getReceiverHeight()    / 100.0;
        double horizontalDist   = Math.abs(mirror.getXOffset() / 100.0);

        double beamSpreadAngle = Math.toRadians(SUN_ANGULAR_WIDTH);

        // Beam width at receiver: mirror projection + height-based spread
        double beamWidth = mirrorWidth + 2.0 * receiverHeight * Math.tan(beamSpreadAngle);

        // Additional spread due to slant distance
        double distance = Math.sqrt(receiverHeight*receiverHeight
                                    + horizontalDist*horizontalDist);
        beamWidth += distance * Math.tan(beamSpreadAngle);

        // Spillage factor
        double effectiveWidth  = Math.min(beamWidth, receiverDiameter);
        double spillageRatio   = effectiveWidth / beamWidth;
        double rotationAngleR  = Math.abs(Math.toRadians(mirror.getRotationAngle()));
        double angularCorrection = Math.cos(rotationAngleR);

        return Math.min(1.0, spillageRatio * angularCorrection);
    }

    // ================================================================
    // 4.  ATMOSPHERIC HELPERS
    // ================================================================

    private double calculateAirMass(double altitudeDeg) {
        if (altitudeDeg <= 0.0) return 40.0;
        double zenith    = 90.0 - altitudeDeg;
        double cosZenith = Math.cos(Math.toRadians(zenith));
        return 1.0 / (cosZenith + 0.50572 * Math.pow(96.07995 - zenith, -1.6364));
    }

    private double calculateAtmosphericRefraction(double altitudeDeg) {
        if (altitudeDeg > 85.0) return 0.0;
        double te = Math.tan(Math.toRadians(altitudeDeg));
        if (altitudeDeg > 5.0)
            return 58.1/te - 0.07/(te*te*te) + 0.000086/Math.pow(te, 5.0);
        if (altitudeDeg > -0.575)
            return 1735.0 + altitudeDeg*(-518.2 + altitudeDeg
                   *(103.4 + altitudeDeg*(-12.79 + altitudeDeg*0.711)));
        return -20.774 / te;
    }

    // ================================================================
    // 5.  SUNRISE / SUNSET  (unchanged)
    // ================================================================
    public DaylightTimes calculateSunriseSunset(LocalDate date) {
        int dayOfYear = date.getDayOfYear();
        double declination = -23.45
                * Math.cos(Math.toRadians(360.0 / 365.0 * (dayOfYear + 10)));

        double latRad = this.latitude;
        double decRad = Math.toRadians(declination);
        double hourAngle = Math.toDegrees(Math.acos(
                (-Math.sin(Math.toRadians(-0.833))
                 - Math.sin(latRad) * Math.sin(decRad))
                / (Math.cos(latRad) * Math.cos(decRad))));

        double Bday = 360.0 * (dayOfYear - 81) / 365.0;
        double EoT  = 9.87*Math.sin(Math.toRadians(2*Bday))
                    - 7.53*Math.cos(Math.toRadians(Bday))
                    - 1.50*Math.sin(Math.toRadians(Bday));

        int    zoneDiff       = 3;
        double timeCorrection = EoT + 4.0*Math.toDegrees(this.longitude) - 60.0*zoneDiff;

        double sunriseMin = 720.0 - 4.0*hourAngle - timeCorrection;
        double sunsetMin  = 720.0 + 4.0*hourAngle - timeCorrection;

        int srH = ((int)(sunriseMin/60)) % 24; if (srH < 0) srH += 24;
        int srM = (int) Math.round(sunriseMin % 60);
        if (srM == 60) { srM = 0; srH++; }

        int ssH = ((int)(sunsetMin/60)) % 24; if (ssH < 0) ssH += 24;
        int ssM = (int) Math.round(sunsetMin % 60);
        if (ssM == 60) { ssM = 0; ssH++; }

        return new DaylightTimes(
                date.atTime(srH, srM),
                date.atTime(ssH, ssM));
    }
}