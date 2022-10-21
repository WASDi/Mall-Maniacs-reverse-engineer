package tpgviewer.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class BytesFromFileGetter {


    public static byte[] getBytes(String filePath) {
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
