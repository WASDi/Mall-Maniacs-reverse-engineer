package senfile.serializer;

import senfile.GameMap;

public class SenFileSerializerMain {

    public static void main(String[] args) {
        String filePath = GameMap.FUTURE.senFilePath;
        SenFileSerializer serializer = new SenFileSerializer(filePath + "_ORIGINAL");
        serializer.serialize(filePath);
    }
}
