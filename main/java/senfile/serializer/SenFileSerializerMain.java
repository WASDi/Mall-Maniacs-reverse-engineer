package senfile.serializer;

import senfile.GameMap;

public class SenFileSerializerMain {

    public static void main(String[] args) {
        String filePath = GameMap.ICA.senFilePath;
        String phFilePath = "/home/wasd/Downloads/Mall Maniacs/scene_ica/PHMALL1_ICA.SEN";

        serializeFromOriginal(filePath);
        serializeFromOriginal(phFilePath);
    }

    private static void serializeFromOriginal(String filePath) {
        new SenFileSerializer(filePath + "_ORIGINAL")
                .serialize(filePath);
    }
}
