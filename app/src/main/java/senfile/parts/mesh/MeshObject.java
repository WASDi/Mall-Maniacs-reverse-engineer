package senfile.parts.mesh;

import senfile.factories.VerticesFactory;

public class MeshObject extends SenMesh {

    public final int constant8; // == 100
    public final int constant9; // == 10000000
    public final int numVertices;
    public final int constant10; // == 100
    public final int constant11; // == 0
    public final int ok_18; // == 100 + numVertices*8
    public final int numSuboReferences;
    public final int ok_20; // == 100 + numVertices*8
    public final int constant12; // == 1
    public final int _22; // Evenly divided by 8. Has relationship to ok_
    public final int constant13; // == 0
    public final int constant14; // == 0
    public final int ok_25; // == 100 + numVertices*8
    public final Vertex[] vertices;
    public final int[] suboReferences;
    public final VertexGroupDefinition vertexGroup;

    public MeshObject(String name, int bytesLeftUntilName, int meshIdx, int[] rawData) {
        super(name, bytesLeftUntilName, meshIdx, rawData);
        int idx = SHARED_DATA_SIZE;
        constant8 = rawData[idx++];
        constant9 = rawData[idx++];
        numVertices = rawData[idx++];
        constant10 = rawData[idx++];
        constant11 = rawData[idx++];
        ok_18 = rawData[idx++];
        numSuboReferences = rawData[idx++];
        ok_20 = rawData[idx++];
        constant12 = rawData[idx++];
        _22 = rawData[idx++];
        constant13 = rawData[idx++];
        constant14 = rawData[idx++];
        ok_25 = rawData[idx++];

        vertices = VerticesFactory.parseVertices(idx, numVertices, rawData);
        idx += numVertices * 2;

        suboReferences = new int[numSuboReferences];
        System.arraycopy(rawData, idx, suboReferences, 0, numSuboReferences);
        idx += numSuboReferences;

        vertexGroup = new VertexGroupDefinition(rawData[idx++],
                                                rawData[idx++],
                                                rawData[idx++]);
    }

    @Override
    public Vertex[] getVertices() {
        return vertices;
    }

    @Override
    public int[] getSuboOffsets() {
        return suboReferences;
    }

    @Override
    public void visit(MeshVisitor visitor) {
        visitor.visit(this);
    }

}
