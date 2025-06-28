package senfile.parts.elements;

import senfile.Util;

import java.nio.ByteBuffer;

/**
 * Always same amount of these as meshes EXCEPT for ENDSCENE
 */
public class ObjiElement {

    public final int _1; // linear steps around 6-13, probably references. To ONAM values?
    public final int _2; // Always 1, except for part of ENDSCENE
    public final int constant; // == 0

    public final float x;
    public final float y;
    public final float z;

    public final short rotX;
    public final short rotY;
    public final short rotZ;
    public final short elementIdx;

    public String nameOfMesh;

    public ObjiElement(ByteBuffer buffer) {
        _1 = buffer.getInt();
        _2 = buffer.getInt();
        constant = buffer.getInt();

        x = buffer.getFloat();
        y = buffer.getFloat();
        z = buffer.getFloat();

        rotX = buffer.getShort();
        rotY = buffer.getShort();
        rotZ = buffer.getShort();
        elementIdx = buffer.getShort();
    }

    public void setNameOfMesh(String nameOfMesh) {
        this.nameOfMesh = nameOfMesh;
    }

    public boolean ignoreBecauseUnderline() {
        return Util.ignoreBecauseUnderline(nameOfMesh);
    }

}