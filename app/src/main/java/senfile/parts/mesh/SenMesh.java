package senfile.parts.mesh;

import senfile.Util;

public abstract class SenMesh {

    protected static final int SHARED_DATA_SIZE = 12;

    public final String name;
    public final int bytesLeftUntilName;
    public final int meshIdx;

    public final int flags; // uint32 flags (always 1 in observed data)
    public final int unknown1; // uint32 unknown (always 1 in observed data)
    public final int numExtraSub; // extra sub-objects beyond root (0 for mesh objects, non-zero for characters)
    public final int subTransformOffset; // byte offset into blob of sub-object transform table
    public final int subMaterialOffset; // byte offset into blob of per-sub material index table
    public final int lodOffset; // byte offset into blob of first LOD descriptor

    public final int meshHandle; // display-list handle / render-object index (patched at runtime)

    public final int reserved0; // == 0
    public final int reserved1; // == 0
    public final int reserved2; // == 0

    public final int unknown4; // Majority is 0. 23347218 for some MALL1_ICA and 68321298 for some PHFUTUREMALL and WOODMALL

    public final int reserved4; // == 0


    public SenMesh(String name, int bytesLeftUntilName, int meshIdx, int[] rawData) {
        this.name = name;
        this.bytesLeftUntilName = bytesLeftUntilName;
        this.meshIdx = meshIdx;
        int idx = 0;
        this.flags = rawData[idx++];
        this.unknown1 = rawData[idx++];
        this.numExtraSub = rawData[idx++];
        this.subTransformOffset = rawData[idx++];
        this.subMaterialOffset = rawData[idx++];
        this.lodOffset = rawData[idx++];
        this.meshHandle = rawData[idx++];
        this.reserved0 = rawData[idx++];
        this.reserved1 = rawData[idx++];
        this.reserved2 = rawData[idx++];
        this.unknown4 = rawData[idx++];
        this.reserved4 = rawData[idx++];
    }

    public abstract Vertex[] getVertices();

    public abstract int[] getSuboOffsets();

    public abstract void visit(MeshVisitor visitor);

    public boolean ignoreBecauseUnderline() {
        return Util.ignoreBecauseUnderline(name);
    }
}
