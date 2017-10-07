package senfile;

public enum GameMap {
    ICA("scene_ica", "MALL1_ICA.SEN"),
    WOOD("scene_wood", "WOODMALL.SEN"),
    ORIENT("scene_orient", "ORIENTMALL.SEN"),
    AQUA("scene_aqua", "AQUAMALL.SEN"),
    FUTURE("scene_future", "FUTUREMALL.SEN");

    private static final String ROOT_DIRECTORY = "/home/wasd/Downloads/Mall Maniacs/";
    public static final GameMap SELECTED_MAP = AQUA;

    public final String senFileDirectory;
    public final String senFilePath;

    GameMap(String subFolder, String senFile) {
        this.senFileDirectory = ROOT_DIRECTORY + subFolder + "/";
        this.senFilePath = this.senFileDirectory + senFile;
    }
}
