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
    // Yeni eklenen sabitler
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

//    public double calculateTotalEnergy(SimulationState state) {
//        SolarPosition sunPos = calculateSolarPosition(state.getCurrentTime());
//        double dni = sunPos.getSolarIntensity(); // Direct Normal Irradiance (W/m²)
//        List<MirrorPosition> mirrors = state.getMirrorPositions();
//        double totalEnergy = 0.0;
//
//        for (MirrorPosition mirror : mirrors) {
//            // Calculate mirror area in m²
//            double mirrorArea = (state.getReflectorWidth() * state.getReflectorLength()) / 10000.0;
//
//            // Calculate all efficiency factors
//            double cosineEfficiency = calculateCosineEfficiency(mirror, sunPos);
//            double spillageEfficiency = calculateSpillageLoss(mirror, state);
//            double blockingEfficiency = calculateBlockingAndShadingLoss(mirror, mirrors, state, sunPos);
//
//            // Calculate energy contribution from this mirror
//            double mirrorEnergy = dni * mirrorArea
//                    * MIRROR_REFLECTIVITY
//                    * cosineEfficiency
//                    * spillageEfficiency
//                    * blockingEfficiency
//                    * RECEIVER_ABSORPTIVITY
//                    * SHADING_FACTOR;
//
//            totalEnergy += mirrorEnergy;
//        }
//
//        return totalEnergy;
//    }
    public double calculateTotalEnergy(SimulationState state) {
        SolarPosition sunPos = calculateSolarPosition(state.getCurrentTime());
        double dni = sunPos.getSolarIntensity();
        List<MirrorPosition> mirrors = state.getMirrorPositions();
        double totalEnergy = 0.0;

        for (MirrorPosition mirror : mirrors) {
            // Ayna alanı hesabı (m²)
            double mirrorArea = (state.getReflectorWidth() * state.getReflectorLength()) / 10000.0;

            // Verimlilik faktörleri
            double cosineEfficiency = calculateCosineEfficiency(mirror, sunPos);
            double spillageEfficiency = calculateSpillageLoss(mirror, state);
            double blockingEfficiency = calculateBlockingAndShadingLoss(mirror, mirrors, state, sunPos);

            // Mesafe kaybı hesabı (yeni eklendi)
            double distanceLoss = calculateDistanceLoss(mirror, state);

            // Yükseklik optimizasyon faktörü (yeni eklendi)
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

    // Yeni eklenen mesafe kaybı hesaplama metodu
    private double calculateDistanceLoss(MirrorPosition mirror, SimulationState state) {
        // Alıcıya olan mesafeyi hesapla (cm'den m'ye çevir)
        double receiverHeight = state.getReceiverHeight() / 100.0;
        double mirrorX = Math.abs(mirror.getXOffset()) / 100.0;

        // Pitagor teoremi ile mesafe hesabı
        double distance = Math.sqrt(receiverHeight * receiverHeight + mirrorX * mirrorX);

        // Minimum mesafe (normalize etmek için)
        double minDistance = Math.sqrt(
                (MIN_RECEIVER_HEIGHT / 100.0) * (MIN_RECEIVER_HEIGHT / 100.0)
                + (MIN_MIRROR_SPACING / 100.0) * (MIN_MIRROR_SPACING / 100.0)
        );

        // Mesafe kaybı faktörü (ters kare yasası)
        return Math.pow(DISTANCE_LOSS_FACTOR, (distance / minDistance - 1));
    }

    // Yeni eklenen yükseklik optimizasyon metodu
    private double calculateHeightEfficiency(double height) {
        // Optimal yükseklikten sapma
        double deviation = Math.abs(height - OPTIMAL_HEIGHT);

        // Verim kaybı hesabı (üstel azalma)
        return Math.exp(-HEIGHT_PENALTY_FACTOR * deviation);
    }

    // Güncellenen spillage loss hesabı
    public double calculateSpillageLoss(MirrorPosition mirror, SimulationState state) {
        // Boyutları metreye çevir
        double mirrorWidth = state.getReflectorWidth() / 100.0;
        double receiverDiameter = state.getReceiverDiameter() / 100.0;
        double receiverHeight = state.getReceiverHeight() / 100.0;
        double horizontalDistance = Math.abs(mirror.getXOffset() / 100.0);

        // Işın yayılımı hesabı
        double beamSpreadAngle = Math.toRadians(SUN_ANGULAR_WIDTH);
        double beamWidth = mirrorWidth + 2 * receiverHeight * Math.tan(beamSpreadAngle);

        // Mesafeye bağlı ek yayılım (yeni eklendi)
        double distance = Math.sqrt(receiverHeight * receiverHeight + horizontalDistance * horizontalDistance);
        double additionalSpread = distance * Math.tan(beamSpreadAngle);
        beamWidth += additionalSpread;

        // Efektif genişlik hesabı
        double effectiveWidth = Math.min(beamWidth, receiverDiameter);

        // Spillage verimi hesabı
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
        if (altitude > 85.0) {
            return 0;
        }

        double te = Math.tan(Math.toRadians(altitude));
        if (altitude > 5.0) {
            return 58.1 / te - 0.07 / (te * te * te) + 0.000086 / Math.pow(te, 5);
        }
        if (altitude > -0.575) {
            return 1735.0 + altitude * (-518.2 + altitude * (103.4 + altitude * (-12.79 + altitude * 0.711)));
        }
        return -20.774 / te;
    }

    public double calculateCosineEfficiency(MirrorPosition mirror, SolarPosition sunPos) {
        // Güneş vektörü
        double sunAltRad = Math.toRadians(sunPos.getAltitudeAngle());
        double sunAzRad = Math.toRadians(sunPos.getAzimuthAngle());

        double[] sunVector = {
            -Math.cos(sunAltRad) * Math.sin(sunAzRad),
            Math.cos(sunAltRad) * Math.cos(sunAzRad),
            Math.sin(sunAltRad)
        };

        // Ayna normal vektörü (rotasyon açısından)
        double mirrorRotRad = Math.toRadians(mirror.getRotationAngle());
        double[] mirrorNormal = {
            Math.sin(mirrorRotRad),
            0,
            Math.cos(mirrorRotRad)
        };

        // Cosine verimi
        double dotProduct = 0;
        for (int i = 0; i < 3; i++) {
            dotProduct += sunVector[i] * mirrorNormal[i];
        }

        return Math.abs(dotProduct);
    }

//    public double calculateSpillageLoss(MirrorPosition mirror, SimulationState state) {
//        // Convert dimensions to meters
//        double mirrorWidth = state.getReflectorWidth() / 100.0;
//        double receiverDiameter = state.getReceiverDiameter() / 100.0;
//        double receiverHeight = state.getReceiverHeight() / 100.0;
//        double horizontalDistance = Math.abs(mirror.getXOffset() / 100.0);
//
//        // Calculate beam spread due to sun's angular width
//        double beamSpreadAngle = Math.toRadians(SUN_ANGULAR_WIDTH);
//        double beamWidth = mirrorWidth + 2 * receiverHeight * Math.tan(beamSpreadAngle);
//
//        // Calculate effective width that hits the receiver
//        double effectiveWidth = Math.min(beamWidth, receiverDiameter);
//
//        // Calculate spillage efficiency including angular correction
//        double spillageRatio = effectiveWidth / beamWidth;
//        double rotationAngle = Math.abs(Math.toRadians(mirror.getRotationAngle()));
//        double angularCorrection = Math.cos(rotationAngle);
//
//        return Math.min(1.0, spillageRatio * angularCorrection);
//    }

    public double calculateBlockingAndShadingLoss(MirrorPosition currentMirror,
            List<MirrorPosition> allMirrors, SimulationState state, SolarPosition sunPos) {
        // Convert dimensions to meters
        double mirrorWidth = state.getReflectorWidth() / 100.0;
        double supportHeight = state.getSupportHeight() / 100.0;
        double currentX = currentMirror.getXOffset() / 100.0;
        double receiverHeight = state.getReceiverHeight() / 100.0;

        // Calculate sun vector
        double sunAltRad = Math.toRadians(sunPos.getAltitudeAngle());
        double sunAzRad = Math.toRadians(sunPos.getAzimuthAngle());
        double[] sunVector = {
            -Math.cos(sunAltRad) * Math.sin(sunAzRad),
            Math.cos(sunAltRad) * Math.cos(sunAzRad),
            Math.sin(sunAltRad)
        };

        // Calculate reflected ray vector to receiver
        double[] reflectedVector = calculateReflectedVector(currentX, receiverHeight, supportHeight);

        // Check blocking and shading from each neighbor
        double effectiveArea = mirrorWidth;
        double blockedArea = 0.0;

        for (MirrorPosition otherMirror : allMirrors) {
            if (otherMirror.getMirrorIndex() == currentMirror.getMirrorIndex()) {
                continue;
            }

            double otherX = otherMirror.getXOffset() / 100.0;
            double distance = Math.abs(currentX - otherX);

            // Add shading losses
            if (checkShadowing(currentX, otherX, sunVector, mirrorWidth, supportHeight)) {
                blockedArea += calculateOverlap(distance, mirrorWidth, sunVector);
            }

            // Add blocking losses
            if (checkBlocking(currentX, otherX, reflectedVector, mirrorWidth, supportHeight)) {
                blockedArea += calculateOverlap(distance, mirrorWidth, reflectedVector);
            }
        }

        return Math.max(0.0, (effectiveArea - blockedArea) / effectiveArea);
    }

    private double[] calculateReflectedVector(double x, double receiverHeight, double supportHeight) {
        double[] vector = {-x, 0, receiverHeight - supportHeight};
        double magnitude = Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]);
        for (int i = 0; i < 3; i++) {
            vector[i] /= magnitude;
        }
        return vector;
    }

    private boolean checkShadowing(double currentX, double otherX, double[] sunVector,
            double mirrorWidth, double height) {
        double dx = Math.abs(currentX - otherX);
        double projectedWidth = mirrorWidth * Math.abs(sunVector[0] / sunVector[2]);
        return dx < projectedWidth;
    }

    private boolean checkBlocking(double currentX, double otherX, double[] reflectedVector,
            double mirrorWidth, double height) {
        double dx = Math.abs(currentX - otherX);
        double projectedWidth = mirrorWidth * Math.abs(reflectedVector[0] / reflectedVector[2]);
        return dx < projectedWidth;
    }

    private double calculateOverlap(double distance, double width, double[] vector) {
        double projectedWidth = width * Math.abs(vector[0] / vector[2]);
        return Math.max(0.0, projectedWidth - distance);
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
        if (sunriseHour < 0) {
            sunriseHour += 24;
        }
        sunsetHour = sunsetHour % 24;
        if (sunsetHour < 0) {
            sunsetHour += 24;
        }

        LocalDateTime sunrise = date.atTime(sunriseHour, sunriseMin);
        LocalDateTime sunset = date.atTime(sunsetHour, sunsetMin);

        return new DaylightTimes(sunrise, sunset);
    }

    public double calculateOptimalMirrorAngle(double mirrorX, SolarPosition sunPos, SimulationState state) {
        // Convert sun position to radians
        double sunAltitude = Math.toRadians(sunPos.getAltitudeAngle());
        double sunAzimuth = Math.toRadians(sunPos.getAzimuthAngle());

        // Sun ray vector
        double[] sunRay = {
            -Math.cos(sunAltitude) * Math.sin(sunAzimuth),
            Math.cos(sunAltitude) * Math.cos(sunAzimuth),
            Math.sin(sunAltitude)
        };

        // Target vector to receiver tube (using current state values)
        double receiverHeight = state.getReceiverHeight() / 100.0; // convert cm to meters
        double supportHeight = state.getSupportHeight() / 100.0;

        double[] targetRay = {
            -mirrorX,
            0,
            receiverHeight - (supportHeight + 0.02) // add 2cm in meters
        };

        // Normalize targetRay
        double targetMagnitude = Math.sqrt(
                targetRay[0] * targetRay[0]
                + targetRay[1] * targetRay[1]
                + targetRay[2] * targetRay[2]
        );

        for (int i = 0; i < 3; i++) {
            targetRay[i] /= targetMagnitude;
        }

        // Normal vector (bisector of incident and reflected rays)
        double[] normalVector = {
            sunRay[0] + targetRay[0],
            sunRay[1] + targetRay[1],
            sunRay[2] + targetRay[2]
        };

        // Normalize normal vector
        double normalMagnitude = Math.sqrt(
                normalVector[0] * normalVector[0]
                + normalVector[1] * normalVector[1]
                + normalVector[2] * normalVector[2]
        );

        for (int i = 0; i < 3; i++) {
            normalVector[i] /= normalMagnitude;
        }

        // Calculate rotation angle around Y axis
        return Math.toDegrees(Math.atan2(normalVector[0], normalVector[2]));
    }
}
