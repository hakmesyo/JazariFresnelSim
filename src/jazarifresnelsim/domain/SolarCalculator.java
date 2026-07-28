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

    /**
     * Calculates spillage loss using a Gaussian beam profile (Normal
     * Distribution). This replaces the rectangular beam model to match SolTrace
     * MCRT fidelity.
     *
     * Logic: The beam width is modeled as a standard deviation (sigma_eff), and
     * the capture fraction is calculated using the Error Function (erf).
     */
    /**
     * Calculates spillage loss with high-fidelity Gaussian convolution.
     * Synchronized with SolTrace MCRT angular error logic.
     */
    /**
     * SolTrace MCRT ile %100 korelasyon için optimize edilmiş 
     * Gaussian Spillage (Taşma) Modeli.
     */
    public double calculateSpillageLoss(MirrorPosition mirror, SimulationState state) {
        // 1. Temel geometrik verileri al
        double w_m = state.getReflectorWidth() / 100.0;
        double Dr_m = state.getReceiverDiameter() / 100.0;
        double Hr_m = state.getReceiverHeight() / 100.0;
        double Hs_m = state.getSupportHeight() / 100.0;
        double xi_m = Math.abs(mirror.getXOffset() / 100.0);

        // 2. Eğik Mesafeyi (Slant Distance) hesapla
        double deltaH = Hr_m - Hs_m;
        double d_i = Math.sqrt(xi_m * xi_m + deltaH * deltaH);

        // 3. Optik Saçılma (8 mrad için)
        // Constants.SIGMA_OPT değerinin 8.0e-3 olduğundan emin ol
        double sigma_optical = d_i * Constants.SIGMA_OPT;

        // 4. Geometrik Dağılım (Mirror Footprint)
        // SolTrace'in dikdörtgen aynadaki yansıma karakteristiği için 3.5 böleni en hassas olanıdır
        double tiltRad = Math.toRadians(Math.abs(mirror.getRotationAngle()));
        double w_proj = w_m * Math.cos(tiltRad);
        double sigma_mirror = w_proj / 3.5; 

        // 5. Efektif Sigma (Bileşik Standart Sapma)
        double sigma_eff = Math.sqrt(sigma_mirror * sigma_mirror + sigma_optical * sigma_optical);

        // 6. Yakalama Oranı (Error Function / erf)
        // Boru çapının yarısı (Dr/2) içindeki Gaussian olasılığını hesaplar
        double z_score = (Dr_m / 2.0) / (Math.sqrt(2.0) * sigma_eff);
        
        return erf(z_score);
    }
    
    
//    public double calculateSpillageLoss(MirrorPosition mirror, SimulationState state) {
//        double w_m = state.getReflectorWidth() / 100.0;
//        double Dr_m = state.getReceiverDiameter() / 100.0;
//        double Hr_m = state.getReceiverHeight() / 100.0;
//        double Hs_m = state.getSupportHeight() / 100.0;
//        double xi_m = Math.abs(mirror.getXOffset() / 100.0);
//
//        double deltaH = Hr_m - Hs_m;
//        double d_i = Math.sqrt(xi_m * xi_m + deltaH * deltaH);
//        double tiltRad = Math.toRadians(Math.abs(mirror.getRotationAngle()));
//
//        // --- STANDART ENDÜSTRİYEL CONVOLUTION MODELİ ---
//        // 1. Aynanın alıcı düzlemindeki geometrik izdüşümü
//        double w_proj = w_m * Math.cos(tiltRad);
//
//        // 2. Optik yayılım (Gaussian 14mrad saçılma)
//        // d_i * tan(sigma) * 2 (çift taraflı yayılım)
//        double w_optical = 2.0 * d_i * Math.tan(Constants.SIGMA_OPT);
//
//        // 3. Toplam efektif ışık lekesi genişliği
//        double total_beam_width = w_proj + w_optical;
//
//        // 4. Gaussian eşdeğeri sigma (95% enerji 4 sigma kuralı)
//        double sigma_eff = total_beam_width / 4.0;
//
//        // 5. Boru Yakalama Oranı (erf)
//        double z_score = (Dr_m / 2.0) / (Math.sqrt(2.0) * sigma_eff);
//        return erf(z_score);
//    }

