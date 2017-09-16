package senfile.serializer;

import senfile.HeaderTexts;
import senfile.SenFile;

import java.nio.ByteBuffer;

public class TnamSerializer {
    public static void serializeTnam(SenFile senFile, ByteBuffer buffer) {
        buffer.putInt(HeaderTexts.TNAM);
        buffer.putInt(senFile.tnam.sizeOfTnam);
        for (String name : senFile.tnam.names) {
            for (char c : name.toCharArray()) {
                buffer.put((byte) c);
            }
            buffer.put((byte) 0);
        }
    }
}
