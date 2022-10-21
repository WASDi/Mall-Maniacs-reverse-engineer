package senfile.factories;

import senfile.Util;
import senfile.parts.Onam;

import java.nio.ByteBuffer;
import java.util.List;

public class OnamFactory {

    public static Onam parseFromBufferPosition(ByteBuffer buffer) {
        int bytesLeft = buffer.getInt();
        List<String> names = Util.readNulSeparatedNames(buffer, bytesLeft);
        return new Onam(names, bytesLeft);
    }

}