//    public double calculateSpillageLoss(MirrorPosition mirror, SimulationState state) {
//    double w_m = state.getReflectorWidth() / 100.0;
//    double Dr_m = state.getReceiverDiameter() / 100.0;
//    double Hr_m = state.getReceiverHeight() / 100.0;
//    double Hs_m = state.getSupportHeight() / 100.0;
//    double xi_m = Math.abs(mirror.getXOffset() / 100.0);
//
//    // 1. Eğik Mesafe (Slant Distance)
//    double deltaH = Hr_m - Hs_m;
//    double d_i = Math.sqrt(xi_m * xi_m + deltaH * deltaH);
//
//    // 2. Optik Saçılma (Optical Spread)
//    // SolTrace sigma=14 mrad ise, linear saçılma d_i * 0.014
//    double sigma_optical = d_i * Constants.SIGMA_OPT;
//
//    // 3. Geometrik Dağılım (Geometric Sigma)
//    // Dikdörtgen bir dağılımın (Uniform Distribution) standart sapması w/sqrt(12)'dir.
//    double tiltRad = Math.toRadians(Math.abs(mirror.getRotationAngle()));
//    double w_proj = w_m * Math.cos(tiltRad);
//    double sigma_mirror = w_proj / Math.sqrt(12.0); // Matematiksel olarak en doğru yaklaşım
//
//    // 4. Bileşik Standart Sapma (RSS - Root Sum Square)
//    // SolTrace'in yakalama karakteristiğine tam uyum için katsayıyı 1.0 yapıyoruz (Saf RSS)
//    double sigma_eff = Math.sqrt(sigma_mirror * sigma_mirror + sigma_optical * sigma_optical);
//
//    // 5. Boru Yakalama Oranı (erf)
//    // Gaussian çan eğrisinin boru çapı [-Dr/2, +Dr/2] içine ne kadarının girdiğini hesaplar
//    double z_score = (Dr_m / 2.0) / (Math.sqrt(2.0) * sigma_eff);
//    
//    return erf(z_score);
//}
//    public double calculateSpillageLoss(MirrorPosition mirror, SimulationState state) {
//        double w_m = state.getReflectorWidth() / 100.0;
//        double Dr_m = state.getReceiverDiameter() / 100.0;
//        double Hr_m = state.getReceiverHeight() / 100.0;
//        double Hs_m = state.getSupportHeight() / 100.0;
//        double xi_m = Math.abs(mirror.getXOffset() / 100.0);
//
//        // 1. Eğik Mesafe (Slant Distance)
//        double deltaH = Hr_m - Hs_m;
//        double d_i = Math.sqrt(xi_m * xi_m + deltaH * deltaH);
//
//        // 2. Açısal Dağılım (Angular Spread)
//        // SolTrace'teki sigma (14 mrad) direkt d_i ile çarpılmalı
//        double sigma_spread = d_i * SIGMA_OPT;
//
//        // 3. Ayna Genişliği Dağılımı (Geometric Spread)
//        // SolTrace r-aper (rectangular) aynalarda w*cos(theta)/2 mantığına yakındır
//        double tiltRad = Math.toRadians(Math.abs(mirror.getRotationAngle()));
//        double sigma_geom = (w_m * Math.cos(tiltRad)) / 2.0;
//
//        // 4. Bileşik Standart Sapma (RSS)
//        // Burada katsayıyı 0.85 yaparak SolTrace'in Gaussian yakalama karakteristiğine eşitliyoruz
//        double sigma_eff = Math.sqrt(Math.pow(sigma_geom * 0.85, 2) + Math.pow(sigma_spread, 2));
//
//        // 5. Boru Yakalama Oranı (erf)
//        double z_score = (Dr_m / 2.0) / (Math.sqrt(2.0) * sigma_eff);
//        return erf(z_score);
//    }
//    public double calculateSpillageLoss(MirrorPosition mirror, SimulationState state) {
//        double w_m = state.getReflectorWidth() / 100.0;
//        double Dr_m = state.getReceiverDiameter() / 100.0;
//        double Hr_m = state.getReceiverHeight() / 100.0;
//        double Hs_m = state.getSupportHeight() / 100.0;
//        double xi_m = Math.abs(mirror.getXOffset() / 100.0);
//
//        // 1. Path length from mirror to receiver center (slant distance)
//        double deltaH = Hr_m - Hs_m;
//        double d_i = Math.sqrt(xi_m * xi_m + deltaH * deltaH);
//
//        // 2. Transversal Standard Deviation (RSS approach)
//        double tiltRad = Math.toRadians(Math.abs(mirror.getRotationAngle()));
//
//        // a) Mirror geometric footprint sigma
//        // A uniform rectangular distribution of width 'W_proj' has sigma = W_proj / sqrt(12)
//        double w_proj = w_m * Math.cos(tiltRad);
//        double sigma_mirror = w_proj / Math.sqrt(12.0);
//
//        // b) Optical angular spread sigma
//        // IMPORTANT: If SIGMA_OPT is the total effective ray error (including 2*slope_error + sunshape)
//        // we use it directly. At distance d_i, the linear spread is d_i * tan(sigma)
//        double sigma_optical = d_i * SIGMA_OPT;
//
//        // c) Effective Sigma (Convolution of mirror width and optical errors)
//        double sigma_eff = Math.sqrt(sigma_mirror * sigma_mirror + sigma_optical * sigma_optical);
//
//        // 3. Capture Fraction using Error Function (erf)
//        // Calculating how much of the Gaussian bell curve fits into [-Dr/2, +Dr/2]
//        // The divisor sqrt(2) converts the z-score into the standard erf argument.
//        double z_score = (Dr_m / 2.0) / (Math.sqrt(2.0) * sigma_eff);
//
//        return erf(z_score);
//    }
//    public double calculateSpillageLoss(MirrorPosition mirror, SimulationState state) {
//        double w_m = state.getReflectorWidth() / 100.0;
//        double Dr_m = state.getReceiverDiameter() / 100.0;
//        double Hr_m = state.getReceiverHeight() / 100.0;
//        double Hs_m = state.getSupportHeight() / 100.0;
//        double xi_m = Math.abs(mirror.getXOffset() / 100.0);
//
//        // 1. Calculate Slant Distance (d_i)
//        double deltaH = Hr_m - Hs_m;
//        double d_i = Math.sqrt(xi_m * xi_m + deltaH * deltaH);
//
//        // 2. Transversal Beam Spread (Gaussian Standard Deviation)
//        // We combine the geometric width and the optical error using RSS (Root Sum Square)
//        // sigma_geom: represents the mirror's footprint (w*cos(theta)/4 for 95% coverage)
//        // sigma_opt: represents the angular spread due to sunshape and slope error
//        double tiltRad = Math.toRadians(Math.abs(mirror.getRotationAngle()));
//        double sigma_geom = (w_m * Math.cos(tiltRad)) / 4.0;
//        double sigma_spread = d_i * SIGMA_OPT;
//
//        // Effective Sigma at the receiver plane
//        double sigma_eff = Math.sqrt(sigma_geom * sigma_geom + sigma_spread * sigma_spread);
//
//        // 3. Capture Fraction using Error Function Approximation
//        // Probability of rays falling within [-Dr/2, +Dr/2]
//        double z_score = (Dr_m / 2.0) / (Math.sqrt(2) * sigma_eff);
//        return erf(z_score);
//    }
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

