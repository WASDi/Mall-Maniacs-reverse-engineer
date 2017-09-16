package senfile;

public enum GameMap {
    ICA("scene_ica", "MALL1_ICA.SEN", 32),
    WOOD("scene_wood", "WOODMALL.SEN", 33),
    ORIENT("scene_orient", "ORIENTMALL.SEN", 36),
    AQUA("scene_aqua", "AQUAMALL.SEN", 33),
    FUTURE("scene_future", "FUTUREMALL.SEN", 35);

    private static final String ROOT_DIRECTORY = "/home/wasd/Downloads/Mall Maniacs/";
    public static final GameMap SELECTED_MAP = ICA;

    public final String senFilePath;
    public final String mergedTpgFilesFormat;
    public final int numMergedTpgFiles;

    GameMap(String subFolder, String senFile, int numMergedTpgFiles) {

        String mapFolder = ROOT_DIRECTORY + subFolder + "/";
        senFilePath = mapFolder + senFile;
        mergedTpgFilesFormat = mapFolder + "MERGED%02d.TPG";

        this.numMergedTpgFiles = numMergedTpgFiles;
    }
}
