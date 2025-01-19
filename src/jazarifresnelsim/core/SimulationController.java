package jazarifresnelsim.core;

import jazarifresnelsim.domain.SolarCalculator;
import jazarifresnelsim.models.SimulationState;
import jazarifresnelsim.models.MirrorPosition;
import jazarifresnelsim.models.SolarPosition;
import controlP5.ControlP5;
import controlP5.Textfield;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import static jazarifresnelsim.domain.Constants.*;
import jazarifresnelsim.ui.IGUIUpdateCallback;

public class SimulationController implements ISimulationController {

    private final SimulationState state;
    private final SolarCalculator solarCalculator;
    private int frameCounter;
    private int selectedMonth = 1;
    private int selectedDay = 1;
    private static final float FRAMES_PER_UPDATE = 60; // Assuming 60 FPS
    private IGUIUpdateCallback guiCallback;

    public SimulationController(SimulationState state) {
        this.state = state;
        this.solarCalculator = new SolarCalculator(state.getLatitude(), state.getLongitude(), 0);
        this.frameCounter = 0;
    }

    @Override
    public void startSimulation() {
        state.setAnimating(true);
        frameCounter = 0;
        updateCurrentTime();
        updateSolarPosition();
        updateMirrorPositions();
    }

    @Override
    public void stopSimulation() {
        state.setAnimating(false);
    }

    @Override
    public void update() {
        if (!state.isAnimating()) {
            return;
        }

        frameCounter++;
        if (frameCounter >= FRAMES_PER_UPDATE) {
            // Mevcut zamanı simülasyon adımına göre güncelle
            LocalDateTime currentTime = state.getCurrentTime();
            double stepMinutes = state.getSimulationStepMinutes();
            currentTime = currentTime.plusMinutes((long) stepMinutes);

            // End time kontrolünü debug edelim
            LocalDateTime endTime = state.getEndTime();
            System.out.println("Current Time: " + currentTime.format(DateTimeFormatter.ofPattern("HH:mm")));
            System.out.println("End Time: " + endTime.format(DateTimeFormatter.ofPattern("HH:mm")));

            if (currentTime.isAfter(endTime)) {
                System.out.println("Simulation ended: Current time passed end time");
                stopSimulation();
                return;
            }

            state.setCurrentTime(currentTime);

            if (guiCallback != null) {
                String timeStr = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                guiCallback.onTimeUpdate(timeStr);
            }

            updateSolarPosition();
            updateMirrorPositions();

            frameCounter = 0;
        }
    }

    public void setGUICallback(IGUIUpdateCallback callback) {
        this.guiCallback = callback;
    }

    private void updateCurrentTimeDisplay() {
        if (guiCallback != null) {
            String currentTimeStr = state.getCurrentTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            guiCallback.onTimeUpdate(currentTimeStr);
        }
    }

    @Override
    public void updateGUIDisplay(ControlP5 cp5) {
        // Interface'den implement edilen metod
        String currentTimeStr = state.getCurrentTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        if (cp5 != null) {
            Textfield currentTimeField = cp5.get(Textfield.class, "CurrentTime");
            if (currentTimeField != null) {
                currentTimeField.setText(currentTimeStr);
            }
        }
    }

    @Override
    public void setTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        state.setTimeRange(startTime, endTime);
        state.setCurrentTime(startTime);
        selectedMonth = startTime.getMonthValue();
        selectedDay = startTime.getDayOfMonth();
        updateSolarPosition();
        updateMirrorPositions();
    }

    @Override
    public void setSimulationStep(double stepMinutes) {
        state.setSimulationStepMinutes(stepMinutes);
    }

    @Override
    public void setLocation(double latitude, double longitude) {
        state.setLatitude(latitude);
        state.setLongitude(longitude);
        solarCalculator.updateLocation(latitude, longitude);
        updateSolarPosition();
        updateMirrorPositions();
    }

