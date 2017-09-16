package senfile.serializer;

import senfile.HeaderTexts;
import senfile.SenFile;

import java.nio.ByteBuffer;

public class OnamSerializer {
    public static void serializeOnam(SenFile senFile, ByteBuffer buffer) {
        buffer.putInt(HeaderTexts.ONAM);
        buffer.putInt(senFile.onam.sizeOfOnam);
        for (String name : senFile.onam.names) {
            for (char c : name.toCharArray()) {
                buffer.put((byte) c);
            }
            buffer.put((byte) 0);
        }
    }
}
