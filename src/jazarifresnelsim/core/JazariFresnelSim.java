package jazarifresnelsim.core;

import processing.core.*;
import controlP5.*;
import peasy.*;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import jazarifresnelsim.model.SolarPosition;
import jazarifresnelsim.model.SolarTracker;

/**
 * JazariFresnelSim - A Linear Fresnel Solar Collector Simulation
 *
 * This class provides a real-time 3D simulation of a linear Fresnel solar
 * collector system. It visualizes the sun's position, mirror alignments, and
 * reflected rays throughout the day. The simulation includes a complete GUI for
 * controlling parameters such as location, date, and time settings.
 *
 * Key features: - Real-time sun position calculation based on date, time, and
 * location - Dynamic mirror angle optimization for maximum solar collection -
 * Interactive 3D visualization with camera controls - Configurable simulation
 * parameters through GUI - Time-based animation with adjustable simulation
 * speed
 *
 *
 * @version 1.0
 */
public class JazariFresnelSim extends PApplet {

    // GUI Control
    private ControlP5 cp5;
    private PeasyCam cam;

    /**
     * Reflector parameters (all dimensions in centimeters)
     */
    private static final int NUM_REFLECTORS = 4;          // Number of mirrors (2 left + 2 right)
    private static final float RECEIVER_HEIGHT = 130;     // Height of receiver tube
    private static final float RECEIVER_DIAMETER = 16;    // Diameter of receiver tube
    private static final float REFLECTOR_WIDTH = 20;      // Width of each mirror
    private static final float REFLECTOR_LENGTH = 100;    // Length of each mirror (1 meter)
    private static final float REFLECTOR_SPACING = 30;    // Spacing between mirrors
    private static final float SUPPORT_HEIGHT = 30;       // Height of mirror supports

    /**
     * Location parameters (El-Cezeri Lab, Siirt University Kezer Campus)
     */
    private double latitude = 37.962984;   // 37°56'N
    private double longitude = 41.850347;  // 41°57'E

    /**
     * Time management variables
     */
    private LocalDateTime currentTime;     // Current simulation time
    private LocalDateTime startTime;       // Start time for simulation
    private LocalDateTime endTime;         // End time for simulation
    private boolean isAnimating = false;   // Animation state flag
    private int frameCounter = 0;          // Frame counter for animation timing
    private int selectedMonth = 1;         // Currently selected month
    private int selectedDay = 1;           // Currently selected day

    /**
     * 3D Models and Solar Tracking
     */
    private PShape receiverTube;           // 3D model of receiver tube
    private PShape[] reflectors;           // Array of mirror 3D models
    private SolarTracker solarTracker;     // Solar position calculator

    /**
     * Main method to launch the application
     *
     * @param args
     */
    public static void main(String[] args) {
        PApplet.main(new String[]{JazariFresnelSim.class.getName()});
    }

    /**
     * Processing settings method to configure the window
     */
    @Override
    public void settings() {
        size(1280, 800, P3D);
        smooth(8);
    }

    /**
     * Setup method to initialize the simulation Configures initial time, 3D
     * models, camera, and GUI
     */
    @Override
    public void setup() {
        surface.setTitle("Linear Fresnel Reflector Simulation");

        // Initialize time settings
        currentTime = LocalDateTime.now()
                .withHour(12)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        startTime = currentTime;
        endTime = currentTime.withHour(17).withMinute(0);

        // Initialize solar tracker
        solarTracker = new SolarTracker(latitude, longitude, 0);

        // Setup 3D models
        setupModels();

        // Configure camera
        cam = new PeasyCam(this, 0, -REFLECTOR_LENGTH / 2, RECEIVER_HEIGHT / 2, 800);
        cam.setMinimumDistance(10);
        cam.setMaximumDistance(5000);

        // Setup GUI elements
        cp5 = new ControlP5(this);
        setupGUI();
        cp5.setAutoDraw(false);
    }

