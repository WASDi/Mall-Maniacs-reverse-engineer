package senfile.parts.elements;

import senfile.Util;

import java.nio.ByteBuffer;

/**
 * Always same amount of these as meshes EXCEPT for ENDSCENE
 */
public class ObjiElement {

    public final int _1; // linear steps around 6-13
    public final int _2; // Always 1, except for part of ENDSCENE
    public final int constant; // == 0

    public final float x;
    public final float y;
    public final float z;

    public final int _7; // 0 for objects, 0x3FFF for characters. Many FF and 00 in all
    public final int _8; // two shorts, right part increment and left part majority 0

    public String nameOfMesh;

    public ObjiElement(ByteBuffer buffer) {
        _1 = buffer.getInt();
        _2 = buffer.getInt();
        constant = buffer.getInt();

        x = buffer.getFloat();
        y = buffer.getFloat();
        z = buffer.getFloat();

        _7 = buffer.getInt();
        _8 = buffer.getInt();
    }

    public void setNameOfMesh(String nameOfMesh) {
//        if (!ignoreBecauseUnderline()) {
//            debugXYZ(nameOfMesh);
//            debugOther(nameOfMesh);
//        }

        this.nameOfMesh = nameOfMesh;
    }

    private void debugXYZ(String nameOfMesh) {
        System.out.printf("%20s,   x ... %10.2f (%08X),   y ... %10.2f (%08X),   z ... %10.2f (%08X)\n",
                          nameOfMesh,
                          x, Float.floatToRawIntBits(x),
                          y, Float.floatToRawIntBits(y),
                          z, Float.floatToRawIntBits(z)
        );
    }

    private void debugOther(String nameOfMesh) {
        System.out.printf("%20s,  %8d (%08X)\n",
                          nameOfMesh,
                          _8, _8
        );
    }

    public boolean ignoreBecauseUnderline() {
        return Util.ignoreBecauseUnderline(nameOfMesh);
    }

}