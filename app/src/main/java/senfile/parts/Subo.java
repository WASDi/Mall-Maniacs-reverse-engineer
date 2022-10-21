package senfile.parts;

import senfile.parts.elements.SuboElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Subo {

    public final List<SuboElement> elements;
    public final Map<Integer, SuboElement> elementByOffset;

    public final int sizeOfSubo;

    public Subo(List<SuboElement> elements, int sizeOfSubo) {
        this.elements = elements;
        this.sizeOfSubo = sizeOfSubo;

        elementByOffset = new HashMap<>();
        for (SuboElement element : elements) {
            elementByOffset.put(element.getOffset(), element);
        }
    }

    public SuboElement elementByOffset(int offset) {
        return elementByOffset.get(offset);
    }
}
