package jazarifresnelsim.ui;

import jazarifresnelsim.models.SimulationState;
import jazarifresnelsim.models.MirrorPosition;
import jazarifresnelsim.models.SolarPosition;
import java.util.List;
import processing.core.*;
import static jazarifresnelsim.domain.Constants.*;

public class FresnelRenderer implements IRenderer {

    private final PApplet sketch;
    private final SimulationState state;
    private PShape receiverTube;
    private PShape[] reflectors;

    public FresnelRenderer(PApplet sketch, SimulationState state) {
        this.sketch = sketch;
        this.state = state;
        initializeModels();
    }

    private void initializeModels() {
        // Create receiver tube model
        receiverTube = sketch.createShape(PApplet.GROUP);
        float radius = state.getReceiverDiameter() / 2;
        int sides = 20;

        // Main tube
        PShape tube = sketch.createShape();
        tube.beginShape(PApplet.TRIANGLE_STRIP);
        tube.fill(100, 50, 50);
        tube.noStroke();

        for (float angle = 0; angle <= PApplet.TWO_PI + 0.1; angle += PApplet.TWO_PI / sides) {
            float x = PApplet.cos(angle) * radius;
            float z = PApplet.sin(angle) * radius;
            // Tube'un merkezi referans noktası olsun
            tube.vertex(x, -state.getReflectorLength() / 2, z);
            tube.vertex(x, state.getReflectorLength() / 2, z);
        }
        tube.endShape();
        receiverTube.addChild(tube);
        
        // Support structures
        PShape supports = sketch.createShape();
        supports.beginShape(PApplet.TRIANGLES);
        supports.fill(70);
        supports.noStroke();

        float supportWidth = 15;
        float supportSpacing = state.getReflectorLength() * 0.8f;

        // End supports
        for (float pos : new float[]{-supportSpacing / 2, supportSpacing / 2}) {
            supports.vertex(-supportWidth / 2, pos, 0);
            supports.vertex(supportWidth / 2, pos, 0);
            supports.vertex(0, pos, -state.getReceiverHeight());
        }
        supports.endShape();
        receiverTube.addChild(supports);

        // Create reflector mirrors
        reflectors = new PShape[state.getNumReflectors()];
        for (int i = 0; i < state.getNumReflectors(); i++) {
            reflectors[i] = createReflectorShape();
        }
    }

    private PShape createReflectorShape() {
        PShape reflector = sketch.createShape(PApplet.GROUP);

        // Ön yüz (ayna yüzeyi)
        PShape frontFace = sketch.createShape();
        frontFace.beginShape(PApplet.QUADS);
        frontFace.fill(240, 240, 240);  // Parlak beyaz
        frontFace.ambient(200);
        frontFace.specular(255);
        frontFace.shininess(100);
        frontFace.stroke(200);
        frontFace.strokeWeight(1);
        frontFace.normal(0, 0, 1);
        frontFace.vertex(-state.getReflectorWidth() / 2, -state.getReflectorLength() / 2, 0);
        frontFace.vertex(state.getReflectorWidth() / 2, -state.getReflectorLength() / 2, 0);
        frontFace.vertex(state.getReflectorWidth() / 2, state.getReflectorLength() / 2, 0);
        frontFace.vertex(-state.getReflectorWidth() / 2, state.getReflectorLength() / 2, 0);
        frontFace.endShape();

        // Arka yüz
        PShape backFace = sketch.createShape();
        backFace.beginShape(PApplet.QUADS);
        backFace.fill(100, 100, 100);  // Koyu gri
        backFace.stroke(150);
        backFace.strokeWeight(1);
        backFace.normal(0, 0, -1);
        backFace.vertex(-state.getReflectorWidth() / 2, state.getReflectorLength() / 2, -1);
        backFace.vertex(state.getReflectorWidth() / 2, state.getReflectorLength() / 2, -1);
        backFace.vertex(state.getReflectorWidth() / 2, -state.getReflectorLength() / 2, -1);
        backFace.vertex(-state.getReflectorWidth() / 2, -state.getReflectorLength() / 2, -1);
        backFace.endShape();

        // İki yüzü grup olarak birleştir
        reflector.addChild(frontFace);
        reflector.addChild(backFace);

        return reflector;
    }

