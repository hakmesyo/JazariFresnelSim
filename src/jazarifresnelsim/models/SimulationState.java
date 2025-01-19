// SimulationState.java
package jazarifresnelsim.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import static jazarifresnelsim.domain.Constants.*;

/**
 * Maintains the current state of the simulation. This class is mutable but
 * thread-safe through synchronization.
 */
public class SimulationState {

//    private static final double DEFAULT_LATITUDE = 37.962984;   // Siirt University
//    private static final double DEFAULT_LONGITUDE = 41.850347;
    private double latitude;
    private double longitude;
    private LocalDateTime currentTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isAnimating;
    private List<MirrorPosition> mirrorPositions;
    private SolarPosition currentSolarPosition;
    private double simulationStepMinutes;
    private int numReflectors = NUM_REFLECTORS;
    private float receiverHeight = RECEIVER_HEIGHT;
    private float receiverDiameter = RECEIVER_DIAMETER;
    private float reflectorWidth = REFLECTOR_WIDTH;
    private float reflectorLength = REFLECTOR_LENGTH;
    private float reflectorSpacing = REFLECTOR_SPACING;
    private float supportHeight = SUPPORT_HEIGHT;

    public SimulationState() {
        this.latitude = DEFAULT_LATITUDE;
        this.longitude = DEFAULT_LONGITUDE;
        this.currentTime = LocalDateTime.now()
                .withHour(12)
                .withMinute(0)
                .withSecond(0);
        this.startTime = currentTime;
        this.endTime = currentTime.withHour(17).withMinute(0);
        this.isAnimating = false;
        this.mirrorPositions = new ArrayList<>();
        this.simulationStepMinutes = 1.0;
    }

    public synchronized LocalDateTime getEndTime() {
        return endTime;
    }

    // Var olan setTimeRange metodunu güncelleyelim
    public synchronized void setTimeRange(LocalDateTime start, LocalDateTime end) {
        System.out.println("Setting time range in state:");
        System.out.println("Start: " + start.format(DateTimeFormatter.ofPattern("HH:mm")));
        System.out.println("End: " + end.format(DateTimeFormatter.ofPattern("HH:mm")));
        this.startTime = start;
        this.endTime = end;
    }

    // Yeni direkt setter metodu ekleyelim
    public synchronized void setEndTime(LocalDateTime end) {
        System.out.println("Setting end time to: " + end.format(DateTimeFormatter.ofPattern("HH:mm")));
        this.endTime = end;
    }

    // Synchronized getters and setters
    public synchronized double getLatitude() {
        return latitude;
    }

    public synchronized void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public synchronized double getLongitude() {
        return longitude;
    }

    public synchronized void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public synchronized LocalDateTime getCurrentTime() {
        return currentTime;
    }

    public synchronized void setCurrentTime(LocalDateTime time) {
        this.currentTime = time;
    }

    public synchronized boolean isAnimating() {
        return isAnimating;
    }

    public synchronized void setAnimating(boolean animating) {
        this.isAnimating = animating;
    }

    public synchronized SolarPosition getCurrentSolarPosition() {
        return currentSolarPosition;
    }

    public synchronized void setCurrentSolarPosition(SolarPosition position) {
        this.currentSolarPosition = position;
    }

    public synchronized LocalDateTime getStartTime() {
        return startTime;
    }


    public synchronized double getSimulationStepMinutes() {
        return simulationStepMinutes;
    }

    public synchronized void setSimulationStepMinutes(double minutes) {
        this.simulationStepMinutes = minutes;
    }

    public synchronized void updateMirrorPositions(List<MirrorPosition> newPositions) {
        //System.out.println("Updating mirror positions with size: " + newPositions.size());
        this.mirrorPositions = new ArrayList<>(newPositions);
    }

    public synchronized List<MirrorPosition> getMirrorPositions() {
        return mirrorPositions != null ? new ArrayList<>(mirrorPositions) : new ArrayList<>();
    }

    public synchronized int getNumReflectors() {
        return numReflectors;
    }

    public synchronized void setNumReflectors(int value) {
        this.numReflectors = value;
    }

    public synchronized float getReceiverHeight() {
        return receiverHeight;
    }

    public synchronized void setReceiverHeight(float value) {
        this.receiverHeight = value;
    }

    public float getReceiverDiameter() {
        return receiverDiameter;
    }

    public float getReflectorWidth() {
        return reflectorWidth;
    }

    public float getReflectorLength() {
        return reflectorLength;
    }

    public float getReflectorSpacing() {
        return reflectorSpacing;
    }

    public float getSupportHeight() {
        return supportHeight;
    }

    public boolean isIsAnimating() {
        return isAnimating;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }


    public void setIsAnimating(boolean isAnimating) {
        this.isAnimating = isAnimating;
    }

    public void setMirrorPositions(List<MirrorPosition> mirrorPositions) {
        this.mirrorPositions = mirrorPositions;
    }

    public void setReceiverDiameter(float receiverDiameter) {
        this.receiverDiameter = receiverDiameter;
    }

    public void setReflectorWidth(float reflectorWidth) {
        this.reflectorWidth = reflectorWidth;
    }

    public void setReflectorLength(float reflectorLength) {
        this.reflectorLength = reflectorLength;
    }

    public void setReflectorSpacing(float reflectorSpacing) {
        this.reflectorSpacing = reflectorSpacing;
    }

    public void setSupportHeight(float supportHeight) {
        this.supportHeight = supportHeight;
    }

}
