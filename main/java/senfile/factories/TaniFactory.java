package senfile.factories;

import senfile.parts.Tani;

import java.nio.ByteBuffer;

public class TaniFactory {

    // Sizes
    // ORIENTMALL.SEN   60
    // FUTUREMALL.SEN   12
    // AQUAMALL.SEN    116

    // Animationer?

    public static Tani parseFromBufferPosition(ByteBuffer buffer) {
        int bytesLeft = buffer.getInt();

        if (bytesLeft % 4 != 0) {
            throw new IllegalArgumentException("tani with " + bytesLeft + " bytes not dividable by 4, must change from int[] to byte[]");
        }

        int[] taniInts = new int[bytesLeft / 4];

        for (int i = 0; i < taniInts.length; i++) {
            taniInts[i] = buffer.getInt();
        }

//        Util.skip(buffer, bytesLeft);
        return new Tani(taniInts, bytesLeft);
    }

}
