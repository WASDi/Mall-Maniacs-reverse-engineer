package senfile.factories;

import senfile.parts.Mapi;
import senfile.parts.elements.MapiElement;

import java.nio.ByteBuffer;

public class MapiFactory {

    public static final int BYTES_PER_MAPI_ELEMENT = 16;

    public static Mapi parseFromBufferPosition(ByteBuffer buffer) {
        int bytesLeft = buffer.getInt();

        int numMapiElements = bytesLeft / BYTES_PER_MAPI_ELEMENT;
        MapiElement[] elements = new MapiElement[numMapiElements];

        for (int i = 0; i < numMapiElements; i++) {
            int _1 = buffer.getInt();
            int _2 = buffer.getInt();
            byte[] posBytes = new byte[8];
            buffer.get(posBytes);
            MapiElement mapiElement = new MapiElement(_1, _2, posBytes);
            elements[i] = mapiElement;
        }

        return new Mapi(elements);
    }

}
