package render.texture;

import org.lwjglb.engine.graph.Texture;
import render.util.Utils;
import senfile.GameMap;
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

    private static Texture singletonTexture;

    private TextureAtlas(BufferedImage atlas) {
        this.atlas = atlas;
    }

    private static TextureAtlas renderAtlas() {
        BufferedImage atlas = new BufferedImage(SIZE_X * TpgImage.SIZE,
                                                SIZE_Y * TpgImage.SIZE,
                                                TpgImage.BUFFERED_IMAGE_TYPE);
        Graphics atlasGraphics = atlas.getGraphics();

        for (int idx = 0; idx < GameMap.SELECTED_MAP.numTpgFiles; idx++) {
            String filePath = String.format(GameMap.SELECTED_MAP.tpgFilesFormat, idx);
            TpgImage tpgImage = TpgImageFactory.fromFile(filePath);
            BufferedImage subImage = tpgImage.renderLazy();

            int x = TpgImage.SIZE * (idx % SIZE_Y);
            int y = TpgImage.SIZE * (idx / SIZE_X);
            atlasGraphics.drawImage(subImage, x, y, null);
        }

        return new TextureAtlas(atlas);
    }

    public static Texture getSingletonTexture() {
        if (singletonTexture == null) {
            singletonTexture = renderAtlas().toLwjglTexture();
        }
        return singletonTexture;
    }

    public Texture toLwjglTexture() {
        ByteBuffer pixels = Utils.toByteBuffer(atlas);

        int textureHandle = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, textureHandle);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, atlas.getWidth(), atlas.getHeight(), 0,
                     GL_RGB, GL_UNSIGNED_BYTE, pixels);

        return new Texture(textureHandle);
    }


    public static void main(String[] args) throws Exception {

        BufferedImage atlas = renderAtlas().atlas;

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
