package senfile.factories;

import senfile.parts.Subo;
import senfile.parts.elements.SuboElement;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SuboFactory {

    private static final int SUBO_ELEMENT_HEADER_SIZE = 8;

    public static Subo parseFromBufferPosition(ByteBuffer buffer) {
        int bytesLeft = buffer.getInt();
        int endPos = buffer.position() + bytesLeft;

        List<SuboElement> elements = new ArrayList<>();

        int startOffset = buffer.position();
        while (endPos - buffer.position() >= SUBO_ELEMENT_HEADER_SIZE) {
            int offset = buffer.position() - startOffset;
            SuboElement element = new SuboElement(buffer, offset);
            elements.add(element);
        }

        if (buffer.position() != endPos) {
            // padding
            buffer.position(endPos);
        }

        return new Subo(elements, bytesLeft);
    }

}