    /**
     * Sets up 3D models for the receiver tube and reflectors
     */
    private void setupModels() {
        // Create receiver tube model
        receiverTube = createShape(GROUP);
        float radius = RECEIVER_DIAMETER / 2;
        int sides = 20;

        // Main tube
        PShape tube = createShape();
        tube.beginShape(TRIANGLE_STRIP);
        tube.fill(100, 50, 50);
        tube.noStroke();

        for (float angle = 0; angle <= TWO_PI + 0.1; angle += TWO_PI / sides) {
            float x = cos(angle) * radius;
            float z = sin(angle) * radius;
            tube.vertex(x, -REFLECTOR_LENGTH / 2, z);
            tube.vertex(x, REFLECTOR_LENGTH / 2, z);
        }
        tube.endShape();
        receiverTube.addChild(tube);

        // Support structures
        PShape supports = createShape();
        supports.beginShape(TRIANGLES);
        supports.fill(70);
        supports.noStroke();

        float supportWidth = 15;
        float supportSpacing = REFLECTOR_LENGTH * 0.8f;

        // End supports
        for (float pos : new float[]{-supportSpacing / 2, supportSpacing / 2}) {
            supports.vertex(-supportWidth / 2, pos, 0);
            supports.vertex(supportWidth / 2, pos, 0);
            supports.vertex(0, pos, -RECEIVER_HEIGHT);
        }
        supports.endShape();
        receiverTube.addChild(supports);

        // Create reflector mirrors
        reflectors = new PShape[NUM_REFLECTORS];
        for (int i = 0; i < NUM_REFLECTORS; i++) {
            reflectors[i] = createShape();
            reflectors[i].beginShape(QUADS);
            reflectors[i].fill(200, 200, 220);
            reflectors[i].stroke(150);
            reflectors[i].strokeWeight(1);
            reflectors[i].vertex(-REFLECTOR_WIDTH / 2, -REFLECTOR_LENGTH / 2, 0);
            reflectors[i].vertex(REFLECTOR_WIDTH / 2, -REFLECTOR_LENGTH / 2, 0);
            reflectors[i].vertex(REFLECTOR_WIDTH / 2, REFLECTOR_LENGTH / 2, 0);
            reflectors[i].vertex(-REFLECTOR_WIDTH / 2, REFLECTOR_LENGTH / 2, 0);
            reflectors[i].endShape();
        }
    }

