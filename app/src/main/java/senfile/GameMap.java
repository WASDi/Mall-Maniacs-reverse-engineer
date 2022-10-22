package senfile;

public enum GameMap {
    ICA("scene_ica", "MALL1_ICA.SEN"),
    WOOD("scene_wood", "WOODMALL.SEN"),
    ORIENT("scene_orient", "ORIENTMALL.SEN"),
    AQUA("scene_aqua", "AQUAMALL.SEN"),
    FUTURE("scene_future", "FUTUREMALL.SEN");

    public static final GameMap SELECTED_MAP = AQUA;

    public final String senFileDirectory;
    public final String senFilePath;

    GameMap(String subFolder, String senFile) {
        this.senFileDirectory = Util.ROOT_DIR + subFolder + "/";
        this.senFilePath = this.senFileDirectory + senFile;
    }
}