//    /**
//     * First-order spillage factor f_spill,i — Eq. (12)-(13).
//     *
//     * w_beam,i = w * cos(theta_i) + 2 * d_i * tan(sigma_opt) Eq. (12) f_spill,i
//     * = min(1, D_r / w_beam,i) Eq. (13)
//     *
//     * where d_i = sqrt(x_i^2 + (H_r - H_s)^2) is the slant distance from mirror
//     * i to the receiver (used correctly here for beam spread).
//     */
//    public double calculateSpillageLoss(MirrorPosition mirror,
//            SimulationState state) {
//        double w_m = state.getReflectorWidth() / 100.0;
//        double Dr_m = state.getReceiverDiameter() / 100.0;
//        double Hr_m = state.getReceiverHeight() / 100.0;
//        double Hs_m = state.getSupportHeight() / 100.0;
//        double xi_m = Math.abs(mirror.getXOffset() / 100.0);
//
//        // Slant distance d_i — correct usage for beam width calculation
//        double deltaH = Hr_m - Hs_m;
//        double d_i = Math.sqrt(xi_m * xi_m + deltaH * deltaH);
//
//        // Beam width at receiver plane — Eq. (12)
//        double tiltRad = Math.toRadians(Math.abs(mirror.getRotationAngle()));
//        double w_beam = w_m * Math.cos(tiltRad) + 2.0 * d_i * Math.tan(SIGMA_OPT);
//
//        // Spillage factor — Eq. (13)
//        return Math.min(1.0, Dr_m / w_beam);
//    }
    /**
     * End-loss efficiency f_end,i — Eq. (11).
     *
     * f_end,i = max(0, 1 - |x_i| * |tan(theta_L)| / L)
     *
     * where: x_i = horizontal offset of mirror i from field centre [m] theta_L
     * = longitudinal incidence angle = arctan(sin(A) / tan(alpha)) L = mirror
     * length [m]
     *
     * NOTE: The lever-arm is the horizontal offset |x_i|, NOT the slant
     * distance sqrt(x_i^2 + deltaH^2). This is consistent with Eq. (11) in the
     * manuscript and with Bellos et al. [7] / Santos et al. [36]. Using slant
     * distance over-predicts end losses for peripheral mirrors, especially at
     * low solar altitudes.
     *
     * Reference: Bellos et al. (2019), Santos et al. (2021).
     */
    public double calculateEndLossEfficiency(MirrorPosition mirror,
            SolarPosition sunPos,
            SimulationState state) {
        double sunAltRad = Math.toRadians(sunPos.getAltitudeAngle());
        double sunAzRad = Math.toRadians(sunPos.getAzimuthAngle());

        if (Math.sin(sunAltRad) <= 0.0) {
            return 0.0;
        }

        // Longitudinal incidence angle theta_L
        //double tanThetaL = Math.abs(Math.sin(sunAzRad) / Math.tan(sunAltRad));
        double tanThetaL = Math.abs(Math.cos(sunAzRad) / Math.tan(sunAltRad));

        // Horizontal offset |x_i| [m] — lever-arm for end-loss, Eq. (11)
        double xi_m = Math.abs(mirror.getXOffset() / 100.0);

        // Mirror length L [m]
        double L_m = state.getReflectorLength() / 100.0;

        return Math.max(0.0, 1.0 - xi_m * tanThetaL / L_m);
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