    /**
     * Sets up the graphical user interface elements
     */
    private void setupGUI() {
        int panelX = 20;
        int panelY = 50;
        int width_x = 280;
        int textHeight = 35;
        int spacing = 20;

        // GUI font configuration
        PFont guiFont = createFont("Arial Bold", 14);
        cp5.setFont(guiFont);

        // Color scheme
        int backgroundColor = color(0, 20, 50);
        int labelColor = color(255);
        int textColor = color(200, 255, 255);
        int activeColor = color(0, 100, 200);
        int foregroundColor = color(100, 150, 200);

        // Main control group
        Group guiGroup = cp5.addGroup("Controls")
                .setPosition(panelX, panelY)
                .setWidth(width_x)
                .setHeight(30)
                .setBackgroundColor(backgroundColor)
                .setBackgroundHeight(500);

        int currentY = spacing + 10;

        // Latitude input
        cp5.addTextfield("Latitude")
                .setPosition(spacing, currentY)
                .setSize(width_x - 2 * spacing, textHeight)
                .setGroup(guiGroup)
                .setText(String.valueOf(latitude))
                .setColor(textColor)
                .setColorBackground(backgroundColor)
                .setColorActive(activeColor)
                .setColorForeground(foregroundColor)
                .setColorLabel(labelColor)
                .setColorCursor(color(255))
                .setColorValue(color(255))
                .setCaptionLabel("LATITUDE")
                .setAutoClear(false)
                .getCaptionLabel()
                .align(ControlP5.LEFT, ControlP5.TOP_OUTSIDE);

        currentY += textHeight + spacing;

        // Longitude input
        cp5.addTextfield("Longitude")
                .setPosition(spacing, currentY)
                .setSize(width_x - 2 * spacing, textHeight)
                .setGroup(guiGroup)
                .setText(String.valueOf(longitude))
                .setColor(textColor)
                .setColorBackground(backgroundColor)
                .setColorActive(activeColor)
                .setColorForeground(foregroundColor)
                .setColorLabel(labelColor)
                .setColorCursor(color(255))
                .setColorValue(color(255))
                .setCaptionLabel("LONGITUDE")
                .setAutoClear(false)
                .getCaptionLabel()
                .align(ControlP5.LEFT, ControlP5.TOP_OUTSIDE);

        currentY += textHeight + spacing;

        // Date input
        cp5.addTextfield("Date")
                .setPosition(spacing, currentY)
                .setSize(width_x - 2 * spacing, textHeight)
                .setGroup(guiGroup)
                .setText(currentTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .setColor(textColor)
                .setColorBackground(backgroundColor)
                .setColorActive(activeColor)
                .setColorForeground(foregroundColor)
                .setColorLabel(labelColor)
                .setColorCursor(color(255))
                .setColorValue(color(255))
                .setCaptionLabel("DATE (DD.MM.YYYY)")
                .setAutoClear(false)
                .getCaptionLabel()
                .align(ControlP5.LEFT, ControlP5.TOP_OUTSIDE);

        currentY += textHeight + spacing;

        // Start time input
        cp5.addTextfield("StartTime")
                .setPosition(spacing, currentY)
                .setSize(width_x - 2 * spacing, textHeight)
                .setGroup(guiGroup)
                .setText("08:00")
                .setColor(textColor)
                .setColorBackground(backgroundColor)
                .setColorActive(activeColor)
                .setColorForeground(foregroundColor)
                .setColorLabel(labelColor)
                .setColorCursor(color(255))
                .setColorValue(color(255))
                .setCaptionLabel("START TIME (HH:MM)")
                .setAutoClear(false)
                .getCaptionLabel()
                .align(ControlP5.LEFT, ControlP5.TOP_OUTSIDE);

        currentY += textHeight + spacing;

        // End time input
        cp5.addTextfield("EndTime")
                .setPosition(spacing, currentY)
                .setSize(width_x - 2 * spacing, textHeight)
                .setGroup(guiGroup)
                .setText("17:00")
                .setColor(textColor)
                .setColorBackground(backgroundColor)
                .setColorActive(activeColor)
                .setColorForeground(foregroundColor)
                .setColorLabel(labelColor)
                .setColorCursor(color(255))
                .setColorValue(color(255))
                .setCaptionLabel("END TIME (HH:MM)")
                .setAutoClear(false)
                .getCaptionLabel()
                .align(ControlP5.LEFT, ControlP5.TOP_OUTSIDE);

        currentY += textHeight + spacing;

        // Simulation step input
        cp5.addTextfield("SimulationStep")
                .setPosition(spacing, currentY)
                .setSize(width_x - 2 * spacing, textHeight)
                .setGroup(guiGroup)
                .setText("1")
                .setColor(textColor)
                .setColorBackground(backgroundColor)
                .setColorActive(activeColor)
                .setColorForeground(foregroundColor)
                .setColorLabel(labelColor)
                .setColorCursor(color(255))
                .setColorValue(color(255))
                .setCaptionLabel("SIMULATION STEP (MINUTES)")
                .setAutoClear(false)
                .getCaptionLabel()
                .align(ControlP5.LEFT, ControlP5.TOP_OUTSIDE);

        currentY += textHeight + spacing;

        // Current time display (read-only)
        cp5.addTextfield("CurrentTime")
                .setPosition(spacing, currentY)
                .setSize(width_x - 2 * spacing, textHeight)
                .setGroup(guiGroup)
                .setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
                .setColor(textColor)
                .setColorBackground(color(40, 40, 40))
                .setColorForeground(color(40, 40, 40))
                .setColorLabel(labelColor)
                .setColorValue(color(200, 200, 200))
                .setCaptionLabel("CURRENT TIME")
                .setLock(true)
                .getCaptionLabel()
                .align(ControlP5.LEFT, ControlP5.TOP_OUTSIDE);

        currentY += textHeight + spacing;

        // Control buttons
        int buttonWidth = (width_x - 3 * spacing) / 2;
        cp5.addButton("Start")
                .setPosition(spacing, currentY)
                .setSize(buttonWidth, textHeight)
                .setGroup(guiGroup)
                .setColorLabel(labelColor)
                .setColorBackground(color(0, 100, 0))
                .setColorForeground(color(0, 150, 0))
                .setColorActive(color(0, 200, 0));

        cp5.addButton("Stop")
                .setPosition(2 * spacing + buttonWidth, currentY)
                .setSize(buttonWidth, textHeight)
                .setGroup(guiGroup)
                .setColorLabel(labelColor)
                .setColorBackground(color(100, 0, 0))
                .setColorForeground(color(150, 0, 0))
                .setColorActive(color(200, 0, 0));
    }

    /**
     * Process control event
     *
     * @param event GUI control events
     */
    public void controlEvent(ControlEvent event) {
        if (event.isController()) {
            String name = event.getController().getName();
            switch (name) {
                case "Month" -> {
                    selectedMonth = (int) event.getController().getValue() + 1;
                    updateDaysInMonth(selectedMonth);
                    updateCurrentTime();
                }
                case "Day" -> {
                    selectedDay = (int) event.getController().getValue() + 1;
                    updateCurrentTime();
                }
                case "Start" ->
                    startSimulation();
                case "Stop" ->
                    stopSimulation();
            }
        }
    }

    /**
     * Updates the days dropdown based on the selected month
     *
     * @param month The selected month (1-12)
     */
    private void updateDaysInMonth(int month) {
        DropdownList dayList = (DropdownList) cp5.get("Day");
        List<String> days = new ArrayList<>();
        int maxDays;

        switch (month) {
            case 2 -> {
                // February
                int year = currentTime.getYear();
                maxDays = ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) ? 29 : 28;
            }
            case 4, 6, 9, 11 -> // 30-day months
                maxDays = 30;
            default -> // 31-day months
                maxDays = 31;
        }

        for (int i = 1; i <= maxDays; i++) {
            days.add(String.format("%02d", i));
        }

        dayList.clear();
        dayList.addItems(days);

        if (selectedDay > maxDays) {
            selectedDay = maxDays;
        }
    }

