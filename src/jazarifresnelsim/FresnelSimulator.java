package jazarifresnelsim;

import jazarifresnelsim.ui.FresnelRenderer;
import jazarifresnelsim.models.SimulationState;
import jazarifresnelsim.models.SolarPosition;
import jazarifresnelsim.ui.IRenderer;
import jazarifresnelsim.core.ISimulationController;
import processing.core.PApplet;
import processing.core.PFont;
import controlP5.*;
import java.time.LocalDate;
import peasy.PeasyCam;
import jazarifresnelsim.core.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import static jazarifresnelsim.domain.Constants.*;
import jazarifresnelsim.domain.DaylightTimes;
import jazarifresnelsim.domain.ShadingDetector;
import jazarifresnelsim.domain.SolarCalculator;
import jazarifresnelsim.models.MirrorPosition;
import jazarifresnelsim.ui.IGUIUpdateCallback;
import processing.event.MouseEvent;

/**
 * Processing-based 3D interactive LFR simulator.
 *
 * VERSION 4.0 — All deprecated Constants aliases replaced.
 *
 * @author Yunus Demirtas, Musa Atas — Siirt University
 * @version 4.0
 */
public class FresnelSimulator extends PApplet implements IGUIUpdateCallback {

    private SimulationState state;
    private ISimulationController simulationController;
    private IRenderer renderer;
    private PeasyCam cam;
    private ControlP5 cp5;
    private SolarCalculator solarCalculator;

    public static void main(String[] args) {
        PApplet.main(new String[]{FresnelSimulator.class.getName()});
    }

    @Override
    public void settings() {
        size(WINDOW_WIDTH, WINDOW_HEIGHT, P3D);
        smooth(8);
    }

    @Override
    public void setup() {
        surface.setTitle("Jazari Linear Fresnel Reflector Simulation");

        state = new SimulationState();
        solarCalculator = new SolarCalculator(state.getLatitude(), state.getLongitude(), 0);

        simulationController = new SimulationController(state);
        ((SimulationController) simulationController).setGUICallback(this);

        // Camera — use state values instead of deprecated constants
        cam = new PeasyCam(this, 0,
                -state.getReflectorLength() / 2,
                state.getReceiverHeight() / 2, 800);
        cam.setMinimumDistance(10);
        cam.setMaximumDistance(5000);

        renderer = new FresnelRenderer(this, state);

        setupGUI();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.withHour(8).withMinute(0).withSecond(0);
        LocalDateTime endTime = now.withHour(17).withMinute(0).withSecond(0);

        simulationController.setTimeRange(startTime, endTime);
        simulationController.setSimulationStep(1);
        simulationController.updateSolarPosition();
        simulationController.updateMirrorPositions();

        if (cp5 != null) {
            cp5.setAutoDraw(false);
        }
    }

    @Override
    public void onTimeUpdate(String currentTime) {
        if (cp5 != null) {
            Textfield field = cp5.get(Textfield.class, "CURRENT TIME");
            if (field != null) {
                field.setText(currentTime);
            }
        }
    }

