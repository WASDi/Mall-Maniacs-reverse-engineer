package render.texture;

import org.lwjglb.engine.graph.Texture;
import render.util.Utils;
import senfile.GameMap;
import senfile.factories.SenFileFactory;
import senfile.parts.Tnam;
import tpgviewer.tpg.TpgImage;
import tpgviewer.tpg.TpgImageFactory;

import javax.imageio.ImageIO;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

public class TextureAtlas {

    public static final int SIZE_X = 6;
    public static final int SIZE_Y = 6;

    private final BufferedImage atlas;

    private TextureAtlas(BufferedImage atlas) {
        this.atlas = atlas;
    }

    public static TextureAtlas atlasFromTnam(Tnam tnam) {
        if (tnam.names.size() > SIZE_X * SIZE_Y) {
            throw new IllegalArgumentException("Atlas too small, " + SIZE_X + "x" + SIZE_Y);
        }
        BufferedImage atlas = new BufferedImage(SIZE_X * TpgImage.SIZE,
                                                SIZE_Y * TpgImage.SIZE,
                                                TpgImage.BUFFERED_IMAGE_TYPE);
        Graphics atlasGraphics = atlas.getGraphics();

        int idx = 0;
        for (String textureFile : tnam.names) {
            String filePath = GameMap.SELECTED_MAP.senFileDirectory + textureFile + ".TPG";
            TpgImage tpgImage = TpgImageFactory.fromFile(filePath);
            BufferedImage subImage = tpgImage.renderLazy();

            int x = TpgImage.SIZE * (idx % SIZE_Y);
            int y = TpgImage.SIZE * (idx / SIZE_X);
            atlasGraphics.drawImage(subImage, x, y, null);
            idx++;
        }
        return new TextureAtlas(atlas);
    }

    public Texture toLwjglTexture() {
        ByteBuffer pixels = Utils.toByteBuffer(atlas);

        int textureHandle = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, textureHandle);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, atlas.getWidth(), atlas.getHeight(), 0,
                     GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        return new Texture(textureHandle);
    }


    public static void main(String[] args) throws Exception {

        BufferedImage atlas = atlasFromTnam(SenFileFactory.getMap(GameMap.SELECTED_MAP).tnam).atlas;

        File outputFile = new File("/tmp/atlas.png");
        ImageIO.write(atlas, "png", outputFile);

        System.out.println("Exported to " + outputFile.getAbsolutePath());

    }

    public static float xOffsetFromIndex(int index) {
        int gridX = index % SIZE_X;
        return gridX * 1f / SIZE_X;
    }

    public static float yOffsetFromIndex(int index) {
        int gridY = index / SIZE_X;
        return gridY * 1f / SIZE_Y;
    }
}
