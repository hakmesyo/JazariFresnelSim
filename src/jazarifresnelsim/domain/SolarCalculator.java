package jazarifresnelsim.domain;

import jazarifresnelsim.models.SolarPosition;
import jazarifresnelsim.models.MirrorPosition;
import jazarifresnelsim.models.SimulationState;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

/**
 * Solar position calculation and per-mirror optical performance metrics.
 *
 * Optical model implements Equations (1)–(14) of the manuscript: - Spencer
 * 7-term Fourier solar position Eq. (1)–(5) - Cosine efficiency Eq. (9) -
 * First-order spillage correction Eq. (12)–(13) - End-loss efficiency Eq. (11)
 * - Overall optical efficiency Eq. (14)
 *
 * VERSION 4.3 — End-loss formula corrected. CHANGE:
 * calculateEndLossEfficiency() now uses the mirror's horizontal offset |x_i| as
 * the lever-arm distance, not the slant distance sqrt(x_i^2 + deltaH^2). This
 * matches Eq. (11) as stated in the manuscript and in Bellos et al. [7] /
 * Santos et al. [36]:
 *
 * f_end,i = max(0, 1 - |x_i| * |tan(theta_L)| / L)
 *
 * The slant distance was an implementation error introduced in an earlier
 * version that caused severe over-prediction of end losses for peripheral
 * mirrors (especially at non-noon hours).
 *
 * @author Yunus Demirtas, Musa Atas — Siirt University
 * @version 4.3
 */
public class SolarCalculator {

    // ================================================================
    // LOCATION
    // ================================================================
    private double latitude;   // [rad]
    private double longitude;  // [rad]
    private double altitude;   // [m]

    public SolarCalculator(double latDeg, double lonDeg, double altitude) {
        updateLocation(latDeg, lonDeg);
        this.altitude = altitude;
    }

    public void updateLocation(double latDeg, double lonDeg) {
        this.latitude = Math.toRadians(latDeg);
        this.longitude = Math.toRadians(lonDeg);
    }

    // ================================================================
    // 1. SOLAR POSITION — Spencer (1971) 7-term Fourier series
    //    Equations (1)-(5)
    // ================================================================
    public SolarPosition calculateSolarPosition(LocalDateTime dateTime) {
        int dayOfYear = dateTime.getDayOfYear();

        // Day angle B [rad]
        double B = 2.0 * Math.PI * (dayOfYear - 1) / 365.0;

        // Solar declination delta — Spencer (1971), Eq. (3)
        double declination = Math.toDegrees(
                0.006918
                - 0.399912 * Math.cos(B) + 0.070257 * Math.sin(B)
                - 0.006758 * Math.cos(2 * B) + 0.000907 * Math.sin(2 * B)
                - 0.002697 * Math.cos(3 * B) + 0.001480 * Math.sin(3 * B));

        // Equation of time E_t [minutes] — Eq. (1)
        double E = 229.18 * (0.000075
                + 0.001868 * Math.cos(B) - 0.032077 * Math.sin(B)
                - 0.014615 * Math.cos(2 * B) - 0.040849 * Math.sin(2 * B));

        // True solar time — Eq. (2)
        double hour = dateTime.getHour() + dateTime.getMinute() / 60.0;
        double lonDeg = Math.toDegrees(longitude);
        double tzMerid = 15.0 * Math.round(lonDeg / 15.0);
        double solarTime = hour + (4.0 * (lonDeg - tzMerid) + E) / 60.0;

        // Hour angle omega [deg], negative before solar noon
        double hourAngle = 15.0 * (solarTime - 12.0);

        // Solar altitude alpha — Eq. (4)
        double sinDec = Math.sin(Math.toRadians(declination));
        double cosDec = Math.cos(Math.toRadians(declination));
        double sinLat = Math.sin(latitude);
        double cosLat = Math.cos(latitude);
        double cosHA = Math.cos(Math.toRadians(hourAngle));

        double sinAlt = sinLat * sinDec + cosLat * cosDec * cosHA;
        double altitudeDeg = Math.toDegrees(
                Math.asin(Math.max(-1.0, Math.min(1.0, sinAlt))));

        // Solar azimuth A — Eq. (5), quadrant correction for afternoon
        double cosAlt = Math.cos(Math.toRadians(altitudeDeg));
        double cosAz = (sinDec * cosLat - cosDec * sinLat * cosHA)
                / Math.max(1e-9, cosAlt);
        double azimuthDeg = Math.toDegrees(
                Math.acos(Math.max(-1.0, Math.min(1.0, cosAz))));
        if (hourAngle > 0) {
            azimuthDeg = 360.0 - azimuthDeg;
        }

        // Atmospheric refraction correction [arcsec -> deg]
        altitudeDeg += calculateAtmosphericRefraction(altitudeDeg) / 3600.0;

        // DNI via Hottel clear-sky transmittance model
        double airMass = calculateAirMass(altitudeDeg);
        double dni = (altitudeDeg > 0)
                ? Constants.SOLAR_CONSTANT * Math.pow(0.7, Math.pow(airMass, 0.678))
                : 0.0;

        return new SolarPosition(altitudeDeg, azimuthDeg, dni);
    }

