package senfile.parts.elements;

/**
 * Defines a bounding box of 4 points from an image file to apply to a 3D face.
 */
public class MapiElement {

    public final int tpgFileIndex;
    public final int isInPhFile; // 0 if true, 0x640000 if false

    //x1,y1  x2,y2  x3,y3  x4,y4
    public final byte[] textureCoordBytes;

    public MapiElement(int _1, int _2, byte[] textureCoordBytes) {

        this.tpgFileIndex = _1;
        this.isInPhFile = _2;
        this.textureCoordBytes = textureCoordBytes;
    }
}
