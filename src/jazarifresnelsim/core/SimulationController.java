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
import jazarifresnelsim.domain.MirrorTracker;
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

//    private double calculateOptimalMirrorAngle(double mirrorX, SolarPosition sunPos, SimulationState state) {
//        // Güneş pozisyonunu radyana çevir
//        double sunAltitude = Math.toRadians(sunPos.getAltitudeAngle());
//        double sunAzimuth = Math.toRadians(sunPos.getAzimuthAngle());
//
//        // Güneş ışını vektörü
//        double[] sunRay = {
//            -Math.cos(sunAltitude) * Math.sin(sunAzimuth),
//            Math.cos(sunAltitude) * Math.cos(sunAzimuth),
//            Math.sin(sunAltitude)
//        };
//
//        // Alıcı tüpe giden hedef vektörü (state'den alınan güncel değerlerle)
//        double receiverHeight = state.getReceiverHeight() / 100.0; // cm'yi metreye çevir
//        double supportHeight = state.getSupportHeight() / 100.0;
//
//        double[] targetRay = {
//            -mirrorX,
//            0,
//            receiverHeight - (supportHeight + 0.02) // 2cm'yi metre cinsinden ekle
//        };
//
//        // targetRay'i normalize et
//        double targetMagnitude = Math.sqrt(
//                targetRay[0] * targetRay[0]
//                + targetRay[1] * targetRay[1]
//                + targetRay[2] * targetRay[2]
//        );
//
//        for (int i = 0; i < 3; i++) {
//            targetRay[i] /= targetMagnitude;
//        }
//
//        // Normal vektör (gelen ve yansıyan ışınların açıortayı)
//        double[] normalVector = {
//            sunRay[0] + targetRay[0],
//            sunRay[1] + targetRay[1],
//            sunRay[2] + targetRay[2]
//        };
//
//        // Normal vektörü normalize et
//        double normalMagnitude = Math.sqrt(
//                normalVector[0] * normalVector[0]
//                + normalVector[1] * normalVector[1]
//                + normalVector[2] * normalVector[2]
//        );
//
//        for (int i = 0; i < 3; i++) {
//            normalVector[i] /= normalMagnitude;
//        }
//
//        // Y ekseni etrafındaki dönme açısını hesapla
//        return Math.toDegrees(Math.atan2(normalVector[0], normalVector[2]));
//    }
@Override
    public void updateMirrorPositions() {
        List<MirrorPosition> newPositions = new ArrayList<>();
        SolarPosition sunPos = state.getCurrentSolarPosition();

        if (sunPos == null) {
            return;
        }

        int numReflectors = state.getNumReflectors();
        float spacing = state.getReflectorSpacing();
        float supportHeight = state.getSupportHeight();
        
        // YENİ EKLENEN SINIF BURADA DEVREYE GİRİYOR
        MirrorTracker tracker = new MirrorTracker();

        for (int i = 0; i < numReflectors; i++) {
            double offset = (i < numReflectors / 2)
                    ? -(i + 0.5) : (i - numReflectors / 2 + 0.5);

            double xOffset = offset * spacing;

            // Artık SolarCalculator üzerinden değil, MirrorTracker üzerinden hesaplıyoruz
            double rotationAngle = tracker.calculateOptimalMirrorAngle(
                    xOffset / 100.0, sunPos, state);

            newPositions.add(new MirrorPosition(
                    rotationAngle,
                    xOffset,
                    supportHeight + 2,
                    i
            ));
        }

        state.updateMirrorPositions(newPositions);
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

}
