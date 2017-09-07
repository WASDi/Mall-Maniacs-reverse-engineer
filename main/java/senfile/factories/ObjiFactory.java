package senfile.factories;

import senfile.parts.Obji;
import senfile.parts.elements.ObjiElement;
import senfile.parts.elements.SuboElement;

import java.nio.ByteBuffer;

public class ObjiFactory {

    private static final int BYTES_PER_OBJI_ELEMENT = 32;

    public static Obji parseFromBufferPosition(ByteBuffer buffer) {
        int bytesLeft = buffer.getInt();

        int numElements = bytesLeft / BYTES_PER_OBJI_ELEMENT;
        ObjiElement[] elements = new ObjiElement[numElements];

        for (int i = 0; i < numElements; i++) {
            ObjiElement suboElement = new ObjiElement(buffer);
            elements[i] = suboElement;
        }

        return new Obji(elements);
    }

}
