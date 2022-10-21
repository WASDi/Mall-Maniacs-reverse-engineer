package senfile.serializer;

import senfile.HeaderTexts;
import senfile.SenFile;
import senfile.factories.MapiFactory;
import senfile.parts.Mapi;
import senfile.parts.elements.MapiElement;

import java.nio.ByteBuffer;

public class MapiSerializer {
    public static void serializeMapi(SenFile senFile, ByteBuffer buffer) {
        Mapi mapi = senFile.mapi;

        buffer.putInt(HeaderTexts.MAPI);
        buffer.putInt(MapiFactory.BYTES_PER_MAPI_ELEMENT * mapi.elements.length);
        for (MapiElement mapiElement : mapi.elements) {
            buffer.putInt(mapiElement.tpgFileIndex);
            buffer.putInt(mapiElement.isInPhFile);
            for (byte textureCoordByte : mapiElement.textureCoordBytes) {
                buffer.put(textureCoordByte);
            }
        }
    }
}