    @Override
    public void render() {
        sketch.pushMatrix();  // Ana çizim transformasyonu

        SolarPosition sunPos = state.getCurrentSolarPosition();
        if (sunPos != null) {
            setupLighting(sunPos);
        }

        // Önce grid ve compass
        drawGrid();
        drawCompassLabels();

        // Debug için pozisyonları kontrol edelim
        List<MirrorPosition> positions = state.getMirrorPositions();
        //System.out.println("Mirror positions size: " + (positions != null ? positions.size() : "null"));

        // Sonra receiver tube
        drawReceiverTube();

        if (sunPos != null) {
            // En son aynalar ve güneş ışınları
            drawReflectors(state.getMirrorPositions(), sunPos);
            drawSunAndRays(sunPos);
        }

        sketch.popMatrix();
    }

    private void setupLighting(SolarPosition sunPos) {
        sketch.lights();

        float azimuth = sketch.radians((float) sunPos.getAzimuthAngle());
        float altitude = sketch.radians((float) sunPos.getAltitudeAngle());

        // Ana güneş ışığı
        sketch.directionalLight(255, 255, 200,
                -sketch.cos(altitude) * sketch.sin(azimuth),
                -sketch.cos(altitude) * sketch.cos(azimuth),
                sketch.sin(altitude));

        // Yardımcı ışık kaynağı (fill light)
        sketch.pointLight(100, 100, 100, // Işık rengi
                0, 0, RECEIVER_HEIGHT * 2);  // Işık pozisyonu

        // Ambient ışık
        sketch.ambientLight(120, 120, 120);
    }

    private void drawGrid() {
        sketch.stroke(100);
        sketch.strokeWeight(1);
        int gridSize = 400;
        int spacing = 50;

        for (int x = -gridSize; x <= gridSize; x += spacing) {
            sketch.line(x, -gridSize, 0, x, gridSize, 0);
            sketch.line(-gridSize, x, 0, gridSize, x, 0);
        }
    }

    private void drawCompassLabels() {
        sketch.textSize(16);
        sketch.textAlign(PApplet.CENTER, PApplet.CENTER);
        sketch.fill(0);

        float gridSize = 400;
        sketch.text("S", 0, -gridSize - 30);
        sketch.text("N", 0, gridSize + 30);
        sketch.text("E", -gridSize - 30, 0);
        sketch.text("W", gridSize + 30, 0);
    }

    private void drawReceiverTube() {
        sketch.pushMatrix();
        // Referans noktasını receiver'ın alt noktası yerine orta noktası yapalım
        float height = state.getReceiverHeight();
        sketch.translate(0, 0, height);
        sketch.shape(receiverTube);
        sketch.popMatrix();
    }

    private void drawReflectors(List<MirrorPosition> positions, SolarPosition sunPos) {
        for (MirrorPosition pos : positions) {
            sketch.pushMatrix();
            // Ana pozisyonlama - aynaları yukarı kaldır
            sketch.translate((float) pos.getXOffset(), 0, SUPPORT_HEIGHT + 2);

            // Tabureyi (support) çiz - ama tabure için aşağı in
            sketch.pushMatrix();
            sketch.translate(0, 0, -SUPPORT_HEIGHT);  // Tabure için aşağı git
            drawSupport();
            sketch.popMatrix();

            // Aynayı çiz
            sketch.pushMatrix();
            sketch.rotateY(sketch.radians((float) pos.getRotationAngle()));

            // Normal vektör
            sketch.stroke(255, 0, 0);
            sketch.strokeWeight(2);
            float dashLength = 5;
            for (float j = 0; j < 100; j += dashLength * 2) {
                sketch.line(0, 0, j, 0, 0, j + dashLength);
            }

            // Aynayı çiz
            sketch.shape(reflectors[pos.getMirrorIndex()]);
            sketch.popMatrix();

            // Açı etiketini çiz (ayna dönüşünden bağımsız)
            drawMirrorAngleLabel(pos.getRotationAngle());

            sketch.popMatrix();
        }
    }

