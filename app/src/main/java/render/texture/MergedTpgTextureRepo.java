package render.texture;

import org.lwjglb.engine.graph.Texture;
import tpgviewer.tpg.TpgImage;
import tpgviewer.tpg.TpgImageFactory;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

@Deprecated
public class MergedTpgTextureRepo {

    private static final int NUM_MERGED_TPG_FILES = 32;
    private static final boolean throwBecauseDeprecated = true;

    private Texture[] textures = new Texture[NUM_MERGED_TPG_FILES];

    public static final MergedTpgTextureRepo REPO = new MergedTpgTextureRepo();

    private MergedTpgTextureRepo() {
        if (throwBecauseDeprecated) {
            throw new UnsupportedOperationException("use TextureAtlas");
        }
        for (int i = 0; i < NUM_MERGED_TPG_FILES; i++) {
            initMergedTpgTexture(i);
        }
    }

    private void initMergedTpgTexture(int idx) {
        String filePath = String.format("/home/wasd/Downloads/Mall Maniacs/scene_ica/MERGED%02d.TPG", idx);
        TpgImage tpgImage = TpgImageFactory.fromFile(filePath);
        ByteBuffer pixels = tpgImage.toByteBuffer();

        int textureHandle = glGenTextures();
        textures[idx] = new Texture(textureHandle);

        glBindTexture(GL_TEXTURE_2D, textureHandle);

        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, TpgImage.SIZE, TpgImage.SIZE, 0,
                     GL_RGB, GL_UNSIGNED_BYTE, pixels);
    }

    public Texture textureForMergedId(int mergedId) {
        return textures[mergedId];
    }

}