    private void setupGUI() {
        cp5 = new ControlP5(this);

        PFont guiFont = createFont("Arial Bold", 14);
        cp5.setFont(guiFont);

        int backgroundColor = color(0, 20, 50);
        int labelColor = color(255);
        int textColor = color(200, 255, 255);
        int activeColor = color(0, 100, 200);
        int foregroundColor = color(100, 150, 200);

        Group guiGroup = cp5.addGroup("SETTINGS")
                .setPosition(width - 20 - GUI_PANEL_WIDTH, 50)
                .setWidth(GUI_PANEL_WIDTH)
                .setBackgroundColor(backgroundColor)
                .setBackgroundHeight(GUI_PANEL_HEIGHT)
                .setBarHeight(GUI_BAR_HEIGHT);

        int currentY = GUI_SPACING + 10;

        // Use new canonical constant names for initial values
        addTextField("NUMBER OF MIRRORS", String.valueOf(NUM_MIRRORS),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("RECEIVER HEIGHT", String.valueOf(RECEIVER_HEIGHT_CM),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("RECEIVER DIAMETER", String.valueOf(RECEIVER_DIAMETER_CM),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("MIRROR WIDTH", String.valueOf(MIRROR_WIDTH_CM),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("MIRROR LENGTH", String.valueOf(MIRROR_LENGTH_CM),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("MIRROR SPACING", String.valueOf(MIRROR_SPACING_CM),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("SUPPORT HEIGHT", String.valueOf(SUPPORT_HEIGHT_CM),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("LATITUDE", String.valueOf(state.getLatitude()),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("LONGITUDE", String.valueOf(state.getLongitude()),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("DATE", state.getCurrentTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("START TIME", "08:00",
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("END TIME", "17:00",
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("SIMULATION STEP", "10",
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("CURRENT TIME",
                state.getCurrentTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor)
                .lock();
        currentY += TEXT_HEIGHT + GUI_SPACING;

        setupControlButtons(guiGroup, labelColor, currentY - 10);
    }

    private Textfield addTextField(String name, String defaultValue,
            int x, int y, int w, int h,
            Group group, int bgColor, int labelColor,
            int textColor, int activeColor, int fgColor) {

        Textfield field = cp5.addTextfield(name)
                .setPosition(x, y)
                .setSize(w, h)
                .setGroup(group)
                .setText(defaultValue)
                .setColor(textColor)
                .setColorBackground(bgColor)
                .setColorActive(activeColor)
                .setColorForeground(fgColor)
                .setColorLabel(labelColor)
                .setCaptionLabel(name.toUpperCase())
                .setAutoClear(false);

        field.onChange(event -> {
            if (name.equals("NUMBER OF MIRRORS")) {
                handleSystemParameterUpdate();
            }
        });

        field.getCaptionLabel().align(ControlP5.LEFT, ControlP5.TOP_OUTSIDE);
        return field;
    }

    private void setupControlButtons(Group group, int labelColor, int yPosition) {
        int buttonWidth = (GUI_PANEL_WIDTH - 3 * GUI_SPACING) / 2;

        cp5.addButton("Start")
                .setPosition(GUI_SPACING, yPosition)
                .setSize(buttonWidth, TEXT_HEIGHT + 5)
                .setGroup(group)
                .setColorLabel(labelColor)
                .setColorBackground(color(0, 100, 0))
                .setColorForeground(color(0, 150, 0))
                .setColorActive(color(0, 200, 0));

        cp5.addButton("Stop")
                .setPosition(2 * GUI_SPACING + buttonWidth, yPosition)
                .setSize(buttonWidth, TEXT_HEIGHT + 5)
                .setGroup(group)
                .setColorLabel(labelColor)
                .setColorBackground(color(100, 0, 0))
                .setColorForeground(color(150, 0, 0))
                .setColorActive(color(200, 0, 0));
    }

    public void controlEvent(ControlEvent event) {
        if (event.isController()) {
            String name = event.getController().getName();

            switch (name) {
                case "Start" ->
                    handleStart();
                case "Stop" ->
                    simulationController.stopSimulation();

                case "LATITUDE", "LONGITUDE" ->
                    handleLocationUpdate();

                case "NUMBER OF MIRRORS", "RECEIVER HEIGHT", "RECEIVER DIAMETER", "MIRROR WIDTH", "MIRROR LENGTH", "MIRROR SPACING", "SUPPORT HEIGHT" ->
                    handleSystemParameterUpdate();

                case "START TIME", "END TIME" ->
                    handleTimeUpdate();
            }
        }
    }

    private void handleTimeUpdate() {
        try {
            String startTimeStr = cp5.get(Textfield.class, "START TIME").getText();
            String endTimeStr = cp5.get(Textfield.class, "END TIME").getText();
            String dateStr = cp5.get(Textfield.class, "DATE").getText();

            LocalDateTime startDT = LocalDateTime.parse(
                    dateStr + " " + startTimeStr,
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            LocalDateTime endDT = LocalDateTime.parse(
                    dateStr + " " + endTimeStr,
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

            state.setStartTime(startDT);
            state.setEndTime(endDT);
        } catch (Exception e) {
            System.out.println("Error updating time: " + e.getMessage());
        }
    }

    private void reinitializeSystem() {
        renderer = new FresnelRenderer(this, state);
        simulationController.updateMirrorPositions();
        simulationController.updateSolarPosition();
    }

    private void handleSystemParameterUpdate() {
        try {
            int numMirrors = Integer.parseInt(cp5.get(Textfield.class, "NUMBER OF MIRRORS").getText());
            float recHeight = Float.parseFloat(cp5.get(Textfield.class, "RECEIVER HEIGHT").getText());
            float recDiameter = Float.parseFloat(cp5.get(Textfield.class, "RECEIVER DIAMETER").getText());
            float mirrorWidth = Float.parseFloat(cp5.get(Textfield.class, "MIRROR WIDTH").getText());
            float mirrorLength = Float.parseFloat(cp5.get(Textfield.class, "MIRROR LENGTH").getText());
            float mirrorSpacing = Float.parseFloat(cp5.get(Textfield.class, "MIRROR SPACING").getText());
            float supportHeight = Float.parseFloat(cp5.get(Textfield.class, "SUPPORT HEIGHT").getText());

            double latitude = Double.parseDouble(cp5.get(Textfield.class, "LATITUDE").getText());
            double longitude = Double.parseDouble(cp5.get(Textfield.class, "LONGITUDE").getText());

            String dateStr = cp5.get(Textfield.class, "DATE").getText();
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            double simStep = Double.parseDouble(cp5.get(Textfield.class, "SIMULATION STEP").getText());

            state.setNumReflectors(numMirrors);
            state.setReceiverHeight(recHeight);
            state.setReceiverDiameter(recDiameter);
            state.setReflectorWidth(mirrorWidth);
            state.setReflectorLength(mirrorLength);
            state.setReflectorSpacing(mirrorSpacing);
            state.setSupportHeight(supportHeight);
            state.setLatitude(latitude);
            state.setLongitude(longitude);

            SolarCalculator calculator = new SolarCalculator(latitude, longitude, 0);
            DaylightTimes daylight = calculator.calculateSunriseSunset(date);

            cp5.get(Textfield.class, "START TIME").setText(
                    daylight.getSunrise().format(DateTimeFormatter.ofPattern("HH:mm")));
            cp5.get(Textfield.class, "END TIME").setText(
                    daylight.getSunset().format(DateTimeFormatter.ofPattern("HH:mm")));

            state.setStartTime(daylight.getSunrise());
            state.setEndTime(daylight.getSunset());
            state.setCurrentTime(daylight.getSunrise());
            state.setSimulationStepMinutes(simStep);

            simulationController.setLocation(latitude, longitude);
            reinitializeSystem();

        } catch (NumberFormatException e) {
            System.out.println("Error parsing numeric values: " + e.getMessage());
        } catch (DateTimeParseException e) {
            System.out.println("Error parsing date: " + e.getMessage());
        }
    }

    private void handleStart() {
        try {
            handleSystemParameterUpdate();
            simulationController.startSimulation();
        } catch (Exception e) {
            System.out.println("Error in handleStart: " + e.getMessage());
        }
    }

    @Override
    public void draw() {
        if (cam != null) {
            cam.setActive(!isMouseOverGUI());
        }
        background(135, 206, 235);

        simulationController.update();
        renderer.render();

        cam.beginHUD();
        cp5.draw();
        simulationController.updateGUIDisplay(cp5);
        drawInfo();
        cam.endHUD();
    }

    private void drawInfo() {
        fill(0);
        textAlign(LEFT);
        textSize(14);

        int infoX = 20;
        int infoY = height - 200;

        String dateStr = state.getCurrentTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String timeStr = state.getCurrentTime().format(DateTimeFormatter.ofPattern("HH:mm"));

        SolarPosition sunPos = state.getCurrentSolarPosition();
        if (sunPos != null) {
            text("Date: " + dateStr, infoX, infoY);
            text("Time: " + timeStr, infoX, infoY + 20);
            text(String.format("Sun Altitude: %.1f°", sunPos.getAltitudeAngle()),
                    infoX, infoY + 40);
            text(String.format("Sun Azimuth: %.1f°", sunPos.getAzimuthAngle()),
                    infoX, infoY + 60);

            // Optical power — pure Q_opt, no thermal
            double opticalPower = solarCalculator.calculateOpticalPower(state);
            text(String.format("Optical Power: %.1f W (%.2f kW)",
                    opticalPower, opticalPower / 1000), infoX, infoY + 80);

            List<MirrorPosition> mirrors = state.getMirrorPositions();
            double avgSpillage = mirrors.stream()
                    .mapToDouble(m -> solarCalculator.calculateSpillageLoss(m, state))
                    .average().orElse(0.0);

            ShadingDetector shadingDetector = new ShadingDetector();
            double avgBlocking = mirrors.stream()
                    .mapToDouble(m -> shadingDetector.calculateBlockingAndShadingLoss(
                    m, mirrors, state, sunPos))
                    .average().orElse(0.0);

            text(String.format("Spillage Factor: %.1f%%", avgSpillage * 100),
                    infoX, infoY + 100);
            text(String.format("Shading Efficiency: %.1f%%", avgBlocking * 100),
                    infoX, infoY + 120);
            text(String.format("Mirror Spacing (p): %.1f cm", state.getReflectorSpacing()), infoX, infoY + 140);
            text(String.format("Net Gap (p-w): %.1f cm", (state.getReflectorSpacing() - state.getReflectorWidth())), infoX, infoY + 160);
            text(String.format("FPS: %.1f", frameRate),
                    infoX, infoY + 180);
        }
    }

    private void handleLocationUpdate() {
        try {
            double lat = Double.parseDouble(cp5.get(Textfield.class, "LATITUDE").getText());
            double lon = Double.parseDouble(cp5.get(Textfield.class, "LONGITUDE").getText());
            simulationController.setLocation(lat, lon);
            solarCalculator.updateLocation(lat, lon);
        } catch (NumberFormatException e) {
            println("Invalid location values");
        }
    }

    @Override
    public void dispose() {
        if (cp5 != null) {
            cp5.dispose();
        }
        super.dispose();
    }

    private boolean isMouseOverGUI() {
        int guiX = width - 20 - GUI_PANEL_WIDTH;
        int guiY = 50;
        Group settingsGroup = cp5.get(Group.class, "SETTINGS");
        if (settingsGroup != null) {
            int effectiveHeight = settingsGroup.isOpen() ? GUI_PANEL_HEIGHT : GUI_BAR_HEIGHT;
            return (mouseX >= guiX && mouseX <= guiX + GUI_PANEL_WIDTH
                    && mouseY >= guiY && mouseY <= guiY + effectiveHeight);
        }
        return false;
    }

    @Override
    public void mousePressed() {
        if (isMouseOverGUI()) {
            cam.setActive(false);
        }
    }

    @Override
    public void mouseReleased() {
        if (!isMouseOverGUI()) {
            cam.setActive(true);
        }
    }

    @Override
    public void mouseWheel(MouseEvent event) {
        if (isMouseOverGUI()) {
            return;
        }
    }
}
