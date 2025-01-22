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
import jazarifresnelsim.domain.SolarCalculator;
import jazarifresnelsim.models.MirrorPosition;
import jazarifresnelsim.ui.IGUIUpdateCallback;
import processing.event.MouseEvent;

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
        surface.setTitle("Linear Fresnel Reflector Simulation");

        // Önce state'i oluştur
        state = new SimulationState();

        solarCalculator = new SolarCalculator(state.getLatitude(), state.getLongitude(), 0);

        // Sonra controller'ı oluştur
        simulationController = new SimulationController(state);
        ((SimulationController) simulationController).setGUICallback(this);

        // Setup camera
        cam = new PeasyCam(this, 0, -REFLECTOR_LENGTH / 2, RECEIVER_HEIGHT / 2, 800);
        cam.setMinimumDistance(10);
        cam.setMaximumDistance(5000);

        // Setup renderer
        renderer = new FresnelRenderer(this, state);

        // Setup GUI
        setupGUI();

        // Initialize simulation with current time
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.withHour(8).withMinute(0).withSecond(0);
        LocalDateTime endTime = now.withHour(17).withMinute(0).withSecond(0);

        simulationController.setTimeRange(startTime, endTime);
        simulationController.setSimulationStep(1);

        // Start initial calculation without animation
        simulationController.updateSolarPosition();
        simulationController.updateMirrorPositions();

        // Disable automatic GUI drawing
        if (cp5 != null) {
            cp5.setAutoDraw(false);
        }
    }

    @Override
    public void onTimeUpdate(String currentTime) {
        if (cp5 != null) {
            Textfield currentTimeField = cp5.get(Textfield.class, "CURRENT TIME");
            if (currentTimeField != null) {
                currentTimeField.setText(currentTime);
                System.out.println("Current time updated to: " + currentTime);
            }
        }
    }

    private void setupGUI() {
        cp5 = new ControlP5(this);

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
        Group guiGroup = cp5.addGroup("SETTINGS")
                .setPosition(width - 20 - GUI_PANEL_WIDTH, 50)
                .setWidth(GUI_PANEL_WIDTH)
                .setBackgroundColor(backgroundColor)
                .setBackgroundHeight(GUI_PANEL_HEIGHT)
                .setBarHeight(GUI_BAR_HEIGHT);

        int currentY = GUI_SPACING + 10;

        // System Parameters
        addTextField("NUMBER OF MIRRORS", String.valueOf(NUM_REFLECTORS),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("RECEIVER HEIGHT", String.valueOf(RECEIVER_HEIGHT),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("RECEIVER DIAMETER", String.valueOf(RECEIVER_DIAMETER),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("MIRROR WIDTH", String.valueOf(REFLECTOR_WIDTH),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("MIRROR LENGTH", String.valueOf(REFLECTOR_LENGTH),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("MIRROR SPACING", String.valueOf(REFLECTOR_SPACING),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("SUPPORT HEIGHT", String.valueOf(SUPPORT_HEIGHT),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        // Latitude input
        addTextField("LATITUDE", String.valueOf(state.getLatitude()),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        // Longitude input
        addTextField("LONGITUDE", String.valueOf(state.getLongitude()),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        // Date input
        addTextField("DATE", state.getCurrentTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        // Time inputs
        addTextField("START TIME", "08:00",
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        addTextField("END TIME", "17:00",
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        // Simulation step input
        addTextField("SIMULATION STEP", "10",
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor);
        currentY += TEXT_HEIGHT + GUI_SPACING;

        // Current time display (read-only)
        addTextField("CURRENT TIME",
                state.getCurrentTime().format(DateTimeFormatter.ofPattern("HH:mm")).toString(),
                GUI_SPACING, currentY, GUI_PANEL_WIDTH - 2 * GUI_SPACING, TEXT_HEIGHT,
                guiGroup, backgroundColor, labelColor, textColor, activeColor, foregroundColor)
                .lock();
        currentY += TEXT_HEIGHT + GUI_SPACING;

        // Control buttons
        setupControlButtons(guiGroup, labelColor, currentY - 10);
    }

    private Textfield addTextField(String name, String defaultValue,
            int x, int y, int width, int height,
            Group group, int bgColor, int labelColor,
            int textColor, int activeColor, int fgColor) {
        Textfield field = cp5.addTextfield(name)
                .setPosition(x, y)
                .setSize(width, height)
                .setGroup(group)
                .setText(defaultValue)
                .setColor(textColor)
                .setColorBackground(bgColor)
                .setColorActive(activeColor)
                .setColorForeground(fgColor)
                .setColorLabel(labelColor)
                .setCaptionLabel(name.toUpperCase())
                .setAutoClear(false);

        // onChange event'ini field oluştuktan sonra ekleyelim
        field.onChange(event -> {
            System.out.println("TextField " + name + " changed to: " + cp5.get(Textfield.class, name).getText());
            if (name.equals("NUMBER OF MIRRORS")) {
                handleSystemParameterUpdate();
            }
        });

        field.getCaptionLabel().align(ControlP5.LEFT, ControlP5.TOP_OUTSIDE);

        return field;
    }

    private void setupControlButtons(Group group, int labelColor, int yPosition) {
        int buttonWidth = (GUI_PANEL_WIDTH - 3 * GUI_SPACING) / 2;

        // Start button
        cp5.addButton("Start")
                .setPosition(GUI_SPACING, yPosition)
                .setSize(buttonWidth, TEXT_HEIGHT + 5)
                .setGroup(group)
                .setColorLabel(labelColor)
                .setColorBackground(color(0, 100, 0))
                .setColorForeground(color(0, 150, 0))
                .setColorActive(color(0, 200, 0));

        // Stop button
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
            String value = "";
            if (event.getController() instanceof Textfield) {
                value = ((Textfield) event.getController()).getText();
            }
            System.out.println("Control Event: " + name + " = " + value);

            switch (name) {
                case "Start" -> {
                    System.out.println("\nChecking all time values before start:");
                    System.out.println("START TIME: " + cp5.get(Textfield.class, "START TIME").getText());
                    System.out.println("END TIME: " + cp5.get(Textfield.class, "END TIME").getText());
                    System.out.println("DATE: " + cp5.get(Textfield.class, "DATE").getText());
                    handleStart();
                }
                case "Stop" ->
                    simulationController.stopSimulation();

                case "LATITUDE", "LONGITUDE" ->
                    handleLocationUpdate();

                case "NUMBER OF MIRRORS", "RECEIVER HEIGHT", "RECEIVER DIAMETER", "MIRROR WIDTH", "MIRROR LENGTH", "MIRROR SPACING", "SUPPORT HEIGHT" ->
                    handleSystemParameterUpdate();

                case "START TIME", "END TIME" -> {
                    System.out.println("\nTime input changed:");
                    System.out.println("Field: " + name + " = " + value);
                    // İsterseniz burada zaman değişikliklerini hemen işleyebiliriz
                    handleTimeUpdate();
                }
            }
        }
    }

    private void handleTimeUpdate() {
        try {
            String startTimeStr = cp5.get(Textfield.class, "START TIME").getText();
            String endTimeStr = cp5.get(Textfield.class, "END TIME").getText();
            String dateStr = cp5.get(Textfield.class, "DATE").getText();

            System.out.println("Processing time update:");
            System.out.println("Start Time: " + startTimeStr);
            System.out.println("End Time: " + endTimeStr);
            System.out.println("Date: " + dateStr);

            LocalDateTime startDateTime = LocalDateTime.parse(
                    dateStr + " " + startTimeStr,
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            );
            LocalDateTime endDateTime = LocalDateTime.parse(
                    dateStr + " " + endTimeStr,
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            );

            // State'i güncelle
            state.setStartTime(startDateTime);
            state.setEndTime(endDateTime);

            System.out.println("Time values updated in state");

        } catch (Exception e) {
            System.out.println("Error updating time values: " + e.getMessage());
        }
    }

    private void reinitializeSystem() {
        // Renderer'ı yeni state ile yeniden oluştur
        renderer = new FresnelRenderer(this, state);
        // Controller'ı güncelle
        simulationController.updateMirrorPositions();
        simulationController.updateSolarPosition();
    }

//    private void handleSystemParameterUpdate() {
//        try {
//            System.out.println("\nReading all parameters from GUI...");
//
//            // Sistem parametrelerini oku
//            int numMirrors = Integer.parseInt(cp5.get(Textfield.class, "NUMBER OF MIRRORS").getText());
//            float recHeight = Float.parseFloat(cp5.get(Textfield.class, "RECEIVER HEIGHT").getText());
//            float recDiameter = Float.parseFloat(cp5.get(Textfield.class, "RECEIVER DIAMETER").getText());
//            float mirrorWidth = Float.parseFloat(cp5.get(Textfield.class, "MIRROR WIDTH").getText());
//            float mirrorLength = Float.parseFloat(cp5.get(Textfield.class, "MIRROR LENGTH").getText());
//            float mirrorSpacing = Float.parseFloat(cp5.get(Textfield.class, "MIRROR SPACING").getText());
//            float supportHeight = Float.parseFloat(cp5.get(Textfield.class, "SUPPORT HEIGHT").getText());
//
//            // Konum parametrelerini oku
//            double latitude = Double.parseDouble(cp5.get(Textfield.class, "LATITUDE").getText());
//            double longitude = Double.parseDouble(cp5.get(Textfield.class, "LONGITUDE").getText());
//
//            // Zaman parametrelerini oku
//            String dateStr = cp5.get(Textfield.class, "DATE").getText();
//            String startTimeStr = cp5.get(Textfield.class, "START TIME").getText();
//            String endTimeStr = cp5.get(Textfield.class, "END TIME").getText();
//            double simStep = Double.parseDouble(cp5.get(Textfield.class, "SIMULATION STEP").getText());
//
//            System.out.println("\nUpdating state with new values...");
//
//            // Sistem parametrelerini güncelle
//            state.setNumReflectors(numMirrors);
//            state.setReceiverHeight(recHeight);
//            state.setReceiverDiameter(recDiameter);
//            state.setReflectorWidth(mirrorWidth);
//            state.setReflectorLength(mirrorLength);
//            state.setReflectorSpacing(mirrorSpacing);
//            state.setSupportHeight(supportHeight);
//
//            // Konum parametrelerini güncelle
//            state.setLatitude(latitude);
//            state.setLongitude(longitude);
//
//            // Zaman parametrelerini güncelle
//            try {
//                LocalDateTime startDateTime = LocalDateTime.parse(
//                        dateStr + " " + startTimeStr,
//                        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
//                );
//                LocalDateTime endDateTime = LocalDateTime.parse(
//                        dateStr + " " + endTimeStr,
//                        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
//                );
//
//                state.setStartTime(startDateTime);
//                state.setEndTime(endDateTime);
//                state.setCurrentTime(startDateTime);
//                state.setSimulationStepMinutes(simStep);
//
//            } catch (Exception e) {
//                System.out.println("Error parsing date/time values: " + e.getMessage());
//            }
//
//            System.out.println("State updated. Reinitializing system...");
//
//            // Güneş hesaplayıcıyı güncelle
//            simulationController.setLocation(latitude, longitude);
//
//            // Sistem parametrelerini güncelle
//            reinitializeSystem();
//
//            System.out.println("System reinitialization complete.");
//
//        } catch (NumberFormatException e) {
//            System.out.println("Error parsing numeric values: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
    private void handleSystemParameterUpdate() {
        try {
            System.out.println("\nReading all parameters from GUI...");

            // Sistem parametrelerini oku
            int numMirrors = Integer.parseInt(cp5.get(Textfield.class, "NUMBER OF MIRRORS").getText());
            float recHeight = Float.parseFloat(cp5.get(Textfield.class, "RECEIVER HEIGHT").getText());
            float recDiameter = Float.parseFloat(cp5.get(Textfield.class, "RECEIVER DIAMETER").getText());
            float mirrorWidth = Float.parseFloat(cp5.get(Textfield.class, "MIRROR WIDTH").getText());
            float mirrorLength = Float.parseFloat(cp5.get(Textfield.class, "MIRROR LENGTH").getText());
            float mirrorSpacing = Float.parseFloat(cp5.get(Textfield.class, "MIRROR SPACING").getText());
            float supportHeight = Float.parseFloat(cp5.get(Textfield.class, "SUPPORT HEIGHT").getText());

            // Konum parametrelerini oku
            double latitude = Double.parseDouble(cp5.get(Textfield.class, "LATITUDE").getText());
            double longitude = Double.parseDouble(cp5.get(Textfield.class, "LONGITUDE").getText());

            // Tarih ve zaman parametrelerini oku
            String dateStr = cp5.get(Textfield.class, "DATE").getText();
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            double simStep = Double.parseDouble(cp5.get(Textfield.class, "SIMULATION STEP").getText());

            // Sistem parametrelerini güncelle
            state.setNumReflectors(numMirrors);
            state.setReceiverHeight(recHeight);
            state.setReceiverDiameter(recDiameter);
            state.setReflectorWidth(mirrorWidth);
            state.setReflectorLength(mirrorLength);
            state.setReflectorSpacing(mirrorSpacing);
            state.setSupportHeight(supportHeight);

            // Konum parametrelerini güncelle
            state.setLatitude(latitude);
            state.setLongitude(longitude);

            // Güneş doğuş-batış saatlerini hesapla
            SolarCalculator calculator = new SolarCalculator(latitude, longitude, 0);
            DaylightTimes daylight = calculator.calculateSunriseSunset(date);

            // GUI'deki start ve end time'ı güncelle
            String sunriseStr = daylight.getSunrise().format(DateTimeFormatter.ofPattern("HH:mm"));
            String sunsetStr = daylight.getSunset().format(DateTimeFormatter.ofPattern("HH:mm"));

            cp5.get(Textfield.class, "START TIME").setText(sunriseStr);
            cp5.get(Textfield.class, "END TIME").setText(sunsetStr);

            // State'i yeni zaman değerleriyle güncelle
            state.setStartTime(daylight.getSunrise());
            state.setEndTime(daylight.getSunset());
            state.setCurrentTime(daylight.getSunrise());
            state.setSimulationStepMinutes(simStep);

            System.out.println("State updated. Reinitializing system...");

            // Güneş hesaplayıcıyı güncelle
            simulationController.setLocation(latitude, longitude);

            // Sistem parametrelerini güncelle
            reinitializeSystem();

            System.out.println("System reinitialization complete.");

        } catch (NumberFormatException e) {
            System.out.println("Error parsing numeric values: " + e.getMessage());
            e.printStackTrace();
        } catch (DateTimeParseException e) {
            System.out.println("Error parsing date: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleStart() {
        try {
            System.out.println("\nStart button pressed - updating all parameters...");

            // Tüm parametreleri güncelle
            handleSystemParameterUpdate();

            // Simülasyonu başlat
            simulationController.startSimulation();

        } catch (Exception e) {
            System.out.println("Error in handleStart:");
            System.out.println("Error message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void draw() {
        // Mouse GUI üzerindeyse kamerayı devre dışı bırak
        if (cam != null) {
            cam.setActive(!isMouseOverGUI());
        }
        background(135, 206, 235); // Sky blue background

        simulationController.update();
        renderer.render();

        // GUI should be drawn after 3D scene and not affected by camera
        cam.beginHUD();
        cp5.draw();
        simulationController.updateGUIDisplay(cp5);  // GUI güncelleme
        drawInfo();
        cam.endHUD();
    }

    private void drawInfo() {
        fill(0);
        textAlign(LEFT);
        textSize(14);

        int infoX = 20;
        int infoY = height - 140;  // Adjusted for more info

        String dateStr = state.getCurrentTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String timeStr = state.getCurrentTime().format(DateTimeFormatter.ofPattern("HH:mm"));

        SolarPosition sunPos = state.getCurrentSolarPosition();
        if (sunPos != null) {
            // Basic info
            text("Date: " + dateStr, infoX, infoY);
            text("Time: " + timeStr, infoX, infoY + 20);
            text(String.format("Sun Altitude: %.1f°", sunPos.getAltitudeAngle()),
                    infoX, infoY + 40);
            text(String.format("Sun Azimuth: %.1f°", sunPos.getAzimuthAngle()),
                    infoX, infoY + 60);

            // Energy calculations
            double totalEnergy = solarCalculator.calculateTotalEnergy(state);
            text(String.format("Total Energy: %.1f W (%.2f kW)",
                    totalEnergy, totalEnergy / 1000), infoX, infoY + 80);

            // Efficiency information
            List<MirrorPosition> mirrors = state.getMirrorPositions();
            double avgSpillage = mirrors.stream()
                    .mapToDouble(m -> solarCalculator.calculateSpillageLoss(m, state))
                    .average()
                    .orElse(0.0);
            double avgBlocking = mirrors.stream()
                    .mapToDouble(m -> solarCalculator.calculateBlockingAndShadingLoss(m, mirrors, state, sunPos))
                    .average()
                    .orElse(0.0);

            text(String.format("Spillage Efficiency: %.1f%%", avgSpillage * 100),
                    infoX, infoY + 100);
            text(String.format("Blocking Efficiency: %.1f%%", avgBlocking * 100),
                    infoX, infoY + 120);
        }
    }

    // Konum güncellendiğinde SolarCalculator'ı da güncelle
    private void handleLocationUpdate() {
        try {
            double lat = Double.parseDouble(cp5.get(Textfield.class, "LATITUDE").getText());
            double lon = Double.parseDouble(cp5.get(Textfield.class, "LONGITUDE").getText());
            simulationController.setLocation(lat, lon);
            solarCalculator.updateLocation(lat, lon);  // Yeni eklenen
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

    // FresnelSimulator sınıfına eklenecek yeni metod
    private boolean isMouseOverGUI() {
        // Settings panel konumunu al
        int guiX = width - 20 - GUI_PANEL_WIDTH;
        int guiY = 50;

        // Grup nesnesini al
        Group settingsGroup = cp5.get(Group.class, "SETTINGS");
        if (settingsGroup != null) {
            // Grup açıksa tam yüksekliği, kapalıysa sadece başlık çubuğu yüksekliğini kullan
            int effectiveHeight = settingsGroup.isOpen() ? GUI_PANEL_HEIGHT : GUI_BAR_HEIGHT;

            return (mouseX >= guiX && mouseX <= guiX + GUI_PANEL_WIDTH
                    && mouseY >= guiY && mouseY <= guiY + effectiveHeight);
        }

        return false;
    }

    // Mouse olaylarını ele almak için
    @Override
    public void mousePressed() {
        if (isMouseOverGUI()) {
            cam.setActive(false);
        }
    }

    @Override
    public void mouseReleased() {
        // Mouse bırakıldığında kamerayı tekrar aktif et
        // Ancak hala GUI üzerinde değilse
        if (!isMouseOverGUI()) {
            cam.setActive(true);
        }
    }

    // Mouse wheel olayını özelleştirmek için
    @Override
    public void mouseWheel(MouseEvent event) {
        if (isMouseOverGUI()) {
            // GUI üzerindeyken wheel olayını engelle
            return;
        }
    }
}
