package jazarifresnelsim.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import static jazarifresnelsim.domain.Constants.*;

/**
 * Maintains the current state of the simulation.
 * Thread-safe through synchronization on mutable fields.
 *
 * VERSION 4.0 — Uses canonical constant names from Constants.java v4.0
 *
 * @author Yunus Demirtas, Musa Atas — Siirt University
 * @version 4.0
 */
public class SimulationState {

    private double latitude;
    private double longitude;
    private LocalDateTime currentTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean isAnimating;
    private List<MirrorPosition> mirrorPositions;
    private SolarPosition currentSolarPosition;
    private double simulationStepMinutes;

    // Geometry — initialized from Constants (v4.0 canonical names)
    private int numReflectors       = NUM_MIRRORS;
    private float receiverHeight    = RECEIVER_HEIGHT_CM;
    private float receiverDiameter  = RECEIVER_DIAMETER_CM;
    private float reflectorWidth    = MIRROR_WIDTH_CM;
    private float reflectorLength   = MIRROR_LENGTH_CM;
    private float reflectorSpacing  = MIRROR_SPACING_CM;
    private float supportHeight     = SUPPORT_HEIGHT_CM;

    public SimulationState() {
        this.latitude  = DEFAULT_LATITUDE;
        this.longitude = DEFAULT_LONGITUDE;
        this.currentTime = LocalDateTime.now()
                .withHour(12).withMinute(0).withSecond(0);
        this.startTime = currentTime;
        this.endTime   = currentTime.withHour(17).withMinute(0);
        this.isAnimating = false;
        this.mirrorPositions = new ArrayList<>();
        this.simulationStepMinutes = 1.0;
    }

    // --- Time ---

    public synchronized LocalDateTime getCurrentTime()  { return currentTime; }
    public synchronized void setCurrentTime(LocalDateTime time) { this.currentTime = time; }

    public synchronized LocalDateTime getStartTime()    { return startTime; }
    public synchronized void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public synchronized LocalDateTime getEndTime()      { return endTime; }
    public synchronized void setEndTime(LocalDateTime end) { this.endTime = end; }

    public synchronized void setTimeRange(LocalDateTime start, LocalDateTime end) {
        this.startTime = start;
        this.endTime   = end;
    }

    public synchronized double getSimulationStepMinutes() { return simulationStepMinutes; }
    public synchronized void setSimulationStepMinutes(double minutes) { this.simulationStepMinutes = minutes; }

    // --- Location ---

    public synchronized double getLatitude()  { return latitude; }
    public synchronized void setLatitude(double latitude) { this.latitude = latitude; }

    public synchronized double getLongitude() { return longitude; }
    public synchronized void setLongitude(double longitude) { this.longitude = longitude; }

    // --- Animation ---

    public synchronized boolean isAnimating()  { return isAnimating; }
    public synchronized void setAnimating(boolean animating) { this.isAnimating = animating; }
    public boolean isIsAnimating() { return isAnimating; }
    public void setIsAnimating(boolean isAnimating) { this.isAnimating = isAnimating; }

    // --- Solar Position ---

    public synchronized SolarPosition getCurrentSolarPosition() { return currentSolarPosition; }
    public synchronized void setCurrentSolarPosition(SolarPosition position) { this.currentSolarPosition = position; }

    // --- Mirror Positions ---

    public synchronized void updateMirrorPositions(List<MirrorPosition> newPositions) {
        this.mirrorPositions = new ArrayList<>(newPositions);
    }
    public synchronized List<MirrorPosition> getMirrorPositions() {
        return mirrorPositions != null ? new ArrayList<>(mirrorPositions) : new ArrayList<>();
    }
    public void setMirrorPositions(List<MirrorPosition> mirrorPositions) {
        this.mirrorPositions = mirrorPositions;
    }

    // --- Geometry Getters/Setters ---

    public synchronized int getNumReflectors()    { return numReflectors; }
    public synchronized void setNumReflectors(int value) { this.numReflectors = value; }

    public synchronized float getReceiverHeight() { return receiverHeight; }
    public synchronized void setReceiverHeight(float value) { this.receiverHeight = value; }

    public float getReceiverDiameter()  { return receiverDiameter; }
    public void setReceiverDiameter(float receiverDiameter) { this.receiverDiameter = receiverDiameter; }

    public float getReflectorWidth()    { return reflectorWidth; }
    public void setReflectorWidth(float reflectorWidth) { this.reflectorWidth = reflectorWidth; }

    public float getReflectorLength()   { return reflectorLength; }
    public void setReflectorLength(float reflectorLength) { this.reflectorLength = reflectorLength; }

    public float getReflectorSpacing()  { return reflectorSpacing; }
    public void setReflectorSpacing(float reflectorSpacing) { this.reflectorSpacing = reflectorSpacing; }

    public float getSupportHeight()     { return supportHeight; }
    public void setSupportHeight(float supportHeight) { this.supportHeight = supportHeight; }
}