package senfile.serializer;

import senfile.HeaderTexts;
import senfile.SenFile;
import senfile.parts.Subo;
import senfile.parts.elements.SuboElement;

import java.nio.ByteBuffer;

public class SuboSerializer {
    public static void serializeSubo(SenFile senFile, ByteBuffer buffer) {
        Subo subo = senFile.getSubo();

        buffer.putInt(HeaderTexts.SUBO);
        buffer.putInt(subo.sizeOfSubo);
        int endPos = buffer.position() + subo.sizeOfSubo;

        for (SuboElement element : subo.elements) {
            buffer.put(element.numFacesSigned);
            buffer.put(element._2_boolean);
            buffer.put(element._3);
            buffer.put(element.alpha);
            buffer.putShort(element.constant1);
            buffer.put(element.shortsPerFace);
            buffer.put(element.constant2);
            for (SuboElement.FaceInfo faceInfo : element.faceInfos) {
                for (byte vertexIndex : faceInfo.vertexIndices) {
                    buffer.put(vertexIndex);
                }
                for (short restShort : faceInfo.restShorts) {
                    buffer.putShort(restShort);
                }
            }
        }

        if (buffer.position() < endPos) {
            while (buffer.position() < endPos) {
                buffer.put((byte) 0); // padding
            }
        } else if (buffer.position() > endPos) {
            throw new IllegalStateException("SuboSerializer went beyond its size");
        }
    }
}
