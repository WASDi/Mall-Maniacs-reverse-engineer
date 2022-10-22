package tpgviewer;

import senfile.Util;
import tpgviewer.tpg.TpgImage;
import tpgviewer.tpg.TpgImageFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TpgFileExporter {

    public static void main(String[] args) throws IOException {
        String inputFile = Util.ROOT_DIR + "scene_aqua/MERGED00.TPG";

        TpgImage tpgImage = TpgImageFactory.fromFile(inputFile);
        BufferedImage bufferedImage = tpgImage.renderLazy();

        File outputFile = new File("/tmp/MERGED_export.png");
        ImageIO.write(bufferedImage, "png", outputFile);

        System.out.println("Exported to " + outputFile.getAbsolutePath());
    }

}
