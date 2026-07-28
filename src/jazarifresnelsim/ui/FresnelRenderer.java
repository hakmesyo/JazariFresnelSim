package jazarifresnelsim.ui;

import jazarifresnelsim.models.SimulationState;
import jazarifresnelsim.models.MirrorPosition;
import jazarifresnelsim.models.SolarPosition;
import java.util.List;
import processing.core.*;
import static jazarifresnelsim.domain.Constants.*;

/**
 * 3D renderer for LFR mirror field visualization.
 *
 * VERSION 4.0 — All deprecated Constants aliases replaced.
 * Geometry values are read from SimulationState, not from Constants.
 *
 * @author Yunus Demirtas, Musa Atas — Siirt University
 * @version 4.0
 */
public class FresnelRenderer implements IRenderer {

    private final PApplet sketch;
    private final SimulationState state;
    private PShape receiverTube;
    private PShape[] reflectors;

    public FresnelRenderer(PApplet sketch, SimulationState state) {
        this.sketch = sketch;
        this.state  = state;
        initializeModels();
    }

    private void initializeModels() {
        float recDiam   = state.getReceiverDiameter();
        float recHeight = state.getReceiverHeight();
        float mirrorLen = state.getReflectorLength();
        float mirrorWid = state.getReflectorWidth();
        int   numMirrors = state.getNumReflectors();

        // --- Receiver tube ---
        receiverTube = sketch.createShape(PApplet.GROUP);
        float radius = recDiam / 2;
        int sides = 20;

        PShape tube = sketch.createShape();
        tube.beginShape(PApplet.TRIANGLE_STRIP);
        tube.fill(100, 50, 50);
        tube.noStroke();
        for (float angle = 0; angle <= PApplet.TWO_PI + 0.1; angle += PApplet.TWO_PI / sides) {
            float x = PApplet.cos(angle) * radius;
            float z = PApplet.sin(angle) * radius;
            tube.vertex(x, -mirrorLen / 2, z);
            tube.vertex(x,  mirrorLen / 2, z);
        }
        tube.endShape();
        receiverTube.addChild(tube);

        // Support structures
        PShape supports = sketch.createShape();
        supports.beginShape(PApplet.TRIANGLES);
        supports.fill(70);
        supports.noStroke();
        float supportWidth   = 15;
        float supportSpacing = mirrorLen * 0.8f;
        for (float pos : new float[]{-supportSpacing / 2, supportSpacing / 2}) {
            supports.vertex(-supportWidth / 2, pos, 0);
            supports.vertex( supportWidth / 2, pos, 0);
            supports.vertex(0, pos, -recHeight);
        }
        supports.endShape();
        receiverTube.addChild(supports);

        // --- Reflector mirrors ---
        reflectors = new PShape[numMirrors];
        for (int i = 0; i < numMirrors; i++) {
            reflectors[i] = createReflectorShape(mirrorWid, mirrorLen);
        }
    }

    private PShape createReflectorShape(float width, float length) {
        PShape reflector = sketch.createShape(PApplet.GROUP);

        // Front face (mirror surface)
        PShape frontFace = sketch.createShape();
        frontFace.beginShape(PApplet.QUADS);
        frontFace.fill(240, 240, 240);
        frontFace.ambient(200);
        frontFace.specular(255);
        frontFace.shininess(100);
        frontFace.stroke(200);
        frontFace.strokeWeight(1);
        frontFace.normal(0, 0, 1);
        frontFace.vertex(-width / 2, -length / 2, 0);
        frontFace.vertex( width / 2, -length / 2, 0);
        frontFace.vertex( width / 2,  length / 2, 0);
        frontFace.vertex(-width / 2,  length / 2, 0);
        frontFace.endShape();

        // Back face
        PShape backFace = sketch.createShape();
        backFace.beginShape(PApplet.QUADS);
        backFace.fill(100, 100, 100);
        backFace.stroke(150);
        backFace.strokeWeight(1);
        backFace.normal(0, 0, -1);
        backFace.vertex(-width / 2,  length / 2, -1);
        backFace.vertex( width / 2,  length / 2, -1);
        backFace.vertex( width / 2, -length / 2, -1);
        backFace.vertex(-width / 2, -length / 2, -1);
        backFace.endShape();

        reflector.addChild(frontFace);
        reflector.addChild(backFace);
        return reflector;
    }

    @Override
    public void render() {
        sketch.pushMatrix();

        SolarPosition sunPos = state.getCurrentSolarPosition();
        if (sunPos != null) {
            setupLighting(sunPos);
        }

        drawGrid();
        drawCompassLabels();
        drawReceiverTube();

        List<MirrorPosition> positions = state.getMirrorPositions();
        if (sunPos != null) {
            drawReflectors(positions, sunPos);
            drawSunAndRays(sunPos);
        }

        sketch.popMatrix();
    }

    private void setupLighting(SolarPosition sunPos) {
        sketch.lights();

        float azimuth  = sketch.radians((float) sunPos.getAzimuthAngle());
        float altitude = sketch.radians((float) sunPos.getAltitudeAngle());

        sketch.directionalLight(255, 255, 200,
                -sketch.cos(altitude) * sketch.sin(azimuth),
                -sketch.cos(altitude) * sketch.cos(azimuth),
                 sketch.sin(altitude));

        sketch.pointLight(100, 100, 100,
                0, 0, state.getReceiverHeight() * 2);

        sketch.ambientLight(120, 120, 120);
    }

