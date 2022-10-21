package senfile.serializer;

import senfile.HeaderTexts;
import senfile.SenFile;
import senfile.parts.Tani;

import java.nio.ByteBuffer;

public class TaniSerializer {
    public static void serializeTani(SenFile senFile, ByteBuffer buffer) {
        Tani tani = senFile.tani;
        if (tani == null) {
            return;
        }

        buffer.putInt(HeaderTexts.TANI);
        buffer.putInt(tani.sizeOfTani);
        for (int taniInt : senFile.tani.taniInts) {
            buffer.putInt(taniInt);
        }

    }
}
