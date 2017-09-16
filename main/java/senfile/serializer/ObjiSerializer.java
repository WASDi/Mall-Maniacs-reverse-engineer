package senfile.serializer;

import senfile.HeaderTexts;
import senfile.SenFile;
import senfile.factories.ObjiFactory;
import senfile.parts.Obji;
import senfile.parts.elements.ObjiElement;

import java.nio.ByteBuffer;

public class ObjiSerializer {
    public static void serializeObji(SenFile senFile, ByteBuffer buffer) {
        Obji obji = senFile.obji;

        buffer.putInt(HeaderTexts.OBJI);
        buffer.putInt(ObjiFactory.BYTES_PER_OBJI_ELEMENT * obji.elements.length);
        for (ObjiElement element : obji.elements) {
            boolean entredoors = element.nameOfMesh.equals("ENTREDOORS");

            buffer.putInt(element._1);
            buffer.putInt(element._2);
            buffer.putInt(element.constant);
            buffer.putFloat(element.x);
            buffer.putFloat(element.y);
            buffer.putFloat(element.z + (entredoors ? -10000 : 0));
            buffer.putShort(element.rotX);
            buffer.putShort((short) (element.rotY + (entredoors ? Short.MAX_VALUE : 0)));
            buffer.putShort(element.rotZ);
            buffer.putShort(element.elementIdx);
        }
    }
}
