package senfile.parts.elements;

/**
 * Defines a bounding box of 4 points from an image file to apply to a 3D face.
 */
public class MapiElement {

    public final int materialIndex;
    public final int flags; // 0=in PH archive, 0x640000=standalone file

    //x1,y1  x2,y2  x3,y3  x4,y4
    public final byte[] textureCoordBytes;

    public MapiElement(int materialIndex, int flags, byte[] textureCoordBytes) {

        this.materialIndex = materialIndex;
        this.flags = flags;
        this.textureCoordBytes = textureCoordBytes;
    }
}
