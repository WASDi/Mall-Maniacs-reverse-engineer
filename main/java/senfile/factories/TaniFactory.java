package senfile.factories;

import senfile.Util;
import senfile.parts.Cols;
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
        Util.skip(buffer, bytesLeft);
        return new Tani();
    }

}
