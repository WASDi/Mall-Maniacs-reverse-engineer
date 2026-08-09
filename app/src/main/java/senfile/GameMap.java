package senfile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum GameMap {
    ICA("scene_ica", "MALL1_ICA.SEN"),
    WOOD("scene_wood", "WOODMALL.SEN"),
    ORIENT("scene_orient", "ORIENTMALL.SEN"),
    AQUA("scene_aqua", "AQUAMALL.SEN"),
    FUTURE("scene_future", "FUTUREMALL.SEN");

    public static final GameMap SELECTED_MAP = AQUA;
    public static final boolean RENDER_CHARACTERS = false;

    public final String senFileDirectory;
    public final String senFilePath;

    GameMap(String subFolder, String senFile) {
        this.senFileDirectory = Util.ROOT_DIR + subFolder + "/";
        this.senFilePath = this.senFileDirectory + (RENDER_CHARACTERS ? "CHARACTERS.SEN" : senFile);
    }

    public List<String> listSenFilePaths() {
        List<String> paths = new ArrayList<>();
        File dir = new File(senFileDirectory);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".sen")) {
                    paths.add(file.getAbsolutePath());
                }
            }
        }
        Collections.sort(paths);
        return paths;
    }
}
