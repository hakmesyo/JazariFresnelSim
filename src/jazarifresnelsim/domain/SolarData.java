package jazarifresnelsim.domain;
import java.util.HashMap;
import java.util.Map;

public class SolarData {
    public static Map<String, Map<String, double[]>> getAllLocationData() {
        Map<String, Map<String, double[]>> allData = new HashMap<>();

        // DIYARBAKIR
        Map<String, double[]> diyarbakir = new HashMap<>();
        diyarbakir.put("0321", new double[]{68.3, 53.2, 551.9, 604.6, 765.1, 782.2, 777.6, 518.8, 114.7, 0.0});
        diyarbakir.put("0621", new double[]{180.5, 480.2, 750.4, 880.1, 920.5, 940.2, 920.3, 880.5, 750.1, 480.8});
        diyarbakir.put("0921", new double[]{95.4, 340.2, 610.8, 810.5, 865.2, 880.4, 865.1, 810.2, 610.5, 340.1});
        diyarbakir.put("1221", new double[]{0.0, 45.2, 290.1, 460.5, 530.8, 550.2, 530.4, 460.1, 290.5, 45.3});
        allData.put("Diyarbakir", diyarbakir);

        // JEDDAH (Cidde)
        Map<String, double[]> jeddah = new HashMap<>();
        jeddah.put("0321", new double[]{450.2, 680.5, 820.1, 910.4, 960.8, 980.2, 960.5, 910.3, 820.1, 680.4});
        jeddah.put("0621", new double[]{520.4, 750.1, 890.6, 960.8, 1010.2, 1025.5, 1010.1, 960.4, 890.2, 750.6});
        jeddah.put("0921", new double[]{430.8, 650.2, 790.5, 880.1, 930.4, 950.8, 930.2, 880.5, 790.1, 650.3});
        jeddah.put("1221", new double[]{310.5, 520.1, 680.4, 780.2, 820.6, 840.1, 820.4, 780.3, 680.1, 520.4});
        allData.put("Jeddah", jeddah);

        // BERLIN
        Map<String, double[]> berlin = new HashMap<>();
        berlin.put("0321", new double[]{85.2, 210.4, 340.1, 420.5, 460.8, 480.2, 460.5, 420.3, 340.1, 210.4});
        berlin.put("0621", new double[]{280.4, 450.1, 580.6, 640.8, 680.2, 700.5, 680.1, 640.4, 580.2, 450.6});
        berlin.put("0921", new double[]{60.8, 180.2, 310.5, 390.1, 430.4, 450.8, 430.2, 390.5, 310.1, 180.3});
        berlin.put("1221", new double[]{0.0, 15.2, 95.4, 150.2, 180.6, 180.1, 150.4, 95.3, 15.1, 0.0});
        allData.put("Berlin", berlin);

        return allData;
    }
}