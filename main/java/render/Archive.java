package render;

import de.matthiasmann.twl.utils.PNGDecoder;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

public class Archive {

    private void initMySpriteTexture() {
        PNGDecoder decoder;
        try {
            decoder = new PNGDecoder(new FileInputStream("/home/wasd/IdeaProjects/MallManiacsHax/assets/merged00_aqua.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ByteBuffer buf = ByteBuffer.allocateDirect(
                3 * decoder.getWidth() * decoder.getHeight());
        try {
            decoder.decode(buf, decoder.getWidth() * 3, PNGDecoder.Format.RGB);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        buf.flip();

        // ... //

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, decoder.getWidth(), decoder.getHeight(), 0,
                     GL_RGB, GL_UNSIGNED_BYTE, buf);
    }

}
