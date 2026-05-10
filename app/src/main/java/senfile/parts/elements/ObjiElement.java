package senfile.parts.elements;

import senfile.Util;

import java.nio.ByteBuffer;

/**
 * Always same amount of these as meshes EXCEPT for ENDSCENE
 */
public class ObjiElement {

    public final int nameOffset; // byte offset into ONAM buffer
    public final int objectType; // 1=mesh, 3=sentinel/end, other=billboard
    public final int meshHandle; // index into KEEP mesh table

    public final float x;
    public final float y;
    public final float z;

    public final short rotX;
    public final short rotY;
    public final short rotZ;
    public final short sceneryEntryIdx;

    public String nameOfMesh;

    public ObjiElement(ByteBuffer buffer) {
        nameOffset = buffer.getInt();
        objectType = buffer.getInt();
        meshHandle = buffer.getInt();

        x = buffer.getFloat();
        y = buffer.getFloat();
        z = buffer.getFloat();

        rotX = buffer.getShort();
        rotY = buffer.getShort();
        rotZ = buffer.getShort();
        sceneryEntryIdx = buffer.getShort();
    }

    public void setNameOfMesh(String nameOfMesh) {
        this.nameOfMesh = nameOfMesh;
    }

    public boolean ignoreBecauseUnderline() {
        return Util.ignoreBecauseUnderline(nameOfMesh);
    }

}