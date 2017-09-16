package senfile.serializer;

import senfile.GameMap;
import senfile.HeaderTexts;
import senfile.SenFile;
import senfile.factories.SenFileFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class SenFileSerializer {

    public static void serialize(SenFile senFile) {

        int headerSizeInBytes = 8;
        ByteBuffer buffer = ByteBuffer.allocate(headerSizeInBytes + senFile.fileSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(HeaderTexts.REV2);
        buffer.putInt(senFile.fileSize);

        MeshSerializer.serializeMeshes(senFile, buffer);
        ColsSerializer.serializeCols(senFile, buffer);
        MapiSerializer.serializeMapi(senFile, buffer);
        SuboSerializer.serializeSubo(senFile, buffer);
        TnamSerializer.serializeTnam(senFile, buffer);
        ObjiSerializer.serializeObji(senFile, buffer);
        OnamSerializer.serializeOnam(senFile, buffer);

        while (buffer.position() < buffer.capacity()) {
            buffer.put((byte) 0); // padding
        }

        buffer.flip();
        File file = new File("/tmp/OUT.SEN");
        FileChannel channel = null;
        try {
            channel = new FileOutputStream(file, false).getChannel();
            channel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static int roundTo4WithMargin(int i) {
        return i - i % 4 + 4;
    }


    public static void main(String[] args) {
        SenFile senFile = SenFileFactory.getMap(GameMap.ICA);
        serialize(senFile);
    }


}
