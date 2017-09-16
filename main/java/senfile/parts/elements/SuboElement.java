package senfile.parts.elements;

import senfile.Util;

import java.nio.ByteBuffer;

public class SuboElement {

    public final byte numFacesSigned;
    public final byte triangleOrQuad; // value = 3 or 4. TREAT SPECIALLY IF INHERENT TRIANGLE TO FIX RENDERING BUG !!!
    public final byte _3; // constant for objects
    public final byte alpha;

    public final short constant1; // == 0
    public final byte shortsPerFace;
    public final byte constant2; // == 0

    public final FaceInfo[] faceInfos;

    private final int offset;

    public String nameOfMesh;

    public SuboElement(ByteBuffer buffer, int offset) {
        this.offset = offset;

        numFacesSigned = buffer.get();
        triangleOrQuad = buffer.get();
        _3 = buffer.get();
        alpha = buffer.get();

        constant1 = buffer.getShort();
        shortsPerFace = buffer.get();
        constant2 = buffer.get();

        int numRestShorts = shortsPerFace - 2;
        int numFacesUnsigned = numFacesSigned & 0xFF;
        faceInfos = new FaceInfo[numFacesUnsigned];

        for (int i = 0; i < numFacesUnsigned; i++) {
            byte[] vertexIndices = new byte[4];
            buffer.get(vertexIndices);
            short[] restShorts = new short[numRestShorts];
            for (int j = 0; j < numRestShorts; j++) {
                restShorts[j] = buffer.getShort();
            }
            faceInfos[i] = new FaceInfo(vertexIndices, restShorts);
        }
    }

    public boolean isQuad() {
        return triangleOrQuad == 4;
    }

    public int getOffset() {
        return offset;
    }

    public void setNameOfMesh(String nameOfMesh) {
        this.nameOfMesh = nameOfMesh;
    }

    public boolean ignoreBecauseUnderline() {
        return Util.ignoreBecauseUnderline(nameOfMesh);
    }

    public static final class FaceInfo {

        public final byte[] vertexIndices;
        // for numRestShorts 1 = mapi index
        // for numRestShorts 2 = linear increment + mapi index
        // for numRestShorts 3 = linear increment + mapi index + zero
        public final short[] restShorts;

        public FaceInfo(byte[] vertexIndices, short[] restShorts) {
            this.vertexIndices = vertexIndices;
            this.restShorts = restShorts;
        }

        public int getMapiIndex() {
            if (restShorts.length == 1) {
                return restShorts[0];
            }
            return restShorts[1];
        }

    }

}
