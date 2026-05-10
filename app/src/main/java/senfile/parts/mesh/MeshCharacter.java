package senfile.parts.mesh;

import senfile.factories.VerticesFactory;

public class MeshCharacter extends SenMesh {

    public final int meshDataBaseSize; // == 280 (0x118) byte offset to vertex data from mesh start

    // Sub-object transform table: 15 entries (subObjIdx 1..15), each 8 bytes = 4 × int16.
    // Parse mirrors parse_sen.py parse_sub_object_table:
    //   struct.unpack_from('<hhh', data, sub_transform_offset + i*8)  → x, y, z
    // In rawData (int32 array), each entry occupies 2 consecutive ints:
    //   int0: low16 = offset_x, high16 = offset_y
    //   int1: low16 = offset_z, high16 = padding (ignored)
    public final short[] subTransformX = new short[15]; // offset_x for subObjIdx 1..15
    public final short[] subTransformY = new short[15]; // offset_y
    public final short[] subTransformZ = new short[15]; // offset_z

    // Sub-object material table: 15 entries (subObjIdx 1..15), each 4 bytes = 1 int32.
    // Previously misread as boneHierarchy_0..13 (14 ints); corrected to 15 ints.
    public final int[] subMaterialIndex = new int[15]; // material index for subObjIdx 1..15

    public final int maxDrawDistance; // == 10000000
    public final int numVertices;
    public final int vertexDataOffset; // == 280
    public final int reserved0; // == 0
    public final int vertexDataEndOffset; // == 280 + numVertices*8
    public final int numSuboReferences;
    public final int suboRefsTableOffset; // == 280 + numVertices*8
    public final int numVertexGroups;
    public final int faceGroupsCount; // 724 to 1280, dividable by 4
    public final int reserved1; // == 0
    public final int reserved2; // == 0
    public final int vertexGroupDefOffset; // == 280 + numVertices*8
    public final Vertex[] vertices;
    public final int[] suboReferences;
    public final VertexGroupDefinition[] vertexGroupDefinitions;

    public Vertex[][] groupedVertices;
    public int[][] groupedVertexIds;
    public int[] vertexId2Group;

    public MeshCharacter(String name, int bytesLeftUntilName, int meshIdx, int[] rawData) {
        super(name, bytesLeftUntilName, meshIdx, rawData);
        int idx = SHARED_DATA_SIZE;
        meshDataBaseSize = rawData[idx++];

        // Read 15 sub-transform entries (2 ints each = 30 ints total).
        // Entry i covers subObjIdx i+1.
        for (int i = 0; i < 15; i++) {
            int int0 = rawData[idx++]; // low16 = x, high16 = y
            int int1 = rawData[idx++]; // low16 = z, high16 = padding
            subTransformX[i] = (short) (int0 & 0xFFFF);
            subTransformY[i] = (short) ((int0 >> 16) & 0xFFFF);
            subTransformZ[i] = (short) (int1 & 0xFFFF);
        }

        // Read 15 sub-material entries (1 int each = 15 ints total).
        for (int i = 0; i < 15; i++) {
            subMaterialIndex[i] = rawData[idx++];
        }

        maxDrawDistance = rawData[idx++];
        numVertices = rawData[idx++];
        vertexDataOffset = rawData[idx++];
        reserved0 = rawData[idx++];
        vertexDataEndOffset = rawData[idx++];
        numSuboReferences = rawData[idx++];
        suboRefsTableOffset = rawData[idx++];
        numVertexGroups = rawData[idx++];
        faceGroupsCount = rawData[idx++];
        reserved1 = rawData[idx++];
        reserved2 = rawData[idx++];
        vertexGroupDefOffset = rawData[idx++];

        vertices = VerticesFactory.parseVertices(idx, numVertices, rawData);
        idx += numVertices * 2;

        suboReferences = new int[numSuboReferences];
        System.arraycopy(rawData, idx, suboReferences, 0, numSuboReferences);
        idx += numSuboReferences;

        vertexGroupDefinitions = new VertexGroupDefinition[numVertexGroups];
        int totalIntsLeft = parseVertexGroups(name, rawData, idx);

        groupVertices();

        idx += totalIntsLeft;
        int remainingInts = rawData.length - idx;
        if (remainingInts != 0) {
            throw new IllegalStateException("Parsed character but has " + remainingInts + " left for " + name);
        }
    }

    private void groupVertices() {
        groupedVertices = new Vertex[numVertexGroups][];
        groupedVertexIds = new int[numVertexGroups][];
        vertexId2Group = new int[numVertices];
        int i = 0;
        for (int x = 0; x < vertexGroupDefinitions.length; x++) {
            VertexGroupDefinition vertexGroup = vertexGroupDefinitions[x];
            groupedVertices[x] = new Vertex[vertexGroup.size];
            groupedVertexIds[x] = new int[vertexGroup.size];
            for (int y = 0; y < vertexGroup.size; y++) {
                groupedVertices[x][y] = vertices[i];
                groupedVertexIds[x][y] = i;
                vertexId2Group[i] = x;

                i++;
            }
        }
    }

    private int parseVertexGroups(String name, int[] rawData, int idx) {
        int totalIntsLeft = rawData.length - idx;
        int expectedNumVertexGroups = totalIntsLeft / 3;
        if (vertexGroupDefinitions.length != expectedNumVertexGroups) {
            throw new IllegalStateException("vertexGroupDefinitions has " + vertexGroupDefinitions.length
                                                    + " elements but expectedNumVertexGroups is " + expectedNumVertexGroups
                                                    + " for character " + name);
        }

        // VONKEL     sizes (16): 4, 13, 20, 3, 4, 4, 3, 4, 4, 1, 6, 7, 4, 6, 7, 4
        // KASSOERSKA sizes (10): 4, 14, 13, 3, 4, 4, 3, 4, 4, 10
        for (int i = 0; i < vertexGroupDefinitions.length; i++) {
            int size = rawData[idx + i * 3];
            int constant0 = rawData[idx + i * 3 + 1];
            int index = rawData[idx + i * 3 + 2];
            VertexGroupDefinition ending3Ints = new VertexGroupDefinition(size, constant0, index);
            vertexGroupDefinitions[i] = ending3Ints;
        }
        return totalIntsLeft;
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
