package jazarifresnelsim.domain;

import jazarifresnelsim.models.SolarPosition;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import jazarifresnelsim.models.MirrorPosition;
import jazarifresnelsim.models.SimulationState;
import static jazarifresnelsim.optimization.problem.DesignParameters.MIN_MIRROR_SPACING;
import static jazarifresnelsim.optimization.problem.DesignParameters.MIN_RECEIVER_HEIGHT;

public class SolarCalculator {

    private static final double SOLAR_CONSTANT = 1361.0; // W/m²
    private static final double MIRROR_REFLECTIVITY = 0.92;  // Mirror reflection efficiency
    private static final double RECEIVER_ABSORPTIVITY = 0.95; // Receiver tube absorption efficiency
    private static final double SHADING_FACTOR = 0.95; // General shading factor
    private static final double SUN_ANGULAR_WIDTH = 0.53; // Sun's angular width in degrees

    private double latitude;  // in radians
    private double longitude; // in radians
    private double altitude; // in meters
    
    // Sabitler
    private static final double DISTANCE_LOSS_FACTOR = 0.95; // Mesafe kaybı faktörü
    private static final double OPTIMAL_HEIGHT = 150.0; // cm
    private static final double HEIGHT_PENALTY_FACTOR = 0.002; // Her cm için kayıp faktörü
    
    public SolarCalculator(double latitudeDegrees, double longitudeDegrees, double altitude) {
        updateLocation(latitudeDegrees, longitudeDegrees);
        this.altitude = altitude;
    }

    public void updateLocation(double latitudeDegrees, double longitudeDegrees) {
        this.latitude = Math.toRadians(latitudeDegrees);
        this.longitude = Math.toRadians(longitudeDegrees);
    }

