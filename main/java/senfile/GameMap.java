package senfile;

public enum GameMap {
    ICA("scene_ica", "MALL1_ICA.SEN", 32, "MERGED"),
    WOOD("scene_wood", "WOODMALL.SEN", 33, "MERGED"),
    ORIENT("scene_orient", "ORIENTMALL.SEN", 24, "ORIENT"),
    AQUA("scene_aqua", "AQUAMALL.SEN", 21, "AQUA"),
    FUTURE("scene_future", "FUTUREMALL.SEN", 23, "FUT");

    private static final String ROOT_DIRECTORY = "/home/wasd/Downloads/Mall Maniacs/";
    public static final GameMap SELECTED_MAP = WOOD;

    public final String senFilePath;
    public final String tpgFilesFormat;
    public final int numTpgFiles;

    GameMap(String subFolder, String senFile, int numTpgFiles, String tpgFilesPrefix) {

        String mapFolder = ROOT_DIRECTORY + subFolder + "/";
        senFilePath = mapFolder + senFile;
        tpgFilesFormat = mapFolder + tpgFilesPrefix + "%02d.TPG";

        this.numTpgFiles = numTpgFiles;
    }
}
