package senfile.parts.mesh;

import senfile.Util;

public abstract class SenMesh {

    protected static final int SHARED_DATA_SIZE = 12;

    public final String name;
    public final int bytesLeftUntilName;
    public final int meshIdx;

    public final int constant1; // == 1
    public final int constant2; // == 1
    public final int isCharacter1; // 15 (0xF) if true, 0 if false
    public final int constant3; // == 52
    public final int isCharacter2; // 172 (0xAC) if true, 52 (0x34) if false
    public final int isCharacter3; // 232 (0xE8) if true, 52 (0x34) if false

    public final int _7; // Delat värde mellan liknande mesh. Vid ändring försvinner rendering vid olika avstånd

    public final int constant4; // == 0
    public final int constant5; // == 0
    public final int constant6; // == 0

    public final int _11; // Majority is 0. 23347218 for some MALL1_ICA and 68321298 for some PHFUTUREMALL and WOODMALL

    public final int constant7; // == 0


    public SenMesh(String name, int bytesLeftUntilName, int meshIdx, int[] rawData) {
        this.name = name;
        this.bytesLeftUntilName = bytesLeftUntilName;
        this.meshIdx = meshIdx;
        int idx = 0;
        this.constant1 = rawData[idx++];
        this.constant2 = rawData[idx++];
        this.isCharacter1 = rawData[idx++];
        this.constant3 = rawData[idx++];
        this.isCharacter2 = rawData[idx++];
        this.isCharacter3 = rawData[idx++];
        this._7 = rawData[idx++];
        this.constant4 = rawData[idx++];
        this.constant5 = rawData[idx++];
        this.constant6 = rawData[idx++];
        this._11 = rawData[idx++];
        this.constant7 = rawData[idx++];
    }

    public abstract Vertex[] getVertices();

    public abstract int[] getSuboOffsets();

    public abstract void visit(MeshVisitor visitor);

    public boolean ignoreBecauseUnderline() {
        return Util.ignoreBecauseUnderline(name);
    }
}