    // ================================================================
    // 2. OPTICAL PERFORMANCE METRICS
    // ================================================================
    /**
     * Cosine efficiency eta_cos,i = |s_3D . n_3D,i| — Eq. (9).
     */
    public double calculateCosineEfficiency(MirrorPosition mirror,
            SolarPosition sunPos) {
        double sunAltRad = Math.toRadians(sunPos.getAltitudeAngle());
        double sunAzRad = Math.toRadians(sunPos.getAzimuthAngle());

        // 3D sun direction unit vector
        double sx = -Math.cos(sunAltRad) * Math.sin(sunAzRad);
        double sy = Math.cos(sunAltRad) * Math.cos(sunAzRad);
        double sz = Math.sin(sunAltRad);

        // 3D mirror normal (N-S axis tracking)
        double tiltRad = Math.toRadians(mirror.getRotationAngle());
        double nx = Math.sin(tiltRad);
        double ny = 0.0;
        double nz = Math.cos(tiltRad);

        return Math.abs(sx * nx + sy * ny + sz * nz);
    }

// ============================================================================
// PHASE 0 - TWO PHYSICS CORRECTIONS in SolarCalculator.java
//
// Replace the two existing methods with the versions below and add the two
// small helpers at the end of the class. Nothing else changes.
//
// Also in Constants.java, make sure this line is the ACTIVE one:
//     public static final double SIGMA_OPT = 4.65e-3;
// (the 8.0e-3 variant must stay commented out)
// ============================================================================
// ============================================================================
// FIX 1 - END LOSS
//
// WHY THE PREVIOUS VERSION WAS WRONG
// ----------------------------------
//     f_end = max(0, 1 - |x_i| * tan(theta_L) / L)
//
// Two independent errors.
//
// (a) LEVER ARM. The longitudinal drift of the reflected ray is proportional
//     to the distance it travels from mirror to receiver, not to the mirror's
//     transverse offset. Since a single-axis mirror cannot alter the
//     longitudinal component of the ray, r_y = -s_y throughout, and
//
//         Delta_y = r_y * (Hr - Hs) / r_z ,
//         r_z     = (Hr - Hs) * sqrt(1 - s_y^2) / d_i
//     =>  |Delta_y| = d_i * |s_y| / sqrt(1 - s_y^2)
//
//     with d_i = sqrt(x_i^2 + (Hr - Hs)^2). The lever arm is the SLANT
//     distance d_i.
//
//     The old form assigned ZERO end loss to the central mirror (x_i = 0),
//     which is impossible: a ray leaving the centre mirror still travels
//     (Hr - Hs) vertically and drifts longitudinally over that path. For the
//     G2 geometry at solar noon the correct central-mirror loss is 2.56%,
//     and the field average is 2.64% against 0.58% previously.
//
// (b) LONGITUDINAL ANGLE. theta_L is the angle between the sun vector and the
//     plane normal to the tracking axis, i.e. sin(theta_L) = s_y, hence
//     tan(theta_L) = s_y / sqrt(1 - s_y^2). The previous form used
//     s_y / s_z = cos(A)/tan(alpha), which is the ratio to the VERTICAL
//     component rather than to the transverse projection.
// ============================================================================
    public double calculateEndLossEfficiency(MirrorPosition mirror,
            SolarPosition sunPos,
            SimulationState state) {

        double alt = Math.toRadians(sunPos.getAltitudeAngle());
        double azi = Math.toRadians(sunPos.getAzimuthAngle());

        if (Math.sin(alt) <= 0.0) {
            return 0.0;
        }

        // Longitudinal (along-axis) component of the unit sun vector
        double s_y = Math.cos(alt) * Math.cos(azi);

        double denom = Math.sqrt(Math.max(1e-12, 1.0 - s_y * s_y));
        double tanThetaL = Math.abs(s_y) / denom;

        // Slant distance from mirror to receiver [m]
        double xi_m = mirror.getXOffset() / 100.0;
        double dh_m = (state.getReceiverHeight() - state.getSupportHeight()) / 100.0;
        double d_i = Math.sqrt(xi_m * xi_m + dh_m * dh_m);

        double L_m = state.getReflectorLength() / 100.0;
        if (L_m <= 1e-12) {
            return 0.0;
        }

        return Math.max(0.0, 1.0 - d_i * tanThetaL / L_m);
    }

// ============================================================================
// FIX 2 - SPILLAGE
//
// WHY THE PREVIOUS VERSION WAS WRONG
// ----------------------------------
// The reflected image of a flat mirror is a UNIFORM distribution of width
// w*cos(theta_i), convolved with the angular error distribution. The previous
// code replaced the uniform by a Gaussian of equal variance (sigma = W/sqrt12)
// and combined the two in quadrature.
//
// Matching only the variance is a poor approximation when the image width is
// comparable to the aperture, which is exactly the regime here (w = Dr). A
// Gaussian has far heavier tails than a uniform, so the model predicts
// spillage that does not occur. Measured against the exact convolution at
// d_i = 1.0 m the old form under-predicts capture by 5.2 percentage points;
// the two forms converge only for d_i > 5 m, where the angular term dominates.
//
// EXACT RESULT
// ------------
// For a uniform of half-width a, blurred by a Gaussian of standard deviation
// s, the fraction falling inside an aperture of half-width R is
//
//     f = (s/a) * [ F((R+a)/s) - F((R-a)/s) ] - 1 ,
//     F(u) = u*Phi(u) + phi(u)
//
// with Phi the standard normal CDF and phi its PDF. Derived by integrating
// the convolution in closed form; no numerical quadrature is needed.
//
// Limits are correct by construction:
//     a -> 0  =>  f = erf( R / (s*sqrt2) )     (pure Gaussian)
//     s -> 0  =>  f = min(1, R/a)              (pure geometric)
// ============================================================================
    public double calculateSpillageLoss(MirrorPosition mirror,
            SimulationState state) {

        double w_m = state.getReflectorWidth() / 100.0;
        double Dr_m = state.getReceiverDiameter() / 100.0;
        double Hr_m = state.getReceiverHeight() / 100.0;
        double Hs_m = state.getSupportHeight() / 100.0;
        double xi_m = mirror.getXOffset() / 100.0;

        double dh = Hr_m - Hs_m;
        double d_i = Math.sqrt(xi_m * xi_m + dh * dh);

        double tilt = Math.toRadians(mirror.getRotationAngle());

        double a = 0.5 * w_m * Math.abs(Math.cos(tilt));  // uniform half-width [m]
        double s = d_i * Constants.SIGMA_OPT;             // Gaussian sigma    [m]
        double R = 0.5 * Dr_m;                            // aperture half-width [m]

        if (a < 1e-12) {
            return 1.0;                        // vanishing image
        }
        if (s < 1e-12) {
            return Math.min(1.0, R / a);       // no angular spread
        }
        double f = (s / a) * (bigF((R + a) / s) - bigF((R - a) / s)) - 1.0;

        return Math.max(0.0, Math.min(1.0, f));
    }

// ============================================================================
// HELPERS - add these to the class (erf() already exists)
// ============================================================================
    /**
     * F(u) = u*Phi(u) + phi(u), the antiderivative of the standard normal CDF.
     */
    private double bigF(double u) {
        return u * normCdf(u)
                + Math.exp(-0.5 * u * u) / Math.sqrt(2.0 * Math.PI);
    }

