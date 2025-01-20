// SolarCalculator.java copied from Open Jazari Library/utils https://github.com/hakmesyo/OJL
package jazarifresnelsim.domain;

import jazarifresnelsim.models.SolarPosition;
import java.time.LocalDateTime;
import java.util.List;
import jazarifresnelsim.models.MirrorPosition;
import jazarifresnelsim.models.SimulationState;

/**
 * Calculates solar position based on time and location. This class handles all
 * astronomical calculations for solar tracking.
 */
public class SolarCalculator {

    private double latitude;  // in radians
    private double longitude; // in radians
    private double altitude; // in meters

    // Physical constants for energy calculations
    private static final double SOLAR_CONSTANT = 1361.0; // W/m²
    private static final double MIRROR_REFLECTIVITY = 0.92;  // Mirror reflection efficiency
    private static final double RECEIVER_ABSORPTIVITY = 0.95; // Receiver tube absorption efficiency
    private static final double SHADING_FACTOR = 0.95; // General shading factor
    private static final double SUN_ANGULAR_WIDTH = 0.53; // Sun's angular width in degrees

    public SolarCalculator(double latitudeDegrees, double longitudeDegrees, double altitude) {
        updateLocation(latitudeDegrees, longitudeDegrees);
        this.altitude = altitude;
    }

    public void updateLocation(double latitudeDegrees, double longitudeDegrees) {
        this.latitude = Math.toRadians(latitudeDegrees);
        this.longitude = Math.toRadians(longitudeDegrees);
    }

    public SolarPosition calculateSolarPosition(LocalDateTime dateTime) {
        //System.out.println("Calculating solar position for: " + dateTime);
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
    /**
     * Calculates total energy output considering all losses
     * @param state Current simulation state
     * @return Total energy in Watts
     */
    public double calculateTotalEnergy(SimulationState state) {
        SolarPosition sunPos = calculateSolarPosition(state.getCurrentTime());
        double dni = sunPos.getSolarIntensity(); // Direct Normal Irradiance (W/m²)
        List<MirrorPosition> mirrors = state.getMirrorPositions();
        double totalEnergy = 0.0;
        
        for (MirrorPosition mirror : mirrors) {
            // Calculate mirror area in m²
            double mirrorArea = (state.getReflectorWidth() * state.getReflectorLength()) / 10000.0;
            
            // Calculate all efficiency factors
            double cosineEfficiency = calculateCosineEfficiency(mirror, sunPos);
            double spillageEfficiency = calculateSpillageLoss(mirror, state);
            double blockingEfficiency = calculateBlockingAndShadingLoss(mirror, mirrors, state, sunPos);
            
            // Calculate energy contribution from this mirror
            double mirrorEnergy = dni * mirrorArea * 
                                MIRROR_REFLECTIVITY * 
                                cosineEfficiency * 
                                spillageEfficiency * 
                                blockingEfficiency * 
                                RECEIVER_ABSORPTIVITY * 
                                SHADING_FACTOR;
            
            totalEnergy += mirrorEnergy;
        }
        
        return totalEnergy;
    }

    /**
     * Calculates spillage losses based on mirror width and receiver geometry
     * Considers beam spreading and receiver intercept factor
     */
    public double calculateSpillageLoss(MirrorPosition mirror, SimulationState state) {
        // Convert dimensions to meters
        double mirrorWidth = state.getReflectorWidth() / 100.0;
        double receiverDiameter = state.getReceiverDiameter() / 100.0;
        double receiverHeight = state.getReceiverHeight() / 100.0;
        double horizontalDistance = Math.abs(mirror.getXOffset() / 100.0);
        
        // Calculate beam spread due to sun's angular width
        double beamSpreadAngle = Math.toRadians(SUN_ANGULAR_WIDTH);
        double beamWidth = mirrorWidth + 2 * receiverHeight * Math.tan(beamSpreadAngle);
        
        // Calculate effective width that hits the receiver
        double effectiveWidth = Math.min(beamWidth, receiverDiameter);
        
        // Calculate spillage efficiency including angular correction
        double spillageRatio = effectiveWidth / beamWidth;
        double rotationAngle = Math.abs(Math.toRadians(mirror.getRotationAngle()));
        double angularCorrection = Math.cos(rotationAngle);
        
        return Math.min(1.0, spillageRatio * angularCorrection);
    }

    /**
     * Calculates blocking and shading losses from neighboring mirrors
     */
    public double calculateBlockingAndShadingLoss(MirrorPosition currentMirror, 
                                                 List<MirrorPosition> allMirrors, 
                                                 SimulationState state,
                                                 SolarPosition sunPos) {
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

    // Helper methods for vector calculations and geometric checks
    private double[] calculateReflectedVector(double x, double receiverHeight, double supportHeight) {
        double[] vector = {-x, 0, receiverHeight - supportHeight};
        double magnitude = Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]);
        for (int i = 0; i < 3; i++) vector[i] /= magnitude;
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
}