    /**
     * Updates the current simulation time based on selected date
     */
    private void updateCurrentTime() {
        currentTime = currentTime.withMonth(selectedMonth).withDayOfMonth(selectedDay);
        startTime = currentTime;
        endTime = currentTime.withHour(17).withMinute(0);
    }

    /**
     * Starts the simulation with current parameters
     */
    private void startSimulation() {
        try {
            String startTimeStr = cp5.get(Textfield.class, "StartTime").getText();
            String endTimeStr = cp5.get(Textfield.class, "EndTime").getText();
            String dateStr = cp5.get(Textfield.class, "Date").getText();

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            LocalDate simDate = LocalDate.parse(dateStr, dateFormatter);
            LocalDateTime startDateTime = LocalDateTime.of(simDate, LocalTime.parse(startTimeStr, timeFormatter));
            LocalDateTime endDateTime = LocalDateTime.of(simDate, LocalTime.parse(endTimeStr, timeFormatter));

            currentTime = startDateTime;
            startTime = startDateTime;
            endTime = endDateTime;

            updateLocation();
            isAnimating = true;
        } catch (Exception e) {
            println("Invalid date or time format!");
        }
    }

    /**
     * Stops the simulation
     */
    private void stopSimulation() {
        isAnimating = false;
    }

    /**
     * Updates the location parameters from GUI inputs
     */
    private void updateLocation() {
        try {
            latitude = Double.parseDouble(cp5.get(Textfield.class, "Latitude").getText());
            longitude = Double.parseDouble(cp5.get(Textfield.class, "Longitude").getText());
            solarTracker = new SolarTracker(latitude, longitude, 0);
        } catch (NumberFormatException e) {
            println("Invalid location values!");
        }
    }

    /**
     * Main draw method called every frame
     */
    @Override
    public void draw() {
        background(135, 206, 235); // Sky blue background

        // Calculate current sun position
        SolarPosition currentSunPos = solarTracker.calculateSolarPosition(currentTime);

        pushMatrix();

        // Setup lighting based on sun position
        setupLighting(currentSunPos);

        drawGrid();
        drawCompassLabels();

        // Draw receiver tube
        pushMatrix();
        translate(0, 0, RECEIVER_HEIGHT);
        shape(receiverTube);
        popMatrix();

        // Handle animation updates
        if (isAnimating) {
            frameCounter++;
            int simulationStep = Integer.parseInt(cp5.get(Textfield.class, "SimulationStep").getText());
            float framesPerUpdate = frameRate; // Updates per second

            if (frameCounter >= framesPerUpdate) {
                currentTime = currentTime.plusMinutes(simulationStep);
                if (currentTime.isAfter(endTime)) {
                    isAnimating = false;
                }
                cp5.get(Textfield.class, "CurrentTime").setText(
                        currentTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                );
                frameCounter = 0;
            }
        }

        // Draw reflectors and sun
        drawReflectors(currentSunPos);
        drawSunAndRays(currentSunPos);

        popMatrix();

        // Draw GUI elements
        cam.beginHUD();
        cp5.draw();
        drawInfo(currentSunPos);
        cam.endHUD();
    }

