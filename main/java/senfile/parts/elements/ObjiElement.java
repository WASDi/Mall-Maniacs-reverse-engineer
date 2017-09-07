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

    public final short xLeft;
    public final short xRight;

    public final short yLeft;
    public final short yRight; // negativt är uppåt

    public final short zLeft;
    public final short zRight; // -18102 till 14502 OCH -1 eller 0 OCH 13740 till 18355

    public final int _7; // 0 for objects, 0x3FFF for characters. Many FF and 00 in all
    public final int _8; // two shorts, right part increment and left part majority 0

    public String nameOfMesh;

    public ObjiElement(ByteBuffer buffer) {
        _1 = buffer.getInt();
        _2 = buffer.getInt();
        constant = buffer.getInt();

        xLeft = buffer.getShort();
        xRight = buffer.getShort();

        yLeft = buffer.getShort();
        yRight = buffer.getShort();

        zLeft = buffer.getShort();
        zRight = buffer.getShort();

        _7 = buffer.getInt();
        _8 = buffer.getInt();
    }

    public void setNameOfMesh(String nameOfMesh) {
        if (!Util.IGNORE_UNDERLINES || nameOfMesh.charAt(0) != '_') {
            System.out.printf("%15s,   x ... %6d (%04X), %6d (%04X),   y ... %6d (%04X), %6d (%04X),   z ... %6d (%04X), %6d (%04X)\n",
                              nameOfMesh,
                              xLeft, xLeft,
                              xRight, xRight,
                              yLeft, yLeft,
                              yRight, yRight,
                              zLeft, zLeft,
                              zRight, zRight
            );
        }

        this.nameOfMesh = nameOfMesh;
    }

}

/*


Hur är formatet för XYZ?

Ica:
Lampor har Y = 04 00 FA C3



*/