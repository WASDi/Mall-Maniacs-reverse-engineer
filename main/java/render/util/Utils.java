package render.util;

import org.lwjgl.BufferUtils;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.List;

public class Utils {

    public static float[] floatListToArray(List<Float> list) {
        float[] array = new float[list.size()];
        int idx = 0;
        for (float f : list) {
            array[idx++] = f;
        }
        return array;
    }

    public static int[] intListToArray(List<Integer> list) {
        int[] array = new int[list.size()];
        int idx = 0;
        for (int i : list) {
            array[idx++] = i;
        }
        return array;
    }

    public static ByteBuffer toByteBuffer(BufferedImage image) {
        if (image.getType() != BufferedImage.TYPE_INT_RGB) {
            throw new IllegalArgumentException("Must be BufferedImage.TYPE_INT_RGB");
        }
        int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

        ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[y * image.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                buffer.put((byte) (pixel & 0xFF));
                buffer.put((byte) 255);
            }
        }
        buffer.flip();

        return buffer;
    }
}
