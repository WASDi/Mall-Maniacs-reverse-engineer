package senfile.factories;

import senfile.Util;
import senfile.parts.Tnam;

import java.nio.ByteBuffer;
import java.util.List;

public class TnamFactory {

    public static Tnam parseFromBufferPosition(ByteBuffer buffer) {
        int bytesLeft = buffer.getInt();
        List<String> names = Util.readNulSeparatedNames(buffer, bytesLeft);
        return new Tnam(names, bytesLeft);
    }

}