    /**
     * Draws the reflector mirrors and their supports
     */
    private void drawReflectors(SolarPosition sunPos) {
        float sunDist = 1000;
        float azimuth = radians((float) sunPos.azimuthAngle);
        float altitude = radians((float) sunPos.altitudeAngle);
        // Calculate sun vector
        float sunX = -sunDist * cos(altitude) * sin(azimuth);
        float sunY = sunDist * cos(altitude) * cos(azimuth);
        float sunZ = sunDist * sin(altitude);
        for (int i = 0; i < NUM_REFLECTORS; i++) {
            float offset = (i < NUM_REFLECTORS / 2)
                    ? -(i + 0.5f) : (i - NUM_REFLECTORS / 2 + 0.5f);
            float x = offset * REFLECTOR_SPACING;
            pushMatrix();
            translate(x, 0, 0);
            // Draw support structure
            stroke(150);
            strokeWeight(1);
            drawSupport();
            pushMatrix();
            translate(0, 0, SUPPORT_HEIGHT + 2);
            float mirrorAngle = calculateOptimalMirrorAngle(x, sunPos);
            // Draw mirror angle text
            pushMatrix();
            translate(0, REFLECTOR_LENGTH / 2 + 1, -15);  // Aynanın önüne
            rotateX(3 * HALF_PI);  // Metni yere dik hale getir
            // Önce beyaz arka planı çiz
            pushMatrix();
            translate(0, 0, 1);  // Arka plan biraz önde
            fill(255);  // Beyaz renk
            noStroke();
            rectMode(CENTER);
            rect(0, 0, 50, 20);  // Arka plan dikdörtgeni
            popMatrix();
            // Sonra yazıyı çiz (arka plandan da önde)
            pushMatrix();
            translate(0, 0, 2);  // Yazı arka plandan da önde
            fill(0);
            textAlign(CENTER, CENTER);
            textSize(11);
            text(String.format("%.1f°", mirrorAngle), 0, 0);
            popMatrix();
            popMatrix();
            // Now rotate the mirror
            rotateY(radians(mirrorAngle));
            // Draw normal vector
            stroke(255, 0, 0);
            strokeWeight(2);
            float dashLength = 5;
            for (float j = 0; j < 100; j += dashLength * 2) {
                line(0, 0, j, 0, 0, j + dashLength);
            }
            // Draw mirror
            stroke(150);
            strokeWeight(1);
            shape(reflectors[i]);
            popMatrix();
            popMatrix();
        }
        // Draw sun
        pushMatrix();
        translate(sunX, sunY, sunZ);
        fill(255, 255, 0);
        noStroke();
        sphere(30);
        popMatrix();
        // Draw rays
        stroke(255, 255, 0, 100);
        strokeWeight(2);
        for (int i = 0; i < NUM_REFLECTORS; i++) {
            float offset = (i < NUM_REFLECTORS / 2)
                    ? -(i + 0.5f) : (i - NUM_REFLECTORS / 2 + 0.5f);
            float x = offset * REFLECTOR_SPACING;
            // Incident ray
            line(x, 0, SUPPORT_HEIGHT + 2, sunX, sunY, sunZ);
            // Reflected ray
            line(x, 0, SUPPORT_HEIGHT + 2, 0, 0, RECEIVER_HEIGHT);
        }
    }

    /**
     * Draws the sun and solar rays
     */
    private void drawSunAndRays(SolarPosition sunPos) {
        float sunDist = 1000;
        float azimuth = radians((float) sunPos.azimuthAngle);
        float altitude = radians((float) sunPos.altitudeAngle);

        float sunX = -sunDist * cos(altitude) * sin(azimuth);
        float sunY = sunDist * cos(altitude) * cos(azimuth);
        float sunZ = sunDist * sin(altitude);

        // Draw sun
        pushMatrix();
        translate(sunX, sunY, sunZ);
        fill(255, 255, 0);
        noStroke();
        sphere(30);
        popMatrix();

        // Draw rays
        stroke(255, 255, 0, 100);
        strokeWeight(2);
        for (int i = 0; i < NUM_REFLECTORS; i++) {
            float offset = (i < NUM_REFLECTORS / 2)
                    ? -(i + 0.5f) : (i - NUM_REFLECTORS / 2 + 0.5f);
            float x = offset * REFLECTOR_SPACING;
            // Incident ray
            line(x, 0, SUPPORT_HEIGHT + 2, sunX, sunY, sunZ);
            // Reflected ray
            line(x, 0, SUPPORT_HEIGHT + 2, 0, 0, RECEIVER_HEIGHT);
        }
    }