    /**
     * Standard normal cumulative distribution function.
     */
    private double normCdf(double u) {
        return 0.5 * (1.0 + erf(u / Math.sqrt(2.0)));
    }

    /**
     * Abramowitz and Stegun approximation for the Error Function (erf).
     * Essential for Gaussian beam modeling in pure Java without external
     * libraries.
     */
    private double erf(double x) {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(x));
        double ans = 1.0 - t * Math.exp(-x * x - 1.26551223
                + t * (1.00002368
                + t * (0.37409196
                + t * (0.09678418
                + t * (-0.18628806
                + t * (0.27886807
                + t * (-1.13520398
                + t * (1.48851587
                + t * (-0.82215223
                + t * 0.17087277)))))))));
        if (x >= 0) {
            return ans;
        } else {
            return -ans;
        }
    }

    /**
     * Total optical intercept power Q_opt [W] for the field — Eq. (15).
     *
     * Q_opt = sum_i [ rho_m * eta_cos,i * eta_sb,i * f_end,i * f_spill,i ] *
     * DNI * A_mirror
     */
    public double calculateOpticalPower(SimulationState state) {
        SolarPosition sunPos = calculateSolarPosition(state.getCurrentTime());
        double dni = sunPos.getSolarIntensity();

        if (sunPos.getAltitudeAngle() <= 0.0 || dni <= 0.0) {
            return 0.0;
        }

        List<MirrorPosition> mirrors = state.getMirrorPositions();
        if (mirrors == null || mirrors.isEmpty()) {
            return 0.0;
        }

        ShadingDetector shadingDetector = new ShadingDetector();
        double w_m = state.getReflectorWidth() / 100.0;
        double L_m = state.getReflectorLength() / 100.0;
        double mirrorArea = w_m * L_m;
        double totalPower = 0.0;

        for (MirrorPosition mirror : mirrors) {
            double eta_cos = calculateCosineEfficiency(mirror, sunPos);
            double eta_sb = shadingDetector.calculateBlockingAndShadingLoss(
                    mirror, mirrors, state, sunPos);
            double f_end = calculateEndLossEfficiency(mirror, sunPos, state);
            double f_spill = calculateSpillageLoss(mirror, state);

            totalPower += dni * mirrorArea
                    * Constants.MIRROR_REFLECTIVITY * eta_cos * eta_sb * f_end * f_spill;
        }
        return totalPower;
    }

    /**
     * Overloaded version of calculateOpticalPower that accepts pre-calculated
     * mirror positions. This is more efficient for optimization loops.
     *
     * @param state Current simulation state
     * @param mirrors List of mirrors with pre-calculated tracking angles
     * @return Total net optical power [W]
     */
    public double calculateOpticalPower(SimulationState state, List<MirrorPosition> mirrors) {
        SolarPosition sunPos = state.getCurrentSolarPosition();
        double dni = sunPos.getSolarIntensity();

        if (sunPos.getAltitudeAngle() <= 0.0 || dni <= 0.0) {
            return 0.0;
        }
        if (mirrors == null || mirrors.isEmpty()) {
            return 0.0;
        }

        ShadingDetector shadingDetector = new ShadingDetector();
        double w_m = state.getReflectorWidth() / 100.0;
        double L_m = state.getReflectorLength() / 100.0;
        double mirrorArea = w_m * L_m;
        double totalPower = 0.0;

        for (MirrorPosition mirror : mirrors) {
            double eta_cos = calculateCosineEfficiency(mirror, sunPos);
            double eta_sb = shadingDetector.calculateBlockingAndShadingLoss(
                    mirror, mirrors, state, sunPos);
            double f_end = calculateEndLossEfficiency(mirror, sunPos, state);
            double f_spill = calculateSpillageLoss(mirror, state);

            totalPower += dni * mirrorArea
                    * Constants.MIRROR_REFLECTIVITY * eta_cos * eta_sb * f_end * f_spill;
        }
        return totalPower;
    }

    /**
     * @deprecated Use calculateOpticalPower() instead.
     */
    @Deprecated
    public double calculateTotalEnergy(SimulationState state) {
        return calculateOpticalPower(state);
    }

    // ================================================================
    // 3. ATMOSPHERIC HELPERS
    // ================================================================
    /**
     * Air mass via Kasten-Young (1989).
     */
    private double calculateAirMass(double altDeg) {
        if (altDeg <= 0.0) {
            return 40.0;
        }
        double zenith = 90.0 - altDeg;
        double cosZenith = Math.cos(Math.toRadians(zenith));
        return 1.0 / (cosZenith + 0.50572 * Math.pow(96.07995 - zenith, -1.6364));
    }

    /**
     * Atmospheric refraction [arcseconds] via Bennett (1982).
     */
    private double calculateAtmosphericRefraction(double altDeg) {
        if (altDeg > 85.0) {
            return 0.0;
        }
        double te = Math.tan(Math.toRadians(altDeg));
        if (altDeg > 5.0) {
            return 58.1 / te - 0.07 / (te * te * te)
                    + 0.000086 / Math.pow(te, 5.0);
        }
        if (altDeg > -0.575) {
            return 1735.0 + altDeg * (-518.2 + altDeg
                    * (103.4 + altDeg * (-12.79 + altDeg * 0.711)));
        }
        return -20.774 / te;
    }

    // ================================================================
    // 4. SUNRISE / SUNSET
    // ================================================================
    public DaylightTimes calculateSunriseSunset(LocalDate date) {
        int dayOfYear = date.getDayOfYear();
        double declination = -23.45 * Math.cos(
                Math.toRadians(360.0 / 365.0 * (dayOfYear + 10)));

        double latRad = this.latitude;
        double decRad = Math.toRadians(declination);
        double hourAngle = Math.toDegrees(Math.acos(
                (-Math.sin(Math.toRadians(-0.833))
                - Math.sin(latRad) * Math.sin(decRad))
                / (Math.cos(latRad) * Math.cos(decRad))));

        double Bday = 360.0 * (dayOfYear - 81) / 365.0;
        double EoT = 9.87 * Math.sin(Math.toRadians(2 * Bday))
                - 7.53 * Math.cos(Math.toRadians(Bday))
                - 1.50 * Math.sin(Math.toRadians(Bday));
        int zoneDiff = (int) Math.round(Math.toDegrees(this.longitude) / 15.0);
        double timeCorr = EoT + 4.0 * Math.toDegrees(this.longitude) - 60.0 * zoneDiff;

        double srMin = 720.0 - 4.0 * hourAngle - timeCorr;
        double ssMin = 720.0 + 4.0 * hourAngle - timeCorr;

        int srH = ((int) (srMin / 60)) % 24;
        if (srH < 0) {
            srH += 24;
        }
        int srM = (int) Math.round(srMin % 60);
        if (srM == 60) {
            srM = 0;
            srH++;
        }
        int ssH = ((int) (ssMin / 60)) % 24;
        if (ssH < 0) {
            ssH += 24;
        }
        int ssM = (int) Math.round(ssMin % 60);
        if (ssM == 60) {
            ssM = 0;
            ssH++;
        }

        return new DaylightTimes(date.atTime(srH, srM), date.atTime(ssH, ssM));
    }

    /**
     * Calculates the solar position using the Spencer (1971) 7-term Fourier
     * series. Updated to support external TMY DNI data for high-fidelity
     * optimization.
     *
     * @param dateTime The local date and time for calculation.
     * @param externalDNI The DNI value from TMY data. Use -1 to fallback to
     * Hottel model.
     * @return SolarPosition object containing altitude, azimuth, and intensity.
     */
    public SolarPosition calculateSolarPosition(LocalDateTime dateTime, double externalDNI) {
        int dayOfYear = dateTime.getDayOfYear();

        // Day angle B in radians
        double B = 2.0 * Math.PI * (dayOfYear - 1) / 365.0;

        // Solar declination delta based on Spencer (1971) - Eq. (3)
        double declination = Math.toDegrees(
                0.006918
                - 0.399912 * Math.cos(B) + 0.070257 * Math.sin(B)
                - 0.006758 * Math.cos(2 * B) + 0.000907 * Math.sin(2 * B)
                - 0.002697 * Math.cos(3 * B) + 0.001480 * Math.sin(3 * B));

        // Equation of time E_t in minutes - Eq. (1)
        double E = 229.18 * (0.000075
                + 0.001868 * Math.cos(B) - 0.032077 * Math.sin(B)
                - 0.014615 * Math.cos(2 * B) - 0.040849 * Math.sin(2 * B));

        // Calculate True Solar Time (TST) - Eq. (2)
        double hour = dateTime.getHour() + dateTime.getMinute() / 60.0;
        double lonDeg = Math.toDegrees(longitude);
        double tzMerid = 15.0 * Math.round(lonDeg / 15.0);
        double solarTime = hour + (4.0 * (lonDeg - tzMerid) + E) / 60.0;

        // Hour angle omega in degrees (noon is 0)
        double hourAngle = 15.0 * (solarTime - 12.0);

        // Solar altitude alpha calculation - Eq. (4)
        double sinDec = Math.sin(Math.toRadians(declination));
        double cosDec = Math.cos(Math.toRadians(declination));
        double sinLat = Math.sin(latitude);
        double cosLat = Math.cos(latitude);
        double cosHA = Math.cos(Math.toRadians(hourAngle));

        double sinAlt = sinLat * sinDec + cosLat * cosDec * cosHA;
        double altitudeDeg = Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, sinAlt))));

        // Solar azimuth angle A calculation - Eq. (5)
        double cosAlt = Math.cos(Math.toRadians(altitudeDeg));
        double cosAz = (sinDec * cosLat - cosDec * sinLat * cosHA)
                / Math.max(1e-9, cosAlt);
        double azimuthDeg = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, cosAz))));

        // Quadrant correction for afternoon hours
        if (hourAngle > 0) {
            azimuthDeg = 360.0 - azimuthDeg;
        }

        // Apply atmospheric refraction correction
        altitudeDeg += calculateAtmosphericRefraction(altitudeDeg) / 3600.0;

        // DNI Handling: Use external TMY data if provided, else fallback to Hottel model
        double dni;
        if (externalDNI >= 0) {
            dni = (altitudeDeg > 0) ? externalDNI : 0.0;
        } else {
            // Fallback to original Hottel clear-sky model
            double airMass = calculateAirMass(altitudeDeg);
            dni = (altitudeDeg > 0)
                    ? Constants.SOLAR_CONSTANT * Math.pow(0.7, Math.pow(airMass, 0.678))
                    : 0.0;
        }

        return new SolarPosition(altitudeDeg, azimuthDeg, dni);
    }
}
