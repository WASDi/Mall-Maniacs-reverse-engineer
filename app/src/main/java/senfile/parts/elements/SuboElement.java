package senfile.parts.elements;

import senfile.Util;

import java.nio.ByteBuffer;

public class SuboElement {

    public final byte numFacesSigned;
    public final byte facesPerPolygon; // 3=triangles, 4=quads. TREAT SPECIALLY IF INHERENT TRIANGLE TO FIX RENDERING BUG !!!
    public final byte constantValue4; // constant value 4 for objects?
    public final byte transparency; // Different values, does it mean order of rendering?

    public final short constant1; // == 0
    public final byte shortsPerFace;
    public final byte constant2; // == 0

    public final FaceInfo[] faceInfos;

    private final int offset;

    public String nameOfMesh;

    public SuboElement(ByteBuffer buffer, int offset) {
        this.offset = offset;

        numFacesSigned = buffer.get();
        facesPerPolygon = buffer.get();
        constantValue4 = buffer.get();
        transparency = buffer.get();

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
        return facesPerPolygon == 4;
    }

    public SuboElementTransparencyMode getTransparencyMode() {
        return SuboElementTransparencyMode.resolveFromByte(transparency);
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
        // ABOVE IS FALSE FOR TRANSPARENT FACES
        public final short[] faceData;

        public FaceInfo(byte[] vertexIndices, short[] faceData) {
            this.vertexIndices = vertexIndices;
            this.faceData = faceData;
        }

        public int getMapiIndex() {
            if (faceData.length == 0) {
                throw new IllegalArgumentException("faceData.length == 0");
            } else if (faceData.length == 1) {
                return faceData[0];
            }

            if (faceData[1] == 0) {
                // Transparent face?
                return faceData[0];
            }

            return faceData[1];
        }

    }

}
