package tpgviewer.tpg;

import tpgviewer.util.BytesFromFileGetter;

public class TpgImageFactory {

    private TpgImageFactory() {
    }

    public static TpgImage fromFile(String filePath) {
        byte[] bytes = BytesFromFileGetter.getBytes(filePath);
        return new TpgImage(bytes);
    }

}
