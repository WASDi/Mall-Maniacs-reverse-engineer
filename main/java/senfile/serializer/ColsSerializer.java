package senfile.serializer;

import senfile.HeaderTexts;
import senfile.SenFile;

import java.nio.ByteBuffer;

public class ColsSerializer {
    public static void serializeCols(SenFile senFile, ByteBuffer buffer) {
        buffer.putInt(HeaderTexts.COLS);
        buffer.putInt(senFile.cols.sizeOfCols);
        for (int colInt : senFile.cols.colInts) {
            buffer.putInt(colInt);
        }
    }
}
