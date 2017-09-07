package senfile.factories;

import senfile.Util;
import senfile.parts.Cols;

import java.nio.ByteBuffer;

public class ColsFactory {

    public static Cols parseFromBufferPosition(ByteBuffer buffer) {
        int bytesLeft = buffer.getInt();
        Util.skip(buffer, bytesLeft);
        return new Cols();
    }

}
