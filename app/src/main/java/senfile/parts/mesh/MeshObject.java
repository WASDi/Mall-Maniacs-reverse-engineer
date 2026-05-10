package senfile.parts.mesh;

import senfile.factories.VerticesFactory;

public class MeshObject extends SenMesh {

    public final int meshDataBaseSize; // == 100
    public final int maxDrawDistance; // == 10000000
    public final int numVertices;
    public final int vertexDataOffset; // == 100
    public final int reserved0; // == 0
    public final int vertexDataEndOffset; // == 100 + numVertices*8
    public final int numSuboReferences;
    public final int suboRefsTableOffset; // == 100 + numVertices*8
    public final int numMeshParts; // == 1
    public final int faceGroupsCount; // evenly divisible by 8
    public final int reserved1; // == 0
    public final int reserved2; // == 0
    public final int vertexGroupDefOffset; // == 100 + numVertices*8
    public final Vertex[] vertices;
    public final int[] suboReferences;
    public final VertexGroupDefinition vertexGroup;

    public MeshObject(String name, int bytesLeftUntilName, int meshIdx, int[] rawData) {
        super(name, bytesLeftUntilName, meshIdx, rawData);
        int idx = SHARED_DATA_SIZE;
        meshDataBaseSize = rawData[idx++];
        maxDrawDistance = rawData[idx++];
        numVertices = rawData[idx++];
        vertexDataOffset = rawData[idx++];
        reserved0 = rawData[idx++];
        vertexDataEndOffset = rawData[idx++];
        numSuboReferences = rawData[idx++];
        suboRefsTableOffset = rawData[idx++];
        numMeshParts = rawData[idx++];
        faceGroupsCount = rawData[idx++];
        reserved1 = rawData[idx++];
        reserved2 = rawData[idx++];
        vertexGroupDefOffset = rawData[idx++];

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
