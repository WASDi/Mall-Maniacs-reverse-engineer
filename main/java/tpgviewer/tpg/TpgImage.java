package tpgviewer.tpg;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class TpgImage {

    public static final int SIZE = 256;
    public static final int BUFFERED_IMAGE_TYPE = BufferedImage.TYPE_INT_RGB;
    private static final int PALETTE_START = 256 * 256;

    private final byte[] bytes;
    private BufferedImage cachedImage = null;

    public TpgImage(byte[] bytes) {
        this.bytes = bytes;
    }

    public BufferedImage renderLazy() {
        if (cachedImage == null) {
            cachedImage = new BufferedImage(SIZE, 256, BUFFERED_IMAGE_TYPE);
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    int rgb = getRgb(x, y);
                    cachedImage.setRGB(x, y, rgb);
                }
            }
        }
        return cachedImage;
    }

    private int getRgb(int x, int y) {
        int paletteColor = bytes[x + y * SIZE] & 0xFF;
        int r = bytes[PALETTE_START + 4 * paletteColor];
        int g = bytes[PALETTE_START + 4 * paletteColor + 1];
        int b = bytes[PALETTE_START + 4 * paletteColor + 2];
//        int a = bytes[PALETTE_START + 4 * paletteColor + 3]; // alltid 0xCD ?
        return (r << 16) + (g << 8) + b;
    }

    public ByteBuffer toByteBuffer() {
        ByteBuffer buf = ByteBuffer.allocateDirect(SIZE * SIZE * 3);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int paletteColor = bytes[x + y * SIZE] & 0xFF;
                buf.put(bytes, PALETTE_START + 4 * paletteColor, 3);
            }
        }
        buf.flip();
        return buf;
    }
}
