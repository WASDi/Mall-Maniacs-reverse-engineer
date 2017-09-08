package render;

import tpgviewer.tpg.TpgImage;
import tpgviewer.tpg.TpgImageFactory;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

public class MergedTpgTextureRepo {

    private static final int NUM_MERGED_TPG_FILES = 32;
    private int[] texture_MERGED = new int[NUM_MERGED_TPG_FILES];

    public static final MergedTpgTextureRepo REPO = new MergedTpgTextureRepo();

    public MergedTpgTextureRepo() {
        for (int i = 0; i < NUM_MERGED_TPG_FILES; i++) {
            initMergedTpgTexture(i);
        }
    }

    private void initMergedTpgTexture(int idx) {
        String filePath = String.format("/home/wasd/Downloads/Mall Maniacs/scene_ica/MERGED%02d.TPG", idx);
        TpgImage tpgImage = TpgImageFactory.fromFile(filePath);
        ByteBuffer pixels = tpgImage.toByteBuffer();

        texture_MERGED[idx] = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, texture_MERGED[idx]);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, TpgImage.SIZE, TpgImage.SIZE, 0,
                     GL_RGB, GL_UNSIGNED_BYTE, pixels);
    }

    public int textureGlIdForMergedId(int mergedId) {
        return texture_MERGED[mergedId];
    }

}