    private void drawMirrorAngleLabel(double angle) {
        sketch.pushMatrix();
        sketch.translate(0, REFLECTOR_LENGTH / 2 + 1, -15);
        sketch.rotateX(3 * sketch.HALF_PI);  // Sadece X ekseni etrafında döndür

        // Beyaz arka plan
        sketch.pushMatrix();
        sketch.translate(0, 0, 1);
        sketch.fill(255);
        sketch.noStroke();
        sketch.rectMode(PApplet.CENTER);
        sketch.rect(0, 0, 50, 20);
        sketch.popMatrix();

        // Açı değeri
        sketch.pushMatrix();
        sketch.translate(0, 0, 2);
        sketch.fill(0);
        sketch.textAlign(PApplet.CENTER, PApplet.CENTER);
        sketch.textSize(11);
        sketch.text(String.format("%.1f°", angle), 0, 0);
        sketch.popMatrix();
        sketch.popMatrix();
    }

    private void drawSupport() {
        // Support frame
        sketch.pushMatrix();
        sketch.fill(50);
        // Ana gövde
        sketch.translate(0, 0, SUPPORT_HEIGHT / 2);
        sketch.box(REFLECTOR_WIDTH * 0.9f, REFLECTOR_LENGTH, 5);

        // Ayaklar
        sketch.translate(0, 0, -SUPPORT_HEIGHT / 2);
        float legSpacing = REFLECTOR_WIDTH * 0.4f;
        for (float xPos : new float[]{-legSpacing, legSpacing}) {
            for (float yPos : new float[]{-REFLECTOR_LENGTH * 0.4f, REFLECTOR_LENGTH * 0.4f}) {
                sketch.pushMatrix();
                sketch.translate(xPos, yPos, 0);
                sketch.box(5, 5, SUPPORT_HEIGHT);
                sketch.popMatrix();
            }
        }
        sketch.popMatrix();
    }

    private void drawSunAndRays(SolarPosition sunPos) {
        float sunDist = 1000;
        float azimuth = sketch.radians((float) sunPos.getAzimuthAngle());
        float altitude = sketch.radians((float) sunPos.getAltitudeAngle());

        float sunX = -sunDist * sketch.cos(altitude) * sketch.sin(azimuth);
        float sunY = sunDist * sketch.cos(altitude) * sketch.cos(azimuth);
        float sunZ = sunDist * sketch.sin(altitude);

        // Draw sun
        sketch.pushMatrix();
        sketch.translate(sunX, sunY, sunZ);
        sketch.fill(255, 255, 0);
        sketch.noStroke();
        sketch.sphere(30);
        sketch.popMatrix();

        // Draw rays
        sketch.stroke(255, 255, 0, 100);
        sketch.strokeWeight(2);
        for (MirrorPosition pos : state.getMirrorPositions()) {
            float mirrorX = (float) pos.getXOffset();
            // Incident ray
            sketch.line(mirrorX, 0, SUPPORT_HEIGHT + 2, sunX, sunY, sunZ);
            // Reflected ray
            sketch.line(mirrorX, 0, SUPPORT_HEIGHT + 2, 0, 0, RECEIVER_HEIGHT);
        }
    }

    @Override
    public void setupCamera() {
        // Camera için gerekli view ve transform ayarları
        sketch.perspective(sketch.PI / 3.0f,
                (float) sketch.width / sketch.height,
                1,
                10000);

        // Camera pozisyonu için gerekli matrix transformları
        sketch.camera(0, 0, RECEIVER_HEIGHT * 2, // Camera position
                0, 0, RECEIVER_HEIGHT / 2, // Look at point
                0, 0, -1);                    // Up vector
    }

    @Override
    public void dispose() {
        // PShape nesneleri Processing tarafından yönetildiği için
        // explicit dispose gerekmiyor. Garbage collector temizleyecektir.
        receiverTube = null;
        if (reflectors != null) {
            for (int i = 0; i < reflectors.length; i++) {
                reflectors[i] = null;
            }
        }
        reflectors = null;
    }
}
