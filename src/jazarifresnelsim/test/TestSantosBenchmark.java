package jazarifresnelsim.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import jazarifresnelsim.domain.MirrorTracker;
import jazarifresnelsim.domain.ShadingDetector;
import jazarifresnelsim.domain.SolarCalculator;
import jazarifresnelsim.models.MirrorPosition;
import jazarifresnelsim.models.SimulationState;
import jazarifresnelsim.models.SolarPosition;

/**
 * Santos et al. (2021) validasyon verisi ile karşılaştırma testi. Düzeltmeler:
 * Spillage loss = 1.0 (Parabolik ayna varsayımı için)
 */
public class TestSantosBenchmark {

    public static void main(String[] args) {
        runSantos2021Validation();
    }

    public static void runSantos2021Validation() {
        SimulationState state = new SimulationState();

        // 1. Santos 2021 (Solar Energy 227) Geometrisi
        state.setNumReflectors(16);
        state.setReflectorWidth(75.0f);
        state.setReflectorSpacing(105.0f);
        state.setReceiverHeight(720.0f);
        state.setReceiverDiameter(34.1f);
        state.setReflectorLength(3000.0f);
        state.setSupportHeight(30.0f);

        // Evora, Portekiz koordinatları
        SolarCalculator calc = new SolarCalculator(38.6, -7.9, 0);

        System.out.println("Transversal_Angle(Theta_T), JazariFresnelSim_Gamma_T");
        MirrorTracker tracker = new MirrorTracker();
        ShadingDetector shadingDetector = new ShadingDetector();

        // 0'dan 90 dereceye kadar 5'er derece artarak güneşi hareket ettir
        for (int thetaT = 0; thetaT <= 90; thetaT += 5) {

            double altitude = 90.0 - thetaT;
            double azimuth = 90.0; // Güneş tam doğuda

            SolarPosition sunPos = new SolarPosition(altitude, azimuth, 1000.0);
            state.setCurrentSolarPosition(sunPos);

            // Ayna pozisyonlarını ve açılarını hesapla
            List<MirrorPosition> positions = new ArrayList<>();
            float spacing = state.getReflectorSpacing();
            int numReflectors = state.getNumReflectors();

            for (int i = 0; i < numReflectors; i++) {
                double offset = (i < numReflectors / 2) ? -(i + 0.5) : (i - numReflectors / 2 + 0.5);
                double xOffset = offset * spacing;
                double rotationAngle = tracker.calculateOptimalMirrorAngle(xOffset / 100.0, sunPos, state);
                positions.add(new MirrorPosition(rotationAngle, xOffset, state.getSupportHeight() + 2, i));
            }
            state.updateMirrorPositions(positions);

            // Alan ağırlıklı ortalama Gamma_T (Intercept Factor) hesabı
            double totalGamma = 0.0;
            for (MirrorPosition mirror : positions) {
                // Cosine Efficiency (Fiziksel doğruluk)
                double cosEff = calc.calculateCosineEfficiency(mirror, sunPos);

                // VALIDASYON DÜZELTMESİ:
                // Santos çalışması parabolik aynalarla odaklama yaptığı için, 
                // bizim düz ayna (flat mirror) odaklama kaybımızı (spill) 
                // validasyon boyunca yok sayıyoruz (1.0).
                double spillEff = 1.0;

                // Block/Shading Efficiency (SolarCalculator'daki yeni yön kontrollü metod çalışmalı!)
                double blockEff = shadingDetector.calculateBlockingAndShadingLoss(mirror, positions, state, sunPos);

                // Saf Geometrik Verimlilik (Intercept Factor)
                double mirrorGamma = cosEff * spillEff * blockEff;
                totalGamma += mirrorGamma;
            }

            double avgGamma = totalGamma / numReflectors;
            System.out.println(thetaT + ", " + String.format(Locale.US, "%.4f", avgGamma));
        }
    }
}
