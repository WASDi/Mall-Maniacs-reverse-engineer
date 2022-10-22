package senfile;

import senfile.factories.SenFileFactory;
import senfile.parts.elements.MapiElement;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class MapiBoundingMaskDumper {

    private static final Random r = new Random();

    public static void main(String[] args) throws IOException {
        String filePath = Util.ROOT_DIR + "scene_aqua/OBJECTS.SEN";
//        for (int i = 0; i < 32; i++) {
//            dumpMapi(filePath, i);
//        }
        dumpMapi(filePath, 0);
    }

    private static void dumpMapi(String filePath, int tpgFileIndex) throws IOException {
        SenFile senFile = SenFileFactory.fromFile(filePath);

        BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        int numElementsDumped = 0;
        for (MapiElement element : senFile.mapi.elements) {
            if (element.tpgFileIndex == tpgFileIndex) {

                if(numElementsDumped == 5) {
                    break;
                }
                numElementsDumped++;

                g2d.setColor(randomColor());
                byte[] posBytes = element.textureCoordBytes;
                for (int i = 0; i < posBytes.length; i += 2) {
                    int x1 = posBytes[i] & 0xFF;
                    int y1 = posBytes[i + 1] & 0xFF;

                    int x2 = posBytes[(i + 2) % posBytes.length] & 0xFF;
                    int y2 = posBytes[(i + 3) % posBytes.length] & 0xFF;

                    g2d.drawLine(x1, y1, x2, y2);
                    System.out.printf("%d %d %d %d\n", x1, y1, x2, y2);
                }
                System.out.println();
            }
        }

        File outputFile = new File("/tmp/mapi/dump_" + tpgFileIndex + ".png");
        ImageIO.write(img, "png", outputFile);

        System.out.println("Exported " + numElementsDumped + " elements to " + outputFile.getAbsolutePath());
    }

    private static Color randomColor() {
        return new Color(128 + r.nextInt(128),
                         128 + r.nextInt(128),
                         128 + r.nextInt(128),
                         128);
    }

}