    /**
     * Sets up scene lighting based on sun position
     */
    private void setupLighting(SolarPosition sunPos) {
        float azimuth = radians((float) sunPos.azimuthAngle);
        float altitude = radians((float) sunPos.altitudeAngle);

        lights();
        directionalLight(255, 255, 200,
                -cos(altitude) * sin(azimuth),
                -cos(altitude) * cos(azimuth),
                sin(altitude));
        ambientLight(60, 60, 60);
    }

    /**
     * Draws simulation information overlay
     */
    private void drawInfo(SolarPosition sunPos) {
        fill(0);
        textAlign(LEFT);
        textSize(14);

        int infoX = 20;
        int infoY = height - 100;

        String dateStr = String.format("%02d/%02d/%d",
                currentTime.getDayOfMonth(),
                currentTime.getMonthValue(),
                currentTime.getYear());

        text("Date: " + dateStr, infoX, infoY);
        text("Time: " + currentTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                infoX, infoY + 20);
        text("Sun Altitude: " + String.format("%.1f°", sunPos.altitudeAngle),
                infoX, infoY + 40);
        text("Sun Azimuth: " + String.format("%.1f°", sunPos.azimuthAngle),
                infoX, infoY + 60);
    }

    /**
     * Draws the reference grid on the ground
     */
    private void drawGrid() {
        stroke(100);
        strokeWeight(1);
        int gridSize = 400;
        int spacing = 50;

        for (int x = -gridSize; x <= gridSize; x += spacing) {
            line(x, -gridSize, 0, x, gridSize, 0);
            line(-gridSize, x, 0, gridSize, x, 0);
        }
    }

    /**
     * Draws compass direction labels
     */
    private void drawCompassLabels() {
        textSize(16);
        textAlign(CENTER, CENTER);
        fill(0);

        float gridSize = 400;
        text("N", 0, -gridSize - 30);
        text("S", 0, gridSize + 30);
        text("E", -gridSize - 30, 0);
        text("W", gridSize + 30, 0);
    }

    /**
     * Calculates the optimal mirror angle for maximum reflection
     *
     * @param mirrorX X-position of the mirror
     * @param sunPos Current sun position
     * @return Optimal rotation angle in degrees
     */
    private float calculateOptimalMirrorAngle(float mirrorX, SolarPosition sunPos) {
        // Calculate sun ray vector
        float sunAltitude = radians((float) sunPos.altitudeAngle);
        float sunAzimuth = radians((float) sunPos.azimuthAngle);
        PVector sunRay = new PVector(
                -cos(sunAltitude) * sin(sunAzimuth),
                cos(sunAltitude) * cos(sunAzimuth),
                sin(sunAltitude)
        ).normalize();

        // Calculate target vector (to receiver)
        PVector targetRay = new PVector(
                -mirrorX,
                0,
                RECEIVER_HEIGHT - (SUPPORT_HEIGHT + 2)
        ).normalize();

        // Normal vector is the bisector of incident and reflected rays
        PVector normalVector = PVector.add(sunRay, targetRay).normalize();

        // Calculate rotation angle around Y axis
        float theta = degrees(atan2(normalVector.x, normalVector.z));

        return theta;
    }

    /**
     * Draws the support structure for a mirror
     */
    private void drawSupport() {
        // Support frame
        pushMatrix();
        fill(50);
        translate(0, 0, SUPPORT_HEIGHT / 2);
        box(REFLECTOR_WIDTH * 0.9f, REFLECTOR_LENGTH, 5);

        // Support legs
        translate(0, 0, -SUPPORT_HEIGHT / 2);
        float legSpacing = REFLECTOR_WIDTH * 0.4f;
        for (float xPos : new float[]{-legSpacing, legSpacing}) {
            for (float yPos : new float[]{-REFLECTOR_LENGTH * 0.4f, REFLECTOR_LENGTH * 0.4f}) {
                pushMatrix();
                translate(xPos, yPos, 0);
                box(5, 5, SUPPORT_HEIGHT);
                popMatrix();
            }
        }
        popMatrix();

    }
}
