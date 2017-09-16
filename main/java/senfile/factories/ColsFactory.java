package senfile.factories;

import senfile.parts.Cols;

import java.nio.ByteBuffer;

public class ColsFactory {

    public static Cols parseFromBufferPosition(ByteBuffer buffer) {
        int bytesLeft = buffer.getInt();
        if (bytesLeft % 4 != 0) {
            throw new IllegalArgumentException("cols with " + bytesLeft + " bytes not dividable by 4, must change from int[] to byte[]");
        }

        int[] colInts = new int[bytesLeft / 4];

        for (int i = 0; i < colInts.length; i++) {
            colInts[i] = buffer.getInt();
        }

        return new Cols(bytesLeft, colInts);
    }

}