//    private void updateSolarPosition() {
//        SolarPosition newPosition = solarCalculator.calculateSolarPosition(state.getCurrentTime());
//        state.setCurrentSolarPosition(newPosition);
//    }
//
//    private void updateMirrorPositions() {
//        System.out.println("Updating mirror positions...");
//        List<MirrorPosition> newPositions = new ArrayList<>();
//        SolarPosition sunPos = state.getCurrentSolarPosition();
//
//        if (sunPos == null) {
//            System.out.println("Solar position is null!");
//            return;
//        }
//
//        System.out.println("Solar position: " + sunPos);
//
//        for (int i = 0; i < NUM_REFLECTORS; i++) {
//            double offset = (i < NUM_REFLECTORS / 2)
//                    ? -(i + 0.5) : (i - NUM_REFLECTORS / 2 + 0.5);
//            double xOffset = offset * REFLECTOR_SPACING;
//
//            double rotationAngle = calculateOptimalMirrorAngle(xOffset, sunPos);
//
//            MirrorPosition pos = new MirrorPosition(
//                    rotationAngle,
//                    xOffset,
//                    SUPPORT_HEIGHT + 2,
//                    i
//            );
//            newPositions.add(pos);
//            System.out.println("Added mirror position: " + pos);
//        }
//
//        state.updateMirrorPositions(newPositions);
//        System.out.println("Updated positions size: " + newPositions.size());
//    }
    private double calculateOptimalMirrorAngle(double mirrorX, SolarPosition sunPos) {
        // Convert sun position to radians
        double sunAltitude = Math.toRadians(sunPos.getAltitudeAngle());
        double sunAzimuth = Math.toRadians(sunPos.getAzimuthAngle());

        // Sun ray vector
        double[] sunRay = {
            -Math.cos(sunAltitude) * Math.sin(sunAzimuth),
            Math.cos(sunAltitude) * Math.cos(sunAzimuth),
            Math.sin(sunAltitude)
        };

        // Target vector (to receiver)
        double[] targetRay = {
            -mirrorX,
            0,
            RECEIVER_HEIGHT - (SUPPORT_HEIGHT + 2)
        };

        // Normalize target ray
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

    private void updateCurrentTime() {
        // Sadece current time'ı start time'a eşitleyelim
        state.setCurrentTime(state.getStartTime());
    }

    public void updateDaysInMonth(int month) {
        selectedMonth = month;
        int maxDays;

        switch (month) {
            case 2 -> {
                // February
                int year = state.getCurrentTime().getYear();
                maxDays = ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) ? 29 : 28;
            }
            case 4, 6, 9, 11 -> // 30-day months
                maxDays = 30;
            default -> // 31-day months
                maxDays = 31;
        }

        if (selectedDay > maxDays) {
            selectedDay = maxDays;
        }

        updateCurrentTime();
    }

    @Override
    public void updateSolarPosition() {
        SolarPosition newPosition = solarCalculator.calculateSolarPosition(state.getCurrentTime());
        state.setCurrentSolarPosition(newPosition);
    }

    @Override
    public void updateMirrorPositions() {
        List<MirrorPosition> newPositions = new ArrayList<>();
        SolarPosition sunPos = state.getCurrentSolarPosition();

        if (sunPos == null) {
            return;
        }

        // Constants.NUM_REFLECTORS yerine state.getNumReflectors() kullan
        int numReflectors = state.getNumReflectors();

        for (int i = 0; i < numReflectors; i++) {
            double offset = (i < numReflectors / 2)
                    ? -(i + 0.5) : (i - numReflectors / 2 + 0.5);

            // state'den spacing değerini al    
            double xOffset = offset * state.getReflectorSpacing();

            double rotationAngle = calculateOptimalMirrorAngle(xOffset, sunPos);

            newPositions.add(new MirrorPosition(
                    rotationAngle,
                    xOffset,
                    state.getSupportHeight() + 2, // state'den support height değerini al
                    i
            ));
        }

        state.updateMirrorPositions(newPositions);
    }

}
