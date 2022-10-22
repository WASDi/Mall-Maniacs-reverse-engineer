package senfile.serializer;

import senfile.GameMap;
import senfile.Util;

public class SenFileSerializerMain {

    public static void main(String[] args) {
        long startTime = System.nanoTime();

        String filePath = GameMap.ICA.senFilePath;
        String phFilePath = Util.ROOT_DIR + "scene_ica/PHMALL1_ICA.SEN";

        serializeFromOriginal(filePath);
//        serializeFromOriginal(phFilePath);

        long endTime = System.nanoTime();
        System.out.printf("\nCompleted in %.0fms\n", (endTime - startTime) / 1e6);
    }

    private static void serializeFromOriginal(String filePath) {
        new SenFileSerializer(filePath + "_ORIGINAL")
                .serialize(filePath);
    }
}