    public SolarPosition calculateSolarPosition(LocalDateTime dateTime) {
        int dayOfYear = dateTime.getDayOfYear();

        // Calculate solar declination angle (Spencer formula)
        double B = 2 * Math.PI * (dayOfYear - 1) / 365.0;
        double declination = Math.toDegrees(0.006918 - 0.399912 * Math.cos(B) + 0.070257 * Math.sin(B)
                - 0.006758 * Math.cos(2 * B) + 0.000907 * Math.sin(2 * B)
                - 0.002697 * Math.cos(3 * B) + 0.001480 * Math.sin(3 * B));

        // Calculate equation of time
        double E = 229.18 * (0.000075 + 0.001868 * Math.cos(B) - 0.032077 * Math.sin(B)
                - 0.014615 * Math.cos(2 * B) - 0.040849 * Math.sin(2 * B));

        // Calculate hour angle
        double hour = dateTime.getHour() + dateTime.getMinute() / 60.0;
        double localLongitude = 45.0; // UTC+3 meridian
        double timeCorrection = 4 * (Math.toDegrees(longitude) - localLongitude) + E;
        double solarTime = hour + timeCorrection / 60.0;
        double hourAngle = 15.0 * (solarTime - 12.0);

        // Calculate altitude angle
        double sinDeclination = Math.sin(Math.toRadians(declination));
        double cosDeclination = Math.cos(Math.toRadians(declination));
        double sinLatitude = Math.sin(latitude);
        double cosLatitude = Math.cos(latitude);
        double sinHourAngle = Math.sin(Math.toRadians(hourAngle));
        double cosHourAngle = Math.cos(Math.toRadians(hourAngle));

        double sinAltitude = sinLatitude * sinDeclination
                + cosLatitude * cosDeclination * cosHourAngle;
        double altitudeAngle = Math.toDegrees(Math.asin(sinAltitude));

        // Calculate azimuth angle
        double cosAzimuth = (sinDeclination * cosLatitude
                - cosDeclination * sinLatitude * cosHourAngle)
                / Math.cos(Math.toRadians(altitudeAngle));
        double azimuthAngle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cosAzimuth))));

        if (hourAngle > 0) {
            azimuthAngle = 360 - azimuthAngle;
        }

        // Apply atmospheric corrections
        double airMass = calculateAirMass(altitudeAngle);
        double atmosphericRefraction = calculateAtmosphericRefraction(altitudeAngle);
        altitudeAngle += atmosphericRefraction / 3600.0; // Convert arcseconds to degrees

        // Calculate solar intensity with atmospheric effects
        double solarIntensity = SOLAR_CONSTANT * Math.pow(0.7, Math.pow(airMass, 0.678));

        return new SolarPosition(altitudeAngle, azimuthAngle, solarIntensity);
    }

    public double calculateTotalEnergy(SimulationState state) {
        SolarPosition sunPos = calculateSolarPosition(state.getCurrentTime());
        double dni = sunPos.getSolarIntensity();
        List<MirrorPosition> mirrors = state.getMirrorPositions();
        double totalEnergy = 0.0;
        
        ShadingDetector shadingDetector = new ShadingDetector();

        for (MirrorPosition mirror : mirrors) {
            // Ayna alanı hesabı (m²)
            double mirrorArea = (state.getReflectorWidth() * state.getReflectorLength()) / 10000.0;

            // Verimlilik faktörleri
            double cosineEfficiency = calculateCosineEfficiency(mirror, sunPos);
            double spillageEfficiency = calculateSpillageLoss(mirror, state);
            double blockingEfficiency = shadingDetector.calculateBlockingAndShadingLoss(mirror, mirrors, state, sunPos);

            // Mesafe kaybı hesabı 
            double distanceLoss = calculateDistanceLoss(mirror, state);

            // Yükseklik optimizasyon faktörü 
            double heightEfficiency = calculateHeightEfficiency(state.getReceiverHeight());

            // Toplam enerji hesabı
            double mirrorEnergy = dni
                    * mirrorArea
                    * MIRROR_REFLECTIVITY
                    * cosineEfficiency
                    * spillageEfficiency
                    * blockingEfficiency
                    * RECEIVER_ABSORPTIVITY
                    * SHADING_FACTOR
                    * distanceLoss
                    * heightEfficiency;

            totalEnergy += mirrorEnergy;
        }

        return totalEnergy;
    }

    private double calculateDistanceLoss(MirrorPosition mirror, SimulationState state) {
        double receiverHeight = state.getReceiverHeight() / 100.0;
        double mirrorX = Math.abs(mirror.getXOffset()) / 100.0;
        double distance = Math.sqrt(receiverHeight * receiverHeight + mirrorX * mirrorX);
        double minDistance = Math.sqrt(
                (MIN_RECEIVER_HEIGHT / 100.0) * (MIN_RECEIVER_HEIGHT / 100.0)
                + (MIN_MIRROR_SPACING / 100.0) * (MIN_MIRROR_SPACING / 100.0)
        );
        return Math.pow(DISTANCE_LOSS_FACTOR, (distance / minDistance - 1));
    }

    private double calculateHeightEfficiency(double height) {
        double deviation = Math.abs(height - OPTIMAL_HEIGHT);
        return Math.exp(-HEIGHT_PENALTY_FACTOR * deviation);
    }

    public double calculateSpillageLoss(MirrorPosition mirror, SimulationState state) {
        double mirrorWidth = state.getReflectorWidth() / 100.0;
        double receiverDiameter = state.getReceiverDiameter() / 100.0;
        double receiverHeight = state.getReceiverHeight() / 100.0;
        double horizontalDistance = Math.abs(mirror.getXOffset() / 100.0);

        double beamSpreadAngle = Math.toRadians(SUN_ANGULAR_WIDTH);
        double beamWidth = mirrorWidth + 2 * receiverHeight * Math.tan(beamSpreadAngle);

        double distance = Math.sqrt(receiverHeight * receiverHeight + horizontalDistance * horizontalDistance);
        double additionalSpread = distance * Math.tan(beamSpreadAngle);
        beamWidth += additionalSpread;

        double effectiveWidth = Math.min(beamWidth, receiverDiameter);
        double spillageRatio = effectiveWidth / beamWidth;
        double rotationAngle = Math.abs(Math.toRadians(mirror.getRotationAngle()));
        double angularCorrection = Math.cos(rotationAngle);

        return Math.min(1.0, spillageRatio * angularCorrection);
    }

    private double calculateAirMass(double altitude) {
        double zenith = 90 - altitude;
        double cosZenith = Math.cos(Math.toRadians(zenith));
        return 1 / (cosZenith + 0.50572 * Math.pow(96.07995 - zenith, -1.6364));
    }

    private double calculateAtmosphericRefraction(double altitude) {
        if (altitude > 85.0) return 0;
        double te = Math.tan(Math.toRadians(altitude));
        if (altitude > 5.0) return 58.1 / te - 0.07 / (te * te * te) + 0.000086 / Math.pow(te, 5);
        if (altitude > -0.575) return 1735.0 + altitude * (-518.2 + altitude * (103.4 + altitude * (-12.79 + altitude * 0.711)));
        return -20.774 / te;
    }

    public double calculateCosineEfficiency(MirrorPosition mirror, SolarPosition sunPos) {
        double sunAltRad = Math.toRadians(sunPos.getAltitudeAngle());
        double sunAzRad = Math.toRadians(sunPos.getAzimuthAngle());

        double[] sunVector = {
            -Math.cos(sunAltRad) * Math.sin(sunAzRad),
            Math.cos(sunAltRad) * Math.cos(sunAzRad),
            Math.sin(sunAltRad)
        };

        double mirrorRotRad = Math.toRadians(mirror.getRotationAngle());
        double[] mirrorNormal = {
            Math.sin(mirrorRotRad),
            0,
            Math.cos(mirrorRotRad)
        };

        double dotProduct = 0;
        for (int i = 0; i < 3; i++) {
            dotProduct += sunVector[i] * mirrorNormal[i];
        }

        return Math.abs(dotProduct);
    }

    public DaylightTimes calculateSunriseSunset(LocalDate date) {
        int dayOfYear = date.getDayOfYear();
        double declination = -23.45 * Math.cos(Math.toRadians(360.0 / 365.0 * (dayOfYear + 10)));

        double latRad = this.latitude;
        double decRad = Math.toRadians(declination);
        double hourAngle = Math.toDegrees(Math.acos(
                (-Math.sin(Math.toRadians(-0.833)) - Math.sin(latRad) * Math.sin(decRad))
                / (Math.cos(latRad) * Math.cos(decRad))
        ));

        double B = 360.0 * (dayOfYear - 81) / 365.0;
        double EoT = 9.87 * Math.sin(Math.toRadians(2 * B))
                - 7.53 * Math.cos(Math.toRadians(B))
                - 1.5 * Math.sin(Math.toRadians(B));

        int zoneDiff = 3;
        double timeCorrection = EoT + (4 * Math.toDegrees(this.longitude)) - (60 * zoneDiff);

        double sunriseMinutes = 720 - 4 * hourAngle - timeCorrection;
        double sunsetMinutes = 720 + 4 * hourAngle - timeCorrection;

        int sunriseHour = (int) Math.floor(sunriseMinutes / 60);
        int sunriseMin = (int) Math.round(sunriseMinutes % 60);
        if (sunriseMin == 60) {
            sunriseMin = 0;
            sunriseHour += 1;
        }

        int sunsetHour = (int) Math.floor(sunsetMinutes / 60);
        int sunsetMin = (int) Math.round(sunsetMinutes % 60);
        if (sunsetMin == 60) {
            sunsetMin = 0;
            sunsetHour += 1;
        }

        sunriseHour = sunriseHour % 24;
        if (sunriseHour < 0) sunriseHour += 24;
        sunsetHour = sunsetHour % 24;
        if (sunsetHour < 0) sunsetHour += 24;

        LocalDateTime sunrise = date.atTime(sunriseHour, sunriseMin);
        LocalDateTime sunset = date.atTime(sunsetHour, sunsetMin);

        return new DaylightTimes(sunrise, sunset);
    }
}