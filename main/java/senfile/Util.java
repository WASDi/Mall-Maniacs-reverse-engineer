package senfile;

import senfile.parts.elements.SuboElement;
import senfile.parts.mesh.SenMesh;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Util {

    private static final boolean IGNORE_UNDERLINES = true;

    public static boolean ignoreBecauseUnderline(String meshName) {
        return Util.IGNORE_UNDERLINES && meshName.charAt(0) == '_';
    }

    public static int getIndexOfNul(byte[] nameBytes) {
        for (int i = 0; i < nameBytes.length; i++) {
            if (nameBytes[i] == 0) {
                return i;
            }
        }
        return nameBytes.length;
    }

    public static void skip(ByteBuffer buffer, int length) {
        buffer.position(buffer.position() + length);
    }

    public static List<String> pathToAllSenFilesInDirectory(String pathToDirectory) {
        File dir = new File(pathToDirectory);
        return pathToAllSenFilesInDirectory(dir);
    }

    public static List<String> pathToAllSenFilesInDirectory(File dir) {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(dir + " is not a directory");
        }

        List<String> pathsToSenFiles = new ArrayList<>();
        File[] filesInDirectory = dir.listFiles();
        for (File file : filesInDirectory) {
            if (file.isDirectory()) {
                pathsToSenFiles.addAll(pathToAllSenFilesInDirectory(file));
            } else if (file.getName().toLowerCase().endsWith(".sen")) {
                pathsToSenFiles.add(file.getAbsolutePath());
            }
        }
        return pathsToSenFiles;
    }

    public static List<String> readNulSeparatedNames(ByteBuffer buffer, int length) {
        int end = buffer.position() + length;
        List<String> names = new ArrayList<>();

        StringBuilder sb = new StringBuilder();
        while (buffer.position() < end) {
            char c = (char) buffer.get();
            if (c == 0) {
                if (sb.length() != 0) {
                    names.add(sb.toString());
                    sb.setLength(0);
                }
            } else {
                sb.append(c);
            }
        }

        return names;
    }


    public static boolean hasTransparency(SenMesh mesh, SenFile senFile) {
        for (int suboOffset : mesh.getSuboOffsets()) {
            SuboElement subo = senFile.subo.elementByOffset(suboOffset);
            if (subo.transparency != 0) {
//                System.out.println(mesh.name + ": " + subo.transparency);
            }
            if (subo.transparency == 64) {
                return true;
            }
        }
        return false;
    }

    public static int getTransparency(SenMesh mesh, SenFile senFile) {
        for (int suboOffset : mesh.getSuboOffsets()) {
            SuboElement subo = senFile.subo.elementByOffset(suboOffset);
            if (subo.transparency != 0) {
                return subo.transparency;
            }
        }
        return -1000;
    }


}