    private void drawGrid() {
        sketch.stroke(100);
        sketch.strokeWeight(1);
        int gridSize = 400;
        int spacing  = 50;
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
        sketch.text("N", 0,  gridSize + 30);
        sketch.text("E", -gridSize - 30, 0);
        sketch.text("W",  gridSize + 30, 0);
    }

    private void drawReceiverTube() {
        sketch.pushMatrix();
        sketch.translate(0, 0, state.getReceiverHeight());
        sketch.shape(receiverTube);
        sketch.popMatrix();
    }

    private void drawReflectors(List<MirrorPosition> positions, SolarPosition sunPos) {
        float supportH  = state.getSupportHeight();
        float mirrorLen = state.getReflectorLength();
        float mirrorWid = state.getReflectorWidth();

        for (MirrorPosition pos : positions) {
            sketch.pushMatrix();
            sketch.translate((float) pos.getXOffset(), 0, supportH + 2);

            // Support structure
            sketch.pushMatrix();
            sketch.translate(0, 0, -supportH);
            drawSupport(mirrorWid, mirrorLen, supportH);
            sketch.popMatrix();

            // Mirror
            sketch.pushMatrix();
            sketch.rotateY(sketch.radians((float) pos.getRotationAngle()));

            // Normal vector dashes
            sketch.stroke(255, 0, 0);
            sketch.strokeWeight(2);
            float dashLength = 5;
            for (float j = 0; j < 100; j += dashLength * 2) {
                sketch.line(0, 0, j, 0, 0, j + dashLength);
            }

            sketch.shape(reflectors[pos.getMirrorIndex()]);
            sketch.popMatrix();

            // Angle label
            drawMirrorAngleLabel(pos.getRotationAngle(), mirrorLen);

            sketch.popMatrix();
        }
    }

    private void drawMirrorAngleLabel(double angle, float mirrorLength) {
        sketch.pushMatrix();
        sketch.translate(0, mirrorLength / 2 + 1, -15);
        sketch.rotateX(3 * sketch.HALF_PI);

        sketch.pushMatrix();
        sketch.translate(0, 0, 1);
        sketch.fill(255);
        sketch.noStroke();
        sketch.rectMode(PApplet.CENTER);
        sketch.rect(0, 0, 50, 20);
        sketch.popMatrix();

        sketch.pushMatrix();
        sketch.translate(0, 0, 2);
        sketch.fill(0);
        sketch.textAlign(PApplet.CENTER, PApplet.CENTER);
        sketch.textSize(11);
        sketch.text(String.format("%.1f°", angle), 0, 0);
        sketch.popMatrix();

        sketch.popMatrix();
    }

    private void drawSupport(float mirrorWidth, float mirrorLength, float supportHeight) {
        sketch.pushMatrix();
        sketch.fill(50);

        // Main body
        sketch.translate(0, 0, supportHeight / 2);
        sketch.box(mirrorWidth * 0.9f, mirrorLength, 5);

        // Legs
        sketch.translate(0, 0, -supportHeight / 2);
        float legSpacing = mirrorWidth * 0.4f;
        for (float xPos : new float[]{-legSpacing, legSpacing}) {
            for (float yPos : new float[]{-mirrorLength * 0.4f, mirrorLength * 0.4f}) {
                sketch.pushMatrix();
                sketch.translate(xPos, yPos, 0);
                sketch.box(5, 5, supportHeight);
                sketch.popMatrix();
            }
        }
        sketch.popMatrix();
    }

    private void drawSunAndRays(SolarPosition sunPos) {
        float sunDist  = 1000;
        float azimuth  = sketch.radians((float) sunPos.getAzimuthAngle());
        float altitude = sketch.radians((float) sunPos.getAltitudeAngle());

        float sunX = -sunDist * sketch.cos(altitude) * sketch.sin(azimuth);
        float sunY =  sunDist * sketch.cos(altitude) * sketch.cos(azimuth);
        float sunZ =  sunDist * sketch.sin(altitude);

        // Sun sphere
        sketch.pushMatrix();
        sketch.translate(sunX, sunY, sunZ);
        sketch.fill(255, 255, 0);
        sketch.noStroke();
        sketch.sphere(30);
        sketch.popMatrix();

        // Rays
        float supportH  = state.getSupportHeight();
        float recHeight  = state.getReceiverHeight();

        sketch.stroke(255, 255, 0, 100);
        sketch.strokeWeight(2);
        for (MirrorPosition pos : state.getMirrorPositions()) {
            float mirrorX = (float) pos.getXOffset();
            // Incident ray
            sketch.line(mirrorX, 0, supportH + 2, sunX, sunY, sunZ);
            // Reflected ray → receiver
            sketch.line(mirrorX, 0, supportH + 2, 0, 0, recHeight);
        }
    }

    @Override
    public void setupCamera() {
        float recHeight = state.getReceiverHeight();
        sketch.perspective(sketch.PI / 3.0f,
                (float) sketch.width / sketch.height, 1, 10000);
        sketch.camera(0, 0, recHeight * 2,
                      0, 0, recHeight / 2,
                      0, 0, -1);
    }

    @Override
    public void dispose() {
        receiverTube = null;
        if (reflectors != null) {
            for (int i = 0; i < reflectors.length; i++) {
                reflectors[i] = null;
            }
        }
        reflectors = null;
    }
}